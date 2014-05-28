package org.oasis.datacore.monitoring;

import java.util.Date;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class TimeTaggerInInterceptor extends AbstractPhaseInterceptor<Message> {
   
   public TimeTaggerInInterceptor() {
      super(Phase.RECEIVE);
   }
   
   @Override
   public void handleMessage(Message serverInRequestMessage) throws Fault {    
      Date now = new Date();
      serverInRequestMessage.getExchange().put("dc.timestamp", now.getTime());
   }
   
}
