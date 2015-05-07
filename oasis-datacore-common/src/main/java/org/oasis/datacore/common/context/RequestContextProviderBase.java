package org.oasis.datacore.common.context;

import java.util.Map;


public abstract class RequestContextProviderBase implements DCRequestContextProvider {

   @Override
   public Object get(String key) {
      Map<String, Object> requestContext = this.getRequestContext();
      if (requestContext == null) {
         return null; // no context nor enforced case
      }
      return requestContext.get(key);
   }

   @Override
   public void set(String key, Object value) {
      Map<String, Object> requestContext = this.getRequestContext();
      if (requestContext == null) {
         return; // no context nor enforced case
      }
      requestContext.put(key, value);
   }

}
