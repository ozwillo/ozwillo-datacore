package org.oasis.datacore.sample;

import java.util.ArrayList;
import java.util.List;

import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Marka Investment Model
 * @author agiraudon
 */

@Component
public class MarkaInvestModel extends DatacoreSampleBase {
	
	public static String COMPANY_MODEL_NAME = "sample.marka.company";
	public static String FIELD_MODEL_NAME = "sample.marka.field";
	public static String SECTOR_MODEL_NAME = "sample.marka.sector";
	public static String COUNTRY_MODEL_NAME = "sample.marka.country";
	public static String CITY_MODEL_NAME = "sample.marka.city";
	public static String USER_MODEL_NAME = "sample.marka.user";
	public static String INVESTOR_MODEL_NAME = "sample.marka.investor";
	public static String INVESTOR_TYPE_MODEL_NAME = "sample.marka.investor_type";
	public static String COST_TYPE_MODEL_NAME = "sample.marka.cost";
	public static String INVESTMENT_ASSISTANCE_REQUEST_MODEL_NAME = "sample.marka.planned_investment_assistance_request";

	private List<DCModel> listModel = new ArrayList<>(); 
	
	@Autowired
	private DataModelServiceImpl modelAdminService;
	
	@Override
	public void doInit() {
				
		DCModel companyModel = new DCModel(COMPANY_MODEL_NAME);
		companyModel.addField(new DCField("id", DCFieldTypeEnum.INTEGER.getType(), true, 100));
		companyModel.addField(new DCField("name", DCFieldTypeEnum.STRING.getType(), true, 100));
		companyModel.addField(new DCResourceField("field", FIELD_MODEL_NAME));
		companyModel.addField(new DCField("lastAnnualRevenue", DCFieldTypeEnum.FLOAT.getType()));
		companyModel.addField(new DCField("employeeNb", DCFieldTypeEnum.INTEGER.getType()));
		companyModel.addField(new DCField("incorporationYear", DCFieldTypeEnum.INTEGER.getType()));
		companyModel.addField(new DCField("website", DCFieldTypeEnum.STRING.getType()));
		companyModel.addField(new DCResourceField("country", COUNTRY_MODEL_NAME));
		companyModel.addField(new DCField("address", DCFieldTypeEnum.STRING.getType()));
		companyModel.addField(new DCResourceField("city", CITY_MODEL_NAME));
		companyModel.setHistorizable(true);
		companyModel.getSecurity().addAdmin("model_admin_sample.marka.company");
		listModel.add(companyModel);
		
		DCModel fieldModel = new DCModel(FIELD_MODEL_NAME);
		fieldModel.addField(new DCField("id", DCFieldTypeEnum.INTEGER.getType(), true, 100));
		fieldModel.addField(new DCField("name", DCFieldTypeEnum.STRING.getType(), true, 100));
		fieldModel.setContributable(true);
		fieldModel.getSecurity().addReader("model_readers_sample.marka.field");
		listModel.add(fieldModel);

		DCModel sectorModel = new DCModel(SECTOR_MODEL_NAME);
		sectorModel.addField(new DCField("id", DCFieldTypeEnum.INTEGER.getType(), true, 100));
		sectorModel.addField(new DCField("name", DCFieldTypeEnum.STRING.getType(), true, 100));
		listModel.add(sectorModel);
		
		DCModel countryModel = new DCModel(COUNTRY_MODEL_NAME);
		countryModel.addField(new DCField("id", DCFieldTypeEnum.INTEGER.getType(), true, 100));
		countryModel.addField(new DCField("name", DCFieldTypeEnum.STRING.getType(), true, 100));
		countryModel.addField(new DCField("lat", DCFieldTypeEnum.FLOAT.getType()));
		countryModel.addField(new DCField("long", DCFieldTypeEnum.FLOAT.getType()));
		countryModel.addField(new DCField("population", DCFieldTypeEnum.INTEGER.getType()));
		countryModel.addField(new DCField("language", DCFieldTypeEnum.STRING.getType()));
		listModel.add(countryModel);
		
		DCModel cityModel = new DCModel(CITY_MODEL_NAME);
		cityModel.addField(new DCField("id", DCFieldTypeEnum.INTEGER.getType(), true, 100));
		cityModel.addField(new DCField("name", DCFieldTypeEnum.STRING.getType(), true, 100));
		cityModel.addField(new DCField("population", DCFieldTypeEnum.INTEGER.getType()));
		cityModel.addField(new DCListField("postalCodes", new DCField("postalCode", DCFieldTypeEnum.STRING.getType())));
		cityModel.addField(new DCField("lat", DCFieldTypeEnum.FLOAT.getType()));
		cityModel.addField(new DCField("long", DCFieldTypeEnum.FLOAT.getType()));
		cityModel.addField(new DCResourceField("country", COUNTRY_MODEL_NAME));
		cityModel.getSecurity().addResourceAdmin("model_resource_admin_sample.marka.city");
		listModel.add(cityModel);
		
		DCModel userModel = new DCModel(USER_MODEL_NAME);
		userModel.addField(new DCField("id", DCFieldTypeEnum.INTEGER.getType(), true, 100));
		userModel.addField(new DCField("firstName", DCFieldTypeEnum.STRING.getType(), true, 100));
		userModel.addField(new DCField("lastName", DCFieldTypeEnum.STRING.getType(), true, 100));
		userModel.addField(new DCListField("companies", new DCResourceField("company", COMPANY_MODEL_NAME)));
		userModel.addField(new DCField("email", DCFieldTypeEnum.STRING.getType()));
		userModel.addField(new DCField("tel", DCFieldTypeEnum.STRING.getType()));
		userModel.addField(new DCField("fax", DCFieldTypeEnum.STRING.getType()));
		listModel.add(userModel);
		
		DCModel investorModel = new DCModel(INVESTOR_MODEL_NAME);
		investorModel.addField(new DCField("id", DCFieldTypeEnum.INTEGER.getType(), true, 100));
		investorModel.addField(new DCResourceField("user", USER_MODEL_NAME, true, 100));
		investorModel.addField(new DCListField("types", new DCResourceField("type", INVESTOR_TYPE_MODEL_NAME)));
		investorModel.addField(new DCField("fundsAvailable", DCFieldTypeEnum.FLOAT.getType()));
		investorModel.addField(new DCListField("sectors", new DCResourceField("sector", SECTOR_MODEL_NAME)));
		listModel.add(investorModel);
		
		DCModel investorTypeModel = new DCModel(INVESTOR_TYPE_MODEL_NAME);
		investorTypeModel.addField(new DCField("id", DCFieldTypeEnum.INTEGER.getType(), true, 100));
		investorTypeModel.addField(new DCField("code", DCFieldTypeEnum.STRING.getType(), true, 100));
		investorTypeModel.addField(new DCField("description", DCFieldTypeEnum.STRING.getType()));
		listModel.add(investorTypeModel);
		
		DCModel costModel = new DCModel(COST_TYPE_MODEL_NAME);
		costModel.addField(new DCField("id", DCFieldTypeEnum.INTEGER.getType(), true, 100));
		costModel.addField(new DCField("name", DCFieldTypeEnum.STRING.getType(), true, 100));
		listModel.add(costModel);
		
		DCModel plannedInvestmentAssistanceRequestModel = new DCModel(INVESTMENT_ASSISTANCE_REQUEST_MODEL_NAME);
		plannedInvestmentAssistanceRequestModel.addField(new DCField("id", DCFieldTypeEnum.INTEGER.getType(), true, 100));
		plannedInvestmentAssistanceRequestModel.addField(new DCListField("sectors", new DCResourceField("sector", SECTOR_MODEL_NAME)));
		plannedInvestmentAssistanceRequestModel.addField(new DCResourceField("company", COMPANY_MODEL_NAME));
		plannedInvestmentAssistanceRequestModel.addField(new DCField("fundRequired", DCFieldTypeEnum.FLOAT.getType()));
		plannedInvestmentAssistanceRequestModel.addField(new DCField("start", DCFieldTypeEnum.DATE.getType()));
		plannedInvestmentAssistanceRequestModel.addField(new DCField("end", DCFieldTypeEnum.DATE.getType()));
		listModel.add(plannedInvestmentAssistanceRequestModel);

	   createModelsAndCleanTheirData(companyModel, fieldModel, sectorModel, countryModel, cityModel, userModel, investorModel, investorTypeModel, costModel, plannedInvestmentAssistanceRequestModel);
	}

	public List<DCModel> getListModel() {
		return listModel;
	}

	public void setListModel(List<DCModel> listModel) {
		this.listModel = listModel;
	}
	
}
