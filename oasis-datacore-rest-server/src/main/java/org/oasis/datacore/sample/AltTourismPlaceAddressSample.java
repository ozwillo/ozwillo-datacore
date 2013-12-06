package org.oasis.datacore.sample;

import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.client.DatacoreClientApi;
import org.oasis.datacore.rest.server.ResourceService;
import org.oasis.datacore.rest.server.event.DCInitIdEventListener;
import org.oasis.datacore.rest.server.event.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;


/**
 * Used by tests & demo.
 * TODO better : rather use Bootstrappable, or ??
 * TODO only if fillWithSamples global boolean prop
 * @author mdutoo
 *
 */
@Component
//@DependsOn({"datacoreApiImpl", "datacoreApiServer"}) // else ConnectException NOO deadlock, & same for ApplicationContextAware
public class AltTourismPlaceAddressSample implements ApplicationListener<ContextRefreshedEvent> {

   public static final String OASIS_ADDRESS = "oasis.address";
   
   public static final String MY_APP_PLACE = "my.app.place";

   public static final String ALTTOURISM_PLACEKIND = "altTourism.placeKind";
   public static final String ALTTOURISM_PLACE = "altTourism.place";

   /** impl, to be able to modify it
    * TODO LATER extract interface */
   @Autowired
   private DataModelServiceImpl modelAdminService;

   @Autowired
   @Qualifier("datacoreApiCachedClient")
   private /*DatacoreApi*/DatacoreClientApi datacoreApiClient;
   
   @Autowired
   private ResourceService resourceService;

   @Autowired
   private EventService eventService;
   
   /** to cleanup db
    * TODO LATER rather in service */
   @Autowired
   private /*static */MongoOperations mgo;
   

   @Override
   public void onApplicationEvent(ContextRefreshedEvent event) {
      this.initModel();
      this.initData();
   }
   
   //@PostConstruct // NOO deadlock, & same for ApplicationContextAware
   public void initModel() {

      // Mixin for shared fields - use 1
      DCModel myAppPlaceAddress = new DCModel(MY_APP_PLACE);
      myAppPlaceAddress.addField(new DCField("name", "string", true, 100)); // my.app.place.name ?!
      modelAdminService.addModel(myAppPlaceAddress);

      // Mixin for shared fields - use 2
      DCModel altTourismPlaceKind = new DCModel(AltTourismPlaceAddressSample.ALTTOURISM_PLACEKIND);
      altTourismPlaceKind.addField(new DCField("name", "string", true, 100));
      altTourismPlaceKind.addListener(eventService.initialize(new DCInitIdEventListener(AltTourismPlaceAddressSample.ALTTOURISM_PLACEKIND, "name"))); // TODO null
      DCModel altTourismPlace = new DCModel(AltTourismPlaceAddressSample.ALTTOURISM_PLACE);
      altTourismPlace.addField(new DCField("name", "string", true, 100));
      altTourismPlace.addField(new DCResourceField("kind", AltTourismPlaceAddressSample.ALTTOURISM_PLACEKIND, true, 100)); // hotel... ; alternativeTourismPlaceKind
      modelAdminService.addModel(altTourismPlaceKind);
      modelAdminService.addModel(altTourismPlace);
      
      // Mixin for shared fields
      DCMixin oasisAddress = new DCMixin(OASIS_ADDRESS);
      oasisAddress.addField(new DCField("streetAndNumber", "string", false, 100)); // my.app.place.address.streetAndNumber ?!
      oasisAddress.addField(new DCField("zipCode", "string", true, 100)); // "string" for cedex / po box
      oasisAddress.addField(new DCField("cityName", "string", false, 100)); // OR only resource ??
      oasisAddress.addField(new DCField("city", "resource", false, 100));
      oasisAddress.addField(new DCField("countryName", "string", false, 100)); // OR only resource ??
      oasisAddress.addField(new DCField("country", "resource", false, 100));
      modelAdminService.addMixin(oasisAddress);

   }
   
   public void initData() {
      // cleaning data first (else Conflict ?!)
      mgo.dropCollection(MY_APP_PLACE);
      mgo.dropCollection(ALTTOURISM_PLACEKIND);
      mgo.dropCollection(ALTTOURISM_PLACE);
      
      // Mixin for shared fields - use 1
      //MY_APP_PLACE
      
      DCResource myAppPlace1 = resourceService.create(MY_APP_PLACE, "my_place_1").set("name", "my_place_1");
      
      DCResource myAppPlace1Posted = datacoreApiClient.postDataInType(myAppPlace1);
      
      // check that field without mixin fails, at update : 
      myAppPlace1Posted.set("zipCode", "69100");

      // check that field without mixin fails, same but already at creation :
      DCResource myAppPlace2 = resourceService.create(MY_APP_PLACE, "my_place_2").set("name", "my_place_2")
            .set("zipCode", "69100");
      
      // step 2 - now adding mixin
      modelAdminService.getModel(MY_APP_PLACE).addMixin(modelAdminService.getMixin(OASIS_ADDRESS));
      ///modelAdminService.addModel(myAppPlaceAddress); // LATER re-add...

      // check, at update :
      DCResource myAppPlace1Put = datacoreApiClient.putDataInType(myAppPlace1Posted);

      // check, same but already at creation :
      DCResource myAppPlace2Posted = datacoreApiClient.postDataInType(myAppPlace2);
      
      
      // Mixin for shared fields - use 2
      //ALTTOURISM_PLACEKIND

      DCResource altTourismHotelKind = resourceService.create(ALTTOURISM_PLACEKIND, "hotel").set("name", "hotel");
      altTourismHotelKind = datacoreApiClient.postDataInType(altTourismHotelKind);

      DCResource altTourismWineryKind = resourceService.create(ALTTOURISM_PLACEKIND, "winery").set("name", "winery");
      altTourismWineryKind = datacoreApiClient.postDataInType(altTourismWineryKind);
      DCResource altTourismPlaceJoWinery = resourceService.create(ALTTOURISM_PLACE, "Jo_Winery")
            .set("name", "Jo_Winery").set("kind", altTourismWineryKind.getUri());
      DCResource altTourismPlaceJoWineryPosted = datacoreApiClient.postDataInType(altTourismPlaceJoWinery);

      // check that field without mixin fails, at update : 
      altTourismPlaceJoWineryPosted.set("zipCode", "1000");

      // check that field without mixin fails, same but already at creation :
      DCResource altTourismMonasteryKind = resourceService.create(ALTTOURISM_PLACEKIND, "monastery").set("name", "monastery");
      altTourismMonasteryKind = datacoreApiClient.postDataInType(altTourismMonasteryKind);
      DCResource altTourismPlaceSofiaMonastery = resourceService.create(ALTTOURISM_PLACE, "Sofia_Monastery")
            .set("name", "Sofia_Monastery").set("kind", altTourismMonasteryKind.getUri())
            .set("zipCode", "1000");
         
      // step 2 - now adding mixin
      modelAdminService.getModel(ALTTOURISM_PLACE).addMixin(modelAdminService.getMixin(OASIS_ADDRESS));
      ///modelAdminService.addModel(altTourismPlace); // LATER re-add...

      // check, at update :
      DCResource altTourismPlaceJoWineryPut = datacoreApiClient.putDataInType(altTourismPlaceJoWineryPosted);

      // check, same but already at creation :
      DCResource altTourismPlaceSofiaMonasteryPosted = datacoreApiClient.postDataInType(altTourismPlaceSofiaMonastery);
   }
   
}
