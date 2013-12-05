package org.oasis.datacore.rest.server.event;

import java.util.Map;


/**
 * Simple all purpose event impl.
 * For better performance, use custom fields in dedicated impl rather than properties.
 * 
 * @author mdutoo
 *
 */
public class DCPropertiesEvent extends DCEvent {

   private Map<String, Object> properties;

   public DCPropertiesEvent(String type, String topic, Map<String,Object> properties) {
      super(type, topic);
      this.properties = properties;
   }

   public Map<String, Object> getProperties() {
      return properties;
   }

}
