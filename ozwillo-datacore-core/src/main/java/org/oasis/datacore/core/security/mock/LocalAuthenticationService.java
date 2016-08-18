package org.oasis.datacore.core.security.mock;

import java.util.Set;

import org.oasis.datacore.core.security.DCUserImpl;
import org.oasis.datacore.core.security.service.impl.DatacoreSecurityServiceImpl;
import org.oasis.datacore.rest.client.cxf.mock.AuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
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
	
   public static final String SYSTEM_USER = "system";
   public static final String ADMIN_USER = "admin";

   /** guest login & group NOO forbidden
    * @obsolete */
   public final static String GUEST = "guest";
   /** local mock admin group, to specify in datacore.security.admins prop  */
   public final static String ADMIN_GROUP = "admin";
   
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
 
   /**
    * @obsolete use rather getCurrentUser().getEntityGroups()
    * (permissions should be precomputed rather than added at getGroups time)
    * TODO move comment contents once actual impl done
    * @return
    */
   public Set<String> getCurrentUserEntityGroups() {
      Authentication currentUserAuth = SecurityContextHolder.getContext().getAuthentication();
      ///HashSet<String> currentUserRoles;
      if (currentUserAuth == null) {
         throw new RuntimeException("No authentication in security context");   
         /*// should not happen if security is enabled
         boolean isSecurityEnabled = true; // TODO conf prop
         if (isSecurityEnabled) {
            throw new RuntimeException("No authentication in security context");   
         } else {
            // assume guest
            ///currentUserRoles = new HashSet<String>(1);
            ///currentUserRoles.add("GUEST"); // modeled as (Datacore-wide) default group
         }*/
         
      } else {
         // TODO refactor to getRoles AND / OR cache it in UserDatacoreImpl
         DCUserImpl user = (DCUserImpl) currentUserAuth.getPrincipal();
         /*Collection<? extends GrantedAuthority> grantedAuthorities = currentUserAuth.getAuthorities();
         currentUserRoles = new HashSet<String>(grantedAuthorities.size() + 1);
         currentUserRoles.add("GUEST"); // modeled as (Datacore-wide) default group
         for (GrantedAuthority grantedAuthority : grantedAuthorities) {
            currentUserRoles.add(grantedAuthority.getAuthority());
         }*/
         return user.getEntityGroups();
      }
      // TODO Q or (b) also GUEST OK when empty ACL ?
      // NO maybe dangerous because *adding* a group / role to ACL would *remove* GUEST from it
      // so solution would be : API (i.e. Social Graph) as (a) but storage translates it to (b), allows to store less characters
      // requires adding an $or criteria : _w $size 0 $or _w $in currentUserRoles
      // which is more complex and might be (TODO test) worse performance-wise
      // => OPT LATER
      // (in any way, there'll always be a balance to find between performance and storage)
   }
   
}
