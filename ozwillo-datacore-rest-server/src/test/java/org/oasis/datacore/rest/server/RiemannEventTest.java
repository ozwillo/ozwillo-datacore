package org.oasis.datacore.rest.server;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.riemann.riemann.client.RiemannClient;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })
public class RiemannEventTest {
	/*
	 * WARNING: Riemann should be listening on port 5555
	 * @Ignore
	 */
	@Test
	@Ignore
	public void sendSimpleEventToRiemann() {
		RiemannClient c;
		try {
			c = RiemannClient.tcp("127.0.0.1", 5555);
			c.connect();
			c.event().
			  service("queryLogging").
			  state("running").
			  description("?name=Lyon").
			  metric(1).
			  tags("query").
			  ttl(30).
			  send();
			
			c.event().
			  service("queryLogging").
			  state("running").
			  description("?name=Lyon").
			  metric(1).
			  tags("query").
			  ttl(30).
			  send();
			
			c.event().
			  service("queryLogging").
			  state("running").
			  description("?name=Paris").
			  metric(1).
			  tags("query").
			  ttl(30).
			  send();

			//c.query("tagged \"cold\" and metric > 0"); // => List<Event>;
			c.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
