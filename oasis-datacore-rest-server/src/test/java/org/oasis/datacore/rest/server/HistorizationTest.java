package org.oasis.datacore.rest.server;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.apache.cxf.common.util.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.security.mock.MockAuthenticationService;
import org.oasis.datacore.historization.service.HistorizationService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.sample.MarkaInvestData;
import org.oasis.datacore.sample.MarkaInvestModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })

public class HistorizationTest {

	@Autowired
	@Qualifier("datacoreApiCachedJsonClient")
	private DatacoreCachedClient datacoreApiClient;

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
	private MockAuthenticationService mockAuthenticationService;

	@Autowired
	private MarkaInvestData markaInvestData;

	@Autowired
	private HistorizationService historizationService;

	@Before
	public void flushData() {
		truncateModel(MarkaInvestModel.CITY_MODEL_NAME);
		truncateModel(MarkaInvestModel.COMPANY_MODEL_NAME);
		truncateModel("h." + MarkaInvestModel.COMPANY_MODEL_NAME);
		truncateModel(MarkaInvestModel.COST_TYPE_MODEL_NAME);
		truncateModel(MarkaInvestModel.COUNTRY_MODEL_NAME);
		truncateModel(MarkaInvestModel.FIELD_MODEL_NAME);
		truncateModel(MarkaInvestModel.INVESTMENT_ASSISTANCE_REQUEST_MODEL_NAME);
		truncateModel(MarkaInvestModel.INVESTOR_MODEL_NAME);
		truncateModel(MarkaInvestModel.INVESTOR_TYPE_MODEL_NAME);
		truncateModel(MarkaInvestModel.SECTOR_MODEL_NAME);
		truncateModel(MarkaInvestModel.USER_MODEL_NAME);

		mockAuthenticationService.loginAs("admin");
		markaInvestData.createDataSample();
		mockAuthenticationService.logout();

	}
   
   /**
    * Logout after tests to restore default unlogged state.
    * This is required in tests that use authentication,
    * else if last test fails, tests that don't login will use logged in user
    * rather than default one, which may trigger a different behaviour and
    * make some tests fail (ex. DatacoreApiServerTest.test3clientCache asserting
    * that creator is admin or guest).
    */
   @After
   public void logoutAfter() {
      mockAuthenticationService.logout();
   }
   
	@Test
	public void testHistorization() {
		
		mockAuthenticationService.loginAs("admin");
		
		datacoreApiClient.postAllDataInType(markaInvestData.getData().get(MarkaInvestModel.FIELD_MODEL_NAME), MarkaInvestModel.FIELD_MODEL_NAME);
		datacoreApiClient.postAllDataInType(markaInvestData.getData().get(MarkaInvestModel.COUNTRY_MODEL_NAME), MarkaInvestModel.COUNTRY_MODEL_NAME);
		datacoreApiClient.postAllDataInType(markaInvestData.getData().get(MarkaInvestModel.CITY_MODEL_NAME), MarkaInvestModel.CITY_MODEL_NAME);
		
		List<DCResource> listCompanies = markaInvestData.getData().get(MarkaInvestModel.COMPANY_MODEL_NAME);
		Assert.assertNotNull("Company list must not be null", listCompanies);
		Assert.assertTrue("Company list must contain elements", listCompanies.size() > 0);

		DCResource company = listCompanies.get(0);

		datacoreApiClient.postDataInType(company, MarkaInvestModel.COMPANY_MODEL_NAME);
		
		String companyId =  "" + company.getProperties().get("id");
		
		DCResource companyInserted = datacoreApiClient.getData(MarkaInvestModel.COMPANY_MODEL_NAME, companyId);
		Assert.assertNotNull("Company hasn't been inserted correctly", companyInserted);
		
		DCResource companyHistorizedResource = datacoreApiClient.findHistorizedResource(MarkaInvestModel.COMPANY_MODEL_NAME, companyId, companyInserted.getVersion().intValue());
		Assert.assertNotNull("Historized resource hasn't be stored correctly", companyHistorizedResource);
		Assert.assertEquals("Historized resource version should be the same as the inserted resource", companyInserted.getVersion(), companyHistorizedResource.getVersion());
		mockAuthenticationService.logout();
	}

