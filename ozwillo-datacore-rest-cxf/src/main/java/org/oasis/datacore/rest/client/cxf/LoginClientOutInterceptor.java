package org.oasis.datacore.rest.client.cxf;

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.oasis.datacore.rest.client.cxf.CxfMessageHelper;
import org.springframework.security.authentication.TestingAuthenticationToken;
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
public class LoginClientOutInterceptor extends AbstractPhaseInterceptor<Message> {
   private static final Logger LOG = LogUtils.getLogger(LoginClientOutInterceptor.class);

   public LoginClientOutInterceptor() {
      super(Phase.SETUP);
   }

	@Override
   public void handleMessage(Message clientOutRequestMessage) throws Fault {
      Authentication currentUserAuth = SecurityContextHolder.getContext().getAuthentication();
      if (currentUserAuth instanceof TestingAuthenticationToken) {
         TestingAuthenticationToken currentUserTestAuth = (TestingAuthenticationToken) currentUserAuth;
         String basicOrBearerAuthHeader = null;
         if (currentUserTestAuth.getCredentials() instanceof String) {
            String basicOrBearerAuthHeaderFound = (String) currentUserTestAuth.getCredentials();
            if (basicOrBearerAuthHeaderFound.startsWith("Basic ")
                  || basicOrBearerAuthHeaderFound.startsWith("Bearer ")) {
               basicOrBearerAuthHeader = basicOrBearerAuthHeaderFound;
            }
         }
         if (basicOrBearerAuthHeader == null) {
            // works only with server in localauthdevmode 
            CxfMessageHelper.setHeader(clientOutRequestMessage, "testUser", currentUserAuth.getName());
         } else {
            CxfMessageHelper.setHeader(clientOutRequestMessage, "Authorization", basicOrBearerAuthHeader);
         }
         // TODO Basic Auth "Basic " + base64(username:password) ; if (currentUserAuth instanceof UsernamePasswordAuthenticationToken) { // TODO & if name = admin OR compute base64
         ///CxfMessageHelper.setHeader(clientOutRequestMessage, "Authorization", "Basic YWRtaW46YWRtaW4=");
      }
   }

}
