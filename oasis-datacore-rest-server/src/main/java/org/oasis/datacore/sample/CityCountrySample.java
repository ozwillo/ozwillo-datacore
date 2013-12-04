package org.oasis.datacore.sample;

import javax.annotation.PostConstruct;

import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Used by tests & demo.
 * TODO better : rather use Bootstrappable, or ??
 * TODO only if fillWithSamples global boolean prop
 * @author mdutoo
 *
 */
@Component
public class CityCountrySample {
   
   public static String COUNTRY_MODEL_NAME = "sample.city.country";
   public static String CITY_MODEL_NAME = "sample.city.city";
   public static String POI_MODEL_NAME = "sample.city.pointOfInterest";

   /** impl, to be able to modify it
    * TODO LATER extract interface */
   @Autowired
   private DataModelServiceImpl modelAdminService;
   
   @PostConstruct
   public void init() {
      DCModel countryModel = new DCModel(COUNTRY_MODEL_NAME);
      countryModel.setDocumentation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");
      countryModel.addField(new DCField("name", "string", true, 100));
      
      DCModel poiModel = new DCModel(POI_MODEL_NAME);
      poiModel.setDocumentation("{ \"uri\": \"http://localhost:8080/dc/type/city/France/Lyon\", "
            + "\"inCountry\": \"http://localhost:8080/dc/type/country/France\", \"name\": \"France\" }");
      poiModel.addField(new DCField("name", "string", true, 50));
      poiModel.addField(new DCField("description", "string"));
      poiModel.addField(new DCField("location", "string")); // wkt
      
      DCModel cityModel = new DCModel(CITY_MODEL_NAME);
      cityModel.addField(new DCField("name", "string", true, 100));
      cityModel.addField(new DCField("inCountry", "resource", true, 100)); // TODO , countryModel.getName())));
      cityModel.addField(new DCField("populationCount", "int", false, 50));
      cityModel.addField(new DCField("founded", "date", false, 0));
      DCMapField name_18nMapField = new DCMapField("name_i18n"); // i18n
      name_18nMapField.addField(new DCField("fr", "string"));
      name_18nMapField.addField(new DCField("en", "string"));
      cityModel.addField(name_18nMapField);
      cityModel.addField(new DCListField("pointsOfInterest",
            //new DCResourceField("zzz", "resource", poiModel.getName()))); // TODO
            new DCField("zzz", "resource")));
      
      modelAdminService.addModel(poiModel);
      modelAdminService.addModel(cityModel);
      modelAdminService.addModel(countryModel);
   }
   
}
