package org.oasis.datacore.monitoring;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class SendEventsInterceptor extends AbstractPhaseInterceptor<Message> {
   
   @Value("${dtMonitoring.monitorReqRes}")
   protected boolean sendEvents;
   
   @Autowired
   private SimpleRiemannExtractor simpleRiemannExtractor;
   
   public SendEventsInterceptor() {
      super(Phase.SEND_ENDING);
   }
   
   @Override
   public void handleMessage(Message serverOutResponseMessage) throws Fault {
      if(sendEvents) {
        simpleRiemannExtractor.sendSimple();
      }
   }

}
