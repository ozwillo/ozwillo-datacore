package org.oasis.datacore.rest.server.cxf;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import org.oasis.datacore.rest.api.util.JaxrsApiProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;


/**
 * Let your JAXRS (root) server-side resources extend it to get
 * access to HTTP protocol info through injected JAXRS API context
 * objects.
 * 
 * @author mdutoo
 *
 */
public class JaxrsServerBase {
   
   @Autowired
   @Qualifier("datacore.cxfJaxrsApiProvider")
   protected HttpHeaders httpHeaders;

   @Autowired
   @Qualifier("datacore.cxfJaxrsApiProvider")
   protected UriInfo uriInfo;

   @Autowired
   @Qualifier("datacore.cxfJaxrsApiProvider")
   protected Request jaxrsRequest;

   @Autowired
   @Qualifier("datacore.cxfJaxrsApiProvider")
   protected JaxrsApiProvider jaxrsApiProvider;
   
}
