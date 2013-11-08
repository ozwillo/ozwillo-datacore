package org.oasis.datacore.rest.server.common;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang.StringUtils;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;

/**
 * Methods helping writing unit tests
 * @author agiraudon
 *
 */

public class DatacoreTestUtils {

	public static DCResource buildResource(String containerUrl, String modelType, String iri) {
		DCResource resource = new DCResource();
		List<String> types = new ArrayList<String>();
		types.add(modelType);
		resource.setUri(UriHelper.buildUri(containerUrl, modelType, iri));
		resource.setTypes(types);
		return resource;
	}
	
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
