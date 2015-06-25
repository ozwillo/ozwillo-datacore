package org.oasis.datacore.rest.server.resource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.oasis.datacore.common.context.DCRequestContextProviderFactory;
import org.oasis.datacore.core.entity.EntityModelService;
import org.oasis.datacore.core.entity.EntityService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.model.DCSecurity;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.core.security.EntityPermissionService;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.oasis.datacore.historization.exception.HistorizationException;
import org.oasis.datacore.historization.service.HistorizationService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.server.MonitoringLogServiceImpl;
import org.oasis.datacore.rest.server.event.DCResourceEvent;
import org.oasis.datacore.rest.server.event.DCResourceEvent.Types;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCResourceParsingContext;
import org.oasis.datacore.rest.server.parsing.service.QueryParsingService;
import org.oasis.datacore.server.uri.BadUriException;
import org.oasis.datacore.server.uri.UriService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * TODO LATER2 once ModelService & Models are exposed as REST and available on client side,
 * make this code also available on client side
 * 
 * @author mdutoo
 *
 */
@Component // TODO @Service ??
public class ResourceService {
   
   /** context */
   public static final String PUT_MODE = "putRatherThanPatchMode";

   // default conf : (TODO LATER might be conf'd in prop file or even request headers)
   private boolean detailedErrorsMode = true;
   private boolean matchBaseUrlMode = true;
   private boolean normalizeUrlMode = true;
   
   @Autowired
   private UriService uriService;
   @Autowired
   private ResourceEntityMapperService resourceEntityMapperService;
   @Autowired
   private EntityService entityService;
   
   /** for using model caches of embedded referencing resources */
   @Autowired
   private EntityModelService entityModelService;
   
   /** to put creator as owner */
   @Autowired
   @Qualifier("datacoreSecurityServiceImpl")
   private DatacoreSecurityService datacoreSecurityService;
   
   @Autowired
   private EntityPermissionService entityPermissionService;
   
   @Autowired
   private EventService eventService;
   
   @Autowired
   private DCModelService modelService;

   /** to pass putRatherThanPatchMode */
   @Autowired
   protected DCRequestContextProviderFactory requestContextProviderFactory;

   @Autowired
   private QueryParsingService queryParsingService;
   
   @Autowired
   private HistorizationService historizationService;

   @Autowired
   private MonitoringLogServiceImpl monitoringLogServiceImpl;
   
   
   /** helper method to create (fluently) new Resources FOR TESTING */
   public DCResource create(String modelType, String id) {
      DCResource r = DCResource.create(uriService.getContainerUrl(), modelType, id);
      // init iri field :
      // TODO NO rather using creation (or builder) event hooked behaviours
      /*DCModel model = modelService.getModel(modelType);
      String iriFieldName = model.getIriFieldName();
      if (iriFieldName != null) {
         r.set(iriFieldName, iri);
      }*/
      return r;
   }

   /**
    * helper method to create (build) new Resources FOR TESTING
    * triggers build event
    * @throws ResourceException
    * @throws RuntimeException wrapping AbortOperationEventException if a listener aborts it
    */
   public DCResource build(DCResource resource) throws ResourceException, RuntimeException {
      //if (resource.getUri() == null) {
      this.eventService.triggerResourceEvent(DCResourceEvent.Types.ABOUT_TO_BUILD, resource);
      //modelAdminService.getModel(resource).triggerEvent()
      return resource;
   }
   /** helper method to create (build) new Resources FOR TESTING
    * @obsolete use rather DCResource.create() & build() */
   public DCResource build(String type, String id, Map<String,Object> fieldValues) {
      DCResource resource = new DCResource();
      resource.setUri(uriService.buildUri(type, id));
      //resource.setVersion(-1l);
      resource.setProperty("type", type);
      for (Map.Entry<String, Object> fieldValueEntry : fieldValues.entrySet()) {
         resource.setProperty(fieldValueEntry.getKey(), fieldValueEntry.getValue());
      }
      return resource;
   }
   

