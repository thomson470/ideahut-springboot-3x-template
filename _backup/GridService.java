package net.ideahut.springboot.template.service;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public interface GridService {
	
	JsonNode getGrid(String parent, String name);
	Map<String, Map<String, String>> getMenus();

}
