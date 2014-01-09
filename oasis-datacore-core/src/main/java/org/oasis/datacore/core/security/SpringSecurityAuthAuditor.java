package org.oasis.datacore.core.security;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


/**
 * Returns current user id according to Spring Security.
 * 
 * NB. is integrated with Oasis Kernel OAuth2 as long as Spring Security is.
 * 
 * @author mdutoo
 *
 */
public class SpringSecurityAuthAuditor implements AuditorAware<String> {
   
   /** in case there is no Spring Security Authentication object in context
    * (even guest or system should have one). Can only happen if EntityServiceImpl
    * is used directly and not through its secured interface. */
   public static final String NO_USER = "*no_user*";

	/**
	 * Returns current user id
	 */
	@Override
	public String getCurrentAuditor() {
	   Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return (authentication == null) ? NO_USER : authentication.getName();
	}

}
