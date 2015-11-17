package org.oasis.datacore.monitoring;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.apache.cxf.message.Exchange;

public class ConfigurableRiemannExtractor extends RiemannExtractorBase {
   public void sendConfigurable() {
      Exchange exchange = getExchange();
      Map<String, String> data = new HashMap<String, String>();
      
      throw new NotImplementedException();
      
      //send(data, "configurable");
   }
}
