package org.oasis.datacore.server.context;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.cxf.message.Message;
import org.oasis.datacore.common.context.DCRequestContextProviderFactory;
import org.oasis.datacore.core.context.DatacoreRequestContextService;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.client.cxf.CxfMessageHelper;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Hides the specifics of dealing with (CXF...) RequestContext to provide
 * Datacore's request-scoped features.
 * TODO OPT refactor our DebugRequestContextService ?
 * @author mdutoo
 *
 */
@Component
public class DatacoreRequestContextServiceImpl implements DatacoreRequestContextService {
   
   public static final String PARAM_DEBUG = "dc.params.debug";
   public static final String QUERY_EXPLAIN = "dc.query.explain";

   @Autowired
   protected DCRequestContextProviderFactory requestContextProviderFactory;
   
   /**
    * 
    * @return false if no context, else request debug header or param
    */
   @Override
   public boolean isDebug() {
      Map<String, Object> requestContext = requestContextProviderFactory.getRequestContext();
      if (requestContext == null) {
         return false;
      }
      Object debugFound = requestContext.get(PARAM_DEBUG);
      if (debugFound != null) {
         return (boolean) debugFound; // already set
      }
      boolean debug = CxfMessageHelper.isCxfMessage(requestContext)
            && ("true".equalsIgnoreCase(CxfMessageHelper.getHeaderString(requestContext, DatacoreApi.DEBUG_HEADER))
            || "true".equalsIgnoreCase(CxfMessageHelper.getSingleParameterValue(requestContext, DatacoreApi.DEBUG_PARAM)));
      requestContext.put(PARAM_DEBUG, debug);
      return debug;
   }

   /**
    * Must only be called if isDebug()
    * @return never null, ex. query explain data
    */
   @Override
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

   @Override
   public String getAcceptContentType() {
      Map<String, Object> requestContext = requestContextProviderFactory.getRequestContext();
      if (requestContext == null) {
         return null;
      }
      return CxfMessageHelper.getHeaderString(requestContext, Message.ACCEPT_CONTENT_TYPE);
   }
   
   /**
    * TODO cache
    * @return null means all, empty means minimal (@id, o:version), only dc:DublinCore_0
    * means minimal + @type + dc: fields, others come in addition to it
    */
   @Override
   public LinkedHashSet<String> getViewMixinNames() {
      Map<String, Object> requestContext = requestContextProviderFactory.getRequestContext();
      if (requestContext == null) {
         return null;
      }
      String csvMixinNames = CxfMessageHelper.getHeaderString(requestContext, DatacoreApi.VIEW_HEADER);
      if (csvMixinNames == null) {
         return null;
      }
      LinkedHashSet<String> mixinNameSet = new LinkedHashSet<String>();
      if ((csvMixinNames = csvMixinNames.trim()).isEmpty()) {
         return mixinNameSet; // minimal : @id, o:version
      }
      String[] mixinNames = StringUtils.commaDelimitedListToStringArray(csvMixinNames);
      for (String mixinName : mixinNames) {
         if (!(mixinName = mixinName.trim()).isEmpty()) {
            mixinNameSet.add(mixinName);
         }
      }
      return mixinNameSet;
   }

   @Override
   public boolean getPutRatherThanPatchMode() {
      Object found = requestContextProviderFactory.get(ResourceService.PUT_MODE);
      return found != null && ((Boolean) found).booleanValue();
   }
   
}
