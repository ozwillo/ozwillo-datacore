package org.oasis.datacore.core.meta.model.contribution;

import org.oasis.datacore.core.meta.model.DCSecurity;
import org.springframework.security.core.userdetails.User;

/**
 * Contribution security strategy
 * We need readers to be able to create contributions
 * A contributor can only see his contribution
 * A model admin / model resource admin can see all the contributions on the model
 * @author agiraudon
 *
 */
public class DCContributionSecurity extends DCSecurity {
	
	private DCSecurity delegate;

	public DCContributionSecurity(DCSecurity delegate) {
		this.delegate = delegate;
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public boolean isGuestReadable() {
		return delegate.isGuestReadable();
	}

	public void setGuestReadable(boolean isGuestReadable) {
		delegate.setGuestReadable(isGuestReadable);
	}

	public boolean isAuthentifiedReadable() {
		return delegate.isAuthentifiedReadable();
	}

	public void setAuthentifiedReadable(boolean isAuthentifiedReadable) {
		delegate.setAuthentifiedReadable(isAuthentifiedReadable);
	}

	public boolean isAuthentifiedWritable() {
		return delegate.isAuthentifiedWritable();
	}

	public void setAuthentifiedWritable(boolean isAuthentifiedWriteable) {
		delegate.setAuthentifiedWritable(isAuthentifiedWriteable);
	}

	public boolean isAuthentifiedCreatable() {
		return delegate.isAuthentifiedCreatable();
	}

	public void setAuthentifiedCreatable(boolean isAuthentifiedCreatable) {
		delegate.setAuthentifiedCreatable(isAuthentifiedCreatable);
	}

	public void addAdmin(String group) {
		delegate.addAdmin(group);
	}

	public void removeAdmin(String group) {
		delegate.removeAdmin(group);
	}

	public void addResourceAdmin(String group) {
		delegate.addResourceAdmin(group);
	}

	public void removeResourceAdmin(String group) {
		delegate.removeResourceAdmin(group);
	}

	public void addResourceCreator(String group) {
		delegate.addResourceCreator(group);
	}

	public void removeResourceCreator(String group) {
		delegate.removeResourceCreator(group);
	}

	public void addReader(String group) {
		delegate.addReader(group);
	}

	public void removeReader(String group) {
		delegate.removeReader(group);
	}

	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	public void addWriter(String group) {
		delegate.addWriter(group);
	}

	public void removeWriter(String group) {
		delegate.removeWriter(group);
	}

	public boolean isAdmin(User user) {
		return delegate.isAdmin(user);
	}

	public boolean isResourceAdmin(User user) {
		return delegate.isResourceAdmin(user);
	}
	
	public boolean isResourceCreator(User user) {
		return delegate.isResourceReader(user);
	}

	public boolean isResourceReader(User user) {
		return delegate.isResourceReader(user);
	}

	public boolean isResourceWriter(User user) {
		return delegate.isResourceWriter(user);
	}

	public String toString() {
		return delegate.toString();
	}
	
}
