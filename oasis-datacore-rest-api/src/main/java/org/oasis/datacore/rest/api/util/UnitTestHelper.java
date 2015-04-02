package org.oasis.datacore.rest.api.util;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Methods helping writing unit tests
 * @author agiraudon, mdutoo
 *
 */

public class UnitTestHelper {
   
   /**
    * Test assert helper
    * @param waex
    * @return
    */
   public static String readBodyAsString(WebApplicationException waex) {
      Object entity = waex.getResponse().getEntity();
      if (entity instanceof String) {
         return (String) entity;
      }
      if (entity instanceof InputStream) {
         try {
            return IOUtils.toString((InputStream) entity);
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }
      throw new IllegalArgumentException(); // when ??
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
