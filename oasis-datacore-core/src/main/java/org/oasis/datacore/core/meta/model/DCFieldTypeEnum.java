package org.oasis.datacore.core.meta.model;

public enum DCFieldTypeEnum {

	STRING("string"),
	BOOLEAN("boolean"),
	INTEGER("int"),
	FLOAT("float"),
	LONG("long"),
	DOUBLE("double"),
	DATE("date"),
	MAP("map"),
	LIST("list"),
	RESOURCE("resource");
	
	private String type;
	
	private DCFieldTypeEnum(String type) {
		this.type = type;
	}
	
	public String getType() {
		return this.type;
	}
	
}
