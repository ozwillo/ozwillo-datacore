package org.oasis.datacore.kernel.client;

import java.util.Date;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.springframework.beans.factory.annotation.Autowired;

public class TimeComputerOutInterceptor extends AbstractPhaseInterceptor<Message> {
   
   @Autowired
   private RiemannClientLog riemannClientLog;

   public TimeComputerOutInterceptor() {
      super(Phase.SEND);
   }
   
   @Override
   public void handleMessage(Message serverOutRequestMessage) throws Fault {    
      long test = (long) serverOutRequestMessage.getExchange().get("datacore.timestamp");
      String method = (String) serverOutRequestMessage.getExchange().get("datacore.req");
      
      Date now = new Date();
      long duration = now.getTime() - (long) test;
      System.out.println(duration);
      //TODO: Send it to Riemann
      
      riemannClientLog.sendTimeEvent("TimeComputer", method, duration, "timestamp", "duration");
   }
   
}
