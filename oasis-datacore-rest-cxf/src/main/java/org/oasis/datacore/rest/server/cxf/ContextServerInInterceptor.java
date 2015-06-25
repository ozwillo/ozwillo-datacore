package org.oasis.datacore.rest.server.cxf;

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.oasis.datacore.common.context.DCRequestContextProviderFactory;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.client.cxf.CxfMessageHelper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * USELESS since RequestContext wraps CXF context.
 * 
 * Sets DCRequestContext datacore.project from HTTP X-Datacore-Project header
 * and other Datacore headers.
 * 
 * @author mdutoo
 *
 * @obsolete
 */

public class ContextServerInInterceptor extends AbstractPhaseInterceptor<Message> {
   private static final Logger LOG = LogUtils.getLogger(ContextServerInInterceptor.class);

   /** not accessed statically to get CXF-based impl */
   @Autowired
   private DCRequestContextProviderFactory requestContextProviderFactory;
   
   public ContextServerInInterceptor() {
      super(Phase.PRE_UNMARSHAL);
   }

   @Override
   public void handleMessage(Message serverInRequestMessage) throws Fault {
      for (String contextHeader : DatacoreApi.CONTEXT_HEADERS) {
         String headerValue = CxfMessageHelper.getHeaderString(serverInRequestMessage, contextHeader);
         if (headerValue != null) {
            requestContextProviderFactory.set(contextHeader, headerValue);
         }
      }
   }

}
