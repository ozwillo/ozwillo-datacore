package org.oasis.datacore.rest.server.parsing.model;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;


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

	NOT_EQUALS(DCFieldTypeEnum.equalableTypes, "<>","&lt;&gt;","$ne","!="), // LATER2 also Map & List ???
	GREATER_OR_EQUAL(DCFieldTypeEnum.comparableTypes, ">=","&gt;=","$gte"),
	LOWER_OR_EQUAL(DCFieldTypeEnum.comparableTypes, "<=","&lt;=","$lte"),
	EQUALS(DCFieldTypeEnum.equalableTypes, "==", "=", ""),
	GREATER_THAN(DCFieldTypeEnum.comparableTypes, ">","&gt;","$gt"),
	LOWER_THAN(DCFieldTypeEnum.comparableTypes, "<","&lt;","$lt"),
	SORT_DESC(DCFieldTypeEnum.comparableTypes,"-"),
	SORT_ASC(DCFieldTypeEnum.comparableTypes, "+"),
	IN(DCFieldTypeEnum.equalableTypes, DCFieldTypeEnum.LIST, "$in"),
	NOT_IN(DCFieldTypeEnum.equalableTypes, DCFieldTypeEnum.LIST, "$nin"),
	REGEX(DCFieldTypeEnum.onlyString, "$regex"),
	EXISTS(DCFieldTypeEnum.everyTypes, "$exists"),
	ALL(DCFieldTypeEnum.onlyList, DCFieldTypeEnum.LIST, "$all"),
	ELEM_MATCH(DCFieldTypeEnum.onlyList, DCFieldTypeEnum.LIST, "$elemMatch"),
	SIZE(DCFieldTypeEnum.onlyList, DCFieldTypeEnum.INTEGER, "$size");

   public static final Set<QueryOperatorsEnum> noValueOperators = Sets.immutableEnumSet(new ImmutableSet
         .Builder<QueryOperatorsEnum>().add(QueryOperatorsEnum.SORT_ASC)
         .add(QueryOperatorsEnum.SORT_DESC).add(QueryOperatorsEnum.EXISTS).build()); // uses EnumSet
   
	private Collection<String> listOperators;
	private Collection<DCFieldTypeEnum> listCompatibleTypes;
	/** field type to parse criteria value as, if differs from criteria field's (for
	 * $in, $nin, $all, $elemMatch, $size) ; else null */
   private DCFieldTypeEnum parsingType;
	
	private QueryOperatorsEnum(Collection<DCFieldTypeEnum> compatibleTypes,
	      DCFieldTypeEnum parsingType, String... operators) {
		this.listOperators = new ArrayList<String>();
		if(operators != null) {
			for(String operator : operators) {
				this.listOperators.add(operator);
			}
		}
      this.listCompatibleTypes = compatibleTypes;
      this.parsingType = parsingType;
	}
	
   private QueryOperatorsEnum(Collection<DCFieldTypeEnum> compatibleTypes, String... operators) {
      this(compatibleTypes, null, operators);
   }
	
	public Collection<String> getListOperators() {
		return this.listOperators;
	}
	
	public Collection<DCFieldTypeEnum> getListCompatibleTypes() {
		return this.listCompatibleTypes;
	}
   
   public DCFieldTypeEnum getParsingType() {
      return parsingType;
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
