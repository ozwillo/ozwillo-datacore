package org.oasis.datacore.rest.client.cxf.obsolete;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.AbstractLoggingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.StringMap;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.oasis.datacore.rest.api.DCResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;

/**
 * TODO NOT USED : cached client impl is required and enough to put data resources in cache
 * (this class might be to put returned object(s) in cache ;
 * BUT can't prevent HTTP 304 not modified response to trigger an exception later without hacks)
 * 
 * NB. not as CXF RequestHandler because can't enrich request (nor maybe work on client side),
 * not as JAXRS 2 RequestFilter because not supported yet (??)
 * 
 * @author mdutoo
 * @obsolete not used
 *
 */

public class ETagClientInInterceptor extends AbstractPhaseInterceptor<Message> {
   private static final Logger LOG = LogUtils.getLogger(ETagClientInInterceptor.class);
   
   // TODO static object & list etag method names...

   @Autowired
   @Qualifier("datacore.rest.client.cache.rest.api.DCResource")
   private Cache resourceCache; // EhCache getNativeCache
   
   public ETagClientInInterceptor() {
      super(Phase.PRE_INVOKE);
   }

   @Override
   public void handleMessage(Message clientInResponseMessage) throws Fault {
      /*Integer httpStatusCode = (Integer) clientInResponseMessage.get("org.apache.cxf.message.Message.RESPONSE_CODE");
      if (httpStatusCode != null && 304 == httpStatusCode) {
         // not modified, can return from cache :
         //Map<Object,Object> requestContext = CxfMessageHelper.getRequestContext(clientInResponseMessage);
         //String uri = (String) requestContext.get("org.apache.cxf.request.uri");
         //Object cached = cache.get(uri);
         clientInResponseMessage.getExchange().put("org.apache.cxf.message.Message.RESPONSE_CODE", 299); // used in AbstractClient.getResponseCode()
         clientInResponseMessage.put("org.apache.cxf.message.Message.RESPONSE_CODE", 299); // not required
      }*//*
      String etag = CxfMessageHelper.getSingleHeader(clientInResponseMessage, HttpHeaders.ETAG);
      if (etag != null) {
         // must cache
         if (List.class.isAssignableFrom(type)) {
            // TODO LATER cache data list-returning methods (multi-GET ; though not find / queries ??)
         } else {
            // case of getData method :
            DCData dcData = (DCData) res;
            cache.put(dcData.getUri(), dcData);
         }
      }*/// NOOOO done from wrapper
      // TODO LATER evict & cache also DCData (LATER & list) returned by POST, PUT, PATCH (?)
      // TODO LATER evict DCData at DELETE (?)
   }
}
