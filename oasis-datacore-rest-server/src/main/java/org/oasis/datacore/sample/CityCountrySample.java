package org.oasis.datacore.sample;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCI18nField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
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
   public static String CITYLEGALINFO_MIXIN_NAME = "sample.city.city.legalInfo";

   @Override
   public void buildModels(List<DCModelBase> modelsToCreate) {
      DCModel countryModel = new DCModel(COUNTRY_MODEL_NAME);
      countryModel.setDocumentation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");
      countryModel.addField(new DCField("n:name", "string", true, 100));
      
      DCModel poiModel = new DCModel(POI_MODEL_NAME);
      poiModel.addField(new DCField("n:name", "string", true, 50));
      poiModel.addField(new DCField("poi:description", "string"));
      poiModel.addField(new DCField("poi:location", "string")); // wkt
      
      DCModel cityModel = new DCModel(CITY_MODEL_NAME);
      cityModel.setDocumentation("{ \"uri\": \"http://localhost:8080/dc/type/city/France/Lyon\", "
            + "\"inCountry\": \"http://localhost:8080/dc/type/country/France\", \"name\": \"Lyon\" }");
      cityModel.addField(new DCField("n:name", "string", true, 100));
      cityModel.addField(new DCResourceField("city:inCountry", COUNTRY_MODEL_NAME, true, 100));
      cityModel.addField(new DCField("city:populationCount", "int", false, 50));
      cityModel.addField(new DCField("city:longitudeDMS", "double", false, 0));
      cityModel.addField(new DCField("city:latitudeDMS", "double", false, 0));
      cityModel.addField(new DCField("city:founded", "date", false, 0));
      cityModel.addField(new DCField("city:isComCom", "boolean", (Object) false, 0));
         
      // i18n sample :
      cityModel.addField(new DCI18nField("i18n:name", 100));
      // more complete Resource sample (may be used also as embedded referenced Resource) :
      cityModel.addField(new DCListField("city:pointsOfInterest",
            new DCResourceField("zzz", POI_MODEL_NAME))); // sample of
      // embedded referencing Resource (city:pointsOfInterest) using POI model :
      // numéro de version, uri atteignable
      // BUT BEWARE may not be up to date (see version) nor complete (TODO mixins deriving from models i.e. less fields BUT with same uri i.e. vue)

      // embedded Resource (map mixin) sample : pas de numéro de version, uri pas utilisable
      DCMixin cityLegalInfoMixin = new DCMixin(CITYLEGALINFO_MIXIN_NAME);
      cityLegalInfoMixin.addField(new DCField("cityli:legalInfo1", "string"));
      cityLegalInfoMixin.addField(new DCField("cityli:legalInfo2", "string"));
      cityModel.addField(new DCResourceField("city:legalInfo", CITYLEGALINFO_MIXIN_NAME));
      ///cityModel.addMixin(cityLegalInfoMixin);

      modelsToCreate.addAll(Arrays.asList((DCModelBase) poiModel, cityModel, countryModel, cityLegalInfoMixin));
   }

   public void fillData() {
      // cleaning data first (else Conflict ?!)
      //////////////////cleanDataOfCreatedModels();
      
      DCResource franceCountry = resourceService.create(COUNTRY_MODEL_NAME, "France")
            .set("n:name", "France");
      DCResource ukCountry = resourceService.create(COUNTRY_MODEL_NAME, "UK")
            .set("n:name", "UK");
      DCResource italiaCountry = resourceService.create(COUNTRY_MODEL_NAME, "Italia")
            .set("n:name", "Italia");
      DCResource russiaCountry = resourceService.create(COUNTRY_MODEL_NAME, "Russia")
            .set("n:name", "Russia");
      franceCountry = postDataInType(franceCountry);
      ukCountry = postDataInType(ukCountry);
      italiaCountry = postDataInType(italiaCountry);
      russiaCountry = postDataInType(russiaCountry);

      DCResource view = resourceService.create(POI_MODEL_NAME, "Basilica_di_Superga").set("n:name", "Basilica di Superga");
      DCResource monument = resourceService.create(POI_MODEL_NAME, "Mole_Antonelliana").set("n:name", "Mole Antonelliana");
      postDataInType(view);
      postDataInType(monument);

      DCResource lyonCity = resourceService.create(CITY_MODEL_NAME, "France/Lyon")
            .set("n:name", "Lyon").set("city:inCountry", franceCountry.getUri())
            .set("city:populationCount", 500000)
            .set("city:longitudeDMS", new Float(500000.234).doubleValue()).set("city:latitudeDMS", "-500000"); // float & string as double
      DCResource londonCity = resourceService.create(CITY_MODEL_NAME, "UK/London")
            .set("n:name", "London").set("city:inCountry", ukCountry.getUri())
            .set("city:populationCount", 10000000)
            .set("city:longitudeDMS", 500000.234).set("city:latitudeDMS", -500000.234) // double
            .set("i18n:name", DCResource.listBuilder()
                  .add(DCResource.propertiesBuilder().put("@language", "fr").put("@value", "Londres").build())
                  .add(DCResource.propertiesBuilder().put("@language", "en").put("@value", "London").build())
                  .build());
      DCResource torinoCity = resourceService.create(CITY_MODEL_NAME, "Italia/Torino")
            .set("n:name", "Torino").set("city:inCountry", italiaCountry.getUri())
            .set("city:populationCount", 900000)
            .set("i18n:name", DCResource.listBuilder()
                  .add(DCResource.propertiesBuilder().put("@language", "fr").put("@value", "Turin").build())
                  .add(DCResource.propertiesBuilder().put("@language", "it").put("@value", "Torino").build())
                  .build())
            .set("city:pointsOfInterest", DCResource.listBuilder().add(view.getUri()).add(monument.getUri()).build());
      DCResource moscowCity = resourceService.create(CITY_MODEL_NAME, "Russia/Moscow")
            .set("n:name", "Moscow").set("city:inCountry", russiaCountry.getUri())
            .set("city:populationCount", 10000000).set("city:founded", new DateTime(1147, 4, 4, 0, 0, DateTimeZone.UTC).toString())
            .set("i18n:name", DCResource.listBuilder()
                  .add(DCResource.propertiesBuilder().put("@language", "fr").put("@value", "Moscou").build())
                  .add(DCResource.propertiesBuilder().put("@language", "en").put("@value", "Moscow").build())
                  .add(DCResource.propertiesBuilder().put("@language", "ru").put("@value", "Moskva").build())
                  .add(DCResource.propertiesBuilder().put("@language", "us").put("@value", "Moscow").build())
                  .build());
      
      lyonCity = postDataInType(lyonCity);
      londonCity = postDataInType(londonCity);
      torinoCity = postDataInType(torinoCity);
      moscowCity = postDataInType(moscowCity);
      
      // embedded referenced Resources sample :
      DCResource tourEiffelPoi = resourceService.create(POI_MODEL_NAME, "France/Paris/TourEiffel").set("n:name", "Tour Eiffel");
      DCResource louvresPoi = resourceService.create(POI_MODEL_NAME, "France/Paris/Louvres").set("n:name", "Louvres");
      DCResource parisCity = resourceService.create(CITY_MODEL_NAME, "France/Paris")
            .set("n:name", "Paris").set("city:inCountry", franceCountry.getUri())
            .set("city:populationCount", 1000000).set("city:legalInfo",
                  resourceService.create(CITYLEGALINFO_MIXIN_NAME, "France/Paris/city:legalInfo")
                  .set("cityli:legalInfo1", "Some Paris legal info"))
            .set("city:pointsOfInterest", DCResource.listBuilder()
                  .add(tourEiffelPoi).add(louvresPoi).build()); // .toMap()
      tourEiffelPoi = postDataInType(tourEiffelPoi);
      louvresPoi = postDataInType(louvresPoi);
      parisCity = postDataInType(parisCity);
      
   }
}
