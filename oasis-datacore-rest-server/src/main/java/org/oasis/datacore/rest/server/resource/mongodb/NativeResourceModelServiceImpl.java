package org.oasis.datacore.rest.server.resource.mongodb;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.oasis.datacore.core.entity.NativeModelService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.rest.api.DCResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver.IndexDefinitionHolder;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;


/**
 * Provides a native (DCEntity) model that is mapped to DCResource
 * and in sync with DCEntity annotations (@Indexed...).
 * 
 * @author mdutoo
 *
 */
@Component
public class NativeResourceModelServiceImpl implements NativeModelService {

   public static final String NATIVE_MODEL_NAME = "dc:Datacore";
   
   @Autowired
   private MongoMappingContext mappingContext;
   /** to know which default entity fields are indexed ; alas not available for autowiring */
   protected MongoPersistentEntityIndexResolver indexResolver;

   private DCModelBase nativeResourceModel;
   private Set<String> nativeFieldNames;
   private Set<String> nativeIndexedFieldNames;

   /** not in nativeResourceModel ex. security */
   private Set<String> nonExposedFieldNames;
   

   @Override
   public DCModelBase getNativeModel(DCModelBase model) {
      return nativeResourceModel;
   }

   @Override
   public Set<String> getNativeFieldNames(DCModelBase model) {
      return nativeFieldNames;
   }

   @Override
   public Set<String> getNativeIndexedFieldNames(DCModelBase model) {
      return nativeIndexedFieldNames;
   }
   
   @PostConstruct
   private void init() throws NativeModelException {
      indexResolver = new MongoPersistentEntityIndexResolver(mappingContext); // alas not available for autowiring
      
      nativeResourceModel = buildNativeModel();
      nativeFieldNames = nativeResourceModel.getFieldMap().keySet();
      nativeIndexedFieldNames = nativeResourceModel.getFieldMap().values().stream()
            .filter(f -> f.isQueriable()) // i.e. queryLimit > 0
            .map(f -> f.getName()).collect(Collectors.toSet());
      
      nonExposedFieldNames = new ImmutableSet.Builder<String>()
            .add(DCEntity.KEY_AR)
            .add(DCEntity.KEY_R)
            .add(DCEntity.KEY_W)
            .add(DCEntity.KEY_O)
            .build();
      
      checkIndexedFields();

   }

   private DCModelBase buildNativeModel() {
      return new DCMixin(NATIVE_MODEL_NAME)
      // NB. see details on mongo storing conf in DCEntity.java
      // TODO rather using Enum, see BSON$RegexFlag
            .addField(new DCField(DCResource.KEY_URI, "string", true, 100000, DCEntity.KEY_URI))
            .addField(new DCField(DCResource.KEY_VERSION, "long", false, 0, DCEntity.KEY_V)) // NOT indexed, not required at creation
            .addField(new DCField(DCResource.KEY_TYPES, "string", true, 100000, DCEntity.KEY_T))
      // (at top level) computed ones :
            .addField(new DCField(DCResource.KEY_DCCREATED, "date", false, 100000, DCEntity.KEY_CR_AT)) // DCEntity.KEY_ID // retrieved from ObjectId, index useful for getting first ones ex. when loading models...
            .addField(new DCField(DCResource.KEY_DCMODIFIED, "date", false, 100000, DCEntity.KEY_CH_AT)) // index useful for getting latest changes
            .addField(new DCField(DCResource.KEY_DCCREATOR, "string", false, 0, DCEntity.KEY_CR_BY)) // index LATER ?
            .addField(new DCField(DCResource.KEY_DCCONTRIBUTOR, "string", false, 0, DCEntity.KEY_CH_BY)); // index LATER ?
            //dcEntityIndexedFields.put("o:allReaders", new DCListField(DCEntity.KEY_AR... // don't allow to look it up
   }

   private void checkIndexedFields() throws NativeModelException {
      List<IndexDefinitionHolder> indexDefHolders = indexResolver.resolveIndexForClass(DCEntity.class);
      if (indexDefHolders.size() != nativeIndexedFieldNames.size()) {
         throw new NativeModelException("Native DCModel is not up to date with DCEntity annotations : "
               + "queriable fields " + nativeIndexedFieldNames + " differ from index annotations "
               + indexDefHolders.stream().map(idh -> idh.getPath()).collect(Collectors.toList()));
      }
      indexDefHolders : for (IndexDefinitionHolder indexDefHolder : indexDefHolders) {
         if (nonExposedFieldNames.contains(indexDefHolder.getPath())) {
            continue;
         }
         for (DCField nativeIndexedField : nativeResourceModel.getFieldMap().values()) {
            if (indexDefHolder.getPath().equals(nativeIndexedField.getStorageName())) {
               if (nativeIndexedField.getQueryLimit() <= 0) {
                  throw new NativeModelException("Native DCModel is not up to date with DCEntity annotations : "
                        + "Native model field " + nativeIndexedField.getName()
                        + " is @Indexed but not queriable");
               }
               continue indexDefHolders;
            }
         }
         throw new NativeModelException("Native DCModel is not up to date with DCEntity annotations : "
               + "can't find DCEntity index " + indexDefHolder.getPath()
               + " among native model fields " + nativeResourceModel.getFieldMap().keySet());
      }
   }
   
}
