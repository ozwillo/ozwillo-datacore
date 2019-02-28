package org.oasis.datacore.rest.server.resource.mongodb;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;

import org.oasis.datacore.core.entity.NativeModelService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.rest.api.DCResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver.IndexDefinitionHolder;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.stereotype.Component;


/**
 * Provides a native (DCEntity) model that is mapped to DCResource
 * and in sync with DCEntity annotations (@Indexed...).
 * 
 * NB. entity fulltext tokens field is not there because it only exists if fulltext is conf'd,
 * so TODO LATER refactor native model to support dynamic mixins and move it there,
 * then TODO LATER2 when transformation module is there, make entity just another
 * resource that is transformed from and to the actual resource. 
 * 
 * @author mdutoo
 *
 */
@Component
public class NativeResourceModelServiceImpl implements NativeModelService {
   
   @Autowired
   private MongoMappingContext mappingContext;
   /** to know which default entity fields are indexed ; alas not available for autowiring */
   protected MongoPersistentEntityIndexResolver indexResolver;

   private DCModelBase nativeResourceModel;
   /** not in nativeResourceModel ex. security, inherits from visible nativeResourceModel */
   private DCModelBase nonExposedNativeModel;
   private Set<String> nativeFieldNames;
   private Set<String> nativeExposedOrNotIndexedFieldNames;
   

   @Override
   public DCModelBase getNativeModel(DCModelBase model) {
      return nativeResourceModel;
   }

   @Override
   public String getNativeIdFieldName(DCModelBase model) {
      return DCResource.KEY_URI;
   }

   @Override
   public DCModelBase getNonExposedNativeModel(DCModelBase model) {
      return nonExposedNativeModel;
   }

   @Override
   public Set<String> getNativeFieldNames(DCModelBase model) {
      return nativeFieldNames;
   }

   @Override
   public Set<String> getNativeExposedOrNotIndexedFieldNames(DCModelBase model) {
      return nativeExposedOrNotIndexedFieldNames;
   }
   
   @PostConstruct
   private void init() throws NativeModelException {
      indexResolver = new MongoPersistentEntityIndexResolver(mappingContext); // alas not available for autowiring
      
      nativeResourceModel = buildNativeResourceModel();
      nonExposedNativeModel = buildNativeNonExposedModel(nativeResourceModel);
      
      nativeFieldNames = nativeResourceModel.getFieldMap().keySet();
      nativeExposedOrNotIndexedFieldNames = nonExposedNativeModel.getGlobalFieldMap().values().stream()
            .filter(f -> f.isIndexed()) // i.e. queryLimit > 0 ; NB. for list fields, its element's
            .map(f -> f.getName()).collect(Collectors.toSet());
      
      checkIndexedFields();

   }

   private DCModelBase buildNativeResourceModel() {
      DCModelBase dublinCoreModel = new DCMixin(DUBLINCORE_MODEL_NAME)
      // (at top level) computed ones :
            .addField(new DCField(DCResource.KEY_DCCREATED, "date", false, 100000, DCEntity.KEY_ID)) // DCEntity.KEY_ID // retrieved from ObjectId, index useful for getting first ones ex. when loading models...
            .addField(new DCField(DCResource.KEY_DCMODIFIED, "date", false, 100000, DCEntity.KEY_CH_AT)) // index useful for getting latest changes
            .addField(new DCField(DCResource.KEY_DCCREATOR, "string", false, 0, DCEntity.KEY_CR_BY)) // index LATER ?
            .addField(new DCField(DCResource.KEY_DCCONTRIBUTOR, "string", false, 0, DCEntity.KEY_CH_BY)); // index LATER ?
      return new DCMixin(NATIVE_MODEL_NAME)
            .addMixin(dublinCoreModel)
      // NB. see details on mongo storing conf in DCEntity.java
      // TODO rather using Enum, see BSON$RegexFlag
            .addField(new DCField(DCResource.KEY_URI, "string", true, 100000, DCEntity.KEY_URI))
            .addField(new DCField(DCResource.KEY_VERSION, "long", false, 0, DCEntity.KEY_V)) // NOT indexed, not required at creation
            .addField(new DCField(DCResource.KEY_TYPES, "string", true, 100000, DCEntity.KEY_T)); // index LATER ?
   }

   private DCModelBase buildNativeNonExposedModel(DCModelBase nativeResourceModel) {
      return new DCMixin(NATIVE_ENTITY_MODEL_NAME)
      // NB. see details on mongo storing conf in DCEntity.java
      // TODO rather using Enum, see BSON$RegexFlag
            .addMixin(nativeResourceModel) // still has visible fields
            .addField(new DCField(DCEntity.KEY_B, "string", true, 100000)) // if multiProjectStorage compound index with _uri
            .addField(new DCListField(DCEntity.KEY_AR, new DCField("useless", "string", true, 100000))) // index
            .addField(new DCListField(DCEntity.KEY_R, new DCField("useless", "string", true, 0)))
            .addField(new DCListField(DCEntity.KEY_W, new DCField("useless", "string", true, 0)))
            .addField(new DCListField(DCEntity.KEY_O, new DCField("useless", "string", true, 0)))
            .addField(new DCField(DCEntity.KEY_ALIAS_OF, "string", null, 10000));

   }

   private void checkIndexedFields() throws NativeModelException {
      TypeInformation<? extends Object> type = ClassTypeInformation.from(DCEntity.class);

      // get the map's key type
      Iterable<? extends MongoPersistentEntityIndexResolver.IndexDefinitionHolder> indexDefHolders = indexResolver.resolveIndexFor(type);
      Set<String> indexedPathes = StreamSupport.stream(indexDefHolders.spliterator(), false)
            .map(idf -> idf.getPath()).collect(Collectors.toSet());
      
      // native id field :
      String idStorageReadName = nonExposedNativeModel.getGlobalFieldMap()
            .get(this.getNativeIdFieldName(null)).getStorageReadName();
      if (idStorageReadName == null) {
         throw new NativeModelException("Native id field should be storable");
      }
      indexedPathes.add(idStorageReadName); // indexed on its own
      
      Set<String> unusedIndexedPathes = new HashSet<String>(indexedPathes);
      
      for (DCField nativeIndexedField : nonExposedNativeModel.getGlobalFieldMap().values()) {
         String storageReadName = nativeIndexedField.getStorageReadName();
         if (storageReadName != null // else not stored i.e. soft computed therefore unqueriable
               && indexedPathes.contains(storageReadName)) {
            if (!nativeIndexedField.isIndexed()) { // NB. for list fields, its element's
               throw new NativeModelException("Native DCModel is not up to date with DCEntity annotations : "
                     + "Native model field " + nativeIndexedField.getName()
                     + " is @Indexed in DCEntity (with storage name " + storageReadName
                     + ") but its DCField is not indexed !");
            }
            unusedIndexedPathes.remove(storageReadName);
         }
      }

      if (!unusedIndexedPathes.isEmpty()) {
         throw new NativeModelException("Native DCModel is not up to date with DCEntity annotations : "
               + "some @Indexed DCEntity fields are not used by any DCField : " + unusedIndexedPathes
               + " (native id field " + this.getNativeIdFieldName(null)
               + " ; DCFields : " + nativeExposedOrNotIndexedFieldNames + ", @Indexed : "
               + StreamSupport.stream(indexDefHolders.spliterator(), false).map(idh -> idh.getPath()).collect(Collectors.toList()));
      }
   }
   
}
