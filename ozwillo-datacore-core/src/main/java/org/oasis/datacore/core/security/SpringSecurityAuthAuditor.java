package org.oasis.datacore.core.security;

import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * Returns current user id according to Spring Security.
 * 
 * NB. is integrated with Ozwillo Kernel OAuth2 as long as Spring Security is.
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
	public Optional<String> getCurrentAuditor() {
		 Optional<String> opt = Optional.of(datacoreSecurityService.getCurrentUserId());
		 return opt;
	}

}