   /**
    * Helper without event but with canCreate, canUpdate, putRatherThanPatchMode
    * FOR CLEANING RESOURCE PRIORI TO COMPUTING FIELDS & TESTING MAINLY
    * @param resource
    * @param modelType
    * @return
    * @throws ExternalResourceException
    * @throws ResourceTypeNotFoundException
    * @throws ResourceNotFoundException
    * @throws BadUriException
    * @throws ResourceObsoleteException
    * @throws ResourceException
    */
   public DCEntity resourceToEntity(DCResource resource)
               throws ExternalResourceException, ResourceTypeNotFoundException, ResourceNotFoundException,
               BadUriException, ResourceObsoleteException, ResourceException {
      return this.resourceToEntity(resource, resource.getModelType(), true, true, true, false);
   }

   /**
    * Similar to createOrUpdate() (by which it is called) save for persistence,
    * i.e. does all ABOUT_TO_BUILD event, checks & resource parsing until
    * ABOUT_TO_CREATE/UPDATE event excepted.
    * @param resource
    * @param modelType
    * @param canCreate
    * @param canUpdate
    * @param putRatherThanPatchMode
    * @return
    * @throws ExternalResourceException
    * @throws ResourceTypeNotFoundException
    * @throws ResourceNotFoundException
    * @throws BadUriException
    * @throws ResourceObsoleteException
    * @throws ResourceException
    */
   public DCEntity resourceToEntity(DCResource resource, String modelType,
         boolean canCreate, boolean canUpdate, boolean putRatherThanPatchMode, boolean triggerEvent)
               throws ExternalResourceException, ResourceTypeNotFoundException, ResourceNotFoundException,
               BadUriException, ResourceObsoleteException, ResourceException {
      
      DCProject project = modelService.getProject(); // TODO TODO explode if none 
      
      // checking whether creation or update & its consistency :
      Long version = resource.getVersion();
      boolean isCreation = entityService.isCreation(version);
      if (isCreation) {
         if (!canCreate) {
            throw new ResourceException("Version of data resource to update is required in PUT",
                  resource, project);
         }
      } else {
         if (!canUpdate) {
            throw new ResourceException("Version is forbidden in POSTed data resource "
                  + "to create in strict POST mode", resource, project);
         }
      }
      
      DCModelBase dcModel = modelService.getModelBase(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         // TODO LATER OPT client side might deem it a data health / governance problem,
         // and put it in the corresponding inbox
         throw new ResourceTypeNotFoundException(modelType, "Can't find model type. "
               + "Maybe it is badly spelled, or it has been deleted or renamed since (only in test). "
               + "In this case, the missing model must first be created again, "
               + "before patching the entity.", null, resource, project);
      }
      DCModelBase storageModel = modelService.getStorageModel(dcModel); // TODO cache, in context ?
      if (storageModel == null) {
         // TODO LATER OPT client side might deem it a data health / governance problem,
         // and put it in the corresponding inbox
         throw new ResourceTypeNotFoundException(modelType,
               "Can't find storage model of model type, meaning it's a true (definition) mixin. "
               + "Maybe it had one at some point and this model (and its inherited mixins) "
               + "has changed since (only in test). In this case, the missing model must first "
               + "be created again, before patching the entity.", null, resource, project);
      }
      modelType = dcModel.getName(); // normalize ; not useful yet, only TODO LATER IF ex. CountryFR aliasable from Country
      
      
      // TODO in resourceService.build() :
      List<String> resourceTypes = resource.getTypes(); // TODO use it to know what decorating sub mixin to write only... 
      resourceTypes.clear(); /// TODO else re-added
      if (/*resourceTypes.isEmpty()*/true) { // NB. can't be null ; TODO or always ?? because in DCResource.create()...
         resourceTypes.add(modelType);
         resourceTypes.addAll(dcModel.getGlobalMixinNames());
      } // else TODO better add missing ones, or allow them if optional...
      // pre parse hooks (before parsing, to give them a chance to still patch resource ex. auto set uri & id / iri)
      // TODO better as 2nd pass / within parsing ?? or rather on entity ????
      if (triggerEvent) {
         eventService.triggerResourceEvent(DCResourceEvent.Types.ABOUT_TO_BUILD, resource);
      }
      
      
      String stringUri = resource.getUri();
      DCURI uri = uriService.normalizeAdaptCheckTypeOfUri(stringUri, modelType, normalizeUrlMode, matchBaseUrlMode);
      stringUri = uri.toString();
      
      if (uri.isExternalUri()) {
         // TODO or maybe also allow this endpoint's baseUrl ??
         throw new ExternalResourceException(uri, null, null, resource, project);
         // TODO LATER OPT or true broker mode, i.e. this Datacore acts as proxy of another ?
      }
      
      // getting existing :
      // (if isCreation to check that doesn't already exist, else to get ACLs)
      DCEntity existingDataEntity = entityService.getByUriUnsecured(stringUri, dcModel); // NB. unsecured
      // in order to avoid having to fill readers ACL by all writers ACL
      if (existingDataEntity != null) {
         if (isCreation) {
            // already exists, but only allow creation
            throw new ResourceException("Already exists at uri (forbidden in strict POST mode) :\n"
                        + existingDataEntity.toString(), resource, project); // TODO TODO security check access first !!!
         }/* else {
            // HTTP ETag checking (provided in an If-Match header) :
            // NB. DISABLED FOR NOW because will be checked at db save time anyway
            // (and to support multiple POSTs, would have to be complex or use
            // the given dataResource.version instead)
            String httpEntity = dataEntity.getVersion().toString(); // no need of additional uri because only for THIS resource
            EntityTag eTag = new EntityTag(httpEntity);
            ResponseBuilder builder = request.evaluatePreconditions(eTag); // NB. request is injected
            if(builder != null) {
               // client is not up to date (send back 412)
               throw new WebApplicationException(builder.build());
            }
         }*/
      } else {
         if (!isCreation) {
            throw new ResourceObsoleteException("Trying to update missing resource "
                  + "(to rather create it, provide no version or < 0)", resource, project);
         }
      }
      
      // supporting PUT vs default PATCH-like POST mode :
      DCEntity dataEntity = new DCEntity();
      entityModelService.fillDataEntityCaches(dataEntity, dcModel, storageModel, null); // TODO or in DCEntityService ? TODO def
      dataEntity.setUri(stringUri);
      if (!isCreation) {
         dataEntity.setVersion(version);
      } // else keep null version
      if (existingDataEntity != null) {
         dataEntity.setPreviousEntity(existingDataEntity);
         dataEntity.copyNonResourceFieldsFrom(existingDataEntity); // id (else OptimisticLockingFailureException), rights
         // BUT not props (done by EntityPermissionEvaluator when filtering, and only if !putRatherThanPatchModel)
      }
      // NB. cached model has been set at entity get

      Map<String, Object> dataProps = resource.getProperties();
      
      dataEntity.setTypes(buildResourceTypes(resource, dcModel)); // TODO TODO TODO TODO or no modelType, or remove modelName ??
      
      // parsing resource according to model :
      DCResourceParsingContext resourceParsingContext =
            new DCResourceParsingContext(dcModel, storageModel, uri);
      requestContextProviderFactory.set(PUT_MODE, putRatherThanPatchMode);
      //List<DCEntity> embeddedEntitiesToAlsoUpdate = new ArrayList<DCEntity>(); // TODO embeddedEntitiesToAlsoUpdate ??
      //resourceParsingContext.setEmbeddedEntitiesToAlsoUpdate(embeddedEntitiesToAlsoUpdate);
      resourceEntityMapperService.resourceToEntityFields(dataProps, dataEntity.getProperties(),
            (!putRatherThanPatchMode && existingDataEntity != null) ? existingDataEntity.getProperties() : null,
            dcModel.getGlobalFieldMap(), resourceParsingContext); // TODO dcModel.getFieldNames(), abstract DCModel-DCMapField ??
      
      if (resourceParsingContext.hasErrors()) {
         String msg = DCResourceParsingContext.formatParsingErrorsMessage(resourceParsingContext, detailedErrorsMode);
         throw new ResourceException(msg, resource, project);
      } // else TODO if warnings return them as response header ?? or only if failIfWarningsMode ??

      resourceEntityMapperService.entityToResource(dataEntity, resource, true); // apply view (& CLEARS props first)
      // NB. rather than manually updating resource, because props may have changed
      // (ex. if POST/PATCH of a null prop => has not actually been removed)
      // ((and LATER possibly because of behaviours))
      // NB. done before ABOUT_TO_ events so that they can do their checks on the cleaned up resource
      // NB. only thing to still change is persistence-computed fields : o:version...
      
      return dataEntity;
   }
   

