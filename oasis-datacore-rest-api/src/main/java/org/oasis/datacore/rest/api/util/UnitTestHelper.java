package org.oasis.datacore.rest.api.util;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang.StringUtils;

/**
 * Methods helping writing unit tests
 * @author agiraudon
 *
 */

public class UnitTestHelper {
	
	public static String arrayToIri(String... array) {
		String iri = "";
		for(String tmp : array) {
			if(!StringUtils.isEmpty(tmp)) {
				iri += tmp + "/";
			}
		}
		return iri.substring(0, iri.length()-1);
	}
	
	public static int getHttpStatusFromWAE(WebApplicationException webApplicationException) {
		if(webApplicationException != null && webApplicationException.getResponse() != null) {
			return webApplicationException.getResponse().getStatus();
		} else {
			return 0;
		}		
	}

}
