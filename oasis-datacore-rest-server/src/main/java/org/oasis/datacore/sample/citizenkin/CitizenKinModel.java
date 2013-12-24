package org.oasis.datacore.sample.citizenkin;

import org.oasis.datacore.core.meta.model.*;
import org.oasis.datacore.sample.DatacoreSampleBase;
import org.springframework.stereotype.Component;

/**
 * User: schambon
 * Date: 12/24/13
 *
 * disabled for now
 */
//@Component
public class CitizenKinModel extends DatacoreSampleBase {

    public static final String MODEL_NAME = "citizenkin.forminstance";

    public static final String STRING_TYPE = DCFieldTypeEnum.STRING.getType();
    public static final String DATE_TYPE = DCFieldTypeEnum.DATE.getType();

    @Override
    public void init() {

        DCModel model = new DCModel(MODEL_NAME);
        model.addField(new DCField("definition_uuid", STRING_TYPE));
        model.addField(new DCField("definition_name", STRING_TYPE));
        model.addField(new DCField("initiator", STRING_TYPE));
        model.addField(new DCField("recipient", STRING_TYPE));
        model.addField(new DCField("startDate", DATE_TYPE));
        model.addField(new DCField("modifyDate", DATE_TYPE));
        model.addField(new DCField("state", STRING_TYPE));
        model.addField(new DCField("comment", STRING_TYPE));

        model.addField(new DCMapField("fields")); // field values; name -> opaque blob (actually, json)

        modelAdminService.addModel(model);

    }
}