   /**
    * Does some checks, calls resourceToEntity() which does all checks,
    * then persists (including history) ; calls events : (ABOUT_TO_)CREATE/UPDATE(D).
    * @param resource
    * @param modelType TODO rather DCModel to allow storing same model in different places (contributions, storage strategy...)
    * @param canCreate
    * @param canUpdate
    * @param putRatherThanPatchMode 
    * @return
    * @throws ResourceTypeNotFoundException if unknown model type
    * @throws ResourceNotFoundException
    * @throws BadUriException if missing (null), malformed, syntax,
    * relative without type, uri's and given modelType don't match
    * @throws ResourceParsingException
    * @throws ResourceObsoleteException
    * @throws ResourceException if no data (null resource), forbidden version in strict POST mode,
    * missing required version in PUT mode,
    */
   public DCResource createOrUpdate(DCResource resource, String modelType,
         boolean canCreate, boolean canUpdate, boolean putRatherThanPatchMode)
               throws ExternalResourceException, ResourceTypeNotFoundException, ResourceNotFoundException,
               BadUriException, ResourceObsoleteException, ResourceException {
      // TODO pass request to validate ETag,
      // or rather in a CXF ResponseHandler (or interceptor but closer to JAXRS 2) see http://cxf.apache.org/docs/jax-rs-filters.html
      // by getting result with outMessage.getContent(Object.class), see ServiceInvokerInterceptor.handleMessage() l.78
      // conf :
      // TODO header or param...
      ///boolean detailedErrorsMode = true;
      ///boolean matchBaseUrlMode = true;
      ///boolean normalizeUrlMode = true;
      
      DCProject project = modelService.getProject(); // TODO TODO explode if none 

      if (resource == null) {
         throw new ResourceException("No data (modelType = " + modelType + ")", null, project);
      }
      
      DCEntity dataEntity = this.resourceToEntity(resource, modelType,
            canCreate, canUpdate, putRatherThanPatchMode, true);
      DCModelBase dcModel = entityModelService.getModel(dataEntity);
      
      boolean isCreation = !(resource.getVersion() != null
            && resource.getVersion() >= 0); // NB. already checked by resourceToEntity()

      // pre save hooks
      // TODO better as 2nd pass / within parsing ?? or rather on entity ????
      Types aboutToEventType = isCreation ? DCResourceEvent.Types.ABOUT_TO_CREATE : DCResourceEvent.Types.ABOUT_TO_UPDATE;
      eventService.triggerResourceEvent(aboutToEventType, resource);
      
      if (isCreation) {
         entityPermissionService.setOwners(dataEntity, buildCreatorOwners(dcModel));
         try {
        	 historizeResource(resource, dataEntity, dcModel);
        	 entityService.create(dataEntity);
        	 // NB. if creation fails will exist in history, but OK if 1. hidden on GET and 2. rewritten on recreate
         } catch (DuplicateKeyException dkex) {
            // detected by unique index on _uri
            // TODO unicity across shards : index not sharded so also handle duplicates a posteriori
            throw new ResourceAlreadyExistsException("Trying to create already existing "
                  + "data resource." , resource, project);
         } catch (NonTransientDataAccessException ntdaex) {
            // unexpected, so rethrowing runtime ex (will be wrapped in 500 server error)
            throw ntdaex;
         }
         // NB. no PREVIOUS version to historize (*relief*, handling consistently its failures would be hard)
         
      } else {
         try {
            historizeResource(resource, dataEntity, dcModel);
            entityService.update(dataEntity);
         } catch (OptimisticLockingFailureException olfex) {
            throw new ResourceObsoleteException("Trying to update data resource "
                  + "without up-to-date version but " + resource.getVersion(), resource, project);
            // and not dataEntity.getVersion() which had already to be incremented by Spring
         } catch (NonTransientDataAccessException ntdaex) {
            // unexpected, so rethrowing runtime ex (will be wrapped in 500 server error)
            throw ntdaex;
         }
      }
      
      //resourceEntityMapperService.entityToResourcePersistenceComputedFields(dataEntity, resource); // NOOO would miss filtered stuff gotten back from existing
      resourceEntityMapperService.entityToResource(dataEntity, resource, true); // apply view (??)

      // 2nd pass : post save hooks
      // TODO better
      Types doneEventType = isCreation ? DCResourceEvent.Types.CREATED : DCResourceEvent.Types.UPDATED;
      eventService.triggerResourceEvent(doneEventType, resource);
      
      return resource;
   }
   

