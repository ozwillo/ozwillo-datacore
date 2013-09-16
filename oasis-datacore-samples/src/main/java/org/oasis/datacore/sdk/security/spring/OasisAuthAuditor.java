package org.oasis.datacore.sdk.security.spring;

import org.springframework.data.domain.AuditorAware;

public class OasisAuthAuditor implements AuditorAware<String> {

	/**
	 * Returns current user
	 * TODO integrate with Oasis Kernel Auth
	 * 
	 * If Oasis Users are in same db, could be an instance of User instead and let audited entities refer to it
	 * see http://satishab.blogspot.fr/2013/03/part-2-persistence-layer-with-mongo-db.html
	 */
	@Override
	public String getCurrentAuditor() {
		return "mdutoo";
	}

}
