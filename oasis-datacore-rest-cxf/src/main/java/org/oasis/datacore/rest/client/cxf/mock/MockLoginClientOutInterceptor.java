package org.oasis.datacore.rest.client.cxf.mock;

import java.util.logging.Logger;

import javax.ws.rs.core.HttpHeaders;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.oasis.datacore.rest.client.cxf.CxfMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Used to set testUser header to current spring security context authentication name if any.
 * NB. not as CXF RequestHandler because can't enrich request (nor maybe work on client side),
 * not as JAXRS 2 RequestFilter because not supported yet (??)
 *
 * @author mdutoo
 *
 */
public class MockLoginClientOutInterceptor extends AbstractPhaseInterceptor<Message> {
   private static final Logger LOG = LogUtils.getLogger(MockLoginClientOutInterceptor.class);

   public MockLoginClientOutInterceptor() {
      super(Phase.SETUP);
   }

   @Override
   public void handleMessage(Message clientOutRequestMessage) throws Fault {
      Authentication currentUserAuth = SecurityContextHolder.getContext().getAuthentication();
      if (currentUserAuth != null) {
         // TODO Basic Auth "Basic " + base64(username:password)
         CxfMessageHelper.setHeader(clientOutRequestMessage,
               HttpHeaders.AUTHORIZATION, currentUserAuth.getName());
      }
   }

}
