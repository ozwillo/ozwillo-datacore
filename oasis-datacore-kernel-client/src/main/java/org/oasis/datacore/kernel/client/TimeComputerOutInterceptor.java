package org.oasis.datacore.kernel.client;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.oasis.datacore.core.security.DCUserImpl;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.oasis.datacore.rest.api.util.JaxrsApiProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class TimeComputerOutInterceptor extends AbstractPhaseInterceptor<Message> {
   
   @Autowired
   @Qualifier("datacore.cxfJaxrsApiProvider")
   protected JaxrsApiProvider jaxrsApiProvider;

   @Autowired
   @Qualifier("datacoreSecurityServiceImpl")
   private DatacoreSecurityService datacoreSecurityService;

   @Autowired
   private RiemannClientLog riemannClientLog;

   public TimeComputerOutInterceptor() {
      super(Phase.SEND);
   }
   
   @Override
   public void handleMessage(Message serverOutRequestMessage) throws Fault {
      Exchange exchange = serverOutRequestMessage.getExchange();
      
      //Compute time taken
      long test = (long) exchange.get("datacore.timestamp");
      Date now = new Date();
      long duration = now.getTime() - (long) test;
      System.out.println(duration);
      
      //Data from req
      Map<String, Object> data = (Map<String, Object>)exchange.get("reqContext");

      Map<String, String> fullData = convertAllDataToString(data);

      //Data for res
      HttpHeaders resHeader = jaxrsApiProvider.getHttpHeaders();
      String status = serverOutRequestMessage.get(Message.RESPONSE_CODE).toString();

      //Determine which function has been used to provide the response
      String operationName = serverOutRequestMessage.getContextualProperty("org.apache.cxf.resource.operation.name").toString();

      //Information about the user
      try {
         DCUserImpl user = datacoreSecurityService.getCurrentUser();
      } catch(Exception e) {

      }

      fullData.put("statusCode", status);
      fullData.put("resHeaders", resHeader.toString());//TODO parsetostring
      fullData.put("operationName", operationName);

      //TODO resource(s) in & out, (un)parsed query
      //TODO Passing attributes to Riemann client as a Map is bugged but corrected so wait for the next release.
      riemannClientLog.sendFullDataEvent("TimeComputer", data.get("method").toString(), fullData, duration, "timestamp", "duration", "fullContext");
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