   /** security resource creation owners if any
    * NOOOO also user group if would not be owner */
   private Set<String> buildCreatorOwners(DCModelBase dcModel) {
      Set<String> creatorOwners;
      DCSecurity dcSecurity = modelService.getSecurity(dcModel);
      if (dcSecurity != null) { // try to get project defaults :
         dcSecurity = modelService.getProject(dcModel.getProjectName()).getSecurityDefaults();
      }
      if (dcSecurity != null) {
         creatorOwners = new HashSet<String>(dcSecurity.getResourceCreationOwners());
      } else {
         creatorOwners = new HashSet<String>(2);
      }
      boolean addUserGroup = true;
      if (!creatorOwners.isEmpty()) {
         addUserGroup = false;
         /*for (String g : datacoreSecurityService.getCurrentUser().getEntityGroups()) {
            if (creatorOwners.contains(g)) {
               notYetRights = false;
               break;
            }
         }*/
      } // else setting creator (group) as sole owner
      if (addUserGroup) {
         creatorOwners.add(datacoreSecurityService.getUserGroup());
      }
      return creatorOwners;
   }
   

   /**
    * Shortcut to buildResourceTypes(DCResource resource, DCModel dcModel)
    * @param resource
    * @return model name (taken from URI), then all its mixins (including inherited models),
    * then any additional resource type
    * @throws BadUriException 
    * @throws ResourceTypeNotFoundException 
    */
   public List<String> buildResourceTypes(DCResource resource, DCProject project)
         throws BadUriException, ResourceTypeNotFoundException {
      DCURI uri = uriService.normalizeAdaptCheckTypeOfUri(resource.getUri(),
            null, normalizeUrlMode, matchBaseUrlMode);
      return buildResourceTypes(resource, uri.getType(), project);
   }
   /**
    * Shortcut to buildResourceTypes(DCResource resource, DCModel dcModel)
    * @param resource
    * @param modelType
    * @return model name, then all its mixins (including inherited models),
    * then any additional resource type
    * @throws ResourceTypeNotFoundException 
    */
   public List<String> buildResourceTypes(DCResource resource, String modelType, DCProject project)
         throws ResourceTypeNotFoundException {
      DCModelBase dcModel = modelService.getModelBase(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         // TODO LATER OPT client side might deem it a data health / governance problem,
         // and put it in the corresponding inbox
         throw new ResourceTypeNotFoundException(modelType, "Can't find model type. "
               + "Maybe it is badly spelled, or it has been deleted or renamed since (only in test). "
               + "In this case, the missing model must first be created again, "
               + "before patching the entity.", null, resource, project);
      }
      return buildResourceTypes(resource, dcModel);
   }
   /**
    * 
    * @param resource
    * @param dcModel
    * @return model name, then all its mixins (including inherited models),
    * then any additional resource type
    */
   public List<String> buildResourceTypes(DCResource resource, DCModelBase dcModel) {
      Set<String> modelMixinNames = dcModel.getGlobalMixinNames(); // NB. ordered
      LinkedHashSet<String> types = new LinkedHashSet<String>(modelMixinNames.size() + 1);
      types.add(dcModel.getName());
      types.addAll(modelMixinNames);
      types.addAll(resource.getTypes()); // NB. insertion order is <i>not</i> affected if an element is <i>re-inserted</i> into the set
      return new ArrayList<String>(types);
      //return resource.getTypes();
   }

   
   /**
    * 
    * @param uri
    * @param modelType
    * @param version
    * @return
    * @throws ResourceTypeNotFoundException
    * @throws ResourceNotFoundException
    * @throws ResourceException if asked to abort from within triggered event
    */
   public DCResource getIfVersionDiffers(String uri, String modelType, Long version)
         throws ResourceTypeNotFoundException, ResourceNotFoundException, ResourceException {
      DCProject project = modelService.getProject(); // TODO explode if none 
      DCModelBase dcModel = modelService.getModelBase(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         // TODO LATER OPT client side might deem it a data health / governance problem,
         // and put it in the corresponding inbox
         throw new ResourceTypeNotFoundException(modelType, "Can't find model type for " + uri
               + ". Maybe it is badly spelled, or it has been deleted or renamed since (only in test). "
               + "In this case, the missing model must first be created again, "
               + "before patching the entity.", null, null, project);
      }
      modelType = dcModel.getName(); // normalize ; TODO useful ?
      
      boolean isUpToDate = entityService.isUpToDate(uri, dcModel, version);
      if (isUpToDate) {
         return null;
      }

      return get(uri, dcModel);
   }

