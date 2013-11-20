package org.oasis.datacore.sample;

import javax.annotation.PostConstruct;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Component;

/**
 * Citizen Kin model
 * @author jguillemotte
 */
@Component
public class CitizenKinModel {

    public static String USER_MODEL_NAME = "sample.citizenkin.user";
    public static String PROCEDURE_MODEL_NAME = "sample.citizenkin.procedure";

	@Autowired
	private DataModelServiceImpl modelAdminService;

	@Autowired
	private MongoOperations mongoOperations;

	@PostConstruct
	public void init() {

		mongoOperations.dropCollection(USER_MODEL_NAME);
		mongoOperations.dropCollection(PROCEDURE_MODEL_NAME);

        DCModel userModel = new DCModel(USER_MODEL_NAME);
        userModel.addField(new DCField("id", DCFieldTypeEnum.STRING.getType(), true, 100));
		userModel.addField(new DCField("firstName", DCFieldTypeEnum.STRING.getType(), true, 100));
        userModel.addField(new DCField("lastName", DCFieldTypeEnum.STRING.getType(), true, 100));

        DCModel procedureModel = new DCModel(PROCEDURE_MODEL_NAME);
        procedureModel.addField(new DCField("name", DCFieldTypeEnum.STRING.getType(), true, 100));
        procedureModel.addField(new DCListField("agents", new DCField("agent", DCFieldTypeEnum.RESOURCE.getType())));

        modelAdminService.addModel(userModel);
        modelAdminService.addModel(procedureModel);

    }

}
