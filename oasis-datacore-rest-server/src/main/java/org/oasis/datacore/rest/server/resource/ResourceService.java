package org.oasis.datacore.rest.server.resource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.oasis.datacore.core.entity.EntityService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
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
    * TODO: replace modelType with the DCModel you want to use
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

      boolean isCreation = true;
      
      DCProject project = modelService.getProject(); // TODO TODO explode if none 

      if (resource == null) {
         throw new ResourceException("No data (modelType = " + modelType + ")", null, project);
      }
      
      Long version = resource.getVersion();
      if (version != null && version >= 0) { // version < 0 means new
         if (!canUpdate) {
            throw new ResourceException("Version is forbidden in POSTed data resource "
                  + "to create in strict POST mode", resource, project);
         } else {
            isCreation = false;
         }
      } else if (!canCreate) {
         throw new ResourceException("Version of data resource to update is required in PUT",
               resource, project);
      }
      
      DCModelBase dcModel = modelService.getModelBase(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         throw new ResourceTypeNotFoundException(modelType,
               null, null, resource, project);
      }
      DCModelBase storageModel = modelService.getStorageModel(modelType); // TODO cache, in context ?
      if (storageModel == null) {
         throw new ResourceTypeNotFoundException(modelType,
               "Unknown storage model for model type", null, resource, project);
      }
      modelType = dcModel.getName(); // normalize ; TODO useful ?
      
      
      // TODO in resourceService.build() :
      List<String> resourceTypes = resource.getTypes(); // TODO use it to know what decorating sub mixin to write only... 
      resourceTypes.clear(); /// TODO else re-added
      if (/*resourceTypes.isEmpty()*/true) { // NB. can't be null ; TODO or always ?? because in DCResource.create()...
         resourceTypes.add(modelType);
         resourceTypes.addAll(dcModel.getGlobalMixinNames());
      } // else TODO better add missing ones, or allow them if optional...
      // pre parse hooks (before parsing, to give them a chance to still patch resource ex. auto set uri & id / iri)
      // TODO better as 2nd pass / within parsing ?? or rather on entity ????
      eventService.triggerResourceEvent(DCResourceEvent.Types.ABOUT_TO_BUILD, resource);
      
      
      String stringUri = resource.getUri();
      DCURI uri = uriService.normalizeAdaptCheckTypeOfUri(stringUri, modelType, normalizeUrlMode, matchBaseUrlMode);
      stringUri = uri.toString();
      
      if (uri.isExternalUri()) {
         // TODO or maybe also allow this endpoint's baseUrl ??
         throw new ExternalResourceException(uri, null, null, resource, project);
         // TODO LATER OPT or true broker mode, i.e. this Datacore acts as proxy of another ?
      }

      DCEntity dataEntity = null;
      if (!canUpdate || !isCreation) {
         dataEntity = entityService.getByUriUnsecured(stringUri, dcModel); // NB. unsecured
         // in order to avoid having to fill readers ACL by all writers ACL
         if (dataEntity != null) {
            if (!canUpdate) {
               // already exists, but only allow creation
               throw new ResourceException("Already exists at uri (forbidden in strict POST mode) :\n"
                           + dataEntity.toString(), resource, project); // TODO TODO security check access first !!!
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
            if (!canCreate) {
               throw new ResourceNotFoundException("Data resource doesn't exist (forbidden in PUT)",
                     resource, project);
            }
         }
      }
      
      /**/
      
      // supporting PUT vs default PATCH-like POST mode :
      if (dataEntity == null) {
         dataEntity = new DCEntity();
         dataEntity.setCachedModel(dcModel); // TODO or in DCEntityService ?
         dataEntity.setUri(stringUri);
         // NB. null version
      } else {
         dataEntity.setVersion(version);
         if (putRatherThanPatchMode) {
            dataEntity.getProperties().clear();
         } // else reuse existing entity as base : PATCH-like behaviour
      }

      Map<String, Object> dataProps = resource.getProperties();
      
      dataEntity.setModelName(dcModel.getName()); // TODO LATER2 check that same (otherwise ex. external approvable contrib ??)
      ///dataEntity.setId(stringUri); // NOO "invalid Object Id" TODO better
      dataEntity.setTypes(buildResourceTypes(resource, dcModel)); // TODO or no modelType, or remove modelName ??
      
      // parsing resource according to model :
      DCResourceParsingContext resourceParsingContext =
            new DCResourceParsingContext(dcModel, storageModel, uri);
      //List<DCEntity> embeddedEntitiesToAlsoUpdate = new ArrayList<DCEntity>(); // TODO embeddedEntitiesToAlsoUpdate ??
      //resourceParsingContext.setEmbeddedEntitiesToAlsoUpdate(embeddedEntitiesToAlsoUpdate);
      resourceEntityMapperService.resourceToEntityFields(dataProps, dataEntity.getProperties(),
            dcModel.getGlobalFieldMap(), resourceParsingContext,
            putRatherThanPatchMode, true); // TODO dcModel.getFieldNames(), abstract DCModel-DCMapField ??
      
      if (resourceParsingContext.hasErrors()) {
         String msg = DCResourceParsingContext.formatParsingErrorsMessage(resourceParsingContext, detailedErrorsMode);
         throw new ResourceException(msg, resource, project);
      } // else TODO if warnings return them as response header ?? or only if failIfWarningsMode ??

      // pre save hooks
      // TODO better as 2nd pass / within parsing ?? or rather on entity ????
      Types aboutToEventType = isCreation ? DCResourceEvent.Types.ABOUT_TO_CREATE : DCResourceEvent.Types.ABOUT_TO_UPDATE;
      eventService.triggerResourceEvent(aboutToEventType, resource);
      
      if (isCreation) {
         // setting creator (group) as sole owner
         Set<String> creatorOwners = new HashSet<String>(1);
         creatorOwners.add(datacoreSecurityService.getUserGroup());
         entityPermissionService.setOwners(dataEntity, creatorOwners);
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

      resource = resourceEntityMapperService.entityToResource(dataEntity);
      // NB. rather than manually updating resource, because props may have changed
      // (ex. if POST/PATCH of a null prop => has not actually been removed)
      // ((and LATER possibly because of behaviours))

      // 2nd pass : post save hooks
      // TODO better
      Types doneEventType = isCreation ? DCResourceEvent.Types.CREATED : DCResourceEvent.Types.UPDATED;
      eventService.triggerResourceEvent(doneEventType, resource);
      
      return resource;
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
         throw new ResourceTypeNotFoundException(modelType, null, null, resource, project);
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
         throw new ResourceTypeNotFoundException(modelType, null, null, null, project);
      }
      modelType = dcModel.getName(); // normalize ; TODO useful ?
      
      DCEntity entity = null;
      boolean isUpToDate = entityService.isUpToDate(uri, dcModel, version);
      if (isUpToDate) {
         return null;
      }
      
      entity = entityService.getByUri(uri, dcModel);
      if (entity == null) {
         //return Response.noContent().build();
         throw new ResourceNotFoundException("No resource with uri", uri, null, project);
         // rather than NO_CONTENT ; like Atol ex. deleteApplication in
         // https://github.com/pole-numerique/oasis/blob/master/oasis-webapp/src/main/java/oasis/web/apps/ApplicationDirectoryResource.java
      }
      DCResource resource = resourceEntityMapperService.entityToResource(entity);
      
      eventService.triggerResourceEvent(DCResourceEvent.Types.READ, resource);
      return resource;
   }

   /**
    * 
    * @param uri
    * @param modelType
    * @return
    * @throws ResourceTypeNotFoundException
    * @throws ResourceNotFoundException
    * @throws ResourceException if asked to abort from within triggered event
    */
   public DCResource get(String uri, String modelType)
         throws ResourceTypeNotFoundException, ResourceNotFoundException, ResourceException {
      DCProject project = modelService.getProject(); // TODO explode if none 
      DCModelBase dcModel = modelService.getModelBase(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         throw new ResourceTypeNotFoundException(modelType, null, null, null, project);
      }
      
      DCEntity entity = entityService.getByUri(uri, dcModel);
      if (entity == null) {
         //return Response.noContent().build();
         throw new ResourceNotFoundException("No resource with uri", uri, null, project);
         // rather than NO_CONTENT ; like Atol ex. deleteApplication in
         // https://github.com/pole-numerique/oasis/blob/master/oasis-webapp/src/main/java/oasis/web/apps/ApplicationDirectoryResource.java
      }
      DCResource resource = resourceEntityMapperService.entityToResource(entity);

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
         throws ResourceTypeNotFoundException, ResourceException {
      DCProject project = modelService.getProject(); // TODO explode if none 
	   
      DCModelBase dcModel = modelService.getModelBase(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         throw new ResourceTypeNotFoundException(modelType, null, null, null, project);
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
      
      DCResource resource = resourceEntityMapperService.entityToResource(dataEntity);
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
