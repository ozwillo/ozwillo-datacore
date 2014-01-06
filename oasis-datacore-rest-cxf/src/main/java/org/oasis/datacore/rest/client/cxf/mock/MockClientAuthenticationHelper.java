package org.oasis.datacore.rest.client.cxf.mock;

import java.util.ArrayList;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Mock local client auth that allows any user.
 * 
 * @author mdutoo
 *
 */
public class MockClientAuthenticationHelper {
   
   public static void loginAs(String username) {
      UserDetails userDetails = new User(username, "", new ArrayList<GrantedAuthority>(0));
      
      Authentication authentication = new TestingAuthenticationToken(userDetails, "");
      // TODO rather than PreAuthenticatedAuthenticationToken because mock 
      
      authentication.setAuthenticated(true); // else in MethodSecurityInterceptor tries to reauth...
      // TODO but don't do it for guest ??
      
      // TODO guest rather using AnonymousAuthenticationToken ?!
      
      SecurityContext sc = new SecurityContextImpl();
      sc.setAuthentication(authentication);
      SecurityContextHolder.setContext(sc);
   }
   
   public static void logout() {
      SecurityContextHolder.clearContext();
   }
   
}
