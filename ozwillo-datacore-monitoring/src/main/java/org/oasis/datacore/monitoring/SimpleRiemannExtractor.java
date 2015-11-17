package org.oasis.datacore.monitoring;

import java.util.HashMap;
import java.util.Map;
import org.apache.cxf.message.Exchange;

public class SimpleRiemannExtractor extends RiemannExtractorBase {
   
   public void sendSimple() {
      Exchange exchange = getExchange();
      Map<String, String> data = new HashMap<String, String>();
      
      //Information about user
      try {
         data.put("dc.userId", exchange.get("dc.userId").toString());
      } catch(Exception e) {

      }
      
      data.put("dc.status", exchange.get("dc.status").toString());
      
      send(data, "simple");   
   }  

}