   /**
    * 
    * @param uri
    * @param modelType only to avoid reparsing uri, TODO rather pass DCURI
    * @return
    * @throws ResourceTypeNotFoundException
    * @throws ResourceNotFoundException
    * @throws ResourceException if asked to abort from within triggered event
    */
   public DCResource get(String uri, String modelType)
         throws ResourceTypeNotFoundException, ResourceNotFoundException, ResourceException { 
      DCModelBase dcModel = modelService.getModelBase(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         // TODO LATER OPT client side might deem it a data health / governance problem,
         // and put it in the corresponding inbox
         throw new ResourceTypeNotFoundException(modelType, "Can't find model type for " + uri
               + ". Maybe it is badly spelled, or it has been deleted or renamed since (only in test). "
               + "In this case, the missing model must first be created again, "
               + "before patching the entity.", null, null, modelService.getProject()); // explodes if none
      }
      return get(uri, dcModel);
   }

   private DCResource get(String uri, DCModelBase dcModel)
         throws ResourceNotFoundException, ResourceException {  
      DCEntity entity = entityService.getByUri(uri, dcModel);
      if (entity == null) {
         //return Response.noContent().build();
         throw new ResourceNotFoundException("No resource with uri", uri, null,
               modelService.getProject()); // explodes if none
         // rather than NO_CONTENT ; like Atol ex. deleteApplication in
         // https://github.com/pole-numerique/oasis/blob/master/oasis-webapp/src/main/java/oasis/web/apps/ApplicationDirectoryResource.java
      }
      DCResource resource = resourceEntityMapperService.entityToResource(entity, null,
            false); // view already applied by Entity PermissionEvaluator

      eventService.triggerResourceEvent(DCResourceEvent.Types.READ, resource);
      
      //Log to AuditLog Endpoint TODO rather using event
      //monitoringLogServiceImpl.postLog(modelType, "Resource:Get");

      return resource;
   }
   
