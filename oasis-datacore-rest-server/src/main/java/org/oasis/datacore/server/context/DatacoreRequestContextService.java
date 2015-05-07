package org.oasis.datacore.server.context;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cxf.message.Message;
import org.oasis.datacore.common.context.DCRequestContextProviderFactory;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.client.cxf.CxfMessageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Hides the specifics of dealing with (CXF...) RequestContext to provide
 * Datacore's request-scoped features.
 * TODO OPT refactor our DebugRequestContextService ?
 * @author mdutoo
 *
 */
@Component
public class DatacoreRequestContextService {
   
   public static final String PARAM_DEBUG = "dc.params.debug";
   public static final String QUERY_EXPLAIN = "dc.query.explain";

   @Autowired
   protected DCRequestContextProviderFactory requestContextProviderFactory;
   
   /**
    * 
    * @return false if no context, else request debug header or param
    */
   public boolean isDebug() {
      Map<String, Object> requestContext = requestContextProviderFactory.getRequestContext();
      if (requestContext == null) {
         return false;
      }
      Object debugFound = requestContext.get(PARAM_DEBUG);
      if (debugFound != null) {
         return (boolean) debugFound; // already set
      }
      boolean debug = "true".equalsIgnoreCase(CxfMessageHelper.getHeaderString(requestContext, DatacoreApi.DEBUG_HEADER))
            || "true".equalsIgnoreCase(CxfMessageHelper.getSingleParameterValue(requestContext, DatacoreApi.DEBUG_PARAM));
      requestContext.put(PARAM_DEBUG, debug);
      return debug;
   }

   /**
    * Must only be called if isDebug()
    * @return never null, ex. query explain data
    */
   public Map<String, Object> getDebug() {
      Map<String, Object> requestContext = requestContextProviderFactory.getRequestContext(); // not null because isDebug()
      @SuppressWarnings("unchecked")
      Map<String,Object> explainCtx = (Map<String,Object>) requestContext.get(QUERY_EXPLAIN);
      if (explainCtx == null) {
         explainCtx = new LinkedHashMap<String,Object>(); // preserves order
         requestContext.put(QUERY_EXPLAIN, explainCtx);
      }
      return explainCtx;
   }
   
   public String getAcceptContentType() {
      Map<String, Object> requestContext = requestContextProviderFactory.getRequestContext();
      if (requestContext == null) {
         return null;
      }
      return CxfMessageHelper.getHeaderString(requestContext, Message.ACCEPT_CONTENT_TYPE);
   }
   
}
