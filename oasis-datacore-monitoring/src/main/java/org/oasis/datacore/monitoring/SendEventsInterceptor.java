package org.oasis.datacore.monitoring;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.springframework.beans.factory.annotation.Autowired;

public class SendEventsInterceptor extends AbstractPhaseInterceptor<Message> {
   
   @Autowired
   private SimpleRiemannExtractor simpleRiemannExtractor;
   
   public SendEventsInterceptor() {
      super(Phase.SEND_ENDING);
   }
   
   @Override
   public void handleMessage(Message serverOutResponseMessage) throws Fault {
      simpleRiemannExtractor.sendSimple();
   }

}
