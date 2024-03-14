package net.ideahut.springboot.template.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import net.ideahut.springboot.crud.Condition;
import net.ideahut.springboot.mapper.DataMapper;
import net.ideahut.springboot.mapper.DataMapperImpl;
import net.ideahut.springboot.object.Option;
import net.ideahut.springboot.template.AppConstants;

/*
 * Alternatif bisa disimpan di database
 * dan dibuat class untuk mengisi Options & Additionals.
 * Dengan menyimpan di database, maka label bisa mengikuti bahasa aktif yang di-request.
 */
class GridHelper {
	
	private GridHelper() {}
	
	private static DataMapper mapper = new DataMapperImpl();
	
	
	static interface GridOption {
		List<Option> getOptions(ApplicationContext context);
	}
	
	static interface GridAdditional {
		JsonNode getAdditional(ApplicationContext context);
	}
	
	
	/*
	 * OPTIONS
	 */
	static class StaticOption {
		
		// YES / NO
		static GridOption YES_NO = new GridOption() {
			@Override
			public List<Option> getOptions(ApplicationContext context) {
				return Arrays.asList(
					new Option("Y", "Yes"),
					new Option("N", "No")
				);
			}
		};
		
		// GENDER
		static GridOption GENDER = new GridOption() {
			@Override
			public List<Option> getOptions(ApplicationContext context) {
				return Arrays.asList(
					new Option("M", "Male"),
					new Option("F", "Female")
				);
			}
		};
		
		// CRUD CONDITION
		static GridOption CRUD_CONDITION = new GridOption() {
			@Override
			public List<Option> getOptions(ApplicationContext context) {
				List<Option> options = new ArrayList<>();
				for (Condition condition : Condition.values()) {
					options.add(new Option(condition.name(), condition.name()));
				}
				return options;
			}
		};
		
		// USER STATUS
		static GridOption USER_STATUS = new GridOption() {
			@Override
			public List<Option> getOptions(ApplicationContext context) {
				return Arrays.asList(
					new Option(AppConstants.Profile.Status.REGISTER + "", "Register"),
					new Option(AppConstants.Profile.Status.ACTIVE + "", "Active"),
					new Option(AppConstants.Profile.Status.INACTIVE + "", "InActive"),
					new Option(AppConstants.Profile.Status.BLOCKED + "", "Blocked")
				);
			}
		};
		
		// MENU TYPE
		static GridOption MENU_TYPE = new GridOption() {
			@Override
			public List<Option> getOptions(ApplicationContext context) {
				return Arrays.asList(
					new Option("mobile", "Mobile"),
					new Option("portal", "Portal")
				);
			}
		};
	}
	
	/*
	 * ADDITIONALS
	 */
	static class StaticAdditional {
		
		// MONTHS
		static GridAdditional MONTHS = new GridAdditional() {
			@Override
			public JsonNode getAdditional(ApplicationContext context) {
				String str = "[\"January\", \"February\", \"March\", \"April\", \"May\", \"June\", \"July\", \"August\", \"September\", \"October\", \"November\", \"December\", \"Jan\", \"Feb\", \"Mar\", \"Apr\", \"May\", \"Jun\", \"Jul\", \"Aug\", \"Sep\", \"Oct\", \"Nov\", \"Dec\"]";
				return mapper.read(str, ArrayNode.class);
			}
		};
		
		
		// DAYS
		static GridAdditional DAYS = new GridAdditional() {
			@Override
			public JsonNode getAdditional(ApplicationContext context) {
				String str = "[\"Sunday\", \"Monday\", \"Tuesday\", \"Wednesday\", \"Thursday\", \"Friday\", \"Saturday\", \"Sun\", \"Mon\", \"Tue\", \"Wed\", \"Thu\", \"Fri\", \"Sat\"]";
				return mapper.read(str, ArrayNode.class);
			}
		};
		
		
	}

}
