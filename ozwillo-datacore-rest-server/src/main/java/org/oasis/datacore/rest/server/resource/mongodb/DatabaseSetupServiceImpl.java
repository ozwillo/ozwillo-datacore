package org.oasis.datacore.rest.server.resource.mongodb;

import java.util.*;

import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.oasis.datacore.core.entity.DatabaseSetupService;
import org.oasis.datacore.core.entity.NativeModelService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCI18nField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.historization.exception.HistorizationException;
import org.oasis.datacore.historization.service.impl.HistorizationServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Indexes;

/**
 * Creates indexes
 * @author mdutoo
 *
 */
@Component
public class   DatabaseSetupServiceImpl implements DatabaseSetupService {

   protected final Logger logger = LoggerFactory.getLogger(getClass());

   /** impl, to be able to modify it
    * TODO LATER extract interface */
   @Autowired
   protected DataModelServiceImpl modelAdminService;
   @Autowired
   protected NativeModelService nativeModelService;

   @Autowired
   private MongoTemplate mongoTemplate;

   @Autowired
   private HistorizationServiceImpl historizationService;
   
   
   @Override
   public boolean cleanModel(DCModelBase model) {
      if (!model.isStorage()) {
         // TODO rm indexes specific to it in inheriting models also
         return cleanDataOfCreatedModel(model);
         // TODO LATER remove indexes specific to this model only (OPT and restores overriden ones if any)
      }
      if (model.getCollectionName().endsWith(HistorizationServiceImpl.HISTORIZATION_COLLECTION_SUFFIX)) {
         return false;
      }
      
      mongoTemplate.dropCollection(model.getCollectionName()); // delete data // storageModel.getAbsoluteName()
      // TODO rm indexes specific to it in inheriting models also
   
      // TODO LATER make historizable & contributable more than storage models !
      if (model.isHistorizable()) {
         try {
            String historizationCollectionName = historizationService.getHistorizedCollectionNameFromOriginalModel(model);
            mongoTemplate.dropCollection(historizationCollectionName);
         } catch (HistorizationException e) {
            logger.error("error while dropping (historization of) model "
                  + model.getName(), e);
         }
      }
      
      if (model.isContributable()) {
         String contributionCollectionName = model.getName() + ".c"; // TODO TODOOOOOO move
         mongoTemplate.dropCollection(contributionCollectionName);
      }
      
      return true;
   }
   
   @Override
   public boolean cleanDataOfCreatedModel(DCModelBase model) {
      DCModelBase storageModel;
      Query deleteQuery = new Query();
      if (model.isStorage()) {
         storageModel = model;
      } else { // not only if isInstanciable (ex. geocifr), also ex. geoci
         storageModel = modelAdminService.getStorageModel(model);
         if (storageModel == null) {
            return false;
         }
         deleteQuery.addCriteria(new Criteria(DCEntity.KEY_T).is(model.getName()));
      }
      if (storageModel.getCollectionName().endsWith(HistorizationServiceImpl.HISTORIZATION_COLLECTION_SUFFIX)) {
         return false;
      }
      // delete (rather than drop & recreate !) : 
      mongoTemplate.remove(deleteQuery, storageModel.getCollectionName());

      // TODO LATER make historizable & contributable more than storage models !
      if (model.isHistorizable()) {
         try {
            mongoTemplate.remove(deleteQuery, historizationService.getHistorizedCollectionNameFromOriginalModel(model));
         } catch (HistorizationException e) {
            throw new RuntimeException("Historization init error for Model " + model.getName(), e);
         }
      }
      
      if (storageModel.isContributable()) {
         String contributionCollectionName = storageModel.getName() + ".c"; // TODO TODOOOOOO move
         mongoTemplate.remove(new Query(), contributionCollectionName);
      }
      return true;
   }
   
   
   @Override
   public boolean ensureCollectionAndIndices(DCModelBase model, boolean deleteCollectionsFirst) {
      if (!model.isStorage()) {
         model = modelAdminService.getStorageModel(model);
         if (model == null) {
            return false;
         }
      }
      if (model.getCollectionName().endsWith(HistorizationServiceImpl.HISTORIZATION_COLLECTION_SUFFIX)) {
         return false;
      }
      if (deleteCollectionsFirst) {
         // cleaning data first
         mongoTemplate.dropCollection(model.getCollectionName());
         // TODO better when not storage
      }
      boolean collectionAlreadyExists = ensureCollectionAndIndices(model);
      
      if(model.isHistorizable()) { // TODO when not isStorage
         collectionAlreadyExists = ensureHistorizedCollectionAndIndices(model, deleteCollectionsFirst)
               || collectionAlreadyExists;
      }
      
      if (model.isContributable()) { // TODO when not isStorage
         collectionAlreadyExists = ensureContributedCollectionAndIndices(model, deleteCollectionsFirst);
      }
      
      return collectionAlreadyExists;
   }

