package org.oasis.datacore.sample;

import java.util.Arrays;
import java.util.List;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.springframework.stereotype.Component;

/**
 * Car / Motorcycle / Brand
 * Sample Data
 * @author agiraudon
 *
 */

@Component
public class BrandCarMotorcycleModel extends DatacoreSampleBase {

	public static String CAR_MODEL_NAME = "sample.brand.car";
	public static String MOTORCYCLE_MODEL_NAME = "sample.brand.motorcycle";
	public static String BRAND_MODEL_NAME = "sample.brand.brand";

	@Override
   public void buildModels(List<DCModelBase> modelsToCreate) {

		DCModel brandModel = new DCModel(BRAND_MODEL_NAME);
		brandModel.setDocumentation(""); // TODO
		brandModel.addField(new DCField("name", "string", true, 100));

		DCModel carModel = new DCModel(CAR_MODEL_NAME);
		carModel.setDocumentation(""); // TODO
		carModel.addField(new DCResourceField("brand", BRAND_MODEL_NAME, true, 100));
		carModel.addField(new DCField("model", "string", true, 100));
		carModel.addField(new DCField("year", "int"));

		DCModel motorcycleModel = new DCModel(MOTORCYCLE_MODEL_NAME);
		motorcycleModel.setDocumentation(""); // TODO
		motorcycleModel.addField(new DCResourceField("brand", BRAND_MODEL_NAME, true, 100));
		motorcycleModel.addField(new DCField("model", "string", true, 100));
		motorcycleModel.addField(new DCField("year", "int"));
		motorcycleModel.addField(new DCField("hp", "int"));

		modelsToCreate.addAll(Arrays.asList((DCModelBase) brandModel, carModel, motorcycleModel));
	}

	@Override
	public void fillData() {
	}

}
