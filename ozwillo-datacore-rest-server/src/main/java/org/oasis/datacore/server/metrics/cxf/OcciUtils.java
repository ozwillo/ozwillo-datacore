package org.oasis.datacore.server.metrics.cxf;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Helper to retrieve and parse JSON of an OCCI configuration from an OCCI Server.
 * 
 * @author mardut
 *
 */
public class OcciUtils {

   private static final Logger logger = LoggerFactory.getLogger(OcciUtils.class);

   private static ObjectMapper mapper = new ObjectMapper(); // to parse OCCI

   /**
    * Retrieves conf from OCCI server.
    * 
    * @param occiUrl URL of an OCCI collection or resource, resp. ex. /compute/ or /compute/9cf2aadd-a652-464b-9182-0945a6f08371
    * @return parsed JSON tree, or null if null or empty occiUrl (disabled).
    * Get values out of it using JSON pointer expressions, ex.
    * occiJsonNode.at("/resources/0/attributes/occi.compute.cores").asDouble(0)
    * @throws IOException
    */
  public static JsonNode getOcciJsonNode(String occiUrlString) throws IOException {
     if (occiUrlString == null || occiUrlString.isEmpty()) {
        return null; // disabled
     }
     
     try {
        URL occiUrl = new URL(occiUrlString);
        
        URLConnection httpConn = occiUrl.openConnection();
        httpConn.setRequestProperty("Accept", "application/json");
        httpConn.connect();
        String occiJsonRes = IOUtils.toString(httpConn.getInputStream());
        JsonNode occiJsonNode = mapper.readTree(occiJsonRes); 
        return occiJsonNode;
        
     } catch (MalformedURLException e) {
        logger.error("Bad URL for datacoreApiServer.metrics.meanRequestThreshold.occiUrl : " + occiUrlString, e);
        throw e;
     }
  }
  
}
