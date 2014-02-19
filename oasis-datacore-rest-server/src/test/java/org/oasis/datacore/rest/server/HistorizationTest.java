package org.oasis.datacore.rest.server;

import java.util.List;

import org.apache.cxf.common.util.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.security.mock.MockAuthenticationService;
import org.oasis.datacore.historization.service.HistorizationService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.sample.MarkaInvestData;
import org.oasis.datacore.sample.MarkaInvestModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })

public class HistorizationTest {

	@Autowired
	private DatacoreCachedClient datacoreApiClient;

	@Value("${datacoreApiClient.containerUrl}")
	private String containerUrl;

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