package org.oasis.datacore.core.request;

import java.util.Map;


/**
 * Provides access to Datacore current request context
 * i.e. CXF Exchange's InMessage if any, else to use it a try / finally wrapper must be used.
 * 
 * @author mdutoo
 *
 */
public interface DCRequestContextProvider {
   
   public Map<String, Object> getRequestContext();
   
   /** shortcut */
   public Object get(String key);
   
}
