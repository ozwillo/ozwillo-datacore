package org.oasis.datacore.sample;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCI18nField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.sample.meta.ProjectInitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;


/**
 * Used by tests & demo.
 * 
 * @author mdutoo
 *
 */
@Component
public class CityCountrySample extends DatacoreSampleBase {

   public static String COMMON_MODEL_NAME = "sample.city.common";
   public static String COUNTRY_MODEL_NAME = "sample.city.country";
   public static String CITY_MODEL_NAME = "sample.city.city";
   public static String POI_MODEL_NAME = "sample.city.pointOfInterest";
   public static String CITYLEGALINFO_MIXIN_NAME = "sample.city.city.legalInfo";

   @Autowired
   private ProjectInitService projectInitService;

   @Override
   public void buildModels(List<DCModelBase> modelsToCreate) {
      DCModel commonModel = new DCModel(COMMON_MODEL_NAME); // to test upper fields merge (because odisp:name is readonly in city)
      commonModel.addField(new DCField("common:id", "string", false, 0));

      DCModel countryModel = new DCModel(COUNTRY_MODEL_NAME);
      countryModel.setDocumentation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");
      //countryModel.addMixin(commonModel);
      countryModel.addField(new DCField("n:name", "string", true, 100));
      countryModel.getField("n:name").setFulltext(true); // string fulltext example ; NB. requires queryLimit > 0

      DCModel poiModel = new DCModel(POI_MODEL_NAME);
      poiModel.addField(new DCField("n:name", "string", true, 50));
      poiModel.addField(new DCField("poi:description", "string"));
      poiModel.addField(new DCField("poi:location", "string")); // wkt

      DCModel cityModel = new DCModel(CITY_MODEL_NAME);
      cityModel.setDocumentation("{ \"uri\": \"http://localhost:8080/dc/type/city/France/Lyon\", "
            + "\"inCountry\": \"http://localhost:8080/dc/type/country/France\", \"name\": \"Lyon\" }");
      cityModel.addMixin(commonModel);
      cityModel.addMixin(projectInitService.getMetamodelProject().getModel(ResourceModelIniter.MODEL_DISPLAYABLE_NAME));
      cityModel.addField(new DCField("n:name", "string", true, 100));
      cityModel.addField(new DCResourceField("city:inCountry", COUNTRY_MODEL_NAME, true, 100));
      cityModel.addField(new DCField("city:populationCount", "int", false, 50));
      cityModel.addField(new DCField("city:longitudeDMS", "double", false, 0));
      cityModel.addField(new DCField("city:latitudeDMS", "double", false, 0));
      cityModel.addField(new DCField("city:founded", "date", false, 0));
      cityModel.addField(new DCField("city:isComCom", "boolean", (Object) false, 0));

      // i18n & aliasedStorageNames sample :
      cityModel.addField(new DCI18nField(ResourceModelIniter.DISPLAYABLE_NAME_PROP, 100, ResourceModelIniter.DISPLAYABLE_NAME_PROP, true));
      cityModel.addField(new DCI18nField("i18n:name", 100, ResourceModelIniter.DISPLAYABLE_NAME_PROP, true));
      cityModel.getField("i18n:name").setFulltext(true); // i18n fulltext example ; NB. requires queryLimit > 0
      cityModel.addField(
          new DCI18nField("city:i18nname", 100,
              new LinkedHashSet<>(new ImmutableSet.Builder<String>().add(ResourceModelIniter.DISPLAYABLE_NAME_PROP).add("i18n:name").build()))
              .setDefaultLanguage("fr"));
      // more complete Resource sample (may be used also as embedded referenced Resource) :
      cityModel.getField("city:i18nname").setFulltext(true); // i18n fulltext example (REQUIRED because i18n readonly)
      cityModel.getField("city:i18nname").setRequired(true);
      cityModel.addField(new DCListField("city:pointsOfInterest",
            new DCResourceField("zzz", POI_MODEL_NAME))); // sample of
      // embedded referencing Resource (city:pointsOfInterest) using POI model :
      // numéro de version, uri atteignable
      // BUT BEWARE may not be up to date (see version) nor complete (TODO mixins deriving from models i.e. less fields BUT with same uri i.e. vue)

      // embedded Resource (map mixin) sample : pas de numéro de version, uri pas utilisable TODO LATER
      DCMixin cityLegalInfoMixin = new DCMixin(CITYLEGALINFO_MIXIN_NAME);
      cityLegalInfoMixin.addField(new DCField("cityli:legalInfoAuthor", "string", false, 100)); // used in list indexed field test
      cityLegalInfoMixin.addField(new DCField("cityli:legalInfo1", "string"));
      cityLegalInfoMixin.addField(new DCField("cityli:legalInfo2", "string"));
      cityLegalInfoMixin.setInstanciable(false); // embedded and not referencing
      cityModel.addField(new DCResourceField("city:legalInfo", CITYLEGALINFO_MIXIN_NAME));
      ///cityModel.addMixin(cityLegalInfoMixin);

      // map list (list of map mixin) sample : pas de numéro de version, uri pas utilisable
      cityModel.addField(new DCListField("city:historicalEvents", new DCMapField("zzz")
            .addField(new DCField("city:historicalEventDate", "date", true, 100))
            .addField(new DCI18nField("city:historicalEventName", 0))));

       // set id fields = [country, name], e.g. /France/Lyon and enforce that on update
      cityModel.setIdFieldNames(ImmutableList.of("city:inCountry", "city:i18nname"));
      cityModel.setEnforceIdFieldNames(true);

      modelsToCreate.addAll(Arrays.asList(poiModel, commonModel, cityModel, countryModel, cityLegalInfoMixin));
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
            .set("n:name", "Lyon")
            .set("city:inCountry", franceCountry.getUri())
            .set("city:populationCount", 500000)
            .set("city:longitudeDMS", new Float(500000.234).doubleValue())
            .set("city:latitudeDMS", "-500000") // float & string as double
            .set("city:i18nname", DCResource.listBuilder()
                 .add(DCResource.propertiesBuilder().put("@language", "fr").put("@value", "Lyon").build())
                 .add(DCResource.propertiesBuilder().put("@language", "en").put("@value", "Lyon").build())
                 .build());
      DCResource londonCity = resourceService.create(CITY_MODEL_NAME, "UK/London")
            .set("n:name", "London").set("city:inCountry", ukCountry.getUri())
            .set("city:populationCount", 10000000)
            .set("city:longitudeDMS", 500000.234).set("city:latitudeDMS", -500000.234) // double
            .set("city:i18nname", DCResource.listBuilder()
                  .add(DCResource.propertiesBuilder().put("@language", "fr").put("@value", "Londres").build())
                  .add(DCResource.propertiesBuilder().put("@language", "en").put("@value", "London").build())
                  .build());
      DCResource torinoCity = resourceService.create(CITY_MODEL_NAME, "Italia/Torino")
            .set("n:name", "Torino").set("city:inCountry", italiaCountry.getUri())
            .set("city:populationCount", 900000)
            .set("city:i18nname", DCResource.listBuilder()
                  .add(DCResource.propertiesBuilder().put("@language", "fr").put("@value", "Turin").build())
                  .add(DCResource.propertiesBuilder().put("@language", "it").put("@value", "Torino").build())
                  .build())
            .set("city:pointsOfInterest", DCResource.listBuilder().add(view.getUri()).add(monument.getUri()).build());

      DCResource moscowCity = resourceService.create(CITY_MODEL_NAME, "Russia/Moscow")
            .set("n:name", "Moscow").set("city:inCountry", russiaCountry.getUri())
            .set("city:populationCount", 10000000).set("city:founded", new DateTime(1147, 4, 4, 0, 0, DateTimeZone.UTC).toString())
            .set("city:i18nname", DCResource.listBuilder()
                  .add(DCResource.propertiesBuilder().put("@language", "fr").put("@value", "Moscou").build())
                  .add(DCResource.propertiesBuilder().put("@language", "en").put("@value", "Moscow").build())
                  .add(DCResource.propertiesBuilder().put("@language", "ru").put("@value", "Moskva").build())
                  .add(DCResource.propertiesBuilder().put("@language", "us").put("@value", "Moscow").build())
                  .build());
      // for fulltext ordering :
      DCResource saintEtienneCity = resourceService.create(CITY_MODEL_NAME, "France/Saint-Etienne")
            .set("n:name", "Saint-Etienne").set("city:inCountry", franceCountry.getUri())
            .set("city:i18nname", DCResource.listBuilder().add(DCResource.propertiesBuilder()
                  .put("l", "fr").put("v", "Saint-Etienne").build()).build());
      DCResource saintLoubertCity = resourceService.create(CITY_MODEL_NAME, "France/Saint-Loubert")
            .set("n:name", "Saint-Loubert").set("city:inCountry", franceCountry.getUri())
            .set("city:i18nname", DCResource.listBuilder().add(DCResource.propertiesBuilder()
                  .put("l", "fr").put("v", "Saint-Loubert").build()).build());
      DCResource saintLoCity = resourceService.create(CITY_MODEL_NAME, "France/Saint-Lô")
            .set("n:name", "Saint-Lô").set("city:inCountry", franceCountry.getUri())
            .set("city:i18nname", DCResource.listBuilder().add(DCResource.propertiesBuilder()
                  .put("@language", "fr").put("@value", "Saint-Lô").build()).build());
      DCResource saintLormelCity = resourceService.create(CITY_MODEL_NAME, "France/Saint-Lormel")
            .set("n:name", "Saint-Lormel").set("city:inCountry", franceCountry.getUri())
            .set("city:i18nname", DCResource.listBuilder().add(DCResource.propertiesBuilder()
                  .put("@language", "fr").put("@value", "Saint-Lormel").build()).build()); // entered after Saint-Lo so first in default order
      DCResource saintVallierCity = resourceService.create(CITY_MODEL_NAME, "France/Saint-Vallier")
            .set("n:name", "Saint-Vallier").set("city:inCountry", franceCountry.getUri())
            .set("city:i18nname", DCResource.listBuilder().add(DCResource.propertiesBuilder()
                  .put("@language", "fr").put("@value", "Saint-Vallier").build()).build());
      
      lyonCity = postDataInType(lyonCity);
      londonCity = postDataInType(londonCity);
      torinoCity = postDataInType(torinoCity);
      moscowCity = postDataInType(moscowCity);
      saintEtienneCity = postDataInType(saintEtienneCity);
      saintLoubertCity = postDataInType(saintLoubertCity);
      saintLoCity = postDataInType(saintLoCity);
      saintLormelCity = postDataInType(saintLormelCity);
      saintVallierCity = postDataInType(saintVallierCity);
      
      // embedded referenced Resources sample :
      DCResource tourEiffelPoi = resourceService.create(POI_MODEL_NAME, "France/Paris/TourEiffel").set("n:name", "Tour Eiffel");
      DCResource louvresPoi = resourceService.create(POI_MODEL_NAME, "France/Paris/Louvres").set("n:name", "Louvres");
      DCResource parisCity = resourceService.create(CITY_MODEL_NAME, "France/Paris")
            .set("n:name", "Paris").set("city:inCountry", franceCountry.getUri())
            .set("city:populationCount", 1000000).set("city:legalInfo",
                  resourceService.create(CITYLEGALINFO_MIXIN_NAME, "France/Paris/city:legalInfo")
                  .set("cityli:legalInfo1", "Some Paris legal info"))
            .set("city:pointsOfInterest", DCResource.listBuilder()
                  .add(tourEiffelPoi).add(louvresPoi).build()) // .toMap()
            .set("city:i18nname", DCResource.listBuilder().add(DCResource.propertiesBuilder()
                  .put("@language", "fr").put("@value", "Paris").build()).build());
      tourEiffelPoi = postDataInType(tourEiffelPoi);
      louvresPoi = postDataInType(louvresPoi);
      parisCity = postDataInType(parisCity);
      
   }
}
