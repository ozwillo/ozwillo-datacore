package org.oasis.datacore.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.JaxrsApiProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

public class ContextInInterceptor extends AbstractPhaseInterceptor<Message> {

   @Autowired
   @Qualifier("datacore.cxfJaxrsApiProvider")
   protected JaxrsApiProvider jaxrsApiProvider;

   @Value("${dtMonitoring.getReqData}")
   private boolean getReqData;

   @Value("${dtMonitoring.logReqContent}")
   private boolean logReqContent;

   public ContextInInterceptor() {
      super(Phase.PRE_INVOKE);
   }

   @Override
   public void handleMessage(Message serverInRequestMessage) throws Fault {     
      Exchange ex = serverInRequestMessage.getExchange();
      ex.put("dc.method", serverInRequestMessage.get(Message.HTTP_REQUEST_METHOD));

      if(getReqData && isInServerContext()) {
         try {
            ex.put("dc.req.headers", jaxrsApiProvider.getHttpHeaders());
            ex.put("dc.uri", jaxrsApiProvider.getRequestUri());
            ex.put("dc.query", jaxrsApiProvider.getQueryParameters());
         } catch(Exception e) {

         }
      }

      //Log Resource model type sent in request if any
      if(logReqContent) {
         @SuppressWarnings("static-access")
         String type = (String) serverInRequestMessage.get(serverInRequestMessage.CONTENT_TYPE);
         if (type == null || !MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.valueOf(type))) {
            return; // avoid introspection of non-JSON content
         }
         
         MessageContentsList objs = MessageContentsList.getContentsList(serverInRequestMessage);
         if(objs != null && objs.size() != 0) {
            Object resource = objs.get(0);
            if(resource instanceof ArrayList<?>) {
               ArrayList<DCResource> dcRes = (ArrayList<DCResource>) resource;
               ex.put("dc.req.model", dcRes.get(0).getModelType());
            }
         }
      }
   }

   public boolean isInServerContext() {
      return PhaseInterceptorChain.getCurrentMessage() != null ? true : false;
   }
}
