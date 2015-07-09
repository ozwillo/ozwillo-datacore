package org.oasis.datacore.model.event;

import org.oasis.datacore.core.init.InitService;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.model.resource.ModelResourceMappingService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.server.event.AbortOperationEventException;
import org.oasis.datacore.rest.server.event.DCEvent;
import org.oasis.datacore.rest.server.event.DCEventListener;
import org.oasis.datacore.rest.server.event.DCResourceEvent;
import org.oasis.datacore.rest.server.event.DCResourceEventListener;
import org.oasis.datacore.rest.server.resource.ResourceEntityMapperService;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.rest.server.resource.ResourceObsoleteException;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.sample.ResourceModelIniter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;


/**
 * Inits from Resource and updates DCProject
 * TODO LATER everything else
 * 
 * @author mdutoo
 *
 */
public class ProjectResourceDCListener extends DCResourceEventListener implements DCEventListener {
   
   @Autowired
   private DataModelServiceImpl dataModelAdminService;
   @Autowired
   private ModelResourceMappingService mrMappingService;
   @Autowired
   private ResourceService resourceService;
   @Autowired
   private ResourceEntityMapperService reMappingService;
   /** to know whether inited, until when we can't updateDirectlyImpactedModels
    * (because some are probably not even persisted yet) */
   @Autowired
   private InitService initService;

   public ProjectResourceDCListener() {
      super();
   }

   public ProjectResourceDCListener(String resourceType) {
      super(resourceType);
   }

   @Override
   public void handleEvent(DCEvent event) throws AbortOperationEventException {
      switch (event.getType()) {
      case DCResourceEvent.ABOUT_TO_BUILD :
         // Adding computed fields (computed by DCProject ??) and doing consistency checks :
         // NB. computed fields (ex. ??) must NOT be required
         // NB. not done on ABOUT_TO_CREATE/UPDATE else computed fields won't be added to
         // entity that has already been parsed and will be persisted as is
         DCResourceEvent re = (DCResourceEvent) event;
         DCResource r = re.getResource();
         try {
            
            // first cleaning up resource ; required else default values have not been set by parsing yet
            // which makes toProject fail on ex. maxScan (null because unset) :
            reMappingService.entityToResource(resourceService.resourceToEntity(r), r, false); // TODO write
            
            // map to project & check : 
            DCProject project = mrMappingService.toProject(r); // checks project !
            // clean resource / enrich with up-to-date computed fields
            // (ex. ??) :
            mrMappingService.projectToResource(project, r); 
            // consistency check : (NOT on startup else order of load can make it fail)
            ///mrMappingService.checkProject(project, r);
            
         } catch (ResourceObsoleteException roex) { // specific handling for better logging
            // abort else POSTer won't know that his project can't be used
            throw new AbortOperationEventException(new ResourceObsoleteException("Resource obsolete error while converting "
                  + "or enriching resource to project " + r.get(ResourceModelIniter.POINTOFVIEW_NAME_PROP)
                  + ", aborting POSTing resource", roex, r, dataModelAdminService.getProject()));
         } catch (ResourceException rex) {
            // abort else POSTer won't know that his project can't be used
            throw new AbortOperationEventException(rex);
         } catch (AccessDeniedException adex) { // specific handling for better logging
            // abort else POSTer won't know that his project can't be used
            throw new AbortOperationEventException(new AccessDeniedException("Access denied error while converting "
                  + "or enriching resource to project " + r.get(ResourceModelIniter.POINTOFVIEW_NAME_PROP)
                  + ", aborting POSTing resource", adex));
         } catch (Throwable t) {
            // abort else POSTer won't know that his model can't be used
            throw new AbortOperationEventException(new ResourceException("Unknown error while converting "
                  + "or enriching resource to project " + r.get(ResourceModelIniter.POINTOFVIEW_NAME_PROP)
                  + ", aborting POSTing resource", t, r, dataModelAdminService.getProject()));
         }
         return;
      case DCResourceEvent.CREATED :
      case DCResourceEvent.UPDATED :
         re = (DCResourceEvent) event;
         r = re.getResource();
         handleCreatedOrUpdated(r);
         // TODO if (used as) mixin, do it also for all impacted models (& mixins) !
         return;
      case DCResourceEvent.DELETED :
         re = (DCResourceEvent) event;
         r = re.getResource();
         // TODO LATER check that is not used as mixin in any other models (& mixins),
         // else require that they be deleted first !
         handleDeleted(r);
         return;
      }
   }

