package org.oasis.datacore.playground.security.rest;

import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;


/**
 * Resource that handles playground's OASIS OAuth2 login redirection.
 * 
 * OASIS OAuth2 is done as in https://github.com/pole-numerique/oasis/blob/master/HOWTO/login.md
 * * redirect browser to https://oasis-demo.atolcd.com/a/auth?response_type=code&client_id=29ef97c0-bb33-4f96-be22-3fd480e05d5f&scope=openid%20datacore&redirect_uri=https://portal.ozwillo.com/callback
 * + TODO LATER state and nonce randoms : provide and check at the end
 * * (see *Token*java) then handle browser redirect to https://portal.ozwillo.com/callback?code=eyJpZCI6IjA2MzBhNGRjLThhMWYtNDdkNi05NDY3LWU1N2NmNzM0ZDE3Yy9mUTNoY05nWmpvLWdhejFuUU9RME9nIiwiaWF0IjoxNDE1MjcyNDM3NzQ0LCJleHAiOjE0MTUyNzI0OTc3NDR9
 * to get token, by POST to  
 *    'Authorization:Basic ' + new Buffer(conf.app_client_id + ':' + conf.app_client_secret).toString("base64")
      grant_type: 'authorization_code',
      //redirect_uri: 'http://requestb.in/' + requestBinId // NO since Sept. 2014 has to be an approved app ex. portal
      redirect_uri: 'https://portal.ozwillo.com/callback',
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
@Path("dc/playground/login")
@Component("datacore.playground.loginResource") // else can't autowire Qualified
public class PlaygroundAuthenticationLoginResource extends PlaygroundAuthenticationResourceBase {
   
   /** cache */
   private String loginRedirectUri = null;
   
   @PostConstruct
   protected void init() {
      String state = "a"; // TODO LATER
      String nonce = "a"; // TODO LATER
      loginRedirectUri = accountsLoginEndpointUrl + "?response_type=code&client_id=" + datacoreOAuthClientId
            + "&state=" + state + "&nonce=" + nonce
            + "&scope=" + playgroundScopes + "&redirect_uri=" + playgroundTokenExchangeRedirectUrl;
   }

   @GET
   @Path("")
   public void redirectToKernelLogin() {
      try {
         throw new WebApplicationException(Response.seeOther(new URI(this.loginRedirectUri)).build());
      } catch (URISyntaxException usex) {
         throw new WebApplicationException(usex, Response.serverError()
               .entity("Redirection target " + this.loginRedirectUri + " should be an URI").build());
      }
   }
   
}
