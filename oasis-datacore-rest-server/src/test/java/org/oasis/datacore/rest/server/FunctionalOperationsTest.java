package org.oasis.datacore.rest.server;

import java.util.List;

import org.apache.cxf.common.util.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
	@Ignore
	public void testEquals() {
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
		
		// Simple equals
		queryParameters = new QueryParameters();
		queryParameters.add("name", "=Societe Generale");
		listResource = api.findDataInType(MarkaInvestModel.COMPANY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertTrue(!listResource.isEmpty());
		
		queryParameters = null;
		listResource = null;
		Assert.assertNull(queryParameters);
		Assert.assertNull(listResource);
		
		// Double equals
		queryParameters = new QueryParameters();
		queryParameters.add("lastAnnualRevenue", "==956210000");
		listResource = api.findDataInType(MarkaInvestModel.COMPANY_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertTrue(!listResource.isEmpty());
		
	}
	
	@Test
	@Ignore
	public void testSortDesc() {
//		
//		DCResource field1 = buildResource(MarkaInvestModel.FIELD_MODEL_NAME, new SimpleEntry<>("id", 1),new SimpleEntry<>("name", "IT Services"));	
//		DCResource field2 = buildResource(MarkaInvestModel.FIELD_MODEL_NAME, new SimpleEntry<>("id", 2),new SimpleEntry<>("name", "Accounting"));	
//		DCResource field3 = buildResource(MarkaInvestModel.FIELD_MODEL_NAME, new SimpleEntry<>("id", 3),new SimpleEntry<>("name", "Banking"));	
		
		QueryParameters queryParameters = null;
		List<DCResource> listResource = null;
		
		queryParameters = new QueryParameters();
		queryParameters.add("name", "%2B");
		listResource = api.findDataInType(MarkaInvestModel.FIELD_MODEL_NAME, queryParameters, 0, 10);
		Assert.assertNotNull(listResource);
		Assert.assertTrue(!listResource.isEmpty());
		
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