package org.oasis.datacore.core.entity;

import java.util.Set;

import org.oasis.datacore.core.meta.model.DCModelBase;

/**
 * Provides the model of native fields i.e. DCEntity's.
 * Guides their key mapping and query translation, indexing...
 * 
 * @author mdutoo
 *
 */
public interface NativeModelService {

   public static final String NATIVE_MODEL_NAME = "o:Ozwillo_0";
   public static final String DUBLINCORE_MODEL_NAME = "dc:DublinCore_0";
   public static final String NATIVE_ENTITY_MODEL_NAME = "oent:Entity_0";

   /**
    * @param model
    * @return (cached) native model i.e. DCEntity's, might be specific to given model if any ex. for Contributions
    */
   public DCModelBase getNativeModel(DCModelBase model);

   /**
    * i.e. that is or at least participates (History, Contributions) to unique index
    * @param model
    * @return ex. @id
    */
   public String getNativeIdFieldName(DCModelBase model);

   /**
    * @param model
    * @return (cached) native model of non exposed DCEntity fields ex. security, inherits of native model
    */
   public DCModelBase getNonExposedNativeModel(DCModelBase model);

   /**
    * shortcut, used to translate in & out
    * @param model
    * @return (cached) native model field names
    */
   public Set<String> getNativeFieldNames(DCModelBase model);
   
   /**
    * shortcut
    * @param model
    * @return above's (cached) native field names filtered on queryLimit > O
    */
   public Set<String> getNativeExposedOrNotIndexedFieldNames(DCModelBase model);
   
}
