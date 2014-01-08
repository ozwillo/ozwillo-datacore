package org.oasis.datacore.core.meta.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

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
	RESOURCE("resource", Map.class), // or String (if not root or embedded Resource case)
	NOT_FOUND("", null)
	;
	
	private String type;
	private Class<?> toClass;
   public static final Set<DCFieldTypeEnum> everyTypes = Sets.immutableEnumSet(new ImmutableSet
         .Builder<DCFieldTypeEnum>().addAll(Arrays.asList(DCFieldTypeEnum.values())).build());
   public static final Set<DCFieldTypeEnum> onlyList = Sets.immutableEnumSet(new ImmutableSet
         .Builder<DCFieldTypeEnum>().add(DCFieldTypeEnum.LIST).build());
   public static final Set<DCFieldTypeEnum> onlyString = Sets.immutableEnumSet(new ImmutableSet
         .Builder<DCFieldTypeEnum>().add(DCFieldTypeEnum.STRING).build());
   public static Set<DCFieldTypeEnum> everyTypesWithoutMapListAndResource = Sets.immutableEnumSet(new ImmutableSet
         .Builder<DCFieldTypeEnum>().add(DCFieldTypeEnum.STRING).add(DCFieldTypeEnum.BOOLEAN).
         add(DCFieldTypeEnum.INTEGER).add(DCFieldTypeEnum.FLOAT).add(DCFieldTypeEnum.LONG).
         add(DCFieldTypeEnum.DOUBLE).add(DCFieldTypeEnum.DATE).build());
   public static Set<DCFieldTypeEnum> everyTypesWithoutMapAndList = Sets.immutableEnumSet(new ImmutableSet
         .Builder<DCFieldTypeEnum>().addAll(everyTypesWithoutMapListAndResource)
         .add(DCFieldTypeEnum.RESOURCE).build());
   public static final Set<DCFieldTypeEnum> stringPrimitiveTypes = Sets.immutableEnumSet(new ImmutableSet
         .Builder<DCFieldTypeEnum>().add(DCFieldTypeEnum.STRING).add(DCFieldTypeEnum.RESOURCE).build());
   public static final Set<DCFieldTypeEnum> stringSerializedPrimitiveTypes = Sets.immutableEnumSet(new ImmutableSet
         .Builder<DCFieldTypeEnum>().add(DCFieldTypeEnum.DATE).add(DCFieldTypeEnum.LONG).build());
   //public static final Set<DCFieldTypeEnum> stringSerializedPrimitiveTypes = Sets.immutableEnumSet(new ImmutableSet
   //      .Builder<DCFieldTypeEnum>().add(DCFieldTypeEnum.STRING).add(DCFieldTypeEnum.DATE)
   //      .add(DCFieldTypeEnum.LONG).add(DCFieldTypeEnum.RESOURCE).build());
	
	static {
      Collection<DCFieldTypeEnum> everyTypesWithoutMapListAndResource = new ArrayList<DCFieldTypeEnum>(everyTypes);
      everyTypesWithoutMapListAndResource.remove(DCFieldTypeEnum.MAP);
      everyTypesWithoutMapListAndResource.remove(DCFieldTypeEnum.LIST);
      everyTypesWithoutMapListAndResource.remove(DCFieldTypeEnum.RESOURCE);
	}
	
	private DCFieldTypeEnum(String type, Class<?> toClass) {
		this.type = type;
		this.toClass = toClass;
	}
	
	public String getType() {
		return this.type;
	}
	
	public Class<?> getToClass() {
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
	
}
