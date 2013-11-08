package org.oasis.datacore.rest.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.apache.cxf.common.util.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.sample.BrandCarMotorcycleSample;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.client.DatacoreClientApi;
import org.oasis.datacore.rest.server.common.DatacoreTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })
public class DatacoreApiServerFullTest {
	
	@Autowired
	@Qualifier("datacoreApiClient")
	protected DatacoreClientApi api;

	@Value("${datacoreApiClient.containerUrl}")
	private String containerUrl;

	@Autowired
	private DataModelServiceImpl modelAdminService;

	@Autowired
	private MongoOperations mongoOperations;
	
	/**
	 * URL : /dc/type/${type}
	 * HTTP Method : POST
	 * HTTP Return status : 201
	 * Trying to insert brands into brand collection
	 * Expected behavior : HTTPStatus = 201 (created) and return inserted data
	 */
	@Test
	public void testPostDcTypeCreated() {
		
		Map<String, List<DCResource>> mapData = createDataSample();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleSample.BRAND_MODEL_NAME), BrandCarMotorcycleSample.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		
	}
	
	/**
	 * URL : /dc/type/${type}
	 * HTTP Method : POST
	 * HTTP Return status : 400
	 * Trying to insert brands into car collection
	 * Excepted behavior from API : HTTPStatus = 400 (bad request) and return null
	 */
	@Test
	public void testPostDcTypeBadRequest() {
		
		Map<String, List<DCResource>> mapData = createDataSample();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		
		try {
			postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleSample.BRAND_MODEL_NAME), BrandCarMotorcycleSample.CAR_MODEL_NAME);
		} catch (WebApplicationException e) {
			Assert.assertNull(postedData);
			int httpStatus = DatacoreTestUtils.getHttpStatusFromWAE(e);
			Assert.assertTrue(400 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/type/${type}
	 * HTTP Method : POST
	 * HTTP Return status : 500
	 * Trying to insert a modified brand (with a non existing property)
	 * Excepted behavior from API : HTTPStatus = 500 (Internal Server Error) and return null
	 */
	@Test
	public void testPostDcTypeInternalServerError() {
		
		Map<String, List<DCResource>> mapData = createDataSample();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		
		try {
			List<DCResource> listBrands = mapData.get(BrandCarMotorcycleSample.BRAND_MODEL_NAME);
			Assert.assertNotNull(listBrands);
			DCResource brand = listBrands.get(0);
			Assert.assertNotNull(brand);
			brand.setProperty("fault$/!#property", "test");
			postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleSample.BRAND_MODEL_NAME), BrandCarMotorcycleSample.BRAND_MODEL_NAME);
		} catch (WebApplicationException e) {
			Assert.assertNull(postedData);
			int httpStatus = DatacoreTestUtils.getHttpStatusFromWAE(e);
			Assert.assertTrue(500 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/type/${type}
	 * HTTP Method : POST
	 * HTTP Return status : 409
	 * Trying to insert a modified brand (with an updated property and a version > version on server)
	 * Excepted behavior from API : HTTPStatus = 409 (conflict) and return null
	 */
	@Test
	public void testPostDcTypeConflict() {
		
		Map<String, List<DCResource>> mapData = createDataSample();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		
		try {
			List<DCResource> listBrands = null;
			listBrands = mapData.get(BrandCarMotorcycleSample.BRAND_MODEL_NAME);
			Assert.assertNotNull(listBrands);
			DCResource brand = listBrands.get(0);
			Assert.assertNotNull(brand);
			brand.setProperty("name", "Toyota");
			brand.setVersion(8l);
			postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleSample.BRAND_MODEL_NAME), BrandCarMotorcycleSample.BRAND_MODEL_NAME);
		} catch (WebApplicationException e) {
			Assert.assertNull(postedData);
			int httpStatus = DatacoreTestUtils.getHttpStatusFromWAE(e);
			Assert.assertTrue(409 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/type/${type}
	 * HTTP Method : POST
	 * HTTP Return status : 404
	 * Trying to insert a data on an non existing model
	 * Excepted behavior from API : HTTPStatus = 404 (Not Found) and return null
	 */
	@Test
	public void testPostDcTypeNotFound() {
		
		Map<String, List<DCResource>> mapData = createDataSample();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		
		try {
			List<DCResource> listBrands = null;
			listBrands = mapData.get(BrandCarMotorcycleSample.BRAND_MODEL_NAME);
			Assert.assertNotNull(listBrands);
			postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleSample.BRAND_MODEL_NAME), "nonexistingmodel");
		} catch (WebApplicationException e) {
			Assert.assertNull(postedData);
			int httpStatus = DatacoreTestUtils.getHttpStatusFromWAE(e);
			Assert.assertTrue(404 == httpStatus);
		}
		
	}

	/**
	 * URL : /dc/type/${type}
	 * HTTP Method : PUT
	 * HTTP Return status : 201
	 * Trying to update a car
	 * Expected behavior : HTTPStatus = 201 (created) and return inserted data
	 */
	@Test
	public void testPutDcTypeCreated() {
		
		Map<String, List<DCResource>> mapData = createDataSample();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleSample.BRAND_MODEL_NAME), BrandCarMotorcycleSample.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleSample.CAR_MODEL_NAME), BrandCarMotorcycleSample.CAR_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		
		List<DCResource> listCars = api.findDataInType(BrandCarMotorcycleSample.CAR_MODEL_NAME, "", 0, 1);
		Assert.assertNotNull(listCars);
		DCResource car = listCars.get(0);
		car.setProperty("model", "Espace");
		List<DCResource> listDataToUpdate = new ArrayList<DCResource>();
		listDataToUpdate.add(car);
		
		List<DCResource> updatedResource = null;
		Assert.assertNull(updatedResource);
		updatedResource = api.putAllDataInType(listDataToUpdate, BrandCarMotorcycleSample.CAR_MODEL_NAME);
		
	}
	
	/**
	 * URL : /dc/type/${type}
	 * HTTP Method : PUT
	 * HTTP Return status : 500
	 * Trying to update a modified car (with a non existing property)
	 * Excepted behavior from API : HTTPStatus = 500 (Internal Server Error) and return null
	 */
	@Test
	public void testPutDcTypeInternalServerError() {
			
		Map<String, List<DCResource>> mapData = createDataSample();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> updatedResource = null;
		Assert.assertNull(updatedResource);
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleSample.BRAND_MODEL_NAME), BrandCarMotorcycleSample.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleSample.CAR_MODEL_NAME), BrandCarMotorcycleSample.CAR_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		List<DCResource> listCars = api.findDataInType(BrandCarMotorcycleSample.CAR_MODEL_NAME, "", 0, 1);
		
		try {
			Assert.assertNotNull(listCars);		
			DCResource car = listCars.get(0);
			car.setProperty("fault$/!#property", "test");
			List<DCResource> listDataToUpdate = new ArrayList<DCResource>();
			listDataToUpdate.add(car);
			updatedResource = api.putAllDataInType(listDataToUpdate, BrandCarMotorcycleSample.CAR_MODEL_NAME);
		} catch (WebApplicationException e) {
			Assert.assertNull(updatedResource);
			int httpStatus = DatacoreTestUtils.getHttpStatusFromWAE(e);
			Assert.assertTrue(500 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/type/${type}
	 * HTTP Method : PUT
	 * HTTP Return status : 409
	 * Trying to insert a modified brand (with an updated property and a version > version on server)
	 * Excepted behavior from API : HTTPStatus = 409 (conflict) and return null
	 */
	@Test
	public void testPutDcTypeConflict() {
			
		Map<String, List<DCResource>> mapData = createDataSample();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> updatedResource = null;
		Assert.assertNull(updatedResource);
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleSample.BRAND_MODEL_NAME), BrandCarMotorcycleSample.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleSample.CAR_MODEL_NAME), BrandCarMotorcycleSample.CAR_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		List<DCResource> listCars = api.findDataInType(BrandCarMotorcycleSample.CAR_MODEL_NAME, "", 0, 1);
		
		try {
			Assert.assertNotNull(listCars);		
			DCResource car = listCars.get(0);
			car.setVersion(8l);
			car.setProperty("model", "updateTest");
			List<DCResource> listDataToUpdate = new ArrayList<DCResource>();
			listDataToUpdate.add(car);
			updatedResource = api.putAllDataInType(listDataToUpdate, BrandCarMotorcycleSample.CAR_MODEL_NAME);
		} catch (WebApplicationException e) {
			Assert.assertNull(updatedResource);
			int httpStatus = DatacoreTestUtils.getHttpStatusFromWAE(e);
			Assert.assertTrue(409 == httpStatus);
		}
		
	}
	
	
	/**
	 * URL : /dc/type/${type}
	 * Http method : GET
	 */
	@Test
	public void testGetDcType() {
//		api.findDataInType(type, queryParams, start, limit);
	}
	
	/**
	 * URL : /dc/
	 * Http method : POST
	 */
	@Test
	public void testPostDc() {
//		api.postAllData(dcDatas);
	}
	
	/**
	 * URL : /dc/
	 * Http method : PUT
	 */	
	@Test
	public void testPutDc() {
//		api.putAllData(dcDatas);
	}
	
	/**
	 * URL : /dc/
	 * Http method : GET
	 */
	@Test
	public void testGetDc() {
//		api.findData(queryParams, start, limit);
	}
	
	/**
	 * URL : /dc/type/${type}/${iri}
	 * Http method : PUT
	 */
	@Test
	public void testPutDcTypeIri() {
//		api.putDataInType(dcData, modelType, iri);
	}
	
	/**
	 * URL : /dc/type/${type}/${iri}
	 * Http method : GET
	 */
	@Test
	public void testGetDcTypeIri() {
//		api.getData(modelType, iri, request);
	}
	
	/**
	 * URL : /dc/type/${type}/${iri}
	 * Http method : DELETE
	 */
	@Test
	public void testDeleteDcTypeIri() {
//		api.deleteData(modelType, iri, httpHeaders);
	}

	@Before
	public void flushData() {
		truncateModel(BrandCarMotorcycleSample.BRAND_MODEL_NAME);
		truncateModel(BrandCarMotorcycleSample.CAR_MODEL_NAME);
		truncateModel(BrandCarMotorcycleSample.MOTORCYCLE_MODEL_NAME);
	}
	
	private DCResource buildBrand(String name) {
		DCResource brand = DatacoreTestUtils.buildResource(containerUrl, BrandCarMotorcycleSample.BRAND_MODEL_NAME, name);
		brand.setProperty("name", name);
		return brand;
	}

	private DCResource buildCar(DCResource brand, String model, int year) {
		String iri = DatacoreTestUtils.arrayToIri((String)brand.getProperties().get("name"), model, String.valueOf(year));
		DCResource car = DatacoreTestUtils.buildResource(containerUrl, BrandCarMotorcycleSample.CAR_MODEL_NAME, iri);
		car.setProperty("brand", brand);
		car.setProperty("model", model);
		car.setProperty("year", year);
		return car;
	}
	
	private DCResource buildMotorcycle(DCResource brand, String model, int year, int hp) {
		String iri = DatacoreTestUtils.arrayToIri((String)brand.getProperties().get("name"), model, String.valueOf(year), String.valueOf(hp));
		DCResource motorcycle = DatacoreTestUtils.buildResource(containerUrl, BrandCarMotorcycleSample.MOTORCYCLE_MODEL_NAME, iri);
		motorcycle.setProperty("brand", brand);
		motorcycle.setProperty("model", model);
		motorcycle.setProperty("year", year);
		motorcycle.setProperty("hp", hp);
		return motorcycle;
	}

	private Map<String, List<DCResource>> createDataSample() {
		
		Map<String, List<DCResource>> mapData = new HashMap<String, List<DCResource>>();
		List<DCResource> listBrands = new ArrayList<DCResource>();
		List<DCResource> listCars = new ArrayList<DCResource>();
		List<DCResource> listMotorcycle = new ArrayList<DCResource>();

		DCResource brandRenault = buildBrand("Renault");
		DCResource brandLexus = buildBrand("Lexus");
		DCResource brandHonda = buildBrand("Honda");
		DCResource brandYamaha = buildBrand("Yamaha");
		
		listBrands.add(brandRenault);
		listBrands.add(brandLexus);
		listBrands.add(brandHonda);
		listBrands.add(brandYamaha);
		
		listCars.add(buildCar(brandRenault, "Megane", 1996));
		listCars.add(buildCar(brandRenault, "Clio", 1994));
		listCars.add(buildCar(brandLexus, "is320", 2005));
		
		listMotorcycle.add(buildMotorcycle(brandYamaha, "YZF-R6", 2012, 50));
		listMotorcycle.add(buildMotorcycle(brandYamaha, "YZF-R6", 2012, 80));
		listMotorcycle.add(buildMotorcycle(brandHonda, "NC-750X", 2014, 120));
		
		mapData.put(BrandCarMotorcycleSample.BRAND_MODEL_NAME, listBrands);
		mapData.put(BrandCarMotorcycleSample.CAR_MODEL_NAME, listCars);
		mapData.put(BrandCarMotorcycleSample.MOTORCYCLE_MODEL_NAME, listMotorcycle);
		
		return mapData;
		
	}

	private void truncateModel(String type) {
		if (type != null && !StringUtils.isEmpty(type)) {
			DCModel dcModel = modelAdminService.getModel(type);
			if (dcModel != null) {
				mongoOperations.remove(new Query(), dcModel.getCollectionName());
			}
		} else {
			System.out.println("Model type is null or empty, cannot truncate model");
		}
	}

}