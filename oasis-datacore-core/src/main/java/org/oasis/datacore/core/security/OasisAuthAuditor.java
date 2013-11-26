package org.oasis.datacore.core.security;

import org.springframework.data.domain.AuditorAware;


/**
 * Returns current user
 * TODO integrate with Oasis Kernel Auth, and refactor this as mock / test auditor impl
 * 
 * NB. If Oasis Users are in same db, could be an instance of User instead and let audited entities refer to it
 * see http://satishab.blogspot.fr/2013/03/part-2-persistence-layer-with-mongo-db.html
 * 
 * @author mdutoo
 *
 */
public class OasisAuthAuditor implements AuditorAware<String> {
   
   public static final String TEST_AUDITOR = "Administrator";

	/**
	 * Returns current user
	 */
	@Override
	public String getCurrentAuditor() {
	   //return authenticationService.getCurrentUser();
		return TEST_AUDITOR;
	}

}
