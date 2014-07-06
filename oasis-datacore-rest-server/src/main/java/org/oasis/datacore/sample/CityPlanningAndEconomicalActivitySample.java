package org.oasis.datacore.sample;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.server.resource.ResourceEntityMapperService;
import org.oasis.datacore.rest.server.resource.ResourceNotFoundException;
import org.oasis.datacore.rest.server.resource.ResourceTypeNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import au.com.bytecode.opencsv.CSVReader;


/**
 * Used by tests & demo.
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
   public void doInit() {
      doInitModel();
      doInitData(); // do provide samples TODO better !!!!!!!!!!!!!!!!!!!!!!!!
   }

   @Override
   public void doInitData() {
      try {
         doInitReferenceData();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      doInitSampleDataEconomicalActivity();
      doInitSampleDataCityPlanning();
   }
   
   
   public void doInit1() {
      /////////////////////////////////////////////////////////
      // 0. select data of your application to share, 
      // - either data required by interoperability with other applications
      // - or your application's core business data
      // (as its provider, you now best in which data reside the most value potential)

      
      /////////////////////////////////////////////////////////
      // 1.a provide a denormalized view of your data, and write a Model for it
      // (the simplest one : no outside references, all fields queriable and string).
      // NB. avoid adding internal / technical / business id (unique) fields to allow
      // applications to reconcile with it. Rather, build your Resources' URIs out of those ids,
      // ex. atecoCode "myCode" => uri : "http://data.oasis-eu.org/dc/type/ateco/myCode"
      
      // 1.b group together fields that have been denormalized from another table,
      // and for each add a URI resource reference to a Model corresponding to said table.  

      ///DCModel economicalActivityModel = (DCModel) new DCMixin("!economicalActivityMixin"); // "italianCompanyMixin"
      DCModel flatEconomicalActivityModel = (DCModel) new DCModel("!ecoact:economicalActivity") // OR on company model !!!!!!!!!
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         
         // about company :
         .addField(new DCField("ecoact:codiceFiscale", "string", true, 100)) // ex. "GNTLMR89S61L219Q"
         // => might be extracted to another a generic Italian company Mixin on this Model
         //.addField(new DCResourceField("!companyUri", "!codiceFiscale", true, 100)) // used as identifier
         .addField(new DCField("!ecoact:companyName", "string", true, 100)) // 
         .addField(new DCField("!ecoact:address", "string", true, 100)) // AND NOT SEVERAL FIELDS ! ex. VIA COSTA ANDREA 3D ; HARD TO RECONCILE !!!!!!
         .addField(new DCField("!ecoact:geometry", "string", true, 100)) // point ; WKS ; used as identifier
         // => might be extracted to another a generic company Mixin on this Model
         // but referencing of another (company or codiceFiscale) Model since it's
         // already thought out to be the same as this one
      
         // about company (ateco) type :
         .addField(new DCField("!ecoact:atecoCode", "string", true, 100)) // ex. "1.2" or "1.2.3" (in turkish : "" & "NACE" comes from an international one)
         .addField(new DCField("!?ecoact:atecoDescription", "string", true, 100)) // 1000s ; ex. "Commercio al dettaglio di articoli sportivi e per il tempo libero"
         // => to be extracted to another Model, therefore add its uri/id :
         .addField(new DCResourceField("ecoact:atecoUri", "!ateco", true, 100))
         
         // about city (and company) :
         .addField(new DCField("!ecoact:municipality", "string", true, 100)) // ex. COLLEGNO ; only description (display name) and not toponimo
         // => to be extracted to another Model, therefore add its uri/id :
         .addField(new DCResourceField("!ecoact:municipalityUri", "!city", true, 100))
         ;
      

      DCModel cityPlanningModel = (DCModel) new DCModel("!cityPlanning") // OR urbanArea
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
      
          // about city : "italianCityMixin"
         .addField(new DCField("!ISTAT", "int", true, 100)) // ex. "1272" 4 digits
         .addField(new DCField("!?city:name", "string", true, 100)) // "Torino" ; i18n
         .addField(new DCResourceField("!cityUri", "!city", true, 100))
         
         // about destination of use :
         .addField(new DCField("!?normalizedCode", "string", true, 100)) // COD_N "1,1", "2.4", "6", "6,32"
         .addField(new DCField("!destinationOfUse.description", "strin", true, 100)) // ex. "rezidentiale consolidato"
         .addField(new DCField("!destinationOfUse.sigla", "float", true, 100)) // non-standardized, decided internally by its city ; R2 R9 M1...
         // + city of use to differentiate sigla
         .addField(new DCResourceField("!destinationOfUseUri", "!destinationOfUse", true, 100)) // useful ?
         
         // about public act (of the mayor) that defines this destination :
         .addField(new DCField("!dcc", "string", true, 100)) // public act (of the mayor) that defines this destination ex. 2008/04/01-61
         .addField(new DCResourceField("!dccUri", "!cityDcc", true, 100)) // useful ?
         
         // about urban area ;
         .addField(new DCField("!geometry", "string", true, 100)) // shape of the urban area ; WKS ; used as identifier
         ;
   }

   
   public void doInit2() {
      
      /////////////////////////////////////////////////////////
      // 1.c extract said groups out of it as Mixins (i.e. reusable & referenced parts.
      // NB. a common base Mixin for each referenced Model and referencing Mixin
      // could be defined, but since there could be many different referencing Mixing,
      // it's not really useful => rather promote reuse of Model fields in
      // referencing Mixins ex. check that referencing Mixin fields are the Model's ;
      // the only reason to have different fields would be to have different uses
      // i.e. query limit or index.
      

      DCModel atecoModel = (DCModel) new DCModel("!economicalActivity_!ateco")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(new DCField("!atecoCode", "string", true, 100)) // ex. "1.2" or "1.2.3" (in turkish : "" & "NACE" comes from an international one)
         .addField(new DCField("!?atecoDescription", "string", true, 100));

      DCMixin atecoReferencingMixin = (DCMixin) new DCMixin("!economicalActivity_!ateco.ref")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(atecoModel.getField("!atecoCode")) // ex. "1.2" or "1.2.3" (in turkish : "" & "NACE" comes from an international one)
         .addField(atecoModel.getField("!?atecoDescription")) // 1000s ; ex. "Commercio al dettaglio di articoli sportivi e per il tempo libero"
         .addField(new DCResourceField("!ateco", "!ateco", true, 100));

      DCModel municipalityModel = (DCModel) new DCModel("!economicalActivity_!municipality") // "italianCityMixin"
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(new DCField("!municipality", "string", true, 100)); // ex. COLLEGNO ; only description (display name) and not toponimo;

      DCMixin municipalityReferencingMixin = (DCMixin) new DCMixin("!economicalActivity_!municipality.ref")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(municipalityModel.getField("!municipality")) // ex. COLLEGNO ; only description (display name) and not toponimo
         // => to be extracted to another Model, therefore add its uri/id :
         .addField(new DCResourceField("!municipalityUri", "!city", true, 100));
         
      ///DCModel economicalActivityModel = (DCModel) new DCMixin("!economicalActivityMixin"); // "italianCompanyMixin"
      DCModel economicalActivityModel = (DCModel) new DCModel("!economicalActivity") // OR on company model !!!!!!!!!
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
            
            // about company :
            .addField(new DCField("codiceFiscale", "string", true, 100)) // ex. "GNTLMR89S61L219Q" ; used as identifier
            // => might be extracted to another a generic Italian company Mixin on this Model
            .addField(new DCField("!companyName", "string", true, 100)) // 
            .addField(new DCField("!address", "string", true, 100)) // AND NOT SEVERAL FIELDS ! ex. VIA COSTA ANDREA 3D ; HARD TO RECONCILE !!!!!! ; might be a standardized "address" field
            .addField(new DCField("!geometry", "string", true, 100)) // point ; WKS ; might be used as identifier ; might be a standardized "geo" field
            // => might be extracted to another a generic company Mixin on this Model
            // but referencing of another (company or codiceFiscale) Model since it's
            // already thought out to be the same as this one
         
            // about company (ateco) type :
            .addMixin(atecoReferencingMixin)
            
            // about city :
            .addMixin(municipalityReferencingMixin)
         ;
   }

   
   public void doInitModel() {
      // 2. find out which Models already exist, including "this" ex. economicalActivity,
      // and reconcile with them :
      // - reuse existing fields ; if format is different, if bijective convert them on the fly,
      // else use another field and maintain it (LATER link both & mark it obsolete
      // at field level if the source one changes)
      // - or else add new ones in new Mixins (your referenced Models become Mixins)
      
      
      DCModel atecoModel = (DCModel) new DCModel("!coita:ateco")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(new DCField("!coita:atecoCode", "string", true, 100)) // ex. "1.2" or "1.2.3" (in turkish : "" & "NACE" comes from an international one)
         .addField(new DCField("!?coita:atecoDescription", "string", true, 100));
      atecoModel.setDocumentation("id = !coita:atecoCode");

      DCMixin atecoReferencingMixin = (DCMixin) new DCMixin("!coita:ateco.ref")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(atecoModel.getField("!coita:atecoCode")) // ex. "1.2" or "1.2.3" (in turkish : "" & "NACE" comes from an international one)
         .addField(atecoModel.getField("!?coita:atecoDescription")) // 1000s ; ex. "Commercio al dettaglio di articoli sportivi e per il tempo libero"
         .addField(new DCResourceField("!coita:ateco", "!coita:ateco", true, 100));

      DCModel countryModel = (DCModel) new DCModel("!plo:country")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(new DCField("!plo:country_name", "string", true, 100));
      countryModel.setDocumentation("id = !plo_country_name");

      DCMixin countryReferencingMixin = (DCMixin) new DCMixin("!plo:country_ref")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(countryModel.getField("!plo:country_name")) // ex. ITALY
         // => to be extracted to another Model, therefore add its uri/id :
         .addField(new DCResourceField("!plo:country", "!plo:country", true, 100));

      DCModel cityModel = (DCModel) new DCModel("!pli:city") // more fields in an "italianCityMixin" ?
         .addField(new DCField("!pli:city_name", "string", true, 100)) // ex. COLLEGNO ; only description (display name) and not toponimo
         .addMixin(countryReferencingMixin);
      cityModel.setDocumentation("id = !plo_country_name + '/' + !pli_city_name");

      /*DCMixin cityReferencingMixin = (DCMixin) new DCMixin("!pli:city_ref")
         .addField(cityModel.getField("!pli:city_name")) // ex. COLLEGNO ; only description (display name) and not toponimo
         // => to be extracted to another Model, therefore add its uri/id :
         .addField(new DCResourceField("!pli:city", "!pli:city", true, 100))
         .addMixin(countryReferencingMixin);*/
      DCMixin cityReferencingMixin = (DCMixin) createReferencingMixin(cityModel, true, "!pli:city_name");
      
      DCMixin placeMixin = (DCMixin) new DCMixin("!pl:place")
         .addField(new DCField("!pl:name", "string", true, 100)) // NB. same name, address & geometry as all places to easy lookup (NB. company may have additional "raison social" field)
         .addField(new DCField("!pl:address", "string", true, 100)) // AND NOT SEVERAL FIELDS ! ex. VIA COSTA ANDREA 3D ; HARD TO RECONCILE !!!!!! ; might be a standardized "address" field
         .addField(new DCField("!pl:geo", "string", true, 100)) // point ; WKS ; might be used as identifier ; might be a standardized "geo" field
         // => might be extracted to another a generic company Mixin on this Model
         // but referencing of another (company or codiceFiscale) Model since it's
         // already thought out to be the same as this one
         .addMixin(cityReferencingMixin);
      
      DCModel companyModel = (DCModel) new DCModel("!co:company")
         // about place & city (& country) :
         .addMixin(placeMixin);
         
      ///DCModel economicalActivityModel = (DCModel) new DCMixin("!economicalActivityMixin");
      DCMixin italianCompanyMixin = (DCMixin) new DCMixin("!coit:company")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
            
            // about italian company :
            .addField(new DCField("!coit:codiceFiscale", "string", true, 100)) // ex. "GNTLMR89S61L219Q" ; used as identifier
            // => might be extracted to another a generic Italian company Mixin on this Model
         
            // about italian company (ateco) type :
            .addMixin(atecoReferencingMixin)
         ;
      companyModel.addMixin(italianCompanyMixin);
      
      
      

      DCMixin italianCityMixin = (DCMixin) new DCMixin("!pliit:city")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         // about italian city :
         .addField(new DCField("!pliit:ISTAT", "int", false, 100)); // ex. "1272" 4 digits ; not required else can't collaborate
      cityModel.addMixin(italianCityMixin);

      DCMixin italianCityReferencingMixin = (DCMixin) new DCMixin("!pliit:city_ref")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addMixin(cityReferencingMixin)
         .addField(italianCityMixin.getField("!pliit:ISTAT")) // ex. "1272" 4 digits
         // => to be extracted to another Model, therefore add its uri/id :
         .addField(new DCResourceField("!pli:city", "!pli:city", true, 100));

      DCModel italianUrbanAreaDestinationOfUseModel = (DCModel) new DCModel("!cityareauseit:urbanAreaDestinationOfUse") // OR urbanArea
         .addField(new DCField("!?cityareauseit:normalizedCode", "string", true, 100)) // COD_N "1,1", "2,4", "6", "6,32" ; COULD BE float if transformed but not much useful
         .addField(new DCField("!cityareauseit:description", "string", true, 100)) // ex. "rezidentiale consolidato"
         .addField(new DCField("!cityareauseit:sigla", "string", true, 100)) // non-standardized, decided internally by its city ; R2 R9 M1...
          // + city of use to differentiate sigla
         .addMixin(italianCityReferencingMixin);
      italianUrbanAreaDestinationOfUseModel.setDocumentation("id = !1cityareauseit:normalizedCode + '/' + !pli:city_name + '/' + !cityareauseit:sigla");

      DCMixin italianUrbanAreaDestinationOfUseReferencingMixin = (DCMixin) new DCMixin("!cityareauseit:urbanAreaDestinationOfUse_ref")
         .addField(italianUrbanAreaDestinationOfUseModel.getField("!?cityareauseit:normalizedCode")) // COD_N "1,1", "2,4", "6", "6,32" ; TODO international ??
         .addField(italianUrbanAreaDestinationOfUseModel.getField("!cityareauseit:description")) // ex. "rezidentiale consolidato" ; TODO international ?!
         .addField(italianUrbanAreaDestinationOfUseModel.getField("!cityareauseit:sigla")) // non-standardized, decided internally by its city ; R2 R9 M1...
         // + city of use to differentiate sigla
         .addMixin(italianCityReferencingMixin)
         // => to be extracted to another Model, therefore add its uri/id :
         .addField(new DCResourceField("!cityareauseit:urbanAreaDestinationOfUse", "!cityareauseit:urbanAreaDestinationOfUse", true, 100));
      
      DCMixin placeShapeMixin = (DCMixin) new DCMixin("pls:placeShape") // LATER ONCE GIS will allow to look among all shapes
         .addField(new DCField("!pls:geo", "string", true, 100)); // shape of the area ; WKS 

      DCMixin cityAreaItDCMixin = (DCMixin) new DCMixin("!cityareait:italianCityArea") // (cityPlanning) OR cityArea, urbanArea
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         
         // about italian destination of use & italian city :
         .addMixin(italianUrbanAreaDestinationOfUseReferencingMixin)
         
         // about public act (of the mayor) that defines this destination :
         .addField(new DCField("!cityareait:dcc", "string", true, 100)); // public act (of the mayor) that defines this destination ex. 2008/04/01-61
         ///.addField(new DCResourceField("!dccUri", "!cityDcc", true, 100)) // useful ?
      
      DCModel cityAreaModel = (DCModel) new DCModel("!cityarea:cityArea") // (cityPlanning) OR cityArea, urbanArea
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         
         // about urban area ;
         .addMixin(placeShapeMixin) // shape of the urban area ; WKS ; the only available id
         .addMixin(cityAreaItDCMixin)
         ;
      cityAreaModel.setDocumentation("id = !pli:city + '/' + hash(id)");
      
      super.createModelsAndCleanTheirData(atecoModel, countryModel, cityModel, companyModel, // TODO also Mixins ???
            italianUrbanAreaDestinationOfUseModel, cityAreaModel);
   }


   public void doInitReferenceData() throws Exception {
      List<DCResource> resourcesToPost = new ArrayList<DCResource>();
      
      //////////////////////////////////////////////
      // application "install" (at worst init, if possible deploy) time :

      // country :
      
      //List<DCEntity> country = ldpEntityQueryService.findDataInType(atecoModel, new HashMap<String,List<String>>() {{
      //         put("!?coita:atecoDescription", new ArrayList<String>() {{ add((String) company.get("!?coita:atecoDescription")); }}); }}, 0, 1);
      String countryUri = UriHelper.buildUri(containerUrl, "!plo:country", "Italia");
      try {
         resourceService.get(countryUri, "!plo:country");
      } catch (ResourceNotFoundException rnfex) {
         /*if (Response.Status.NOT_FOUND.getStatusCode() != waex.getResponse().getStatus()) {
            throw new RuntimeException("Unexpected error", waex);
         }*/
         DCResource country = DCResource.create(countryUri)
               .set("!plo:country_name", "Italia");
         // once props are complete, build URI out of them and schedule post :
         ///country.setUriFromId(containerUrl, (String) country.get("!plo:country_name"));
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
            DCResource ateco = DCResource.create(null, "!coita:ateco")
                  .set("!coita:atecoCode", line[0])
                  .set("!?coita:atecoDescription", line[1]);
            
            // once props are complete, build URI out of them and schedule post :
            ateco.setUriFromId(containerUrl, (String) ateco.get("!coita:atecoCode"));
            resourcesToPost.add(ateco);
         }
         
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
            final DCResource company = DCResource.create(null, "!co:company")
                  .set("!coit:codiceFiscale", line[0])
                  //.set("!coita:atecoCode", line[2]) // has to be retrieved
                  .set("!?coita:atecoDescription", line[2])
                  .set("!pl:name", line[1])
                  .set("!pl:address", line[4])
                  .set("!pl:geo", "") // setting dummy for now because required
                  .set("!pli:city_name", line[3])
                  .set("!plo:country_name", "Italia"); // hardcoded
            
            
            // filling company's resource props and missing referencing Mixin props : 
            // - by building URI from known id/iri if no missing referencing Mixin prop,
            // - or by looking up referenced resource with another field as criteria
            
            // NB. single Italia country has to be filled at "install" time
            company.set("!plo:country", UriHelper.buildUri(containerUrl, "!plo:country",
                  (String) company.get("!plo:country_name")));

            company.set("!pli:city", UriHelper.buildUri(containerUrl, "!pli:city",
                  (String) company.get("!plo:country_name") + '/' + (String) company.get("!pli:city_name")));

            // NB. ateco Model has to be filled at "install" time else code is not known
            List<DCEntity> atecos = ldpEntityQueryService.findDataInType(modelAdminService.getModel("!coita:ateco"), new HashMap<String,List<String>>() {{
                     put("!?coita:atecoDescription", new ArrayList<String>() {{ add((String) company.get("!?coita:atecoDescription")); }}); }}, 0, 1);
            DCResource ateco;
            if (atecos != null && !atecos.isEmpty()) {
               ateco = resourceEntityMapperService.entityToResource(atecos.get(0));
            } else {
               ///throw new RuntimeException("Unknown ateco description " + company.get("!?coita:atecoDescription"));
               // WORKAROUND TO WRONG DESCRIPTIONS : (!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!)
               // filling company's provided props :
               ateco = DCResource.create(null, "!coita:ateco")
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
               resourceService.get((String) company.get("!pli:city"), "!pli:city");
            } catch (ResourceNotFoundException rnfex) {
               /*if (Response.Status.NOT_FOUND.getStatusCode() != waex.getResponse().getStatus()) {
                  throw new RuntimeException("Unexpected error", waex.getResponse().getEntity());
               }*/
               DCResource city = DCResource.create((String) company.get("!pli:city"))
                     .set("!pli:city_name", company.get("!pli:city_name"))
                     .set("!plo:country_name", (String) company.get("!plo:country_name"))
                     .set("!plo:country", (String) company.get("!plo:country"));
               // once props are complete, build URI out of them and (schedule) post :
               ///city.setUriFromId(containerUrl, (String) company.get("!plo:country_name") + '/' + (String) company.get("!pli:city_name"));
               /*datacoreApiClient.*/postDataInType(city);
               //resourcesToPost.add(city); // BEWARE must be posted before company else resource reference check fails
            }

            
            // once props are complete, build URI out of them and schedule post :
            
            company.setUri(UriHelper.buildUri(containerUrl, "!co:company",
                  (String) company.get("!plo:country_name") + '/' + (String) company.get("!coit:codiceFiscale")));
            resourcesToPost.add(company);
         }
         
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
            final DCResource cityArea = DCResource.create(null, "!cityarea:cityArea")
                  .set("!pls:geo", line[6]) // (dummy data for now)
                  .set("!cityareait:dcc", line[5])
                  .set("!?cityareauseit:normalizedCode", line[2])
                  .set("!cityareauseit:description", line[3])
                  .set("!cityareauseit:sigla", line[4])
                  .set("!pli:city_name", line[1])
                  .set("!pliit:ISTAT", Integer.parseInt(line[0])) // int !
                  .set("!plo:country_name", "Italia"); // hardcoded
            
            
            // filling company's resource props and missing referencing Mixin props : 
            // - by building URI from known id/iri if no missing referencing Mixin prop,
            // - or by looking up referenced resource with another field as criteria
            
            // NB. single Italia country has to be filled at "install" time
            cityArea.set("!plo:country", UriHelper.buildUri(containerUrl, "!plo:country",
                  (String) cityArea.get("!plo:country_name")));

            cityArea.set("!pli:city", UriHelper.buildUri(containerUrl, "!pli:city",
                  (String) cityArea.get("!plo:country_name") + '/' + (String) cityArea.get("!pli:city_name")));

            cityArea.set("!cityareauseit:urbanAreaDestinationOfUse", UriHelper.buildUri(containerUrl,
                  "!cityareauseit:urbanAreaDestinationOfUse", (String) cityArea.get("!?cityareauseit:normalizedCode")
                  + '/' + (String) cityArea.get("!pli:city_name") + '/' + (String) cityArea.get("!cityareauseit:sigla")));

            
            // filling other Models that this table is a source of :
            
            try {
               DCResource city = resourceService.get((String) cityArea.get("!pli:city"), "!pli:city");
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
                     .set("!pli:city_name", cityArea.get("!pli:city_name"))
                     .set("!pliit:ISTAT", cityArea.get("!pliit:ISTAT"))
                     .set("!plo:country_name", cityArea.get("!plo:country_name"))
                     .set("!plo:country", cityArea.get("!plo:country"));
               // once props are complete, build URI out of them and (schedule) post :
               ///city.setUriFromId(containerUrl, (String) company.get("!plo:country_name") + '/' + (String) company.get("!pli:city_name"));
               /*datacoreApiClient.*/postDataInType(city);
               //resourcesToPost.add(city); // BEWARE must be posted before company else resource reference check fails
            }
            
            try {
               resourceService.get((String) cityArea.get("!cityareauseit:urbanAreaDestinationOfUse"), "!cityareauseit:urbanAreaDestinationOfUse");
            } catch (ResourceNotFoundException rnfex) {
               /*if (Response.Status.NOT_FOUND.getStatusCode() != waex.getResponse().getStatus()) {
                  throw new RuntimeException("Unexpected error", waex.getResponse().getEntity());
               }*/
               DCResource cityAreaUseIt = DCResource.create((String) cityArea.get("!cityareauseit:urbanAreaDestinationOfUse"))
                     .set("!?cityareauseit:normalizedCode", cityArea.get("!?cityareauseit:normalizedCode"))
                     .set("!cityareauseit:description", cityArea.get("!cityareauseit:description"))
                     .set("!cityareauseit:sigla", cityArea.get("!cityareauseit:sigla"))
                     .set("!pli:city_name", cityArea.get("!pli:city_name"))
                     .set("!pli:city", cityArea.get("!pli:city"))
                     .set("!plo:country_name", cityArea.get("!plo:country_name"))
                     .set("!plo:country", cityArea.get("!plo:country"));
               // once props are complete, build URI out of them and (schedule) post :
               ///city.setUriFromId(containerUrl, (String) company.get("!plo:country_name") + '/' + (String) company.get("!pli:city_name"));
               /*datacoreApiClient.*/postDataInType(cityAreaUseIt);
               //resourcesToPost.add(cityAreaUseIt); // BEWARE must be posted before company else resource reference check fails
            }

            
            // once props are complete, build URI out of them and schedule post :
            
            cityArea.setUri(UriHelper.buildUri(containerUrl, "!cityarea:cityArea",
                  (String) cityArea.get("!plo:country_name") + '/' + (String) cityArea.get("!pli:city_name")
                  + '/' + generateId((String) cityArea.get("!pls:geo")))); // OR only hash ?
            resourcesToPost.add(cityArea);
         }
         
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
      DCMixin referencingMixin = (DCMixin) new DCMixin(model.getName() + "_ref");
      if (copyReferencingMixins) {
         // copy referencing mixins :
         for (DCModelBase mixin : model.getMixins()) {
            if (mixin.getName().endsWith("_ref")) {
               referencingMixin.addMixin(mixin);
            }
         }
      }
      // add embedded & copied fields :
      for (String embeddedFieldName : embeddedFieldNames) {
         referencingMixin.addField(model.getField(embeddedFieldName));
      }
      // add actual resource reference field :
      referencingMixin.addField(new DCResourceField(model.getName(), model.getName(), true, 100));
      return referencingMixin;
   }
   
}
