package org.oasis.datacore.playground.security.rest;

import javax.ws.rs.Path;

import org.oasis.datacore.playground.rest.PlaygroundResourceBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * Holds conf for playground OASIS OAuth integration. 
 * 
 * @author mdutoo
 *
 */
@Path("dc/playground")
@Component("datacore.playground.rest.resource") // else can't autowire Qualified
public class PlaygroundAuthenticationResourceBase extends PlaygroundResourceBase {

   protected static final String RESPONSE_ACCESS_TOKEN = "access_token";
   protected static final String BEARER_AUTH_PREFIX = "Bearer ";
   
   @Value("${accounts.authEndpointUrl}")
   protected String accountsLoginEndpointUrl; // = "https://accounts.oasis-eu.org/a/login"; // TODO rm
   @Value("${datacorePlayground.scopes}")
   protected String playgroundScopes; // = "openid%20datacore%20profile%20email"; // also profile email to get any user infos
   @Value("${datacoreOAuthTokenService.client_id}")
   protected String datacoreOAuthClientId; // = "dc"; // = "portal"; // TODO rm
   ///@Value("${datacoreApiServer.baseUrl}")
   ///protected String datacoreApiServerBaseUrl;
   @Value("${datacoreOAuthTokenService.client_secret}")
   protected String datacoreOAuthClientSecret; // = "UJ6mN9Q5SFU6EtVT9zAODkx26U+8eptUDHZgeuVQ1vQ"; // = "1pimkXJiEEDQr1rA89V39SFSqt779weBJqYAhgJ7kX4"; // TODO rm
   @Value("${accounts.tokenEndpointUrl}")
   protected String accountsTokenEndpointUrl; // = "https://accounts.oasis-eu.org/a/token"; // TODO rm
   @Value("${datacorePlayground.tokenExchangeRedirectUrl}")
   protected String playgroundTokenExchangeRedirectUrl; // = "https://data.oasis-eu.org/dc/playground/token"; // = https://portal.oasis-eu.org/callback // TODO rm
   @Value("${datacorePlayground.uiUrl}")
   protected String playgroundUiUrl; // = "http://localhost:8080/dc-ui/index.html"; // TODO rm
   ///@Value("${datacore.devmode}")
   ///protected boolean devmode;
   @Value("${kernel.userInfoEndpointUrl}")
   protected String kernelUserInfoEndpointUrl; // = "https://kernel.oasis-eu.org/a/userinfo"; // TODO rm
   //@Value("${datacorePlayground.cookie.secure}")
   // WARNING #{!datacore.devmode} doesn't work because of Spring EL bug when resolving dotted pathes
   // see http://georgovassilis.blogspot.fr/2013/11/spring-value-and-resolving-property.html
   protected boolean cookieSecure; // requires HTTPS see https://www.owasp.org/index.php/SecureFlag
   @Value("${datacorePlayground.cookie.maxAge}")
   protected int cookieMaxAge; // = 3600; // tokens last 1 hour
   
}
