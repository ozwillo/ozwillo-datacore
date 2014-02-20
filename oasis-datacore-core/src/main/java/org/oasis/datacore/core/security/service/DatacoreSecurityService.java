package org.oasis.datacore.core.security.service;

import org.oasis.datacore.core.security.DCUserImpl;

public interface DatacoreSecurityService {

	public DCUserImpl getCurrentUser();

	public String getCurrentUserId();

	public String getUserGroup();

}
