package org.oasis.datacore.common.context;

import java.util.HashMap;
import java.util.Map;

/**
 * default impl & helper, threaded
 * @author mdutoo
 *
 */
public class SimpleRequestContextProvider implements DCRequestContextProvider {
   
   /** null means outside context (not set) */
   private static ThreadLocal<Map<String, Object>> requestContext = new ThreadLocal<Map<String, Object>>();

   private static boolean isUnitTest;
   private static boolean isInit;
   private static boolean shouldEnforce;
   private static boolean inited = false;
   
   @Override
   public Map<String, Object> getRequestContext() {
      return getSimpleRequestContext();
   }

   public static Map<String, Object> getSimpleRequestContext() {
      Map<String, Object> existing = SimpleRequestContextProvider.requestContext.get();
      if (existing == null) {
         if (shouldEnforce()) {
            throw new RuntimeException("There is no context");  
         }
         // TODO
      }
      return existing;
   }

   public static boolean isUnitTest() {
      if (inited) {
         return isUnitTest;
      }
      init();
      return isUnitTest;
   }

   public static boolean isInit() {
      if (inited) {
         return isInit;
      }
      init();
      return isInit;
   }

   public static boolean shouldEnforce() {
      if (inited) {
         return shouldEnforce;
      }
      init();
      return shouldEnforce;
   }

   private static synchronized void init() {
      //synchronized(SimpleRequestContextProvider.class) {
      for (StackTraceElement stackElt : Thread.currentThread().getStackTrace()) {
         if (stackElt.getClassName().contains("junit")) { // org.springframework.test.context.junit4.SpringJUnit4ClassRunner
            isUnitTest = true;
         }
         if (stackElt.getClassName().contains("InitService")) { // org.oasis.datacore.core.init.InitService
            isInit = true;
         }
      }
      shouldEnforce = !isUnitTest && !isInit;
      inited = true;
   }

   public void execInContext() {
      if (SimpleRequestContextProvider.requestContext.get() != null) {
         throw new RuntimeException("There already is a context, clear it first");
      }
      try {
         /*if (requestContext == null) {
            requestContext = new HashMap<String, Object>();
         }*/
         SimpleRequestContextProvider.requestContext.set(new HashMap<String, Object>());
         
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
   
   @Override
   public void set(String key, Object value) {
      this.getRequestContext().put(key, value);
   }

}
