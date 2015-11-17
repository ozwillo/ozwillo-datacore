package org.oasis.datacore.rest.client.cxf;

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.oasis.datacore.common.context.DCRequestContextProviderFactory;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Used to set X-Datacore-Project and all other Datacore HTTP headers
 * to current DCRequestContextProvider datacore.project.
 * NB. not as CXF RequestHandler because can't enrich request (nor maybe work on client side),
 * not as JAXRS 2 RequestFilter because not supported yet (??)
 *
 * @author mdutoo
 *
 */
public class ContextClientOutInterceptor extends AbstractPhaseInterceptor<Message> {
   private static final Logger LOG = LogUtils.getLogger(ContextClientOutInterceptor.class);
   
   /** not accessed statically to get CXF-based impl */
   @Autowired
   private DCRequestContextProviderFactory requestContextProviderFactory;

   public ContextClientOutInterceptor() {
      super(Phase.SETUP);
   }

	@Override
   public void handleMessage(Message clientOutRequestMessage) throws Fault {
	   for (String contextHeader : DatacoreApi.CONTEXT_HEADERS) {
	      String contextValue = (String) requestContextProviderFactory.get(contextHeader);
	      if (contextValue != null) { // including ""
	         CxfMessageHelper.setHeader(clientOutRequestMessage, contextHeader, contextValue);
	      }
	   }
   }

}
