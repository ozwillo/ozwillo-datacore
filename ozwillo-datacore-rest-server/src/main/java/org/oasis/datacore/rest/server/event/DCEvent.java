package org.oasis.datacore.rest.server.event;

public abstract class DCEvent {

   /** ex. resource.created
    * TODO rather Enum ?! using rather Object type ?? */
   private String type;
   /** Usually (Model or Mixin) type. If several topics, event must be copied. */
   private String topic;
   
   public DCEvent(String type, String topic) {
      if (type == null) {
         throw new NullPointerException("event type can't be null");
      }
      if (topic == null) {
         throw new NullPointerException("event topic can't be null");
      }
      this.type = type;
      this.topic = topic;
   }
   
   public String getType() {
      return type;
   }
   public String getTopic() {
      return topic;
   }
   /* for unmarshalling only
   public void setType(String type) {
      this.type = type;
   }*/
   /* for unmarshalling only
   public void setTopic(String topic) {
      this.topic = topic;
   }*/
   
   public String toString() {
      return toStringBuilder().toString();
   }
   
   protected StringBuilder toStringBuilder() {
      StringBuilder sb = new StringBuilder(this.getClass().getName());
      sb.append(' ');
      sb.append(type);
      sb.append(" on ");
      sb.append(topic);
      return sb;
   }
   
}
