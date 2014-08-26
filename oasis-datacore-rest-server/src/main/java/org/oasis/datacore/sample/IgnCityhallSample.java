package org.oasis.datacore.sample;

import java.util.Arrays;
import java.util.List;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.server.event.DCInitIdEventListener;
import org.springframework.stereotype.Component;


/**
 * Used by tests & demo.
 * 
 * TODO naming good practices :
 * * prefix model (& mixin) type by business domain,
 * and overriden / copied / inherited business domain as well if any
 * * prefix field name by main model if it appears in several as copies
 * (and vice versa : non-prefixed ones won't be able to be copied)
 * 
 * @author mdutoo
 *
 */
@Component
public class IgnCityhallSample extends DatacoreSampleBase {

   // .bdparcellaire
   public static final String IGN_COMMUNE = "ign.commune"; // SCH OUI ET COMCOM
   public static final String IGN_DEPARTEMENT = "ign.departement"; // SCH OUI
   public static final String IGN_CANTON = "ign.canton"; // SCH NON
   public static final String IGN_ARRONDISSEMENT = "ign.arrondissement"; // SCH NON
   public static final String IGN_DIVISION = "ign.division"; // Cadastrale
   public static final String IGN_LOCALISANT = "ign.localisant"; // Parcellaire
   public static final String IGN_PARCELLE = "ign.parcelle";
   public static final String IGN_BATIMENT = "ign.batiment";
   public static final String IGN_PLAQUE_ADDRESSE = "ign.plaqueAdresse";
   
   public static final String CITYHALL_IGN_PARCELLE = "cityhall.ign.parcelle";


   @Override
   public void buildModels(List<DCModelBase> modelsToCreate) {
      this.buildModelsIgn(modelsToCreate);
      //this.buildModelsCityhallIgnV1Mixin(modelsToCreate); // ONLY IN TESTS
      //this.buildModelsCityhallIgnV2Inheritance(modelsToCreate); // ONLY IN TESTS
   }

   @Override
   public void fillData() {
      this.fillDataIgn();
   }

