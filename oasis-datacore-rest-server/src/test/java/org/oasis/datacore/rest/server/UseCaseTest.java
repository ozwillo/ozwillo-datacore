package org.oasis.datacore.rest.server;

import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.apache.cxf.common.util.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.security.mock.MockAuthenticationService;
import org.oasis.datacore.historization.exception.HistorizationException;
import org.oasis.datacore.historization.service.HistorizationService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.rest.client.QueryParameters;
import org.oasis.datacore.rights.rest.api.RightsApi;
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
public class UseCaseTest {
	
	@Autowired
	private RightsApi rightsApi;
	
	@Autowired
	private DatacoreCachedClient datacoreApiClient;

	@Value("${datacoreApiClient.containerUrl}")
	private String containerUrl;

	@Autowired
	private DataModelServiceImpl modelAdminService;

	@Autowired
	private MongoOperations mongoOperations;

	@Autowired
	private MockAuthenticationService mockAuthenticationService;

	@Autowired
	private MarkaInvestData markaInvestData;
	
    /** for testing purpose */
    @Autowired
    @Qualifier("datacoreApiImpl") 
    private DatacoreApiImpl datacoreApiImpl;
	
	@Autowired
	private HistorizationService historizationService;
	
	@Before
	public void flushData() {
		
		truncateModel(MarkaInvestModel.CITY_MODEL_NAME);
		truncateModel(MarkaInvestModel.COMPANY_MODEL_NAME);
		truncateHistorizationModel(MarkaInvestModel.COMPANY_MODEL_NAME);
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
	
	@Test
	@Ignore
	public void useCase() {
		
		mockAuthenticationService.loginAs("admin");
		datacoreApiClient.postAllDataInType(markaInvestData.getData().get(MarkaInvestModel.FIELD_MODEL_NAME), MarkaInvestModel.FIELD_MODEL_NAME);
		datacoreApiClient.postAllDataInType(markaInvestData.getData().get(MarkaInvestModel.COUNTRY_MODEL_NAME), MarkaInvestModel.COUNTRY_MODEL_NAME);
		datacoreApiClient.postAllDataInType(markaInvestData.getData().get(MarkaInvestModel.CITY_MODEL_NAME), MarkaInvestModel.CITY_MODEL_NAME);
		mockAuthenticationService.logout();
		
		//ReadAllDataInType for the first time
		readAllDataInType();
		
		addSomeCompanies();
		
		//ReadAllDataInType one more time
	    readAllDataInType();
	      
	    updateOne();
	    
		//yet Another ReadAllDataInType
		readAllDataInType();
		
		deleteWrongData();
		
		//Check everything is ok
		readAllDataInType();
	}
	
	public void readAllDataInType() {
      List<DCResource> dataInType = datacoreApiClient.findDataInType(MarkaInvestModel.COMPANY_MODEL_NAME, new QueryParameters(), null, null);
	  Assert.assertNotNull("Should find some data.", dataInType);	
	}
	

	public void addSomeCompanies() {	
		
		List<DCResource> listField = new ArrayList<DCResource>();
		DCResource field4 = buildResource(MarkaInvestModel.FIELD_MODEL_NAME, new SimpleEntry<>("id", 4),new SimpleEntry<>("name", "Sport"));
		DCResource field5 = buildResource(MarkaInvestModel.FIELD_MODEL_NAME, new SimpleEntry<>("id", 5),new SimpleEntry<>("name", "Video Games"));
		listField.add(field4);
		listField.add(field5);
		try {
			mockAuthenticationService.loginAs("admin");
			datacoreApiClient.postAllDataInType(listField, MarkaInvestModel.FIELD_MODEL_NAME);
			mockAuthenticationService.logout();
		} catch (WebApplicationException e) {}
		
		DCResource country1 = buildResource(MarkaInvestModel.COUNTRY_MODEL_NAME,
				new SimpleEntry<>("id", 1),new SimpleEntry<>("name", "France"),
				new SimpleEntry<>("lat", 46f), new SimpleEntry<>("long", 2f),
				new SimpleEntry<>("population", 65000000), new SimpleEntry<>("language", "FR_fr"));
		
		List<String> listPostalCodeCity1 = new ArrayList<>();
		listPostalCodeCity1.add("69001");
		listPostalCodeCity1.add("69002");
		listPostalCodeCity1.add("69003");
		DCResource city1 = buildResource(MarkaInvestModel.CITY_MODEL_NAME,
				new SimpleEntry<>("id", 1),new SimpleEntry<>("name", "Lyon"),
				new SimpleEntry<>("population", 2000000), new SimpleEntry<>("postalCodes", listPostalCodeCity1),
				new SimpleEntry<>("lat", 61f), new SimpleEntry<>("long", 12f), new SimpleEntry<>("country", country1.getUri()));
		
		DCResource company3 = buildResource(MarkaInvestModel.COMPANY_MODEL_NAME,
				new SimpleEntry<>("id", 3), new SimpleEntry<>("name", "CDK"),
				new SimpleEntry<>("field", field4.getUri()), new SimpleEntry<>("lastAnnualRevenue", 100000f),
				new SimpleEntry<>("employeeNb", 10), new SimpleEntry<>("incorporationYear", 1989),
				new SimpleEntry<>("website", "www.cdk.fr"), new SimpleEntry<>("country", country1.getUri()),
				new SimpleEntry<>("address", "22 rue Terme"), new SimpleEntry<>("city", city1.getUri()));
		
		DCResource company4 = buildResource(MarkaInvestModel.COMPANY_MODEL_NAME,
				new SimpleEntry<>("id", 4), new SimpleEntry<>("name", "Arkane Studios"),
				new SimpleEntry<>("field", field5.getUri()), new SimpleEntry<>("lastAnnualRevenue", 555000f),
				new SimpleEntry<>("employeeNb", 40), new SimpleEntry<>("incorporationYear", 1989),
				new SimpleEntry<>("website", "www.arkane-studios.com"), new SimpleEntry<>("country", country1.getUri()),
				new SimpleEntry<>("address", ""), new SimpleEntry<>("city", city1.getUri()));
		
		
		try {
		  mockAuthenticationService.loginAs("admin");
	      datacoreApiClient.postDataInType(company3, MarkaInvestModel.COMPANY_MODEL_NAME);
	      datacoreApiClient.postDataInType(company4, MarkaInvestModel.COMPANY_MODEL_NAME);
	      mockAuthenticationService.logout();
	    } catch (Exception e) {
	      Assert.fail("An error occured");
	    }
	}
	
	public void updateOne() {
		mockAuthenticationService.loginAs("admin");
		DCResource data = datacoreApiClient.getData(MarkaInvestModel.COMPANY_MODEL_NAME, "3");
		Assert.assertNotNull(data);
	    Assert.assertNotNull(data.getVersion());
	    long version = data.getVersion();
		data.set("employeeNb", 11);		
		
		DCResource putData = datacoreApiClient.postDataInType(data, MarkaInvestModel.COMPANY_MODEL_NAME);
		mockAuthenticationService.logout();
	    Assert.assertNotNull(putData);
	    Assert.assertEquals(version + 1, (long) putData.getVersion());
	}
	
	public void deleteWrongData() {
		//Let's say we want to delete company 4 data
		mockAuthenticationService.loginAs("admin");
		datacoreApiClient.deleteData(MarkaInvestModel.COMPANY_MODEL_NAME, "4");
		try {
			datacoreApiClient.getData(MarkaInvestModel.COMPANY_MODEL_NAME, "4");
		} catch (NotFoundException e) {
			Assert.assertTrue(true);
		}
		mockAuthenticationService.logout();
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
	
	private void truncateHistorizationModel(String type) {
		if (type != null && !StringUtils.isEmpty(type)) {
			DCModel dcModel = modelAdminService.getModel(type);
			if (dcModel != null && dcModel.isHistorizable()) {
				try {
					String historizationCollectionName = historizationService.getHistorizedCollectionNameFromOriginalModel(dcModel);
					mongoOperations.remove(new Query(), historizationCollectionName);
				} catch (HistorizationException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("Model type is null or empty, cannot truncate model");
		}
	}
}
