package org.oasis.datacore.rest.server.resource.mongodb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.oasis.datacore.historization.exception.HistorizationException;
import org.oasis.datacore.historization.service.impl.HistorizationServiceImpl;
import org.oasis.datacore.rest.api.DCResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * Creates indexes
 * @author mdutoo
 *
 */
@Component
public class DatabaseSetupServiceImpl implements DatabaseSetupService {

   protected final Logger logger = LoggerFactory.getLogger(getClass());

   /** impl, to be able to modify it
    * TODO LATER extract interface */
   @Autowired
   protected DataModelServiceImpl modelAdminService;
   @Autowired
   protected NativeModelService nativeModelService;
   @Autowired
   protected MongoOperations mgo;

   @Autowired
   private HistorizationServiceImpl historizationService;
   
   
   @Override
   public void cleanModel(DCModelBase model) {
      ///DCProject project = modelAdminService.getProject(model.getProjectName());
      ///DCModelBase storageModel = project.getStorageModel(modelType);
      if (!model.isStorage()) {
         return;
      }
      mgo.dropCollection(model.getCollectionName()); // delete data // storageModel.getAbsoluteName()
   
      // TODO LATER make historizable & contributable more than storage models !
      if (model.isHistorizable()) {
         try {
            String historizationCollectionName = historizationService.getHistorizedCollectionNameFromOriginalModel(model);
            //mgo.remove(new Query(), historizationCollectionName);
            mgo.dropCollection(historizationCollectionName);
         } catch (HistorizationException e) {
            logger.error("error while dropping (historization of) model "
                  + model.getName(), e);
         }
      }
      
      if (model.isContributable()) {
         String contributionCollectionName = model.getName() + ".c"; // TODO TODOOOOOO move
         //mgo.remove(new Query(), historizationCollectionName);
         mgo.dropCollection(contributionCollectionName);
      }
   }
   
   /**
    * 
    * @param storageModel
    * @throws RuntimeException if not storage
    */
   @Override
   public void cleanDataOfCreatedModel(DCModelBase storageModel) throws RuntimeException {
      if (!storageModel.isStorage()) {
         throw new RuntimeException("Can't clean data of non storage model "
               + storageModel.getAbsoluteName());
      }
      
      // delete (rather than drop & recreate !) : 
      mgo.remove(new Query(), storageModel.getCollectionName());

      // TODO LATER make historizable & contributable more than storage models !
      if (storageModel.isHistorizable()) {
         try {
            DCModelBase historizedModel = historizationService.getHistorizationModel(storageModel);
            if (historizedModel == null) {
               historizedModel = historizationService.createHistorizationModel(storageModel); // TODO ??????
            }
            mgo.remove(new Query(), historizedModel.getCollectionName());
         } catch (HistorizationException e) {
            throw new RuntimeException("Historization init error of Model " + storageModel.getName(), e);
         }
      }
      
      if (storageModel.isContributable()) {
         String contributionCollectionName = storageModel.getName() + ".c"; // TODO TODOOOOOO move
         mgo.remove(new Query(), contributionCollectionName);
      }
   }
   
   
   /**
    * @param model
    * @param deleteCollectionsFirst
    * @return
    */
   public boolean ensureCollectionAndIndices(DCModelBase model, boolean deleteCollectionsFirst) {
      if (deleteCollectionsFirst) {
         // cleaning data first
         mgo.dropCollection(model.getCollectionName());
      }
      boolean collectionAlreadyExists = ensureCollectionAndIndices(model);
      
      if(model.isHistorizable()) {
         collectionAlreadyExists = ensureHistorizedCollectionAndIndices(model, deleteCollectionsFirst)
               || collectionAlreadyExists;
      }
      
      if (model.isContributable()) {
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
         if (deleteCollectionsFirst) {
            // cleaning data first
            mgo.dropCollection(historizedModel.getCollectionName());
         }
         
         boolean res = ensureGenericCollectionAndIndices(historizedModel);
         // compound index on uri & version :
         mgo.getCollection(model.getCollectionName()).createIndex(
               new BasicDBObject(DCEntity.KEY_URI, 1).append(DCEntity.KEY_V, 1),
               new BasicDBObject("unique", true));
         // NB. does nothing if already exists http://docs.mongodb.org/manual/tutorial/create-an-index/
         return res;
      } catch (HistorizationException e) {
         throw new RuntimeException("Historization init error of Model " + model.getName(), e);
      }
   }

