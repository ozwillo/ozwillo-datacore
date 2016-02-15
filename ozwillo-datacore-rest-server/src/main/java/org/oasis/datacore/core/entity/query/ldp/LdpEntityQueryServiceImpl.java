package org.oasis.datacore.core.entity.query.ldp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.oasis.datacore.core.context.DatacoreRequestContextService;
import org.oasis.datacore.core.entity.EntityModelService;
import org.oasis.datacore.core.entity.NativeModelService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.mongodb.DatacoreMongoTemplate;
import org.oasis.datacore.core.entity.query.QueryException;
import org.oasis.datacore.core.meta.ModelNotFoundException;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCI18nField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.core.security.DCUserImpl;
import org.oasis.datacore.core.security.EntityPermissionEvaluator;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.server.MonitoringLogServiceImpl;
import org.oasis.datacore.rest.server.cxf.CxfJaxrsApiProvider;
import org.oasis.datacore.rest.server.parsing.model.DCQueryParsingContext;
import org.oasis.datacore.rest.server.parsing.model.DCResourceParsingContext;
import org.oasis.datacore.rest.server.parsing.model.QueryOperatorsEnum;
import org.oasis.datacore.rest.server.parsing.service.QueryParsingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.CursorProviderQueryCursorPreparer;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;


/**
 * Provides W3C LDP (Linked Data Platform)-like query on top of Datacore MongoDB storage.
 * 
 * TODO LATER move to -core : by removing parsing (& dependency to queryParsingContext)
 * & temporary dependency to DatacoreApiImpl, or by making -core depend on -server...
 * 
 * @author mdutoo
 *
 */
@Component
public class LdpEntityQueryServiceImpl implements LdpEntityQueryService {

   public static final String DEBUG_QUERY_EXPLAIN = "queryExplain";
   public static final String DEBUG_WARNINGS = "warnings";

   private static final Logger logger = LoggerFactory.getLogger(LdpEntityQueryServiceImpl.class);

   public static final String MONGO_FIELD_PREFIX = "_p.";
   private static final String DOT = ".";
   
   private static Set<String> findConfParams = new HashSet<String>();
   static {
      // TODO rather using Enum, see BSON$RegexFlag
      findConfParams.add(DatacoreApi.START_PARAM);
      findConfParams.add(DatacoreApi.LIMIT_PARAM);
      findConfParams.add(DatacoreApi.DEBUG_PARAM);
      findConfParams.add("format");
      findConfParams.add(DCResource.KEY_I18N_LANGUAGE_JSONLD);
      findConfParams.add(DCI18nField.KEY_LANGUAGE);
   }

   @Value("${datacoreApiServer.query.detailedErrorsMode}")
   private boolean detailedErrorsMode = true;
   
   /** pagination - default maximum number of documents to scan when fulfilling a query, overriden by
    * DCFields', themselves limited by DCModel's. 0 means no limit (for tests), else ex.
    * 1000 (secure default), 100000 (on query-only nodes using secondary & timeout)... 
    * http://docs.mongodb.org/manual/reference/operator/meta/maxScan/ */
   @Value("${datacoreApiServer.query.maxScan}")
   private int maxScan;
   /** pagination - default maximum start position : 500... */
   @Value("${datacoreApiServer.query.maxStart}")
   private int maxStart;
   /** pagination - default maximum number of documents returned : 100... */
   @Value("${datacoreApiServer.query.maxLimit}")
   private int maxLimit;
   /** pagination - default number of documents returned : 10... */
   @Value("${datacoreApiServer.query.defaultLimit}")
   protected int defaultLimit;
   /** microseconds after which db.killOp() if > 0, requires 2.6+ server http://docs.mongodb.org/manual/reference/method/cursor.maxTimeMS/#cursor.maxTimeMS */
   @Value("${datacoreApiServer.query.maxTime}")
   protected int maxTime;

   
   /** to get storage model */
   @Autowired
   private DCModelService modelService;

   /** to fill entity's models cache */
   @Autowired
   private EntityModelService entityModelService;
   
   /** NB. when moving LDP service to -core, might make it optional */
   @Autowired
   private NativeModelService nativeModelService;

   /** to evaluate model-level rights and then get user groups */
   @Autowired
   @Qualifier("datacoreSecurityServiceImpl")
   private DatacoreSecurityService securityService;
   
   @Autowired
   private /*MongoOperations*/DatacoreMongoTemplate mgo; // TODO remove it by hiding it in services
   
   @Autowired
   private QueryParsingService queryParsingService;

   @Autowired
   private MonitoringLogServiceImpl monitoringLogServiceImpl;

