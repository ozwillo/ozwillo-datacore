package org.oasis.datacore.rest.server.parsing.model;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;

import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;


/**
 * Operator enums and their sub operators must be declared in the order
 * in which they will be attempted to be parsed (save for empty "" equals),
 * ex. "==" before "=" and ">=" before "=" otherwise they will never be recognized.
 * 
 * NB. to specify an equality on a string value starting with =, use the == operator
 * or provide the value as JSON (i.e. quote the string).
 * 
 * @author agiraudon
 *
 */
public enum QueryOperatorsEnum {

	NOT_EQUALS(DCFieldTypeEnum.everyTypesWithoutMapAndList(), "<>","&lt;&gt;","$ne","!="),
	GREATER_OR_EQUAL(DCFieldTypeEnum.everyTypesWithoutMapListAndResource(), ">=","&gt;=","$gte"),
	LOWER_OR_EQUAL(DCFieldTypeEnum.everyTypesWithoutMapListAndResource(), "<=","&lt;=","$lte"),
	EQUALS(DCFieldTypeEnum.everyTypesWithoutMapAndList(), "==", "=", ""),
	GREATER_THAN(DCFieldTypeEnum.everyTypesWithoutMapListAndResource(), ">","&gt;","$gt"),
	LOWER_THAN(DCFieldTypeEnum.everyTypesWithoutMapListAndResource(), "<","&lt;","$lt"),
	SORT_DESC(DCFieldTypeEnum.everyTypesWithoutMapListAndResource(),"-"),
	SORT_ASC(DCFieldTypeEnum.everyTypesWithoutMapListAndResource(), "+"),
	IN(DCFieldTypeEnum.everyTypesWithoutMapAndList(), "$in"),
	NOT_IN(DCFieldTypeEnum.everyTypesWithoutMapAndList(), "$nin"),
	REGEX(DCFieldTypeEnum.onlyString(), "$regex"),
	EXISTS(DCFieldTypeEnum.everyTypes(), "$exists"),
	ALL(DCFieldTypeEnum.onlyList(), "$all"),
	ELEM_MATCH(DCFieldTypeEnum.onlyList(), "$elemMatch"),
	SIZE(DCFieldTypeEnum.onlyList(), "$size");
		
	private Collection<String> listOperators;
	private Collection<DCFieldTypeEnum> listCompatibleTypes;
	
	private QueryOperatorsEnum(Collection<DCFieldTypeEnum> compatibleTypes, String... operators) {
		this.listOperators = new ArrayList<String>();
		if(operators != null) {
			for(String operator : operators) {
				this.listOperators.add(operator);
			}
		}
		this.listCompatibleTypes = compatibleTypes;
	}
	
	public Collection<String> getListOperators() {
		return this.listOperators;
	}
	
	public Collection<DCFieldTypeEnum> getListCompatibleTypes() {
		return this.listCompatibleTypes;
	}

	public static SimpleEntry<QueryOperatorsEnum, Integer> getEnumFromOperator(String operator) {

		if(operator != null && !"".equals(operator)) {
			for (QueryOperatorsEnum queryOperatorsEnum : QueryOperatorsEnum.values()) {
				if (queryOperatorsEnum != null && queryOperatorsEnum.getListOperators() != null) {
					for (String tmpOperator : queryOperatorsEnum.getListOperators()) {
						if (tmpOperator != null && !"".equals(tmpOperator)) {
							if(tmpOperator.length() <= operator.length()) {
								if(tmpOperator.equals(operator.substring(0, tmpOperator.length()))) {
									return new SimpleEntry<>(queryOperatorsEnum, tmpOperator.length());
								}
							}
						}
					}
				}
			}
		}
		
		return new SimpleEntry<>(QueryOperatorsEnum.EQUALS, 0);
		
	}
	
}
