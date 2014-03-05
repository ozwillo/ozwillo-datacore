package org.oasis.datacore.rest.api.client;

import org.apache.cxf.jaxrs.client.WebClient;

import javax.ws.rs.core.Response;

import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.rest.api.client.AuditLogClientAPI.RemoteEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-client-test-context.xml" })
public class AuditLogEndpointTest {

   @Autowired
   private AuditLogClientAPI auditLogAPIClient;

   @Test
   public void postLogWithJAXRSClient() throws Exception {
      Map<String, Object> map = new HashMap<String, Object>();
      map.put("foo", "bar");

      RemoteEvent content = new RemoteEvent();
      content.time = Instant.now();
      content.log = map;

      Response res = auditLogAPIClient.json(content);
      Assert.assertTrue("Sould get 200 or 204", res.getStatus() == 200 || res.getStatus() == 204);
   }
   
   @Test
   public void postLogWithWebClient() throws Exception {
      long timestamp = System.currentTimeMillis();
      String body = "{\"time\": " + timestamp + ", \"log\": { \"foo\": \"bar\" } }";
	
      WebClient client = WebClient.create("https://oasis-demo.atolcd.com/");
      client.path("l/event");
      client.header("Content-Type", "application/json");
      Assert.assertEquals("Should post to the right url.", "https://oasis-demo.atolcd.com/l/event", client.getCurrentURI().toString());

      Response r = client.post(body);
      Assert.assertFalse("Sould not get 500 error", r.getStatus() == 500);
      Assert.assertTrue("Sould get 200 or 204", r.getStatus() == 200 || r.getStatus() == 204);
   }

   @Test
   public void postLogToMockServer() throws Exception {
	   long timestamp = System.currentTimeMillis();
	   String body = "{\"time\": " + timestamp + ", \"log\": { \"foo\": \"bar\" } }";

	   WebClient client = WebClient.create("http://localhost:8282");
	   client.path("l/event");
	   client.header("Content-Type", "application/json");

	   Response r = client.post(body);
	   Assert.assertEquals(200, r.getStatus());
   }

}
