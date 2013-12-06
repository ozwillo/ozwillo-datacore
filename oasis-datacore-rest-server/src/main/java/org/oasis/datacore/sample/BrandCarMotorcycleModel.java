package org.oasis.datacore.sample;

import javax.annotation.PostConstruct;

import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Component;

/**
 * Car / Motorcycle / Brand
 * Sample Data
 * @author agiraudon
 *
 */

@Component
public class BrandCarMotorcycleModel {

	public static String CAR_MODEL_NAME = "sample.brand.car";
	public static String MOTORCYCLE_MODEL_NAME = "sample.brand.motorcycle";
	public static String BRAND_MODEL_NAME = "sample.brand.brand";

	@Autowired
	private DataModelServiceImpl modelAdminService;

	@Autowired
	private MongoOperations mongoOperations;
	
	@PostConstruct
	public void init() {
		
		mongoOperations.dropCollection(BRAND_MODEL_NAME);
		mongoOperations.dropCollection(CAR_MODEL_NAME);
		mongoOperations.dropCollection(MOTORCYCLE_MODEL_NAME);

		DCModel brandModel = new DCModel(BRAND_MODEL_NAME);
		brandModel.addField(new DCField("name", "string", true, 100));

		DCModel carModel = new DCModel(CAR_MODEL_NAME);
		carModel.addField(new DCResourceField("brand", BRAND_MODEL_NAME, true, 100));
		carModel.addField(new DCField("model", "string", true, 100));
		carModel.addField(new DCField("year", "int"));

		DCModel motorcycleModel = new DCModel(MOTORCYCLE_MODEL_NAME);
		motorcycleModel.addField(new DCResourceField("brand", BRAND_MODEL_NAME, true, 100));
		motorcycleModel.addField(new DCField("model", "string", true, 100));
		motorcycleModel.addField(new DCField("year", "int"));
		motorcycleModel.addField(new DCField("hp", "int"));

		modelAdminService.addModel(brandModel);
		modelAdminService.addModel(carModel);
		modelAdminService.addModel(motorcycleModel);

	}

}
