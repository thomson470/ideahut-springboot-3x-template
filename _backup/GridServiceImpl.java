package net.ideahut.springboot.template.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.ideahut.springboot.mapper.DataMapper;
import net.ideahut.springboot.object.Option;
import net.ideahut.springboot.template.service.GridHelper.GridAdditional;
import net.ideahut.springboot.template.service.GridHelper.GridOption;
import net.ideahut.springboot.util.BeanUtil;

@Service
public class GridServiceImpl implements GridService, BeanUtil.BeanConfigure<GridService> {
	
	private static final TypeReference<Set<String>> TYPEREF_KEYS = new TypeReference<>() {};
	// menuGrids untuk kebutuhan menu sample, bisa juga tidak digunakan kalau menu disimpan ke database
	private static final TypeReference<Map<String, Map<String, String>>> TYPEREF_MENUS = new TypeReference<>() {};
	
	private static final class Keys {
		private Keys() {}
		private static final String PREFIX = "GridService";
		private static final String LOCK = PREFIX + "--LOCK--";
		private static final String MENU = PREFIX + "--MENU--";
		private static final String LIST = PREFIX + "--LIST--";
	}
	
	@Autowired
	private ApplicationContext appContext;
	@Autowired
	private DataMapper dataMapper;
	@Autowired
	private RedisTemplate<String, byte[]> redisTemplate;
	
	private Map<String, GridOption> gridOptions = new LinkedHashMap<>();
	private Map<String, GridAdditional> gridAdditionals = new LinkedHashMap<>();

	
	@Override
	public Callable<GridService> reconfigureBean(Collection<Object> arguments) {
		GridServiceImpl self = this;
		return new Callable<GridService>() {
			@Override
			public GridService call() throws Exception {
				reload();
				return self;
			}
		};
	}

	@Override
	public boolean isBeanConfigured() {
		return true;
	}
	
	private synchronized boolean lock(boolean yes) {
		if (yes) {
			ValueOperations<String, byte[]> lockOps = redisTemplate.opsForValue();
			byte[] lockBytes = lockOps.get(Keys.LOCK);
			if (lockBytes != null) {
				return false;
			}
			lockOps.set(Keys.LOCK, "1".getBytes());
		} else {
			redisTemplate.delete(Keys.LOCK);
		}
		return true;
	}
	
	private void clear() {
		ValueOperations<String, byte[]> valueOps = redisTemplate.opsForValue();
		byte[] bytes = valueOps.get(Keys.LIST);
		if (bytes != null) {
			Set<String> cacheKeys = dataMapper.read(bytes, TYPEREF_KEYS);
			cacheKeys.add(Keys.LIST);
			cacheKeys.add(Keys.MENU);
			redisTemplate.delete(cacheKeys);
		}
	}
	
	public boolean reload() throws Exception {
		if (!lock(true)) {
			return false;
		}
		clear();
		Map<String, Map<String, String>> menus = new LinkedHashMap<>();
		Set<String> cacheKeys = new HashSet<String>(); 
		try {
			PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(this.getClass().getClassLoader());
			String prefix = "/grid/";
			Resource[] resources = resolver.getResources("classpath:" + prefix + "**/*.json");
			ValueOperations<String, byte[]> valueOps = redisTemplate.opsForValue();
			Map<String, String> menu;
			for (Resource resource : resources) {
				String path = resource.getURI().getPath();
				int find = path.indexOf(prefix);
				path = path.substring(find + prefix.length());
				find = path.lastIndexOf("/");
				String parent = "_";
				if (find != -1) {
					parent = path.substring(0, find);
				}
				byte[] bytes = resource.getContentAsByteArray();
				JsonNode node = dataMapper.read(bytes, JsonNode.class, DataMapper.JSON);
				String name = node.get("name").asText();
				String ckey = Keys.PREFIX + "-" + parent + "-" + name;
				valueOps.set(ckey, bytes);
				cacheKeys.add(ckey);
				menu = menus.get(parent);
				if (menu == null) {
					menu = new LinkedHashMap<>();
					menus.put(parent, menu);
				}
				menu.put(name, node.get("title").asText());
			}
			byte[] bytes = dataMapper.writeAsBytes(cacheKeys, DataMapper.JSON);
			valueOps.set(Keys.LIST, bytes);
			bytes = dataMapper.writeAsBytes(menus, DataMapper.JSON);
			valueOps.set(Keys.MENU, bytes);
		} catch (Exception e) {
			clear();
			throw e;
		} finally {
			cacheKeys.clear();
			lock(false);
		}
		
		// Register Options
		gridOptions.clear();
		gridOptions.put("CRUD_CONDITION", GridHelper.StaticOption.CRUD_CONDITION);
		gridOptions.put("GENDER", GridHelper.StaticOption.GENDER);
		gridOptions.put("YES_NO", GridHelper.StaticOption.YES_NO);
		gridOptions.put("USER_STATUS", GridHelper.StaticOption.USER_STATUS);
		gridOptions.put("MENU_TYPE", GridHelper.StaticOption.MENU_TYPE);
		
		// Register Additionals
		gridAdditionals.clear();
		gridAdditionals.put("DAYS", GridHelper.StaticAdditional.DAYS);
		gridAdditionals.put("MONTHS", GridHelper.StaticAdditional.MONTHS);
		
		return true;
	}

	@Override
	public JsonNode getGrid(String parent, String name) {
		String ckey = Keys.PREFIX + "-" + (parent != null ? parent : "_") + "-" + name;
		ValueOperations<String, byte[]> valueOps = redisTemplate.opsForValue();
		byte[] bytes = valueOps.get(ckey);
		if (bytes == null) {
			return null;
		}
		ObjectNode jgrid = dataMapper.read(bytes, ObjectNode.class);
		if (jgrid.has("options")) {
			JsonNode joptions = jgrid.get("options");
			if(joptions.isArray()) {
				ObjectNode nodes = dataMapper.createObjectNode();
				ArrayNode jarrays = (ArrayNode) joptions;
				for (JsonNode joption : jarrays) {
					String key = joption.asText();
					GridOption option = gridOptions.get(key);
					if (option != null) {
						List<Option> options = option.getOptions(appContext);
						nodes.putArray(key).addAll(dataMapper.convert(options, ArrayNode.class));
					}
				}
				jgrid.remove("options");
				jgrid.putIfAbsent("options", nodes);
			} else {
				jgrid.remove("options");
			}
		}
		if (jgrid.has("additionals")) {
			JsonNode jadditionals = jgrid.get("additionals");
			if(jadditionals.isArray()) {
				ObjectNode nodes = dataMapper.createObjectNode();
				ArrayNode jarrays = (ArrayNode) jadditionals;
				for (JsonNode jadditional : jarrays) {
					String key = jadditional.asText();
					GridAdditional additional = gridAdditionals.get(key);
					if (additional != null) {
						JsonNode node = additional.getAdditional(appContext);
						nodes.putIfAbsent(key, node);
					}
				}
				jgrid.remove("additionals");
				jgrid.putIfAbsent("additionals", nodes);
			} else {
				jgrid.remove("additionals");
			}
		}
		return jgrid;
	}

	@Override
	public Map<String, Map<String, String>> getMenus() {
		ValueOperations<String, byte[]> valueOps = redisTemplate.opsForValue();
		byte[] bytes = valueOps.get(Keys.MENU);
		if (bytes == null) {
			return new HashMap<>();
		}
		return dataMapper.read(bytes, TYPEREF_MENUS);
	}	

}
