package org.oasis.datacore.rest.client.cxf;

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.DCRequestContextProviderFactory;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Used to set X-Datacore-Project HTTP header to current DCRequestContextProvider datacore.project
 * NB. not as CXF RequestHandler because can't enrich request (nor maybe work on client side),
 * not as JAXRS 2 RequestFilter because not supported yet (??)
 *
 * @author mdutoo
 *
 */
public class ProjectClientOutInterceptor extends AbstractPhaseInterceptor<Message> {
   private static final Logger LOG = LogUtils.getLogger(ProjectClientOutInterceptor.class);
   
   /** not accessed statically to get CXF-based impl */
   @Autowired
   private DCRequestContextProviderFactory requestContextProviderFactory;

   public ProjectClientOutInterceptor() {
      super(Phase.SETUP);
   }

	@Override
   public void handleMessage(Message clientOutRequestMessage) throws Fault {
      String contextProject = (String) requestContextProviderFactory.get(DCRequestContextProvider.PROJECT);
      if (contextProject != null) {
         CxfMessageHelper.setHeader(clientOutRequestMessage, DatacoreApi.PROJECT_HEADER, contextProject);
      }
   }

}
