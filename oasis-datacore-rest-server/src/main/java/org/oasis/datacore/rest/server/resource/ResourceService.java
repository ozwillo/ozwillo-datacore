package org.oasis.datacore.rest.server.resource;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityNotFoundException;

import org.oasis.datacore.core.entity.EntityService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.security.EntityPermissionService;
import org.oasis.datacore.core.security.mock.MockAuthenticationService;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.oasis.datacore.historization.exception.HistorizationException;
import org.oasis.datacore.historization.service.HistorizationService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.server.BadUriException;
import org.oasis.datacore.rest.server.event.DCResourceEvent;
import org.oasis.datacore.rest.server.event.DCResourceEvent.Types;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCResourceParsingContext;
import org.oasis.datacore.rest.server.parsing.service.QueryParsingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * TODO LATER2 once ModelService & Models are exposed as REST and available on client side,
 * make this code also available on client side
 * 
 * @author mdutoo
 *
 */
@Component // TODO @Service ??
public class ResourceService {

   @Autowired
   private UriService uriService;
   @Autowired
   private ResourceEntityMapperService resourceEntityMapperService;
   @Autowired
   private EntityService entityService;
   
   /** to put creator as owner */
   @Autowired
   private MockAuthenticationService authenticationService;
   
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
    * Helper for building Datacore maps
    * ex. resourceService.propertiesBuilder().put("name", "John").put("age", 18).build()
    * @return
    */
   public Builder<String, Object> propertiesBuilder() {
      return new ImmutableMap.Builder<String, Object>();
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
    * @param resource
    * @param modelType
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
      boolean detailedErrorsMode = true;
      boolean matchBaseUrlMode = true;
      boolean normalizeUrlMode = true;

      boolean isCreation = true;

      if (resource == null) {
         throw new ResourceException("No data (modelType = " + modelType + ")", null);
      }
      
      Long version = resource.getVersion();
      if (version != null && version >= 0) {
         if (!canUpdate) {
            throw new ResourceException("Version is forbidden in POSTed data resource "
                  + "to create in strict POST mode", resource);
         } else {
            isCreation = false;
         }
      } else if (!canCreate) {
         throw new ResourceException("Version of data resource to update is required in PUT", resource);
      }
      
      DCModel dcModel = modelService.getModel(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         throw new ResourceTypeNotFoundException(modelType, null, null, resource);
      }
      modelType = dcModel.getName(); // normalize ; TODO useful ?
      
      
      // TODO in resourceService.build() :
      List<String> resourceTypes = resource.getTypes(); // TODO use it to know what decorating sub mixin to write only... 
      resourceTypes.clear(); /// TODO else re-added
      if (/*resourceTypes.isEmpty()*/true) { // NB. can't be null ; TODO or always ?? because in DCResource.create()...
         resourceTypes.add(modelType);
         resourceTypes.addAll(dcModel.getGlobalMixinNameSet());
      } // else TODO better add missing ones, or allow them if optional...
      // pre parse hooks (before parsing, to give them a chance to still patch resource ex. auto set uri & id / iri)
      // TODO better as 2nd pass / within parsing ?? or rather on entity ????
      eventService.triggerResourceEvent(DCResourceEvent.Types.ABOUT_TO_BUILD, resource);
      
      
      String stringUri = resource.getUri();
      DCURI uri = uriService.normalizeAdaptCheckTypeOfUri(stringUri, modelType, normalizeUrlMode, matchBaseUrlMode);
      stringUri = uri.toString();
      
      if (uri.isExternalUri()) {
         // TODO or maybe also allow this endpoint's baseUrl ??
         throw new ExternalResourceException(uri, null, null, resource);
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
                           + dataEntity.toString(), resource); // TODO TODO security check access first !!!
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
            if (!canCreate) {
               throw new ResourceNotFoundException("Data resource doesn't exist (forbidden in PUT)", resource);
            }
         }
      }
      
      /**/
      
      // supporting PUT vs default PATCH-like POST mode :
      if (dataEntity == null) {
         dataEntity = new DCEntity();
         dataEntity.setCachedModel(dcModel); // TODO or in DCEntityService ?
         dataEntity.setUri(stringUri);
      } else if (putRatherThanPatchMode) {
         dataEntity.getProperties().clear();
      } // else reuse existing entity as base : PATCH-like behaviour

      Map<String, Object> dataProps = resource.getProperties();
      
      dataEntity.setModelName(dcModel.getName()); // TODO LATER2 check that same (otherwise ex. external approvable contrib ??)
      dataEntity.setVersion(version);
      ///dataEntity.setId(stringUri); // NOO "invalid Object Id" TODO better
      dataEntity.setTypes(resource.getTypes()); // TODO or no modelType, or remove modelName ??
      
      // parsing resource according to model :
      DCResourceParsingContext resourceParsingContext = new DCResourceParsingContext(dcModel, stringUri);
      //List<DCEntity> embeddedEntitiesToAlsoUpdate = new ArrayList<DCEntity>(); // TODO embeddedEntitiesToAlsoUpdate ??
      //resourceParsingContext.setEmbeddedEntitiesToAlsoUpdate(embeddedEntitiesToAlsoUpdate);
      resourceEntityMapperService.resourceToEntityFields(dataProps, dataEntity.getProperties(),
            dcModel.getGlobalFieldMap(), resourceParsingContext,
            putRatherThanPatchMode); // TODO dcModel.getFieldNames(), abstract DCModel-DCMapField ??
      
      if (resourceParsingContext.hasErrors()) {
         String msg = DCResourceParsingContext.formatParsingErrorsMessage(resourceParsingContext, detailedErrorsMode);
         throw new ResourceException(msg, resource);
      } // else TODO if warnings return them as response header ?? or only if failIfWarningsMode ??

      // pre save hooks
      // TODO better as 2nd pass / within parsing ?? or rather on entity ????
      Types aboutToEventType = isCreation ? DCResourceEvent.Types.ABOUT_TO_CREATE : DCResourceEvent.Types.ABOUT_TO_UPDATE;
      eventService.triggerResourceEvent(aboutToEventType, resource);
      
      if (isCreation) {
         // setting creator (group) as sole owner
         Set<String> creatorOwners = new HashSet<String>(1);
         creatorOwners.add(authenticationService.getUserGroup(datacoreSecurityService.getCurrentUserId()));
         entityPermissionService.setOwners(dataEntity, creatorOwners);
         try {
        	 try {
                 historizationService.historize(dataEntity, dcModel);
              } catch (HistorizationException hex) {
             	throw new ResourceException("Error while historizing", hex, resource);
              }
        	 entityService.create(dataEntity);
        	 // NB. if creation fails will exist in history, but OK if 1. hidden on GET and 2. rewritten on recreate
         } catch (DuplicateKeyException dkex) {
            // detected by unique index on _uri
            // TODO unicity across shards : index not sharded so also handle duplicates a posteriori
            throw new ResourceAlreadyExistsException("Trying to create already existing "
                  + "data resource." , resource);
         } catch (NonTransientDataAccessException ntdaex) {
            // unexpected, so rethrowing runtime ex (will be wrapped in 500 server error)
            throw ntdaex;
         }
         // NB. no PREVIOUS version to historize (*relief*, handling consistently its failures would be hard)
         
      } else {
         try {
            try {
               historizationService.historize(dataEntity, dcModel);
             } catch (HistorizationException hex) {
                throw new ResourceException("Error while historizing", hex, resource);
             } catch (Exception ex) {
                throw new ResourceException("Unknown error while historizing", ex, resource);
             }
            entityService.update(dataEntity);
         } catch (OptimisticLockingFailureException olfex) {
            throw new ResourceObsoleteException("Trying to update data resource "
                  + "without up-to-date version but " + dataEntity.getVersion(), resource);
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
   
   


   public DCResource getIfVersionDiffers(String uri, String modelType, Long version)
         throws ResourceTypeNotFoundException, ResourceNotFoundException {
      DCModel dcModel = modelService.getModel(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         throw new ResourceTypeNotFoundException(modelType, null, null, null);
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
         throw new ResourceNotFoundException("No resource with uri " + uri, null);
         // rather than NO_CONTENT ; like Atol ex. deleteApplication in
         // https://github.com/pole-numerique/oasis/blob/master/oasis-webapp/src/main/java/oasis/web/apps/ApplicationDirectoryResource.java
      }
      DCResource resource = resourceEntityMapperService.entityToResource(entity);
      return resource;
   }

   public DCResource get(String uri, String modelType)
         throws ResourceTypeNotFoundException, ResourceNotFoundException {
      DCModel dcModel = modelService.getModel(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         throw new ResourceTypeNotFoundException(modelType, null, null, null);
      }
      modelType = dcModel.getName(); // normalize ; TODO useful ?
      
      DCEntity entity = entityService.getByUri(uri, dcModel);
      if (entity == null) {
         //return Response.noContent().build();
         throw new ResourceNotFoundException("No resource with uri " + uri, null);
         // rather than NO_CONTENT ; like Atol ex. deleteApplication in
         // https://github.com/pole-numerique/oasis/blob/master/oasis-webapp/src/main/java/oasis/web/apps/ApplicationDirectoryResource.java
      }
      DCResource resource = resourceEntityMapperService.entityToResource(entity);
      return resource;
   }

   /**
    * NB. deleting a non-existing resource does silently nothing.
    * @param uri
    * @param modelType
    * @param version
    * @throws ResourceTypeNotFoundException
    */
   public void delete(String uri, String modelType, Long version) throws ResourceTypeNotFoundException {
	   
      DCModel dcModel = modelService.getModel(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         throw new ResourceTypeNotFoundException(modelType, null, null, null);
      }
      modelType = dcModel.getName(); // normalize ; TODO useful ?
      
      DCEntity dataEntity = null;
      
      if (uri != null && version != null && dcModel != null) {
    	  dataEntity = entityService.getByUriUnsecured(uri, dcModel);
      } else {
    	  throw new RuntimeException("Cannot get uri or version or model, cannot evaluate permissions");
      }
      
      if(dataEntity != null && version.equals(dataEntity.getVersion())) {
    	  entityService.deleteByUriId(dataEntity);
      } else {
    	  throw new EntityNotFoundException();
      }
      
   }
}
