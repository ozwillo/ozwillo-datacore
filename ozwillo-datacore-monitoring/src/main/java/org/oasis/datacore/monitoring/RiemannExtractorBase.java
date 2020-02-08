package org.oasis.datacore.monitoring;

import java.util.Map;

import org.apache.cxf.message.Exchange;
import org.oasis.datacore.rest.server.cxf.CxfJaxrsApiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import io.riemann.riemann.client.RiemannClient;

/*
 * List of available information available in cxf exchange:
 * dc.duration
 * dc.status
 * dc.operation
 * dc.userId
 * dc.uri
 * dc.query
 * dc.method
 * dc.req.headers
 * dc.req.model
 * dc.res.headers
 * dc.res.model
 */

abstract class RiemannExtractorBase implements InitializingBean {
   
   @Value("${dtMonitoring.monitorReqRes}")
   protected boolean monitorReqRes;

   @Value("${dtMonitoring.logResContent}")
   protected boolean logResContent;

   @Value("${riemann.serverIP}")
   private String riemannIP;

   @Value("${riemann.serverPort}")
   private int riemannPort;

   private RiemannClient client;

   private static final Logger logger = LoggerFactory.getLogger(RiemannExtractorBase.class);
   
   @Autowired
   private CxfJaxrsApiProvider cxfJaxrsApiProvider;

   @Override
   public void afterPropertiesSet() throws Exception {
      try {
         //UDP is bugged in 0.2.10, rather use TCP for now
         //client = RiemannClient.udp(riemannIP, riemannPort);
         client = RiemannClient.tcp(riemannIP, riemannPort);
         client.connect();
      } catch(Exception e) {
         logger.error("Unable to start RiemannClient.");
      }
   }

   /*
    * Disconnect client.
    * Ensure Riemann Client stops when shutting down datacore.
    * Need destroy-method="disconnect" in bean definition.
    */
   public void disconnect() {
      client.close();
   }

   /*
    * Send a simple event to Riemann containing minimal information :
    */
   public void send() {
      Exchange exchange = getExchange();    
      long time = (long) exchange.get("dc.duration");
      String desc = exchange.get("dc.method").toString();
      String tags = "duration";
            
      try {
         client.event().
           service("dc").
           state("running").
           description(desc).
           metric(time).
           tags(tags).
           ttl(30).
           send();
         //client.disconnect();
      } catch(Exception e) {
         logger.error("Unable to send default data to Riemann : " + e);
      }
   }
   
   public void send(Map<String, String> data, String... tags) {
      Exchange exchange = getExchange();
      
      long time = (long) exchange.get("dc.duration");
      String desc = (String) exchange.get("dc.method");
      if (desc == null) {
         // happens when went wrong ex. body not well-formed JSON
         return;
      }
      String model;
      Object modelObject = exchange.get("dc.req.model");
      if (modelObject != null) {
         model = modelObject.toString();
      } else {
         model = "dcModelNotFound"; // happens when ex. bad containerUrl : http://data.ozwillo.comdc/type...
      }
            
      try {
         client.event().
           service(model).
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
   
   public Exchange getExchange() {
     return cxfJaxrsApiProvider.getExchange();  
   }

}
