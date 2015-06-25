package org.oasis.datacore.common.context;

import java.util.HashMap;
import java.util.Map;

/**
 * default impl & helper, threaded
 * @author mdutoo
 *
 */
public class SimpleRequestContextProvider<T> extends RequestContextProviderBase implements DCRequestContextProvider {
   
   /** null means outside context (not set) */
   private static ThreadLocal<Map<String, Object>> requestContext = new ThreadLocal<Map<String, Object>>();

   private static boolean isUnitTest;
   private static boolean isInit;
   private static boolean shouldEnforce;
   private static boolean inited = false;
   
   public SimpleRequestContextProvider() {
      
   }
   
   @Override
   public Map<String, Object> getRequestContext() {
      return getSimpleRequestContext();
   }

   /**
    * 
    * @return null if none (rather than exploding, to allow to check if there is any)
    */
   public static Map<String, Object> getSimpleRequestContext() {
      Map<String, Object> existing = SimpleRequestContextProvider.requestContext.get();
      // NB. don't explode if null to allow to check if there is any ex. in CxfRequestContextProvider
      /*if (existing == null) {
         if (shouldEnforce()) {
            throw new RuntimeException("There is no context");  
         }
      }*/
      return existing;
   }
   
   /**
    * 
    * @param newContext null allowed (for reset, when going out)
    * @return existing
    */
   public static Map<String, Object> setSimpleRequestContext(Map<String, Object> requestContext) {
      Map<String, Object> existingContext = SimpleRequestContextProvider.requestContext.get();
      if (requestContext != null) {
         requestContext = new HashMap<String, Object>(requestContext); // to avoid ImmutableMap
      } else {
         // shortcut for empty
         requestContext = (existingContext != null) ? existingContext : new HashMap<String, Object>(3);
      }
      SimpleRequestContextProvider.requestContext.set(requestContext);
      return existingContext;
   }
   
   /**
    * Adds it to the existing context
    * @param newContext null allowed (for reset, when going out)
    * @return existing
    * @throws RuntimeException if null context (unstack is rather set(null))
    */
   public static Map<String, Object> stackSimpleRequestContext(Map<String, Object> requestContext) {
      if (requestContext == null) {
         throw new RuntimeException("Can't stack null context (unstack is rather set(null))");
      }
      Map<String, Object> existingContext = SimpleRequestContextProvider.requestContext.get();
      HashMap<String, Object> newContext;
      if (existingContext != null) {
         newContext = new HashMap<String, Object>(existingContext); // copy, to keep existingContext unchanged for later
         newContext.putAll(requestContext); // override
      } else {
         newContext = new HashMap<String, Object>(requestContext); // to avoid ImmutableMap
      }
      SimpleRequestContextProvider.requestContext.set(newContext);
      return existingContext;
   }

   /**
    * @obsolete
    * @return
    */
   public static boolean isUnitTest() {
      if (inited) {
         return isUnitTest;
      }
      init();
      return isUnitTest;
   }

   /**
    * @obsolete
    * @return
    */
   public static boolean isInit() {
      if (inited) {
         return isInit;
      }
      init();
      return isInit;
   }

   /**
    * @obsolete
    * @return
    */
   public static boolean shouldEnforce() {
      if (inited) {
         return shouldEnforce;
      }
      init();
      return shouldEnforce;
   }

   /**
    * @obsolete
    */
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

   /**
    * sets the given context, or adds ("stacks") it to existing context if any
    * @param requestContext null means empty ; may be immutable, will be recreated anyway
    * @throws RuntimeException thrown, or wrapping what's thrown, by executeInternal()
    */
   public T execInContext(Map<String, Object> requestContext) {
      Map<String, Object> existingContext = SimpleRequestContextProvider.requestContext.get();
      /*if (existingContext != null) {
         throw new RuntimeException("There already is a context, clear it first");
      }*/
      try {
         if (requestContext != null) {
            stackSimpleRequestContext(requestContext);
         } else {
            setSimpleRequestContext(requestContext);
         }
         
         try {
            return this.executeInternal();
         } catch (RuntimeException e) {
            throw e;
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
         
      } finally {
         // reset null or preexisting context if any :
         SimpleRequestContextProvider.requestContext.set(existingContext);
      }
   } 
   
   /**
    * to be overriden
    * @throws Exception actually ex. ResourceException
    */
   protected T executeInternal() throws Exception {
      return null;
   }

}