   /** to access debug switch & put its explained results */
   @Autowired
   private CxfJaxrsApiProvider cxfJaxrsApiProvider;
   /** to access debug switch & put its explained results */
   @Autowired
   protected DatacoreRequestContextService serverRequestContext;
   
   /** to evaluate model-level rights */
   @Autowired
   private EntityPermissionEvaluator permissionEvaluator;

   
   @Override
   public List<DCEntity> findDataInType(String modelType, Map<String, List<String>> params,
         Integer start, Integer limit) throws QueryException {
      ///boolean detailedErrorsMode = true; // TODO also conf from context
      
      DCModelBase model = modelService.getModelBase(modelType); // NB. if used from JAXRS, modelType can't be null thanks to JAXRS
      if (model == null) {
         // TODO LATER OPT client side might deem it a data health / governance problem,
         // and put it in the corresponding inbox
         throw new ModelNotFoundException(modelType, modelService.getProject(),
               "Can't find model type. Maybe it is badly spelled, or it has been deleted "
               + "or renamed since (only in test). In this case, the missing model must first "
               + "be created again, before patching the entity.");
      }
      DCModelBase storageModel = modelService.getStorageModel(model); // TODO cache, in context ?
      if (storageModel == null) {
         // TODO LATER OPT client side might deem it a data health / governance problem,
         // and put it in the corresponding inbox
         throw new ModelNotFoundException(modelType, modelService.getProject(),
               "Can't find storage model of model type, meaning it's a true (definition) mixin. "
               + "Maybe it had one at some point and this model (and its inherited mixins) "
               + "has changed since (only in test).In this case, the missing model must first "
               + "be created again, before patching the entity.");
      }
      
      if (params == null) {
         params = new HashMap<String,List<String>>(0);
      }
      
      //Log to AuditLog Endpoint
      //monitoringLogServiceImpl.postLog(modelType, "findDataInType");

      // parsing paging :
      if (start == null) { // should not happen
         start = 0;
      } else if (start > maxStart) {
         // max, else prefer ranged query
         throw new QueryException("Can't ask for start (" + start + ") > " + maxStart
               + ". Rather use a ranged query, that is typically with a > or < sorted "
               + "criteria on an indexed required Resource (ex. dcmo:name) "
               + "or native (ex. dc:modified) field ex. my:field=>\"lastValueReturned\"+ "
               + "(with the first query having been sort only ex. my:field=\"lastValueReturned\"+).");
      }
      if (limit == null) { // should not happen
         limit = defaultLimit;
      } else if (limit > maxLimit) {
         limit = maxLimit; // max, else prefer ranged query, next will raise start > maxStart error
      }
      // NB. limit will be limited through $maxScan by models' queryLimit and storageModel' maxScan
      
      // parsing query parameters criteria according to model :
      DCQueryParsingContext queryParsingContext = new DCQueryParsingContext(model, storageModel);
      if (permissionEvaluator.isThisModelAllowed(model,
            securityService.getCurrentUser(), EntityPermissionEvaluator.READ)) {
         queryParsingContext.getTopLevelAllowedMixins().add(model.getName());
         // this requires checking resource-level rights, to avoid it query in a model that
         // you have model-level rights on
      }
      
      // setting global language if any :
      // (BEFORE parsing & building criteria)
      List<String> globalLanguages = params.get(DCResource.KEY_I18N_LANGUAGE_JSONLD);
      if (globalLanguages == null || globalLanguages.isEmpty()) {
         globalLanguages = params.get(DCI18nField.KEY_LANGUAGE);
      }
      if (globalLanguages != null && !globalLanguages.isEmpty()) {
         String globalLanguage = globalLanguages.get(0);
         if (queryParsingContext.checkLanguage(globalLanguage)) {
            queryParsingContext.setGlobalLanguage(globalLanguage);
            if (globalLanguages.size() > 1) {
               queryParsingContext.addWarning("Found more than one global language : "
                     + globalLanguages + ", only the first is used");
            }
         }
      }
      
      parseQueryParameters(params, queryParsingContext);
      
      buildQueryAndSort(queryParsingContext, null);
      
      Sort sort = queryParsingContext.getSort();
      if (sort == null) {
         // TODO sort by default : configured in model (last modified date, uri,
         // iri?, types?, owners? other fields...)
         sort = new Sort(Direction.DESC, DCEntity.KEY_CH_AT); // new Sort(Direction.ASC, "_uri")...
      }
      
      Query springMongoQuery = queryParsingContext.getQuery()
            // adding paging & sorting :
            .with(sort).skip(start).limit(limit); // TODO rather range query, if possible on sort field
      
      List<DCEntity> foundEntities = executeMongoDbQuery(springMongoQuery, queryParsingContext);
      
      if (queryParsingContext.isFulltext()) {
         // if fulltext, prepend a token exact match lookup :
         buildQueryAndSort(queryParsingContext, "$");
         springMongoQuery = queryParsingContext.getQuery()
               // adding paging & sorting :
               .with(sort).skip(start).limit(limit); // TODO rather range query, if possible on sort field
         List<DCEntity> exactMatchFoundEntities = executeMongoDbQuery(springMongoQuery, queryParsingContext);
         
         if (!exactMatchFoundEntities.isEmpty() // (otherwise nothing to merge)
               && !queryParsingContext.getFulltextSorts().isEmpty()) {
               // (otherwise sort on _chAt so ex. Saint-Lô can't be hidden by ex. Saint-Lormel)
            // merge :
            foundEntities = foundEntities.stream()
                  .filter(fe -> !exactMatchFoundEntities.contains(fe))
                  .collect(Collectors.toList());
            if (queryParsingContext.getFulltextSorts().get(0) == QueryOperatorsEnum.SORT_ASC) {
               // moving exact matches at the start :
               exactMatchFoundEntities.addAll(foundEntities);
               foundEntities = exactMatchFoundEntities;
            } else { // SORT_DESC
               // moving exact matches at the end :
               foundEntities.addAll(exactMatchFoundEntities);
            }
            // (both obligatorily distinct & within limit)
         }
      }
      
      if (logger.isDebugEnabled()) {
         logger.debug("Done Spring Mongo query as " + securityService.getCurrentUserId() + ": "
               + springMongoQuery + "\n   in collection " + storageModel.getCollectionName()
               + " from Model " + model.getName() + " and parameters: " + params
               + "\n   with result nb " + foundEntities.size());
      }
      
      if (serverRequestContext.isDebug()) {
         Map<String, Object> debugCtx = serverRequestContext.getDebug(); // never null because isDebug()
         debugCtx.put("queryParameters", params);
         debugCtx.put(DatacoreApi.START_PARAM, start);
         debugCtx.put(DatacoreApi.LIMIT_PARAM, limit);
         DCUserImpl user = securityService.getCurrentUser();
         debugCtx.put("ownedEntities", foundEntities.stream()
               .filter(e -> permissionEvaluator.hasPermission(null, e, EntityPermissionEvaluator.GET_RIGHTS))
               .map(e -> new DCEntity(e)) // otherwise once filled getCachedModel() triggers foundDatas being null on client side ?!?!
               .collect(Collectors.toList()));
      }
      
      // setting cached model for future mapping to resource :
      // POLY NOO might differ, & also storageModel
      for (DCEntity foundEntity : foundEntities) {
         entityModelService.checkAndFillDataEntityCaches(foundEntity);
         // NB. accepting even entities with wrong model type (not instanciable, no storage
         // because the model type or his mixins has changed, only in test),
         // so that they may be at least read in order to be patched.
         // However, we don't reassign model to a known one (ex. the storage that has been looked in)
         // otherwise it might break rights (though such wrong models should only happen
         // in sandbox / test / draft / not yet published phase).
         // TODO LATER better : put such cases in data health / governance inbox, through event
      }
      
      return foundEntities;
   }


