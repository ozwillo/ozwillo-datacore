package org.oasis.datacore.core.meta.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

public enum DCFieldTypeEnum {

	STRING("string", String.class),
	BOOLEAN("boolean", Boolean.class),
	INTEGER("int", Integer.class),
	FLOAT("float", Float.class),
	LONG("long", Long.class),
	DOUBLE("double", Double.class),
	DATE("date", DateTime.class),
	MAP("map", Map.class),
	LIST("list", List.class),
	RESOURCE("resource", DCField.class),
	NOT_FOUND("", null)
	;
	
	private String type;
	private Class toClass;
	
	private DCFieldTypeEnum(String type, Class toClass) {
		this.type = type;
		this.toClass = toClass;
	}
	
	public String getType() {
		return this.type;
	}
	
	public Class getToClass() {
		return this.toClass;
	}
	
	public static DCFieldTypeEnum getEnumFromStringType(String type) {
		
		if(type != null && !"".equals(type)) {
			for (DCFieldTypeEnum dcFieldTypeEnum : DCFieldTypeEnum.values()) {
				if (dcFieldTypeEnum != null && type.equals(dcFieldTypeEnum.getType())) {
					return dcFieldTypeEnum;
				}
			}
		}
		
		return NOT_FOUND;
		
	}
	
	public static Collection<DCFieldTypeEnum> everyTypes() {
		Collection<DCFieldTypeEnum> listAllTypes = new ArrayList<>();
		for(DCFieldTypeEnum dcFieldTypeEnum : DCFieldTypeEnum.values()) {
			listAllTypes.add(dcFieldTypeEnum);
		}
		return listAllTypes;
	}
	
	public static Collection<DCFieldTypeEnum> onlyList() {
		Collection<DCFieldTypeEnum> listDcFieldTypeEnums = new ArrayList<>();
		listDcFieldTypeEnums.add(DCFieldTypeEnum.LIST);
		return listDcFieldTypeEnums;
	}
		
	public static Collection<DCFieldTypeEnum> onlyString() {
		Collection<DCFieldTypeEnum> listDcFieldTypeEnums = new ArrayList<>();
		listDcFieldTypeEnums.add(DCFieldTypeEnum.STRING);
		return listDcFieldTypeEnums;
	}
	
	public static Collection<DCFieldTypeEnum> everyTypesWithoutMapListAndResource() {
		Collection<DCFieldTypeEnum> listDcFieldTypeEnums = everyTypes();
		listDcFieldTypeEnums.remove(DCFieldTypeEnum.MAP);
		listDcFieldTypeEnums.remove(DCFieldTypeEnum.LIST);
		listDcFieldTypeEnums.remove(DCFieldTypeEnum.RESOURCE);
		return listDcFieldTypeEnums;
	}
	
	public static Collection<DCFieldTypeEnum> everyTypesWithoutMapAndList() {
		Collection<DCFieldTypeEnum> listDcFieldTypeEnums = everyTypes();
		listDcFieldTypeEnums.remove(DCFieldTypeEnum.MAP);
		listDcFieldTypeEnums.remove(DCFieldTypeEnum.LIST);
		return listDcFieldTypeEnums;
	}
	
}
