package org.oasis.datacore.rest.api.client;

import java.net.URLEncoder;

import org.apache.cxf.jaxrs.client.WebClient;
import javax.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/*
 * WARNING: You need to get a valid token from atolcd before running these tests.
 * @Ignore
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-client-test-context.xml" })
public class AuditLogEndpointTest {
	private String url = "https://oasis-demo.atolcd.com/l/event";
	private String token = "eyJpZCI6IjgyOTQ5ZDA3LTM3ZjgtNDY0Ni05NjY5LTQ3OGU0MmU0NDhiMCIsImlhdCI6MTM5Mzg0MDQ4MjY5NSwiZXhwIjoxMzkzODQ0MDgyNjk1fQ";
	
	/*
	 * Will fail with error if it receive status code 500
	 * @Ignore
	 */
	@Test
	@Ignore
	public void postLogWithSimpleHttp() throws Exception {	
		String urlParameters = URLEncoder.encode("body={\"foo\": \"bar\"}", "UTF-8");

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Authorization", "Bearer " + token);
		con.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));

		//Send post
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + urlParameters);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
 
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		System.out.println(response.toString());
	}

	/*
	 * WARNING: Don't forget to use a valid token.
	 */
	@Test
	@Ignore
	public void postLogWithCXFClient() throws Exception {
		String body = "{\"foo\": \"bar\"}";
		
		WebClient client = WebClient.create("https://oasis-demo.atolcd.com/");
		client.path("l/event");
		client.header("Content-Type", "application/json");
		client.header("Authorization", "Bearer " + token);
		
		Assert.assertEquals("Should post to the right url.", "https://oasis-demo.atolcd.com/l/event", client.getCurrentURI().toString());
		
		Response r = client.post(body);
		
		Assert.assertFalse("Sould not get 500 error", r.getStatus() == 500);
		Assert.assertTrue("Sould get 200 ?", r.getStatus() == 200);
		//System.out.println(r.getHeaders());
		
	}

	@Test
	public void postLogToMockServer() throws Exception {
		String body = "{\"foo\": \"bar\"}";
		
		WebClient client = WebClient.create("http://localhost:8282");
		client.path("l/event");
		client.header("Content-Type", "application/json");
		client.header("Authorization", "Bearer " + token);	
		Response r = client.post(body);
		
		Assert.assertEquals(200, r.getStatus());	
	}
}
