package org.oasis.datacore.sample;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.codec.binary.Base64;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCI18nField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.server.resource.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import au.com.bytecode.opencsv.CSVReader;


/**
 * Draft of Provto-provided CityPlanning and EconomicalActivity Models.
 * 
 * Used by tests & demo, as well as "datacorisation" / Model design methodology draft.
 * TODO move this methodology to starter kit : on wiki, D2.2, playground...
 * 
 * Feedback to provider (Provto) :
 * * problem with ATECO descriptions data :
 *  - some provided ATECO descriptions are not among official ones (see below about
 * dataset used) but seem to have been simplified. This is even more critical because
 * they are used as linking id / foreign key instead of ATECO code which is not known
 * in economical activity dataset. For instance :
 * "Commercio al dettaglio di articoli sportivi e per il tempo libero"
 * instead of
 * "Commercio al dettaglio di articoli sportivi, biciclette e articoli per il tempo libero".
 *  - ATECO descriptions often map to two codes. Of those, one seems to be a non-leaf
 * category, but can it be removed ? (i.e. is it never used in the data ?)
 * TODO CSI If you don't want to patch your data it's fine, but patch the data you send to the
 * datacore and provide us with a cleaned up ATECO reference dataset.
 * * dataset - ATECO descriptions ("!?ecoact:atecoDescription" field) :
 * since none was provided, I've used http://www.vi.camcom.it/a_ITA_1834_1.html
 * (from http://www3.istat.it/strumenti/definizioni/ateco/ ) to build
 * oasis-datacore/oasis-datacore-rest-server/src/main/resources/samples/provto/economicalActivity/ateco_20081217.csv
 * * as always, please check choices made for : Model/Mixin & field names, uri format,
 * overall modelling (perimeter of each Model and Mixin, links between them, types of fields)
 *  - TODO CSI especially for the ateco fields : look up "ateco rdf" in google and look in ex. http://www.w3c.it/papers/RDF.pdf
 * * if everything is OK, start using those Models in your demo. This means writing code that
 * first creates new Resources in them. For this, copy / paste / adapt to your language of choice
 * code from fillData(), whose comments say all that must be taken care of.
 * 
 * @author mdutoo
 *
 */
@Component
public class CityPlanningAndEconomicalActivitySample extends DatacoreSampleBase {
   
   /** to be able to build a full uri, to avoid using ResourceService */
   ///@Value("${datacoreApiClient.baseUrl}") 
   ///private String baseUrl; // useless
   /////@Value("${datacoreApiClient.containerUrl}") // DOESN'T WORK 
   @Value("${datacoreApiServer.containerUrl}")
   private String containerUrl;
   

