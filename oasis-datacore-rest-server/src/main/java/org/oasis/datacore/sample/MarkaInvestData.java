package org.oasis.datacore.sample;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("markaInvestModel")
public class MarkaInvestData extends DatacoreSampleBase {

	private List<DCResource> listCompany = new ArrayList<DCResource>();
	private List<DCResource> listField = new ArrayList<DCResource>();
	private List<DCResource> listSector = new ArrayList<DCResource>();
	private List<DCResource> listCountry = new ArrayList<DCResource>();
	private List<DCResource> listCity = new ArrayList<DCResource>();
	private List<DCResource> listUser = new ArrayList<DCResource>();
	private List<DCResource> listInvestor = new ArrayList<DCResource>();
	private List<DCResource> listInvestorType = new ArrayList<DCResource>();
	private List<DCResource> listCost = new ArrayList<DCResource>();
	private List<DCResource> listPlannedInvestmentAssistanceRequest = new ArrayList<DCResource>();
	
	private HashMap<String, List<DCResource>> mapData = new HashMap<String, List<DCResource>>();;
	
	@Value("${datacoreApiServer.containerUrl}")
	private String containerUrl;
	
	@Value("#{new Boolean('${datacoreApiServer.enableMarkaSampleDataInsertionAtStartup}')}")
	private Boolean enableMarkaSampleDataInsertionAtStartup;

   /** to help clean data */
   @Autowired // (makes @DependsOn not necessary)
	private MarkaInvestModel markaInvestModel;

   @Override
   public void buildModels(List<DCModelBase> modelsToCreate) {
      
   }

   @Override
   public HashSet<DCModel> getCreatedModels() {
      return markaInvestModel.getCreatedModels();
   }
	
