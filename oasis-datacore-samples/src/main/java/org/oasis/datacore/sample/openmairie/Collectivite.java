package org.oasis.datacore.sample.openmairie;

import org.oasis.datacore.api.DCObject;
import org.oasis.datacore.api.DatacoreClient;
import org.oasis.datacore.sdk.data.spring.GenericEntity;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "om.collectivite")
public class Collectivite extends GenericEntity<Collectivite> {
   private static final long serialVersionUID = 3610494701392647570L;
   
   public static final String NIVEAU_COM = "com";
   public static final String NIVEAU_COMCOM = "comcom";
   
   //private String om_collectivite; // (ID) // originally technical autoincr id
   private String libelle; // (city name)
   private String niveau; // (hierarchy : comcom, com) SAID NOT IN DATACORE NOR LOCALLY AFTER MIGRATION (DROPPED) 

   /** datacore integration - level 1 alt 1, TODO rather in GenericDatacoreClientEntity ??? */
   private DCObject dcObject; // existing datacore dcObject ; NB. not @Transient because Collectivite not stored locally anymore
   /** datacore integration - level 1 ; TODO rather in GenericDatacoreClientEntity or itf or builder ?? */
   public DCObject toDCObject() {
      DCObject dcObject = new DCObject(this.dcObject); // base ; clones if not new (i.e. already one in datacore)
      if (this.dcObject == null) {
         // new (because else would have gotten from datacore)
         String rdfTypeOrBase = "city";
         dcObject.put("uri", DatacoreClient.DATACORE_BASE_URI + rdfTypeOrBase + "/France/" + this.getLibelle());
         dcObject.put("type", rdfTypeOrBase);
      }
      dcObject.put("name", this.getLibelle());
      //dcObject.put("country", "France"); // NOT IN BUSINESS MODEL so should stay unchanged as in this.dcObject
      return dcObject;
   }
   /** datacore integration - level 1 ;
    * (NB. wouldn't load refs (done in service) TODO or rather here in fromDCObject(DCObject, DatacoreClient) ???) ;
    * rather new Collectivite().fromDCObject() than static ; TODO rather in GenericDatacoreClientEntity or itf or builder ? */
   public void fromDCObject(DCObject cityDCObject) {
      // set fields : TODO reflexive
      this.setLibelle((String) cityDCObject.get("name"));
      // ...
   }
   
   public String getOm_collectivite() {
      return this.getId();
   }
   public void setOm_collectivite(String om_collectivite) {
      this.setId(om_collectivite);
   }
   public String getLibelle() {
      return libelle;
   }
   public void setLibelle(String libelle) {
      this.libelle = libelle;
   }
   public String getNiveau() {
      return niveau;
   }
   public void setNiveau(String niveau) {
      this.niveau = niveau;
   }
   public void setDcObject(DCObject dcObject) {
      this.dcObject = dcObject;
   }
   public DCObject getDcObject() {
      return dcObject;
   }
   
}