   public boolean ensureHistorizedCollectionAndIndices(DCModelBase model, boolean deleteCollectionsFirst) {
      DCModelBase historizedModel;
      try {
         ///historizedModel = historizationService.getOrCreateHistorizationModel(model);
         historizedModel = historizationService.getHistorizationModel(model);
         if (historizedModel == null) {
            historizedModel = historizationService.createHistorizationModel(model);
         }
         String historizedCollectionName = historizationService.getHistorizedCollectionNameFromOriginalModel(model);
         if (deleteCollectionsFirst) {
            // cleaning data first
            mongoTemplate.dropCollection(historizedCollectionName);
         }
         
         //boolean collectionAlreadyExists = ensureGenericCollectionAndIndices(historizedModel); // NOO only use is GET(uri, version)
         boolean collectionAlreadyExists = mongoTemplate.collectionExists(historizedCollectionName);
         List<IndexModel> list = new ArrayList<>();

         // compound index on uri & version :
         IndexOptions indexOptions = new IndexOptions().unique(true);
         mongoTemplate.getCollection(historizedCollectionName).createIndex(Indexes.descending(DCEntity.KEY_URI, DCEntity.KEY_V), indexOptions);
//         mongoTemplate.getCollection(historizedCollectionName).createIndex(
//               new BasicDBObject(DCEntity.KEY_URI, 1).append(DCEntity.KEY_V, 1),
//               new BasicDBObject("unique", true));
         // NB. does nothing if already exists http://docs.mongodb.org/manual/tutorial/create-an-index/
         return collectionAlreadyExists;
      } catch (HistorizationException e) {
         throw new RuntimeException("Historization init error of Model " + model.getName(), e);
      }
   }

   public boolean ensureContributedCollectionAndIndices(DCModelBase model, boolean deleteCollectionsFirst) {
      //contributionModel = contributionService.createContributionModel(model); // TODO TODOOO
      if (deleteCollectionsFirst) {
         // cleaning data first
         String contributionCollectionName = model.getName() + ".c"; // TODO TODOOOOOO move
         mongoTemplate.dropCollection(contributionCollectionName);
      }
      // TODO TODOOOOO compound index on uri and contributor / organization ?!
      return false; // ensureCollectionAndIndices(historizedModel); // TODO TODOOOO
   }

