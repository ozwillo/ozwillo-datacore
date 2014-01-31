package org.oasis.datacore.core.security.service.impl;

import org.oasis.datacore.core.security.DCUserImpl;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("datacoreSecurityServiceImpl")
public class DatacoreSecurityServiceImpl implements DatacoreSecurityService {

	/**
	 * User id (for resource auditor & creator as owner) in case there is no
	 * Spring Security Authentication object in context (even guest or system
	 * should have one). Can only happen if EntityServiceImpl is used directly
	 * and not through its secured interface.
	 */
	public static final String NO_USER = "*no_user*";

	public DCUserImpl getCurrentUser() {

		Authentication currentUserAuth = SecurityContextHolder.getContext().getAuthentication();

		if (currentUserAuth == null) {
			throw new RuntimeException("No authentication in security context");
		}

		if (currentUserAuth.getPrincipal() instanceof DCUserImpl) {
			return (DCUserImpl) currentUserAuth.getPrincipal();
		} else {
			throw new UsernameNotFoundException("");
		}

	}

	public String getCurrentUserId() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication.getPrincipal() != null && authentication.getPrincipal() instanceof DCUserImpl) {
			return ((DCUserImpl) authentication.getPrincipal()).getUsername();
		} else {
			return (authentication == null) ? NO_USER : authentication.getName();
		}

	}

}
