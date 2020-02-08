package org.oasis.datacore.rest.server.cxf;

import java.util.ArrayList;
import java.util.Map;

import org.apache.cxf.message.Exchange;
import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.DCRequestContextProviderFactory;
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
   
   public CxfRequestContextProvider() {
      DCRequestContextProviderFactory.setRequestContextProvider(this);
   }

   @Override
   public Map<String, Object> getRequestContext() {
      Map<String, Object> existingLocalRequestContext = SimpleRequestContextProvider.getSimpleRequestContext();
      
      Exchange exchange = this.getExchange();
      if (exchange != null) { // REST !
         ArrayList<Map<String,Object>> fallbacks = new ArrayList<Map<String,Object>>(2);
         
         if (exchange.getOutMessage() != null) {
            // stack order from top : (exchange then) outMessage, existingLocalRequestContext if exists
            fallbacks.add(exchange.getOutMessage()); // case of client out request being built ((or server out response being built))
            // such as in ContextClientOutInterceptor setting view, project, debug... in REST client
            // so that REST server can use them as was set by REST client app user
            if (existingLocalRequestContext != null // ex. in REST server
                  && !existingLocalRequestContext.isEmpty()) {
               fallbacks.add(existingLocalRequestContext); // case of server in request ((or client in response being built))
            }
            return new MapWithReadonlyFallbacks<String,Object>(exchange, fallbacks);
            
         } else if (exchange.getInMessage() != null) {
            // stack order from top : existingLocalRequestContext if exists, (exchange then) inMessage
            if (existingLocalRequestContext != null // ex. in REST server
                  && !existingLocalRequestContext.isEmpty()) {
               fallbacks.add(exchange);
               fallbacks.add(exchange.getInMessage()); // case of server in request ((or client in response being built))
               return new MapWithReadonlyFallbacks<String,Object>(existingLocalRequestContext, fallbacks);
            } else {
               fallbacks.add(exchange.getInMessage());
               return new MapWithReadonlyFallbacks<String,Object>(exchange, fallbacks);
            }
         }
         
         // at least one of those will be not null, the way CXF works
         throw new RuntimeException("CXF exchange but both in & out messages are null" + exchange.toString());
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
