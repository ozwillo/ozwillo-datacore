package org.oasis.datacore.core.meta.model.contribution;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModelBase;

public class DCContributionMixin extends DCMixin {

	public static String MIXIN_NAME = "oasis.contribution";
	
	/**
	 * Fields
	 */
	public static String ID = "id";
	public static String TITLE = "title";
	public static String COMMENT = "comment";
	public static String VALIDATION_COMMENT = "validationComment";
	public static String USER_ID = "userId";
	public static String STATUS = "status";
	public static String MODEL_TYPE = "modelType";
	
	@Override
	public String getName() {
		return MIXIN_NAME;
	}
		
	public DCContributionMixin() {
		super();
	    this.addField(new DCField(ID, "string", false, 100));
	    this.addField(new DCField(TITLE, "string", false, 1));
	    this.addField(new DCField(COMMENT, "string", false, 100));
	    this.addField(new DCField(VALIDATION_COMMENT, "string", false, 100));
	    this.addField(new DCField(USER_ID, "string", false, 100));
	    this.addField(new DCField(STATUS, "string", false, 100));
	    this.addField(new DCField(MODEL_TYPE, "string", false, 100));
	}
	
	@Override
	public DCModelBase addField(DCField field) {
		field.setName(getFieldName(field.getName()));
		return super.addField(field);
	}
	
	public static String getFieldName(String fieldName) {
		String MIXIN_PREFIX = "odc";
		String MIXIN_FIELD_NAME_SEPARATOR = ":";
		return MIXIN_PREFIX + MIXIN_FIELD_NAME_SEPARATOR + fieldName;
	}
	
}
