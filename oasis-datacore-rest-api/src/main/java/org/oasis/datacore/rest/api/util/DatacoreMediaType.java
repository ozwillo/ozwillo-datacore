package org.oasis.datacore.rest.api.util;

import javax.ws.rs.core.MediaType;

/*
 * DatacoreMediaType
 * Manage special Media Type used by datacore
 */

public class DatacoreMediaType {
   
   public static final String MEDIA_TYPE_WILDCARD = "*";
   public static final String CHARSET_PARAMETER = "charset";
   
   public static final String APPLICATION_RDF = "application/rdf+xml";
   public static final MediaType APPLICATION_RDF_TYPE = new MediaType("application", "rdf+xml");
   
   public static final String APPLICATION_NTRIPLES = "text/plain";
   public static final MediaType APPLICATION_NTRIPLES_TYPE = new MediaType("text", "plain");
   
   public static final String APPLICATION_TURTLE = "text/turtle";
   public static final MediaType APPLICATION_TURTLE_TYPE = new MediaType("text", "turtle");
   
   public static final String APPLICATION_N3 = "text/rdf+n3";
   public static final MediaType APPLICATION_N3_TYPE = new MediaType("text", "rdf+n3");
   
   public static final String APPLICATION_NQUADS = "text/x-nquads";
   public static final MediaType APPLICATION_NQUADS_TYPE = new MediaType("text", "x-nquads");
   
   public static final String APPLICATION_RDF_JSON = "application/rdf+json";
   public static final MediaType APPLICATION_RDF_JSON_TYPE = new MediaType("application", "rdf+json");
   

}
