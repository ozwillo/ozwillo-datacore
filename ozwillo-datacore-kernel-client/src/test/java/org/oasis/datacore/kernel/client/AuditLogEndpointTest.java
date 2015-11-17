package org.oasis.datacore.kernel.client;

import org.apache.cxf.jaxrs.client.WebClient;

import javax.ws.rs.core.Response;

import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.kernel.client.AuditLogClientAPI;
import org.oasis.datacore.kernel.client.AuditLogClientAPI.RemoteEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-kernel-client-test-context.xml" })
public class AuditLogEndpointTest {

   @Autowired
   private AuditLogClientAPI auditLogAPIClient;

   @Value("${kernel.baseUrl}")
   private String kernelBaseUrl;

   @Ignore // TODO re-enable once auth OK again
   @Test
   public void postLogWithJAXRSClient() throws Exception {
      Map<String, Object> map = new HashMap<String, Object>();
      map.put("foo", "bar");

      RemoteEvent content = new RemoteEvent(Instant.now(), map);

      Response res = auditLogAPIClient.json(content);
      Assert.assertTrue("Sould get 200 or 204", res.getStatus() == 200 || res.getStatus() == 204);
   }

   @Ignore // TODO re-enable once auth OK again
   @Test
   public void postLogWithWebClient() throws Exception {
      long timestamp = System.currentTimeMillis();
      String body = "{\"time\": " + timestamp + ", \"log\": { \"foo\": \"bar\" } }";
	
      WebClient client = WebClient.create(kernelBaseUrl);
      client.path("l/event");
      client.header("Content-Type", "application/json");
      Assert.assertEquals("Should post to the right url.", kernelBaseUrl + "l/event", client.getCurrentURI().toString());

      Response r = client.post(body);
      Assert.assertFalse("Sould not get 500 error", r.getStatus() == 500);
      Assert.assertFalse("Sould not get 401 error", r.getStatus() == 401);
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
