package org.oasis.datacore.sample;

import java.util.HashSet;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ws.rs.WebApplicationException;

import org.oasis.datacore.core.init.InitService;
import org.oasis.datacore.core.init.Initable;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.client.DatacoreClientApi;
import org.oasis.datacore.rest.server.DatacoreApiImpl;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoOperations;


/**
 * To write a sample class, make it extend this class and annotate it with @Component.
 * If your sample class depends on other sample classes (ex. data one on its model one),
 * tell Spring about it ex. add @DependsOn("citizenKinModel").
 * 
 * To call Datacore, use datacoreApiImpl (server side) and the helper methods here.
 * Calling it using datacoreApiCachedClient (DatacoreClientApi) works SAVE from appserver
 * (tomcat) and therefore can only be used in tests (or if deploying CXF on jetty within tomcat).
 * 
 * See why InitService (and not mere @PostConstruct or independent ApplicationListeners)
 * in InitService comments.
 *
 * @author mdutoo
 *
 */
public abstract class DatacoreSampleBase implements Initable/*implements ApplicationListener<ContextRefreshedEvent> */{

   @Autowired
   private InitService initService;

   /** impl, to be able to modify it
    * TODO LATER extract interface */
   @Autowired
   protected DataModelServiceImpl modelAdminService;

   @Autowired//
   @Qualifier("datacoreApiCachedClient")
   protected /*DatacoreApi*/DatacoreClientApi datacoreApiClient;
   /** for tests */
   @Autowired
   protected DatacoreApiImpl datacoreApiImpl;
   /** to cleanup db
    * TODO LATER rather in service */
   @Autowired
   protected /*static */MongoOperations mgo;
   
   @Autowired
   protected ResourceService resourceService;

   @Autowired
   protected EventService eventService;

   private HashSet<DCModel> models = new HashSet<DCModel>();


   /*@Override
   public void onApplicationEvent(ContextRefreshedEvent event) {
      this.init();
   }*/
   //@PostConstruct // NOO deadlock, & same for ApplicationContextAware
   @Override
   public abstract void init();

   @PostConstruct
   public void register() {
      initService.register(this);
   }
   
   protected void createModelsAndCleanTheirData(DCModelBase ... modelOrMixins) {
      for (DCModelBase modelOrMixin : modelOrMixins) {
         if (modelOrMixin instanceof DCModel) {
            DCModel model = (DCModel) modelOrMixin;
            // adding model
            modelAdminService.addModel(model);
            models.add(model);
            // cleaning data
            mgo.dropCollection(model.getCollectionName());
            
         } else { // mixin
            DCMixin mixin = (DCMixin) modelOrMixin;
            modelAdminService.addMixin(mixin);
         }
      }
   }

   public void cleanDataOfCreatedModels() {
      for (DCModel model : this.models) {
         // cleaning data
         mgo.dropCollection(model.getCollectionName());
      }
   }


   protected DCResource putDataInType(DCResource resource) {
      try {
         return datacoreApiImpl.putDataInType(resource, resource.getModelType(),
               UriHelper.parseURI(resource.getUri()).getId());
      } catch (WebApplicationException e) {
         if (e.getResponse().getStatus() / 100 != 2) {
            throw e;
         }
         return (DCResource) e.getResponse().getEntity();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   protected DCResource postDataInType(DCResource resource) {
      try {
         return datacoreApiImpl.postDataInType(resource, resource.getModelType());
      } catch (WebApplicationException e) {
         if (e.getResponse().getStatus() / 100 != 2) {
            throw e;
         }
         return (DCResource) e.getResponse().getEntity();
      }
   }

   /***
    * helper method to build new DCResources FOR TESTING ; 
    * copies the given Resource's field that are among the given modelOrMixins
    * (or all if modelOrMixins is null or empty) to this Resource
    * @param source
    * @param modelOrMixins
    * @return
    */
   public DCResource copy(DCResource THIS, DCResource source, DCModelBase ... modelOrMixins) {
      if (modelOrMixins == null || modelOrMixins.length == 0) {
         /*for (Object modelOrMixin : source.getProperties().keySet()) {
            
         }*/
         DCModel sourceModel = modelAdminService.getModel(source.getModelType()); // TODO service
         modelOrMixins = new DCModelBase[] { sourceModel };
         /*int sourceTypeNb = sourceTypes.size();
         // TODO or only mail Model (and its own mixins) ?
         if (sourceTypeNb > 1) {
            List<DCModelBase> modelOrMixinList = new ArrayList<DCModelBase>(sourceTypeNb);
            modelOrMixinList.add(sourceModel);
            for (int i = 1; i < sourceTypeNb; i++) {
               DCModelBase mixin = modelAdminService.getMixinOrModel(sourceTypes.get(i));
               modelOrMixinList.add(mixin);
            }
            modelOrMixins = modelOrMixinList.toArray(new DCModelBase[sourceTypeNb]);
         }*/
      }
      DCModel thisModel = modelAdminService.getModel(THIS.getModelType()); // TODO service
      for (DCModelBase modelOrMixin : modelOrMixins) {
         boolean hasModelOrMixin = thisModel.getName().equals(modelOrMixin.getName())
               /*|| modelAdminService.hasMixin(THIS, modelOrMixin)*/; // TODO service
         Map<String, DCField> fieldMap = modelOrMixin.getGlobalFieldMap();
         for (String fieldName : fieldMap.keySet()) {
            //DCField field = fieldMap.get(fieldName);
            if (hasModelOrMixin || thisModel.getField(fieldName) != null) {
               Object sourceValue = source.get(fieldName);
               THIS.set(fieldName, sourceValue);
            }
         }
      }
      return THIS;
   }
   public DCResource copy(DCResource THIS, DCResource source) {
      return copy(THIS, source, (DCModelBase[]) null);
   }
   
}
