package org.oasis.datacore.playground.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.AbstractBeanFactory;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Holds common conf for playground.
 * 
 * @author mdutoo
 *
 */
public abstract class PlaygroundResourceBase {
   
   /** to get any conf prop */
   @Autowired
   protected AbstractBeanFactory beanFactory;

   @Value("${spring.profiles.current:dev}")
   protected String environment;
   @Value("${datacore.securitymode:devmode}")
   protected String securitymode;
   @Value("${datacore.devmode}")
   protected boolean devmode;
   @Value("${datacore.localauthdevmode}")
   protected boolean localauthdevmode;
   @Value("${datacoreApiServer.baseUrl}")
   protected String baseUrl;
   @Value("${datacoreApiServer.containerUrl}")
   protected String containerUrl;
   @Value("${datacoreApiServer.apiDocsUrl}")
   protected String apiDocsUrl;
   @Value("${kernel.baseUrl}")
   protected String kernelBaseUrl;
   @Value("${accounts.baseUrl}")
   protected String accountsBaseUrl;
   
   ////////////////////////////////////////////////
   // NOT REQUIRED BY OAUTH :
   
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
   @Value("${datacoreApiServer.query.defaultLimit}")
   protected int queryDefaultLimit;

   /** for quick JSON serialization only */
   protected ObjectMapper jsonNodeMapper = new ObjectMapper();

   /** to get any conf prop
    * see http://stackoverflow.com/questions/1771166/access-properties-file-programatically-with-spring */
   public String getPropertyValue(String propName) {
      return beanFactory.resolveEmbeddedValue("${" + propName + "}");
   }
   
}
