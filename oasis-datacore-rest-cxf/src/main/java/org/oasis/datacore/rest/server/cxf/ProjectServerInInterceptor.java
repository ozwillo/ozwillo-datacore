package org.oasis.datacore.rest.server.cxf;

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.DCRequestContextProviderFactory;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.client.cxf.CxfMessageHelper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sets DCRequestContext datacore.project from HTTP X-Datacore-Project header.
 * 
 * @author mdutoo
 *
 */

public class ProjectServerInInterceptor extends AbstractPhaseInterceptor<Message> {
   private static final Logger LOG = LogUtils.getLogger(ProjectServerInInterceptor.class);

   /** not accessed statically to get CXF-based impl */
   @Autowired
   private DCRequestContextProviderFactory requestContextProviderFactory;
   
   public ProjectServerInInterceptor() {
      super(Phase.PRE_UNMARSHAL);
   }

   @Override
   public void handleMessage(Message serverInRequestMessage) throws Fault {
      String headerProject = CxfMessageHelper.getHeaderString(serverInRequestMessage, DatacoreApi.PROJECT_HEADER);
      if (headerProject != null) {
         requestContextProviderFactory.set(DCRequestContextProvider.PROJECT, headerProject);
      }
   }

}
