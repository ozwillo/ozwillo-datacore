package org.oasis.datacore.rest.server.resource;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.oasis.datacore.core.entity.EntityService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.security.EntityPermissionService;
import org.oasis.datacore.core.security.mock.MockAuthenticationService;
import org.oasis.datacore.historization.exception.HistorizationException;
import org.oasis.datacore.historization.service.HistorizationService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.server.BadUriException;
import org.oasis.datacore.rest.server.event.DCResourceEvent;
import org.oasis.datacore.rest.server.event.DCResourceEvent.Types;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCResourceParsingContext;
import org.oasis.datacore.rest.server.parsing.service.QueryParsingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
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
   
   /** Base URL of this endpoint. If broker mode enabled, used to detect when to use it.. */
   @Value("${datacoreApiServer.baseUrl}") 
   private String baseUrl; // "http://" + "data-lyon-1.oasis-eu.org" + "/"
   /** Unique per container, defines it. To be able to build a full uri
    * (for GET, DELETE, possibly to build missing or custom / relative URI...) */
   @Value("${datacoreApiServer.containerUrl}") 
   private String containerUrl; // "http://" + "data.oasis-eu.org" + "/"

   @Autowired
   private ResourceEntityMapperService resourceEntityMapperService;
   @Autowired
   private EntityService entityService;
   
   /** to put creator as owner */
   @Autowired
   private MockAuthenticationService authenticationService;
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
   
   
   public String getBaseUrl() {
      return baseUrl;
   }

   public String getContainerUrl() {
      return containerUrl;
   }
   
   /** helper method to create (fluently) new Resources FOR TESTING */
   public DCResource create(String modelType, String id) {
      DCResource r = DCResource.create(this.containerUrl, modelType, id);
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
      resource.setUri(this.buildUri(type, id));
      //resource.setVersion(-1l);
      resource.setProperty("type", type);
      for (Map.Entry<String, Object> fieldValueEntry : fieldValues.entrySet()) {
         resource.setProperty(fieldValueEntry.getKey(), fieldValueEntry.getValue());
      }
      return resource;
   }

   public String buildUri(String modelType, String id) {
      return new DCURI(this.containerUrl, modelType, id).toString();
   }

   /**
    * 
    * @param uri
    * @return
    * @throws BadUriException if missing (null), malformed, syntax,
    * relative without type, uri's and given modelType don't match
    */
   public DCURI parseUri(String uri) throws BadUriException {
      return normalizeAdaptCheckTypeOfUri(uri, null, false, true);
   }

   /**
    * TODO extract to UriService
    * Used
    * * by internalPostDataInType on root posted URI (where type is known and that can be relative)
    * * to check referenced URIs and URIs post/put/patch where type is not known (and are never relative)
    * @param stringUriValue
    * @param modelType if any should already have been checked and should be same as uri's if not relative
    * @param normalizeUrlMode
    * @param replaceBaseUrlMode
    * @return
    * @throws BadUriException if missing (null), malformed, syntax,
    * relative without type, uri's and given modelType don't match
    */
   public DCURI normalizeAdaptCheckTypeOfUri(String stringUri, String modelType,
         boolean normalizeUrlMode, boolean matchBaseUrlMode) throws BadUriException {
      if (stringUri == null || stringUri.length() == 0) {
         // TODO LATER2 accept empty uri and build it according to model type (governance)
         // for now, don't support it :
         throw new BadUriException();
      }
      String[] containerAndPathWithoutSlash;
      try {
         containerAndPathWithoutSlash = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            stringUri, this.containerUrl, normalizeUrlMode, matchBaseUrlMode);
      } catch (URISyntaxException usex) {
         throw new BadUriException("Bad URI syntax", stringUri, usex);
      } catch (MalformedURLException muex) {
         throw new BadUriException("Malformed URL", stringUri, muex);
      }
      String urlContainer = containerAndPathWithoutSlash[0];
      if (urlContainer == null) {
         // accept local type-relative uri (type-less iri) :
         if (modelType == null) {
            throw new BadUriException("URI can't be relative when no target model type is provided", stringUri);
         }
         return new DCURI(this.containerUrl, modelType, stringUri);
         // TODO LATER accept also iri including type
      }
      
      // otherwise absolute uri :
      String urlPathWithoutSlash = containerAndPathWithoutSlash[1];
      ///stringUri = this.containerUrl + urlPathWithoutSlash; // useless
      ///return stringUri;

      // check URI model type : against provided one if any, otherwise against known ones
      DCURI dcUri = UriHelper.parseURI(
            urlContainer, urlPathWithoutSlash/*, refModel*/); // TODO LATER cached model ref ?! what for ?
      String uriType = dcUri.getType();
      if (modelType != null) {
         if (!modelType.equals(uriType)) {
            throw new BadUriException("URI resource model type " + uriType
                  + " does not match provided target one " + modelType, stringUri);
         }
      }
      return dcUri;
   }
   
   

   /**
    * @param resource
    * @param modelType
    * @param canCreate
    * @param canUpdate
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
         boolean canCreate, boolean canUpdate) throws ResourceTypeNotFoundException,
         ResourceNotFoundException, BadUriException, ResourceObsoleteException, ResourceException {
      // TODO pass request to validate ETag,
      // or rather in a CXF ResponseHandler (or interceptor but closer to JAXRS 2) see http://cxf.apache.org/docs/jax-rs-filters.html
      // by getting result with outMessage.getContent(Object.class), see ServiceInvokerInterceptor.handleMessage() l.78
      
      // conf :
      // TODO header or param...
      boolean detailedErrorsMode = true;
      boolean matchBaseUrlMode = true;
      boolean normalizeUrlMode = true;
      boolean brokerMode = false;

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
      DCURI uri = this.normalizeAdaptCheckTypeOfUri(stringUri, modelType, normalizeUrlMode, matchBaseUrlMode);
      stringUri = uri.toString();
      
      // TODO extract to checkContainer()
      if (!this.getContainerUrl().equals(uri.getContainer())) {
         if (!brokerMode) {
            throw new ResourceException("In non-broker mode, can only serve data from own container "
                        + this.getContainerUrl() + " but URI container is "
                        + uri.getContainer(), resource);
         }
         // else TODO LATER OPT broker mode
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
      
      if (dataEntity == null) {
         dataEntity = new DCEntity();
         dataEntity.setCachedModel(dcModel); // TODO or in DCEntityService ?
         dataEntity.setUri(stringUri);
      }

      Map<String, Object> dataProps = resource.getProperties();
      
      dataEntity.setModelName(dcModel.getName()); // TODO LATER2 check that same (otherwise ex. external approvable contrib ??)
      dataEntity.setVersion(version);
      ///dataEntity.setId(stringUri); // NOO "invalid Object Id" TODO better
      dataEntity.setTypes(resource.getTypes()); // TODO or no modelType, or remove modelName ??
      
      // parsing resource according to model :
      DCResourceParsingContext resourceParsingContext = new DCResourceParsingContext(dcModel, stringUri);
      //List<DCEntity> embeddedEntitiesToAlsoUpdate = new ArrayList<DCEntity>(); // TODO embeddedEntitiesToAlsoUpdate ??
      //resourceParsingContext.setEmbeddedEntitiesToAlsoUpdate(embeddedEntitiesToAlsoUpdate);
      resourceEntityMapperService.resourceToEntityFields(dataProps, dataEntity.getProperties(), dcModel.getGlobalFieldMap(),
            resourceParsingContext); // TODO dcModel.getFieldNames(), abstract DCModel-DCMapField ??
      
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
         creatorOwners.add(authenticationService.getUserGroup(authenticationService.getCurrentUserId()));
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

      //Map<String, Object> dcDataProps = new HashMap<String,Object>(dataEntity.getProperties());
      //DCData dcData = new DCData(dcDataProps);
      //dcData.setUri(uri);
      resource.setVersion(dataEntity.getVersion());
      
      resource.setCreated(dataEntity.getCreated()); // NB. if already provided, only if creation
      resource.setCreatedBy(dataEntity.getCreatedBy()); // NB. if already provided, only if creation
      resource.setLastModified(dataEntity.getLastModified());
      resource.setLastModifiedBy(dataEntity.getLastModifiedBy());
      
      // TODO setProperties if changed ex. default values ? or copied queriable fields, or on behaviours ???
      // ex. for model.getDefaultValues() { dcData.setProperty(name, defaultValue);

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

      entityService.deleteByUriId(uri, version, dcModel);
   }
}
