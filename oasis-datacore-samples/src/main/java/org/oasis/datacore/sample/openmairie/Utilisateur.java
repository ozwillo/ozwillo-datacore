package org.oasis.datacore.sample.openmairie;

import org.oasis.datacore.sdk.data.spring.GenericEntity;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * At some point should migrate in the social graph, but for now
 * rather only looking at how to put it in datacore
 * 
 * @author mdutoo
 *
 */
@Document(collection = "om.utilisateur")
public class Utilisateur extends GenericEntity<Utilisateur> {
   private static final long serialVersionUID = -2349703104550484398L;
   
   //private String om_utilisateur; // (user id) originally technical autoincr id
   private String nom; // (full name)
   private String email;
   private String login;
   private String pwd; // LATER Ozwillo auth
   private String om_profil; // (access right level) LATER Ozwillo rights
   private String om_collectivite; // (city) TODO ref in datacore
   
   public String getOm_utilisateur() {
      return getId();
   }
   public void setOm_utilisateur(String om_utilisateur) {
      this.setId(om_utilisateur);
   }
   public String getNom() {
      return nom;
   }
   public void setNom(String nom) {
      this.nom = nom;
   }
   public String getEmail() {
      return email;
   }
   public void setEmail(String email) {
      this.email = email;
   }
   public String getLogin() {
      return login;
   }
   public void setLogin(String login) {
      this.login = login;
   }
   public String getPwd() {
      return pwd;
   }
   public void setPwd(String pwd) {
      this.pwd = pwd;
   }
   public String getOm_profil() {
      return om_profil;
   }
   public void setOm_profil(String om_profil) {
      this.om_profil = om_profil;
   }
   public String getOm_collectivite() {
      return om_collectivite;
   }
   public void setOm_collectivite(String om_collectivite) {
      this.om_collectivite = om_collectivite;
   }
   
}
