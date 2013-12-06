package org.oasis.datacore.sample;

import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.client.DatacoreClientApi;
import org.oasis.datacore.rest.server.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;


/**
 * Used by tests & demo.
 * TODO better : rather use Bootstrappable, or ??
 * TODO only if fillWithSamples global boolean prop
 * @author mdutoo
 *
 */
@Component
//@DependsOn({"datacoreApiImpl", "datacoreApiServer"}) // else ConnectException NOO deadlock, & same for ApplicationContextAware
public class IgnCityhallSample implements ApplicationListener<ContextRefreshedEvent> {

   // .bdparcellaire
   public static final String IGN_COMMUNE = "ign.commune";
   public static final String IGN_DEPARTEMENT = "ign.departement";
   public static final String IGN_CANTON = "ign.canton";
   public static final String IGN_ARRONDISSEMENT = "ign.arrondissement";
   public static final String IGN_DIVISION = "ign.division"; // Cadastrale
   public static final String IGN_LOCALISANT = "ign.localisant"; // Parcellaire
   public static final String IGN_PARCELLE = "ign.parcelle";
   
   public static final String CITYHALL_IGN_PARCELLE = "cityhall.ign.parcelle";

   /** impl, to be able to modify it
    * TODO LATER extract interface */
   @Autowired
   private DataModelServiceImpl modelAdminService;

   @Autowired
   @Qualifier("datacoreApiCachedClient")
   private /*DatacoreApi*/DatacoreClientApi datacoreApiClient;
   
   @Autowired
   private ResourceService resourceService;


   @Override
   public void onApplicationEvent(ContextRefreshedEvent event) {
      this.init();
   }
   