   private void parseQueryParameters(Map<String, List<String>> params, DCQueryParsingContext queryParsingContext) {
      DCModelBase dcModel = queryParsingContext.peekModel();
      DCUserImpl user = securityService.getCurrentUser();
      
      parameterLoop : for (String fieldPath : params.keySet()) {
         if (fieldPath.length() == 0) {
            continue; // ex. ?& or &&
         }
         if (findConfParams.contains(fieldPath)) {
            // skip find conf params
            continue;
         }
         
         //DCField dcField = dcModelService.getFieldByPath(dcModel, fieldPath); // TODO TODOOOOOOOOOOOOO
         // TODO move impl :
         String[] fieldPathElements = fieldPath.split("\\."); // (escaping regex) mongodb field path syntax
         // TODO LATER also XML fieldPath.split("/") ??
         if (fieldPathElements.length == 0) {
            continue; // should not happen
         }

         StringBuilder entityFieldPathSb = new StringBuilder();
         String topFieldPathElement = fieldPathElements[0];
         
         // getting top-level field (DCEntity native (indexed) or not) and checking model-level rights :
         DCField dcField = nativeModelService.getNativeModel(dcModel).getGlobalField(topFieldPathElement); // NB. global for dc:DublinCore_0
         if (dcField == null) {
            dcField = getFieldAndCheckIfRightsHaveToBeDecidedAtResourceLevel(
                  dcModel, topFieldPathElement, user, queryParsingContext, true);
            if (dcField != null) {
               entityFieldPathSb = entityFieldPathSb.append(MONGO_FIELD_PREFIX); // almost the same fieldPath for mongodb
            }
         }
         // TODO LATER :
         /*if (!dcField.isQueriable()) {
            throw new Exception("only indexed fields are allowed at top level");
         }*/
         
         if (dcField == null) {
            queryParsingContext.addError("In type " + dcModel.getName() + ", can't find field with path elements "
                  + Arrays.asList(fieldPathElements) + " : can't find field for first path element "
                  + fieldPathElements[0]);
            continue;
         }
         String storageReadName = dcField.getStorageReadName(); // mapping if necessary, generic (@id to _uri...) or specific
         if (storageReadName == null) {
            queryParsingContext.addError("In type " + queryParsingContext.peekModel().getName()
                  + ", field with path elements " + entityFieldPathSb
                  + " is not storable (soft computed) "
                  + "and therefore not queriable");
            continue;
         }
         entityFieldPathSb.append(storageReadName);
         
         // finding the leaf field
         // TODO submethod with throw ex
         
//         // finding the latest higher list field :
//         DCListField dcListField = null;
//         if ("list".equals(dcField.getType())) {
//            dcListField = (DCListField) dcField;
//            do {
////               dcField = ((DCListField) dcField).getListElementField();
//            } while ("list".equals(dcField.getType()));
//         }
         
         // loop on path elements for finding the leaf field :
         for (int i = 1; i < fieldPathElements.length; i++) {
            String fieldPathElement = fieldPathElements[i];
            
            entityFieldPathSb.append(DOT);
            
            if ("map".equals(dcField.getType())) {
               dcField = handleMapField(dcField, fieldPathElement,
                     queryParsingContext, entityFieldPathSb, fieldPathElements, i);
               
            } else if ("list".equals(dcField.getType())) {
               do {
                 dcField = ((DCListField) dcField).getListElementField();
                 //If we have a map in a list (for i18n)
                 if("map".equals(dcField.getType())) {
                    dcField = handleMapField(dcField, fieldPathElement,
                          queryParsingContext, entityFieldPathSb, fieldPathElements, i);
                 } else if ("resource".equals(dcField.getType())) { // TODO noooooooooooo embedded / subresource
                    dcField = handleResourceField(dcField, fieldPathElement,
                          queryParsingContext, entityFieldPathSb, fieldPathElements, i);
                 } else {
                    storageReadName = dcField.getStorageReadName(); // mapping if necessary, generic (@id to _uri...) or specific
                    if (storageReadName == null) {
                       queryParsingContext.addError("In type " + dcModel.getName() + ", error finding field with path elements"
                             + Arrays.asList(fieldPathElements) + ": can't go below " + i + "th element " 
                             + fieldPathElement + ", because field is not storable (soft computed) "
                             + "and therefore not queriable");
                       continue;
                    }
                    entityFieldPathSb.append(storageReadName);
                 }
                 // TODO TODO check that indexed (or set low limit) ??
               } while (dcField != null && "list".equals(dcField.getType()));
               
            } else if ("resource".equals(dcField.getType())) { // TODO noooooooooooo embedded / subresource
               dcField = handleResourceField(dcField, fieldPathElement,
                     queryParsingContext, entityFieldPathSb, fieldPathElements, i);
               
            } else if ("i18n".equals(dcField.getType())) {
               dcField = handleI18nField(dcField, fieldPathElement,
                     queryParsingContext, entityFieldPathSb, fieldPathElements, i);
               // TODO TODO check that indexed (or set low limit) ??
               
            } else {
               queryParsingContext.addError("In type " + dcModel.getName() + ", can't find field with path elements"
                     + Arrays.asList(fieldPathElements) + ": can't go below " + i + "th element " 
                     + fieldPathElement + ", because field type is neither map nor list nor i18n "
                     + "nor embedded subresource but " + dcField.getType());
               continue parameterLoop; // TODO boum
            }
            
            if (dcField == null) {
               continue parameterLoop;
            }

//            if ("list".equals(dcField.getType())) {
//               // finding the latest higher list field :
//               dcListField = (DCListField) dcField;
//               do {
////                  dcField = ((DCListField) dcField).getListElementField();
//                  // TODO TODO check that indexed (or set low limit) ??
//               } while ("list".equals(dcField.getType()));
//            } else {
//               dcListField = null;
//            }
         
         
         }
         
         List<String> values = params.get(fieldPath);
         if (values == null || values.isEmpty()) {
            queryParsingContext.addError("Missing value for parameter " + fieldPath);
            continue; // should not happen
         }

         String entityFieldPath = entityFieldPathSb.toString();
         // parsing multiple values (of a field that is mentioned several times) :
         // (such as {limit=[10], founded=[>"-0143-04-01T00:00:00.000Z", <"-0043-04-02T00:00:00.000Z"]})
         // NB. can't be done by merely chaining .and(...)'s because of mongo BasicDBObject limitations, see
         // http://www.mkyong.com/java/due-to-limitations-of-the-basicdbobject-you-cant-add-a-second-and/
         for (String operatorAndValue : values) {
            try {
               // parsing query parameter criteria according to model field :
               queryParsingService.parseQueryParameter(entityFieldPath, dcField,
                     operatorAndValue, queryParsingContext);
            } catch (Exception ex) {
               queryParsingContext.addError("Error while parsing query parameter " + fieldPath
                     + " " + operatorAndValue, ex); // even RuntimeException so that more lenient than 503
            }
         }

      }
   }


