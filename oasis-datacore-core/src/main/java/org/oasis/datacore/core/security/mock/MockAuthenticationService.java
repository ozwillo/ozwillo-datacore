package org.oasis.datacore.core.security.mock;

import java.util.ArrayList;
import java.util.Set;

import org.oasis.datacore.core.security.DCUserImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Mock local auth service using default hardcoded user conf.
 * 
 * TODO migrate / merge to custom AuthenticationProvider (?) that returns well-filled
 * OIDCAuthenticationToken
 * http://aykutakin.wordpress.com/2013/07/08/spring-security-spring-custom-authentication-provider/
 * 
 * ALL AUTHENTICATION SERVICES / PROVIDER should provide users :
 * - guest with guest authority (modeled as (Datacore-wide) default group) TODO OR use AnonymousAuthenticationToken ?
 * - all other users with additional guest authority (add it on load if not yet there)
 * - "admin" user ; TODO users are made admin by conf (hardcoded and LATER REST API)
 * 
 * @author mdutoo
 *
 */
@Component // always instanciated but non-test code can't call it
public class MockAuthenticationService {
	
   /** User id (for resource auditor & creator as owner) in case there is no Spring Security
    * Authentication object in context (even guest or system should have one). Can only
    * happen if EntityServiceImpl is used directly and not through its secured interface. */
   public static final String NO_USER = "*no_user*";
   
   /** TODO LATER remove optional when there is a mock in the -core project */
   @Autowired(required = false)
   @Qualifier("datacore.userDetailsService")
   private UserDetailsService mockUserDetailsService;
   
   public void loginAs(String username) {
      UserDetails userDetails = mockUserDetailsService.loadUserByUsername(username);
      if (userDetails == null) {
         SecurityContextHolder.clearContext();
         throw new RuntimeException("Unknown user " + username);
      }
      
      DCUserImpl user = new DCUserImpl((User) userDetails); // NB. also precomputes its permissions 
      
      Authentication authentication = new TestingAuthenticationToken(user, "",
            new ArrayList<GrantedAuthority>(userDetails.getAuthorities()));
      // TODO rather than PreAuthenticatedAuthenticationToken because mock 
      
      authentication.setAuthenticated(true); // else in MethodSecurityInterceptor tries to reauth...
      // TODO but don't do it for guest ??
      
      // TODO guest rather using AnonymousAuthenticationToken ?!
      
      SecurityContext sc = new SecurityContextImpl();
      sc.setAuthentication(authentication);
      SecurityContextHolder.setContext(sc);
   }
   
   public void logout() {
      SecurityContextHolder.clearContext();
   }
   
   /** @return the name of the given user's self group / authority (allows ex. tutor).
    * NB. userId would not work because we have to know that is a *self* group. */
   public String getUserGroup(String userId) {
      return "u_" + userId;
   }


   /** TODO in service interface or at least util */
   public DCUserImpl getCurrentUser() {
      Authentication currentUserAuth = SecurityContextHolder.getContext().getAuthentication();
      if (currentUserAuth == null) {
         throw new RuntimeException("No authentication in security context");  
      }
      return (DCUserImpl) currentUserAuth.getPrincipal();
   }

   /** for resource : to put creator as owner, and get auditor (created/modifiedBy) */
   public String getCurrentUserId() {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      return (authentication == null) ? NO_USER : authentication.getName();
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
