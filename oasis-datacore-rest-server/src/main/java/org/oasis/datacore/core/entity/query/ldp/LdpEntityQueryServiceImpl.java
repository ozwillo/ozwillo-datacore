package org.oasis.datacore.core.entity.query.ldp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.query.QueryException;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCSecurity;
import org.oasis.datacore.core.security.DCUserImpl;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.parsing.model.DCQueryParsingContext;
import org.oasis.datacore.rest.server.parsing.model.DCResourceParsingContext;
import org.oasis.datacore.rest.server.parsing.service.QueryParsingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoOperations;
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

   private static final Logger logger = LoggerFactory.getLogger(EventService.class);
   
   private static Set<String> findConfParams = new HashSet<String>();
   static {
      // TODO rather using Enum, see BSON$RegexFlag
      findConfParams.add("start");
      findConfParams.add("limit");
   }
   
   /** TODO mock */
   @Autowired
   @Qualifier("datacoreSecurityServiceImpl")
   private DatacoreSecurityService datacoreSecurityService;
   
   @Autowired
   private MongoOperations mgo; // TODO remove it by hiding it in services
   
   @Autowired
   private QueryParsingService queryParsingService;

   @Override
   public List<DCEntity> findDataInType(DCModel dcModel, Map<String, List<String>> params,
         Integer start, Integer limit) throws QueryException {
      boolean detailedErrorsMode = true; // TODO
      
      String modelType = dcModel.getName(); // for error logging

      // parsing query parameters criteria according to model :
      DCQueryParsingContext queryParsingContext = new DCQueryParsingContext(dcModel, null);
      
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
         DCField dcField = dcModel.getGlobalField(fieldPathElements[0]);
         if (dcField == null) {
            queryParsingContext.addError("In type " + modelType + ", can't find field with path elements "
                  + Arrays.asList(fieldPathElements) + " : can't find field for first path element "
                  + fieldPathElements[0]);
            continue;
         }
         
         // finding the leaf field
         
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
            if ("map".equals(dcField.getType())) {
               dcField = ((DCMapField) dcField).getMapFields().get(fieldPathElement);
               if (dcField == null) {
                  queryParsingContext.addError("In type " + modelType + ", can't find field with path elements"
                        + Arrays.asList(fieldPathElements) + ": can't go below " + i + "th path element "
                        + fieldPathElement + ", because field is unkown");
                  continue parameterLoop;
               }
            } else if ("resource".equals(dcField.getType())) {
               queryParsingContext.addError("Found criteria requiring join : in type " + modelType + ", field "
                     + fieldPath + " (" + i + "th in field path elements " + Arrays.asList(fieldPathElements)
                     + ") can't be done in findDataInType, do it rather on client side");
               continue parameterLoop; // TODO boum
            } else {
               queryParsingContext.addError("In type " + modelType + ", can't find field with path elements"
                     + Arrays.asList(fieldPathElements) + ": can't go below " + i + "th element " 
                     + fieldPathElement + ", because field is neither map nor list but " + dcField.getType());
               continue parameterLoop; // TODO boum
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
         
         // TODO (mongo)operator for error & in parse ?
         String entityFieldPath = "_p." + fieldPath; // almost the same fieldPath for mongodb (?)

         queryParsingContext.enterCriteria(entityFieldPath, values.size());
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
      

      if (queryParsingContext.hasErrors()) {
         String msg = DCResourceParsingContext.formatParsingErrorsMessage(queryParsingContext, detailedErrorsMode);
         throw new QueryException(msg);
      } // else TODO if warnings return them as response header ?? or only if failIfWarningsMode ??
      
      
      // adding security :
      // TODO Q how to make all tests still work : null case ? other prop ? disable it ??
      // TODO better : in SecurityQueryEnricher ? rather in (Query)ParsingContext ?!?
      // TODO (LATER ?) on all sub Resources !!
      // TODO LATER in findDataInAllTypes(), on all root Resources
      DCSecurity modelSecurity = dcModel.getSecurity();
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
            return new ArrayList<DCEntity>(0); // TODO or exception ??
         }
         
         if (!user.isAdmin()
               && !modelSecurity.isAuthentifiedReadable() // NB. not guest
               && !modelSecurity.isResourceAdmin(user)
               && !modelSecurity.isResourceReader(user)) {
            queryParsingContext.getCriteria().and("_ar").in(user.getEntityGroups());
         } // else (datacore global or model-scoped) admin, so no security check
      } // else public, so no security check
      
      // adding paging & sorting :
      if (start > 500) {
         start = 500; // max (conf'ble in model ?), else prefer ranged query ; TODO or error message ?
      }
      if (limit > 50) {
         limit = 50; // max (conf'ble in model ?), else prefer ranged query ; TODO or error message ?
      }
      Sort sort = queryParsingContext.getSort();
      if (sort == null) {
         // TODO sort by default : configured in model (uri, last modified date, iri?, other fields...)
         sort = new Sort(Direction.ASC, "_uri");
      }
      Query springMongoQuery = new Query(queryParsingContext.getCriteria())
         .with(sort).skip(start).limit(limit); // TODO rather range query, if possible on sort field
         
      // executing the mongo query :
      String collectionName = dcModel.getCollectionName(); // TODO getType() or getCollectionName(); for weird type names ??
      List<DCEntity> foundEntities = mgo.find(springMongoQuery, DCEntity.class, collectionName);
      
      if (logger.isDebugEnabled()) {
         logger.debug("Done Spring Mongo query: " + springMongoQuery
               + "\n   in collection " + collectionName + " from Model " + modelType + " and parameters: " + params
               + "\n   with result nb " + foundEntities.size());
      }
      
      return foundEntities;
   }
  
}
