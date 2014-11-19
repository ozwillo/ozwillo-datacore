package org.oasis.datacore.server.rest;

import java.util.Map;

import org.apache.cxf.message.Exchange;
import org.oasis.datacore.core.request.DCRequestContextProvider;
import org.oasis.datacore.core.request.SimpleRequestContextProvider;
import org.oasis.datacore.rest.server.cxf.CxfJaxrsApiProvider;
import org.springframework.stereotype.Component;


/**
 * impl'd on CXF, if no Exchange & for unit tests defaults to SimpleRequestContextProvider
 * @author mdutoo
 *
 */
@Component("datacore.cxfJaxrsApiProvider")
public class DatacoreCxfJaxrsApiProvider extends CxfJaxrsApiProvider implements DCRequestContextProvider {

   private boolean isUnitTest = true;
   
   public Map<String, Object> getRequestContext() {
      Exchange exchange = this.getExchange();
      if (exchange != null) {
         return exchange.getInMessage();
      }
      if (!isUnitTest) {
         return null;
      }
      // helper for unit tests :
      Map<String, Object> requestContext = SimpleRequestContextProvider.getSimpleRequestContext();
      if (requestContext != null) {
         return requestContext;
      }
      throw new RuntimeException("SimpleRequestContextProvider should have been set up in unit test");
   }

   @Override
   public Object get(String key) {
      return this.getRequestContext().get(key);
   }
   
}