   public boolean ensureContributedCollectionAndIndices(DCModelBase model, boolean deleteCollectionsFirst) {
      //contributionModel = contributionService.createContributionModel(model); // TODO TODOOO
      if (deleteCollectionsFirst) {
         // cleaning data first
         String contributionCollectionName = model.getName() + ".c"; // TODO TODOOOOOO move
         mgo.dropCollection(contributionCollectionName);
      }
      // TODO TODOOOOO compound index on uri and contributor / organization ?!
      return false; // ensureCollectionAndIndices(historizedModel); // TODO TODOOOO
   }

   private boolean ensureCollectionAndIndices(DCModelBase model) {
      boolean res = ensureGenericCollectionAndIndices(model);
      mgo.getCollection(model.getCollectionName()).createIndex(
            new BasicDBObject(DCEntity.KEY_URI, 1), new BasicDBObject("unique", true)); // TODO dropDups ??
      // NB. does nothing if already exists http://docs.mongodb.org/manual/tutorial/create-an-index/
      return res;
   }
   private boolean ensureGenericCollectionAndIndices(DCModelBase model) {
      DBCollection coll;
      boolean collectionAlreadyExists = mgo.collectionExists(model.getCollectionName()); 
      if (collectionAlreadyExists) {
         coll = mgo.getCollection(model.getCollectionName());
      } else {
         coll = mgo.createCollection(model.getCollectionName());
      }
      
      ArrayList<String> requiredIndexes = new ArrayList<String>();

      // computing static indexes
      DCModelBase nonExposedNativeModel = nativeModelService.getNonExposedNativeModel(model);
      for (String nativeFieldName : nativeModelService.getNativeExposedOrNotIndexedFieldNames(model)) {
         if (!DCResource.KEY_URI.equals(nativeFieldName)) {
            DCField nativeField = nonExposedNativeModel.getGlobalField(nativeFieldName);
            requiredIndexes.add(nativeField.getStorageName()); // for query security
         } // else done outside this method
      }
      
      // computing field indices
      ensureFieldIndices(coll, DCEntity.KEY_P + ".", model.getGlobalFieldMap().values(), requiredIndexes);
      
      // getting existing indexes
      List<DBObject> mongoIndexInfos = coll.getIndexInfo();
      Set<String> nonUniqueSingleIndexedPathes = new HashSet<String>(mongoIndexInfos.size());
      for (DBObject mongoIndexInfo : mongoIndexInfos) {
         Object uniqueFound = mongoIndexInfo.get("unique");
         if (uniqueFound != null && ((Boolean) uniqueFound).booleanValue()) {
            continue;
         }
         Set<String> keyNames = ((DBObject) mongoIndexInfo.get("key")).keySet();
         if (keyNames.size() != 1) {
            continue;
         }
         nonUniqueSingleIndexedPathes.add((String) keyNames.iterator().next());
      }

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
      for (String requiredIndex : requiredIndexes) {
         coll.createIndex(new BasicDBObject(requiredIndex, 1));
         // NB. does nothing if same already exists http://docs.mongodb.org/manual/tutorial/create-an-index/
      }
      
      return collectionAlreadyExists;
   }

   private void ensureFieldIndices(DBCollection coll, String prefix,
         Collection<DCField> globalFields, List<String> requiredIndexes) {
      for (DCField globalField : globalFields) {
         ensureFieldIndices(coll, prefix, globalField, requiredIndexes);
      }
   }

   private void ensureFieldIndices(DBCollection coll, String prefix,
         DCField globalField, List<String> requiredIndexes) {
      String prefixedGlobalFieldStorageName = prefix + globalField.getStorageName();
      switch (DCFieldTypeEnum.getEnumFromStringType(globalField.getType())) {
      case LIST:
         DCField listField = ((DCListField) globalField).getListElementField();
         ensureFieldIndices(coll, prefixedGlobalFieldStorageName + ".", listField, requiredIndexes);
         break;
      case MAP:
         Map<String, DCField> mapFields = ((DCMapField) globalField).getMapFields();
         // TODO WARNING : single map field can't be indexed !!!
         ensureFieldIndices(coll, prefixedGlobalFieldStorageName + ".", mapFields.values(), requiredIndexes);
         break;
      // TODO LATER index subresource as Map !!
      case I18N:
         DCField listI18nField = ((DCI18nField) globalField);
         DCField map = ((DCListField) listI18nField).getListElementField();
         Map<String, DCField> mapContent = ((DCMapField) map).getMapFields();
         ensureFieldIndices(coll, prefixedGlobalFieldStorageName + ".", mapContent.values(), requiredIndexes);
         break;
      default:
         if (globalField.getQueryLimit() > 0) {
            requiredIndexes.add(prefixedGlobalFieldStorageName);
         }
         break;
      }
      // TODO LATER embedded resources
   }

}
