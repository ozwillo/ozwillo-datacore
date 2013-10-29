/*
 * Copyright 2011-2013 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oasis.datacore.sdk.data.spring;

import org.oasis.datacore.data.DCEntity;
import org.oasis.datacore.data.meta.DCField;
import org.springframework.core.convert.ConversionException;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;

import com.mongodb.DBObject;

/**
 * Overriden for null prop save hack
 */
public class DatacoreMappingMongoConverter extends MappingMongoConverter {

   public DatacoreMappingMongoConverter(MongoDbFactory mongoDbFactory,
         MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext) {
      super(mongoDbFactory, mappingContext);
   }

   // Overriden for null prop save hack
   protected void writeInternal(Object obj, final DBObject dbo, MongoPersistentEntity<?> entity) {

      if (obj == null) {
         return;
      }

      if (null == entity) {
         throw new MappingException("No mapping metadata found for entity of type " + obj.getClass().getName());
      }

      final BeanWrapper<MongoPersistentEntity<Object>, Object> wrapper = BeanWrapper.create(obj, conversionService);
      final MongoPersistentProperty idProperty = entity.getIdProperty();

      if (!dbo.containsField("_id") && null != idProperty) {

         boolean fieldAccessOnly = idProperty.usePropertyAccess() ? false : useFieldAccessOnly;

         try {
            Object id = wrapper.getProperty(idProperty, Object.class, fieldAccessOnly);
            dbo.put("_id", idMapper.convertId(id));
         } catch (ConversionException ignored) {
         }
      }

      // Write the properties
      entity.doWithProperties(new PropertyHandler<MongoPersistentProperty>() {
         public void doWithPersistentProperty(MongoPersistentProperty prop) {

            if (prop.equals(idProperty)) {
               return;
            }

            boolean fieldAccessOnly = prop.usePropertyAccess() ? false : useFieldAccessOnly;

            Object propertyObj = wrapper.getProperty(prop, prop.getType(), fieldAccessOnly);

            //if (null != propertyObj) { // [OASIS] HACK to allow unset / set to null at save
               if (/*[OASIS] HACK*/null != propertyObj && /*[OASIS] END*/!conversions.isSimpleType(propertyObj.getClass())) {
                  writePropertyInternal(propertyObj, dbo, prop);
               } else {
                  writeSimpleInternal(propertyObj, dbo, prop.getFieldName());
               }
            //} // [OASIS] HACK
         }
      });

      entity.doWithAssociations(new AssociationHandler<MongoPersistentProperty>() {
         public void doWithAssociation(Association<MongoPersistentProperty> association) {
            MongoPersistentProperty inverseProp = association.getInverse();
            Class<?> type = inverseProp.getType();
            Object propertyObj = wrapper.getProperty(inverseProp, type, useFieldAccessOnly);
            //if (null != propertyObj) { // [OASIS] HACK to allow unset / set to null at save
               writePropertyInternal(propertyObj, dbo, inverseProp);
            //} // [OASIS] HACK
         }
      });
      
      // [OASIS] HACK to persist Datacore model fields at root level NOT FOR NOW
      /*if ("DCEntity".equals(entity.getName())) {
         DCEntity dcEntity = (DCEntity) object;
         DCModel dcModel = dcModelService.getModel(dcEntity.getType());
         for (DCField dcField : dcModel.getAllFields()) {
            
         }
      }*/
   }

   // overriden for visibility
   protected void writeSimpleInternal(Object value, DBObject dbObject, String key) {
      dbObject.put(key, getPotentiallyConvertedSimpleWrite(value));
   }
   // overriden for visibility
   protected Object getPotentiallyConvertedSimpleWrite(Object value) {

      if (value == null) {
         return null;
      }

      Class<?> customTarget = conversions.getCustomWriteTarget(value.getClass(), null);

      if (customTarget != null) {
         return conversionService.convert(value, customTarget);
      } else {
         return Enum.class.isAssignableFrom(value.getClass()) ? ((Enum<?>) value).name() : value;
      }
   }

}