   private void buildQueryAndSort(DCQueryParsingContext queryParsingContext,
         String fulltextRegexSuffix) throws QueryException {
      queryParsingContext.initBuildCriteria(fulltextRegexSuffix);
      queryParsingService.buildCriteria(queryParsingContext);
      if (queryParsingContext.hasErrors()) {
         String msg = DCResourceParsingContext.formatParsingErrorsMessage(queryParsingContext, detailedErrorsMode);
         throw new QueryException(msg);
      } // else TODO if warnings return them as response header ?? or only if failIfWarningsMode ??

      entityModelService.addMultiProjectStorageCriteria(
            queryParsingContext.getCriteria(), queryParsingContext.peekStorageModel()); // storageModel
      addResourceLevelSecurityIfRequired(queryParsingContext);
      // NB. no guest specific case (groups ex. EVERYONE must be given to guest
      // and allowed on models or resources)
   }

   
   private DCField getFieldAndCheckIfRightsHaveToBeDecidedAtResourceLevel(DCModelBase dcModel,
         String topFieldPathElement, DCUserImpl user, DCQueryParsingContext queryParsingContext,
         boolean addToTopLevelAllowedMixins) {
      if (user.isAdmin()
            || queryParsingContext.getRightsHaveToBeDecidedAtResourceLevel()) {
         return dcModel.getGlobalField(topFieldPathElement);
         
      } else { // rights might still be decided at model level :
         DCModelBase fieldModel = getFieldModel(dcModel, topFieldPathElement);
         if (fieldModel != null) {
            // check (model-level) security :
            if (!permissionEvaluator.isThisModelAllowed(fieldModel,
                  user, EntityPermissionEvaluator.READ)) {
               queryParsingContext.getForbiddenMixins().add(fieldModel.getName());
               ///throw new Exception("Not allowed to read data of mixin "
               ///      + dcModel.getName() + " and therefore to query on its field " + topFieldPathElement);
            } else if (addToTopLevelAllowedMixins) {
               queryParsingContext.getTopLevelAllowedMixins().add(fieldModel.getName());
            }
            return fieldModel.getField(topFieldPathElement);
         }
      }
      return null;
   }

