package org.oasis.datacore.sample;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.DCResource;
import org.springframework.stereotype.Component;


/**
 * Used by tests & demo.
 * 
 * @author mdutoo
 *
 */
@Component
public class CityCountrySample extends DatacoreSampleBase {
   
   public static String COUNTRY_MODEL_NAME = "sample.city.country";
   public static String CITY_MODEL_NAME = "sample.city.city";
   public static String POI_MODEL_NAME = "sample.city.pointOfInterest";

   @Override
   public void doInit() {
      initModel();
      initData();
   }
   
   public void initModel() {
      DCModel countryModel = new DCModel(COUNTRY_MODEL_NAME);
      countryModel.setDocumentation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");
      countryModel.addField(new DCField("name", "string", true, 100));
      
      DCModel poiModel = new DCModel(POI_MODEL_NAME);
      poiModel.addField(new DCField("name", "string", true, 50));
      poiModel.addField(new DCField("description", "string"));
      poiModel.addField(new DCField("location", "string")); // wkt
      
      DCModel cityModel = new DCModel(CITY_MODEL_NAME);
      cityModel.setDocumentation("{ \"uri\": \"http://localhost:8080/dc/type/city/France/Lyon\", "
            + "\"inCountry\": \"http://localhost:8080/dc/type/country/France\", \"name\": \"Lyon\" }");
      cityModel.addField(new DCField("name", "string", true, 100));
      cityModel.addField(new DCResourceField("inCountry", COUNTRY_MODEL_NAME, true, 100));
      cityModel.addField(new DCField("populationCount", "int", false, 50));
      cityModel.addField(new DCField("founded", "date", false, 0));
      DCMapField name_18nMapField = new DCMapField("name_i18n"); // i18n
      name_18nMapField.addField(new DCField("fr", "string"));
      name_18nMapField.addField(new DCField("en", "string"));
      name_18nMapField.addField(new DCField("it", "string"));
      cityModel.addField(name_18nMapField);
      cityModel.addField(new DCListField("pointsOfInterest",
            new DCResourceField("zzz", POI_MODEL_NAME))); // TODO
            ////new DCField("zzz", "resource")));

      super.createModelsAndCleanTheirData(poiModel, cityModel, countryModel);
   }

   public void initData() {
      // cleaning data first (else Conflict ?!)
      cleanDataOfCreatedModels();
      
      DCResource franceCountry = resourceService.create(COUNTRY_MODEL_NAME, "France")
            .set("name", "France");
      DCResource ukCountry = resourceService.create(COUNTRY_MODEL_NAME, "UK")
            .set("name", "UK");
      DCResource italiaCountry = resourceService.create(COUNTRY_MODEL_NAME, "Italia")
            .set("name", "Italia");
      franceCountry = postDataInType(franceCountry);
      ukCountry = postDataInType(ukCountry);
      italiaCountry = postDataInType(italiaCountry);

      DCResource lyonCity = resourceService.create(CITY_MODEL_NAME, "France/Lyon")
            .set("name", "Lyon").set("inCountry", franceCountry.getUri())
            .set("populationCount", 500000);
      DCResource londonCity = resourceService.create(CITY_MODEL_NAME, "UK/London")
            .set("name", "London").set("inCountry", ukCountry.getUri())
            .set("populationCount", 10000000).set("name_i18n",
                  resourceService.propertiesBuilder().put("en", "London").put("fr", "Londres").build());
      DCResource torinoCity = resourceService.create(CITY_MODEL_NAME, "Italia/Torino")
            .set("name", "Torino").set("inCountry", italiaCountry.getUri())
            .set("populationCount", 900000).set("name_i18n",
                  resourceService.propertiesBuilder().put("it", "Torino").put("fr", "Turin").build());
      lyonCity = postDataInType(lyonCity);
      londonCity = postDataInType(londonCity);
      torinoCity = postDataInType(torinoCity);
      
   }
}
