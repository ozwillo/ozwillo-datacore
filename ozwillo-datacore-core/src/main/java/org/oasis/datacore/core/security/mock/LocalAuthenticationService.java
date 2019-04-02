package org.oasis.datacore.core.security.mock;

import org.oasis.datacore.core.security.service.impl.DatacoreSecurityServiceImpl;
import org.oasis.datacore.rest.client.cxf.mock.AuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Auth service using default hardcoded user conf.
 * Outside dev mode, only for programmatically logged system users.
 * In dev mode, also loggable through BASIC auth & also brings test users.
 * 
 * TODO separate what is mock (loginAs) from what is not (getCurrentUser(Id)/EntityGroups())
 * 
 * Hardcoded conf should provide users :
 * - "guest" with guest authority (modeled as (Datacore-wide) default group)
 * - all other users with additional guest authority (add it on load if not yet there)
 * - "admin" user ; TODO users are made admin by conf (hardcoded and LATER REST API)
 * 
 * @author mdutoo
 *
 */
@Component // always instanciated but non-test code can't call it
public class LocalAuthenticationService {
	
   public static final String ADMIN_USER = "admin";

   /** TODO LATER remove optional when there is a mock in the -core project */
   @Autowired(required = false)
   @Qualifier("datacore.localUserDetailsService")
   private UserDetailsService mockUserDetailsService = null;

   /** to init user */
   @Autowired
   private DatacoreSecurityServiceImpl securityServiceImpl;
   
   /**
    * WARNING can only log in a mock auth hardcoded conf user, not ex. an OAuth user
    * (because it would have to get its details) 
    * @param username
    */
   public void loginAs(String username) {
      if (mockUserDetailsService != null) {
         UserDetails userDetails = mockUserDetailsService.loadUserByUsername(username);
         if (userDetails == null) {
            SecurityContextHolder.clearContext();
            throw new RuntimeException("Unknown user " + username);
         }
         
         User user = securityServiceImpl.buildUser((User) userDetails); // NB. also precomputes its permissions
         AuthenticationHelper.loginAs(user);
         
      } else { // ex. in -core tests or client
         AuthenticationHelper.loginAs(username);
      }
   }
   
   public void logout() {
      AuthenticationHelper.logout(); // clears context
   }
}
