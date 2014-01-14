package org.oasis.datacore.oauth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.BaseClientDetails;
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

	private String clientId;

	private String clientSecret;

	private ObjectMapper mapper = new ObjectMapper();

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

	public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException {

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
				Collection<String> values = (Collection<String>) map.get("scope");
				scope.addAll(values);
			}
		}
		DefaultAuthorizationRequest clientAuthentication = new DefaultAuthorizationRequest(remoteClientId, scope);

		if (map.containsKey("resource_ids") || map.containsKey("client_authorities")) {
			Set<String> resourceIds = new HashSet<String>();
			if (map.containsKey("resource_ids")) {
				@SuppressWarnings("unchecked")
				Collection<String> values = (Collection<String>) map.get("resource_ids");
				resourceIds.addAll(values);
			}
			Set<GrantedAuthority> clientAuthorities = new HashSet<GrantedAuthority>();
			if (map.containsKey("client_authorities")) {
				@SuppressWarnings("unchecked")
				Collection<String> values = (Collection<String>) map.get("client_authorities");
				clientAuthorities.addAll(getAuthorities(values));
			}
			BaseClientDetails clientDetails = new BaseClientDetails();
			clientDetails.setClientId(remoteClientId);
			clientDetails.setResourceIds(resourceIds);
			clientDetails.setAuthorities(clientAuthorities);
			clientAuthentication.addClientDetails(clientDetails);
		}
		
		if (map.containsKey(Claims.ADDITIONAL_AZ_ATTR)) {
			try {
				clientAuthentication.setAuthorizationParameters(Collections.singletonMap(Claims.ADDITIONAL_AZ_ATTR,
						mapper.writeValueAsString(map.get(Claims.ADDITIONAL_AZ_ATTR))));
			} catch (IOException e) {
				throw new IllegalStateException("Cannot convert access token to JSON", e);
			}
		}
		
		if (map.containsKey("sub_groups")) {
			Set<GrantedAuthority> userAuthorities = new HashSet<GrantedAuthority>();
			Collection<String> values = (Collection<String>) map.get("sub_groups");
			userAuthorities.addAll(getAuthorities(values));
			clientAuthentication.setAuthorities(userAuthorities);
		}

		Authentication userAuthentication = getUserAuthentication(map, scope);
		
		clientAuthentication.setApproved(true);
		return new OAuth2Authentication(clientAuthentication, userAuthentication);
	}

	private Authentication getUserAuthentication(Map<String, Object> map, Set<String> scope) {
		String username = (String) map.get("user_name");
		if (username==null) {
			return null;
		}
		Set<GrantedAuthority> userAuthorities = new HashSet<GrantedAuthority>();
		if (map.containsKey("sub_groups")) {
			@SuppressWarnings("unchecked")
			Collection<String> values = (Collection<String>) map.get("sub_groups");
			userAuthorities.addAll(getAuthorities(values));
		}
		else {
			// User authorities had better not be empty or we might mistake user for unauthenticated
			userAuthorities.addAll(getAuthorities(scope));
		}
		String email = (String) map.get("email");
		String id = (String) map.get("user_id");
		return new RemoteUserAuthentication(id, username, email, userAuthorities);
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

	private Map<String, Object> postForMap(String path, MultiValueMap<String, String> formData, HttpHeaders headers) {
		if (headers.getContentType() == null) {
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		}
		Map map = restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<MultiValueMap<String, String>>(formData, headers), Map.class).getBody();
		@SuppressWarnings("unchecked")
		Map<String, Object> result = (Map<String, Object>) map;
		return result;
	}

}