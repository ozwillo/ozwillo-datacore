package org.oasis.datacore.rest.server.sample.data;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;

import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.client.DatacoreClientApi;
import org.oasis.datacore.rest.server.sample.model.MarkaInvestModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("markaInvestModel")
public class MarkaInvestData {
	
	private List<DCResource> listCompany;
	private List<DCResource> listField;
	private List<DCResource> listSector;
	private List<DCResource> listCountry;
	private List<DCResource> listCity;
	private List<DCResource> listUser;
	private List<DCResource> listInvestor;
	private List<DCResource> listInvestorType;
	private List<DCResource> listCost;
	private List<DCResource> listPlannedInvestmentAssistanceRequest;
	
	private HashMap<String, List<DCResource>> mapData;
	
	@Value("${datacoreApiClient.containerUrl}")
	private String containerUrl;
	
	@Value("${datacoreTestServer.enableSampleDataInsertionAtStartup}")
	private boolean enableSampleDataInsertionAtStartup;
	
	@Autowired
	@Qualifier("datacoreApiClient")
	protected DatacoreClientApi api;
	
	@PostConstruct
	public void init() {
		
		listCompany = new ArrayList<DCResource>();
		listField = new ArrayList<DCResource>();
		listSector = new ArrayList<DCResource>();
		listCountry = new ArrayList<DCResource>();
		listCity = new ArrayList<DCResource>();
		listUser = new ArrayList<DCResource>();
		listInvestor = new ArrayList<DCResource>();
		listInvestorType = new ArrayList<DCResource>();
		listCost = new ArrayList<DCResource>();
		listPlannedInvestmentAssistanceRequest = new ArrayList<DCResource>();
	
		mapData = new HashMap<String, List<DCResource>>();
		
		createDataSample();
		if(enableSampleDataInsertionAtStartup) {
			insertData();
		}
				
	}
	
