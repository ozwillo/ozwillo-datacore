package org.oasis.datacore.kernel.client;

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
      Map<String, Object> context = new HashMap<String, Object>();

      if(getReqData && isInServerContext()) {
         try {
            context.put("reqHeaders", jaxrsApiProvider.getHttpHeaders());
            context.put("uri", jaxrsApiProvider.getRequestUri());
            context.put("query", jaxrsApiProvider.getQueryParameters());
         } catch(Exception e) {

         }
      }

      //Log Resource sent in request if any
      if(logReqContent) {
         MessageContentsList objs = MessageContentsList.getContentsList(serverInRequestMessage);
         if(objs != null && objs.size() != 0) {
            Object resource = objs.get(0);
            if(resource instanceof ArrayList) {
               DCResource dcRes = (DCResource) ((ArrayList) resource).get(0);
               context.put("req.model", dcRes.getModelType());
            }
         }
      }

      context.put("method", serverInRequestMessage.get(Message.HTTP_REQUEST_METHOD));
      ex.put("reqContext", context);
   }

   public boolean isInServerContext() {
      return PhaseInterceptorChain.getCurrentMessage() != null ? true : false;
   }
}
