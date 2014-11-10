package org.oasis.datacore.playground.security.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.PostConstruct;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.form.Form;
import org.oasis.datacore.playground.security.TokenEncrypter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Resource that redirects root GET to a given (absolute or relative) target URI.
 * 
 * as in https://github.com/pole-numerique/oasis/blob/master/HOWTO/login.md
 * TODO
 * * redirect browser to https://oasis-demo.atolcd.com/a/auth?response_type=code&client_id=29ef97c0-bb33-4f96-be22-3fd480e05d5f&scope=openid%20datacore&redirect_uri=https://portal.oasis-eu.org/callback
 * + TODO LATER state and nonce randoms : provide and check at the end
 * * then handle browser redirect to https://portal.oasis-eu.org/callback?code=eyJpZCI6IjA2MzBhNGRjLThhMWYtNDdkNi05NDY3LWU1N2NmNzM0ZDE3Yy9mUTNoY05nWmpvLWdhejFuUU9RME9nIiwiaWF0IjoxNDE1MjcyNDM3NzQ0LCJleHAiOjE0MTUyNzI0OTc3NDR9
 * to get token, by POST to  
 *    'Authorization:Basic ' + new Buffer(conf.app_client_id + ':' + conf.app_client_secret).toString("base64")
      grant_type: 'authorization_code',
      //redirect_uri: 'http://requestb.in/' + requestBinId // NO since Sept. 2014 has to be an approved app ex. portal
      redirect_uri: 'https://portal.oasis-eu.org/callback',
      code: code
 * and JSON.parse(body).access_token
 * * put it in a cookie (LATER encrypted, using server-generated salt ?!?!) 
 * * then add a security filter that (LATER decrypts it and) checks it with kernel directly or by reusing spring flow or bean
 * 
 * TODO LATER2 extract to shared project...
 * 
 * @author mdutoo
 *
 */
@Path("dc/playground")
@Component("datacore.rest.loginResource") // else can't autowire Qualified
public class PlaygroundAuthenticationResource {

   private static final String RESPONSE_ACCESS_TOKEN = "access_token";
   private static final String BEARER_AUTH_PREFIX = "Bearer ";
   
   @Value("${accounts.authEndpointUrl}")
   private String accountsLoginEndpointUrl; // = "https://accounts.oasis-eu.org/a/login"; // TODO rm
   @Value("${datacorePlayground.scopes}")
   private String playgroundScopes; // = "openid%20datacore%20profile%20email"; // also profile email to get any user infos
   @Value("${datacoreOAuthTokenService.client_id}")
   private String datacoreOAuthClientId; // = "dc"; // = "portal"; // TODO rm
   @Value("${datacoreApiServer.baseUrl}")
   private String datacoreApiServerBaseUrl;
   @Value("${datacoreOAuthTokenService.client_secret}")
   private String datacoreOAuthClientSecret; // = "UJ6mN9Q5SFU6EtVT9zAODkx26U+8eptUDHZgeuVQ1vQ"; // = "1pimkXJiEEDQr1rA89V39SFSqt779weBJqYAhgJ7kX4"; // TODO rm
   @Value("${accounts.tokenEndpointUrl}")
   private String accountsTokenEndpointUrl; // = "https://accounts.oasis-eu.org/a/token"; // TODO rm
   @Value("${datacorePlayground.tokenExchangeRedirectUrl}")
   private String playgroundTokenExchangeRedirectUrl; // = "https://data.oasis-eu.org/dc/playground/token"; // = https://portal.oasis-eu.org/callback // TODO rm
   @Value("${datacorePlayground.uiUrl}")
   private String playgroundUiUrl; // = "http://localhost:8080/dc-ui/index.html"; // TODO rm
   @Value("${datacore.devmode}")
   private boolean devmode;
   @Value("${kernel.userInfoEndpointUrl}")
   private String kernelUserInfoEndpointUrl; // = "https://kernel.oasis-eu.org/a/userinfo"; // TODO rm
   //@Value("${datacorePlayground.cookie.secure}")
   // WARNING #{!datacore.devmode} doesn't work because of Spring EL bug when resolving dotted pathes
   // see http://georgovassilis.blogspot.fr/2013/11/spring-value-and-resolving-property.html
   private boolean cookieSecure; // requires HTTPS see https://www.owasp.org/index.php/SecureFlag
   @Value("${datacorePlayground.cookie.maxAge}")
   private int cookieMaxAge; // = 3600; // tokens last 1 hour
   @Autowired
   private TokenEncrypter tokenEncrypter;
   private ObjectMapper jsonNodeMapper = new ObjectMapper();
   /** cache */
   private String loginRedirectUri = null;
   