	@SafeVarargs
	private final DCResource buildResource(String modelType, SimpleEntry<String,?>... entries) {
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
	
	public void createDataSample() {
		
		DCResource field1 = buildResource(MarkaInvestModel.FIELD_MODEL_NAME, new SimpleEntry<>("id", "1"),new SimpleEntry<>("name", "IT Services"));	
		DCResource field2 = buildResource(MarkaInvestModel.FIELD_MODEL_NAME, new SimpleEntry<>("id", "2"),new SimpleEntry<>("name", "Accounting"));	
		DCResource field3 = buildResource(MarkaInvestModel.FIELD_MODEL_NAME, new SimpleEntry<>("id", "3"),new SimpleEntry<>("name", "Banking"));	
		listField.add(field1);
		listField.add(field2);
		listField.add(field3);
		
		DCResource country1 = buildResource(MarkaInvestModel.COUNTRY_MODEL_NAME,
				new SimpleEntry<>("id", "1"),new SimpleEntry<>("name", "France"),
				new SimpleEntry<>("lat", 46d), new SimpleEntry<>("long", 2d),
				new SimpleEntry<>("population", 65000000), new SimpleEntry<>("language", "FR_fr"));
		
		DCResource country2 = buildResource(MarkaInvestModel.COUNTRY_MODEL_NAME,
				new SimpleEntry<>("id", "2"),new SimpleEntry<>("name", "Deutschland"),
				new SimpleEntry<>("lat", 50d), new SimpleEntry<>("long", 15d),
				new SimpleEntry<>("population", 55000000), new SimpleEntry<>("language", "De_de"));
		listCountry.add(country1);
		listCountry.add(country2);
		
		List<String> listPostalCodeCity1 = new ArrayList<>();
		listPostalCodeCity1.add("69001");
		listPostalCodeCity1.add("69002");
		listPostalCodeCity1.add("69003");
		DCResource city1 = buildResource(MarkaInvestModel.CITY_MODEL_NAME,
				new SimpleEntry<>("id", "1"),new SimpleEntry<>("name", "Lyon"),
				new SimpleEntry<>("population", 2000000), new SimpleEntry<>("postalCode", listPostalCodeCity1),
				new SimpleEntry<>("lat", 61d), new SimpleEntry<>("long", 12d), new SimpleEntry<>("country", country1));
		
		List<String> listPostalCodeCity2 = new ArrayList<>();
		listPostalCodeCity1.add("10243");
		listPostalCodeCity1.add("10245");
		listPostalCodeCity1.add("10247");
		DCResource city2 = buildResource(MarkaInvestModel.CITY_MODEL_NAME,
				new SimpleEntry<>("id", "2"),new SimpleEntry<>("name", "Berlin"),
				new SimpleEntry<>("population", 1000000), new SimpleEntry<>("postalCode", listPostalCodeCity2),
				new SimpleEntry<>("lat", 75d), new SimpleEntry<>("long", 35d), new SimpleEntry<>("country", country2));
		listCity.add(city1);
		listCity.add(city2);
		
		DCResource company1 = buildResource(MarkaInvestModel.COMPANY_MODEL_NAME,
				new SimpleEntry<>("id", "1"), new SimpleEntry<>("name", "Societe Generale"),
				new SimpleEntry<>("field", field3), new SimpleEntry<>("lastAnnualRevenue", 956210000d),
				new SimpleEntry<>("employeeNb", 35000), new SimpleEntry<>("incorporationYear", 1975),
				new SimpleEntry<>("website", "www.societegenerale.fr"), new SimpleEntry<>("country", country1),
				new SimpleEntry<>("address", "1 rue de la République"), new SimpleEntry<>("city", city1));
		
		DCResource company2 = buildResource(MarkaInvestModel.COMPANY_MODEL_NAME,
				new SimpleEntry<>("id", "2"), new SimpleEntry<>("name", "Fidaudit GmbH"),
				new SimpleEntry<>("field", field2), new SimpleEntry<>("lastAnnualRevenue", 1500000d),
				new SimpleEntry<>("employeeNb", 200), new SimpleEntry<>("incorporationYear", 2002),
				new SimpleEntry<>("website", "www.fidaudit.de"), new SimpleEntry<>("country", country2),
				new SimpleEntry<>("address", "Paderborner Straße 2"), new SimpleEntry<>("city", city2));
		listCompany.add(company1);
		listCompany.add(company2);
		
		DCResource sector1 = buildResource(MarkaInvestModel.SECTOR_MODEL_NAME,
				new SimpleEntry<>("id", "1"),new SimpleEntry<>("name", "Financial"));
		DCResource sector2 = buildResource(MarkaInvestModel.SECTOR_MODEL_NAME,
				new SimpleEntry<>("id", "2"),new SimpleEntry<>("name", "Services"));
		listSector.add(sector1);
		listSector.add(sector2);
		
		List<DCResource> listCompanyUser1 = new ArrayList<>();
		listCompanyUser1.add(company1);
		DCResource user1 = buildResource(MarkaInvestModel.USER_MODEL_NAME,
				new SimpleEntry<>("id", "1"), new SimpleEntry<>("firstName", "Frédéric"),
				new SimpleEntry<>("lastName", "Oudéa"), new SimpleEntry<>("companies", listCompanyUser1),
				new SimpleEntry<>("email", "frederic.oudea@socgen.com"),
				new SimpleEntry<>("tel", "0142143059"), new SimpleEntry<>("fax", "0142143000"));
		List<DCResource> listCompanyUser2 = new ArrayList<>();
		listCompanyUser2.add(company1);
		listCompanyUser2.add(company2);
		DCResource user2 = buildResource(MarkaInvestModel.USER_MODEL_NAME,
				new SimpleEntry<>("id", "1"), new SimpleEntry<>("firstName", "Albert"),
				new SimpleEntry<>("lastName", "Milte"), new SimpleEntry<>("companies", listCompanyUser2),
				new SimpleEntry<>("email", "albert.milte@gmail.com"),
				new SimpleEntry<>("tel", "08445125647"), new SimpleEntry<>("fax", "08445125640"));
		listUser.add(user1);
		listUser.add(user2);
		
		DCResource investorType1 = buildResource(MarkaInvestModel.INVESTOR_TYPE_MODEL_NAME,
				new SimpleEntry<>("id", "1"), new SimpleEntry<>("code", "BA"), new SimpleEntry<>("description", "Business Angel"));
		listInvestorType.add(investorType1);
		
		List<DCResource> listSectorsUser1 = new ArrayList<>();
		listSectorsUser1.add(sector1);
		listSectorsUser1.add(sector2);
		DCResource investor1 = buildResource(MarkaInvestModel.INVESTOR_MODEL_NAME,
				new SimpleEntry<>("id", "1"), new SimpleEntry<>("user", user1),
				new SimpleEntry<>("type", listInvestorType), new SimpleEntry<>("fundsAvailable", 1000000d),
				new SimpleEntry<>("sectors", sector2));
		listInvestor.add(investor1);
		
		DCResource cost1 = buildResource(MarkaInvestModel.COST_TYPE_MODEL_NAME,
				new SimpleEntry<>("id", "1"), new SimpleEntry<>("name", "money"));
		DCResource cost2 = buildResource(MarkaInvestModel.COST_TYPE_MODEL_NAME,
				new SimpleEntry<>("id", "2"), new SimpleEntry<>("name", "human resources"));
		listCost.add(cost1);
		listCost.add(cost2);
		
		DateFormat formatter = new SimpleDateFormat("dd/MM/yy");
		Date end = new Date();
		Date start = new Date();
		try {
			end = formatter.parse("04/01/2014");
			start = formatter.parse("05/10/2013");
		} catch (ParseException e) {}
		DCResource plannedInvestmentRequest1 = buildResource(MarkaInvestModel.INVESTMENT_ASSISTANCE_REQUEST_MODEL_NAME,
				new SimpleEntry<>("id", "1"), new SimpleEntry<>("sectors", listSectorsUser1),
				new SimpleEntry<>("company", company1), new SimpleEntry<>("fundRequired", 521000d),
				new SimpleEntry<>("start", start), new SimpleEntry<>("end", end));
		listPlannedInvestmentAssistanceRequest.add(plannedInvestmentRequest1);

		mapData.put(MarkaInvestModel.COMPANY_MODEL_NAME, listCompany);
		mapData.put(MarkaInvestModel.FIELD_MODEL_NAME, listField);
		mapData.put(MarkaInvestModel.SECTOR_MODEL_NAME, listSector);
		mapData.put(MarkaInvestModel.COUNTRY_MODEL_NAME, listCountry);
		mapData.put(MarkaInvestModel.CITY_MODEL_NAME, listCity);
		mapData.put(MarkaInvestModel.USER_MODEL_NAME, listUser);
		mapData.put(MarkaInvestModel.INVESTOR_MODEL_NAME, listInvestor);
		mapData.put(MarkaInvestModel.INVESTOR_TYPE_MODEL_NAME, listInvestorType);
		mapData.put(MarkaInvestModel.COST_TYPE_MODEL_NAME, listCost);
		mapData.put(MarkaInvestModel.INVESTMENT_ASSISTANCE_REQUEST_MODEL_NAME, listPlannedInvestmentAssistanceRequest);		
				
	}
	

	private void insertData() {
		
		api.putAllData(listCity);
		api.putAllData(listCompany);
		api.putAllData(listCost);
		api.putAllData(listCountry);
		api.putAllData(listField);
		api.putAllData(listInvestor);
		api.putAllData(listInvestorType);
		api.putAllData(listPlannedInvestmentAssistanceRequest);
		api.putAllData(listSector);
		api.putAllData(listUser);
		
	}

	public HashMap<String, List<DCResource>> getData() {
		return mapData;
	}
	
}
