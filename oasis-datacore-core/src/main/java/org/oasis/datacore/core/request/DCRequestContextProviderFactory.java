package org.oasis.datacore.core.request;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DCRequestContextProviderFactory {
   
   private static DCRequestContextProviderFactory instance = null;
   
   @Autowired
   private DCRequestContextProvider requestContextProvider = new SimpleRequestContextProvider();
   
   public DCRequestContextProviderFactory() {
      DCRequestContextProviderFactory.instance = this;
   }
   
   public static DCRequestContextProvider getProvider() {
      return DCRequestContextProviderFactory.instance.requestContextProvider;
   }
   
   /** shortcut to getProvider().getRequestContext() */
   public static Map<String, Object> getRequestContext() {
      return DCRequestContextProviderFactory.getProvider().getRequestContext();
   }
   
   /** shortcut */
   public static Object get(String key) {
      return DCRequestContextProviderFactory.getRequestContext().get(key);
   }
   
}
