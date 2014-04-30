package org.oasis.datacore.kernel.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.model.OperationInfo;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.oasis.datacore.rest.api.util.JaxrsApiProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class ContextInInterceptor extends AbstractPhaseInterceptor<Message> {

   @Autowired
   @Qualifier("datacore.cxfJaxrsApiProvider")
   protected JaxrsApiProvider jaxrsApiProvider;

   public ContextInInterceptor() {
      super(Phase.PRE_INVOKE);
   }

   @Override
   public void handleMessage(Message serverInRequestMessage) throws Fault {
      Exchange ex = serverInRequestMessage.getExchange();
      Map<String, Object> context = new HashMap<String, Object>();

      if(isInServerContext()) {
         try {
            context.put("reqHeaders", jaxrsApiProvider.getHttpHeaders());
            context.put("uri", jaxrsApiProvider.getRequestUri());
            context.put("query", jaxrsApiProvider.getQueryParameters());
         } catch(Exception e) {

         }
      }

      context.put("method", serverInRequestMessage.get(Message.HTTP_REQUEST_METHOD));
      ex.put("reqContext", context);
   }

   public boolean isInServerContext() {
      return PhaseInterceptorChain.getCurrentMessage() != null ? true : false;
   }
}