   /** used to check model-level rights of each queried field ; TODO cache */
   public DCModelBase getFieldModel(DCModelBase dcModel, String topFieldPathElement) {
      if (dcModel.getField(topFieldPathElement) != null) {
         return dcModel;
      }
      // else find it above :
      for (DCModelBase mixin : dcModel.getMixins()) {
         DCModelBase fieldModel = getFieldModel(mixin, topFieldPathElement);
         if (fieldModel != null) {
            return fieldModel;
         }
      }
      return null; // means not found
   }


   /**
    * 
    * @param dcField
    * @param fieldPathElement
    * @param queryParsingContext
    * @param entityFieldPathSb appends fieldPathElement to it
    * @param fieldPathElements for error logging
    * @param i for error logging ; if 0 changed to fieldPathElements.length - 1
    * @return null if error
    */
   private DCField handleMapField(DCField dcField, String fieldPathElement,
         DCQueryParsingContext queryParsingContext, StringBuilder entityFieldPathSb,
         String[] fieldPathElements, int i) {
      DCField subDcField = ((DCMapField) dcField).getMapFields().get(fieldPathElement);
      if (subDcField == null) {
         queryParsingContext.addError("In type " + queryParsingContext.peekModel().getName()
               + ", can't find field with path elements"
               + Arrays.asList(fieldPathElements) + ": can't go below "
               + ((i == 0) ? fieldPathElements.length - 1 : i) + "th path element "
               + fieldPathElement + ", because field is unkown. Allowed fields are "
               + ((DCMapField) dcField).getMapFields().keySet());
         return null;
      }
      String storageReadName = subDcField.getStorageReadName();
      if (storageReadName == null) {
         queryParsingContext.addError("In type " + queryParsingContext.peekModel().getName()
               + ", find field with path elements is not storable (soft computed) "
               + "and therefore not queriable");
         return null;
      }
      entityFieldPathSb.append(storageReadName);
      return subDcField;
   }

