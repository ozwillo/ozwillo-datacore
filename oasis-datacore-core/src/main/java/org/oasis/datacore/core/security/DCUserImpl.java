package org.oasis.datacore.core.security;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.oasis.datacore.core.security.mock.LocalAuthenticationService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;


/**
 * The username is his id and therefore MUST be unique.
 * A user's personal organization group is "u_<username>".
 * TODO implement it on its own and not on top of hardcoded conf'd User anymore ?
 * 
 * @author mdutoo
 *
 */
public class DCUserImpl extends User {
   private static final long serialVersionUID = -1360057839096106512L;
   
   /** user personal organization ACL id is u_ + username. This allows to differentiate it
    * (and knowing which Kernel endpoint manages it) without calling the Kernel, and
    * prevents creating users with the same id as an organization in test user XML conf
    * (when using Kernel users, this will never happen because all ids are uuids). */
   public static final String USER_ORGANIZATION_PREFIX = "u_";

   private Set<String> roles = new HashSet<String>();
   private boolean isGuest;
   private boolean isAdmin;
   private Set<String> entityGroups = new HashSet<String>();
   
   public DCUserImpl(UserDetails user, Set<String> admins) {
      super(user.getUsername(), user.getPassword(),
            user.isEnabled(), user.isAccountNonExpired(),
            user.isCredentialsNonExpired(), user.isAccountNonLocked(),
            initAuthorities(user.getAuthorities()));
      
      initGroups(admins);
   }

   private void initGroups(Set<String> admins) {
      if (this.getUsername().equals(LocalAuthenticationService.GUEST)) {
         this.isGuest = true;
         this.roles.add(LocalAuthenticationService.GUEST);
         this.entityGroups.add(LocalAuthenticationService.GUEST);
         return;
      }
      
      String personalOrganizationGroup = usernameToPersonalOrganizationGroup(this.getUsername()); // TODO or not ???
      if (admins.contains(personalOrganizationGroup)) {
         this.isAdmin = true;
      }
         
      for (GrantedAuthority authority : this.getAuthorities()) {
         String role = authority.getAuthority();
         this.roles.add(role); // NB. or AuthorityUtils.authorityListToSet(user.getAuthorities());
         if (!this.isAdmin && admins.contains(role)) {
        	   this.isAdmin = true;
         }
         this.entityGroups.add(role);
      }
      
 	   this.entityGroups.add(personalOrganizationGroup);
   }
   
   /** TODO move to securityService */
   public static String usernameToPersonalOrganizationGroup(String username) {
      return USER_ORGANIZATION_PREFIX + username;
   }
   
   public static String personalOrganizationGroupToUsername(String group) {
      return group.substring(2);
   }
   
   public static String organizationGroupToId(String group) {
      if (isPersonalOrganizationGroup(group)) {
         return personalOrganizationGroupToUsername(group);
      }
      return group;
   }
   
   public static boolean isPersonalOrganizationGroup(String group) {
      return group.startsWith(USER_ORGANIZATION_PREFIX);
   }

   private static Collection<? extends GrantedAuthority> initAuthorities(
         Collection<? extends GrantedAuthority> authorities) {
      // TODO could add guest here if not already done...
      return Collections.unmodifiableSet(new HashSet<GrantedAuthority>(authorities));
   }

   public Set<String> getRoles() {
      return roles;
   }

   /** TODO or isAuthentified ? */
   public boolean isGuest() {
      return isGuest;
   }

   public boolean isAdmin() {
      return isAdmin;
   }

   public Set<String> getEntityGroups() {
      return entityGroups;
   }
   
}