   /** TODO move to DCModelService ; must be done in modelOrMixin project 
    * @param previousProject */
   public void createOrUpdate(DCProject project, DCProject previousProject) throws AbortOperationEventException {
      // TODO LATER RATHER ALL AT ONCE to avoid inconsistencies ?

      String aboutToEventType;
      if (previousProject == null) {
         // new model
         aboutToEventType = ModelDCEvent.ABOUT_TO_CREATE;
      } else {
         // TODO also update all impacted models !!
         aboutToEventType = ModelDCEvent.ABOUT_TO_UPDATE;
      }
      
      // let's actually register the DCProject
      // TODO TODO in a cleaner way !!!!!!!!!!!!!!!!!!!
      if (previousProject != null) {
         dataModelAdminService.addProject(project); // in case not already there
      } // else TODO load its models

      /*try {
         // let's update storage (index...) :
         eventService.triggerEvent(new ModelDCEvent(aboutToEventType,
               ModelDCEvent.MODEL_DEFAULT_BUSINESS_DOMAIN, modelOrMixin, previousModel));
      } catch (Throwable e) {
         // TODO abort to say error to client ?
         // TODO save state in model or wrapper stored in mongo ?? log all events in mongo ???
         // TODO try to restore previousDocument ???
         throw new AbortOperationEventException("Aborting as asked for in aboutTo event "
               + "of model or mixin " + modelOrMixin, e);
      }*/
      
      String doneEventType = (previousProject == null) ?
            ModelDCEvent.CREATED : ModelDCEvent.UPDATED;
      /*try {
         eventService.triggerEvent(new ModelDCEvent(doneEventType,
               ModelDCEvent.PROJECT_DEFAULT_BUSINESS_DOMAIN, project, previousProject));
      } catch (Throwable e) {
         // TODO abort to say error to client ?
         // TODO save state in model or wrapper stored in mongo ?? log all events in mongo ???
         // TODO try to restore previousDocument ???
         throw new AbortOperationEventException("Aborting as asked for in done event "
               + "of project " + project, e);
      }*/

      if (initService.isInited()
            && doneEventType == ModelDCEvent.UPDATED) {
         ///updateDirectlyImpactedProjects(project);
      } // else creation which has no impact
   }

   /**
    * TODO check that in oasis.meta ??
    * @param r
    * @throws AbortOperationEventException
    */
   private void handleCreatedOrUpdated(DCResource r) throws AbortOperationEventException {
      try {
         DCProject previousProject = dataModelAdminService.getProject(
               (String) r.get(ResourceModelIniter.POINTOFVIEW_NAME_PROP));
         DCProject project = (previousProject == null) ? mrMappingService.toProject(r) // also checks project !
               :  mrMappingService.toProject(r, previousProject);
         // NB. r has been cleaned up / enriched with up-to-date computed fields
         // (ex. ??) before in ABOUT_TO_CREATE/UPDATE
         // step
         createOrUpdate(project, previousProject);
      } catch (ResourceException rex) {
         // abort else POSTer won't know that his project can't be used
         throw new AbortOperationEventException(rex);
      } catch (AccessDeniedException adex) {
         // abort else POSTer won't know that his project can't be used
         throw new AbortOperationEventException(adex);
      } catch (Throwable t) {
         // abort else POSTer won't know that his project can't be used
         throw new AbortOperationEventException(new ResourceException("Unknown error while converting "
               + "or enriching to project, aborting POSTing resource", t, r, dataModelAdminService.getProject()));
      }
   }

   /**
    * TODO check that in oasis.meta ??
    * @param r
    * @throws AbortOperationEventException
    */
   private void handleDeleted(DCResource r) throws AbortOperationEventException {
      String projectName = (String) r.get(ResourceModelIniter.MODEL_PROJECT_NAME);
      DCProject existingProject = dataModelService.getProject(projectName);
      if (existingProject == null) {
         // happens when db is corrupted ? TODO explode if !devmode
         logger.error("Can't find to be removed DCProject " + r
               + " whose resource has been deleted");
         return;
      }

      // TODO LATER check that is not used ex. as visible project ?
      dataModelAdminService.removeProject(existingProject);
      
      /*try {
         eventService.triggerEvent(new ModelDCEvent(ModelDCEvent.DELETED,
               ModelDCEvent.MODEL_DEFAULT_BUSINESS_DOMAIN, modelOrMixin, null));
      } catch (Throwable e) {
         // TODO abort to say error to client ?
         // TODO save state in model or wrapper stored in mongo ?? log all events in mongo ???
         // TODO try to restore previousDocument ???
         throw new AbortOperationEventException("Aborting as asked for in done event "
               + "of model or mixin " + modelOrMixin, e);
      }*/
   }

}
