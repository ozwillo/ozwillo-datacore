package org.oasis.datacore.monitoring;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import com.aphyr.riemann.client.RiemannClient;

public class RiemannClientLog implements InitializingBean {

   @Value("${riemann.serverIP}")
   private String riemannIP;

   @Value("${riemann.serverPort}")
   private int riemannPort;

	private RiemannClient client;

   private static final Logger logger = LoggerFactory.getLogger(RiemannClientLog.class);

   @Override
   public void afterPropertiesSet() throws Exception {
      try {
         //UDP is bugged in 0.2.10, rather use TCP for now
         //client = RiemannClient.udp(riemannIP, riemannPort);
         client = RiemannClient.tcp(riemannIP, riemannPort);
         client.connect();

         //Register a shutdown hook to disconnect Riemann client cleanly
         Runtime runtime = Runtime.getRuntime();
         Thread thread = new Thread(new RiemannDisconnect(client));
         runtime.addShutdownHook(thread);
      } catch(Exception e) {
         logger.error("Unable to start RiemannClient.");
      }
   }

	public void sendEvent(String service, String desc, String... tags) {
		try {
      	client.event().
      	  service(service).
      	  state("running").
      	  description(desc).
      	  metric(1).
      	  tags(tags).
      	  ttl(30).
      	  send();
      	//client.disconnect();
		} catch(Exception e) {
			
		}
	}
	
	public void sendTimeEvent(String service, String desc, long time, String... tags) {
      try {
         client.event().
           service(service).
           state("running").
           description(desc).
           metric(time).
           tags(tags).
           ttl(30).
           send();
         //client.disconnect();
      } catch(Exception e) {
         logger.error("******************ERROR*************");
      }
   }

	public void sendFullDataEvent(String service, String desc, Map<String, String> data, long time, String... tags) {
      try {
         client.event().
           service(service).
           state("running").
           description(desc).
           attributes(data).
           metric(time).
           tags(tags).
           ttl(30).
           send();
         //client.disconnect();
      } catch(Exception e) {
         logger.error("Unable to send all data to Riemann : " + e);
      }
   }

}
