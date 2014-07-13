package org.oasis.datacore.sample;

import java.util.Arrays;
import java.util.List;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.server.event.DCInitIdEventListener;
import org.springframework.stereotype.Component;


/**
 * Used by tests & demo.
 * @author mdutoo
 *
 */
@Component
public class AltTourismPlaceAddressSample extends DatacoreSampleBase {

   public static final String OASIS_ADDRESS = "oasis.address";
   
   public static final String MY_APP_PLACE = "my.app.place";

   public static final String ALTTOURISM_PLACEKIND = "altTourism.placeKind";
   public static final String ALTTOURISM_PLACE = "altTourism.place";
   

   @Override
   public void buildModels(List<DCModelBase> modelsToCreate) {
      // Mixin for shared fields - use 1
      DCModel myAppPlaceAddress = new DCModel(MY_APP_PLACE);
      myAppPlaceAddress.addField(new DCField("name", "string", true, 100)); // my.app.place.name ?!

      // Mixin for shared fields - use 2
      DCModel altTourismPlaceKind = new DCModel(AltTourismPlaceAddressSample.ALTTOURISM_PLACEKIND);
      altTourismPlaceKind.addField(new DCField("name", "string", true, 100));
      ///altTourismPlaceKind.addListener(eventService.init(new DCInitIdEventListener(AltTourismPlaceAddressSample.ALTTOURISM_PLACEKIND, "name"))); // TODO null
      DCModel altTourismPlace = new DCModel(AltTourismPlaceAddressSample.ALTTOURISM_PLACE);
      altTourismPlace.getSecurity().addResourceCreator("model_resource_creator_altTourism.place");
      altTourismPlace.addField(new DCField("name", "string", true, 100));
      altTourismPlace.addField(new DCResourceField("kind", AltTourismPlaceAddressSample.ALTTOURISM_PLACEKIND, true, 100)); // hotel... ; alternativeTourismPlaceKind
      eventService.init(new DCInitIdEventListener(AltTourismPlaceAddressSample.ALTTOURISM_PLACEKIND, "name"));
      
      // Mixin for shared fields
      DCMixin oasisAddress = new DCMixin(OASIS_ADDRESS);
      oasisAddress.addField(new DCField("streetAndNumber", "string", false, 100)); // my.app.place.address.streetAndNumber ?!
      oasisAddress.addField(new DCField("zipCode", "string", true, 100)); // "string" for cedex / po box
      oasisAddress.addField(new DCField("cityName", "string", false, 100)); // OR only resource ??
      oasisAddress.addField(new DCResourceField("city", "city", false, 100)); // TODO create city model, or reuse sample ?!?
      oasisAddress.addField(new DCField("countryName", "string", false, 100)); // OR only resource ??
      oasisAddress.addField(new DCResourceField("country", "country", false, 100)); // TODO create country model, or reuse sample ?!?

      modelsToCreate.addAll(Arrays.asList(myAppPlaceAddress, altTourismPlaceKind, altTourismPlace, oasisAddress));
   }
   
   /**
    * BEWARE changes model, models must be reinited first in tests
    */
   @Override
   public void fillData() {
      // Mixin for shared fields - use 1
      //MY_APP_PLACE
      
      DCResource myAppPlace1 = resourceService.create(MY_APP_PLACE, "my_place_1").set("name", "my_place_1");
      
      DCResource myAppPlace1Posted = /*datacoreApiClient.*/postDataInType(myAppPlace1);
      
      // step 2 - now adding mixin
      modelAdminService.getModel(MY_APP_PLACE).addMixin(modelAdminService.getMixin(OASIS_ADDRESS));
      ///modelAdminService.addModel(myAppPlaceAddress); // LATER re-add...

      // check, at update :
      // (field without mixin would have failed) 
      myAppPlace1Posted.set("zipCode", "69100");
      DCResource myAppPlace1Put = /*datacoreApiClient.*/putDataInType(myAppPlace1Posted);

      // check, same but already at creation :
      DCResource myAppPlace2 = resourceService.create(MY_APP_PLACE, "my_place_2").set("name", "my_place_2")
            .set("zipCode", "69100");
      DCResource myAppPlace2Posted = /*datacoreApiClient.*/postDataInType(myAppPlace2);
      
      
      // Mixin for shared fields - use 2
      //ALTTOURISM_PLACEKIND

      DCResource altTourismHotelKind = resourceService.create(ALTTOURISM_PLACEKIND, "hotel").set("name", "hotel");
      altTourismHotelKind = /*datacoreApiClient.*/postDataInType(altTourismHotelKind);

      DCResource altTourismWineryKind = resourceService.create(ALTTOURISM_PLACEKIND, "winery").set("name", "winery");
      altTourismWineryKind = /*datacoreApiClient.*/postDataInType(altTourismWineryKind);
      DCResource altTourismPlaceJoWinery = resourceService.create(ALTTOURISM_PLACE, "Jo_Winery")
            .set("name", "Jo_Winery").set("kind", altTourismWineryKind.getUri());
      DCResource altTourismPlaceJoWineryPosted = /*datacoreApiClient.*/postDataInType(altTourismPlaceJoWinery);
         
      // step 2 - now adding mixin
      modelAdminService.getModel(ALTTOURISM_PLACE).addMixin(modelAdminService.getMixin(OASIS_ADDRESS));
      ///modelAdminService.addModel(altTourismPlace); // LATER re-add...
      
      // (field without mixin would have failed) same but already at creation :
      DCResource altTourismMonasteryKind = resourceService.create(ALTTOURISM_PLACEKIND, "monastery").set("name", "monastery");
      altTourismMonasteryKind = /*datacoreApiClient.*/postDataInType(altTourismMonasteryKind);
      DCResource altTourismPlaceSofiaMonastery = resourceService.create(ALTTOURISM_PLACE, "Sofia_Monastery")
            .set("name", "Sofia_Monastery").set("kind", altTourismMonasteryKind.getUri())
            .set("zipCode", "1000");

      // check, at update :
      // (field without mixin would have failed)
      altTourismPlaceJoWineryPosted.set("zipCode", "1000");
      DCResource altTourismPlaceJoWineryPut = /*datacoreApiClient.*/putDataInType(altTourismPlaceJoWineryPosted);

      // check, same but already at creation :
      DCResource altTourismPlaceSofiaMonasteryPosted = /*datacoreApiClient.*/postDataInType(altTourismPlaceSofiaMonastery);
      
      
      
      //datacoreApiClient.postDataInType(altTourismPlaceSofiaMonasteryPosted);/// NOO works SAVE on appserver (tomcat)
   }
   
}
