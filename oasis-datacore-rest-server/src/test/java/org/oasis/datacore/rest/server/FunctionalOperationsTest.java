package org.oasis.datacore.rest.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.cxf.common.util.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.client.DatacoreClientApi;
import org.oasis.datacore.rest.client.QueryParameters;
import org.oasis.datacore.sample.MarkaInvestData;
import org.oasis.datacore.sample.MarkaInvestModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Ordering;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })
public class FunctionalOperationsTest {
	
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
	private MarkaInvestData markaInvestData;
		
	@Before
	public void flushData() {
		truncateModel(MarkaInvestModel.CITY_MODEL_NAME);
		truncateModel(MarkaInvestModel.COMPANY_MODEL_NAME);
		truncateModel(MarkaInvestModel.COST_TYPE_MODEL_NAME);
		truncateModel(MarkaInvestModel.COUNTRY_MODEL_NAME);
		truncateModel(MarkaInvestModel.FIELD_MODEL_NAME);
		truncateModel(MarkaInvestModel.INVESTMENT_ASSISTANCE_REQUEST_MODEL_NAME);
		truncateModel(MarkaInvestModel.INVESTOR_MODEL_NAME);
		truncateModel(MarkaInvestModel.INVESTOR_TYPE_MODEL_NAME);
		truncateModel(MarkaInvestModel.SECTOR_MODEL_NAME);
		truncateModel(MarkaInvestModel.USER_MODEL_NAME);
		markaInvestData.createDataSample();
		markaInvestData.insertData();
	}
	
	@Test
	public void testEquals() {
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
		
		// Equal operator : =
		queryParameters = new QueryParameters();
		queryParameters.add("name", "=Societe Generale");
		listResource = api.findDataInType(MarkaInvestModel.COMPANY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		
		queryParameters = null;
		listResource = null;
		Assert.assertNull(queryParameters);
		Assert.assertNull(listResource);
		
		// Equal operator : ==
		queryParameters = new QueryParameters();
		queryParameters.add("lastAnnualRevenue", "==956210000");
		listResource = api.findDataInType(MarkaInvestModel.COMPANY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		
	}
	
	@Test
	public void testSortDesc() {
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
		
		queryParameters = new QueryParameters();
		queryParameters.add("name", "+");
		listResource = api.findDataInType(MarkaInvestModel.FIELD_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		Collection<String> listNames = new ArrayList<String>();
		Assert.assertTrue(listNames.isEmpty());
		for(DCResource resource : listResource) {
			Assert.assertNotNull(resource);
			Assert.assertTrue(listNames.add(String.valueOf(resource.getProperties().get("name"))));
		}
		Assert.assertTrue("Resource list is not sorted correctly, should be DESC but is something else", Ordering.natural().isOrdered(listNames));
		
	}
	
	@Test
	public void testSortAsc() {
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
		
		queryParameters = new QueryParameters();
		queryParameters.add("name", "-");
		listResource = api.findDataInType(MarkaInvestModel.FIELD_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		Collection<String> listNames = new ArrayList<String>();
		Assert.assertTrue(listNames.isEmpty());
		for(DCResource resource : listResource) {
			Assert.assertNotNull(resource);
			Assert.assertTrue(listNames.add(String.valueOf(resource.getProperties().get("name"))));
		}
		Assert.assertTrue("Resource list is not sorted correctly, should be ASC but is something else", Ordering.natural().reverse().isOrdered(listNames));
		
	}
	
	@Test
	public void testGreaterThanAndSortAsc() {
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
		
		queryParameters = new QueryParameters();
		queryParameters.add("id", ">0-");
		listResource = api.findDataInType(MarkaInvestModel.COUNTRY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		Collection<String> listIds = new ArrayList<String>();
		Assert.assertTrue(listIds.isEmpty());
		for(DCResource resource : listResource) {
			Assert.assertNotNull(resource);
			Assert.assertTrue(listIds.add(String.valueOf(resource.getProperties().get("id"))));
		}
		Assert.assertTrue("Resource list is not sorted correctly, should be ASC but is something else", Ordering.natural().reverse().isOrdered(listIds));
		
	}

	@Test
	public void testNotEquals() {
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
		
		// Not equal operator : <>
		queryParameters = new QueryParameters();
		queryParameters.add("name", "<>France");
		listResource = api.findDataInType(MarkaInvestModel.COUNTRY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		for(DCResource dcResource : listResource) {
			Assert.assertNotNull(dcResource);
			Assert.assertFalse("Country France should not be in the found resources list", "France".equals(dcResource.getProperties().get("name")));
		}
		
		queryParameters = null;
		listResource = null;
		Assert.assertNull(queryParameters);
		Assert.assertNull(listResource);
		
		// Not equal operator : &lt;&gt;
		queryParameters = new QueryParameters();
		queryParameters.add("name", "&lt;&gt;France");
		listResource = api.findDataInType(MarkaInvestModel.COUNTRY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		for(DCResource dcResource : listResource) {
			Assert.assertNotNull(dcResource);
			Assert.assertFalse("Country France should not be in the found resources list", "France".equals(dcResource.getProperties().get("name")));
		}
		
		queryParameters = null;
		listResource = null;
		Assert.assertNull(queryParameters);
		Assert.assertNull(listResource);
		
		// Not equal operator : $ne
		queryParameters = new QueryParameters();
		queryParameters.add("name", "$neFrance");
		listResource = api.findDataInType(MarkaInvestModel.COUNTRY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		for(DCResource dcResource : listResource) {
			Assert.assertNotNull(dcResource);
			Assert.assertFalse("Country France should not be in the found resources list", "France".equals(dcResource.getProperties().get("name")));
		}
		
		queryParameters = null;
		listResource = null;
		Assert.assertNull(queryParameters);
		Assert.assertNull(listResource);
		
		// Not equal operator : !=
		queryParameters = new QueryParameters();
		queryParameters.add("name", "!=France");
		listResource = api.findDataInType(MarkaInvestModel.COUNTRY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		for(DCResource dcResource : listResource) {
			Assert.assertNotNull(dcResource);
			Assert.assertFalse("Country France should not be in the found resources list", "France".equals(dcResource.getProperties().get("name")));
		}
				
	}
	
	@Test
	public void testGreaterOrEquals() {
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
		
		// Not equal operator : >=
		queryParameters = new QueryParameters();
		queryParameters.add("start", ">=2013-12-01T15:19:21+00:00");
		listResource = api.findDataInType(MarkaInvestModel.INVESTMENT_ASSISTANCE_REQUEST_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		
		queryParameters = null;
		listResource = null;
		Assert.assertNull(queryParameters);
		Assert.assertNull(listResource);
		
		// Not equal operator : &gt;=
		queryParameters = new QueryParameters();
		queryParameters.add("start", "&gt;=2013-12-01T15:19:21+00:00");
		listResource = api.findDataInType(MarkaInvestModel.COUNTRY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		
		queryParameters = null;
		listResource = null;
		Assert.assertNull(queryParameters);
		Assert.assertNull(listResource);
		
		// Not equal operator : $gte
		queryParameters = new QueryParameters();
		queryParameters.add("start", "$gte2013-12-01T15:19:21+00:00");
		listResource = api.findDataInType(MarkaInvestModel.COUNTRY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
						
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