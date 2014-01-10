package org.oasis.datacore.rest.server.security.mock;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.ClientException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.oasis.datacore.core.security.mock.MockAuthenticationService;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.client.cxf.CxfMessageHelper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Logs in (without any check) as provided user available in conf'd UserDetailsService.
 * Done as CXF interceptor because looks in HTTP request.
 * 
 * @author mdutoo
 *
 */

public class MockLoginServerInInterceptor extends AbstractPhaseInterceptor<Message> {
   private static final Logger LOG = LogUtils.getLogger(MockLoginServerInInterceptor.class);
   
   @Autowired
   private MockAuthenticationService mockAuthenticationService;
   
   public MockLoginServerInInterceptor() {
      super(Phase.PRE_PROTOCOL);
   }

   @Override
   public void handleMessage(Message serverInRequestMessage) throws Fault {
      String username = null;
      
      Map<String, List<String>> headers = CxfMessageHelper.getRequestHeaders(serverInRequestMessage);
      List<String> authHeader = headers.get(HttpHeaders.AUTHORIZATION);
      // TODO Basic Auth "Basic " + base64(username:password)
      if (authHeader != null && !authHeader.isEmpty()) {
         username = authHeader.get(0);
      }
      
      if (username == null && isGetOperation(serverInRequestMessage)) {
         String requestUri = (String) serverInRequestMessage.get(Message.REQUEST_URI);
         boolean decode = true;
         MultivaluedMap<String, String> params = JAXRSUtils.getStructuredParams(requestUri, "&", decode, decode);
         List<String> usernameList = params.get("username");
         if (usernameList != null && !usernameList.isEmpty()) {
            username = usernameList.get(0);
         }
      }
      
      if (username == null) {
         // log as admin, using the "admin" user :
         username = "admin";
         // TODO in production (& integration), rather log as guest, using the "guest" user !
      }

      try {
    	  if(!username.startsWith("Bearer")) {
    		  mockAuthenticationService.loginAs(username); // NB. username can't be null
    	  }
      } catch (RuntimeException rex) {
         throw new Fault(
               new ClientException( // or any non-Fault exception, else blocks in
               // abstractClient.checkClientException() (waits for missing response code)
               // see http://stackoverflow.com/questions/8316354/cxf-ws-interceptor-stop-processing-respond-with-fault
               "Unknown username " + username),
               Fault.FAULT_CODE_CLIENT);
      }
   }

   public static boolean isGetOperation(Message serverInRequestMessage) {
      String requestHttpMethod = (String) serverInRequestMessage.get(Message.HTTP_REQUEST_METHOD);
      if (HttpMethod.GET.equals(requestHttpMethod)) {
         return true;
      }
      return false;
   }
   
}
