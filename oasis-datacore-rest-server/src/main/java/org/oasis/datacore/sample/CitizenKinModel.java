package org.oasis.datacore.sample;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.springframework.stereotype.Component;

/**
 * Citizen Kin model
 * @author jguillemotte
 */
@Component
public class CitizenKinModel extends DatacoreSampleBase {

   public static String USER_MODEL_NAME = "sample.citizenkin.user";
   public static String PROCEDURE_MODEL_NAME = "sample.citizenkin.procedure";

	
	@Override
	public void init() {

		mgo.dropCollection(USER_MODEL_NAME);
		mgo.dropCollection(PROCEDURE_MODEL_NAME);

        DCModel userModel = new DCModel(USER_MODEL_NAME);
        userModel.addField(new DCField("id", DCFieldTypeEnum.INTEGER.getType(), true, 100));
		userModel.addField(new DCField("firstName", DCFieldTypeEnum.STRING.getType(), true, 100));
        userModel.addField(new DCField("lastName", DCFieldTypeEnum.STRING.getType(), true, 100));

        DCModel procedureModel = new DCModel(PROCEDURE_MODEL_NAME);
        procedureModel.addField(new DCField("id", DCFieldTypeEnum.STRING.getType(), true, 100));
        //procedureModel.addField(new DCField("name", DCFieldTypeEnum.STRING.getType(), true, 100));
        procedureModel.addField(new DCListField("agents", new DCResourceField("agent", USER_MODEL_NAME)));

        modelAdminService.addModel(userModel);
        modelAdminService.addModel(procedureModel);

    }

}
