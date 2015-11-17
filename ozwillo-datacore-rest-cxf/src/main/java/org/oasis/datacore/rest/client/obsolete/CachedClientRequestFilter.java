package org.oasis.datacore.rest.client.obsolete;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.collections.EnumerationUtils;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.ListUtils;

/**
 * 
 * @author mdutoo
 * @obsolete not used (though could replace CXF client in interceptor to send ETag)
 */
@Provider // required to implement ClientRequestFilter
public class CachedClientRequestFilter implements ClientRequestFilter {

   @Override
   public void filter(ClientRequestContext requestContext) throws IOException {
      requestContext.getPropertyNames();
      
   }

}
