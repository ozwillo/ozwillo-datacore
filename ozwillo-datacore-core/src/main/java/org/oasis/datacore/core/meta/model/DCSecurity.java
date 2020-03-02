package org.oasis.datacore.core.meta.model;

import java.util.LinkedHashSet;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Security policy for its DCModel. Therefore its values should not change
 * often, ex. modelCreators should be a dedicated group rather than a single
 * user.
 * 
 * NB. no guest mode (but apps can create their own system users for
 * various purposes)
 * 
 * @author mdutoo, agiraudon
 * 
 */
public class DCSecurity {

	/** default is true (but isGuestReadable takes precedence) */
	private boolean isAuthentifiedReadable = true;
	/**
	 * default is false NB. writers can't be anonymous, should at least have a
	 * named account, like on wikipedia
	 */
	private boolean isAuthentifiedWritable = false; // TODO
	/**
	 * default is true NB. creators can't be anonymous, should at least have a
	 * named account, like on wikipedia
	 */
	private boolean isAuthentifiedCreatable = true;
	
	/**
	 * = resource admins ; Give access to all data no matter what permission on the entity
	 * Update model : NO
	 * Create resource : YES
	 * Read resource : YES
	 * Write resource : YES
	 */
	private LinkedHashSet<String> resourceOwners = new LinkedHashSet<>();
	
	/**
	 * Set as owners in ACLs of resource at creation. If model's is null,
	 * uses its project's as defaults. If empty, resourceService will set
	 * newly created resource owners to the current user's u_[org_id].
	 * Somehow redundant when resource creators are already resourceOwners, but
	 * can be useful if resourceOwners are changed so that original owners
	 * remain (or to copy entities across collections in mongo).
	 * Update model : NO
	 * Create resource : YES
	 * Read resource : YES
	 * Write resource : NO
	 */
	private LinkedHashSet<String> resourceCreators = new LinkedHashSet<>();

	/** who to set owner at creation (u_creator if empty) */
   private LinkedHashSet<String> resourceCreationOwners = new LinkedHashSet<>();
	
	/**
	 * Give access to all data no matter what permissions on the entity
	 * Update model : NO
	 * Create resource : NO
	 * Read resource : YES
	 * Write resource : YES
	 */
	private LinkedHashSet<String> resourceWriters = new LinkedHashSet<>();
	
	/**
	 * Give access to all data no matter what permissions on the entity
	 * Update model : NO
	 * Create resource : NO
	 * Read resource : YES
	 * Write resource : NO
	 */
	private LinkedHashSet<String> resourceReaders = new LinkedHashSet<>();
	

	/** defaults */
   public DCSecurity() {
      
   }
   
	/** copy constructor */
	public DCSecurity(DCSecurity original) {
      this.isAuthentifiedReadable = original.isAuthentifiedReadable;
      this.isAuthentifiedWritable = original.isAuthentifiedWritable;
      this.isAuthentifiedCreatable = original.isAuthentifiedCreatable;
      this.resourceOwners = new LinkedHashSet<>(original.resourceOwners);
      this.resourceCreators = new LinkedHashSet<>(original.resourceCreators);
      this.resourceWriters = new LinkedHashSet<>(original.resourceWriters);
      this.resourceReaders = new LinkedHashSet<>(original.resourceReaders);
   }

	public boolean isAuthentifiedReadable() {
		return isAuthentifiedReadable;
	}

	public void setAuthentifiedReadable(boolean isAuthentifiedReadable) {
		this.isAuthentifiedReadable = isAuthentifiedReadable;
	}

	public boolean isAuthentifiedWritable() {
		return isAuthentifiedWritable;
	}

	public void setAuthentifiedWritable(boolean isAuthentifiedWriteable) {
		this.isAuthentifiedWritable = isAuthentifiedWriteable;
	}

	public boolean isAuthentifiedCreatable() {
		return isAuthentifiedCreatable;
	}

	public void setAuthentifiedCreatable(boolean isAuthentifiedCreatable) {
		this.isAuthentifiedCreatable = isAuthentifiedCreatable;
	}
	

   public LinkedHashSet<String> getResourceOwners() {
      return resourceOwners;
   }

   public void setResourceOwners(LinkedHashSet<String> resourceOwners) {
      this.resourceOwners = resourceOwners;
   }

   public LinkedHashSet<String> getResourceCreators() {
      return resourceCreators;
   }

   public void setResourceCreators(LinkedHashSet<String> resourceCreators) {
      this.resourceCreators = resourceCreators;
   }

   public LinkedHashSet<String> getResourceCreationOwners() {
      return resourceCreationOwners;
   }

   public void setResourceCreationOwners(LinkedHashSet<String> resourceCreationOwners) {
      this.resourceCreationOwners = resourceCreationOwners;
   }

   public LinkedHashSet<String> getResourceWriters() {
      return resourceWriters;
   }

   public void setResourceWriters(LinkedHashSet<String> resourceWriters) {
      this.resourceWriters = resourceWriters;
   }

   public LinkedHashSet<String> getResourceReaders() {
      return resourceReaders;
   }

   public void setResourceReaders(LinkedHashSet<String> resourceReaders) {
      this.resourceReaders = resourceReaders;
   }

	public void addResourceOwner(String group) {
		this.resourceOwners.add(group);
	}

	public void removeResourceOwner(String group) {
		this.resourceOwners.remove(group);
	}

	public void addResourceCreator(String group) {
		this.resourceCreators.add(group);
	}

	public void removeResourceCreator(String group) {
		this.resourceCreators.remove(group);
	}

	public void addReader(String group) {
		this.resourceReaders.add(group);
	}

	public void removeReader(String group) {
		this.resourceReaders.remove(group);
	}
	
	public void addWriter(String group) {
		this.resourceWriters.add(group);
	}

	public void removeWriter(String group) {
		this.resourceWriters.remove(group);
	}

	public boolean isResourceOwner(User user) {
		for (GrantedAuthority authority : user.getAuthorities()) {
			if (this.resourceOwners != null && this.resourceOwners.contains(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}

	public boolean isResourceCreator(User user) {
		for (GrantedAuthority authority : user.getAuthorities()) {
			if (this.resourceCreators != null && this.resourceCreators.contains(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isResourceReader(User user) {
		for (GrantedAuthority authority : user.getAuthorities()) {
			if (this.resourceReaders != null && this.resourceReaders.contains(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isResourceWriter(User user) {
		for (GrantedAuthority authority : user.getAuthorities()) {
			if (this.resourceWriters != null && this.resourceWriters.contains(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}

}