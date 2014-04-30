package org.oasis.datacore.kernel.client;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.oasis.datacore.rest.api.DCResource;
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

   @Autowired
   private RiemannClientLog riemannClientLog;

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
         long reqArrival = (long) exchange.get("datacore.timestamp");
         Date now = new Date();
         long duration = now.getTime() - reqArrival;

         //Data from req
         Map<String, Object> data = (Map<String, Object>)exchange.get("reqContext");

         Map<String, String> fullData = convertAllDataToString(data);

         //Log Resource sent back in response if any
         if(logResContent) {
            MessageContentsList objs = MessageContentsList.getContentsList(serverOutResponseMessage);
            if(objs != null && objs.size() != 0) {
               Object resource = objs.get(0);
               if(resource instanceof Response) {
                  // result is already a response (built in server impl code, ex. to return custom status)
                  Response response = (Response) resource;
                  DCResource entity = (DCResource) response.getEntity();
                  if(entity != null) {
                     fullData.put("res.model", entity.getModelType());
                  }
               }
            }
         }

         //Data for res
         HttpHeaders resHeader = jaxrsApiProvider.getHttpHeaders();
         String status = serverOutResponseMessage.get(Message.RESPONSE_CODE).toString();
         fullData.put("statusCode", status);
         fullData.put("resHeaders", resHeader.toString());//TODO extract useful data to string

         //Determine which function has been used to provide the response
         String operationName = serverOutResponseMessage.getContextualProperty("org.apache.cxf.resource.operation.name").toString();
         fullData.put("operationName", operationName);

         //Information about user
         try {
            String user = datacoreSecurityService.getCurrentUserId();
            fullData.put("userId", user);
         } catch(Exception e) {

         }

         //TODO (un)parsed query
         //TODO Passing attributes to Riemann client as a Map is bugged but corrected so wait for the next release.
         riemannClientLog.sendFullDataEvent("TimeComputer", data.get("method").toString(), fullData, duration, "timestamp", "duration", "fullContext");
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
