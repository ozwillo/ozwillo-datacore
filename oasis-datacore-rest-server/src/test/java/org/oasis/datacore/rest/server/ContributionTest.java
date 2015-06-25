package org.oasis.datacore.rest.server;

import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.common.util.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.oasis.datacore.contribution.exception.ContributionDatacoreCheckFailException;
import org.oasis.datacore.contribution.exception.ContributionWithNoModelException;
import org.oasis.datacore.contribution.exception.ContributionWithoutResourcesException;
import org.oasis.datacore.contribution.rest.api.DCContribution;
import org.oasis.datacore.contribution.service.ContributionService;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.core.security.mock.LocalAuthenticationService;
import org.oasis.datacore.historization.service.HistorizationService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.rest.server.resource.ResourceTypeNotFoundException;
import org.oasis.datacore.sample.MarkaInvestData;
import org.oasis.datacore.sample.MarkaInvestModel;
import org.oasis.datacore.server.uri.BadUriException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableMap;

/**
 * TODO reenable once rewritten on PointOfView
 * 
 * @author mdutoo
 *
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })
public class ContributionTest {

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
	private LocalAuthenticationService mockAuthenticationService;

	@Autowired
	private MarkaInvestData markaInvestData;
	
	@Autowired
	private MarkaInvestModel markaInvestModel;

	@Autowired
	private HistorizationService historizationService;
	
	@Autowired
	private ContributionService contributionService;

	@Before
	public void initBeforeTest() {
		/*flushData();
		createData();*/
      //markaInvestData.initData(); // NO don't insert else "Version is forbidden
	   // in POSTed data resource to create in strict POST mode", rather clean & create :
	   markaInvestData.cleanDataOfCreatedModels();
      markaInvestData.createDataSample();

      // set project :
      SimpleRequestContextProvider.setSimpleRequestContext(new ImmutableMap.Builder<String, Object>()
            .put(DatacoreApi.PROJECT_HEADER, DCProject.OASIS_SAMPLE).build());
	}
	
	private void createData() {
		mockAuthenticationService.loginAs("admin");
		markaInvestData.createDataSample();
		mockAuthenticationService.logout();
		DCModelBase fieldModel = modelAdminService.getModelBase(MarkaInvestModel.FIELD_MODEL_NAME);
		Assert.assertNotNull(fieldModel);
		fieldModel.getSecurity().setAuthentifiedReadable(true);
		fieldModel.getSecurity().setAuthentifiedCreatable(true);
		fieldModel.getSecurity().setAuthentifiedWritable(true);
	}
	
	private void flushData() {
		
		for(DCModel model : markaInvestModel.getListModel()) {
			if(model != null) {
				truncateModel(model.getName());
				if(model.isHistorizable()) {
					truncateModel(model.getName() + ".h");
				}
				if(model.isContributable()) {
					truncateModel(model.getName() + ".c");
				}
			}
		}
		
	}

	/**
	 * Logout after tests to restore default unlogged state. This is required in
	 * tests that use authentication, else if last test fails, tests that don't
	 * login will use logged in user rather than default one, which may trigger
	 * a different behaviour and make some tests fail (ex.
	 * DatacoreApiServerTest.test3clientCache asserting that creator is admin or
	 * guest).
	 */
	@After
	public void logoutAfter() {
		mockAuthenticationService.logout();
		///flushData();
		DCModelBase fieldModel = modelAdminService.getModelBase(MarkaInvestModel.FIELD_MODEL_NAME);
		Assert.assertNotNull(fieldModel);
		fieldModel.getSecurity().setAuthentifiedReadable(true);
		fieldModel.getSecurity().setAuthentifiedCreatable(true);
		fieldModel.getSecurity().setAuthentifiedWritable(true);
	}

	@Test
	public void testAddDCContribution() throws Exception {
		
		mockAuthenticationService.loginAs("admin");
		datacoreApiClient.postAllDataInType(markaInvestData.getData().get(MarkaInvestModel.FIELD_MODEL_NAME), MarkaInvestModel.FIELD_MODEL_NAME);
		DCModelBase fieldModel = modelAdminService.getModelBase(MarkaInvestModel.FIELD_MODEL_NAME);
		Assert.assertNotNull(fieldModel);
		fieldModel.getSecurity().setAuthentifiedReadable(false);
		fieldModel.getSecurity().setAuthentifiedCreatable(false);
		fieldModel.getSecurity().setAuthentifiedWritable(false);
		mockAuthenticationService.logout();
		
		mockAuthenticationService.loginAs("contribution_user_1");
		
		List<DCResource> listFields = markaInvestData.getData().get(MarkaInvestModel.FIELD_MODEL_NAME);
		Assert.assertNotNull("Field list must not be null", listFields);
		Assert.assertTrue("Field list must contain elements", listFields.size() > 0);

		DCResource field = listFields.get(0);
		field.set("name", "Insurance");
		DCResource field1 = markaInvestData.buildResource(MarkaInvestModel.FIELD_MODEL_NAME, new SimpleEntry<>("id", 4),new SimpleEntry<>("name", "TEST"));
		
		List<DCResource> listContributionResources = new ArrayList<>();
		listContributionResources.add(field);
		listContributionResources.add(field1);
		
		DCContribution contribution = new DCContribution();
		contribution.setComment("test de contribution");
		contribution.setListResources(listContributionResources);
		contribution.setModelType(MarkaInvestModel.FIELD_MODEL_NAME);
		contribution.setTitle("Contrib 1");
		
		DCContribution returnedContribution = null;
		try {
			returnedContribution = contributionService.create(contribution);
			Assert.assertNotNull(returnedContribution);
			Assert.assertFalse(returnedContribution.getId().isEmpty());
		} catch (ContributionWithoutResourcesException | ContributionWithNoModelException | ContributionDatacoreCheckFailException | BadUriException | ResourceException e) {
			throw e;
		}		
			
		mockAuthenticationService.logout();
	}
	
	@Test
	public void testGetDCContribution() throws Exception {
		
		mockAuthenticationService.loginAs("admin");
		datacoreApiClient.postAllDataInType(markaInvestData.getData().get(MarkaInvestModel.FIELD_MODEL_NAME), MarkaInvestModel.FIELD_MODEL_NAME);
		mockAuthenticationService.logout();
		
		mockAuthenticationService.loginAs("contribution_user_1");
		
		List<DCResource> listFields = markaInvestData.getData().get(MarkaInvestModel.FIELD_MODEL_NAME);
		Assert.assertNotNull("Field list must not be null", listFields);
		Assert.assertTrue("Field list must contain elements", listFields.size() > 0);

		DCResource field = listFields.get(0);
		field.set("name", "Insurance");
		DCResource field1 = markaInvestData.buildResource(MarkaInvestModel.FIELD_MODEL_NAME, new SimpleEntry<>("id", 4),new SimpleEntry<>("name", "TEST"));
		
		List<DCResource> listContributionResources = new ArrayList<>();
		listContributionResources.add(field);
		listContributionResources.add(field1);
		
		DCContribution contribution = new DCContribution();
		contribution.setComment("test de contribution");
		contribution.setListResources(listContributionResources);
		contribution.setModelType(MarkaInvestModel.FIELD_MODEL_NAME);
		contribution.setTitle("Contrib 1");
		
		DCContribution returnedContribution = null;
		try {
			returnedContribution = contributionService.create(contribution);
			Assert.assertNotNull(returnedContribution);
			Assert.assertFalse(returnedContribution.getId().isEmpty());
		} catch (ContributionWithoutResourcesException | ContributionWithNoModelException | ContributionDatacoreCheckFailException | BadUriException | ResourceException e) {
			throw e;
		}
		String contributionId = returnedContribution.getId();
		
		// Test one by getting the contribution with the userId
		List<DCContribution> listContribution = contributionService.get(MarkaInvestModel.FIELD_MODEL_NAME, "contribution_user_1", null, 0, 10);
		Assert.assertNotNull("Null contribution list", listContribution);
		Assert.assertFalse("Contribution list is empty", listContribution.isEmpty());
		Assert.assertTrue("Contribution size must be == 1", listContribution.size() == 1);
		listContribution = null;
		Assert.assertNull(listContribution);
		
		// Test two by getting the contribution with the contribution id
		listContribution = contributionService.get(MarkaInvestModel.FIELD_MODEL_NAME, null, contributionId, 0, 10);
		Assert.assertNotNull("Null contribution list", listContribution);
		Assert.assertTrue("Contribution list is empty", !listContribution.isEmpty());
		Assert.assertTrue("Contribution size must be == 1", listContribution.size() == 1);
		listContribution = null;
		
		// Test three by getting the contribution with a wrong contribution Id
		listContribution = contributionService.get(MarkaInvestModel.FIELD_MODEL_NAME, null, "dskjfldjlfs", 0, 10);
		Assert.assertTrue(listContribution.isEmpty());
		listContribution = null;
		
	}
	
	@Test
	public void testGetDCContributionResources() throws Exception {
		
		mockAuthenticationService.loginAs("admin");
		datacoreApiClient.postAllDataInType(markaInvestData.getData().get(MarkaInvestModel.FIELD_MODEL_NAME), MarkaInvestModel.FIELD_MODEL_NAME);
		mockAuthenticationService.logout();
		
		mockAuthenticationService.loginAs("contribution_user_1");
		
		List<DCResource> listFields = markaInvestData.getData().get(MarkaInvestModel.FIELD_MODEL_NAME);
		Assert.assertNotNull("Field list must not be null", listFields);
		Assert.assertTrue("Field list must contain elements", listFields.size() > 0);

		DCResource field = listFields.get(0);
		field.set("name", "Insurance");
		DCResource field1 = markaInvestData.buildResource(MarkaInvestModel.FIELD_MODEL_NAME, new SimpleEntry<>("id", 4),new SimpleEntry<>("name", "TEST"));
		
		List<DCResource> listContributionResources = new ArrayList<>();
		listContributionResources.add(field);
		listContributionResources.add(field1);
		
		DCContribution contribution = new DCContribution();
		contribution.setComment("test de contribution");
		contribution.setListResources(listContributionResources);
		contribution.setModelType(MarkaInvestModel.FIELD_MODEL_NAME);
		contribution.setTitle("Contrib 1");
		
		DCContribution returnedContribution = null;
		try {
			returnedContribution = contributionService.create(contribution);
			Assert.assertNotNull(returnedContribution);
			Assert.assertFalse(returnedContribution.getId().isEmpty());
		} catch (ContributionWithoutResourcesException | ContributionWithNoModelException | ContributionDatacoreCheckFailException | BadUriException | ResourceException e) {
			throw e;
		}
		String contributionId = returnedContribution.getId();
		
		// Test one by getting the contribution with the userId
		List<DCResource> listContributionResourcesGet = contributionService.get(MarkaInvestModel.FIELD_MODEL_NAME, contributionId);
		Assert.assertNotNull("Null contribution resources list", listContributionResourcesGet);
		Assert.assertFalse("Contribution resources list is empty", listContributionResourcesGet.isEmpty());
		Assert.assertTrue("Contribution resources size must be == 2", listContributionResourcesGet.size() == 2);
		listContributionResourcesGet = null;
		Assert.assertNull(listContributionResourcesGet);
		
	}
	
	@Test
	public void testRemoveDCContribution() throws Exception {
		
		mockAuthenticationService.loginAs("admin");
		datacoreApiClient.postAllDataInType(markaInvestData.getData().get(MarkaInvestModel.FIELD_MODEL_NAME), MarkaInvestModel.FIELD_MODEL_NAME);
		mockAuthenticationService.logout();
		
		mockAuthenticationService.loginAs("contribution_user_1");
		
		List<DCResource> listFields = markaInvestData.getData().get(MarkaInvestModel.FIELD_MODEL_NAME);
		Assert.assertNotNull("Field list must not be null", listFields);
		Assert.assertTrue("Field list must contain elements", listFields.size() > 0);

		DCResource field = listFields.get(0);
		field.set("name", "Insurance");
		DCResource field1 = markaInvestData.buildResource(MarkaInvestModel.FIELD_MODEL_NAME, new SimpleEntry<>("id", 4),new SimpleEntry<>("name", "TEST"));
		
		List<DCResource> listContributionResources = new ArrayList<>();
		listContributionResources.add(field);
		listContributionResources.add(field1);
		
		DCContribution contribution = new DCContribution();
		contribution.setComment("test de contribution");
		contribution.setListResources(listContributionResources);
		contribution.setModelType(MarkaInvestModel.FIELD_MODEL_NAME);
		contribution.setTitle("Contrib 1");
		
		DCContribution returnedContribution = null;
		try {
			returnedContribution = contributionService.create(contribution);
			Assert.assertNotNull(returnedContribution);
			Assert.assertFalse(returnedContribution.getId().isEmpty());
		} catch (ContributionWithoutResourcesException | ContributionWithNoModelException | ContributionDatacoreCheckFailException | BadUriException | ResourceException e) {
			throw e;
		}
		String contributionId = returnedContribution.getId();
		
		// Test one by getting the contribution with the userId
		boolean isCorrectlyRemoved = false;
		try {
			isCorrectlyRemoved = contributionService.remove(MarkaInvestModel.FIELD_MODEL_NAME, contributionId);
		} catch (ResourceTypeNotFoundException e) {
			e.printStackTrace();
		}
		Assert.assertTrue(isCorrectlyRemoved);
		
	}

	private void truncateModel(String type) {
		if (type != null && !StringUtils.isEmpty(type)) {
			mongoOperations.remove(new Query(), type);
		} else {
			System.out.println("Model type is null or empty, cannot truncate model");
		}
	}

}