   @Override
   public void fillData() {
      try {
         doInitReferenceData();
      } catch (WebApplicationException waex) {
         throw new RuntimeException("HTTP " + waex.getResponse().getStatus()
               + " web app error initing reference data :\n" + waex.getResponse().getStringHeaders() + "\n"
               + ((waex.getResponse().getEntity() != null) ? waex.getResponse().getEntity() + "\n\n" : ""), waex);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      doInitSampleDataEconomicalActivity();
      doInitSampleDataCityPlanning();
   }
   
   
   /** Modelling methodology step 1 */
   public void doInitModel1FlatM() {
      /////////////////////////////////////////////////////////
      // 0. select data of your application to share, 
      // - either data required by interoperability with other applications
      // - or your application's core business data
      // (as its provider, you now best in which data reside the most value potential)

      
      /////////////////////////////////////////////////////////
      // 1.a provide a denormalized view of your data :
      // i.e. flat, including fields of related classifications
      // ex. https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-rest-server/src/main/resources/samples/provto/economicalActivity/economicalActivity.csv
      
      // and write a Model for it :
      // (the simplest one : no outside references, all fields queriable and string).
      // See Model specification at https://github.com/pole-numerique/oasis-datacore/tree/master/oasis-datacore-samples/src/main/java/org/oasis/datacore/data/meta
      // tips :
      // - give version "0" to your Models (and Mixins) since they are new, and refer to them
      // (from Resource Model type and Resource fields) using modelType = model name + '/' + model version,
      // ex. plo:country/0 .
      // - give a namespace to each of your Model, and a prefix that shortens it and will prefix field names,
      // that contains what it is about and if need be who is responsible for defining it, ex:
      // place.city => pli: , place.city.italia => pliit:
      // This way, we can know from a field nale what it is about and who (which data community) is responsible,
      // and here is why this is important :
      // For instance, a referencingMixin doesn't really define its fields besides the URI,
      // it rather reuses those of the canonical Model that it derives from.
      // Moreover, it's better not to define too many fields in order to have cross Model queries that are doable
      // (ex. o:displayName, pl:name, pl:address, pl:geo, LATER or even full text index...)
      // So the question to be asked is : are those two fields the same or not,
      // are they useful in the same use case (i.e. queries) ?
      // (NB. namespaces are not versioned, because they only occur in the context of well defined
      // versions of Models and Mixins).
      // TEMPORARY FOR NOW use '_' instead of '/' and set model.name to modelType
      // ex. plo:country_0, to avoid URI parsing problems and let older Model samples still work.
      // - put in their documentation the rules that govern Resources or each of your Models
      // - and first of all, the rules that govern their URIs. Some good practices :
      //    - avoid adding internal / technical / business id (unique) fields to allow
      // applications to reconcile with it. Rather, build your Resources' URIs out of those ids,
      // ex. atecoCode "myCode" => uri : "http://data.oasis-eu.org/dc/modelType/ateco/myCode"
      // - available field types : string, boolean, int, float, long, double, date, map, list, i18n, resource
      //    - "queryLimit":100 means that this field is indexed (wouldn't be if rather 0)
      // and may therefore be queried but at most 100 results will be returned (which can be detected
      // by using the "debug" query switch). Its purpose is to guide usage of your Model, by following
      // a "guide the user but prevent him from wrecking everything" (i.e. unoptimized / too big queries)
      // philosophy rather than a "magic join & optimization query engine" philosophy
      // that doesn't work when distributed.
      
      // 1.b group together fields that have been denormalized from another table,
      // and for each add a URI resource reference to a Model corresponding to said table.  

      ///DCModel economicalActivityModel = (DCModel) new DCMixin("!economicalActivityMixin/0"); // "italianCompanyMixin"
      DCModel flatEconomicalActivityModel = (DCModel) new DCModel("!ecoact:economicalActivity_0") // OR on company model !!!!!!!!!
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         
         // about company :
         .addField(new DCField("!ecoact:codiceFiscale", "string", true, 100)) // ex. "GNTLMR89S61L219Q"
         // => might be extracted to another a generic Italian company Mixin on this Model
         //.addField(new DCResourceField("!companyUri", "!codiceFiscale/0", true, 100)) // used as identifier
         .addField(new DCField("!ecoact:companyName", "string", true, 100)) // 
         .addField(new DCField("!ecoact:address", "string", true, 100)) // AND NOT SEVERAL FIELDS ! ex. VIA COSTA ANDREA 3D ; HARD TO RECONCILE !!!!!!
         .addField(new DCField("!ecoact:geometry", "string", true, 100)) // point ; WKS ; used as identifier
         // => might be extracted to another a generic company Mixin on this Model
         // but referencing of another (company or codiceFiscale) Model since it's
         // already thought out to be the same as this one
      
         // about company (ateco) type :
         .addField(new DCField("!ecoact:atecoCode", "string", true, 100)) // ex. "1.2" or "1.2.3" (in turkish : "" & "NACE" comes from an international one)
         .addField(new DCField("!?ecoact:atecoDescription", "string", true, 100)) // 1000s ; ex. "Commercio al dettaglio di articoli sportivi e per il tempo libero"
         // => to be extracted to another Model, meaning we'll add its uri/id :
         //.addField(new DCResourceField("!ecoact:atecoUri", "!ecoact:ateco_0", true, 100))
         
         // about city (and company) :
         .addField(new DCField("!ecoact:municipality", "string", true, 100)) // ex. COLLEGNO ; only description (display name) and not toponimo
         // => to be extracted to another Model, meaning we'll add its uri/id :
         //.addField(new DCResourceField("!ecoact:municipalityUri", "!ecoact:municipality_0", true, 100))
         ;
      

      DCModel cityPlanningModel = (DCModel) new DCModel("!cityarea:cityPlanning_0") // OR urbanArea
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
      
          // about city : "italianCityMixin"
         .addField(new DCField("!cityarea:ISTAT", "int", true, 100)) // ex. "1272" 4 digits
         .addField(new DCField("!?cityarea:city_name", "string", true, 100)) // "Torino" ; i18n
         // => to be extracted to another Model, meaning we'll add its uri/id :
         //.addField(new DCResourceField("!cityarea:cityUri", "!city_0", true, 100))
         
         // about destination of use :
         .addField(new DCField("!?cityarea:destinationOfUse_normalizedCode", "string", true, 100)) // COD_N "1,1", "2.4", "6", "6,32"
         .addField(new DCField("!cityarea:destinationOfUse_description", "strin", true, 100)) // ex. "rezidentiale consolidato"
         .addField(new DCField("!cityarea:destinationOfUse_sigla", "float", true, 100)) // non-standardized, decided internally by its city ; R2 R9 M1...
         // + city of use to differentiate sigla
         // => to be extracted to another Model, meaning we'll add its uri/id :
         //.addField(new DCResourceField("!destinationOfUse:destinationOfUseUri", "!destinationOfUse:destinationOfUse_0", true, 100)) // useful ?
         
         // about public act (of the mayor) that defines this destination :
         .addField(new DCField("!cityarea:dcc", "string", true, 100)) // public act (of the mayor) that defines this destination ex. 2008/04/01-61
         // => to be extracted to another Model, meaning we'll add its uri/id :
         //.addField(new DCResourceField("!cityarea:dccUri", "!cityarea:cityDcc_0", true, 100)) // useful ?
         
         // about urban area ;
         .addField(new DCField("!cityarea:geometry", "string", true, 100)) // shape of the urban area ; WKS ; used as identifier
         ;
   }


   /** Modelling methodology step 2 */
   public void doInitModel2ExternalizeAsMixins() {
      
      /////////////////////////////////////////////////////////
      // 1.c extract said groups out of it as Mixins (i.e. reusable & referenced parts.
      // NB. a common base Mixin for each referenced Model and referencing Mixin
      // could be defined, but since there could be many different referencing Mixing,
      // it's not really useful => rather promote reuse of Model fields in
      // referencing Mixins ex. check that referencing Mixin fields are the Model's ;
      // the only reason to have different fields would be to have different uses
      // i.e. query limit or index.
      

      DCModel atecoModel = (DCModel) new DCModel("!ateco:ateco_0")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(new DCField("!ateco:atecoCode", "string", true, 100)) // ex. "1.2" or "1.2.3" (in turkish : "" & "NACE" comes from an international one)
         .addField(new DCField("!?ateco:atecoDescription", "string", true, 100));

      DCMixin atecoReferencingMixin = (DCMixin) new DCMixin("!ecoact:ateco_0_ref_0")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(atecoModel.getField("!ateco:atecoCode")) // ex. "1.2" or "1.2.3" (in turkish : "" & "NACE" comes from an international one)
         .addField(atecoModel.getField("!?ateco:atecoDescription")) // 1000s ; ex. "Commercio al dettaglio di articoli sportivi e per il tempo libero"
         .addField(new DCResourceField("!ateco:ateco", "!ateco_0", true, 100));

      DCModel municipalityModel = (DCModel) new DCModel("!municipality:municipality_0") // "italianCityMixin"
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(new DCField("!municipality:municipality", "string", true, 100)); // ex. COLLEGNO ; only description (display name) and not toponimo;

      DCMixin municipalityReferencingMixin = (DCMixin) new DCMixin("!ecoact:municipality_0_ref_0")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(municipalityModel.getField("!municipality")) // ex. COLLEGNO ; only description (display name) and not toponimo
         // => to be extracted to another Model, therefore add its uri/id :
         .addField(new DCResourceField("!ecoact:municipalityUri", "!municipality:municipality_0", true, 100));
         
      ///DCModel economicalActivityModel = (DCModel) new DCMixin("!economicalActivityMixin"); // "italianCompanyMixin"
      DCModel economicalActivityModel = (DCModel) new DCModel("!ecoact:economicalActivity_0") // OR on company model !!!!!!!!!
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
            
            // about company :
            .addField(new DCField("ecoact:codiceFiscale", "string", true, 100)) // ex. "GNTLMR89S61L219Q" ; used as identifier
            // => might be extracted to another a generic Italian company Mixin on this Model
            .addField(new DCField("!ecoact:companyName", "string", true, 100)) // 
            .addField(new DCField("!ecoact:address", "string", true, 100)) // AND NOT SEVERAL FIELDS ! ex. VIA COSTA ANDREA 3D ; HARD TO RECONCILE !!!!!! ; might be a standardized "address" field
            .addField(new DCField("!ecoact:geometry", "string", true, 100)) // point ; WKS ; might be used as identifier ; might be a standardized "geo" field
            // => might be extracted to another a generic company Mixin on this Model
            // but referencing of another (company or codiceFiscale) Model since it's
            // already thought out to be the same as this one
         
            // about company (ateco) type :
            .addMixin(atecoReferencingMixin)
            
            // about city :
            .addMixin(municipalityReferencingMixin)
         ;
   }

   
   /**
    * Defines Models
    */
   @Override
   public void buildModels(List<DCModelBase> modelsToCreate) {
      // 2. find out which Models already exist, including "this" ex. economicalActivity,
      // and reconcile with them :
      // - reuse existing fields ; if format is different, then if bijective convert them on the fly,
      // else use another field(s) and maintain it (LATER link both and if the source one changes :
      //    - either put the change only as an alternate version of the Resource in the contribution derived Model and collection
      // (which should then be made queriable i.e. indexed and supported by the query engine) ;
      //    - or mark it obsolete at resource level in fields such as o:obsoleteFields,
      // o:lastApprovedFieldValues, o:unapprovedFieldValues ;
      //    - or put the change in the same Model and collection but with a contribution id
      // (source and version) as uri suffix (i.e. a polymorphic alternative to contributions)
      // - or else add new ones in new Mixins (your referenced Models become Mixins)
      
      
      DCModel atecoModel = (DCModel) new DCModel("!coita:ateco_0")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(new DCField("!coita:atecoCode", "string", true, 100)) // ex. "1.2" or "1.2.3" (in turkish : "" & "NACE" comes from an international one)
         .addField(new DCField("!?coita:atecoDescription", "string", true, 100));
      atecoModel.setDocumentation("id = !coita:atecoCode");

      DCMixin atecoReferencingMixin = (DCMixin) new DCMixin("!coita:ateco_0_ref_0")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(atecoModel.getField("!coita:atecoCode")) // ex. "1.2" or "1.2.3" (in turkish : "" & "NACE" comes from an international one)
         .addField(atecoModel.getField("!?coita:atecoDescription")) // 1000s ; ex. "Commercio al dettaglio di articoli sportivi e per il tempo libero"
         .addField(new DCResourceField("!coita:ateco", "!coita:ateco_0", true, 100));

      DCModel countryModel = (DCModel) new DCModel("!plo:country_0")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(new DCField("!plo:name", "string", true, 100))
         .addField(new DCI18nField("!plo:name_i18n", 100)); // TODO LATER "required" default language
      countryModel.setDocumentation("id = !plo:name");

      DCMixin countryReferencingMixin = (DCMixin) new DCMixin("!plo:country_0_ref_0")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(countryModel.getField("!plo:name")) // ex. ITALY
         // => to be extracted to another Model, therefore add its uri/id :
         .addField(new DCResourceField("!plo:country", "!plo:country_0", true, 100));

      DCModel cityModel = (DCModel) new DCModel("!pli:city_0") // more fields in an "italianCityMixin" ?
         .addField(new DCField("!pli:name", "string", true, 100)) // ex. COLLEGNO ; only description (display name) and not toponimo
         .addField(new DCI18nField("!pli:name_i18n", 100)) // TODO LATER "required" default language
         .addMixin(countryReferencingMixin);
      cityModel.setDocumentation("id = !plo:name + '/' + !pli:name");

      /*DCMixin cityReferencingMixin = (DCMixin) new DCMixin("!pli:city/0_ref/0")
         .addField(cityModel.getField("!pli:name")) // ex. COLLEGNO ; only description (display name) and not toponimo
         // => to be extracted to another Model, therefore add its uri/id :
         .addField(new DCResourceField("!pli:city", "!pli:city/0", true, 100))
         .addMixin(countryReferencingMixin);*/
      DCMixin cityReferencingMixin = (DCMixin) createReferencingMixin(cityModel, true, "!pli:name");
      
      DCMixin placeMixin = (DCMixin) new DCMixin("!pl:place_0")
         .addField(new DCField("!pl:name", "string", true, 100)) // NB. same name, address & geometry as all places,
         // for easy lookup over all kind of places. NB. company may have additional "raison social" field.
         // NB. there could be an additional, even more generic o:displayName or dc:title field
         // that is computed from pl:name but also person:firstName + ' ' + person:lastName
         // for easy lookup by display name over all data.
         .addField(new DCField("!pl:address", "string", true, 100)) // AND NOT SEVERAL FIELDS ! ex. VIA COSTA ANDREA 3D ; HARD TO RECONCILE !!!!!! ; might be a standardized "address" field
         .addField(new DCField("!pl:geo", "string", true, 100)) // point ; WKS ; might be used as identifier ; might be a standardized "geo" field
         // => might be extracted to another a generic company Mixin on this Model
         // but referencing of another (company or codiceFiscale) Model since it's
         // already thought out to be the same as this one
         .addMixin(cityReferencingMixin);
      
      DCModel companyModel = (DCModel) new DCModel("!co:company_0")
         // about place & city (& country) :
         .addMixin(placeMixin);
         
      ///DCModel economicalActivityModel = (DCModel) new DCMixin("!economicalActivityMixin/0");
      DCMixin italianCompanyMixin = (DCMixin) new DCMixin("!coit:company_0")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
            
            // about italian company :
            .addField(new DCField("!coit:codiceFiscale", "string", true, 100)) // ex. "GNTLMR89S61L219Q" ; used as identifier
            // => might be extracted to another a generic Italian company Mixin on this Model
         
            // about italian company (ateco) type :
            .addMixin(atecoReferencingMixin)
         ;
      companyModel.addMixin(italianCompanyMixin);
      
      
      

      DCMixin italianCityMixin = (DCMixin) new DCMixin("!pliit:city_0")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         // about italian city :
         .addField(new DCField("!pliit:ISTAT", "int", false, 100)); // ex. "1272" 4 digits ; not required else can't collaborate
      cityModel.addMixin(italianCityMixin);

      DCMixin italianCityReferencingMixin = (DCMixin) new DCMixin("!pliit:city_0_ref_0")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addMixin(cityReferencingMixin)
         .addField(italianCityMixin.getField("!pliit:ISTAT")) // ex. "1272" 4 digits
         // => to be extracted to another Model, therefore add its uri/id :
         .addField(new DCResourceField("!pli:city", "!pli:city_0", true, 100));

      DCModel italianUrbanAreaDestinationOfUseModel = (DCModel) new DCModel("!cityareauseit:urbanAreaDestinationOfUse_0") // OR urbanArea
         .addField(new DCField("!?cityareauseit:normalizedCode", "string", true, 100)) // COD_N "1,1", "2,4", "6", "6,32" ; COULD BE float if transformed but not much useful
         .addField(new DCField("!cityareauseit:description", "string", true, 100)) // ex. "rezidentiale consolidato"
         .addField(new DCField("!cityareauseit:sigla", "string", true, 100)) // non-standardized, decided internally by its city ; R2 R9 M1...
          // + city of use to differentiate sigla
         .addMixin(italianCityReferencingMixin);
      italianUrbanAreaDestinationOfUseModel.setDocumentation("id = !1cityareauseit:normalizedCode + '/' + !pli:name + '/' + !cityareauseit:sigla");

      DCMixin italianUrbanAreaDestinationOfUseReferencingMixin = (DCMixin) new DCMixin("!cityareauseit:urbanAreaDestinationOfUse_0_ref_0")
         .addField(italianUrbanAreaDestinationOfUseModel.getField("!?cityareauseit:normalizedCode")) // COD_N "1,1", "2,4", "6", "6,32" ; TODO international ??
         .addField(italianUrbanAreaDestinationOfUseModel.getField("!cityareauseit:description")) // ex. "rezidentiale consolidato" ; TODO international ?!
         .addField(italianUrbanAreaDestinationOfUseModel.getField("!cityareauseit:sigla")) // non-standardized, decided internally by its city ; R2 R9 M1...
         // + city of use to differentiate sigla
         .addMixin(italianCityReferencingMixin)
         // => to be extracted to another Model, therefore add its uri/id :
         .addField(new DCResourceField("!cityareauseit:urbanAreaDestinationOfUse", "!cityareauseit:urbanAreaDestinationOfUse_0", true, 100));
      
      DCMixin placeShapeMixin = (DCMixin) new DCMixin("pls:placeShape_0") // LATER ONCE GIS will allow to look among all shapes
         .addField(new DCField("!pls:geo", "string", true, 100)); // shape of the area ; WKS 

      DCMixin cityAreaItDCMixin = (DCMixin) new DCMixin("!cityareait:italianCityArea_0") // (cityPlanning) OR cityArea, urbanArea
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         
         // about italian destination of use & italian city :
         .addMixin(italianUrbanAreaDestinationOfUseReferencingMixin)
         
         // about public act (of the mayor) that defines this destination :
         .addField(new DCField("!cityareait:dcc", "string", true, 100)); // public act (of the mayor) that defines this destination ex. 2008/04/01-61
         ///.addField(new DCResourceField("!dccUri", "!cityDcc", true, 100)) // useful ?
      
      DCModel cityAreaModel = (DCModel) new DCModel("!cityarea:cityArea_0") // (cityPlanning) OR cityArea, urbanArea
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         
         // about urban area ;
         .addMixin(placeShapeMixin) // shape of the urban area ; WKS ; the only available id
         .addMixin(cityAreaItDCMixin)
         ;
      cityAreaModel.setDocumentation("id = !pli:city + '/' + hash(id)");
      
      modelsToCreate.addAll(Arrays.asList((DCModelBase) atecoModel, countryModel, cityModel, companyModel, // TODO also Mixins ???
            italianUrbanAreaDestinationOfUseModel, cityAreaModel));
   }


   /**
    * Inits reference Data (i.e. that is not sample data)
    * @throws Exception
    */
   public void doInitReferenceData() throws Exception {
      List<DCResource> resourcesToPost = new ArrayList<DCResource>();
      
      //////////////////////////////////////////////
      // application "install" (at worst init, if possible deploy) time :

      // country :
      
      //List<DCEntity> country = ldpEntityQueryService.findDataInType(atecoModel, new HashMap<String,List<String>>() {{
      //         put("!?coita:atecoDescription", new ArrayList<String>() {{ add((String) company.get("!?coita:atecoDescription")); }}); }}, 0, 1);
      String countryUri = UriHelper.buildUri(containerUrl, "!plo:country_0", "Italia");
      try {
         resourceService.get(countryUri, "!plo:country_0");
      } catch (ResourceNotFoundException rnfex) {
         /*if (Response.Status.NOT_FOUND.getStatusCode() != waex.getResponse().getStatus()) {
            throw new RuntimeException("Unexpected error", waex);
         }*/
         DCResource country = DCResource.create(countryUri)
               .set("!plo:name", "Italia")
               .set("!plo:name_i18n", DCResource.listBuilder()
                     .add(DCResource.propertiesBuilder().put("l", "it").put("v", "Italia").build())
                     .add(DCResource.propertiesBuilder().put("l", "en").put("v", "Italy").build())
                     .add(DCResource.propertiesBuilder().put("l", "fr").put("v", "Italie").build())
                     .build());
         // once props are complete, build URI out of them and schedule post :
         ///country.setUriFromId(containerUrl, (String) country.get("!plo:name"));
         resourcesToPost.add(country);
      }
      
      
      // ateco :
      // NB. ateco Model has to be filled at "install" time else code is not known

      String csvResourcePath = "samples/provto/economicalActivity/ateco_20081217.csv";
      InputStream csvIn = getClass().getClassLoader().getResourceAsStream(csvResourcePath );
      if (csvIn == null) {
         throw new RuntimeException("Unable to find in classpath CSV resource " + csvResourcePath);
      }
      CSVReader csvReader = null;
      try  {
         csvReader = new CSVReader(new InputStreamReader(csvIn), ',');
         String [] line;
         //csvReader.readNext(); // no header
         while ((line = csvReader.readNext()) != null) {
            
            // filling company's provided props :
            DCResource ateco = DCResource.create(null, "!coita:ateco_0")
                  .set("!coita:atecoCode", line[0])
                  .set("!?coita:atecoDescription", line[1]);
            
            // once props are complete, build URI out of them and schedule post :
            ateco.setUriFromId(containerUrl, (String) ateco.get("!coita:atecoCode"));
            resourcesToPost.add(ateco);
         }

      } catch (WebApplicationException waex) {
         throw new RuntimeException("HTTP " + waex.getResponse().getStatus()
               + " web app error reading classpath CSV resource " + csvResourcePath
               + " :\n" + waex.getResponse().getStringHeaders() + "\n"
               + ((waex.getResponse().getEntity() != null) ? waex.getResponse().getEntity() + "\n\n" : ""), waex);
         
      } catch (Exception ex) {
         throw new RuntimeException("Error reading classpath CSV resource " + csvResourcePath, ex);
         
      } finally {
         try {
            csvReader.close();
         } catch (IOException e) {
           // TODO log
         }
      }

      for (DCResource resource : resourcesToPost) {
         /*datacoreApiClient.*/postDataInType(resource);
      }
      
   }


   /**
    * Fills sample data for EconomicalActivity sample
    */
   public void doInitSampleDataEconomicalActivity() {
      List<DCResource> resourcesToPost = new ArrayList<DCResource>();
      
      //////////////////////////////////////////////
      // application "use" time :
      
      String csvResourcePath = "samples/provto/economicalActivity/economicalActivity.csv";
      InputStream csvIn = getClass().getClassLoader().getResourceAsStream(csvResourcePath );
      if (csvIn == null) {
         throw new RuntimeException("Unable to find in classpath CSV resource " + csvResourcePath);
      }
      CSVReader csvReader = null;
      try  {
         csvReader = new CSVReader(new InputStreamReader(csvIn), ',');
         String [] line;
         csvReader.readNext(); // header
         while ((line = csvReader.readNext()) != null) {
            
            // filling company's provided props :
            final DCResource company = DCResource.create(null, "!co:company_0")
                  .set("!coit:codiceFiscale", line[0])
                  //.set("!coita:atecoCode", line[2]) // has to be retrieved
                  .set("!?coita:atecoDescription", line[2])
                  .set("!pl:name", line[1])
                  .set("!pl:address", line[4])
                  .set("!pl:geo", "") // setting dummy for now because required
                  .set("!pli:name", line[3])
                  /*.set("!pli:name_i18n", DCResource.listBuilder() // TODO copy it from referenced Resource if any
                        .add(DCResource.propertiesBuilder().put("l", "it").put("v", company.get("!pli:name").build())
                        .build())*/
                  .set("!plo:name", "Italia"); // hardcoded
                  /*.set("!plo:name_i18n", DCResource.listBuilder() // TODO copy it from referenced Resource
                        .add(DCResource.propertiesBuilder().put("l", "it").put("v", "Italia").build())
                        .add(DCResource.propertiesBuilder().put("l", "en").put("v", "Italy").build())
                        .add(DCResource.propertiesBuilder().put("l", "fr").put("v", "Italie").build())
                        .build());*/
            
            // filling company's resource props and missing referencing Mixin props : 
            // - by building URI from known id/iri if no missing referencing Mixin prop,
            // - or by looking up referenced resource with another field as criteria
            
            // NB. single Italia country has to be filled at "install" time
            company.set("!plo:country", UriHelper.buildUri(containerUrl, "!plo:country_0",
                  (String) company.get("!plo:name")));

            company.set("!pli:city", UriHelper.buildUri(containerUrl, "!pli:city_0",
                  (String) company.get("!plo:name") + '/' + (String) company.get("!pli:name")));

            // NB. ateco Model has to be filled at "install" time else code is not known
            List<DCEntity> atecos = ldpEntityQueryService.findDataInType(modelAdminService.getModel("!coita:ateco_0"), new HashMap<String,List<String>>() {{
                     put("!?coita:atecoDescription", new ArrayList<String>() {{ add((String) company.get("!?coita:atecoDescription")); }}); }}, 0, 1);
            DCResource ateco;
            if (atecos != null && !atecos.isEmpty()) {
               ateco = resourceEntityMapperService.entityToResource(atecos.get(0));
            } else {
               ///throw new RuntimeException("Unknown ateco description " + company.get("!?coita:atecoDescription"));
               // WORKAROUND TO WRONG DESCRIPTIONS : (!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!)
               // filling company's provided props :
               ateco = DCResource.create(null, "!coita:ateco_0")
                     .set("!coita:atecoCode", ((String) company.get("!?coita:atecoDescription")).replace(' ', '_'))
                     .set("!?coita:atecoDescription", company.get("!?coita:atecoDescription"));
               // once props are complete, build URI out of them and post :
               ateco.setUriFromId(containerUrl, (String) ateco.get("!coita:atecoCode"));
               /*datacoreApiClient.*/postDataInType(ateco);
               //resourcesToPost.add(ateco); // post and NOT schedule else can't be found in loop
            }
            company.set("!coita:atecoCode", ateco.get("!coita:atecoCode"));
            company.set("!coita:ateco", ateco.getUri());

            
            // filling other Models that this table is a source of :
            
            try {
               resourceService.get((String) company.get("!pli:city"), "!pli:city_0");
            } catch (ResourceNotFoundException rnfex) {
               /*if (Response.Status.NOT_FOUND.getStatusCode() != waex.getResponse().getStatus()) {
                  throw new RuntimeException("Unexpected error", waex.getResponse().getEntity());
               }*/
               DCResource city = DCResource.create((String) company.get("!pli:city"))
                     .set("!pli:name", company.get("!pli:name"))
                     .set("!pli:name_i18n", DCResource.listBuilder()
                           .add(DCResource.propertiesBuilder().put("l", "it").put("v", company.get("!pli:name")).build())
                           .build())
                     .set("!plo:name", (String) company.get("!plo:name"))
                     /*.set("!plo:name_i18n", DCResource.listBuilder() // TODO copy it from referenced Resource
                        .add(DCResource.propertiesBuilder().put("l", "it").put("v", "Italia").build())
                        .add(DCResource.propertiesBuilder().put("l", "en").put("v", "Italy").build())
                        .add(DCResource.propertiesBuilder().put("l", "fr").put("v", "Italie").build())
                        .build())*/
                     .set("!plo:country", (String) company.get("!plo:country"));
               // once props are complete, build URI out of them and (schedule) post :
               ///city.setUriFromId(containerUrl, (String) company.get("!plo:name") + '/' + (String) company.get("!pli:name"));
               /*datacoreApiClient.*/postDataInType(city);
               //resourcesToPost.add(city); // BEWARE must be posted before company else resource reference check fails
            }

            
            // once props are complete, build URI out of them and schedule post :
            
            company.setUri(UriHelper.buildUri(containerUrl, "!co:company_0",
                  (String) company.get("!plo:name") + '/' + (String) company.get("!coit:codiceFiscale")));
            resourcesToPost.add(company);
         }

      } catch (WebApplicationException waex) {
         throw new RuntimeException("HTTP " + waex.getResponse().getStatus()
               + " web app error reading classpath CSV resource " + csvResourcePath
               + " :\n" + waex.getResponse().getStringHeaders() + "\n"
               + ((waex.getResponse().getEntity() != null) ? waex.getResponse().getEntity() + "\n\n" : ""), waex);
         
      } catch (Exception ex) {
         throw new RuntimeException("Error reading classpath CSV resource " + csvResourcePath, ex);
         
      } finally {
         try {
            csvReader.close();
         } catch (IOException e) {
           // TODO log
         }
      }

      for (DCResource resource : resourcesToPost) {
         /*datacoreApiClient.*/postDataInType(resource);
      }
   }
   

   /**
    * Fills sample data for CityPlanning sample
    */
   public void doInitSampleDataCityPlanning() {
      
      //////////////////////////////////////////////
      // application "use" time :
      
      String csvResourcePath = "samples/provto/cityPlanning/cityPlanning_dummygeo.csv";
      InputStream csvIn = getClass().getClassLoader().getResourceAsStream(csvResourcePath );
      if (csvIn == null) {
         throw new RuntimeException("Unable to find in classpath CSV resource " + csvResourcePath);
      }
      List<DCResource> resourcesToPost = new ArrayList<DCResource>();
      CSVReader csvReader = null;
      try  {
         csvReader = new CSVReader(new InputStreamReader(csvIn), ',');
         String [] line;
         csvReader.readNext(); // header
         while ((line = csvReader.readNext()) != null) {
            
            // filling company's provided props :
            final DCResource cityArea = DCResource.create(null, "!cityarea:cityArea_0")
                  .set("!pls:geo", line[6]) // (dummy data for now)
                  .set("!cityareait:dcc", line[5])
                  .set("!?cityareauseit:normalizedCode", line[2])
                  .set("!cityareauseit:description", line[3])
                  .set("!cityareauseit:sigla", line[4])
                  .set("!pli:name", line[1])
                  .set("!pliit:ISTAT", Integer.parseInt(line[0])) // int !
                  .set("!plo:name", "Italia"); // hardcoded
            
            
            // filling company's resource props and missing referencing Mixin props : 
            // - by building URI from known id/iri if no missing referencing Mixin prop,
            // - or by looking up referenced resource with another field as criteria
            
            // NB. single Italia country has to be filled at "install" time
            cityArea.set("!plo:country", UriHelper.buildUri(containerUrl, "!plo:country_0",
                  (String) cityArea.get("!plo:name")));

            cityArea.set("!pli:city", UriHelper.buildUri(containerUrl, "!pli:city_0",
                  (String) cityArea.get("!plo:name") + '/' + (String) cityArea.get("!pli:name")));

            cityArea.set("!cityareauseit:urbanAreaDestinationOfUse", UriHelper.buildUri(containerUrl,
                  "!cityareauseit:urbanAreaDestinationOfUse_0", (String) cityArea.get("!?cityareauseit:normalizedCode")
                  + '/' + (String) cityArea.get("!pli:name") + '/' + (String) cityArea.get("!cityareauseit:sigla")));

            
            // filling other Models that this table is a source of :
            
            try {
               DCResource city = resourceService.get((String) cityArea.get("!pli:city"), "!pli:city_0");
               if (city != null) {
                  if (!cityArea.get("!pliit:ISTAT").equals(city.get("!pliit:ISTAT"))) { // TODO generic diff
                     // TODO if not owner, submit change IF NOT YET SUBMITTED !!!!!!!
                     city.set("!pliit:ISTAT", cityArea.get("!pliit:ISTAT"));
                     /*datacoreApiClient.*/postDataInType(city);
                     //resourcesToPost.add(city); // could be rather scheduled
                  }
               }
            } catch (ResourceNotFoundException rnfex) {
               /*if (Response.Status.NOT_FOUND.getStatusCode() != waex.getResponse().getStatus()) {
                  throw new RuntimeException("Unexpected error", waex.getResponse().getEntity());
               }*/
               DCResource city = DCResource.create((String) cityArea.get("!pli:city"))
                     .set("!pli:name", cityArea.get("!pli:name"))
                     .set("!pliit:ISTAT", cityArea.get("!pliit:ISTAT"))
                     .set("!plo:name", cityArea.get("!plo:name"))
                     .set("!pli:name_i18n", DCResource.listBuilder()
                           .add(DCResource.propertiesBuilder().put("l", "it").put("v", cityArea.get("!pli:name")).build())
                           .build())
                     .set("!plo:country", cityArea.get("!plo:country"));
               // once props are complete, build URI out of them and (schedule) post :
               ///city.setUriFromId(containerUrl, (String) company.get("!plo:name") + '/' + (String) company.get("!pli:name"));
               /*datacoreApiClient.*/postDataInType(city);
               //resourcesToPost.add(city); // BEWARE must be posted before company else resource reference check fails
            }
            
            try {
               resourceService.get((String) cityArea.get("!cityareauseit:urbanAreaDestinationOfUse"), "!cityareauseit:urbanAreaDestinationOfUse_0");
            } catch (ResourceNotFoundException rnfex) {
               /*if (Response.Status.NOT_FOUND.getStatusCode() != waex.getResponse().getStatus()) {
                  throw new RuntimeException("Unexpected error", waex.getResponse().getEntity());
               }*/
               DCResource cityAreaUseIt = DCResource.create((String) cityArea.get("!cityareauseit:urbanAreaDestinationOfUse"))
                     .set("!?cityareauseit:normalizedCode", cityArea.get("!?cityareauseit:normalizedCode"))
                     .set("!cityareauseit:description", cityArea.get("!cityareauseit:description"))
                     .set("!cityareauseit:sigla", cityArea.get("!cityareauseit:sigla"))
                     .set("!pli:name", cityArea.get("!pli:name"))
                     .set("!pli:city", cityArea.get("!pli:city"))
                     .set("!plo:name", cityArea.get("!plo:name"))
                     .set("!plo:country", cityArea.get("!plo:country"));
               // once props are complete, build URI out of them and (schedule) post :
               ///city.setUriFromId(containerUrl, (String) company.get("!plo:name") + '/' + (String) company.get("!pli:name"));
               /*datacoreApiClient.*/postDataInType(cityAreaUseIt);
               //resourcesToPost.add(cityAreaUseIt); // BEWARE must be posted before company else resource reference check fails
            }

            
            // once props are complete, build URI out of them and schedule post :
            
            cityArea.setUri(UriHelper.buildUri(containerUrl, "!cityarea:cityArea_0",
                  (String) cityArea.get("!plo:name") + '/' + (String) cityArea.get("!pli:name")
                  + '/' + generateId((String) cityArea.get("!pls:geo")))); // OR only hash ?
            resourcesToPost.add(cityArea);
         }

      } catch (WebApplicationException waex) {
         throw new RuntimeException("HTTP " + waex.getResponse().getStatus()
               + " web app error reading classpath CSV resource " + csvResourcePath
               + " :\n" + waex.getResponse().getStringHeaders() + "\n"
               + ((waex.getResponse().getEntity() != null) ? waex.getResponse().getEntity() + "\n\n" : ""), waex);
         
      } catch (Exception ex) {
         throw new RuntimeException("Error reading classpath CSV resource " + csvResourcePath, ex);
         
      } finally {
         try {
            csvReader.close();
         } catch (IOException e) {
           // TODO log
         }
      }

      for (DCResource resource : resourcesToPost) {
         /*datacoreApiClient.*/postDataInType(resource);
      }
      
      // Examples of regular use :
      // see unit test CityPlanningAndEconomicalActivityTest
   }


   
   public static String generateId(String tooComplicatedId) {
      try {
         return new String(Base64.encodeBase64( // else unreadable
               MessageDigest.getInstance("MD5").digest(tooComplicatedId.getBytes("UTF-8"))), "UTF-8");
      } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
         // should not happen
         // TODO log
         throw new RuntimeException(e);
      }
   }


