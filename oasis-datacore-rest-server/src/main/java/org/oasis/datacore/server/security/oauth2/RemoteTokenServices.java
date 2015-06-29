package org.oasis.datacore.server.security.oauth2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.oasis.datacore.core.security.service.impl.DatacoreSecurityServiceImpl;
import org.oasis.datacore.playground.security.TokenEncrypter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.DefaultAuthorizationRequest;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * Queries the /check_token endpoint to obtain the contents of an access token.
 *
 * If the endpoint returns a 400 response, this indicates that the token is invalid.
 *
 * @author Dave Syer
 * @author Luke Taylor
 *
 */
public class RemoteTokenServices implements ResourceServerTokenServices {

	protected final Log logger = LogFactory.getLog(getClass());

	private RestOperations restTemplate;

	private String checkTokenEndpointUrl;
	
	private String userInfoEndpointUrl;

	private String clientId;

	private String clientSecret;

	private ObjectMapper mapper = new ObjectMapper();
	
	/** [OASIS] */
   @Autowired
   private TokenEncrypter tokenEncrypter;
   /** to init user */
   @Autowired
   private DatacoreSecurityServiceImpl securityServiceImpl;
   

	public RemoteTokenServices() {
		restTemplate = new RestTemplate();
		((RestTemplate)restTemplate).setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			// Ignore 400
			public void handleError(ClientHttpResponse response) throws IOException {
				if (response.getRawStatusCode() != 400) {
					super.handleError(response);
				}
			}
		});
	}

	public void setRestTemplate(RestOperations restTemplate) {
		this.restTemplate = restTemplate;
	}

	public void setCheckTokenEndpointUrl(String checkTokenEndpointUrl) {
		this.checkTokenEndpointUrl = checkTokenEndpointUrl;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}
	
	public void setUserInfoEndpointUrl(String userInfoEndpointUrl) {
		this.userInfoEndpointUrl = userInfoEndpointUrl;
	}

	public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException {
	   accessToken = tokenEncrypter.decrypt(accessToken); // [OASIS]

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
		formData.add("token", accessToken);
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", getAuthorizationHeader(clientId, clientSecret));
		Map<String, Object> map = postForMap(checkTokenEndpointUrl, formData, headers);

		if (map.containsKey("active") && Boolean.FALSE.equals(map.get("active"))) {
			logger.debug("check_token returned error: " + map.get("error"));
			throw new InvalidTokenException(accessToken);
		}

		if (!map.containsKey("client_id")) {
			logger.warn("client_id must be present in response from introspection point");
			throw OAuth2Exception.create(OAuth2Exception.UNSUPPORTED_RESPONSE_TYPE, "client_id must be present in response from introspection point");
		}
		
		String remoteClientId = (String) map.get("client_id");

		Set<String> scope = new HashSet<String>();
		if (map.containsKey("scope")) {
			Object scopes = map.get("scope");
			if(scopes instanceof String) {
				String values = (String)scopes;
				scope.add(values);
			} else {
				@SuppressWarnings("unchecked")
				Collection<String> values = (Collection<String>) map.get("scope");
				scope.addAll(values);
			}
		}
		DefaultAuthorizationRequest clientAuthentication = new DefaultAuthorizationRequest(remoteClientId, scope);
		
		Authentication userAuthentication = getUserAuthentication(map, scope);
		clientAuthentication.setApproved(true);
		
		return new OAuth2Authentication(clientAuthentication, userAuthentication);
	}

	/**
	 * 
	 * @param map props, including sub (user id) & sub_groups (user groups / authorities)
	 * @param scope (actually not used, can be found in clientAuthentication part of OAuth2Authentication)
	 * @return
	 */
   private Authentication getUserAuthentication(Map<String, Object> map, Set<String> scope) {
		
		Set<GrantedAuthority> userAuthorities = new HashSet<GrantedAuthority>();
		
		if (map.containsKey("sub_groups")) {
			@SuppressWarnings("unchecked")
			Collection<String> values = (Collection<String>) map.get("sub_groups");
			userAuthorities.addAll(getAuthorities(values));
		}
		
		return createRemoteUserAuthentication((String)map.get("sub"), userAuthorities);
			
	}
   
   public Authentication createRemoteUserAuthentication(String username,
         Collection<? extends GrantedAuthority> userAuthorities) {
      return new RemoteUserAuthentication(username, userAuthorities,
            securityServiceImpl.buildUser(loadUser(username, userAuthorities)));
   }
   
   /** impl of Spring UserDetails loading */
   public UserDetails loadUser(String username,
         Collection<? extends GrantedAuthority> authorities) {
      return new User(username, "", authorities);
   }

	@Override
	public OAuth2AccessToken readAccessToken(String accessToken) {
		throw new UnsupportedOperationException("Not supported: read access token");
	}

	private Set<GrantedAuthority> getAuthorities(Collection<String> authorities) {
		Set<GrantedAuthority> result = new HashSet<GrantedAuthority>();
		for (String authority : authorities) {
			result.add(new SimpleGrantedAuthority(authority));
		}
		return result;
	}

	private String getAuthorizationHeader(String clientId, String clientSecret) {
		String creds = String.format("%s:%s", clientId, clientSecret);
		try {
			return "Basic " + new String(Base64.encode(creds.getBytes("UTF-8")));
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Could not convert String");
		}
	}

	/**
	 * Calls OASIS Kernel checkTokenEndpointUrl i.e. /a/tokeninfo to check token
	 * @param path
	 * @param formData
	 * @param headers
	 * @return
	 */
	private Map<String, Object> postForMap(String path, MultiValueMap<String, String> formData, HttpHeaders headers) {
		if (headers.getContentType() == null) {
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<MultiValueMap<String, String>>(formData, headers), Map.class).getBody();
		return map;
	}

}