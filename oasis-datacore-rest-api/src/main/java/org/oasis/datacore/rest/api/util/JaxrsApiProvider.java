package org.oasis.datacore.rest.api.util;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;


/**
 * Provides HTTP protocol info to JAXRS server impl.
 * Can be used either to directly call JAXRS API context objects
 * (headers, uri, request)' methods implemented on top of JAXRS impl context objects
 * (i.e. in case of CXF impls, (In)Message), or through regular JAXRS impls
 * context objects (ex. CXF's).
 * 
 * @author mdutoo
 *
 */
public interface JaxrsApiProvider extends HttpHeaders, UriInfo, Request {

   HttpHeaders getHttpHeaders();
   UriInfo getUriInfo();
   Request getJaxrsRequest();
   
}
