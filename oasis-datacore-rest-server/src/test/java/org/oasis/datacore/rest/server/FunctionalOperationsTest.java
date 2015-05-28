package org.oasis.datacore.rest.server;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.BadRequestException;

import org.apache.cxf.common.util.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.security.mock.LocalAuthenticationService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UnitTestHelper;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
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
	@Qualifier("datacoreApiCachedJsonClient")
	protected DatacoreCachedClient api;

	@Value("${datacoreApiClient.containerUrl}")
   private String containerUrlString;
   @Value("#{new java.net.URI('${datacoreApiClient.containerUrl}')}")
   //@Value("#{uriService.getContainerUrl()}")
   private URI containerUrl;

	@Autowired
	private DataModelServiceImpl modelAdminService;

	@Autowired
	private MongoOperations mongoOperations;
	
	/** TODO actual */
	@Autowired
	private LocalAuthenticationService mockAuthenticationService;
	
	@Autowired
	private MarkaInvestData markaInvestData;
			
	@Before
	public void flushData() {
		/*truncateModel(MarkaInvestModel.CITY_MODEL_NAME);
		truncateModel(MarkaInvestModel.COMPANY_MODEL_NAME);
		truncateModel(MarkaInvestModel.COST_TYPE_MODEL_NAME);
		truncateModel(MarkaInvestModel.COUNTRY_MODEL_NAME);
		truncateModel(MarkaInvestModel.FIELD_MODEL_NAME);
		truncateModel(MarkaInvestModel.INVESTMENT_ASSISTANCE_REQUEST_MODEL_NAME);
		truncateModel(MarkaInvestModel.INVESTOR_MODEL_NAME);
		truncateModel(MarkaInvestModel.INVESTOR_TYPE_MODEL_NAME);
		truncateModel(MarkaInvestModel.SECTOR_MODEL_NAME);
		truncateModel(MarkaInvestModel.USER_MODEL_NAME);

      mockAuthenticationService.loginAs("admin"); // else marka resources not writable
		markaInvestData.createDataSample();
		markaInvestData.insertData();
      mockAuthenticationService.logout();*/
	   markaInvestData.initData(); // cleans data first
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
		
		queryParameters = new QueryParameters().add("name", "+");
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
		
		queryParameters = new QueryParameters().add("name", "-");
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
	
	@Ignore
	@Test
	public void testGreaterOrEquals() {
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
		
		// Greater or equal operator : >=
		queryParameters = new QueryParameters();
		queryParameters.add("start", ">=2013-12-01T15:19:21+00:00");
		listResource = api.findDataInType(MarkaInvestModel.INVESTMENT_ASSISTANCE_REQUEST_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		
		queryParameters = null;
		listResource = null;
		Assert.assertNull(queryParameters);
		Assert.assertNull(listResource);
		
		// Greater or equal operator : &gt;=
		queryParameters = new QueryParameters();
		queryParameters.add("start", "&gt;=2013-12-01T15:19:21+00:00");
		listResource = api.findDataInType(MarkaInvestModel.COUNTRY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		
		queryParameters = null;
		listResource = null;
		Assert.assertNull(queryParameters);
		Assert.assertNull(listResource);
		
		// Greater or equal : $gte
		queryParameters = new QueryParameters();
		queryParameters.add("start", "$gte2013-12-01T15:19:21+00:00");
		listResource = api.findDataInType(MarkaInvestModel.COUNTRY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
						
	}
		
	@Test
	public void testLowerOrEquals() {
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
				
		// Lower or equal operator : <=
		queryParameters = new QueryParameters();
		queryParameters.add("lat", "<=70");
		listResource = api.findDataInType(MarkaInvestModel.CITY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		Assert.assertTrue(listResource.size()==Integer.valueOf(1));
		
		queryParameters = null;
		listResource = null;
		Assert.assertNull(queryParameters);
		Assert.assertNull(listResource);
		
		// Lower or equal operator : &lt;=
		queryParameters = new QueryParameters();
		queryParameters.add("lat", "&lt;=70");
		listResource = api.findDataInType(MarkaInvestModel.CITY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		Assert.assertTrue(listResource.size()==Integer.valueOf(1));
		
		queryParameters = null;
		listResource = null;
		Assert.assertNull(queryParameters);
		Assert.assertNull(listResource);
		
		// Lower or equal operator : $lte
		queryParameters = new QueryParameters();
		queryParameters.add("lat", "$lte70");
		listResource = api.findDataInType(MarkaInvestModel.CITY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		Assert.assertTrue(listResource.size()==Integer.valueOf(1));
						
	}
	
	@Test
	public void testLowerThan() {
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
				
		// Lower or equal operator : <
		queryParameters = new QueryParameters();
		queryParameters.add("lat", "<100");
		listResource = api.findDataInType(MarkaInvestModel.CITY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		Assert.assertTrue(listResource.size()==Integer.valueOf(2));
		
		queryParameters = null;
		listResource = null;
		Assert.assertNull(queryParameters);
		Assert.assertNull(listResource);
		
		// Lower or equal operator : &lt;
		queryParameters = new QueryParameters();
		queryParameters.add("lat", "&lt;100");
		listResource = api.findDataInType(MarkaInvestModel.CITY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		Assert.assertTrue(listResource.size()==Integer.valueOf(2));
		
		queryParameters = null;
		listResource = null;
		Assert.assertNull(queryParameters);
		Assert.assertNull(listResource);
		
		// Lower or equal operator : $lt
		queryParameters = new QueryParameters();
		queryParameters.add("lat", "$lt50");
		listResource = api.findDataInType(MarkaInvestModel.CITY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertTrue("Resource list should be empty", listResource.isEmpty());
		Assert.assertTrue(listResource.size()==Integer.valueOf(0));
						
	}
	
	@Test
	public void testIn() {
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
				
		queryParameters = new QueryParameters()
		   .add("tel", "$in\"0142143059\""); // NB. if not quoted, parsed as int and misses front 0
		listResource = api.findDataInType(MarkaInvestModel.USER_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertEquals("Resource list should have 1 entry", 1, listResource.size());

      queryParameters = new QueryParameters().add("tel", "$in[\"0142143059\"]");
      listResource = api.findDataInType(MarkaInvestModel.USER_MODEL_NAME, queryParameters, 0, 10);
      Assert.assertNotNull(listResource);
      Assert.assertEquals("Resource list should have 1 entry", 1, listResource.size());

      queryParameters = new QueryParameters().add("tel", "$in[\"0142143059\",\"014214305X\"]");
      listResource = api.findDataInType(MarkaInvestModel.USER_MODEL_NAME, queryParameters, 0, 10);
      Assert.assertNotNull(listResource);
      Assert.assertEquals("Resource list should have 1 entry", 1, listResource.size());

      queryParameters = new QueryParameters().add("tel", "$in[\"0142143059\",\"08445125647\"]");
      listResource = api.findDataInType(MarkaInvestModel.USER_MODEL_NAME, queryParameters, 0, 10);
      Assert.assertNotNull(listResource);
      Assert.assertEquals("Resource list should have 2 entries", 2, listResource.size());
	}
	
	@Test
	public void testNotIn() {
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
				
		queryParameters = new QueryParameters().add("firstName", "$nin\"Frédéric\"");
		listResource = api.findDataInType(MarkaInvestModel.USER_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertEquals("Resource list should have one entry", 1, listResource.size());

      queryParameters = new QueryParameters().add("firstName", "$nin[\"Frédéric\"]");
      listResource = api.findDataInType(MarkaInvestModel.USER_MODEL_NAME, queryParameters, 0, 10);
      Assert.assertNotNull(listResource);
      Assert.assertEquals("Resource list should have one entry", 1, listResource.size());

      queryParameters = new QueryParameters().add("firstName", "$nin[\"Frédéric\",\"X\"]");
      listResource = api.findDataInType(MarkaInvestModel.USER_MODEL_NAME, queryParameters, 0, 10);
      Assert.assertNotNull(listResource);
      Assert.assertEquals("Resource list should have one entry", 1, listResource.size());

      queryParameters = new QueryParameters().add("firstName", "$nin[\"Frédéric\",\"Albert\"]");
      listResource = api.findDataInType(MarkaInvestModel.USER_MODEL_NAME, queryParameters, 0, 10);
      Assert.assertNotNull(listResource);
      Assert.assertEquals("Resource list should have no entry", 0, listResource.size());
						
	}
	
	@Test
	public void testRegex() {
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
				
		queryParameters = new QueryParameters();
		queryParameters.add("email", "$regex.*@gmail.com");
		listResource = api.findDataInType(MarkaInvestModel.USER_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		Assert.assertTrue(listResource.size()==Integer.valueOf(1));
						
	}

	@Test
	public void testExists() {
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
				
		// Exist operator : $exists
		queryParameters = new QueryParameters();
		queryParameters.add("email", "$exists");
		listResource = api.findDataInType(MarkaInvestModel.USER_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertEquals("Resource list should have 2 entries", 2, listResource.size());
						
	}
	
	@Test
	public void testAll() {
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
				
		queryParameters = new QueryParameters();
		queryParameters.add("companies", "$all[\"http://data-test.oasis-eu.org/dc/type/sample.marka.company/2\"]");
		listResource = api.findDataInType(MarkaInvestModel.USER_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		Assert.assertTrue(listResource.size()==Integer.valueOf(1));
						
	}
	
	@Test
	public void testElemMatch() {
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
				
		queryParameters = new QueryParameters();
		queryParameters.add("companies", "$elemMatch{\"nonExistingFieldA\":"
		      + "\"http://data-test.oasis-eu.org/dc/type/sample.marka.company/1\""
		      + ", \"nonExistingFieldB\":0}");
		try {
		   listResource = api.findDataInType(MarkaInvestModel.USER_MODEL_NAME, queryParameters, 0, 10);
      } catch (BadRequestException brex) {
         Assert.assertTrue(UnitTestHelper.readBodyAsString(brex)
               .contains("$elemMatch criteria value should be on list whose elements are maps"));
      }
		
		// TODO test that works
		/*
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		Assert.assertTrue(listResource.size()==Integer.valueOf(2));
		*/
		
		try {
		   api.findDataInType(MarkaInvestModel.USER_MODEL_NAME, new QueryParameters()
		      .add("firstName", "$elemMatch{\"nonExistingFieldA\":"
		            + "\"http://data-test.oasis-eu.org/dc/type/sample.marka.company/1\"}"
		            + ", \"nonExistingFieldB\":0"), 0, 10);
		   Assert.fail("Should not be able to use $elemMatch on a non-list field !");
		} catch (BadRequestException brex) {
		   Assert.assertTrue(UnitTestHelper.readBodyAsString(brex)
		         .contains("Field of type string is not compatible with operator ELEM_MATCH"));
		}
	}
	
	@Test
	public void testSize() {
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
				
		queryParameters = new QueryParameters();
		queryParameters.add("companies", "$size1");
		listResource = api.findDataInType(MarkaInvestModel.USER_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertFalse("Resource list should not be empty", listResource.isEmpty());
		Assert.assertTrue(listResource.size()==Integer.valueOf(1));
						
	}
	
	private void truncateModel(String type) {
		if (type != null && !StringUtils.isEmpty(type)) {
			DCModelBase dcModel = modelAdminService.getModelBase(type);
			if (dcModel != null) { // && dcModel.isInstanciable()
				mongoOperations.remove(new Query(), dcModel.getCollectionName());
			}
		} else {
			System.out.println("Model type is null or empty, cannot truncate model");
		}
	}

}