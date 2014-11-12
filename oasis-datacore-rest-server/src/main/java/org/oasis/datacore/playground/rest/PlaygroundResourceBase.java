package org.oasis.datacore.playground.rest;

import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Holds common conf for playground.
 * 
 * @author mdutoo
 *
 */
public abstract class PlaygroundResourceBase {

   @Value("${datacore.devmode}")
   protected boolean devmode;
   @Value("${datacoreApiServer.baseUrl}")
   protected String baseUrl;
   @Value("${datacoreApiServer.containerUrl}")
   protected String containerUrl;
   @Value("${kernel.baseUrl}")
   protected String kernelBaseUrl;
   
   @Value("${datacorePlayground.uiUrl}")
   protected String playgroundUiUrl; // = "http://localhost:8080/dc-ui/index.html"; // TODO rm

   @Value("${datacoreApiServer.knownDatacoreContainerUrls}")
   protected String knownDatacoreContainerUrls;
   @Value("${datacoreApiServer.query.maxScan}")
   protected int queryMaxScan;
   @Value("${datacoreApiServer.query.maxStart}")
   protected int queryMaxStart;
   @Value("${datacoreApiServer.query.maxLimit}")
   protected int queryMaxLimit;

   /** for quick JSON serialization only */
   protected ObjectMapper jsonNodeMapper = new ObjectMapper();
   
}
