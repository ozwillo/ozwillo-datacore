package org.oasis.datacore.monitoring;

import java.io.IOException;

import com.aphyr.riemann.client.RiemannClient;

public class RiemannDisconnect implements Runnable {
   
   private RiemannClient client;
   
   public RiemannDisconnect(RiemannClient c) {
      client = c;
   }

   @Override
   public void run() {
      try {
         client.disconnect();
      } catch (IOException e) {
         //e.printStackTrace();
      }
   }
   
}
