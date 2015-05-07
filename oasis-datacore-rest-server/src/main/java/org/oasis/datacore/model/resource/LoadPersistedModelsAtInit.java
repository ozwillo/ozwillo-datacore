package org.oasis.datacore.model.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.DCRequestContextProviderFactory;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.query.QueryException;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryService;
import org.oasis.datacore.core.init.InitableBase;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.resource.ResourceEntityMapperService;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.sample.ResourceModelIniter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;


/**
 * TODO from client rather than from local service, for Italian Datacore 
 * 
 * @author mdutoo
 *
 */
@Component
public class LoadPersistedModelsAtInit extends InitableBase {

   @Autowired
   private LdpEntityQueryService ldpEntityQueryService;
   @Autowired
   protected ResourceEntityMapperService resourceEntityMapperService; // to unmap LdpEntityQueryService results
   @Autowired
   protected ModelResourceMappingService mrMappingService;
   @Autowired
   private DCModelService dataModelService;
   @Autowired
   private DataModelServiceImpl dataModelAdminService;
   
   @Autowired
   protected EventService eventService;
   
   @Autowired
   ///@Qualifier("datacore.cxfJaxrsApiProvider")
   //protected DCRequestContextProvider requestContextProvider;
   protected DCRequestContextProviderFactory requestContextProviderFactory;

   @Override
   protected void doInit() {
      List<DCResource> projectResources = findDataInType(
            ResourceModelIniter.MODEL_PROJECT_NAME, ResourceModelIniter.POINTOFVIEW_NAME_PROP);
      List<String> loadedProjectNames = new ArrayList<String>(projectResources.size());
      for (DCResource projectResource : projectResources) {
         // TODO TODO
         /*try {
            eventService.triggerResourceEvent(DCResourceEvent.Types.UPDATED, projectResource);
         } catch (ResourceException rex) {
            logger.error("Unexpected ResourceException reloading model resource from persistence " + projectResource, rex);
         }*/
      }
      
      logger.info("Loaded " + loadedProjectNames.size() + " projects : " + loadedProjectNames);
      logger.debug("   loaded projects details : " + loadedProjectNames);
      
      // now, reload all models that had been persisted in mongo in last execution : TODO wrap in projects
      List<DCResource> modelResources = findDataInType(
            ResourceModelIniter.MODEL_MODEL_NAME, ResourceModelIniter.MODEL_NAME_PROP);
      HashMap<String,ResourceException> previousModelsInError = null, modelsInError = null;
      List<String> loadedModelAbsoluteNames = new ArrayList<String>(modelResources.size());
      do {
         previousModelsInError = modelsInError;
         modelsInError = new HashMap<String,ResourceException>(); // not clear() because previousModelsInError
         
         for (DCResource modelResource : modelResources) {
            String modelProjectName = (String) modelResource.get("dcmo:pointOfViewAbsoluteName");
            String modelAbsoluteName = modelProjectName + "." + modelResource.get(ResourceModelIniter.MODEL_NAME_PROP);
            if (previousModelsInError != null && !previousModelsInError.containsKey(modelAbsoluteName)) {
               continue; // not first time and already imported successfully
            }
            try {
               // set context project before loading :
               new SimpleRequestContextProvider() {
                  protected void executeInternal() {
                     try {
                        ///eventService.triggerResourceEvent(DCResourceEvent.Types.UPDATED, modelResource);
                        // NB. handled by ModelResourceDCListener
                        
                        DCModelBase model = mrMappingService.toModelOrMixin(modelResource);
                        dataModelAdminService.addModel(model);
                        loadedModelAbsoluteNames.add(model.getAbsoluteName());
                        // TODO LATER once all is loaded, ((clean cache and)) repersist all in case were wrong
                     } catch (ResourceException rex) {
                        throw new RuntimeException(rex);
                     }
                  }
               }.execInContext(new ImmutableMap.Builder<String, Object>()
                     .put(DCRequestContextProvider.PROJECT, modelProjectName).build());
               modelsInError.remove(modelAbsoluteName);
               
            } catch (RuntimeException ex) {
               Throwable rex = ex.getCause();
               if (rex != null && rex instanceof ResourceException) {
                  modelsInError.put(modelAbsoluteName, (ResourceException) rex);
               } else {
                  logger.debug("Unexpected Exception reloading model resource from persistence " + modelResource, ex);
               }
            }
         }
      } while (!modelsInError.isEmpty() && !modelsInError.equals(previousModelsInError));

      logger.info("Loaded " + loadedModelAbsoluteNames.size() + " models");
      logger.debug("   loaded models details : " + loadedModelAbsoluteNames);
      
      if (!modelsInError.isEmpty()) {
         logger.error("Unable to reload from persistence models with absolute names : "
               + modelsInError);
      }
   }

   private List<DCResource> findDataInType(String modelType, String nameProp) {
      List<DCResource> allResources = new ArrayList<DCResource>();
      List<DCEntity> entities = null;
      Map<String, List<String>> nextProjectsByName = new HashMap<String, List<String>>(2);
      nextProjectsByName.put(DCResource.KEY_DCCREATED, // ResourceModelIniter.POINTOFVIEW_NAME_PROP
            new ImmutableList.Builder<String>().add("+").build()); // older first
      try {
         while (!(entities = ldpEntityQueryService.findDataInType(modelType,
               nextProjectsByName, 0, ldpEntityQueryService.getMaxLimit())).isEmpty()) {
            List<DCResource> resources = resourceEntityMapperService.entitiesToResources(entities);
            // NB. this doesn't check model or anything
            allResources.addAll(resources);
            
            // preparing next find :
            String lastResourceName = (String) resources.get(resources.size() - 1).get(nameProp);
            nextProjectsByName.put(nameProp, new ImmutableList.Builder<String>()
                  .add(">\"" + lastResourceName + "\"+").build()); // older first
         }
      } catch (QueryException qex) {
         throw new RuntimeException(qex); // should not happen
      }
      return allResources;
   }

   @Override
   public int getOrder() {
      return 1000000;
   }

}
