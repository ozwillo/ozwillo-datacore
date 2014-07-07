package org.oasis.datacore.rest.server.parsing.model;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.HashSet;
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
	ALL(true, DCFieldTypeEnum.LIST, "$all"), // list that contains all elements
	ELEM_MATCH(true, DCFieldTypeEnum.MAP, "$elemMatch"), // list whose elements match all criteria ; NB. primitive types are easier done using equals !
	SIZE(true, DCFieldTypeEnum.INTEGER, "$size"); // list of size

   public static final Set<QueryOperatorsEnum> noValueOperators = Sets.immutableEnumSet(new ImmutableSet
         .Builder<QueryOperatorsEnum>().add(QueryOperatorsEnum.SORT_ASC)
         .add(QueryOperatorsEnum.SORT_DESC).add(QueryOperatorsEnum.EXISTS).build()); // uses EnumSet

   private Set<DCFieldTypeEnum> listCompatibleTypes;
   private boolean isListSpecificOperator = false;
	private Set<String> listOperators;
	/** field type to parse criteria value as, if differs from criteria field's (for
	 * $in, $nin, $all, $elemMatch, $size) ; else null */
   private DCFieldTypeEnum parsingType;
	
	private QueryOperatorsEnum(Set<DCFieldTypeEnum> compatibleTypes,
	      DCFieldTypeEnum parsingType, String... listOperators) {
      this.listCompatibleTypes = compatibleTypes;
      this.parsingType = parsingType;
		this.listOperators = (listOperators == null) ? new HashSet<String>(0)
		      : new HashSet<String>(Arrays.asList(listOperators));
	}
	/**
	 * Constructor for list-specific operators : $all, $elemMatch, $size
	 * NB. compatibleTypes are onlyList
    * @param isListSpecificOperator must be true
	 * @param parsingType
	 * @param listOperators
	 */
   private QueryOperatorsEnum(boolean isListSpecificOperator,
         DCFieldTypeEnum parsingType, String... listOperators) {
      this(DCFieldTypeEnum.onlyList, parsingType, listOperators);
      this.isListSpecificOperator = true;
   }
	
   private QueryOperatorsEnum(Set<DCFieldTypeEnum> compatibleTypes, String... operators) {
      this(compatibleTypes, null, operators);
   }
	
	public Set<DCFieldTypeEnum> getListCompatibleTypes() {
		return this.listCompatibleTypes;
	}

   public boolean isListSpecificOperator() {
      return isListSpecificOperator;
   }
   
   public DCFieldTypeEnum getParsingType() {
      return parsingType;
   }
   
   public Set<String> getListOperators() {
      return this.listOperators;
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