	/*
	 * Ensure that the old historized document will be removed if you add a new document with same uri
	 * Prevent duplicate key error in mongo leading to unknow error
	 *    while historizing the new document during an update.
	 */
	@Test
	public void deleteOldHistorizedDoc() {
	   mockAuthenticationService.loginAs("admin");

      datacoreApiClient.postAllDataInType(markaInvestData.getData().get(MarkaInvestModel.FIELD_MODEL_NAME), MarkaInvestModel.FIELD_MODEL_NAME);
      datacoreApiClient.postAllDataInType(markaInvestData.getData().get(MarkaInvestModel.COUNTRY_MODEL_NAME), MarkaInvestModel.COUNTRY_MODEL_NAME);
      datacoreApiClient.postAllDataInType(markaInvestData.getData().get(MarkaInvestModel.CITY_MODEL_NAME), MarkaInvestModel.CITY_MODEL_NAME);

      DCResource field = buildResource(MarkaInvestModel.FIELD_MODEL_NAME, new SimpleEntry<>("id", 1),new SimpleEntry<>("name", "IT Services"));

      DCResource country = buildResource(MarkaInvestModel.COUNTRY_MODEL_NAME,
            new SimpleEntry<>("id", 1),new SimpleEntry<>("name", "France"),
            new SimpleEntry<>("lat", 46f), new SimpleEntry<>("long", 2f),
            new SimpleEntry<>("population", 65000000), new SimpleEntry<>("language", "FR_fr"));

      List<String> listPostalCodeCity1 = new ArrayList<>();
      listPostalCodeCity1.add("69001");
      listPostalCodeCity1.add("69002");
      listPostalCodeCity1.add("69003");
      DCResource city = buildResource(MarkaInvestModel.CITY_MODEL_NAME,
            new SimpleEntry<>("id", 1),new SimpleEntry<>("name", "Lyon"),
            new SimpleEntry<>("population", 2000000), new SimpleEntry<>("postalCodes", listPostalCodeCity1),
            new SimpleEntry<>("lat", 61f), new SimpleEntry<>("long", 12f), new SimpleEntry<>("country", country.getUri()));

      DCResource company = buildResource(MarkaInvestModel.COMPANY_MODEL_NAME,
            new SimpleEntry<>("id", 7), new SimpleEntry<>("name", "Arkane Studios"),
            new SimpleEntry<>("field", field.getUri()), new SimpleEntry<>("lastAnnualRevenue", 555000f),
            new SimpleEntry<>("employeeNb", 40), new SimpleEntry<>("incorporationYear", 1989),
            new SimpleEntry<>("website", "www.arkane-studios.com"), new SimpleEntry<>("country", country.getUri()),
            new SimpleEntry<>("address", ""), new SimpleEntry<>("city", city.getUri()));

      //Store a company
      datacoreApiClient.postDataInType(company, MarkaInvestModel.COMPANY_MODEL_NAME);

      //Update it
      DCResource data = datacoreApiClient.getData(MarkaInvestModel.COMPANY_MODEL_NAME, "7");
      Assert.assertNotNull(data);
      Assert.assertNotNull(data.getVersion());
      long version = data.getVersion();
      data.set("employeeNb", 11);

      DCResource putData = datacoreApiClient.postDataInType(data, MarkaInvestModel.COMPANY_MODEL_NAME);
      Assert.assertNotNull(putData);
      Assert.assertEquals(version + 1, (long) putData.getVersion());

      //Delete it
      datacoreApiClient.deleteData(MarkaInvestModel.COMPANY_MODEL_NAME, "7");
      try {
         datacoreApiClient.getData(MarkaInvestModel.COMPANY_MODEL_NAME, "7");
      } catch (NotFoundException e) {
         Assert.assertTrue(true);
      }

      //Store fresh non updated company one more time
      datacoreApiClient.postDataInType(company, MarkaInvestModel.COMPANY_MODEL_NAME);

      //Try updating it another time
      data = datacoreApiClient.getData(MarkaInvestModel.COMPANY_MODEL_NAME, "7");
      Assert.assertNotNull(data);
      Assert.assertNotNull(data.getVersion());
      version = data.getVersion();
      data.set("employeeNb", 11);

      //This fails if historized data from first update has not been deleted as it should
      try {
        putData = datacoreApiClient.postDataInType(data, MarkaInvestModel.COMPANY_MODEL_NAME);
      } catch (BadRequestException e) {
         Assert.fail("Should be able to update data.");
      }
      Assert.assertNotNull(putData);
      Assert.assertEquals(version + 1, (long) putData.getVersion());

      mockAuthenticationService.logout();
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

  @SafeVarargs
   public final DCResource buildResource(String modelType, SimpleEntry<String,?>... entries) {
      DCResource resource = new DCResource();
      List<String> types = new ArrayList<String>();
      types.add(modelType);
      resource.setTypes(types);
      for(SimpleEntry<String, ?> entry : entries) {
         if(entry != null) {
            if("id".equals(entry.getKey())) {
               resource.setUri(UriHelper.buildUri(containerUrl, modelType, String.valueOf(entry.getValue())));
            }
            resource.setProperty(entry.getKey(), entry.getValue());
         }
      }
      return resource;
   }

}