package org.oasis.datacore.contribution;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.oasis.datacore.contribution.rest.api.DCContribution;
import org.oasis.datacore.core.meta.model.contribution.DCContributionMixin;

public class ContributionUtils {

	public static String generateContributionId() {
		return RandomStringUtils.randomAlphanumeric(25);
	}
	
	
	public static Map<String,Object> getContributionPropertiesMap(DCContribution contribution) {
		
		Map<String, Object> map = new HashMap<String, Object>();
		
		for(Field field : DCContribution.class.getDeclaredFields()) {
			if(field != null && !"listResources".equals(field.getName())) {
				try {
					map.put(DCContributionMixin.getFieldName(field.getName()), field.get(contribution));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		
		return map;
		
	}
	
	public static List<String> getContributionFieldNameList() {
		
		List<String> fieldList = new ArrayList<>();
		
		for(Field field : DCContribution.class.getDeclaredFields()) {
			if(field != null && !"listResources".equals(field.getName())) {
				fieldList.add(DCContributionMixin.getFieldName(field.getName()));
			}
		}
		
		return fieldList;
		
	}
	
}
