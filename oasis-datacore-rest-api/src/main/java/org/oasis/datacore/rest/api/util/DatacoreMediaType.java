package org.oasis.datacore.rest.api.util;

import javax.ws.rs.core.MediaType;

/*
 * DatacoreMediaType
 * Manage special Media Type used by datacore
 */

public class DatacoreMediaType {
   
   public static final String MEDIA_TYPE_WILDCARD = "*";
   public static final String CHARSET_PARAMETER = "charset";

   /** default (compact) */
   public static final String APPLICATION_JSONLD = "application/json+ld";
   public static final MediaType APPLICATION_JSONLD_TYPE = new MediaType("application", "json+ld");
   public static final String APPLICATION_JSONLD_FORMAT_PARAM = "format";
   public static final String APPLICATION_JSONLD_FORMAT_PREFIX = APPLICATION_JSONLD
         + "; " + APPLICATION_JSONLD_FORMAT_PARAM + "=";
   public static final String JSONLD_EXPAND = "expand";
   public static final String APPLICATION_JSONLD_EXPAND = APPLICATION_JSONLD_FORMAT_PREFIX + JSONLD_EXPAND;
   public static final String JSONLD_FRAME = "frame";
   public static final String APPLICATION_JSONLD_FRAME = APPLICATION_JSONLD_FORMAT_PREFIX + JSONLD_FRAME;
   public static final String JSONLD_FLATTEN = "flatten";
   public static final String APPLICATION_JSONLD_FLATTEN = APPLICATION_JSONLD_FORMAT_PREFIX + JSONLD_FLATTEN;
   public static final String JSONLD_COMPACT = "compact";
   public static final String APPLICATION_JSONLD_COMPACT = APPLICATION_JSONLD_FORMAT_PREFIX + JSONLD_COMPACT;
   
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