	@Override
	public void fillData() {
		
		createDataSample();
		
		if(enableMarkaSampleDataInsertionAtStartup) {
			insertData();
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
	
	public void createDataSample() {
		
		listField.clear();		
		DCResource field1 = buildResource(MarkaInvestModel.FIELD_MODEL_NAME, new SimpleEntry<>("id", 1),new SimpleEntry<>("name", "IT Services"));	
		DCResource field2 = buildResource(MarkaInvestModel.FIELD_MODEL_NAME, new SimpleEntry<>("id", 2),new SimpleEntry<>("name", "Accounting"));	
		DCResource field3 = buildResource(MarkaInvestModel.FIELD_MODEL_NAME, new SimpleEntry<>("id", 3),new SimpleEntry<>("name", "Banking"));	
		listField.add(field1);
		listField.add(field2);
		listField.add(field3);
		
		listCountry.clear();
		DCResource country1 = buildResource(MarkaInvestModel.COUNTRY_MODEL_NAME,
				new SimpleEntry<>("id", 1),new SimpleEntry<>("name", "France"),
				new SimpleEntry<>("lat", 46f), new SimpleEntry<>("long", 2l),
				new SimpleEntry<>("population", 65000000), new SimpleEntry<>("language", "FR_fr"));
		
		DCResource country2 = buildResource(MarkaInvestModel.COUNTRY_MODEL_NAME,
				new SimpleEntry<>("id", 2),new SimpleEntry<>("name", "Deutschland"),
				new SimpleEntry<>("lat", 50f), new SimpleEntry<>("long", 15l),
				new SimpleEntry<>("population", 55000000), new SimpleEntry<>("language", "De_de"));
		listCountry.add(country1);
		listCountry.add(country2);
		
		listCity.clear();
		List<String> listPostalCodeCity1 = new ArrayList<>();
		listPostalCodeCity1.add("69001");
		listPostalCodeCity1.add("69002");
		listPostalCodeCity1.add("69003");
		DCResource city1 = buildResource(MarkaInvestModel.CITY_MODEL_NAME,
				new SimpleEntry<>("id", 1),new SimpleEntry<>("name", "Lyon"),
				new SimpleEntry<>("population", 2000000), new SimpleEntry<>("postalCodes", listPostalCodeCity1),
				new SimpleEntry<>("lat", 61f), new SimpleEntry<>("long", 12l), new SimpleEntry<>("country", country1.getUri()));
		
		List<String> listPostalCodeCity2 = new ArrayList<>();
		listPostalCodeCity2.add("10243");
		listPostalCodeCity2.add("10245");
		listPostalCodeCity2.add("10247");
		DCResource city2 = buildResource(MarkaInvestModel.CITY_MODEL_NAME,
				new SimpleEntry<>("id", 2),new SimpleEntry<>("name", "Berlin"),
				new SimpleEntry<>("population", 1000000), new SimpleEntry<>("postalCodes", listPostalCodeCity2),
				new SimpleEntry<>("lat", 75f), new SimpleEntry<>("long", 35l), new SimpleEntry<>("country", country2.getUri()));
		listCity.add(city1);
		listCity.add(city2);
		
		listCompany.clear();
		DCResource company1 = buildResource(MarkaInvestModel.COMPANY_MODEL_NAME,
				new SimpleEntry<>("id", 1), new SimpleEntry<>("name", "Societe Generale"),
				new SimpleEntry<>("field", field3.getUri()), new SimpleEntry<>("lastAnnualRevenue", 956210000f),
				new SimpleEntry<>("employeeNb", 35000), new SimpleEntry<>("incorporationYear", 1975),
				new SimpleEntry<>("website", "www.societegenerale.fr"), new SimpleEntry<>("country", country1.getUri()),
				new SimpleEntry<>("address", "1 rue de la République"), new SimpleEntry<>("city", city1.getUri()));
		
		DCResource company2 = buildResource(MarkaInvestModel.COMPANY_MODEL_NAME,
				new SimpleEntry<>("id", 2), new SimpleEntry<>("name", "Fidaudit GmbH"),
				new SimpleEntry<>("field", field2.getUri()), new SimpleEntry<>("lastAnnualRevenue", 1500000f),
				new SimpleEntry<>("employeeNb", 200), new SimpleEntry<>("incorporationYear", 2002),
				new SimpleEntry<>("website", "www.fidaudit.de"), new SimpleEntry<>("country", country2.getUri()),
				new SimpleEntry<>("address", "Paderborner Straße 2"), new SimpleEntry<>("city", city2.getUri()));
		listCompany.add(company1);
		listCompany.add(company2);
		
		listSector.clear();
		DCResource sector1 = buildResource(MarkaInvestModel.SECTOR_MODEL_NAME,
				new SimpleEntry<>("id", 1),new SimpleEntry<>("name", "Financial"));
		DCResource sector2 = buildResource(MarkaInvestModel.SECTOR_MODEL_NAME,
				new SimpleEntry<>("id", 2),new SimpleEntry<>("name", "Services"));
		listSector.add(sector1);
		listSector.add(sector2);
		
		listUser.clear();
		List<String> listCompanyUser1 = new ArrayList<>();
		listCompanyUser1.add(company1.getUri());
		DCResource user1 = buildResource(MarkaInvestModel.USER_MODEL_NAME,
				new SimpleEntry<>("id", 1), new SimpleEntry<>("firstName", "Frédéric"),
				new SimpleEntry<>("lastName", "Oudéa"), new SimpleEntry<>("companies", listCompanyUser1),
				new SimpleEntry<>("email", "frederic.oudea@socgen.com"),
				new SimpleEntry<>("tel", "0142143059"), new SimpleEntry<>("fax", "0142143000"));
		List<String> listCompanyUser2 = new ArrayList<>();
		listCompanyUser2.add(company1.getUri());
		listCompanyUser2.add(company2.getUri());
		DCResource user2 = buildResource(MarkaInvestModel.USER_MODEL_NAME,
				new SimpleEntry<>("id", 2), new SimpleEntry<>("firstName", "Albert"),
				new SimpleEntry<>("lastName", "Milte"), new SimpleEntry<>("companies", listCompanyUser2),
				new SimpleEntry<>("email", "albert.milte@gmail.com"),
				new SimpleEntry<>("tel", "08445125647"), new SimpleEntry<>("fax", "08445125640"));
		listUser.add(user1);
		listUser.add(user2);
		
		listInvestorType.clear();
		DCResource investorType1 = buildResource(MarkaInvestModel.INVESTOR_TYPE_MODEL_NAME,
				new SimpleEntry<>("id", 1), new SimpleEntry<>("code", "BA"), new SimpleEntry<>("description", "Business Angel"));
		listInvestorType.add(investorType1);
		
		listInvestor.clear();
		List<String> listSectorsUser1 = new ArrayList<>();
		listSectorsUser1.add(sector1.getUri());
		listSectorsUser1.add(sector2.getUri());
		List<String> listInvestorTypeUser1 = new ArrayList<>();
		listInvestorTypeUser1.add(investorType1.getUri());
		DCResource investor1 = buildResource(MarkaInvestModel.INVESTOR_MODEL_NAME,
				new SimpleEntry<>("id", 2), new SimpleEntry<>("user", user1.getUri()),
				new SimpleEntry<>("types", listInvestorTypeUser1), new SimpleEntry<>("fundsAvailable", 1000000f),
				new SimpleEntry<>("sectors", listSectorsUser1));
		listInvestor.add(investor1);
		
		listCost.clear();
		DCResource cost1 = buildResource(MarkaInvestModel.COST_TYPE_MODEL_NAME,
				new SimpleEntry<>("id", 1), new SimpleEntry<>("name", "money"));
		DCResource cost2 = buildResource(MarkaInvestModel.COST_TYPE_MODEL_NAME,
				new SimpleEntry<>("id", 2), new SimpleEntry<>("name", "human resources"));
		listCost.add(cost1);
		listCost.add(cost2);

		listPlannedInvestmentAssistanceRequest.clear();

		String startIso = "2013-07-31T13:42:43.040+0000";
		String endIso = "2013-07-31T13:42:43.040+0000";
				
		DCResource plannedInvestmentRequest1 = buildResource(MarkaInvestModel.INVESTMENT_ASSISTANCE_REQUEST_MODEL_NAME,
				new SimpleEntry<>("id", 1), new SimpleEntry<>("sectors", listSectorsUser1),
				new SimpleEntry<>("company", company1.getUri()), new SimpleEntry<>("fundRequired", 521000f),
				new SimpleEntry<>("start", startIso), new SimpleEntry<>("end", endIso));
		listPlannedInvestmentAssistanceRequest.add(plannedInvestmentRequest1);

		mapData.clear();
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
	
	public void insertData() {
		
		try {
			datacoreApiImpl.postAllDataInType(listField, MarkaInvestModel.FIELD_MODEL_NAME);
		} catch (WebApplicationException e) {
		   listField = (List<DCResource>) e.getResponse().getEntity();
      }
		try {
			datacoreApiImpl.postAllDataInType(listCountry, MarkaInvestModel.COUNTRY_MODEL_NAME);
      } catch (WebApplicationException e) {
         listCountry = (List<DCResource>) e.getResponse().getEntity();
      }
		try {
			datacoreApiImpl.postAllDataInType(listCity, MarkaInvestModel.CITY_MODEL_NAME);
      } catch (WebApplicationException e) {
         listCity = (List<DCResource>) e.getResponse().getEntity();
      }
		try {
			datacoreApiImpl.postAllDataInType(listCompany, MarkaInvestModel.COMPANY_MODEL_NAME);
      } catch (WebApplicationException e) {
         listCompany = (List<DCResource>) e.getResponse().getEntity();
      }
		try {
			datacoreApiImpl.postAllDataInType(listSector, MarkaInvestModel.SECTOR_MODEL_NAME);
      } catch (WebApplicationException e) {
         listSector = (List<DCResource>) e.getResponse().getEntity();
      }
		try {
			datacoreApiImpl.postAllDataInType(listUser, MarkaInvestModel.USER_MODEL_NAME);
      } catch (WebApplicationException e) {
         listUser = (List<DCResource>) e.getResponse().getEntity();
      }
		try {
			datacoreApiImpl.postAllDataInType(listInvestorType, MarkaInvestModel.INVESTOR_TYPE_MODEL_NAME);
      } catch (WebApplicationException e) {
         listInvestorType = (List<DCResource>) e.getResponse().getEntity();
      }
		try {
			datacoreApiImpl.postAllDataInType(listInvestor, MarkaInvestModel.INVESTOR_MODEL_NAME);
      } catch (WebApplicationException e) {
         listInvestor = (List<DCResource>) e.getResponse().getEntity();
      }
		try {
			datacoreApiImpl.postAllDataInType(listCost, MarkaInvestModel.COST_TYPE_MODEL_NAME);
      } catch (WebApplicationException e) {
         listCost = (List<DCResource>) e.getResponse().getEntity();
      }
		try {
			datacoreApiImpl.postAllDataInType(listPlannedInvestmentAssistanceRequest, MarkaInvestModel.INVESTMENT_ASSISTANCE_REQUEST_MODEL_NAME);
      } catch (WebApplicationException e) {
         listPlannedInvestmentAssistanceRequest = (List<DCResource>) e.getResponse().getEntity();
      }
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

	public HashMap<String, List<DCResource>> getData() {
		return mapData;
	}

	public String getContainerUrl() {
		return containerUrl;
	}

	public void setContainerUrl(String containerUrl) {
		this.containerUrl = containerUrl;
	}

	public Boolean getEnableMarkaSampleDataInsertionAtStartup() {
		return enableMarkaSampleDataInsertionAtStartup;
	}

	public void setEnableMarkaSampleDataInsertionAtStartup(Boolean enableMarkaSampleDataInsertionAtStartup) {
		this.enableMarkaSampleDataInsertionAtStartup = enableMarkaSampleDataInsertionAtStartup;
	}
	
}
