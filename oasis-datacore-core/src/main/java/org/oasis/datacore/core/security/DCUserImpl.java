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
   private Set<String> resourceAdminForModelTypes = new HashSet<String>();
   private Set<String> resourceCreatorForModelTypes = new HashSet<String>();
   private Set<String> resourceWriterForModelTypes = new HashSet<String>();
   private Set<String> resourceReaderForModelTypes = new HashSet<String>();
   private Set<String> adminOfModelTypes = new HashSet<String>();
   
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
         
         if (!this.isAdmin && role.equals(ADMIN_GROUP)) { // TODO or dc_r_admin
            this.isAdmin = true;
            
         } else if (role.startsWith("rom_")) { // TODO regex ; dc_r_t_admin ; admin_r_
            String modelTypeName = role.substring("rom_".length(), role.length());
            this.resourceAdminForModelTypes.add(modelTypeName);
            // TODO also type readers & writers ?
            
         } else if (role.startsWith("mo_")) { // TODO regex ; dc_m_t_admin ; admin_t_ ; NOO = thisModelEntity.owners.add(user)
            // TODO Q manage model rights using groups (required if stored in datacore) or model conf ??
            String modelTypeName = role.substring("mo_".length(), role.length());
            this.adminOfModelTypes.add(modelTypeName);
            
         } else if (role.startsWith("rcm_")) { // TODO regex ; dc_m_t_c_
            // TODO Q manage model rights using groups (required if stored in datacore) or model conf ??
            String modelTypeName = role.substring("rcm_".length(), role.length());
            this.resourceCreatorForModelTypes.add(modelTypeName);
            
            // TODO also type creators !! and LATER even readers, writers ?!
            
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

   public boolean isModelTypeResourceAdmin(String modelType) {
      return resourceAdminForModelTypes.contains(modelType);
   }

   public boolean isModelTypeResourceCreator(String modelType) {
      return resourceCreatorForModelTypes.contains(modelType);
   }

   public boolean isModelTypeResourceWriter(String modelType) {
      return resourceWriterForModelTypes.contains(modelType);
   }

   public boolean isModelTypeResourceReader(String modelType) {
      return resourceReaderForModelTypes.contains(modelType);
   }

   public Set<String> getAdminOfModelTypes() {
      return adminOfModelTypes;
   }
   
}
