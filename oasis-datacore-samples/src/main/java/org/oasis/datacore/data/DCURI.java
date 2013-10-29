package org.oasis.datacore.data;

/**
 * TODO NOT USED, replace by -core's
 * 
 * Datacore Resource URI, for now also works as Social Graph Resource URI.
 * TODO LATER refactor SCURI out of it for Social Graph.
 * 
 * @author mdutoo
 *
 */
public class DCURI {

   /** Container host ex. data.oasis-eu.org. Protocol is assumed to be HTTP
    * (if HTTPS, there must be a redirection) */
   private String container;
   /** Base type ex. city. Corresponds to a type of use and a data governance configuration.
    * For Social Graph ex. user (account), organization */
   // TODO alternate extending type SCURI with ex. "account" instead
   private String type;
   /** IRI ex. Lyon, London, Torino. Can't change (save by data migration operations).
    * For Social Graph ex. email */
   private String id;
   
   /**
    * @return http://[container]/type/[type]/id
    */
   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("http://"); // TODO constants
      sb.append(container);
      sb.append("/type/");
      sb.append(type);
      sb.append('/');
      sb.append(id);
      return sb.toString(); // TODO cache ?!?
   }
   
   public String getContainer() {
      return container;
   }
   public void setContainer(String container) {
      this.container = container;
   }
   public String getType() {
      return type;
   }
   public void setType(String type) {
      this.type = type;
   }
   public String getId() {
      return id;
   }
   public void setId(String id) {
      this.id = id;
   }
   
}
