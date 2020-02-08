package org.oasis.datacore.monitoring;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.oasis.datacore.rest.api.util.JaxrsApiProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

public class TimeComputerOutInterceptor extends AbstractPhaseInterceptor<Message> {

   @Autowired
   @Qualifier("datacore.cxfJaxrsApiProvider")
   protected JaxrsApiProvider jaxrsApiProvider;

   @Autowired
   @Qualifier("datacoreSecurityServiceImpl")
   private DatacoreSecurityService datacoreSecurityService;

   @Value("${dtMonitoring.monitorReqRes}")
   private boolean monitorReqRes;

   @Value("${dtMonitoring.logResContent}")
   private boolean logResContent;

   public TimeComputerOutInterceptor() {
      super(Phase.SEND);
   }

   @Override
   public void handleMessage(Message serverOutResponseMessage) throws Fault {
      Exchange exchange = serverOutResponseMessage.getExchange();

      if(monitorReqRes) {
         //Compute time taken
         long reqArrival = (long) exchange.get("dc.timestamp");
         Date now = new Date();
         long duration = now.getTime() - reqArrival;
         exchange.put("dc.duration", duration);

         //Data for res
         HttpHeaders resHeader = jaxrsApiProvider.getHttpHeaders();
         String status = serverOutResponseMessage.get(Message.RESPONSE_CODE).toString();
         exchange.put("dc.status", status);
         exchange.put("dc.res.headers", resHeader.toString());//TODO extract useful data to string

         //Determine which function has been used to provide the response
         String operationName = "" + serverOutResponseMessage.getContextualProperty("org.apache.cxf.resource.operation.name"); // NOT toString() which NPE when 404
         exchange.put("dc.operation", operationName);

         //Information about user
         try {
            String user = datacoreSecurityService.getCurrentUserId();
            exchange.put("dc.userId", user);
         } catch(Exception e) {

         }

      }
   }

   private Map<String, String> convertAllDataToString(Map<String, Object> map) {
      Map<String, String> data = new HashMap<String, String>();

      for(Map.Entry<String, Object> entry : map.entrySet()) {
         try {
            data.put(entry.getKey(), entry.getValue().toString());
         } catch(ClassCastException cce) {
            System.out.println("Error while casting Object into String.");
         }
      }

      return data;
   }

}