   /**
    * 
    * @param model must be storage
    * @return
    */
   private boolean ensureCollectionAndIndices(DCModelBase model) {
      boolean res = ensureGenericCollectionAndIndices(model);
      BasicDBObject dataEntityUniqueIndex = new BasicDBObject();
      if (model.isMultiProjectStorage()) {
         dataEntityUniqueIndex.append(DCEntity.KEY_B, 1);
      }
      // TODO isMultiVersionStorage for History
      IndexOptions indexOptions = new IndexOptions().unique(true);
      mongoTemplate.getCollection(model.getCollectionName()).createIndex(Indexes.descending(DCEntity.KEY_URI), indexOptions);

//      dataEntityUniqueIndex.append(DCEntity.KEY_URI, 1);
//      mongoTemplate.getCollection(model.getCollectionName()).createIndex(
//            dataEntityUniqueIndex, new BasicDBObject("unique", true)); // TODO dropDups ??
      // NB. does nothing if already exists http://docs.mongodb.org/manual/tutorial/create-an-index/
      return res;
   }
   /**
    * 
    * @param model must be storage
    * @return
    */
   private boolean ensureGenericCollectionAndIndices(DCModelBase model) {
      MongoCollection<Document> coll;
      boolean collectionAlreadyExists = mongoTemplate.collectionExists(model.getCollectionName()); 
      if (collectionAlreadyExists) {
         coll = mongoTemplate.getCollection(model.getCollectionName());
      } else {
         coll = mongoTemplate.createCollection(model.getCollectionName());
      }
      
      LinkedHashSet<String> requiredIndexes = new LinkedHashSet<String>();

      // computing static indexes
      DCModelBase nonExposedNativeModel = nativeModelService.getNonExposedNativeModel(model);
      for (String nativeFieldName : nativeModelService.getNativeExposedOrNotIndexedFieldNames(model)) {
         DCField nativeField = nonExposedNativeModel.getGlobalField(nativeFieldName);
         String storageReadName = nativeField.getStorageReadName();
         if (storageReadName != null) {
            if (!DCEntity.KEY_URI.equals(storageReadName)
                  && !DCEntity.KEY_B.equals(storageReadName)) {
               requiredIndexes.add(storageReadName); // for query security
               // (and not on other storage names which must be indexed
               // from being defined in other fields)
            } // else done outside this method
         } // else not stored i.e. soft computed
      }
      
      // computing field indices
      // of THIS storage model's collection, from wherever stored model they come :
      // NB. models inheriting from this one but NOT stored in it are triggered by
      // ModelResourceDCListener.impactxxx
      // (NB. ideally models outside this project can inherit from but can't be stored in this model)
      Collection<DCModelBase> storedModels = modelAdminService.getStoredModels(model); // including this model
      for (DCModelBase storedModel : storedModels) {
         // stored in model so indexes must be ensured for its fields :
         computeFieldsIndices(coll, DCEntity.KEY_P, storedModel.getGlobalFieldMap().values(), requiredIndexes);
      }
      
      // getting existing indexes
      Set<String> nonUniqueSingleIndexedPathes = getNonUniqueSingleIndexedPathes(coll);

      // getting new (for logging purpose only) & obsolete indexes (LATER OPT2 incompatible ones)
      Set<String> newIndexes = new HashSet<String>(requiredIndexes);
      newIndexes.removeAll(nonUniqueSingleIndexedPathes);
      Set<String> indexesToBeDropped = new HashSet<String>(nonUniqueSingleIndexedPathes);
      indexesToBeDropped.removeAll(requiredIndexes);
      
      // logging
      if (logger.isDebugEnabled()
            || logger.isInfoEnabled() && !newIndexes.isEmpty() || !indexesToBeDropped.isEmpty()) {
         String msg = "Indexes of " + model.getAbsoluteName() + ": \n"
               + "   new: " + newIndexes + "\n"
               + "   to be dropped: " + indexesToBeDropped + "\n";
         if (logger.isDebugEnabled()) {
            logger.debug(msg
                  + "   required: " + requiredIndexes + "\n"
                  + "   existing: " + nonUniqueSingleIndexedPathes + "\n");
         } else {
            logger.info(msg);
         }
      }
      
      // actual removal & creation :
      for (String indexToBeDropped : indexesToBeDropped) {
         coll.dropIndex(new BasicDBObject(indexToBeDropped, 1)); // must match spec (key & type)
      }
      for (String requiredIndex : requiredIndexes) { // or newIndexes,
         // but anyway does nothing if same already exists http://docs.mongodb.org/manual/tutorial/create-an-index/
         coll.createIndex(new BasicDBObject(requiredIndex, 1));
      }
      
      return collectionAlreadyExists;
   }

