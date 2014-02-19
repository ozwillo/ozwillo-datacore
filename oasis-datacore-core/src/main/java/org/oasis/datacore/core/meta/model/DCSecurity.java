package org.oasis.datacore.core.meta.model;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Security policy for its DCModel. Therefore its values should not change
 * often, ex. modelCreators should be a dedicated group rather than a single
 * user.
 * 
 * @author mdutoo, agiraudon
 * 
 */
public class DCSecurity {

	/** default is true */
	private boolean isGuestReadable = true;
	/** default is true (but isGuestReadable takes precedence) */
	private boolean isAuthentifiedReadable = true;
	/**
	 * default is true NB. writers can't be anonymous, should at least have a
	 * named account, like on wikipedia
	 */
	private boolean isAuthentifiedWritable = true;
	/**
	 * default is true NB. creators can't be anonymous, should at least have a
	 * named account, like on wikipedia
	 */
	private boolean isAuthentifiedCreatable = true;

	/**
	 * Update model : YES
	 * Create resource : YES
	 * Read resource : YES
	 * Write resource : YES
	 */
	private Set<String> admin = new HashSet<String>();
	
	/**
	 * Give access to all data no matter what permission on the entity
	 * Update model : NO
	 * Create resource : YES
	 * Read resource : YES
	 * Write resource : YES
	 */
	private Set<String> resourceAdmin = new HashSet<String>();
	
	/**
	 * Give access to all data no matter what permissions on the entity
	 * Update model : NO
	 * Create resource : YES
	 * Read resource : YES
	 * Write resource : NO
	 */
	private Set<String> resourceCreator = new HashSet<String>();
	
	/**
	 * Give access to all data no matter what permissions on the entity
	 * Update model : NO
	 * Create resource : NO
	 * Read resource : YES
	 * Write resource : YES
	 */
	private Set<String> resourceWriter = new HashSet<String>();
	
	/**
	 * Give access to all data no matter what permissions on the entity
	 * Update model : NO
	 * Create resource : NO
	 * Read resource : YES
	 * Write resource : NO
	 */
	private Set<String> resourceReader = new HashSet<String>();
	

	public boolean isGuestReadable() {
		return isGuestReadable;
	}

	public void setGuestReadable(boolean isGuestReadable) {
		this.isGuestReadable = isGuestReadable;
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

	public void addAdmin(String group) {
		this.admin.add(group);
	}

	public void removeAdmin(String group) {
		this.admin.remove(group);
	}

	public void addResourceAdmin(String group) {
		this.resourceAdmin.add(group);
	}

	public void removeResourceAdmin(String group) {
		this.resourceAdmin.remove(group);
	}

	public void addResourceCreator(String group) {
		this.resourceCreator.add(group);
	}

	public void removeResourceCreator(String group) {
		this.resourceCreator.remove(group);
	}

	public void addReader(String group) {
		this.resourceReader.add(group);
	}

	public void removeReader(String group) {
		this.resourceReader.remove(group);
	}
	
	public void addWriter(String group) {
		this.resourceWriter.add(group);
	}

	public void removeWriter(String group) {
		this.resourceWriter.remove(group);
	}

	public boolean isAdmin(User user) {
		for (GrantedAuthority authority : user.getAuthorities()) {
			if (this.admin.contains(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}

	public boolean isResourceAdmin(User user) {
		for (GrantedAuthority authority : user.getAuthorities()) {
			if (this.resourceAdmin.contains(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}

	public boolean isResourceCreator(User user) {
		for (GrantedAuthority authority : user.getAuthorities()) {
			if (this.resourceCreator.contains(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isResourceReader(User user) {
		for (GrantedAuthority authority : user.getAuthorities()) {
			if (this.resourceReader.contains(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isResourceWriter(User user) {
		for (GrantedAuthority authority : user.getAuthorities()) {
			if (this.resourceWriter.contains(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}

}