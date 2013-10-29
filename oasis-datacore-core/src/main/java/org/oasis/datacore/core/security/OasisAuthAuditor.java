package org.oasis.datacore.core.security;

import org.springframework.data.domain.AuditorAware;


/**
 * Returns current user
 * TODO integrate with Oasis Kernel Auth
 * 
 * NB. If Oasis Users are in same db, could be an instance of User instead and let audited entities refer to it
 * see http://satishab.blogspot.fr/2013/03/part-2-persistence-layer-with-mongo-db.html
 * 
 * @author mdutoo
 *
 */
public class OasisAuthAuditor implements AuditorAware<String> {

	/**
	 * Returns current user
	 */
	@Override
	public String getCurrentAuditor() {
	   //return authenticationService.getCurrentUser();
		return "mdutoo";
	}

}
