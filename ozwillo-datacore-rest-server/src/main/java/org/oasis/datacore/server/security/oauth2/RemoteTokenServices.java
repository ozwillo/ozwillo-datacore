package org.oasis.datacore.server.security.oauth2;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis.datacore.core.security.service.impl.DatacoreSecurityServiceImpl;
import org.oasis.datacore.playground.security.TokenEncrypter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
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
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * Checks (cached) the token calling the Ozwillo Kernel /a/tokeninfo endpoint
 * and retrieves its info.
 */
@Component(value="tokenServices")
public class RemoteTokenServices implements ResourceServerTokenServices {

	protected final Log logger = LogFactory.getLog(getClass());

	private RestOperations restTemplate;

	@Value("${kernel.checkTokenEndpointUrl}")
	private String checkTokenEndpointUrl;
	
	///@Value("${kernel.userInfoEndpointUrl}")
	///private String userInfoEndpointUrl;

   @Value("${datacoreOAuthTokenService.client_id}")
	private String clientId;

   @Value("${datacoreOAuthTokenService.client_secret}")
	private String clientSecret;

	///private ObjectMapper mapper = new ObjectMapper();
	
	/** [Ozwillo] */
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

	@Cacheable(value={"org.springframework.security.oauth2.provider.OAuth2Authentication"}, key="#accessToken")
	public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException {
	   accessToken = tokenEncrypter.decrypt(accessToken); // [Ozwillo]

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
		formData.add("token", accessToken);
		HttpHeaders headers = new HttpHeaders();
		headers.set(javax.ws.rs.core.HttpHeaders.AUTHORIZATION, getAuthorizationHeader(clientId, clientSecret));
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
		Map<String, String> requestParameters = Collections.emptyMap();
		boolean approved = true;
		Set<String> responseTypes = Collections.emptySet();
		Set<String> resourceIds = Collections.emptySet();
		Map<String, Serializable> extensionProperties = Collections.emptyMap();
		OAuth2Request clientAuthentication = new OAuth2Request(requestParameters, clientId, null, approved, scope,
				resourceIds, checkTokenEndpointUrl, responseTypes, extensionProperties);


		Authentication userAuthentication = getUserAuthentication(map, scope);
		
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
	 * Calls Ozwilo Kernel checkTokenEndpointUrl i.e. /a/tokeninfo to check token
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