   //@PostConstruct // NOO deadlock, & same for ApplicationContextAware
   public void init() {
      // because IGN Model governance say so (but this could be forbidden by OASIS) :
      // field names are in French 
      // all fields are required
      
      DCModel ignCommuneModel = new DCModel(IGN_COMMUNE); // rgc., pdp.
      /*ignCommuneModel.setDocumentation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
      ///ignCommuneModel.addField(new DCField("name", "string", true, 100)); // TODO cityCountry's ??!!
      ignCommuneModel.addField(new DCField("departementCode", "int", true, 100)); // DEP
      ignCommuneModel.addField(new DCResourceField("departement", IGN_DEPARTEMENT, true, 100));
      ignCommuneModel.addField(new DCField("communeCode", "int", true, 100)); // COM ; code, commune ?
      ignCommuneModel.addField(new DCField("arrondissementCode", "int", true, 100)); // ARRD
      ignCommuneModel.addField(new DCField("cantonCode", "int", true, 100)); // CANT
      ignCommuneModel.addField(new DCField("statutAdministratif", "int", true, 100)); // ADMI
      ignCommuneModel.addField(new DCField("population", "int", true, 0)); // POPU ; query ?
      ignCommuneModel.addField(new DCField("surface", "int", true, 0)); // SURFACE ; query ?
      ignCommuneModel.addField(new DCField("nom", "string", true, 100)); // NOM ; com.nom ? ; majuscules ; cityCountry's ??
      ignCommuneModel.addField(new DCField("longitudeDMS", "int", true, 100)); // LONGI_DMS
      ignCommuneModel.addField(new DCField("latitudeDMS", "int", true, 100)); // LATI_DMS

      DCModel ignDepartementModel = new DCModel(IGN_DEPARTEMENT); // rgc., pdp.
      ignDepartementModel.addField(new DCField("nom", "string", true, 100)); // DEP ; dep.nom ?
      ignDepartementModel.addField(new DCField("departementCode", "int", true, 100)); // DEP

      DCModel ignCantonModel = new DCModel(IGN_CANTON); // rgc.
      ignCantonModel.addField(new DCField("nom", "string", true, 100)); // ?? ; dep.nom ?
      ignCantonModel.addField(new DCField("departementCode", "int", true, 100)); // DEP
      ignCantonModel.addField(new DCField("arrondissementCode", "int", true, 100)); // ARRD
      ignCantonModel.addField(new DCField("cantonCode", "int", true, 100)); // CANT

      DCModel ignArrondissementModel = new DCModel(IGN_ARRONDISSEMENT); // rgc., bdparcellaire.
      ignArrondissementModel.addField(new DCResourceField("commune", IGN_COMMUNE, true, 100));
      ignArrondissementModel.addField(new DCField("departementCode", "int", true, 100)); // DEP
      ignArrondissementModel.addField(new DCField("arrondissementCode", "int", true, 100)); // ARRD
      ignArrondissementModel.addField(new DCField("absorbeeCode", "int", true, 100)); // commune
      
      DCModel ignDivisionModel = new DCModel(IGN_DIVISION); // bdparcellaire. ; cadastrale
      ignDivisionModel.addField(new DCField("sectionCadastrale", "string", true, 100)); // section
      ignDivisionModel.addField(new DCField("feuilleCadastrale", "int", true, 100)); // feuille
      
      DCModel ignLocalisantModel = new DCModel(IGN_LOCALISANT); // bdparcellaire. ; parcellaire
      ignLocalisantModel.addField(new DCField("sectionCadastrale", "string", true, 100)); // section
      ignLocalisantModel.addField(new DCField("feuilleCadastrale", "int", true, 100)); // feuille
      ignLocalisantModel.addField(new DCField("numeroParcelle", "int", true, 100)); // numero

      DCModel ignParcelleModel = new DCModel(IGN_PARCELLE); // bdparcellaire.
      ignParcelleModel.addField(new DCField("sectionCadastrale", "string", true, 100)); // section
      ignParcelleModel.addField(new DCField("feuilleCadastrale", "int", true, 100)); // feuille
      ignLocalisantModel.addField(new DCField("numeroParcelle", "int", true, 100)); // numero
      
      modelAdminService.addModel(ignCommuneModel);
      modelAdminService.addModel(ignDepartementModel);
      modelAdminService.addModel(ignArrondissementModel);
      modelAdminService.addModel(ignCantonModel);
      modelAdminService.addModel(ignParcelleModel);
      modelAdminService.addModel(ignDivisionModel);
      modelAdminService.addModel(ignLocalisantModel);

      DCResource departement01 = resourceService.create(IGN_DEPARTEMENT, "203")
            .set("nom", "AIN").set("departementCode", 01);
      DCResource communeLaiz = resourceService.create(IGN_COMMUNE, "203")
            .set("nom", "LAIZ").set("arrondissementCode", 2).set("communeCode", 203).set("cantonCode", 27)
            .set("departementCode", 01).set("departement", departement01.getUri())
            .set("statutAdministratif", 6).set("population", 11).set("surface", 1031)
            .set("longitudeDMS", +45320).set("latitudeDMS", 461454);
      DCResource communePeron = resourceService.create(IGN_COMMUNE, "203")
            .set("nom", "PERON").set("arrondissementCode", 3).set("communeCode", 288).set("cantonCode", 12)
            .set("departementCode", 01).set("departement", departement01.getUri())
            .set("statutAdministratif", 6).set("population", 20).set("surface", 2601)
            .set("longitudeDMS", +55535).set("latitudeDMS", 461124);
      departement01 = datacoreApiClient.postDataInType(departement01);
      communeLaiz = datacoreApiClient.postDataInType(communeLaiz);
      communePeron = datacoreApiClient.postDataInType(communePeron);
      
      // IGN patched by cityhalls - v1 (using Mixin, so in same collection)
      DCMixin cityhallIgnParcelleMixin = new DCMixin(CITYHALL_IGN_PARCELLE); // bdparcellaire.
      cityhallIgnParcelleMixin.addField(new DCField("cityhall.numeroParcelle", "int", true, 100));
      ///modelAdminService.addMixin(cityhallIgnParcelleMixin); // LATER ?
            // numeroParcelle ?? => TODO PREFIXES !!!! OR prefix using declaring Model / Mixin ?!
      ignParcelleModel.addMixin(cityhallIgnParcelleMixin); // NB. doesn't change the Model version ?!?!?!
      ///modelAdminService.addModel(ignParcelleModel); // LATER re-add...
      
      // TODO in separate test
      /*
      // IGN patched by cityhalls - v2 (using inheritance, so in separate collection)
      ///DCModel cityhallIgnParcelleModel = new DCModel("cityhall.ign.parcelle", ignParcelleModel.getName()); // bdparcellaire.
      DCModel cityhallIgnParcelleModel = new DCModel(CITYHALL_IGN_PARCELLE);
      cityhallIgnParcelleModel.addMixin(ignParcelleModel); // inheritance = parent model (copy) as mixin NOO OPPOSITE OVERRIDE
      ///modelAdminService.addMixin(ignParcelleModel); // LATER ??
      cityhallIgnParcelleModel.addField(new DCField("cityhall.numeroParcelle", "int", true, 100)); // alt1 new field along the existing one
      cityhallIgnParcelleModel.addField(new DCField("numeroParcelle", "int", true, 100)); // alt2 override (of def ?? or only of value, or auto ?)
      ///modelAdminService.addModel(cityhallIgnParcelleModel); // LATER re-add...
      */
   }
   
}
