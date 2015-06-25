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

	public void addResourceOwner(String group) {
		delegate.addResourceOwner(group);
	}

	public void removeResourceOwner(String group) {
		delegate.removeResourceOwner(group);
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

	public boolean isResourceOwner(User user) {
		return delegate.isResourceOwner(user);
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
