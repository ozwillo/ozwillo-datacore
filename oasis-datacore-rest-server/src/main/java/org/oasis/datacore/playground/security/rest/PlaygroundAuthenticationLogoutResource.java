package org.oasis.datacore.playground.security.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.oasis.datacore.playground.security.TokenEncrypter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;


import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.jaxrs.client.WebClient;


/**
 * Resource that handles playground's OASIS OAuth2 logout redirection after invalidating cache.
 * 
 * @author Benjamin Girard
 *
 */
@Path("dc/playground/logout")
@Component("datacore.playground.logoutResource") // else can't autowire Qualified
public class PlaygroundAuthenticationLogoutResource extends PlaygroundAuthenticationResourceBase {
   
   /** to be able to get from cache on 304 not modified exception, and other manual put & evict */
  // @Autowired
  // @Qualifier("datacore.rest.server.cache.security.OAuth2Authentication")
   protected Cache resourceCache; // EhCache getNativeCache

   
   public ObjectMapper jsonNodeMapper = new ObjectMapper();

   @Autowired
   private TokenEncrypter tokenEncrypter;
   
   /** cache */

   
   @PostConstruct
   protected void init() {

   
   }

   @GET
   @Path("")
   public void redirectToKernelLogin(
         @CookieValue(value = "authorization", defaultValue = "") String AuthCookie,
         @CookieValue(value = "userinfo", defaultValue = "") String UserInfo,HttpServletResponse response,
         HttpServletRequest request
         ) {
      
      
      
      String logoutRedirectUri = null;
      
      try {
         Map<String,Object> UserInfomap= null;
         try {
            @SuppressWarnings("unchecked")
            Map<String,Object> parsedUserInfo = jsonNodeMapper.readValue(UserInfo, Map.class);
            UserInfomap = parsedUserInfo;
         } catch (IOException ioex) {
            int errorStatus = 400;
            throw new WebApplicationException(Response.status(errorStatus)
                  .entity("Error parsing as JSON userInfoResBody returned by Kernel ("
                        + UserInfo + ") : " + ioex.getMessage())
                        .type(MediaType.TEXT_PLAIN).build());
         }
         
         String cryptToken=AuthCookie;
         String token=tokenEncrypter.decrypt(cryptToken);
         String id_token=(String) UserInfomap.get(RESPONSE_ID_TOKEN);
         
         
         logoutRedirectUri = accountsLogoutPageUrl + "?d_token_hint"+id_token+"&post_logout_redirect_uri="+ LogoutRedirectUrl;
         
         /*
          * STEP 1 : REVOKE TOKEN 
          * 
          * */
         String clientIdColonSecret = datacoreOAuthClientId + ':' + datacoreOAuthClientSecret;
         String tokenExchangeBasicAuth = "Basic " + new String(Base64.encodeBase64(clientIdColonSecret.getBytes()));
        
         
         WebClient tokenExchangeClient = WebClient.create(accountsLogoutPageUrl) // "http://requestb.in/saf6sosa"
               .type(MediaType.APPLICATION_FORM_URLENCODED)
               .header(HttpHeaders.AUTHORIZATION, tokenExchangeBasicAuth);
         Response tokenRevokeRespons;
         try {
            tokenRevokeRespons = tokenExchangeClient.form(new Form()
                  .param("token", token)
                  .param("token_type_hint", "access_token"));
         } catch (Exception ex) {
            // TODO or ServiceUnavailableException ?
            throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                  .entity("Error calling Kernel to revoke token from code  : " + ex.getMessage())
                        .type(MediaType.TEXT_PLAIN).build());
         }         
         /*
          * 
          * STEP 2 REMOVE CACHE AND COOKIE
          * 
          */
         
         resourceCache.evict(cryptToken);
         {
         Cookie cookie = new Cookie("authorization", null);
         Cookie cookie2 = new Cookie("userinfo", null);
         cookie.setMaxAge(0);
         cookie2.setMaxAge(0);
         cookie.setPath(StringUtils.hasLength(request.getContextPath()) ? request.getContextPath() : "/");
         cookie2.setPath(StringUtils.hasLength(request.getContextPath()) ? request.getContextPath() : "/");
         response.addCookie(cookie);
         response.addCookie(cookie2);
         }
         /*
          * 
          * 
          * STEP 3 : LOGOUT ON OZWILLO
          * 
          * */
         
         throw new WebApplicationException(Response.seeOther(new URI(logoutRedirectUri)).build());
         
      } catch (URISyntaxException usex) {
         throw new WebApplicationException(usex, Response.serverError()
               .entity("Redirection target " + logoutRedirectUri + " should be an URI").build());
      }
   }
   
}
