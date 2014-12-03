package org.oasis.datacore.model.resource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.DCRequestContextProviderFactory;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.query.QueryException;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryService;
import org.oasis.datacore.core.init.InitableBase;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.server.event.DCResourceEvent;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.resource.ResourceEntityMapperService;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.sample.ResourceModelIniter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
   protected EventService eventService;
   
   @Autowired
   ///@Qualifier("datacore.cxfJaxrsApiProvider")
   //protected DCRequestContextProvider requestContextProvider;
   protected DCRequestContextProviderFactory requestContextProviderFactory;

   @Override
   protected void doInit() {
      List<DCEntity> projectEntities = null;
      for (int i = 0; projectEntities == null || !projectEntities.isEmpty() ; i += ldpEntityQueryService.getMaxLimit()) {
         try {
            projectEntities = ldpEntityQueryService.findDataInType(ResourceModelIniter.MODEL_PROJECT_NAME,
                  null, i, i + ldpEntityQueryService.getMaxLimit());
            List<DCResource> projectResources = resourceEntityMapperService.entitiesToResources(projectEntities);
            for (DCResource projectResource : projectResources) {
               // TODO TODO
               /*try {
                  eventService.triggerResourceEvent(DCResourceEvent.Types.UPDATED, projectResource);
               } catch (ResourceException rex) {
                  logger.error("Unexpected ResourceException reloading model resource from persistence " + projectResource, rex);
               }*/
            }
         } catch (QueryException e) {
            throw new RuntimeException(e); // should not happen
         }
      }
      
      // now, reload all models that had been persisted in mongo in last execution : TODO wrap in projects
      List<DCResource> modelResources = findDataInType(ResourceModelIniter.MODEL_MODEL_NAME);
      HashSet<String> previousModelsInError = null, modelsInError = null;
      List<ResourceException> rexList = new ArrayList<ResourceException>();
      do {
         previousModelsInError = modelsInError;
         modelsInError = new HashSet<String>(); // not clear() because previousModelsInError
         rexList.clear();
         
         for (DCResource modelResource : modelResources) {
            String modelProjectName = (String) modelResource.get("dcmo:pointOfViewAbsoluteName");
            String modelAbsoluteName = modelProjectName + "." + modelResource.get("dcmo:name");
            if (previousModelsInError != null && !previousModelsInError.contains(modelAbsoluteName)) {
               continue; // not first time and already imported successfully
            }
            try {
               // set context project before loading :
               new SimpleRequestContextProvider() {
                  protected void executeInternal() {
                     try {
                        eventService.triggerResourceEvent(DCResourceEvent.Types.UPDATED, modelResource);
                        // NB. handled by ModelResourceDCListener
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
                  rexList.add((ResourceException) rex);
                  modelsInError.add(modelAbsoluteName);
               } else {
                  logger.debug("Unexpected Exception reloading model resource from persistence " + modelResource, ex);
               }
            }
         }
      } while (!modelsInError.isEmpty() && !modelsInError.equals(previousModelsInError));

      if (!rexList.isEmpty()) {
         logger.error("Unable to reload from persistence models with absolute names : "
               + modelsInError, rexList.get(0));
      }
   }

   private List<DCResource> findDataInType(String modelName) {
      List<DCResource> allModelResources = new ArrayList<DCResource>();
      List<DCEntity> modelEntities = null;
      for (int i = 0; modelEntities == null || !modelEntities.isEmpty() ; i += ldpEntityQueryService.getMaxLimit()) {
         try {
            modelEntities = ldpEntityQueryService.findDataInType(modelName,
                  null, i, i + ldpEntityQueryService.getMaxLimit());
            List<DCResource> modelResources = resourceEntityMapperService.entitiesToResources(modelEntities);
            // NB. this doesn't check model or anything
            allModelResources.addAll(modelResources);
         } catch (QueryException e) {
            throw new RuntimeException(e); // should not happen
         }
      }
      return allModelResources;
   }

   @Override
   public int getOrder() {
      return 1000000;
   }

}