   @PostConstruct
   protected void init() {
      cookieSecure = !devmode; // cookie secure requires HTTPS see https://www.owasp.org/index.php/SecureFlag
      
      String state = "a"; // TODO LATER
      String nonce = "a"; // TODO LATER
      loginRedirectUri = accountsLoginEndpointUrl + "?response_type=code&client_id=" + datacoreOAuthClientId
            + "&state=" + state + "&nonce=" + nonce
            + "&scope=" + playgroundScopes + "&redirect_uri=" + playgroundTokenExchangeRedirectUrl;
   }

   @GET
   @Path("login")
   public void redirectToKernelLogin() {
      try {
         throw new WebApplicationException(Response.seeOther(new URI(this.loginRedirectUri)).build());
      } catch (URISyntaxException usex) {
         throw new WebApplicationException(usex, Response.serverError()
               .entity("Redirection target " + this.loginRedirectUri + " should be an URI").build());
      }
   }

   @GET
   @Path("token")
   public void handleKernelCodeRedirectAndExchangeForToken(
         @QueryParam("code") String code, @QueryParam("state") String state)
               throws BadRequestException, ClientErrorException, InternalServerErrorException {
      String clientIdColonSecret = datacoreOAuthClientId + ':' + datacoreOAuthClientSecret;
      String tokenExchangeBasicAuth = "Basic " + new String(Base64.encodeBase64(clientIdColonSecret.getBytes()));
      WebClient tokenExchangeClient = WebClient.create(accountsTokenEndpointUrl) // "http://requestb.in/saf6sosa"
            //.type(MediaType.APPLICATION_FORM_URLENCODED)
            .header(HttpHeaders.AUTHORIZATION, tokenExchangeBasicAuth);
      Response tokenExchangeRes;
      try {
         tokenExchangeRes = tokenExchangeClient.form(new Form()
               .set("grant_type", "authorization_code").set("code", code)
               .set("redirect_uri", playgroundTokenExchangeRedirectUrl));
      } catch (Exception ex) {
         // TODO or ServiceUnavailableException ?
         throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity("Error calling Kernel to exchange token from code  : " + ex.getMessage())
                     .type(MediaType.TEXT_PLAIN).build());
      }
      // parsing response to get token :
      // NB. no need to go beyond explicit bare JSON parsing (MessageBodyProvider etc.)
      String tokenExchangeResBody = tokenExchangeRes.readEntity(String.class);
      JsonNode tokenExchangeResJsonNode;
      try {
         tokenExchangeResJsonNode = jsonNodeMapper.readTree(tokenExchangeResBody);
      } catch (IOException ioex) {
         int errorStatus = (Status.Family.SUCCESSFUL != tokenExchangeRes.getStatusInfo().getFamily())
               ? tokenExchangeRes.getStatus() : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
         throw new WebApplicationException(Response.status(errorStatus)
               .entity("Error parsing as JSON tokenExchangeResBody returned by Kernel ("
                     + tokenExchangeResBody + ") : " + ioex.getMessage())
                     .type(MediaType.TEXT_PLAIN).build());
      }
      JsonNode errorNode = tokenExchangeResJsonNode.get("error");
      if (errorNode != null) {
         String error = errorNode.asText(); // NB. more lenient thant textValue()
         int errorStatus = (Status.Family.SUCCESSFUL != tokenExchangeRes.getStatusInfo().getFamily())
               ? tokenExchangeRes.getStatus() : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
         throw new WebApplicationException(Response.status(errorStatus)
               .entity("Returned tokenExchangeResBody for exchanging code is error : " + error)
                     .type(MediaType.TEXT_PLAIN).build());
      }
      // also checking for error status even if no JSON error info :
      if (Status.Family.SUCCESSFUL != tokenExchangeRes.getStatusInfo().getFamily()) {
         throw new WebApplicationException(Response.status(tokenExchangeRes.getStatus())
               .entity("Error calling Kernel to exchange token from code : "
                     + "response status is not successful (2xx) but " + tokenExchangeRes.getStatus())
                     .type(MediaType.TEXT_PLAIN).build());
      }
      String token = null;
      try {
         token = tokenExchangeResJsonNode.get(RESPONSE_ACCESS_TOKEN).textValue();
         // NB. also expires_in, scope, id_token
      } catch (Exception ex) {
         throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity("Error getting " + RESPONSE_ACCESS_TOKEN
                     + " from tokenExchangeResJsonNode returned by Kernel ("
                     + tokenExchangeResJsonNode + ") : " + ex.getMessage())
                     .type(MediaType.TEXT_PLAIN).build());
      }
      if (token == null) {
         throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity("No " + RESPONSE_ACCESS_TOKEN
                     + " in tokenExchangeResJsonNode returned by Kernel ("
                     + tokenExchangeResJsonNode + ")")
                     .type(MediaType.TEXT_PLAIN).build());
      }
      
      // TODO LATER check JWT signature see schambon's OpenIdCService using nimbus
      
      // getting user info :
      WebClient userInfoClient = WebClient.create(kernelUserInfoEndpointUrl) // "http://requestb.in/saf6sosa"
            .header(HttpHeaders.AUTHORIZATION, BEARER_AUTH_PREFIX + token);
      Response userInfoRes;
      try {
         userInfoRes = userInfoClient.get();
      } catch (Exception ex) {
         // TODO or ServiceUnavailableException ?
         throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity("Error calling Kernel to get user info  : " + ex.getMessage())
                     .type(MediaType.TEXT_PLAIN).build());
      }
      // parsing response to get token :
      // NB. no need to go beyond explicit bare JSON parsing (MessageBodyProvider etc.)
      String userInfoResBody = userInfoRes.readEntity(String.class);
      /*JsonNode userInfoResJsonNode;
      try {
         userInfoResJsonNode = jsonNodeMapper.readTree(userInfoResBody);
      } catch (IOException ioex) {
         int errorStatus = (Status.Family.SUCCESSFUL != userInfoRes.getStatusInfo().getFamily())
               ? userInfoRes.getStatus() : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
         throw new WebApplicationException(Response.status(errorStatus)
               .entity("Error parsing as JSON tokenExchangeResBody returned by Kernel ("
                     + userInfoResBody + ") : " + ioex.getMessage())
                     .type(MediaType.TEXT_PLAIN).build());
      }
      JsonNode errorNode = tokenExchangeResJsonNode.get("error");
      if (errorNode != null) {
         String error = errorNode.asText(); // NB. more lenient thant textValue()
         int errorStatus = (Status.Family.SUCCESSFUL != tokenExchangeRes.getStatusInfo().getFamily())
               ? tokenExchangeRes.getStatus() : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
         throw new WebApplicationException(Response.status(errorStatus)
               .entity("Returned tokenExchangeResBody for exchanging code is error : " + error)
                     .type(MediaType.TEXT_PLAIN).build());
      }
      // also checking for error status even if no JSON error info :
      if (Status.Family.SUCCESSFUL != tokenExchangeRes.getStatusInfo().getFamily()) {
         throw new WebApplicationException(Response.status(tokenExchangeRes.getStatus())
               .entity("Error calling Kernel to exchange token from code : "
                     + "response status is not successful (2xx) but " + tokenExchangeRes.getStatus())
                     .type(MediaType.TEXT_PLAIN).build());
      }*/
      
      
      // set auth cookie while redirecting to app :
      try {
         String authHeader = BEARER_AUTH_PREFIX + tokenEncrypter.encrypt(token);
         NewCookie encryptedTokenCookie = new NewCookie("authorization", authHeader,
               "/", // WARNING if no ;Path=/ it is not set after a redirect see http://stackoverflow.com/questions/1621499/why-cant-i-set-a-cookie-and-redirect
               null, null, cookieMaxAge, cookieSecure);
         NewCookie userInfoCookie = new NewCookie("userinfo", userInfoResBody,
               "/", // WARNING if no ;Path=/ it is not set after a redirect see http://stackoverflow.com/questions/1621499/why-cant-i-set-a-cookie-and-redirect
               null, null, cookieMaxAge, cookieSecure);
         throw new WebApplicationException(Response.seeOther(new URI(playgroundUiUrl))
               .cookie(encryptedTokenCookie).cookie(userInfoCookie).build()); 
         // or + 
      } catch (URISyntaxException usex) {
         throw new WebApplicationException(usex, Response.serverError()
               .entity("Redirection target " + datacoreApiServerBaseUrl + " should be an URI").build());
      }
   }
   
}