   public void buildModelsIgn(List<DCModelBase> modelsToCreate) {
      // because IGN Model governance say so (but this could be forbidden by OASIS) :
      // field names are in French 
      // all fields are required
      
      DCModel ignCommuneModel = (DCModel) new DCModel(IGN_COMMUNE) // rgc., pdp.
      /*ignCommuneModel.setDocumentation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         ///.addField(new DCField("name", "string", true, 100)) // TODO cityCountry's ??!!
         .addField(new DCField("departementCode", "int", true, 100)) // DEP
         .addField(new DCResourceField("departement", IGN_DEPARTEMENT, true, 100))
         .addField(new DCField("communeCode", "int", true, 100)) // COM ; code, commune ?
         .addField(new DCField("arrondissementCode", "int", true, 100)) // ARRD
         .addField(new DCField("cantonCode", "int", true, 100)) // CANT
         .addField(new DCField("statutAdministratif", "int", true, 100)) // ADMI
         .addField(new DCField("population", "int", true, 0)) // POPU ; query ?
         .addField(new DCField("surface", "int", true, 0)) // SURFACE ; query ?
         .addField(new DCField("communeNom", "string", true, 100)) // NOM ; com.nom ? ; majuscules ; cityCountry's ??
         .addField(new DCField("longitudeDMS", "int", true, 100)) // LONGI_DMS
         .addField(new DCField("latitudeDMS", "int", true, 100)); // LATI_DMS

      DCModel ignDepartementModel = (DCModel) new DCModel(IGN_DEPARTEMENT) // rgc., pdp.
         // listener alt 1 TODO still register it !! :
         //.addListener(new DCInitIdEventListener(IGN_DEPARTEMENT/**/, "departementCode")) 
         .addField(new DCField("nom", "string", true, 100)) // DEP ; dep.nom ?
         .addField(new DCField("departementCode", "int", true, 100)); // DEP
      // listener alt 2 :
      eventService.init(new DCInitIdEventListener(IGN_DEPARTEMENT, "departementCode")); // TODO beware model not yet added...

      DCModel ignCantonModel = (DCModel) new DCModel(IGN_CANTON) // rgc.
         .addField(new DCField("nom", "string", true, 100)) // ?? ; dep.nom ?
         .addField(new DCField("departementCode", "int", true, 100)) // DEP
         .addField(new DCField("arrondissementCode", "int", true, 100)) // ARRD
         .addField(new DCField("cantonCode", "int", true, 100)); // CANT

      DCModel ignArrondissementModel = (DCModel) new DCModel(IGN_ARRONDISSEMENT) // rgc., bdparcellaire.
         .addField(new DCResourceField("commune", IGN_COMMUNE, true, 100))
         .addField(new DCField("departementCode", "int", true, 100)) // DEP ??
         .addField(new DCField("arrondissementCode", "int", true, 100)) // ARRD
         .addField(new DCField("communeAbsorbeeCode", "int", true, 100)) // ??
         .addField(new DCField("arrondissementNom", "string", true, 100)); // specifique
      
      DCModel ignDivisionModel = (DCModel) new DCModel(IGN_DIVISION) // bdparcellaire. ; cadastrale
         .addField(new DCField("departementCode", "int", true, 100))
         .addField(new DCResourceField("commune", IGN_COMMUNE, true, 100))
         .addField(new DCField("communeCode", "int", true, 100)) // COM ; code, commune ?
         .addField(new DCField("communeNom", "string", true, 100)) // NOM ; com.nom ? ; majuscules ; cityCountry's ??
         .addField(new DCField("arrondissementCode", "int", true, 100)) // ARRD
         .addField(new DCField("communeAbsorbeeCode", "int", true, 100))
         .addField(new DCField("sectionCadastrale", "string", true, 100)) // section
         .addField(new DCField("feuilleCadastrale", "int", true, 100)) // feuille
         .addField(new DCListField("echellePlan", new DCField("listField", "int"))) // specifique
         // NB. can be 250, 500, 625, 1000, 1250, 2000, 2500, 4000, 5000, 8000, 10000, 15000, 20000
         .addField(new DCField("numeroEdition", "int", true, 100)); // specifique
      
      DCModel ignLocalisantModel = (DCModel) new DCModel(IGN_LOCALISANT) // bdparcellaire. ; parcellaire
         .addField(new DCField("departementCode", "int", true, 100))
         .addField(new DCResourceField("commune", IGN_COMMUNE, true, 100))
         .addField(new DCField("communeCode", "int", true, 100)) // COM ; code, commune ?
         .addField(new DCField("communeNom", "string", true, 100)) // NOM ; com.nom ? ; majuscules ; cityCountry's ??
         .addField(new DCField("arrondissementCode", "int", true, 100)) // ARRD ??
         .addField(new DCField("communeAbsorbeeCode", "int", true, 100))
         .addField(new DCField("sectionCadastrale", "string", true, 100)) // section
         .addField(new DCField("feuilleCadastrale", "int", true, 100)) // feuille
         .addField(new DCField("numeroParcelle", "int", true, 100)); // numero

      DCModel ignParcelleModel = (DCModel) new DCModel(IGN_PARCELLE) // bdparcellaire.
         .addField(new DCField("departementCode", "int", true, 100))
         .addField(new DCResourceField("commune", IGN_COMMUNE, true, 100))
         .addField(new DCField("communeCode", "int", true, 100)) // COM ; code, commune ?
         .addField(new DCField("communeNom", "string", true, 100)) // NOM ; com.nom ? ; majuscules ; cityCountry's ??
         .addField(new DCField("arrondissementCode", "int", true, 100)) // ARRD ??
         .addField(new DCField("communeAbsorbeeCode", "int", true, 100))
         .addField(new DCField("sectionCadastrale", "string", true, 100)) // section
         .addField(new DCField("feuilleCadastrale", "int", true, 100)) // feuille
         .addField(new DCField("numeroParcelle", "int", true, 100)); // numero

      DCModel ignBatimentModel = (DCModel) new DCModel(IGN_BATIMENT) // bdparcellaire.
         .addField(new DCField("type", "string", true, 100)); // (construction) en dur, légère
      //DCModel ignImageModel = new DCModel(IGN_IMAGE); // bdparcellaire.
      //DCModel ignPlaqueAddresseModel = new DCModel(IGN_PLAQUE_ADDRESSE); // bdparcellaire. ??!!
      
      modelsToCreate.addAll(Arrays.asList((DCModelBase) ignCommuneModel, ignDepartementModel, ignArrondissementModel, ignCantonModel,
         ignParcelleModel, ignDivisionModel, ignLocalisantModel, ignBatimentModel));
   }

   public void fillDataIgn() {
      //DCResource departement01 = resourceService.create(IGN_DEPARTEMENT, "01")
      DCResource departement01 = DCResource.create(null, IGN_DEPARTEMENT)
            .set("nom", "AIN").set("departementCode", 01);
      DCResource communeLaiz = resourceService.create(IGN_COMMUNE, "203")
            .set("communeNom", "LAIZ").set("arrondissementCode", 2).set("communeCode", 203).set("cantonCode", 27)
            .set("departementCode", 01).set("departement", departement01.getUri())
            .set("statutAdministratif", 6).set("population", 11).set("surface", 1031)
            .set("longitudeDMS", +45320).set("latitudeDMS", 461454);
      DCResource communePeron = resourceService.create(IGN_COMMUNE, "288")
            .set("communeNom", "PERON").set("arrondissementCode", 3).set("communeCode", 288).set("cantonCode", 12)
            .set("departementCode", 01).set("departement", departement01.getUri())
            .set("statutAdministratif", 6).set("population", 20).set("surface", 2601)
            .set("longitudeDMS", +55535).set("latitudeDMS", 461124);
      
      departement01 = /*datacoreApiClient.*/postDataInType(departement01);
      communeLaiz = /*datacoreApiClient.*/postDataInType(communeLaiz);
      communePeron = /*datacoreApiClient.*/postDataInType(communePeron);

      DCResource departement72 = resourceService.create(IGN_DEPARTEMENT, "72")
            .set("nom", "").set("departementCode", 72);
      DCResource communeBousse = resourceService.create(IGN_COMMUNE, "044")
            .set("communeNom", "BOUSSE").set("arrondissementCode", 1).set("communeCode", 044).set("cantonCode", 17)
            .set("departementCode", 72).set("departement", departement72.getUri())
            .set("statutAdministratif", 6).set("population", 4).set("surface", 1202)
            .set("longitudeDMS", -00332).set("latitudeDMS", 474613);
      DCResource parcelleBousse389arrdt = resourceService.create(IGN_ARRONDISSEMENT, "04401")
            .set("commune", communeBousse.getUri()).set("arrondissementCode", 1)
            .set("communeAbsorbeeCode", 0).set("arrondissementNom", "1er arrondissement"); // 000
      copy(parcelleBousse389arrdt, departement72);
      DCResource parcelleBousse389 = resourceService.create(IGN_PARCELLE, "0389").set("numeroParcelle", 389)  // ?
            .set("sectionCadastrale", "0B").set("feuilleCadastrale", 1);
      /*      .set("departementCode", 01).set("commune", communeLaiz.getUri())
            .set("communeCode", communeLaiz.get("communeCode")).set("communeNom", "LAIZ")
            .set("arrondissementCode",)
      .addField(new DCField("communeAbsorbeeCode", "int", true, 100))
      .addField(new DCField("sectionCadastrale", "string", true, 100)) // section
      .addField(new DCField("feuilleCadastrale", "int", true, 100)) // feuille
      .addField(new DCField("numeroParcelle", "int", true, 100)); // numero*/
      copy(parcelleBousse389, communeBousse);
      copy(parcelleBousse389, parcelleBousse389arrdt);
      departement72 = /*datacoreApiClient.*/postDataInType(departement72);
      communeBousse = /*datacoreApiClient.*/postDataInType(communeBousse);
      parcelleBousse389arrdt = /*datacoreApiClient.*/postDataInType(parcelleBousse389arrdt);
      parcelleBousse389 = /*datacoreApiClient.*/postDataInType(parcelleBousse389);
   }
   
   public void buildModelsCityhallIgnV1Mixin(List<DCModelBase> modelsToCreate) {
      DCModel ignParcelleModel = (DCModel) getCreatedModel(IgnCityhallSample.IGN_PARCELLE, modelsToCreate);
      
      // IGN patched by cityhalls - v1 (using Mixin, so in same collection)
      DCMixin cityhallIgnParcelleMixin = (DCMixin) new DCMixin(CITYHALL_IGN_PARCELLE) // bdparcellaire.
         .addField(new DCField("cityhall.numeroParcelle", "int", true, 100)) // al
         .addField(new DCField("numeroParcelle", "long", true, 101)); // alt2 override
      // (of def ?? or only of value, or auto ?) TODO check that original field is writable (or in another model)
      ///modelAdminService.addMixin(cityhallIgnParcelleMixin); // LATER ?!
            // numeroParcelle ?? => TODO PREFIXES !!!! OR prefix using declaring Model / Mixin ?!
      ignParcelleModel.addMixin(cityhallIgnParcelleMixin); // NB. doesn't change the Model version ?!?!?!
      ///modelAdminService.addModel(ignParcelleModel); // LATER re-add...
      
      modelsToCreate.addAll(Arrays.asList(ignParcelleModel, cityhallIgnParcelleMixin));
   }
   
   public void buildModelsCityhallIgnV2Inheritance(List<DCModelBase> modelsToCreate) {
      DCModel ignParcelleModel = (DCModel) getCreatedModel(IgnCityhallSample.IGN_PARCELLE, modelsToCreate);
      
      // IGN patched by cityhalls - v2 (using inheritance, so in separate collection)
      ///DCModel cityhallIgnParcelleModel = new DCModel("cityhall.ign.parcelle", ignParcelleModel.getName()); // bdparcellaire.
      DCModel cityhallIgnParcelleModel = (DCModel) new DCModel(CITYHALL_IGN_PARCELLE)
         .addMixin(ignParcelleModel) // inheritance = parent model (copy) as mixin NOO OPPOSITE OVERRIDE
         ///.addMixin(ignParcelleModel) // LATER ??
         .addField(new DCField("cityhall.numeroParcelle", "int", true, 100)) // alt1 new field along the existing one
         .addField(new DCField("numeroParcelle", "long", true, 102)); // alt2 override
         // (of def ?? or only of value, or auto ?) TODO check that original field is writable (or in another model)
      
      modelsToCreate.addAll(Arrays.asList(new DCModelBase[] { cityhallIgnParcelleModel }));
   }
   
}
