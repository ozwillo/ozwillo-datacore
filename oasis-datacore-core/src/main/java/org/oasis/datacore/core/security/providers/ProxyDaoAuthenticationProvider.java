package org.oasis.datacore.core.security.providers;

import org.oasis.datacore.core.security.DCUserImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;

public class ProxyDaoAuthenticationProvider implements AuthenticationProvider {

	@Autowired
	private DaoAuthenticationProvider delegate;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		Authentication auth = delegate.authenticate(authentication);
		if (auth != null && auth.getPrincipal() != null && auth.getPrincipal() instanceof User) {
			DCUserImpl dcUser = new DCUserImpl((User) auth.getPrincipal());
			UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(dcUser, authentication.getCredentials(), dcUser.getAuthorities());
			result.setDetails(authentication.getDetails());
			return result;
		}
		return auth;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		if (authentication.equals(UsernamePasswordAuthenticationToken.class)) {
			return true;
		} else {
			return false;
		}
	}

	public void setDelegate(DaoAuthenticationProvider delegate) {
		this.delegate = delegate;
	}

	public DaoAuthenticationProvider getDelegate() {
		return delegate;
	}

}
