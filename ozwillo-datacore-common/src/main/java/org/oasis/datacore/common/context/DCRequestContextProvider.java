package org.oasis.datacore.common.context;

import java.util.Map;


/**
 * Provides access to Datacore current request context
 * i.e. CXF Exchange's InMessage if any, else to use it a try / finally wrapper must be used.
 * 
 * @author mdutoo
 *
 */
public interface DCRequestContextProvider {
   
   String PROJECT = "X-Datacore-Project"; // must be the same as in DatacoreApi
   
   /**
    * Caller must handle null in case no context set, to allow to check whether there is one.
    * @return null if no context set (if enforced rather explodes)
    */
   Map<String, Object> getRequestContext();
   
   /**
    * (shortcut) If no context, returns null (if enforced rather explodes).
    * Caller must handle null in case no context set (and not enforced).
    * @return null if no context, else mapped value
    */
   Object get(String key);
   
   /**
    * (shortcut) If no context, does nothing (if enforced rather explodes)
    */
   void set(String key, Object value);
   
}
