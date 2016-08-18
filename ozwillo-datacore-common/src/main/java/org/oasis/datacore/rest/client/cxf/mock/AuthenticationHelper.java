package org.oasis.datacore.rest.client.cxf.mock;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;

/**
 * Auth helper ; can be used as is for client or tests (in which case mock local client auth that allows any user).
 * Otherwise, must be called with the rightly built User(Details).
 * TODO further merge with LocalAuthenticationService ?!
 * 
 * @author mdutoo
 *
 */
public class AuthenticationHelper {
   
   /**
    * Unit test only CLIENT SIDE ONLY TODO. Requires org.oasis.datacore.rest.server.security.mock.MockLoginServerInInterceptor
    * to be configured among server interceptors, which only ever happens in unit test setup.
    * @param username
    */
   public static void loginAs(String username) {
      User userDetails = new User(username, "", new ArrayList<GrantedAuthority>(0));
      loginAs(userDetails);
   }

   /**
    * Also works on server-side for system logins or in tests
    * @param userDetails
    */
   public static void loginAs(User userDetails) {
      List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>(userDetails.getAuthorities());
      Authentication authentication = new TestingAuthenticationToken(userDetails, "",
            authorities);
      // TODO rather than PreAuthenticatedAuthenticationToken because mock 
      
      authentication.setAuthenticated(true); // else in MethodSecurityInterceptor tries to reauth...
      // TODO but don't do it for guest ??
      
      // TODO guest rather using AnonymousAuthenticationToken ?!
      
      SecurityContext sc = new SecurityContextImpl();
      sc.setAuthentication(authentication);
      SecurityContextHolder.setContext(sc);
   }

   /**
    * NOT USED FOR NOW
    * @param credentials ex. "Bearer <token>" or (devmode only) "Basic YWRtaW46YWRtaW4="
    */
   public static void login(String credentials) {
      User user = new User("<unknown>", "dummy", new ArrayList<GrantedAuthority>(0));
      Authentication auth = new TestingAuthenticationToken(user, credentials);
      auth.setAuthenticated(true); // else in MethodSecurityInterceptor tries to reauth...
      SecurityContextHolder.getContext().setAuthentication(auth);
   }

   /**
    * devmode only
    * @param username
    * @param password
    */
   public static void loginBasic(String username, String password) {
      login(username, password, "Basic", java.util.Base64.getEncoder().encodeToString(
            (username + ":" + password).getBytes()));
   }

   /**
    * 
    * @param username not used in Bearer mode
    * @param password not used in Bearer mode
    * @param authMode Bearer or (devmode only) Basic
    * @param authHeader
    */
   public static void login(String username, String password, String authMode, String authHeader) {
      User user = new User(username, password, new ArrayList<GrantedAuthority>(0));
      Authentication auth = new TestingAuthenticationToken(user, authMode + " " + authHeader);
      auth.setAuthenticated(true); // else in MethodSecurityInterceptor tries to reauth...
      SecurityContextHolder.getContext().setAuthentication(auth);
   }

   /**
    * devmode only
    */
   public static void loginBasicAsAdmin() {
      //login("Basic YWRtaW46YWRtaW4=");
      loginBasic("admin", "admin");
   }
   
   public static void logout() {
      SecurityContextHolder.clearContext();
   }
   
}