   private DCField handleResourceField(DCField dcField, String fieldPathElement,
         DCQueryParsingContext queryParsingContext, StringBuilder entityFieldPathSb,
         String[] fieldPathElements, int i) {
      queryParsingContext.addWarning("Found criteria which might require join, "
            + "resolving it as embedded subresource for now : in type "
            + queryParsingContext.peekModel().getName() + ", field "
      //queryParsingContext.addError("Found criteria requiring join : in type " + dcModel.getName() + ", field "
      //      + fieldPath + " (" + i + "th in field path elements " + Arrays.asList(fieldPathElements)
            + ") can't be done in findDataInType, do it rather on client side");
      //continue parameterLoop; // TODO boum
      String linkModelType = ((DCResourceField) dcField).getResourceType();
      DCModelBase linkModel = modelService.getModelBase(linkModelType);
      if (linkModel == null) {
         // TODO LATER OPT client side might deem it a data health / governance problem,
         // and put it in the corresponding inbox
         queryParsingContext.addError("In type " + queryParsingContext.peekModel().getName()
               + ", can't find field with path elements"
               + Arrays.asList(fieldPathElements) + ": can't go below "
               + ((i == 0) ? fieldPathElements.length - 1 : i) + "th path element "
               + fieldPathElement + ", because resource field modelType " + linkModelType
               + " is unkown. Maybe it is badly spelled, or it has been deleted "
               + "or renamed since (only in test). In this case, the missing model must first "
               + "be created again, before patching the entity.");
         return null;
      }
      DCModelBase linkStorageModel = modelService.getStorageModel(linkModel);
      // TODO check linkStorageModel, separate cases :
      // fully embedded (isInstanceStorage ? storagePath ? storageModel ?), refMixin,
      // external resource (in which case explode because a join is required)
      
      // TODO handle ex. criteria on geoci?geocifr:country(geoco:Country_0).geocofr:idInsee
      
      // getting resource's field and checking model-level rights :
      ///DCField subDcField = linkModel.getGlobalField(fieldPathElement);
      DCField subDcField = getFieldAndCheckIfRightsHaveToBeDecidedAtResourceLevel(
            linkModel, fieldPathElement, securityService.getCurrentUser(), queryParsingContext, false);
      
      if (subDcField == null) {
         queryParsingContext.addError("In type " + queryParsingContext.peekModel().getName()
               + ", can't find field with path elements"
               + Arrays.asList(fieldPathElements) + ": can't go below "
               + ((i == 0) ? fieldPathElements.length - 1 : i) + "th path element "
               + fieldPathElement + ", because field is unkown. Allowed fields are "
               + linkModel.getGlobalFieldMap().keySet());
      }
      String storageReadName = subDcField.getStorageReadName();
      if (storageReadName == null) {
         queryParsingContext.addError("In type " + queryParsingContext.peekModel().getName()
               + ", find field with path elements is not storable (soft computed) "
               + "and therefore not queriable");
         return null;
      }
      entityFieldPathSb.append(storageReadName);
      return subDcField;
   }

   /**
    * 
    * @param dcField
    * @param fieldPathElement
    * @param queryParsingContext
    * @param entityFieldPathSb appends fieldPathElement to it
    * @param fieldPathElements for error logging
    * @param i for error logging ; if 0 changed to fieldPathElements.length - 1
    * @return
    */
   private DCField handleI18nField(DCField dcField, String fieldPathElement,
         DCQueryParsingContext queryParsingContext,
         StringBuilder entityFieldPathSb, String[] fieldPathElements, int i) {
      DCMapField dcMapField = (DCMapField) ((DCListField) dcField).getListElementField();
      String language = null;
      
      // translating to mongo if needed :
      String entityFieldPathElement;
      switch (fieldPathElement) {
      case DCResource.KEY_I18N_VALUE_JSONLD :
      case DCI18nField.KEY_VALUE :
         entityFieldPathElement = DCI18nField.KEY_VALUE;
         break;
      case DCResource.KEY_I18N_LANGUAGE_JSONLD :
      case DCI18nField.KEY_LANGUAGE :
         entityFieldPathElement = DCI18nField.KEY_LANGUAGE;
         break;
      default :
         if (queryParsingContext.checkLanguage(fieldPathElement)) {
            language = fieldPathElement;
            entityFieldPathElement = DCI18nField.KEY_VALUE;
         } else {
            queryParsingContext.addError("In type " + queryParsingContext.peekModel().getName()
                  + ", can't find field with path elements"
                  + Arrays.asList(fieldPathElements) + ": can't go below "
                  + ((i == 0) ? fieldPathElements.length - 1 : i) + "th path element "
                  + fieldPathElement + ", because field is unkown. Allowed fields are "
                  + ((DCMapField) dcField).getMapFields().keySet()
                  + " or a 2-letter language (followed by .value).");
            return null;
         }
      }
      queryParsingContext.setEntityFieldPathAsI18nValue(entityFieldPathSb.toString()
            + DCI18nField.KEY_VALUE, language);
      // "list of map"-like handling :
      // (leaf field : i18n is obligatorily a list of map of strings)
      return handleMapField(dcMapField, entityFieldPathElement,
            queryParsingContext, entityFieldPathSb, fieldPathElements, i);
   }