   /**
    * NB. deleting a non-existing resource does silently nothing.
    * @param uri
    * @param modelType
    * @param version
    * @throws ResourceTypeNotFoundException
    * @throws ResourceException if asked to abort from within triggered event
    */
   public void delete(String uri, String modelType, Long version)
         throws ResourceTypeNotFoundException, ResourceNotFoundException, ResourceException {
      DCProject project = modelService.getProject(); // TODO explode if none 
	   
      DCModelBase dcModel = modelService.getModelBase(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         // TODO LATER OPT client side might deem it a data health / governance problem,
         // and put it in the corresponding inbox
         throw new ResourceTypeNotFoundException(modelType, "Can't find model type for " + uri
               + ". Maybe it is badly spelled, or it has been deleted or renamed since (only in test). "
               + "In this case, the missing model must first be created again, "
               + "before patching the entity.", null, null, project);
      }
      modelType = dcModel.getName(); // normalize ; TODO useful ?
      
      DCEntity dataEntity = null;
      
      if (uri != null && version != null) {
    	  dataEntity = entityService.getByUriUnsecured(uri, dcModel);
      } else {
         // NB. uri & version can't be null if used from JAXRS
    	  throw new ResourceNotFoundException("Cannot get uri or version, cannot evaluate permissions",
    	        uri, null, project);
      }
      
      if (dataEntity == null || !version.equals(dataEntity.getVersion())) {
         throw new ResourceNotFoundException(null, uri, null, project);
      }
      
      DCResource resource = resourceEntityMapperService.entityToResource(dataEntity, null,
            false); // view already applied by Entity PermissionEvaluator
      eventService.triggerResourceEvent(DCResourceEvent.Types.ABOUT_TO_DELETE, resource);
 	   entityService.deleteByUriId(dataEntity);
      eventService.triggerResourceEvent(DCResourceEvent.Types.DELETED, resource);
   }

   public void historizeResource(DCResource resource, DCEntity dataEntity, DCModelBase dcModel) throws ResourceException {
      DCProject project = modelService.getProject(); // TODO explode if none 
      try {
         historizationService.historize(dataEntity, dcModel);
      } catch (HistorizationException hex) {
         throw new ResourceException("Error while historizing", hex, resource, project);
      } catch (Exception ex) {
        throw new ResourceException("Unknown error while historizing", ex, resource, project);
      }
   }
}
