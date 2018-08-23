package org.oasis.datacore.core.meta.model;

import java.util.Arrays;
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
	I18N("i18n", List.class),
	RESOURCE("resource", Map.class), // TODO (more commonly) or String (if not root or embedded Resource case)
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
   /** types that can be compared (& therefore sorted), i.e. all WithoutMapListAndResource ;
    * TODO add Resource in the case it has a sortable id ?!? */
   public static Set<DCFieldTypeEnum> comparableTypes = Sets.immutableEnumSet(new ImmutableSet
         .Builder<DCFieldTypeEnum>().add(DCFieldTypeEnum.STRING).add(DCFieldTypeEnum.BOOLEAN).
         add(DCFieldTypeEnum.INTEGER).add(DCFieldTypeEnum.FLOAT).add(DCFieldTypeEnum.LONG).
         add(DCFieldTypeEnum.DOUBLE).add(DCFieldTypeEnum.DATE).build());
   /** types that can be tested to equality (& therefore also for $in) i.e. all without map & list */
   public static Set<DCFieldTypeEnum> equalableTypes = Sets.immutableEnumSet(new ImmutableSet
         .Builder<DCFieldTypeEnum>().addAll(comparableTypes).add(DCFieldTypeEnum.RESOURCE).build());
   public static final Set<DCFieldTypeEnum> stringPrimitiveTypes = Sets.immutableEnumSet(new ImmutableSet
         .Builder<DCFieldTypeEnum>().add(DCFieldTypeEnum.STRING).add(DCFieldTypeEnum.RESOURCE).build());
   public static final Set<DCFieldTypeEnum> stringSerializedPrimitiveTypes = Sets.immutableEnumSet(new ImmutableSet
         .Builder<DCFieldTypeEnum>().add(DCFieldTypeEnum.DATE).add(DCFieldTypeEnum.LONG).build());

	DCFieldTypeEnum(String type, Class<?> toClass) {
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
