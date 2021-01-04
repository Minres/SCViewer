package com.minres.scviewer.database;

import java.util.HashMap;

public class RelationTypeFactory {
	
	public static RelationType create(String name){
		if(registry.containsKey(name)){
			return registry.get(name);
		}else{
			RelationType relType = new RelationType(name);
			registry.put(name, relType);
			return relType;
		}
		
	}

	private RelationTypeFactory() {}
	
	private static HashMap<String, RelationType> registry = new HashMap<>();

}
