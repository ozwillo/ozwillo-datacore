package org.oasis.datacore.rest.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.RequestImpl;
import org.apache.cxf.message.MessageImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UnitTestHelper;
import org.oasis.datacore.rest.client.DatacoreClientApi;
import org.oasis.datacore.rest.client.QueryParameters;
import org.oasis.datacore.sample.BrandCarMotorcycleData;
import org.oasis.datacore.sample.BrandCarMotorcycleModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })
public class HTTPOperationsTest {
	
	@Autowired
	@Qualifier("datacoreApiClient")
	protected DatacoreClientApi api;

	@Value("${datacoreApiClient.containerUrl}")
	private String containerUrl;

	@Autowired
	private DataModelServiceImpl modelAdminService;

	@Autowired
	private MongoOperations mongoOperations;
	
	@Autowired
	private BrandCarMotorcycleData brandCarMotorcycleData;
	
	/**
	 * URL : /dc/type/${type}
	 * HTTP Method : POST
	 * HTTP Return status : 201
	 * Trying to insert brands into brand collection
	 * Expected behavior : HTTPStatus = 201 (created) and return inserted data
	 */
	@Test
	public void testPostDcTypeCreated() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		
	}
	
	/**
	 * URL : /dc/type/${type}
	 * HTTP Method : POST
	 * HTTP Return status : 400
	 * Trying to insert brands into car collection
	 * Expected behavior from API : HTTPStatus = 400 (bad request) and return null
	 */
	@Test
	public void testPostDcTypeBadRequestBadModelType() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		
		try {
			postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.CAR_MODEL_NAME);
		} catch (WebApplicationException e) {
			Assert.assertNull(postedData);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 400 but is " + httpStatus, 400 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/type/${type}
	 * HTTP Method : POST
	 * HTTP Return status : 400
	 * Trying to insert a modified brand (with a non existing property)
	 * Expected behavior from API : HTTPStatus = 400 (bad request) and return null
	 */
	@Test
	public void testPostDcTypeBadRequest() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		
		try {
			List<DCResource> listBrands = mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME);
			Assert.assertNotNull(listBrands);
			DCResource brand = listBrands.get(0);
			Assert.assertNotNull(brand);
			brand.setProperty("fault$/!#property", "test");
			postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		} catch (WebApplicationException e) {
			Assert.assertNull(postedData);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 400 but is " + httpStatus, 400 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/type/${type}
	 * HTTP Method : POST
	 * HTTP Return status : 409
	 * Trying to insert a modified brand (with an updated property and a version > version on server)
	 * Expected behavior from API : HTTPStatus = 409 (conflict) and return null
	 */
	@Test
	public void testPostDcTypeConflict() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		
		try {
			List<DCResource> listBrands = null;
			listBrands = mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME);
			Assert.assertNotNull(listBrands);
			DCResource brand = listBrands.get(0);
			Assert.assertNotNull(brand);
			brand.setProperty("name", "Toyota");
			brand.setVersion(8l);
			postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		} catch (WebApplicationException e) {
			Assert.assertNull(postedData);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 409 but is " + httpStatus, 409 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/type/${type}
	 * HTTP Method : POST
	 * HTTP Return status : 404
	 * Trying to insert a data on an non existing model
	 * Expected behavior from API : HTTPStatus = 404 (Not Found) and return null
	 */
	@Test
	public void testPostDcTypeNotFound() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		
		try {
			List<DCResource> listBrands = null;
			listBrands = mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME);
			Assert.assertNotNull(listBrands);
			postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), "nonexistingmodel");
		} catch (WebApplicationException e) {
			Assert.assertNull(postedData);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 404 but is " + httpStatus, 404 == httpStatus);
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
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.CAR_MODEL_NAME), BrandCarMotorcycleModel.CAR_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		
		List<DCResource> listCars = api.findDataInType(BrandCarMotorcycleModel.CAR_MODEL_NAME, new QueryParameters(), 0, 1);
		Assert.assertNotNull(listCars);
		DCResource car = listCars.get(0);
		car.setProperty("model", "Espace");
		List<DCResource> listDataToUpdate = new ArrayList<DCResource>();
		listDataToUpdate.add(car);
		
		List<DCResource> updatedResource = null;
		Assert.assertNull(updatedResource);
		updatedResource = api.putAllDataInType(listDataToUpdate, BrandCarMotorcycleModel.CAR_MODEL_NAME);
		
	}
	
	/**
	 * URL : /dc/type/${type}
	 * HTTP Method : PUT
	 * HTTP Return status : 400
	 * Trying to update a modified car (with a non existing property)
	 * Expected behavior from API : HTTPStatus = 400 (bad request) and return null
	 */
	@Test
	public void testPutDcTypeBadRequest() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> updatedResource = null;
		Assert.assertNull(updatedResource);
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.CAR_MODEL_NAME), BrandCarMotorcycleModel.CAR_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		List<DCResource> listCars = api.findDataInType(BrandCarMotorcycleModel.CAR_MODEL_NAME, new QueryParameters(), 0, 1);
		
		try {
			Assert.assertNotNull(listCars);		
			DCResource car = listCars.get(0);
			car.setProperty("fault$/!#property", "test");
			List<DCResource> listDataToUpdate = new ArrayList<DCResource>();
			listDataToUpdate.add(car);
			updatedResource = api.putAllDataInType(listDataToUpdate, BrandCarMotorcycleModel.CAR_MODEL_NAME);
		} catch (WebApplicationException e) {
			Assert.assertNull(updatedResource);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 400 but is " + httpStatus, 400 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/type/${type}
	 * HTTP Method : PUT
	 * HTTP Return status : 409
	 * Trying to insert a modified brand (with an updated property and a version > version on server)
	 * Expected behavior from API : HTTPStatus = 409 (conflict) and return null
	 */
	@Test
	public void testPutDcTypeConflict() {
			
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> updatedResource = null;
		Assert.assertNull(updatedResource);
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.CAR_MODEL_NAME), BrandCarMotorcycleModel.CAR_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		List<DCResource> listCars = api.findDataInType(BrandCarMotorcycleModel.CAR_MODEL_NAME, new QueryParameters(), 0, 1);
		
		try {
			Assert.assertNotNull(listCars);		
			DCResource car = listCars.get(0);
			car.setVersion(8l);
			car.setProperty("model", "updateTest");
			List<DCResource> listDataToUpdate = new ArrayList<DCResource>();
			listDataToUpdate.add(car); 
			updatedResource = api.putAllDataInType(listDataToUpdate, BrandCarMotorcycleModel.CAR_MODEL_NAME);
		} catch (WebApplicationException e) {
			Assert.assertNull(updatedResource);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 409 but is " + httpStatus, 409 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/type/${type}
	 * HTTP Method : PUT
	 * HTTP Return status : 400
	 * Trying to update a car with no version
	 * Expected behavior from API : HTTPStatus = 400 (bad request) and return null
	 */
	@Test
	public void testPutDcTypeBadRequestNoVersion() {
			
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> updatedResource = null;
		Assert.assertNull(updatedResource);
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.CAR_MODEL_NAME), BrandCarMotorcycleModel.CAR_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		List<DCResource> listCars = api.findDataInType(BrandCarMotorcycleModel.CAR_MODEL_NAME, new QueryParameters(), 0, 1);
		
		try {
			Assert.assertNotNull(listCars);		
			DCResource car = listCars.get(0);
			car.setVersion(null);
			List<DCResource> listDataToUpdate = new ArrayList<DCResource>();
			listDataToUpdate.add(car);
			updatedResource = api.putAllDataInType(listDataToUpdate, BrandCarMotorcycleModel.CAR_MODEL_NAME);
		} catch (WebApplicationException e) {
			Assert.assertNull(updatedResource);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 400 but is " + httpStatus, 400 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/type/${type}
	 * Http method : GET
	 * HTTP Return status : 200
	 * Trying to get a list of motorcycle
	 * Expected behavior from API : HTTPStatus = 200 (OK) and return the list of motorcycles
	 */
	@Test
	public void testGetDcTypeOK() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME), BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		List<DCResource> listMotorcycles = api.findDataInType(BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME, new QueryParameters(), 0, 10);
		Assert.assertNotNull(listMotorcycles);
		
	}
	
	/**
	 * URL : /dc/type/${type}
	 * Http method : GET
	 * HTTP Return status : 400
	 * Trying to get a list of motorcycle with a query on fields that don't exist
	 * Expected behavior from API : HTTPStatus = 400 (bad request) and return null
	 */
	@Test
	public void testGetDcTypeBadRequest() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME), BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		List<DCResource> listMotorcycles = null;
		try {
			listMotorcycles = api.findDataInType(BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME,
			      new QueryParameters().add("test", "true").add("noquery", "0"), 0, 10);
		} catch (WebApplicationException e) {
			Assert.assertNull(listMotorcycles);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 400 but is " + httpStatus, 400 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/type/${type}
	 * Http method : GET
	 * HTTP Return status : 404
	 * Trying to get a list of concept cars (model don't exist)
	 * Expected behavior from API : HTTPStatus = 404 (not found) and return null
	 */
	@Test
	public void testGetDcTypeNotFound() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> listConceptCars = null;
		try {
			listConceptCars = api.findDataInType("ConceptCars", new QueryParameters(), 0, 10);
		} catch (WebApplicationException e) {
			Assert.assertNull(listConceptCars);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 404 but is " + httpStatus, 404 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/
	 * HTTP Method : POST
	 * HTTP Return status : 201
	 * Trying to insert brands and car
	 * Expected behavior : HTTPStatus = 201 (created) and return inserted data
	 */
	@Test
	public void testPostDcCreated() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		List<DCResource> listData = new ArrayList<DCResource>();
		listData.addAll(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME));
		listData.addAll(mapData.get(BrandCarMotorcycleModel.CAR_MODEL_NAME));
		
		postedData = api.postAllData(listData);
		Assert.assertNotNull(postedData);
		
	}
	
	/**
	 * URL : /dc/
	 * HTTP Method : POST
	 * HTTP Return status : 400
	 * Trying to insert a list of resources with a bad URI on one resource
	 * Expected behavior from API : HTTPStatus = 400 (bad request) and return null
	 */
	@Test
	public void testPostDcBadRequestBadURI() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		List<DCResource> listData = new ArrayList<DCResource>();
		listData.addAll(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME));
		listData.addAll(mapData.get(BrandCarMotorcycleModel.CAR_MODEL_NAME));
		Assert.assertNotNull(listData);
		DCResource resource = listData.get(0);
		Assert.assertNotNull(resource);
		resource.setUri("\\o$!$");
		try {
			postedData = api.postAllData(listData);
		} catch (WebApplicationException e) {
			Assert.assertNull(postedData);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 400 but is " + httpStatus, 400 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/
	 * HTTP Method : POST
	 * HTTP Return status : 400
	 * Trying to insert a modified resource (with a non existing property)
	 * Expected behavior from API : HTTPStatus = 400 (bad request) and return null
	 */
	@Test
	public void testPostDcBadRequest() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		
		try {
			List<DCResource> listData = new ArrayList<DCResource>();
			listData.addAll(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME));
			listData.addAll(mapData.get(BrandCarMotorcycleModel.CAR_MODEL_NAME));
			Assert.assertNotNull(listData);
			DCResource resource = listData.get(0);
			Assert.assertNotNull(resource);
			resource.setProperty("fault$/!#property", "test");
			postedData = api.postAllData(listData);
		} catch (WebApplicationException e) {
			Assert.assertNull(postedData);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 400 but is " + httpStatus, 400 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/
	 * HTTP Method : POST
	 * HTTP Return status : 409
	 * Trying to insert a modified resource (with an updated property and a version > version on server)
	 * Expected behavior from API : HTTPStatus = 409 (conflict) and return null
	 */
	@Test
	public void testPostDcConflict() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		
		try {
			List<DCResource> listData = new ArrayList<DCResource>();
			listData.addAll(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME));
			listData.addAll(mapData.get(BrandCarMotorcycleModel.CAR_MODEL_NAME));
			Assert.assertNotNull(listData);
			postedData = api.postAllData(listData);
			postedData = null;
			Assert.assertNull(postedData);
			DCResource resource = listData.get(0);
			Assert.assertNotNull(resource);
			resource.setProperty("name", "Toyota");
			resource.setVersion(8l);
			List<DCResource> newData = new ArrayList<DCResource>();
			newData.add(resource);
			postedData = api.postAllData(newData);
		} catch (WebApplicationException e) {
			Assert.assertNull(postedData);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 409 but is " + httpStatus, 409 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/
	 * HTTP Method : POST
	 * HTTP Return status : 404
	 * Trying to insert a data on an non existing model
	 * Expected behavior from API : HTTPStatus = 404 (Not Found) and return null
	 */
	@Test
	public void testPostDcNotFound() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		
		try {
			List<DCResource> listData = new ArrayList<DCResource>();
			listData.addAll(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME));
			listData.addAll(mapData.get(BrandCarMotorcycleModel.CAR_MODEL_NAME));
			Assert.assertNotNull(listData);
			postedData = api.postAllData(listData);
		} catch (WebApplicationException e) {
			Assert.assertNull(postedData);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 404 but is " + httpStatus, 404 == httpStatus);
		}
		
	}

	/**
	 * URL : /dc/
	 * HTTP Method : PUT
	 * HTTP Return status : 201
	 * Trying to update a motorcycle
	 * Expected behavior : HTTPStatus = 201 (created) and return inserted data
	 */
	@Test
	public void testPutDcCreated() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME), BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		
		List<DCResource> listMotorcycles = api.findDataInType(BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME, new QueryParameters(), 0, 1);
		Assert.assertNotNull(listMotorcycles);
		DCResource motorcycle = listMotorcycles.get(0);
		motorcycle.setProperty("hp", new Integer(200));
		List<DCResource> listDataToUpdate = new ArrayList<DCResource>();
		listDataToUpdate.add(motorcycle);
		
		List<DCResource> updatedResource = null;
		Assert.assertNull(updatedResource);
		updatedResource = api.putAllData(listDataToUpdate);
		
	}
	
	/**
	 * URL : /dc/
	 * HTTP Method : PUT
	 * HTTP Return status : 400
	 * Trying to update a modified motorcycle (with a non existing property)
	 * Expected behavior from API : HTTPStatus = 400 (Bad Request) and return null
	 */
	@Test
	public void testPutDcBadRequest() {
			
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> updatedResource = null;
		Assert.assertNull(updatedResource);
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME), BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		List<DCResource> listMotorcycles = api.findDataInType(BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME, new QueryParameters(), 0, 1);
		
		try {
			Assert.assertNotNull(listMotorcycles);		
			DCResource motorcycle = listMotorcycles.get(0);
			motorcycle.setProperty("fault$/!#property", "test");
			List<DCResource> listDataToUpdate = new ArrayList<DCResource>();
			listDataToUpdate.add(motorcycle);
			updatedResource = api.putAllData(listDataToUpdate);
		} catch (WebApplicationException e) {
			Assert.assertNull(updatedResource);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 400 but is " + httpStatus, 400 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/
	 * HTTP Method : PUT
	 * HTTP Return status : 409
	 * Trying to insert a modified motorcycle (with an updated property and a version > version on server)
	 * Expected behavior from API : HTTPStatus = 409 (conflict) and return null
	 */
	@Test
	public void testPutDcConflict() {
			
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> updatedResource = null;
		Assert.assertNull(updatedResource);
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME), BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		List<DCResource> listMotorcycles = api.findDataInType(BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME, new QueryParameters(), 0, 1);
		
		try {
			Assert.assertNotNull(listMotorcycles);		
			DCResource motorcycle = listMotorcycles.get(0);
			motorcycle.setVersion(8l);
			motorcycle.setProperty("model", "updateTest");
			List<DCResource> listDataToUpdate = new ArrayList<DCResource>();
			listDataToUpdate.add(motorcycle); 
			updatedResource = api.putAllData(listDataToUpdate);
		} catch (WebApplicationException e) {
			Assert.assertNull(updatedResource);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 409 but is " + httpStatus, 409 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/
	 * HTTP Method : PUT
	 * HTTP Return status : 400
	 * Trying to update a car with no version
	 * Expected behavior from API : HTTPStatus = 400 (bad request) and return null
	 */
	@Test
	public void testPutDcBadRequestNoVersion() {
			
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> updatedResource = null;
		Assert.assertNull(updatedResource);
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME), BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		List<DCResource> listMotorcycles = api.findDataInType(BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME, new QueryParameters(), 0, 1);
		
		try {
			Assert.assertNotNull(listMotorcycles);		
			DCResource car = listMotorcycles.get(0);
			car.setVersion(null);
			List<DCResource> listDataToUpdate = new ArrayList<DCResource>();
			listDataToUpdate.add(car);
			updatedResource = api.putAllData(listDataToUpdate);
		} catch (WebApplicationException e) {
			Assert.assertNull(updatedResource);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 400 but is " + httpStatus, 400 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/
	 * Http method : GET
	 * HTTP Return status : 200
	 * Trying to get a list of resources
	 * Expected behavior from API : HTTPStatus = 200 (OK) and return the list of motorcycles
	 */
	@Test
	public void testGetDcOK() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME), BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		List<DCResource> listMotorcycles = api.findData(new QueryParameters().add("year", "2014"), 0, 10);
		Assert.assertNotNull(listMotorcycles);
		
	}
	
	/**
	 * URL : /dc/
	 * Http method : GET
	 * HTTP Return status : 400
	 * Trying to get a list of motorcycle with a query on fields that don't exist
	 * Expected behavior from API : HTTPStatus = 400 (bad request) and return null
	 */
	@Test
	public void testGetDcBadRequest() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME), BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		List<DCResource> listMotorcycles = null;
		try {
			listMotorcycles = api.findData(new QueryParameters().add("test", "true").add("noquery", "0"), 0, 10);
		} catch (WebApplicationException e) {
			Assert.assertNull(listMotorcycles);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 400 but is " + httpStatus, 400 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/
	 * Http method : GET
	 * HTTP Return status : 404
	 * Trying to get a list of concept cars (model don't exist)
	 * Expected behavior from API : HTTPStatus = 404 (not found) and return null
	 */
	@Test
	public void testGetDcNotFound() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> listConceptCars = null;
		try {
			listConceptCars = api.findData(new QueryParameters().add("model", "conceptcars"), 0, 10);
		} catch (WebApplicationException e) {
			Assert.assertNull(listConceptCars);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 404 but is " + httpStatus, 404 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/type/${type}/${iri}
	 * HTTP Method : PUT
	 * HTTP Return status : 201
	 * Trying to update a car
	 * Expected behavior : HTTPStatus = 201 (created) and return inserted data
	 */
	@Test
	public void testPutDcTypeIriCreated() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		List<DCResource> postedData = null;
		Assert.assertNull(postedData);
		
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.CAR_MODEL_NAME), BrandCarMotorcycleModel.CAR_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		
		List<DCResource> listCars = api.findDataInType(BrandCarMotorcycleModel.CAR_MODEL_NAME, new QueryParameters(), 0, 1);
		Assert.assertNotNull(listCars);
		DCResource car = listCars.get(0);
		car.setProperty("model", "Espace");
		
		DCResource updatedResource = null;
		Assert.assertNull(updatedResource);
		updatedResource = api.putDataInType(car, BrandCarMotorcycleModel.CAR_MODEL_NAME, car.getUri());
		
	}
	
	/**
	 * URL : /dc/type/${type}/${iri}
	 * HTTP Method : PUT
	 * HTTP Return status : 400
	 * Trying to update a modified car (with a non existing property)
	 * Expected behavior from API : HTTPStatus = 400 (bad request) and return null
	 */
	@Test
	public void testPutDcTypeIriBadRequest() {
			
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		DCResource updatedResource = null;
		Assert.assertNull(updatedResource);
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.CAR_MODEL_NAME), BrandCarMotorcycleModel.CAR_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		List<DCResource> listCars = api.findDataInType(BrandCarMotorcycleModel.CAR_MODEL_NAME, new QueryParameters(), 0, 1);
		
		try {
			Assert.assertNotNull(listCars);		
			DCResource car = listCars.get(0);
			car.setProperty("fault$/!#property", "test");
			updatedResource = api.putDataInType(car, BrandCarMotorcycleModel.CAR_MODEL_NAME, car.getUri());
		} catch (WebApplicationException e) {
			Assert.assertNull(updatedResource);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 400 but is " + httpStatus, 400 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/type/${type}/${iri}
	 * HTTP Method : PUT
	 * HTTP Return status : 409
	 * Trying to insert a modified brand (with an updated property and a version > version on server)
	 * Expected behavior from API : HTTPStatus = 409 (conflict) and return null
	 */
	@Test
	public void testPutDcTypeIriConflict() {
			
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		DCResource updatedResource = null;
		Assert.assertNull(updatedResource);
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.CAR_MODEL_NAME), BrandCarMotorcycleModel.CAR_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		List<DCResource> listCars = api.findDataInType(BrandCarMotorcycleModel.CAR_MODEL_NAME, new QueryParameters(), 0, 1);
		
		try {
			Assert.assertNotNull(listCars);		
			DCResource car = listCars.get(0);
			car.setVersion(8l);
			car.setProperty("model", "updateTest");
			updatedResource = api.putDataInType(car, BrandCarMotorcycleModel.CAR_MODEL_NAME, car.getUri());
		} catch (WebApplicationException e) {
			Assert.assertNull(updatedResource);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 409 but is " + httpStatus, 409 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/type/${type}/${iri}
	 * HTTP Method : PUT
	 * HTTP Return status : 400
	 * Trying to update a car with no version
	 * Expected behavior from API : HTTPStatus = 400 (bad request) and return null
	 */
	@Test
	public void testPutDcTypeIriBadRequestNoVersion() {
			
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		DCResource updatedResource = null;
		Assert.assertNull(updatedResource);
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.CAR_MODEL_NAME), BrandCarMotorcycleModel.CAR_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		List<DCResource> listCars = api.findDataInType(BrandCarMotorcycleModel.CAR_MODEL_NAME, new QueryParameters(), 0, 1);
		
		try {
			Assert.assertNotNull(listCars);		
			DCResource car = listCars.get(0);
			car.setVersion(null);
			updatedResource = api.putDataInType(car, BrandCarMotorcycleModel.CAR_MODEL_NAME, car.getUri());
		} catch (WebApplicationException e) {
			Assert.assertNull(updatedResource);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 400 but is " + httpStatus, 400 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/type/${type}/${iri}
	 * Http method : GET
	 * HTTP Return status : 200
	 * Trying to get a defined car
	 * Expected behavior from API : HTTPStatus = 200 (OK) and a DCResource
	 */
	@Test
	public void testGetDcTypeIriOK() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		DCResource updatedResource = null;
		Assert.assertNull(updatedResource);
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.CAR_MODEL_NAME), BrandCarMotorcycleModel.CAR_MODEL_NAME);
		Assert.assertNotNull(postedData);		
		DCResource resource = api.getData(BrandCarMotorcycleModel.CAR_MODEL_NAME, "Renault/Megane/1996", new RequestImpl(new MessageImpl()));
		Assert.assertNotNull(resource);
		
	}
	
	/**
	 * URL : /dc/type/${type}/${iri}
	 * Http method : GET
	 * HTTP Return status : 404
	 * Trying to get a concept car (non existing model)
	 * Expected behavior from API : HTTPStatus = 404 (Not Found) and return null
	 */
	@Test
	public void testGetDcTypeIriModelNotFound() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		DCResource updatedResource = null;
		Assert.assertNull(updatedResource);
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.CAR_MODEL_NAME), BrandCarMotorcycleModel.CAR_MODEL_NAME);
		Assert.assertNotNull(postedData);	
		DCResource resource = null;
		Assert.assertNull(resource);
		try {
			resource = api.getData("conceptcars", "Renault/Megane/1996", new RequestImpl(new MessageImpl()));
		} catch (WebApplicationException e) {
			Assert.assertNull(resource);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 404 but is " + httpStatus, 404 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/type/${type}/${iri}
	 * Http method : GET
	 * HTTP Return status : 404
	 * Trying to get a car that does not exist
	 * Expected behavior from API : HTTPStatus = 404 (Not Found) and return null
	 */
	@Test
	public void testGetDcTypeIriResourceNotFound() {
		
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		DCResource updatedResource = null;
		Assert.assertNull(updatedResource);
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.CAR_MODEL_NAME), BrandCarMotorcycleModel.CAR_MODEL_NAME);
		Assert.assertNotNull(postedData);	
		DCResource resource = null;
		try {
			Assert.assertNull(resource);
			resource = api.getData(BrandCarMotorcycleModel.CAR_MODEL_NAME, "Renault/Megane/2007", new RequestImpl(new MessageImpl()));
			Assert.assertNull(resource);
		} catch (WebApplicationException e) {
			Assert.assertNull(resource);
			int httpStatus = UnitTestHelper.getHttpStatusFromWAE(e);
			Assert.assertTrue("HTTP status should be 404 but is " + httpStatus, 404 == httpStatus);
		}
		
	}
	
	/**
	 * URL : /dc/type/${type}/${iri}
	 * Http method : DELETE
	 * HTTP Return status : 204
	 * Trying to get a car that does not exist
	 * Expected behavior from API : HTTPStatus = 204 (No Content) and return null
	 * Why ignore ? : even if we cache the data we can't delete it (probably a client problem)
	 * TODO MDU patch
	 */
	//@Ignore
	@Test
	public void testDeleteDcTypeIri() {
		 
		Map<String, List<DCResource>> mapData = brandCarMotorcycleData.getData();
		Assert.assertTrue(mapData != null && !mapData.isEmpty());
		DCResource updatedResource = null;
		Assert.assertNull(updatedResource);
		List<DCResource> postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.BRAND_MODEL_NAME), BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		Assert.assertNotNull(postedData);
		postedData = null;
		postedData = api.postAllDataInType(mapData.get(BrandCarMotorcycleModel.CAR_MODEL_NAME), BrandCarMotorcycleModel.CAR_MODEL_NAME);
		Assert.assertNotNull(postedData);
		List<DCResource> listCars = api.findDataInType(BrandCarMotorcycleModel.CAR_MODEL_NAME, new QueryParameters().add("model", "Megane").add("year", "1996"), 0, 1);
		Assert.assertNotNull(listCars);
		Assert.assertTrue(!listCars.isEmpty());
		try {
			api.deleteData(BrandCarMotorcycleModel.CAR_MODEL_NAME, "Renault/Megane/1996", new HttpHeadersImpl(new MessageImpl()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Before
	public void flushData() {
		truncateModel(BrandCarMotorcycleModel.BRAND_MODEL_NAME);
		truncateModel(BrandCarMotorcycleModel.CAR_MODEL_NAME);
		truncateModel(BrandCarMotorcycleModel.MOTORCYCLE_MODEL_NAME);
		brandCarMotorcycleData.createDataSample();
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