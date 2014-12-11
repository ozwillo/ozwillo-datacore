package org.oasis.datacore.core.entity.query.ldp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.message.Exchange;
import org.oasis.datacore.core.entity.EntityModelService;
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
import org.oasis.datacore.core.meta.model.DCSecurity;
import org.oasis.datacore.core.security.DCUserImpl;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.server.MonitoringLogServiceImpl;
import org.oasis.datacore.rest.server.cxf.CxfJaxrsApiProvider;
import org.oasis.datacore.rest.server.parsing.model.DCQueryParsingContext;
import org.oasis.datacore.rest.server.parsing.model.DCResourceParsingContext;
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

import com.mongodb.DBObject;


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

   private static final Logger logger = LoggerFactory.getLogger(LdpEntityQueryServiceImpl.class);
   
   private static Set<String> findConfParams = new HashSet<String>();
   static {
      // TODO rather using Enum, see BSON$RegexFlag
      findConfParams.add("start");
      findConfParams.add("limit");
      findConfParams.add("debug");
      findConfParams.add("format");
   }
   private static Map<String,DCField> dcEntityIndexedFields = new HashMap<String,DCField>();
   static {
      // TODO rather using Enum, see BSON$RegexFlag
      dcEntityIndexedFields.put(DCResource.KEY_URI, new DCField(DCEntity.KEY_URI, "string", true, 100000));
      //dcEntityIndexedFields.put(DCResource.KEY_DCCREATED, new DCField(DCEntity.KEY_CR_AT, "date", true, 100000)); // LATER ?
      //dcEntityIndexedFields.put(DCResource.KEY_DCCREATOR, new DCField(DCEntity.KEY_CH_BY, "string", true, 100000)); // LATER ?
      dcEntityIndexedFields.put(DCResource.KEY_DCMODIFIED, new DCField(DCEntity.KEY_CH_AT, "date", true, 100000));
      //dcEntityIndexedFields.put(DCResource.KEY_DCCONTRIBUTOR, new DCField(DCEntity.KEY_CH_BY, "string", true, 100000)); // LATER ?
      //dcEntityIndexedFields.put("o:allReaders", new DCListField(DCEntity.KEY_AR... // don't allow to look it up
   }

   @Value("${datacoreApiServer.query.detailedErrorsMode}")
   private boolean detailedErrorsMode = true;
   
   /** default maximum number of documents to scan when fulfilling a query, overriden by
    * DCFields', themselves limited by DCModel's. 0 means no limit (for tests), else ex.
    * 1000 (secure default), 100000 (on query-only nodes using secondary & timeout)... 
    * http://docs.mongodb.org/manual/reference/operator/meta/maxScan/ */
   @Value("${datacoreApiServer.query.maxScan}")
   private int maxScan;
   /** TODO BETTER ELSE CANT PAGINATE default maximum start position : 500... */
   @Value("${datacoreApiServer.query.maxStart}")
   private int maxStart;
   /** default maximum number of documents returned : 100... */
   @Value("${datacoreApiServer.query.maxLimit}")
   private int maxLimit;
   /** default number of documents returned : 10... */
   @Value("${datacoreApiServer.query.defaultLimit}")
   protected int defaultLimit;

   /** to get storage model */
   @Autowired
   private DCModelService modelService;

   /** to fill entity's models cache */
   @Autowired
   private EntityModelService entityModelService;
   
   @Autowired
   @Qualifier("datacoreSecurityServiceImpl")
   private DatacoreSecurityService datacoreSecurityService;
   
   @Autowired
   private /*MongoOperations*/DatacoreMongoTemplate mgo; // TODO remove it by hiding it in services
   
   @Autowired
   private QueryParsingService queryParsingService;

   @Autowired
   private MonitoringLogServiceImpl monitoringLogServiceImpl;

   /** to access debug switch & put its explained results */
   @Autowired
   private CxfJaxrsApiProvider cxfJaxrsApiProvider;

   
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
      DCModelBase storageModel = modelService.getStorageModel(modelType); // TODO cache, in context ?
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

      // parsing query parameters criteria according to model :
      DCQueryParsingContext queryParsingContext = new DCQueryParsingContext(model, storageModel);
      parseQueryParameters(params, queryParsingContext);
      if (queryParsingContext.hasErrors()) {
         String msg = DCResourceParsingContext.formatParsingErrorsMessage(queryParsingContext, detailedErrorsMode);
         throw new QueryException(msg);
      } // else TODO if warnings return them as response header ?? or only if failIfWarningsMode ??
      
      // add security :
      boolean guestForbidden = addSecurityIfNotGuestForbidden(queryParsingContext);
      if (guestForbidden) {
         return new ArrayList<DCEntity>(0); // TODO or exception ??
      }
      
      // adding paging & sorting :
      if (start == null) { // should not happen
         start = 0;
      } else if (start > maxStart) {
         start = maxStart; // max, else prefer ranged query ; TODO or error message ?
      }
      if (limit == null) { // should not happen
         limit = defaultLimit;
      } else if (limit > maxLimit) {
         limit = maxLimit; // max, else prefer ranged query ; TODO or error message ?
      }
      Sort sort = queryParsingContext.getSort();
      if (sort == null) {
         // TODO sort by default : configured in model (last modified date, uri,
         // iri?, types?, owners? other fields...)
         sort = new Sort(Direction.DESC, DCEntity.KEY_CH_AT); // new Sort(Direction.ASC, "_uri")...
      }
      
      Query springMongoQuery = new Query(queryParsingContext.getCriteria())
         .with(sort).skip(start).limit(limit); // TODO rather range query, if possible on sort field
      
      List<DCEntity> foundEntities = executeMongoDbQuery(springMongoQuery, queryParsingContext);
      
      if (logger.isDebugEnabled()) {
         logger.debug("Done Spring Mongo query: " + springMongoQuery
               + "\n   in collection " + storageModel.getCollectionName()
               + " from Model " + model.getName() + " and parameters: " + params
               + "\n   with result nb " + foundEntities.size());
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
      
      parameterLoop : for (String fieldPath : params.keySet()) {
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

         StringBuilder entityFieldPathSb;
         
         // handling DCEntity native (indexed) fields :
         String topFieldPathElement = fieldPathElements[0];
         boolean isDcEntityIndexedField = false;
         DCField dcField = dcEntityIndexedFields.get(topFieldPathElement);
         if (dcField != null) {
            isDcEntityIndexedField = true;
            entityFieldPathSb = new StringBuilder(dcField.getName()); // mapping @id to _uri etc.
         } else {
            dcField = dcModel.getGlobalField(topFieldPathElement);
            entityFieldPathSb = new StringBuilder("_p."); // almost the same fieldPath for mongodb
            entityFieldPathSb.append(topFieldPathElement);
            
         if (dcField == null) {
            queryParsingContext.addError("In type " + dcModel.getName() + ", can't find field with path elements "
                  + Arrays.asList(fieldPathElements) + " : can't find field for first path element "
                  + fieldPathElements[0]);
            continue;
         }
         
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
            
            entityFieldPathSb.append(".");
            
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
                 } else {
                    entityFieldPathSb.append(fieldPathElement);
                 }
                 // TODO TODO check that indexed (or set low limit) ??
               } while (dcField != null && "list".equals(dcField.getType()));
               
            } else if ("resource".equals(dcField.getType())) { // TODO noooooooooooo embedded / subresource
               queryParsingContext.addError("Found criteria requiring join : in type " + dcModel.getName() + ", field "
                     + fieldPath + " (" + i + "th in field path elements " + Arrays.asList(fieldPathElements)
                     + ") can't be done in findDataInType, do it rather on client side");
               continue parameterLoop; // TODO boum
               
            } else if ("i18n".equals(dcField.getType())) {
               dcField = handleI18nField(dcField, fieldPathElement,
                     queryParsingContext, entityFieldPathSb, fieldPathElements, i);
               // TODO TODO check that indexed (or set low limit) ??
               
            } else {
               queryParsingContext.addError("In type " + dcModel.getName() + ", can't find field with path elements"
                     + Arrays.asList(fieldPathElements) + ": can't go below " + i + "th element " 
                     + fieldPathElement + ", because field type is neither map nor list nor i18n but " + dcField.getType());
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
         
         // handling leaf fields :
         // TODO LATER i18n default : (CONFLICTS WITH $elemMatch)
         /*if ("i18n".equals(dcField.getType())) {
            // by default, search on an i18n field searches on the value (and not the language) :
            dcField = handleI18nField(dcField, DCI18nField.KEY_VALUE,
                  queryParsingContext, entityFieldPathSb, fieldPathElements, 0);
         }*/
         
         }
         
         List<String> values = params.get(fieldPath);
         if (values == null || values.isEmpty()) {
            queryParsingContext.addError("Missing value for parameter " + fieldPath);
            continue; // should not happen
         }

         String entityFieldPath = entityFieldPathSb.toString();
         queryParsingContext.enterCriteria(entityFieldPath , values.size());
         try  {
            // parsing multiple values (of a field that is mentioned several times) :
            // (such as {limit=[10], founded=[>"-0143-04-01T00:00:00.000Z", <"-0043-04-02T00:00:00.000Z"]})
            // NB. can't be done by merely chaining .and(...)'s because of mongo BasicDBObject limitations, see
            // http://www.mkyong.com/java/due-to-limitations-of-the-basicdbobject-you-cant-add-a-second-and/
            for (String operatorAndValue : values) {
               try {
                  // parsing query parameter criteria according to model field :
                  queryParsingService.parseCriteriaFromQueryParameter(operatorAndValue,
                        dcField, queryParsingContext);
               } catch (Exception ex) {
                  queryParsingContext.addError("Error while parsing query criteria " + fieldPath
                        + operatorAndValue, ex);
               }
            }
            
         } finally {
            queryParsingContext.exitCriteria();
         }

      }
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
      }
      String entityFieldPathElement = fieldPathElement;
      entityFieldPathSb.append(entityFieldPathElement);
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
      // translating to mongo if needed :
      String entityFieldPathElement;
      switch (fieldPathElement) {
      case DCResource.KEY_I18N_VALUE_JSONLD :
         entityFieldPathElement = DCI18nField.KEY_VALUE;
         break;
      case DCResource.KEY_I18N_LANGUAGE_JSONLD :
         entityFieldPathElement = DCI18nField.KEY_LANGUAGE;
         break;
      default :
         entityFieldPathElement = fieldPathElement;
      }
      // "list of map"-like handling :
      // (leaf field : i18n is obligatorily a list of map of strings)
      dcField = ((DCListField) dcField).getListElementField();
      dcField = handleMapField(dcField, entityFieldPathElement,
            queryParsingContext, entityFieldPathSb, fieldPathElements, i);
      return dcField;
   }


   private boolean addSecurityIfNotGuestForbidden(DCQueryParsingContext queryParsingContext) {
      // TODO Q how to make all tests still work : null case ? other prop ? disable it ??
      // TODO better : in SecurityQueryEnricher ? rather in (Query)ParsingContext ?!?
      // TODO (LATER ?) on all sub Resources !!
      // TODO LATER in findDataInAllTypes(), on all root Resources
      DCSecurity modelSecurity = queryParsingContext.peekModel().getSecurity();
      if (!modelSecurity.isGuestReadable()) {
         // TODO Q or (b) also GUEST OK when empty ACL ?
         // NO maybe dangerous because *adding* a group / role to ACL would *remove* GUEST from it
         // so solution would be : API (i.e. Social Graph) as (a) but storage translates it to (b), allows to store less characters
         // requires adding an $or criteria : _w $size 0 $or _w $in currentUserRoles
         // which is more complex and might be (TODO test) worse performance-wise
         // => OPT LATER
         // (in any way, there'll always be a balance to find between performance and storage)
         
         DCUserImpl user = datacoreSecurityService.getCurrentUser();
         if (user.isGuest()) {
            return true; // TODO or exception ??
         }
         
         if (!user.isAdmin()
               && !modelSecurity.isAuthentifiedReadable() // NB. not guest
               && !modelSecurity.isResourceAdmin(user)
               && !modelSecurity.isResourceReader(user)) {
            queryParsingContext.getCriteria().and("_ar").in(user.getEntityGroups());
         } // else (datacore global or model-scoped) admin, so no security check
      } // else public, so no security check
      
      return false;
   }

   
   private List<DCEntity> executeMongoDbQuery(Query springMongoQuery,
         DCQueryParsingContext queryParsingContext) throws QueryException {
      DCModelBase model = queryParsingContext.peekModel();
      DCModelBase storageModel = queryParsingContext.peekStorageModel();
      if (!model.getName().equals(storageModel.getName())) {
         // adding criteria on model type when storage larger than model :
         springMongoQuery.addCriteria(new Criteria(DCEntity.KEY_T).is(model.getName()));
      }
      
      // compute overall maxScan :
      // (BEWARE it is NOT the max amount of doc returned because sorts or multiple
      // criteria can eat some scans)
      int maxScan = this.maxScan;
      if (queryParsingContext.getAggregatedQueryLimit() > maxScan) {
         // allow at least enough scan for expected results
         maxScan = queryParsingContext.getAggregatedQueryLimit();
      }
      if (storageModel.getMaxScan() > 0) {
         // limit maxScan : take smallest
         maxScan = (maxScan > storageModel.getMaxScan()) ?
               storageModel.getMaxScan() : maxScan;
      }

      boolean debug = false;
      //TODO debug = context.get("debug") (put by debug() operation) || context.get("headers").get("X-Datacore-Debug")

      Exchange exchange = cxfJaxrsApiProvider.getExchange();
      debug = exchange != null // else not called through REST
            && Boolean.TRUE.equals(exchange.get("dc.params.debug")); // NB. was set by DatacoreApiImpl, TODO rather in custom context

      boolean doExplainQuery = queryParsingContext.isHasNoIndexedField() || debug;
      
      // using custom CursorPreparer to get access to mongo DBCursor for explain() etc. :
      // (rather than mgo.find(springMongoQuery, DCEntity.class, collectionName)) 
      CursorProviderQueryCursorPreparer cursorProvider = new CursorProviderQueryCursorPreparer(mgo,
            springMongoQuery, doExplainQuery, maxScan);
      // TODO LATER mongo 2.6 maxTimeMs() http://docs.mongodb.org/manual/reference/method/cursor.maxTimeMS/#cursor.maxTimeMS

      String collectionName = storageModel.getCollectionName(); // TODO LATER for polymorphism...
      List<DCEntity> foundEntities = mgo.find(springMongoQuery, DCEntity.class, collectionName, cursorProvider);
      
      if (debug) {
         DBObject sortExplain = cursorProvider.getCursorPrepared().explain();
         //TODO context.set("query.explain", cursorProvider.getQueryExplain()) & getQuery(),
         // sortExplain & getSort(), getSpecial() (ex. maxScan), getOptions()?,
         // getReadPreference(), getServerAddress(), collectionName, or even Model (type(s), fields)...
         //Populate cxf exchange with info concerning the query.
         try {
            exchange.put("dc.query.explain", cursorProvider.getQueryExplain()); // TODO constants
            exchange.put("dc.query.query", cursorProvider.getCursorPrepared().getQuery());
            exchange.put("dc.query.sortExplain", sortExplain);
         } catch (Exception e) {
            if (logger.isDebugEnabled()) {
               logger.debug("Unable to find context.");
            }
         }
      }
      
      if (maxScan != 0 && queryParsingContext.isHasNoIndexedField()) {
         // (if maxScan == 0 it is infinite and its limit can't be reached)
         if (foundEntities.size() < springMongoQuery.getLimit()
               && ((int) cursorProvider.getQueryExplain().get("nscanned")) == maxScan) {
            // NB. and not nscannedObjects which may be lower because some scans have been eaten
            // by sorts, multiple criteria etc. see http://docs.mongodb.org/manual/reference/method/cursor.explain/
            throw new QueryException("Query with non indexed fields has reached maxScan (" + maxScan
                  + ") before document limit (found " + foundEntities.size() + "<" + springMongoQuery.getLimit()
                  + ") , meaning some documents can't be found without prohibitive cost. "
                  + "Use only indexed fields, or if you really want the few documents already "
                  + "found lower limit, or if in Model design mode add indexes.");
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
