package org.oasis.datacore.rest.server.sample.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.server.common.DatacoreTestUtils;
import org.oasis.datacore.rest.server.sample.model.BrandCarMotorcycleModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BrandCarMotorcycleData {
	
	private List<DCResource> listBrands;
	private List<DCResource> listCars;
	private List<DCResource> listMotorcycle;
	private Map<String, List<DCResource>> mapData;
	
	@Value("${datacoreApiClient.containerUrl}")
	private String containerUrl;
	
	@PostConstruct
	public void init() {
		listBrands = new ArrayList<DCResource>();
		listCars = new ArrayList<DCResource>();
		listMotorcycle = new ArrayList<DCResource>();
		mapData = new HashMap<String, List<DCResource>>();
	}

	private DCResource buildBrand(String containerUrl, String name) {
		DCResource brand = DatacoreTestUtils.buildResource(containerUrl, BrandCarMotorcycleModel.BRAND_MODEL_NAME, name);
		brand.setProperty("name", name);
		return brand;
	}

	private DCResource buildCar(String containerUrl, DCResource brand, String model, int year) {
		String iri = DatacoreTestUtils.arrayToIri((String)brand.getProperties().get("name"), model, String.valueOf(year));
		DCResource car = DatacoreTestUtils.buildResource(containerUrl, BrandCarMotorcycleModel.CAR_MODEL_NAME, iri);
		car.setProperty("brand", brand);
		car.setProperty("model", model);
		car.setProperty("year", year);
		return car;
	}
	
	private DCResource buildMotorcycle(String containerUrl, DCResource brand, String model, int year, int hp) {
		String iri = DatacoreTestUtils.arrayToIri((String)brand.getProperties().get("name"), model, String.valueOf(year), String.valueOf(hp));
		DCResource motorcycle = DatacoreTestUtils.buildResource(containerUrl, BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME, iri);
		motorcycle.setProperty("brand", brand);
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

	public Map<String, List<DCResource>> getData() {
		return mapData;
	}
	
}
