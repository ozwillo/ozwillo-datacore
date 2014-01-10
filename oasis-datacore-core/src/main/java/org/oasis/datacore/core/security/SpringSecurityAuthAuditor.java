package org.oasis.datacore.core.security;

import org.oasis.datacore.core.security.mock.MockAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;


/**
 * Returns current user id according to Spring Security.
 * 
 * NB. is integrated with Oasis Kernel OAuth2 as long as Spring Security is.
 * 
 * @author mdutoo
 *
 */
public class SpringSecurityAuthAuditor implements AuditorAware<String> {

   @Autowired
   private MockAuthenticationService authenticationService;
   
	/**
	 * Returns current user id
	 */
	@Override
	public String getCurrentAuditor() {
	   return authenticationService.getCurrentUserId();
	}

}
