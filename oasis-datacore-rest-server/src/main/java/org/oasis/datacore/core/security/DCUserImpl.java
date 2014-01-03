package org.oasis.datacore.core.security;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;


/**
 * TODO implement it on its own and not on top of hardcoded conf'd User anymore ?
 * 
 * @author mdutoo
 *
 */
public class DCUserImpl extends User {
   private static final long serialVersionUID = -1360057839096106512L;

   public final static String ADMIN_GROUP = "admin";

   private boolean isAdmin;
   private Set<String> adminOfModelTypes = new HashSet<String>();
   private Set<String> entityGroups = new HashSet<String>();
   private Set<String> roles = new HashSet<String>();
   
   public DCUserImpl(User user) {
      super(user.getUsername(), user.getPassword(),
            user.isEnabled(), user.isAccountNonExpired(),
            user.isCredentialsNonExpired(), user.isAccountNonLocked(),
            initAuthorities(user.getAuthorities()));
      
      initGroups();
   }

   private void initGroups() {
      for (GrantedAuthority authority : this.getAuthorities()) {
         String role = authority.getAuthority();
         this.roles.add(role); // NB. or AuthorityUtils.authorityListToSet(user.getAuthorities());
         
         if (!this.isAdmin && role.equals(ADMIN_GROUP)) {
            this.isAdmin = true;
            
         } else if (role.startsWith("t_admin_")) { // TODO regex
            String modelTypeName = role.substring("t_admin_".length(), role.length());
            this.adminOfModelTypes.add(modelTypeName);
            // TODO also type readers & writers ?
            
            // else if not datacore related, skip
            
         } else {
            this.entityGroups.add(role);
         }
      }
   }

   private static Collection<? extends GrantedAuthority> initAuthorities(
         Collection<GrantedAuthority> authorities) {
      // TODO could add guest here if not already done...
      return Collections.unmodifiableSet(new HashSet<GrantedAuthority>(authorities));
   }

   public boolean isAdmin() {
      return isAdmin;
   }

   public Set<String> getAdminOfModelTypes() {
      return adminOfModelTypes;
   }

   public Set<String> getEntityGroups() {
      return entityGroups;
   }

   public Set<String> getRoles() {
      return roles;
   }
   
}
