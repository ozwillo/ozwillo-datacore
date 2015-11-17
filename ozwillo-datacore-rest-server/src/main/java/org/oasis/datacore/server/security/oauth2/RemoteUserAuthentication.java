package org.oasis.datacore.server.security.oauth2;

import java.util.Collection;

import org.oasis.datacore.core.security.DCUserImpl;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Authentication token representing a user decoded from a UAA access token.
 * 
 * @author Dave Syer
 *
 */
public class RemoteUserAuthentication extends AbstractAuthenticationToken implements Authentication {

	private static final long serialVersionUID = -4366727088199157491L;
	private String username;
	private String email;
	private User user;

	public RemoteUserAuthentication(String username,
	      Collection<? extends GrantedAuthority> authorities, DCUserImpl dcUserImpl) {
		super(authorities);
		this.username = username;
		this.setAuthenticated(true);
		this.user = dcUserImpl;
	}

	@Override
	public Object getCredentials() {
		return "<N/A>";
	}

	@Override
	public Object getPrincipal() {
		return user;
	}

	public String getUsername() {
		return username;
	}

	public String getEmail() {
		return email;
	}

}