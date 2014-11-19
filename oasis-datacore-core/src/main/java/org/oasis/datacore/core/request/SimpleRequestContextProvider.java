package org.oasis.datacore.core.request;

import java.util.HashMap;
import java.util.Map;

/**
 * default impl & helper, threaded
 * @author mdutoo
 *
 */
public class SimpleRequestContextProvider implements DCRequestContextProvider {
   
   /** null means outside context (not set) */
   private static ThreadLocal<Map<String, Object>> requestContext = null;

   @Override
   public Map<String, Object> getRequestContext() {
      return getSimpleRequestContext();
   }

   public static Map<String, Object> getSimpleRequestContext() {
      return SimpleRequestContextProvider.requestContext.get();
   }

   public void execInContext(Map<String, Object> requestContext) {
      if (SimpleRequestContextProvider.requestContext != null) {
         throw new RuntimeException("There already is a context, clear it first");
      }
      try {
         if (requestContext == null) {
            requestContext = new HashMap<String, Object>();
         }
         SimpleRequestContextProvider.requestContext.set(requestContext);
         
         this.executeInternal();
         
      } finally {
         SimpleRequestContextProvider.requestContext.set(null);
      }
   } 
   
   /**
    * to be overriden
    */
   protected void executeInternal() {}

   @Override
   public Object get(String key) {
      return getRequestContext().get(key);
   }

}
