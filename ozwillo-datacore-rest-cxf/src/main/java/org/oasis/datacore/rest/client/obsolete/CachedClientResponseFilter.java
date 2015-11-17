package org.oasis.datacore.rest.client.obsolete;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;

/**
 * 
 * @author mdutoo
 * @obsolete not used
 */
@Provider // required to implement ClientResponseFilter
public class CachedClientResponseFilter implements ClientResponseFilter {

   @Override
   public void filter(ClientRequestContext requestContext,
         ClientResponseContext responseContext) throws IOException {
      responseContext.getEntityTag();
      
   }

}
