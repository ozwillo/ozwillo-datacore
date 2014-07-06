package org.oasis.datacore.sample;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ws.rs.WebApplicationException;

import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryService;
import org.oasis.datacore.core.init.InitService;
import org.oasis.datacore.core.init.Initable;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.security.mock.MockAuthenticationService;
import org.oasis.datacore.historization.exception.HistorizationException;
import org.oasis.datacore.historization.service.impl.HistorizationServiceImpl;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.rest.server.DatacoreApiImpl;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.resource.ResourceEntityMapperService;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.rest.server.resource.UriService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;


/**
 * To write a sample class, make it extend this class and annotate it with @Component.
 * If your sample class depends on other sample classes (ex. data one on its model one),
 * tell Spring about it ex. add @DependsOn("citizenKinModel").
 * 
 * To work with Datacore Resources, use resourceService, or server side datacoreApiImpl
 * and the helper methods here
 * (both requiring to have logged in, which is done within init() as admin by default).
 * 
 * Calling it using datacoreApiCachedClient (DatacoreClientApi) works SAVE from appserver
 * (tomcat) and therefore can only be used in tests (or if deploying CXF on jetty within tomcat)
 * but NOT in samples init.
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

   @Autowired
   private MockAuthenticationService mockAuthenticationService;

   /** impl, to be able to modify it
    * TODO LATER extract interface */
   @Autowired
   protected DataModelServiceImpl modelAdminService;

   @Autowired
   protected /*DatacoreApi*/DatacoreCachedClient datacoreApiClient;
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
   protected UriService uriService; // to build URIs when using ResourceService
   @Autowired
   protected LdpEntityQueryService ldpEntityQueryService;
   @Autowired
   protected ResourceEntityMapperService resourceEntityMapperService; // to unmap LdpEntityQueryService results

   @Autowired
   protected EventService eventService;

   @Autowired
   private HistorizationServiceImpl historizationService;

   private HashSet<DCModel> models = new HashSet<DCModel>();


   /**
    * Override to come before or after
    */
   @Override
   public int getOrder() {
      return 1000;
   }
   /*@Override
   public void onApplicationEvent(ContextRefreshedEvent event) {
      this.init();
   }*/
   //@PostConstruct // NOO deadlock, & same for ApplicationContextAware
   @Override
   public void init() {
      mockAuthenticationService.loginAs("admin");
      try {
         doInit();
      } finally {
         mockAuthenticationService.logout();
      }
   }
   
   public abstract void doInit();
   
   public void initData() {
      mockAuthenticationService.loginAs("admin");
      try {
         doInitData();
      } finally {
         mockAuthenticationService.logout();
      }
   }
   
   public abstract void doInitData();

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
            
            dropAndCreateCollectionAndIndices(model);
            
         } else { // mixin
            DCMixin mixin = (DCMixin) modelOrMixin;
            modelAdminService.addMixin(mixin);
         }
      }
   }

   public void dropAndCreateCollectionAndIndices(DCModel model) {
      // cleaning data
      mgo.dropCollection(model.getCollectionName());
      createCollectionAndIndices(model);
      if(model.isHistorizable()) {
         dropAndCreateHistorizedCollectionAndIndices(model);
      }
   }

   public void dropAndCreateHistorizedCollectionAndIndices(DCModel model) {
      DCModel historizedModel;
      try {
         historizedModel = historizationService.createHistorizationModel(model);
         mgo.dropCollection(historizedModel.getCollectionName());
         createCollectionAndIndices(historizedModel);
      } catch (HistorizationException e) {

      }
   }

   private void createCollectionAndIndices(DCModel model) {
      DBCollection coll = mgo.createCollection(model.getCollectionName());
      
      // generating static indexes
      coll.ensureIndex(new BasicDBObject("_uri", 1), null, true);
      coll.ensureIndex(new BasicDBObject("_ar", 1)); // for query security
      coll.ensureIndex(new BasicDBObject("_chAt", 1)); // for default order
      
      // generating field indices
      generateFieldIndices(coll, "_p.", model.getGlobalFieldMap().values());
   }

   private void generateFieldIndices(DBCollection coll, String prefix, Collection<DCField> globalFields) {
      for (DCField globalField : globalFields) {
         generateFieldIndices(coll, prefix, globalField);
      }
   }

   private void generateFieldIndices(DBCollection coll, String prefix, DCField globalField) {
      String prefixedGlobalFieldName = prefix + globalField.getName();
      if (globalField.getQueryLimit() > 0) {
         coll.ensureIndex(prefixedGlobalFieldName);
      }
      switch (DCFieldTypeEnum.getEnumFromStringType(globalField.getType())) {
      case LIST:
         DCField listField = ((DCListField) globalField).getListElementField();
         if(listField.getType() == "map") {
            Map<String, DCField> mapFields = ((DCMapField) listField).getMapFields();
            generateFieldIndices(coll, prefixedGlobalFieldName + ".", mapFields.values());
         } else {
            generateFieldIndices(coll, prefixedGlobalFieldName + ".", listField);
         }
         break;
      case MAP:
         Map<String, DCField> mapFields = ((DCMapField) globalField).getMapFields();
         // TODO WARNING : single map field can't be indexed !!!
         generateFieldIndices(coll, prefixedGlobalFieldName + ".", mapFields.values());
         break;
      default:
         break;
      }
      // TODO LATER embedded resources
   }

   public void cleanDataOfCreatedModels() {
      for (DCModel model : this.models) {
         dropAndCreateCollectionAndIndices(model);
      }
   }


   /**
    * @obsolete use rather resourceService
    * Requires to have logged in first
    */
   public DCResource putDataInType(DCResource resource) {
      try {
         return datacoreApiImpl.putDataInType(resource, resource.getModelType(),
               uriService.parseUri(resource.getUri()).getId());
      } catch (WebApplicationException e) {
         if (e.getResponse().getStatus() / 100 != 2) {
            throw e;
         }
         return (DCResource) e.getResponse().getEntity();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * @obsolete use rather resourceService
    * Requires to have logged in first
    */
   public DCResource postDataInType(DCResource resource) {
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
