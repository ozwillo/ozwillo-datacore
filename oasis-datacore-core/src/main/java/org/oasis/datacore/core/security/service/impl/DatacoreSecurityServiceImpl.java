package org.oasis.datacore.core.security.service.impl;

import java.util.Set;

import javax.annotation.PostConstruct;

import org.oasis.datacore.core.security.DCUserImpl;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("datacoreSecurityServiceImpl")
public class DatacoreSecurityServiceImpl implements DatacoreSecurityService {

	private static final String USER_GROUP_PREFIX = "u_";

   @Value("${datacore.security.admins}")
	private String commaSeparatedAdmins;
   private Set<String> admins;
	
	/**
	 * User id (for resource auditor & creator as owner) in case there is no
	 * Spring Security Authentication object in context (even guest or system
	 * should have one). Can only happen if EntityServiceImpl is used directly
	 * and not through its secured interface.
	 */
	public static final String NO_USER = "*no_user*";
	
	@PostConstruct
	private void init() {
	   this.admins = StringUtils.commaDelimitedListToSet(this.commaSeparatedAdmins);
	}

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
      if (authentication == null) {
         throw new RuntimeException("No authentication in security context"); // or NO_USER ??
      }

		if (authentication.getPrincipal() != null && authentication.getPrincipal() instanceof DCUserImpl) {
			return ((DCUserImpl) authentication.getPrincipal()).getUsername();
		} else {
			return authentication.getName();
		}

	}

	public String getUserGroup() {
		return USER_GROUP_PREFIX + this.getCurrentUserId();
	}

   public DCUserImpl buildUser(UserDetails loadedUser) {
      return new DCUserImpl(loadedUser, this.admins);
   }

}
