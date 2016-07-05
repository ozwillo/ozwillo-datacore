package org.oasis.datacore.core.entity.mongodb;

import java.lang.reflect.Field;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.MongoTypeMapper;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.util.TypeInformation;

import com.mongodb.DBObject;
import com.mongodb.DBRef;


/**
 * Wrapper used by MongoTemplateManager to postpone index creation to once
 * the (Datacore)MongoTemplate has been created with it (else calls
 * MongoPersistentEntityIndexCreator which calls db before it is set)
 * 
 * @author mdutoo
 */
public class NoIndexCreationMongoConverter implements MongoConverter {

   private MongoConverter delegate;
   private boolean inited = false;

   public NoIndexCreationMongoConverter(MongoConverter delegate) {
      this.delegate = delegate;
   }

   public MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> getMappingContext() {
      return inited ? delegate.getMappingContext() : null;
   }
   
   public void completeInit(MongoTemplate mt) {
      try {
         Field mappingContextField = MongoTemplate.class.getDeclaredField("mappingContext");
         mappingContextField.setAccessible(true);
         mappingContextField.set(mt, delegate.getMappingContext());
      } catch (IllegalArgumentException | IllegalAccessException
            | NoSuchFieldException | SecurityException e) {
         // should not happen
         throw new RuntimeException("error", e);
      }
      this.inited = true;
   }
   
   
   
   public <R> R read(Class<R> type, DBObject source) {
      return delegate.read(type, source);
   }

   public void write(Object source, DBObject sink) {
      delegate.write(source, sink);
   }

   public Object convertToMongoType(Object obj) {
      return delegate.convertToMongoType(obj);
   }

   public MongoTypeMapper getTypeMapper() {
      return delegate.getTypeMapper();
   }

   public Object convertToMongoType(Object obj, TypeInformation<?> typeInformation) {
      return delegate.convertToMongoType(obj, typeInformation);
   }

   public ConversionService getConversionService() {
      return delegate.getConversionService();
   }

   public DBRef toDBRef(Object object, MongoPersistentProperty referingProperty) {
      return delegate.toDBRef(object, referingProperty);
   }
   
}
