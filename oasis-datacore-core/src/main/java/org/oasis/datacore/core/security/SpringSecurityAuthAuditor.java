package org.oasis.datacore.core.security;

import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
	@Qualifier("datacoreSecurityServiceImpl")
	private DatacoreSecurityService datacoreSecurityService;

	/**
	 * Returns current user id
	 */
	@Override
	public String getCurrentAuditor() {
		return datacoreSecurityService.getCurrentUserId();
	}

}
