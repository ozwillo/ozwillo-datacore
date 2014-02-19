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

   /** guest login & group */
   public final static String GUEST = "guest";
   public final static String ADMIN_GROUP = "admin";

   private Set<String> roles = new HashSet<String>();
   private boolean isGuest;
   private boolean isAdmin;
   private Set<String> entityGroups = new HashSet<String>();
   
   public DCUserImpl(User user) {
      super(user.getUsername(), user.getPassword(),
            user.isEnabled(), user.isAccountNonExpired(),
            user.isCredentialsNonExpired(), user.isAccountNonLocked(),
            initAuthorities(user.getAuthorities()));
      
      initGroups();
   }

   private void initGroups() {
      if (this.getUsername().equals(GUEST)) {
         this.isGuest = true;
         this.roles.add(GUEST);
         this.entityGroups.add(GUEST);
         return;
      }
         
      for (GrantedAuthority authority : this.getAuthorities()) {
         String role = authority.getAuthority();
         this.roles.add(role); // NB. or AuthorityUtils.authorityListToSet(user.getAuthorities());
         if (!this.isAdmin && role.equals(ADMIN_GROUP)) {
        	 this.isAdmin = true;
         }
         this.entityGroups.add(role);
      }
      
      // We add the user the default group wich is u_ + user.getId()
      if(this.getUsername() != null && !"".equals(this.getUsername())) {
    	  this.entityGroups.add("u_" + this.getUsername());
      }
   }

   private static Collection<? extends GrantedAuthority> initAuthorities(
         Collection<GrantedAuthority> authorities) {
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
