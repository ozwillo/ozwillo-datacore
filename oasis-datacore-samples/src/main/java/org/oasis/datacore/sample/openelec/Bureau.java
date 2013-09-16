package org.oasis.datacore.sample.openelec;

import org.oasis.datacore.api.DCObject;
import org.oasis.datacore.api.DatacoreClient;
import org.oasis.datacore.sample.openmairie.Collectivite;
import org.oasis.datacore.sdk.data.spring.GenericEntity;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * see samples at http://bureaudevote.fr/bureaudevote69.htm
 * @author mdutoo
 *
 */
@Document(collection = "om.bureau")
public class Bureau extends GenericEntity<Bureau> {
   private static final long serialVersionUID = 669910323108902858L;
   
   private String code; // (poll id) var4 see http://www.openelec.org/documentation/doc-technique/folder.2005-10-21.6244356819/diagramdecoupage.png/view
   @Field("libelle") // keep names short http://stackoverflow.com/questions/5916080/what-are-naming-conventions-for-mongodb
   private String libelle_bureau; // (poll name)
   private String adresse1; // (address 1)
   private String adresse2; // (address 2)
   private String adresse3; // (address 3)
   private String code_canton; // (township code)
   //private String collectivite; // (city) originally embedded in OpenElec
   /** BEWARE @DBRef is bad for performance (requires other queries) ;
    * besides @DBRef is only meaningful before datacore integration */
   @DBRef
   private Collectivite collectivite; // (city)
   /** datacore integration - level 2 alt 1, NOT USED IN OpenElecServiceOasisBureauCityImpl, TODO rather in GenericDatacoreClientEntity ??? */
   private String collectivite_uri; // datacore integration

   /** datacore integration - level 1 alt 1, ONLY FOR OpenElecServiceOasisBureauCityImpl, TODO rather in GenericDatacoreClientEntity ??? */
   @Transient // because Bureau still stored locally
   private DCObject dcObject;
   /** datacore integration - level 3 alt 1, ONLY FOR OpenElecServiceOasisBureauCityImpl, TODO rather in GenericDatacoreClientEntity ??? */
   private String uri;
   /** datacore integration - level 3 ; TODO rather in GenericDatacoreClientEntity or itf or builder ?? */
   public DCObject toDCObject() {
      DCObject dcObject = new DCObject(this.dcObject); // clones if not new (i.e. already one)
      if (this.dcObject == null) {
         String rdfTypeOrBase = "bureau";
         dcObject.put("uri", DatacoreClient.DATACORE_BASE_URI + rdfTypeOrBase + "/" + this.getCode());
         dcObject.put("type", rdfTypeOrBase);
      }
      dcObject.put("code", this.getCode());
      // TODO source (id ???), version (& audit ?)...
      dcObject.put("libelle_bureau", this.getLibelle_bureau());
      dcObject.put("adresse", this.getAdresse1() + "\n"
         + this.getAdresse2() + "\n"
         + this.getAdresse3()); // TODO field mapping
      //dcObject.put("code_canton", "canton1"); // said not in datacore bureau metamodel
      if (this.getCollectivite() != null) {
         dcObject.put("collectivite", this.getCollectivite().getDcObject().getUri()); // datacore ref (uri)
      }
      return dcObject;
   }
   /** datacore integration - level 3 ;
    * NB. doesn't load refs ex. collectivite (done in service) TODO or rather here in fromDCObject(DCObject, DatacoreClient) ??? ;
    * if new rather new Bureau().fromDCObject() than static ; TODO rather in GenericDatacoreClientEntity or itf or builder ? */
   public void fromDCObject(DCObject bureauDCObject) {
      this.setUri(bureauDCObject.getUri());
      this.setDcObject(dcObject); // TODO or at end to avoid if error ?

      // set fields : TODO reflexive
      this.setCode((String) bureauDCObject.get("code"));
      this.setLibelle_bureau((String) bureauDCObject.get("libelle_bureau"));
      // "manual" custom mapping of address
      String address = (String) bureauDCObject.get("address");
      if (address != null) {
         String[] addressFields = address.split("\n");
         this.setAdresse1(addressFields[0]);
         if (address.length() > 0) {
            this.setAdresse2(addressFields[1]);
            if (address.length() > 1) {
               this.setAdresse3(addressFields[2]);
            }
         }
      }
      //bureau.setCode_canton("canton1"); // said not in datacore bureau metamodel
      this.setLibelle_bureau((String) bureauDCObject.get("libelle_bureau"));
   }
   
   public String getCode() {
      return code;
   }
   public void setCode(String code) {
      this.code = code;
   }
   public String getLibelle_bureau() {
      return libelle_bureau;
   }
   public void setLibelle_bureau(String libelle_bureau) {
      this.libelle_bureau = libelle_bureau;
   }
   public String getAdresse1() {
      return adresse1;
   }
   public void setAdresse1(String adresse1) {
      this.adresse1 = adresse1;
   }
   public String getAdresse2() {
      return adresse2;
   }
   public void setAdresse2(String adresse2) {
      this.adresse2 = adresse2;
   }
   public String getAdresse3() {
      return adresse3;
   }
   public void setAdresse3(String adresse3) {
      this.adresse3 = adresse3;
   }
   public String getCode_canton() {
      return code_canton;
   }
   public void setCode_canton(String code_canton) {
      this.code_canton = code_canton;
   }
   public Collectivite getCollectivite() {
      return collectivite;
   }
   public void setCollectivite(Collectivite collectivite) {
      this.collectivite = collectivite;
   }
   public String getCollectivite_uri() {
      return collectivite_uri;
   }
   public void setCollectivite_uri(String collectivite_uri) {
      this.collectivite_uri = collectivite_uri;
   }
   public void setDcObject(DCObject dcObject) {
      this.dcObject = dcObject;
   }
   public DCObject getDcObject() {
      return dcObject;
   }
   public String getUri() {
      return uri;
   }
   public void setUri(String uri) {
      this.uri = uri;
   }
   
}