   /**
    * @param coll
    * @return existing indexes
    */
   private Set<String> getNonUniqueSingleIndexedPathes( MongoCollection<Document> coll) {
      ListIndexesIterable<Document> mongoIndexInfos = coll.listIndexes();
      Set<String> nonUniqueSingleIndexedPathes = new HashSet<>();
      for (Document mongoIndexInfo : mongoIndexInfos) {
         Object uniqueFound = mongoIndexInfo.get("unique");
         if (uniqueFound != null && (Boolean) uniqueFound) {
            continue;
         }
         Set<String> keyNames = ((Document) mongoIndexInfo.get("key")).keySet();
         if (keyNames.size() != 1) {
            continue;
         }
         nonUniqueSingleIndexedPathes.add(keyNames.iterator().next());
      }
      return nonUniqueSingleIndexedPathes;
   }

   /**
    * 
    * @param coll
    * @param prefixWithoutDot
    * @param globalFields map or top-level resource field (NB. subresource fields
    * not supported for now, since can't know whether embedded or not)
    * @param requiredIndexes
    */
   private void computeFieldsIndices( MongoCollection<Document> coll, String prefixWithoutDot,
         Collection<DCField> globalFields, LinkedHashSet<String> requiredIndexes) {
      for (DCField globalField : globalFields) {
         computeMapOrResourceFieldPathAndIndices(coll, prefixWithoutDot, globalField, requiredIndexes);
      }
   }

   private void computeMapOrResourceFieldPathAndIndices( MongoCollection<Document> coll, String prefixWithoutDot,
         DCField globalField, LinkedHashSet<String> requiredIndexes) {
      String storageReadName = globalField.getStorageReadName();
      if (storageReadName == null) {
         return; // not stored i.e. soft computed therefore not queriable
      }
      String prefixedGlobalFieldStorageName = prefixWithoutDot + "." + storageReadName;
      computeFieldIndices(coll, prefixedGlobalFieldStorageName, globalField, requiredIndexes);
   }
   private void computeFieldIndices( MongoCollection<Document> coll, String prefixWithoutDot,
         DCField globalField, LinkedHashSet<String> requiredIndexes) {
      switch (DCFieldTypeEnum.getEnumFromStringType(globalField.getType())) {
      case LIST:
         DCField listElementField = ((DCListField) globalField).getListElementField();
         computeFieldIndices(coll, prefixWithoutDot, listElementField, requiredIndexes);
         break;
      case RESOURCE:
         DCResourceField resourceField = ((DCResourceField) globalField);
         if (resourceField.isStorage() != null && resourceField.isStorage()) {
            DCModelBase submodel =  modelAdminService.getModelBase(resourceField.getResourceType());
            computeFieldsIndices(coll, prefixWithoutDot, submodel.getGlobalFields(), requiredIndexes);
         }
         break;
      case MAP:
         Map<String, DCField> mapFields = ((DCMapField) globalField).getMapFields();
         // TODO WARNING : single map field can't be indexed !!!
         computeFieldsIndices(coll, prefixWithoutDot, mapFields.values(), requiredIndexes);
         break;
      // TODO LATER index subresource as Map !!
      case I18N:
         DCField listI18nField = ((DCI18nField) globalField);
         DCField map = ((DCListField) listI18nField).getListElementField();
         // (skipping list element field since knowing there is a map below)
         Map<String, DCField> mapContent = ((DCMapField) map).getMapFields();
         computeFieldsIndices(coll, prefixWithoutDot, mapContent.values(), requiredIndexes);
         break;
      default:
         if (globalField.getQueryLimit() > 0) {
            requiredIndexes.add(prefixWithoutDot);
         }
         if (globalField.isFulltext()) {
            requiredIndexes.add("_p." + DCField.FULLTEXT_FIELD_NAME + ".v"); // in case not yet ; NB. name guarded against in DCField & mappingService
            // TODO support subresources, OPT compound index on l
         }
         break;
      }
      // TODO LATER embedded resources' _uri, _t...
   }

}
