package org.oasis.datacore.sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UnitTestHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("brandCarMotorcycleModel")
public class BrandCarMotorcycleData extends DatacoreSampleBase {
	
	private List<DCResource> listBrands = new ArrayList<DCResource>();;
	private List<DCResource> listCars = new ArrayList<DCResource>();;
	private List<DCResource> listMotorcycle = new ArrayList<DCResource>();;
	private HashMap<String, List<DCResource>> mapData = new HashMap<String, List<DCResource>>();
	
	@Value("${datacoreApiServer.containerUrl}")
	private String containerUrl;
	
	@Value("#{new Boolean('${datacoreApiServer.enableBrandSampleDataInsertionAtStartup}')}")
	private Boolean enableBrandSampleDataInsertionAtStartup;
	
	/** to help clean data */
	@Autowired // (makes @DependsOn not necessary)
	private BrandCarMotorcycleModel brandCarMotorcycleModel;
	

   @Override
   public void buildModels(List<DCModelBase> modelsToCreate) {
      
   }

   @Override
   public void flushData() {
      super.flushData();
      createDataSample();
   }

   @Override
   public void cleanDataOfCreatedModels() {
      brandCarMotorcycleModel.cleanDataOfCreatedModels();
   }
      
	@Override
	public void fillData() {
		
		createDataSample();
		
		if(enableBrandSampleDataInsertionAtStartup) {
			insertData();
		}
	}

	private DCResource buildBrand(String containerUrl, String name) {
		DCResource brand = UnitTestHelper.buildResource(containerUrl, BrandCarMotorcycleModel.BRAND_MODEL_NAME, name);
		brand.setProperty("name", name);
		return brand;
	}

	private DCResource buildCar(String containerUrl, DCResource brand, String model, int year) {
		String iri = UnitTestHelper.arrayToIri((String)brand.getProperties().get("name"), model, String.valueOf(year));
		DCResource car = UnitTestHelper.buildResource(containerUrl, BrandCarMotorcycleModel.CAR_MODEL_NAME, iri);
		car.setProperty("brand", brand.getUri());
		car.setProperty("model", model);
		car.setProperty("year", year);
		return car;
	}
	
	private DCResource buildMotorcycle(String containerUrl, DCResource brand, String model, int year, int hp) {
		String iri = UnitTestHelper.arrayToIri((String)brand.getProperties().get("name"), model, String.valueOf(year), String.valueOf(hp));
		DCResource motorcycle = UnitTestHelper.buildResource(containerUrl, BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME, iri);
		motorcycle.setProperty("brand", brand.getUri());
		motorcycle.setProperty("model", model);
		motorcycle.setProperty("year", year);
		motorcycle.setProperty("hp", hp);
		return motorcycle;
	}
	
	public void createDataSample() {
				
		DCResource brandRenault = buildBrand(containerUrl, "Renault");
		DCResource brandLexus = buildBrand(containerUrl, "Lexus");
		DCResource brandHonda = buildBrand(containerUrl, "Honda");
		DCResource brandYamaha = buildBrand(containerUrl, "Yamaha");
		
		listBrands.clear();
		listCars.clear();
		listMotorcycle.clear();
		
		listBrands.add(brandRenault);
		listBrands.add(brandLexus);
		listBrands.add(brandHonda);
		listBrands.add(brandYamaha);
		
		listCars.add(buildCar(containerUrl, brandRenault, "Megane", 1996));
		listCars.add(buildCar(containerUrl, brandRenault, "Clio", 1994));
		listCars.add(buildCar(containerUrl, brandLexus, "is320", 2005));
		
		listMotorcycle.add(buildMotorcycle(containerUrl, brandYamaha, "YZF-R6", 2012, 50));
		listMotorcycle.add(buildMotorcycle(containerUrl, brandYamaha, "YZF-R6", 2012, 80));
		listMotorcycle.add(buildMotorcycle(containerUrl, brandHonda, "NC-750X", 2014, 120));
		
		mapData.put(BrandCarMotorcycleModel.BRAND_MODEL_NAME, listBrands);
		mapData.put(BrandCarMotorcycleModel.CAR_MODEL_NAME, listCars);
		mapData.put(BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME, listMotorcycle);
		
	}

	public HashMap<String, List<DCResource>> getData() {
		return mapData;
	}
	
	private void insertData() {
		
		try {
			datacoreApiImpl.postAllDataInType(listBrands, BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		} catch (WebApplicationException e) {}
		try {
		   datacoreApiImpl.postAllDataInType(listCars, BrandCarMotorcycleModel.CAR_MODEL_NAME);
		} catch (WebApplicationException e) {}
		try {
		   datacoreApiImpl.postAllDataInType(listMotorcycle, BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME);
		} catch (WebApplicationException e) {}
	
	}
	
}