   /**
    * NB. no guest specific case (groups ex. EVERYONE must be given to guest
    * and allowed on models or resources)
    * @param queryParsingContext
    * @return
    */
   private boolean addResourceLevelSecurityIfRequired(DCQueryParsingContext queryParsingContext) {
      // TODO Q how to make all tests still work : null case ? other prop ? disable it ??
      // TODO better : in SecurityQueryEnricher ? rather in (Query)ParsingContext ?!?
      // TODO (LATER ?) on all sub Resources !!
      // TODO LATER in findDataInAllTypes(), on all root Resources
      /*DCSecurity modelSecurity = queryParsingContext.peekModel().getSecurity();
      if (!modelSecurity.isGuestReadable()) {
         // TODO Q or (b) also GUEST OK when empty ACL ?
         // NO maybe dangerous because *adding* a group / role to ACL would *remove* GUEST from it
         // so solution would be : API (i.e. Social Graph) as (a) but storage translates it to (b), allows to store less characters
         // requires adding an $or criteria : _w $size 0 $or _w $in currentUserRoles
         // which is more complex and might be (TODO test) worse performance-wise
         // => OPT LATER
         // (in any way, there'll always be a balance to find between performance and storage)
         
         DCUserImpl user = securityService.getCurrentUser();
         if (user.isGuest()) {
            return true; // TODO or exception ??
         }
         
         if (!user.isAdmin()
               && !modelSecurity.isAuthentifiedReadable() // NB. not guest
               && !modelSecurity.isResourceAdmin(user)
               && !modelSecurity.isResourceReader(user)) {
            queryParsingContext.getCriteria().and(DCEntity.KEY_AR).in(user.getEntityGroups());
         } // else (datacore global or model-scoped) admin, so no security check
      } // else public, so no security check
      */
      
      if (queryParsingContext.getRightsHaveToBeDecidedAtResourceLevel()) {
         // i.e. model queried on as well as all fields (if any) are in model-level allowed models
         // (executeMongoDbQuery will add criteria on model queried on if not storage,
         // and if storage then model is had as mixin by all stored models there)
         queryParsingContext.getCriteria().and(DCEntity.KEY_AR)
            .in(securityService.getCurrentUser().getEntityGroups());
      }
      
      return false;
   }

   
   private List<DCEntity> executeMongoDbQuery(Query springMongoQuery,
         DCQueryParsingContext queryParsingContext) throws QueryException {
      DCModelBase model = queryParsingContext.peekModel();
      DCModelBase storageModel = queryParsingContext.peekStorageModel();
      if (!model.getName().equals(storageModel.getName())) {
         // adding criteria on model type when storage larger than model :
         springMongoQuery.addCriteria(new Criteria(DCEntity.KEY_T).is(model.getName()));
      } else {
         // if we query the whole storage model, then we should filter out aliases
         springMongoQuery.addCriteria(new Criteria(DCEntity.KEY_ALIAS_OF).exists(false));
      }
      
      // compute overall maxScan :
      // (BEWARE it is NOT the max amount of doc returned because sorts or multiple
      // criteria can eat some scans)
      boolean applyMaxScan = queryParsingContext.isHasNoIndexedField(); // true // to test only
      int maxScan = 0;
      if (applyMaxScan) {
         maxScan = this.maxScan;
         if (queryParsingContext.getAggregatedQueryLimit() > maxScan) {
            // allow at least enough scan for expected results
            maxScan = queryParsingContext.getAggregatedQueryLimit();
         }
         if (storageModel.getMaxScan() > 0) {
            // limit maxScan : take smallest
            maxScan = (maxScan > storageModel.getMaxScan()) ?
                  storageModel.getMaxScan() : maxScan;
         }
         /*if (springMongoQuery.getLimit() < maxScan) {
            maxScan = springMongoQuery.getLimit(); // should not need more
         }*/ // NOO else criterized unindexed queries are sure to fail
         // ex. HTTPOperationsTest.testDeleteDcTypeIri()'s find
         maxScan = maxScan * 3; // for sort maxScan (because sort "scanned" is ex. 120 when query "scanned" is 83)
         // but also because in the same example limit was 50
      }

      boolean isDebug = serverRequestContext.isDebug();
      boolean doExplainQuery = isDebug || applyMaxScan;
      
      // using custom CursorPreparer to get access to mongo DBCursor for explain() etc. :
      // (rather than mgo.find(springMongoQuery, DCEntity.class, collectionName)) 
      CursorProviderQueryCursorPreparer cursorProvider = new CursorProviderQueryCursorPreparer(mgo,
            springMongoQuery, doExplainQuery, maxScan, maxTime);
      
      // TODO request priority : privilege INDEXED (Queriable) fields for query & sort !!!
      // TODO LATER explode in queryParsingContext.isHasNoIndexedField() if not dev nor paid...
      
      List<DCEntity> foundEntities = mgo.find(springMongoQuery, DCEntity.class,
            storageModel.getCollectionName(), cursorProvider);
      
      if (isDebug) {
         //Populate cxf exchange with info concerning the query :
         //TODO also (?) context.set("query.explain", cursorProvider.getQueryExplain()) & getQuery(),
         // sortExplain & getSort(), getSpecial() (ex. maxScan), getOptions()?
         // or even Model (type(s), fields)...
         // TODO constants
         Map<String, Object> debugCtx = serverRequestContext.getDebug(); // never null because isDebug()
         debugCtx.put("collectionName", cursorProvider.getCursorPrepared().getCollection().getName());
         debugCtx.put("mongoQuery", cursorProvider.getCursorPrepared().getQuery());
         debugCtx.put("springMongoQuery", springMongoQuery);
         debugCtx.put(DEBUG_QUERY_EXPLAIN, cursorProvider.getQueryExplain());
         debugCtx.put("sortExplain", cursorProvider.getCursorPrepared().explain());
         // NB. sort explain is end cursor explain if sort (but we always sort, at worst on _chAt)
         debugCtx.put("mongoIndexes", cursorProvider.getCursorPrepared().getCollection().getIndexInfo());
         debugCtx.put("hasNoIndexedField", queryParsingContext.isHasNoIndexedField());
         debugCtx.put("maxScan", maxScan);
         // NB. can't get special so adding maxScan
         debugCtx.put("options", cursorProvider.getCursorPrepared().getOptions());
         // NB. queryOptions = cursorTailable, slaveOk, oplogReplay, noCursorTimeout, awaitData, exhaust http://api.mongodb.org/cplusplus/1.5.4/namespacemongo.html
         debugCtx.put("readPreference", cursorProvider.getCursorPrepared().getReadPreference());
         debugCtx.put("serverAddress", cursorProvider.getCursorPrepared().getServerAddress());
         debugCtx.put(DEBUG_WARNINGS, queryParsingContext.getWarnings());
         //explainCtx.put("parsedQuery", exchange.get("dc.query.parsedQuery")
      }

      if (maxScan != 0 && doExplainQuery) { // if !doExplainQuery can't check "scanned"
         // (if maxScan == 0 it is infinite and its limit can't be reached)
         if (foundEntities.size() < springMongoQuery.getLimit()
               && (((int) cursorProvider.getQueryExplain().get("nscanned")) >= maxScan
               || ((int) cursorProvider.getCursorPrepared().explain().get("nscanned")) >= maxScan)) {
            if (isDebug) {
               serverRequestContext.getDebug().put("hasReachedMaxScan", true);
            } else {
               // NB. and not nscannedObjects which may be lower because some scans have been eaten
               // by sorts, multiple criteria etc. see http://docs.mongodb.org/manual/reference/method/cursor.explain/
               throw new QueryException("Query or sort with non indexed fields has reached maxScan (" + maxScan
                     + ") before document limit (found " + foundEntities.size() + "<" + springMongoQuery.getLimit()
                     + ") , meaning some documents can't be found without prohibitive cost. "
                     + "Use only indexed fields, or if you really want the few documents already "
                     + "found lower limit, or if in Model design mode add indexes. "
                     + "Retry in debug mode to get more information.");
            }
         }
      }
   
      return foundEntities;
   }


   public int getMaxScan() {
      return maxScan;
   }

   public int getMaxStart() {
      return maxStart;
   }

   public int getMaxLimit() {
      return maxLimit;
   }
   
   /** for tests */
   public void setMaxScan(int maxScan) {
      this.maxScan = maxScan;
   }

   /** for tests */
   public void setMaxStart(int maxStart) {
      this.maxStart = maxStart;
   }

   /** for tests */
   public void setMaxLimit(int maxLimit) {
      this.maxLimit = maxLimit;
   }

}
