package org.oasis.datacore.core.security.service;

import org.oasis.datacore.core.security.DCUserImpl;

public interface DatacoreSecurityService {

	DCUserImpl getCurrentUser();

	String getCurrentUserId();

	String getUserGroup();
}
