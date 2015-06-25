package org.oasis.datacore.rest.server.cxf;

import java.util.ArrayList;
import java.util.Map;

import org.apache.cxf.message.Exchange;
import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.springframework.stereotype.Component;


/**
 * Impl'd on CXF's Exchange
 * if no Exchange & for unit tests defaults to SimpleRequestContextProvider
 * within DCRequestContextProviderFactory, that must be used to access it.
 * Used also on client side.
 * 
 * @author mdutoo
 *
 */
@Component("datacore.cxfJaxrsApiProvider")
public class CxfRequestContextProvider extends CxfJaxrsApiProvider implements DCRequestContextProvider {
   
   protected DelegateRequestContextProvider delegate = new DelegateRequestContextProvider(this);

   @Override
   public Map<String, Object> getRequestContext() {
      Map<String, Object> existingLocalRequestContext = SimpleRequestContextProvider.getSimpleRequestContext();
      Exchange exchange = this.getExchange();
      if (exchange != null) { // REST !
         ArrayList<Map<String,Object>> fallbacks = new ArrayList<Map<String,Object>>(2);
         if (exchange.getInMessage() != null) {
            fallbacks.add(exchange.getInMessage()); // case of server in request
         } else if (exchange.getOutMessage() != null) {
            fallbacks.add(exchange.getOutMessage()); // case of client out request
         }
         
         // stack on top of existing context if any :
         if (existingLocalRequestContext != null // ex. in REST server
               && !existingLocalRequestContext.isEmpty()) {
            fallbacks.add(existingLocalRequestContext); // ex. in REST client, so that
            // ContextClientOutInterceptor can set view (& project, debug) to use
            // by REST server as was set by REST client app user
         }
         
         // NB. at least one of those will be not null, the way CXF works
         // and only one (unless done in (ClientIn or ServerOut) Response interceptor, which it is not for)
         return new MapWithReadonlyFallbacks<String,Object>(exchange, fallbacks);
      } // else not REST
      
      if (existingLocalRequestContext == null && SimpleRequestContextProvider.shouldEnforce()) {
         throw new RuntimeException("There is no context");
      }
      return SimpleRequestContextProvider.getSimpleRequestContext();
      // NB. don't explode if null, to allow to check whether there is any
      /*
      // helper for unit tests :
      if (requestContext == null) {
         return new HashMap<String, Object>(3); // to avoid NullPointerException outside REST ex. below LoadPersistedModelsAtInit
      }
      return requestContext;
      if (requestContext != null || !isUnitTest()) {
         return requestContext;
      }
      throw new RuntimeException("SimpleRequestContextProvider should have been set up in unit test");*/
   }
   
   @Override
   public Object get(String key) {
      return this.delegate.get(key);
   }
   
   @Override
   public void set(String key, Object value) {
      this.delegate.set(key, value);
   }
   
}