   /**
    * TODO extract as helper
    * @param model
    * @param copyReferencingMixins
    * @param embeddedFieldNames
    * @return
    */
   public static DCMixin createReferencingMixin(DCModel model,
         boolean copyReferencingMixins, String ... embeddedFieldNames) {
      String[] modelType = model.getName().split("_", 2); // TODO better
      String modelName = (modelType.length == 2) ? modelType[0] : model.getName();
      String modelVersionIfAny = (modelType.length == 2) ? modelType[1] : null;
      String modelNameWithVersionIfAny = model.getName();
      DCMixin referencingMixin = (DCMixin) new DCMixin(modelNameWithVersionIfAny + "_ref"
            + ((modelVersionIfAny != null) ? "_" + modelVersionIfAny : ""));
      if (copyReferencingMixins) {
         // copy referencing mixins :
         for (DCModelBase mixin : model.getMixins()) {
            if (mixin.getName().endsWith("_ref_" + mixin.getVersion())) {
               referencingMixin.addMixin(mixin);
            }
         }
      }
      // add embedded & copied fields :
      for (String embeddedFieldName : embeddedFieldNames) {
         referencingMixin.addField(model.getField(embeddedFieldName));
      }
      // add actual resource reference field :
      referencingMixin.addField(new DCResourceField(modelName, modelNameWithVersionIfAny, true, 100));
      return referencingMixin;
   }
   
}
