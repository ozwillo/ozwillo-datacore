package org.oasis.datacore.sample;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.WebApplicationException;

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
import org.springframework.stereotype.Component;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;


/**
 * Draft of OpenElec Models.
 * 
 * Used by tests & demo
 * 
 * Feedback to provider (Atreal) :
 * TODO
 * * as always, please check choices made for : Model/Mixin & field names, uri format,
 * overall modelling (perimeter of each Model and Mixin, links between them, types of fields)
 * * if everything is OK, start using those Models in your demo. This means writing code that
 * first creates new Resources in them. For this, copy / paste / adapt to your language of choice
 * code from fillData(), whose comments say all that must be taken care of.
 * 
 * @author mdutoo
 *
 */
@Component
public class OpenElecSample extends DatacoreSampleMethodologyBase {
   

   @Override
   public void fillData() {
      /*try {
         doInitReferenceData();
      } catch (WebApplicationException waex) {
         throw new RuntimeException("HTTP " + waex.getResponse().getStatus()
               + " web app error initing reference data :\n" + waex.getResponse().getStringHeaders() + "\n"
               + ((waex.getResponse().getEntity() != null) ? waex.getResponse().getEntity() + "\n\n" : ""), waex);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      doInitSampleData();*/
   }

   
   @Override
   public void doOSelectPerimeter() {
      
   }
   

   @Override
   public void do1DesignFlatModel() {
      
      DCModel electoralListModel = (DCModel) new DCModel("el:electoralList")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         
         // about : TODO extract to electoralListKind
         .addField(new DCField("el:id", "string", true, 100)) // OU ne garder que name ?? internal ? ex. 1, 2, 3 (main ; european ; european city)
         .addField(new DCField("el:name", "string", true, 100)) // ex. LISTE GENERALE, LISTE EUROPEENNE, LISTE MUNICIPALES EUROPEENNE
         .addField(new DCField("el:inseeCode", "string", true, 100)) // ex. p ???
         ;

      DCModel pollingStationModel = (DCModel) new DCModel("ps:pollingStation")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         
         // about : TODO extract to electoralListKind
         .addField(new DCField("ps:id", "string", true, 100)) // OU ne garder que name ?? internal ? ex. 3, 3, 2, 1
         .addField(new DCField("ps:name", "string", true, 100)) // ex. rien, BUREAU N°2  (ANNEYRON EST)
         .addField(new DCField("ps:address1", "string", true, 100)) // ex. rien, SALLE DES FÊTES
         .addField(new DCField("ps:address2", "string", true, 100)) // OU qu'un champ ?? formatté ? ex.
         .addField(new DCField("ps:address3", "string", true, 100)) // OU qu'un champ ?? formatté ? ex.
         
         // about canton :
         // TODO
         ;

      DCModel cantonModel = (DCModel) new DCModel("ca:canton")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         
         // about :
         .addField(new DCField("ca:id", "string", true, 100)) // OU ne garder que name ?? internal, unique in city ? ex. rien, 1
         .addField(new DCField("ca:name", "string", true, 100)) // ex. rien, CANTON 1
         ;
      
      DCModel personModel = (DCModel) new DCModel("openelec:person")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/

         // about elector : TODO extract to French elector
         .addField(new DCField("elector:id", "string", true, 100)) // int, internal ?? unique in city ? ex. 430, 1060
         .addField(new DCField("elector:numberInTheCity", "string", true, 100)) // int, index or amount ??? unique in city ? ex. 221, 1030
         .addField(new DCField("elector:modificationDate", "string", true, 100)) 
         .addField(new DCField("elector:title", "string", true, 100)) 
         .addField(new DCField("elector:sex", "string", true, 100)) 
         .addField(new DCField("elector:birthName", "string", true, 100))
         .addField(new DCField("elector:surnames", "string", true, 100))
         .addField(new DCField("elector:birthDate", "string", true, 100))
         
         .addField(new DCField("ecoact:geometry", "string", true, 100)) // point ; WKS ; used as identifier
         // => might be extracted to another a generic company Mixin on this Model
         // but referencing of another (company or codiceFiscale) Model since it's
         // already thought out to be the same as this one
      
         // about company (ateco) type :
         .addField(new DCField("ecoact:atecoCode", "string", true, 100)) // ex. "1.2" or "1.2.3" (in turkish : "" & "NACE" comes from an international one)
         .addField(new DCField("ecoact:atecoDescription", "string", true, 100)) // 1000s ; ex. "Commercio al dettaglio di articoli sportivi e per il tempo libero"
         // => to be extracted to another Model, meaning we'll add its uri/id :
         //.addField(new DCResourceField("ecoact:atecoUri", "ecoact:ateco_0", true, 100))
         
         // about city (and company) :
         .addField(new DCField("ecoact:municipality", "string", true, 100)) // ex. COLLEGNO ; only description (display name) and not toponimo
         // => to be extracted to another Model, meaning we'll add its uri/id :
         //.addField(new DCResourceField("ecoact:municipalityUri", "ecoact:municipality_0", true, 100))
         ;
      

      DCModel cityModel = (DCModel) new DCModel("cityarea:cityPlanning_0") // OR urbanArea
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
      
          // about city : "italianCityMixin"
         .addField(new DCField("cityarea:ISTAT", "int", true, 100)) // ex. "1272" 4 digits
         .addField(new DCField("cityarea:city_name", "string", true, 100)) // "Torino" ; i18n
         // => to be extracted to another Model, meaning we'll add its uri/id :
         //.addField(new DCResourceField("cityarea:cityUri", "city_0", true, 100))
         
         // about destination of use :
         .addField(new DCField("cityarea:destinationOfUse_normalizedCode", "string", true, 100)) // COD_N "1,1", "2.4", "6", "6,32"
         .addField(new DCField("cityarea:destinationOfUse_description", "strin", true, 100)) // ex. "rezidentiale consolidato"
         .addField(new DCField("cityarea:destinationOfUse_sigla", "float", true, 100)) // non-standardized, decided internally by its city ; R2 R9 M1...
         // + city of use to differentiate sigla
         // => to be extracted to another Model, meaning we'll add its uri/id :
         //.addField(new DCResourceField("destinationOfUse:destinationOfUseUri", "destinationOfUse:destinationOfUse_0", true, 100)) // useful ?
         
         // about public act (of the mayor) that defines this destination :
         .addField(new DCField("cityarea:dcc", "string", true, 100)) // public act (of the mayor) that defines this destination ex. 2008/04/01-61
         // => to be extracted to another Model, meaning we'll add its uri/id :
         //.addField(new DCResourceField("cityarea:dccUri", "cityarea:cityDcc_0", true, 100)) // useful ?
         
         // about urban area ;
         .addField(new DCField("cityarea:geometry", "string", true, 100)) // shape of the urban area ; WKS ; used as identifier
         ;
   }


   @Override
   public void do2ExternalizeModelAsMixins() {
      
      DCModel atecoModel = (DCModel) new DCModel("ateco:ateco_0")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(new DCField("ateco:atecoCode", "string", true, 100)) // ex. "1.2" or "1.2.3" (in turkish : "" & "NACE" comes from an international one)
         .addField(new DCField("ateco:atecoDescription", "string", true, 100));

      DCMixin atecoReferencingMixin = (DCMixin) new DCMixin("ecoact:ateco_0_ref_0")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(atecoModel.getField("ateco:atecoCode")) // ex. "1.2" or "1.2.3" (in turkish : "" & "NACE" comes from an international one)
         .addField(atecoModel.getField("ateco:atecoDescription")) // 1000s ; ex. "Commercio al dettaglio di articoli sportivi e per il tempo libero"
         .addField(new DCResourceField("ateco:ateco", "ateco_0", true, 100));

      DCModel municipalityModel = (DCModel) new DCModel("municipality:municipality_0") // "italianCityMixin"
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(new DCField("municipality:municipality", "string", true, 100)); // ex. COLLEGNO ; only description (display name) and not toponimo;

      DCMixin municipalityReferencingMixin = (DCMixin) new DCMixin("ecoact:municipality_0_ref_0")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(municipalityModel.getField("municipality")) // ex. COLLEGNO ; only description (display name) and not toponimo
         // => to be extracted to another Model, therefore add its uri/id :
         .addField(new DCResourceField("ecoact:municipalityUri", "municipality:municipality_0", true, 100));
         
      ///DCModel economicalActivityModel = (DCModel) new DCMixin("economicalActivityMixin"); // "italianCompanyMixin"
      DCModel economicalActivityModel = (DCModel) new DCModel("ecoact:economicalActivity_0") // OR on company model !!!!!!!!!
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
            
            // about company :
            .addField(new DCField("ecoact:codiceFiscale", "string", true, 100)) // ex. "GNTLMR89S61L219Q" ; used as identifier
            // => might be extracted to another a generic Italian company Mixin on this Model
            .addField(new DCField("ecoact:companyName", "string", true, 100)) // 
            .addField(new DCField("ecoact:address", "string", true, 100)) // AND NOT SEVERAL FIELDS ! ex. VIA COSTA ANDREA 3D ; HARD TO RECONCILE !!!!!! ; might be a standardized "address" field
            .addField(new DCField("ecoact:geometry", "string", true, 100)) // point ; WKS ; might be used as identifier ; might be a standardized "geo" field
            // => might be extracted to another a generic company Mixin on this Model
            // but referencing of another (company or codiceFiscale) Model since it's
            // already thought out to be the same as this one
         
            // about company (ateco) type :
            .addMixin(atecoReferencingMixin)
            
            // about city :
            .addMixin(municipalityReferencingMixin)
         ;
   }

   
   @Override
   public void do2CreateModels(List<DCModelBase> modelsToCreate) {
      
      DCModel atecoModel = (DCModel) new DCModel("coita:ateco_0")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(new DCField("coita:atecoCode", "string", true, 100)) // ex. "1.2" or "1.2.3" (in turkish : "" & "NACE" comes from an international one)
         .addField(new DCField("coita:atecoDescription", "string", true, 100)); // 1000s ; ex. "Commercio al dettaglio di articoli sportivi e per il tempo libero"
      atecoModel.setDocumentation("id = !coita:atecoCode");

      DCMixin atecoReferencingMixin = createReferencingMixin(atecoModel, "coita:atecoCode", "coita:atecoDescription");

      DCModel countryModel = (DCModel) new DCModel("plo:country_0")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         .addField(new DCField("plo:name", "string", true, 100)) // ex. ITALY
         .addField(new DCI18nField("plo:name_i18n", 100)); // TODO LATER "required" default language
      countryModel.setDocumentation("id = !plo:name");

      DCMixin countryReferencingMixin = createReferencingMixin(countryModel, "plo:name");

      DCModel cityModel = (DCModel) new DCModel("pli:city_0") // more fields in an "italianCityMixin" ?
         .addField(new DCField("pli:name", "string", true, 100)) // ex. COLLEGNO ; only description (display name) and not toponimo
         .addField(new DCI18nField("pli:name_i18n", 100)) // TODO LATER "required" default language
         .addMixin(countryReferencingMixin);
      cityModel.setDocumentation("id = !plo:name + '/' + !pli:name");

      /*DCMixin cityReferencingMixin = (DCMixin) new DCMixin("pli:city/0_ref/0")
         .addField(cityModel.getField("pli:name")) // ex. COLLEGNO ; only description (display name) and not toponimo
         // => to be extracted to another Model, therefore add its uri/id :
         .addField(new DCResourceField("pli:city", "pli:city/0", true, 100))
         .addMixin(countryReferencingMixin);*/
      DCMixin cityReferencingMixin = createReferencingMixin(cityModel, "pli:name");
      
      DCMixin placeMixin = (DCMixin) new DCMixin("pl:place_0")
         .addField(new DCField("pl:name", "string", true, 100)) // NB. same name, address & geometry as all places,
         // for easy lookup over all kind of places. NB. company may have additional "raison social" field.
         // NB. there could be an additional, even more generic o:displayName or dc:title field
         // that is computed from pl:name but also person:firstName + ' ' + person:lastName
         // for easy lookup by display name over all data.
         .addField(new DCField("pl:address", "string", true, 100)) // AND NOT SEVERAL FIELDS ! ex. VIA COSTA ANDREA 3D ; HARD TO RECONCILE !!!!!! ; might be a standardized "address" field
         .addField(new DCField("pl:geo", "string", true, 100)) // point ; WKS ; might be used as identifier ; might be a standardized "geo" field
         // => might be extracted to another a generic company Mixin on this Model
         // but referencing of another (company or codiceFiscale) Model since it's
         // already thought out to be the same as this one
         .addMixin(cityReferencingMixin);
      
      DCModel companyModel = (DCModel) new DCModel("co:company_0")
         // about place & city (& country) :
         .addMixin(placeMixin);
         
      ///DCModel economicalActivityModel = (DCModel) new DCMixin("economicalActivityMixin/0");
      DCMixin italianCompanyMixin = (DCMixin) new DCMixin("coit:company_0")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
            
            // about italian company :
            .addField(new DCField("coit:codiceFiscale", "string", true, 100)) // ex. "GNTLMR89S61L219Q" ; used as identifier
            // => might be extracted to another a generic Italian company Mixin on this Model
         
            // about italian company (ateco) type :
            .addMixin(atecoReferencingMixin)
         ;
      companyModel.addMixin(italianCompanyMixin);
      
      
      

      DCMixin italianCityMixin = (DCMixin) new DCMixin("pliit:city_0")
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         // about italian city :
         .addField(new DCField("pliit:ISTAT", "int", false, 100)); // ex. "1272" 4 digits ; not required else can't collaborate
      cityModel.addMixin(italianCityMixin);

      DCMixin italianCityReferencingMixin = createReferencingMixin(cityModel, cityReferencingMixin,
            italianCityMixin, true, "pliit:ISTAT");

      DCModel italianUrbanAreaDestinationOfUseModel = (DCModel) new DCModel("cityareauseit:urbanAreaDestinationOfUse_0") // OR urbanArea
         .addField(new DCField("cityareauseit:normalizedCode", "string", true, 100)) // COD_N "1,1", "2,4", "6", "6,32" ;
         // COULD BE float if transformed but not much useful ; TODO international ??
         .addField(new DCField("cityareauseit:description", "string", true, 100)) // ex. "rezidentiale consolidato" ; TODO international ?!
         .addField(new DCField("cityareauseit:sigla", "string", true, 100)) // non-standardized, decided internally by its city ; R2 R9 M1...
          // + city of use to differentiate sigla
         .addMixin(italianCityReferencingMixin);
      italianUrbanAreaDestinationOfUseModel.setDocumentation("id = !1cityareauseit:normalizedCode + '/' + !pli:name + '/' + !cityareauseit:sigla");

      DCMixin italianUrbanAreaDestinationOfUseReferencingMixin = createReferencingMixin(italianUrbanAreaDestinationOfUseModel,
         "cityareauseit:normalizedCode", "cityareauseit:description", "cityareauseit:sigla");
      
      DCMixin placeShapeMixin = (DCMixin) new DCMixin("pls:placeShape_0") // LATER ONCE GIS will allow to look among all shapes
         .addField(new DCField("pls:geo", "string", true, 100)); // shape of the area ; WKS 

      DCMixin cityAreaItDCMixin = (DCMixin) new DCMixin("cityareait:italianCityArea_0") // (cityPlanning) OR cityArea, urbanArea
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
         
         // about italian destination of use & italian city :
         .addMixin(italianUrbanAreaDestinationOfUseReferencingMixin)
         
         // about public act (of the mayor) that defines this destination :
         .addField(new DCField("cityareait:dcc", "string", true, 100)); // public act (of the mayor) that defines this destination ex. 2008/04/01-61
         ///.addField(new DCResourceField("dccUri", "cityDcc", true, 100)) // useful ?
      
      DCModel cityAreaModel = (DCModel) new DCModel("cityarea:cityArea_0") // (cityPlanning) OR cityArea, urbanArea
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
    * Modelling methodology step 3. :
    * 
    * 3. init reference Data (i.e. that is not managed by the application and provided among sample data).
    * 
    * BEWARE, the person / organization doing this operation becomes the data "owner"
    * and must maintain it over time by re-doing this operation regularly !
    * 
    * That's why, in an ideal world, all data should be managed by the application including
    * reference data, or have a clearly defined, existing provider that the application knowingly
    * depends on (but here we're trying to define samples without such dependencies).
    * 
    * However, until this time, reference Data is allowed be inited separately,
    * typically at application "install" (at worst init, if possible deploy) time.
    * 
    * @throws Exception
    */
   @Override
   public void do3FillReferenceData() throws Exception {
      List<DCResource> resourcesToPost = new ArrayList<DCResource>();
      
      // country :
      
      //List<DCEntity> country = ldpEntityQueryService.findDataInType(atecoModel, new HashMap<String,List<String>>() {{
      //         put("coita:atecoDescription", new ArrayList<String>() {{ add((String) company.get("coita:atecoDescription")); }}); }}, 0, 1);
      String countryUri = UriHelper.buildUri(containerUrl, "plo:country_0", "Italia");
      try {
         resourceService.get(countryUri, "plo:country_0");
      } catch (ResourceNotFoundException rnfex) {
         /*if (Response.Status.NOT_FOUND.getStatusCode() != waex.getResponse().getStatus()) {
            throw new RuntimeException("Unexpected error", waex);
         }*/
         DCResource country = DCResource.create(countryUri)
               .set("plo:name", "Italia")
               .set("plo:name_i18n", DCResource.listBuilder()
                     .add(DCResource.propertiesBuilder().put("l", "it").put("v", "Italia").build())
                     .add(DCResource.propertiesBuilder().put("l", "en").put("v", "Italy").build())
                     .add(DCResource.propertiesBuilder().put("l", "fr").put("v", "Italie").build())
                     .build());
         // once props are complete, build URI out of them and schedule post :
         ///country.setUriFromId(containerUrl, (String) country.get("plo:name"));
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
            DCResource ateco = DCResource.create(null, "coita:ateco_0")
                  .set("coita:atecoCode", line[0])
                  .set("coita:atecoDescription", line[1]);
            
            // once props are complete, build URI out of them and schedule post :
            ateco.setUriFromId(containerUrl, (String) ateco.get("coita:atecoCode"));
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
    * Modelling methodology step 4. :
    * 
    * 4. fill sample data
    * 
    * - Economical Activity sample
    */
   @Override
   public void do4FillSampleData() {
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
            final DCResource company = DCResource.create(null, "co:company_0")
                  .set("coit:codiceFiscale", line[0])
                  //.set("coita:atecoCode", line[2]) // has to be retrieved
                  .set("coita:atecoDescription", line[2])
                  .set("pl:name", line[1])
                  .set("pl:address", line[4])
                  .set("pl:geo", "") // setting dummy for now because required
                  .set("pli:name", line[3])
                  /*.set("pli:name_i18n", DCResource.listBuilder() // TODO copy it from referenced Resource if any
                        .add(DCResource.propertiesBuilder().put("l", "it").put("v", company.get("pli:name").build())
                        .build())*/
                  .set("plo:name", "Italia"); // hardcoded
                  /*.set("plo:name_i18n", DCResource.listBuilder() // TODO copy it from referenced Resource
                        .add(DCResource.propertiesBuilder().put("l", "it").put("v", "Italia").build())
                        .add(DCResource.propertiesBuilder().put("l", "en").put("v", "Italy").build())
                        .add(DCResource.propertiesBuilder().put("l", "fr").put("v", "Italie").build())
                        .build());*/
            
            // filling company's resource props and missing referencing Mixin props : 
            // - by building URI from known id/iri if no missing referencing Mixin prop,
            // - or by looking up referenced resource with another field as criteria
            
            // NB. single Italia country has to be filled at "install" time
            company.set("plo:country", UriHelper.buildUri(containerUrl, "plo:country_0",
                  (String) company.get("plo:name")));

            company.set("pli:city", UriHelper.buildUri(containerUrl, "pli:city_0",
                  (String) company.get("plo:name") + '/' + (String) company.get("pli:name")));

            // NB. ateco Model has to be filled at "install" time else code is not known
            List<DCEntity> atecos = ldpEntityQueryService.findDataInType("coita:ateco_0",
                  new ImmutableMap.Builder<String, List<String>>().put("coita:atecoDescription",
                        new ImmutableList.Builder<String>().add((String) company.get("coita:atecoDescription")).build()).build(), 0, 1);
            DCResource ateco;
            if (atecos != null && !atecos.isEmpty()) {
               ateco = resourceEntityMapperService.entityToResource(atecos.get(0), null, false);
            } else {
               ///throw new RuntimeException("Unknown ateco description " + company.get("coita:atecoDescription"));
               // WORKAROUND TO WRONG DESCRIPTIONS : (!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!)
               // filling company's provided props :
               ateco = DCResource.create(null, "coita:ateco_0")
                     .set("coita:atecoCode", ((String) company.get("coita:atecoDescription")).replace(' ', '_'))
                     .set("coita:atecoDescription", company.get("coita:atecoDescription"));
               // once props are complete, build URI out of them and post :
               ateco.setUriFromId(containerUrl, (String) ateco.get("coita:atecoCode"));
               /*datacoreApiClient.*/postDataInType(ateco);
               //resourcesToPost.add(ateco); // post and NOT schedule else can't be found in loop
            }
            company.set("coita:atecoCode", ateco.get("coita:atecoCode"));
            company.set("coita:ateco", ateco.getUri());

            
            // filling other Models that this table is a source of :
            
            try {
               resourceService.get((String) company.get("pli:city"), "pli:city_0");
            } catch (ResourceNotFoundException rnfex) {
               /*if (Response.Status.NOT_FOUND.getStatusCode() != waex.getResponse().getStatus()) {
                  throw new RuntimeException("Unexpected error", waex.getResponse().getEntity());
               }*/
               DCResource city = DCResource.create((String) company.get("pli:city"))
                     .set("pli:name", company.get("pli:name"))
                     .set("pli:name_i18n", DCResource.listBuilder()
                           .add(DCResource.propertiesBuilder().put("l", "it").put("v", company.get("pli:name")).build())
                           .build())
                     .set("plo:name", (String) company.get("plo:name"))
                     /*.set("plo:name_i18n", DCResource.listBuilder() // TODO copy it from referenced Resource
                        .add(DCResource.propertiesBuilder().put("l", "it").put("v", "Italia").build())
                        .add(DCResource.propertiesBuilder().put("l", "en").put("v", "Italy").build())
                        .add(DCResource.propertiesBuilder().put("l", "fr").put("v", "Italie").build())
                        .build())*/
                     .set("plo:country", (String) company.get("plo:country"));
               // once props are complete, build URI out of them and (schedule) post :
               ///city.setUriFromId(containerUrl, (String) company.get("plo:name") + '/' + (String) company.get("pli:name"));
               /*datacoreApiClient.*/postDataInType(city);
               //resourcesToPost.add(city); // BEWARE must be posted before company else resource reference check fails
            }

            
            // once props are complete, build URI out of them and schedule post :
            
            company.setUri(UriHelper.buildUri(containerUrl, "co:company_0",
                  (String) company.get("plo:name") + '/' + (String) company.get("coit:codiceFiscale")));
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
    * Modelling methodology step 4. :
    * 
    * 4. fill sample data
    * 
    * - CityPlanning sample
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
            final DCResource cityArea = DCResource.create(null, "cityarea:cityArea_0")
                  .set("pls:geo", line[6]) // (dummy data for now)
                  .set("cityareait:dcc", line[5])
                  .set("cityareauseit:normalizedCode", line[2])
                  .set("cityareauseit:description", line[3])
                  .set("cityareauseit:sigla", line[4])
                  .set("pli:name", line[1])
                  .set("pliit:ISTAT", Integer.parseInt(line[0])) // int !
                  .set("plo:name", "Italia"); // hardcoded
            
            
            // filling company's resource props and missing referencing Mixin props : 
            // - by building URI from known id/iri if no missing referencing Mixin prop,
            // - or by looking up referenced resource with another field as criteria
            
            // NB. single Italia country has to be filled at "install" time
            cityArea.set("plo:country", UriHelper.buildUri(containerUrl, "plo:country_0",
                  (String) cityArea.get("plo:name")));

            cityArea.set("pli:city", UriHelper.buildUri(containerUrl, "pli:city_0",
                  (String) cityArea.get("plo:name") + '/' + (String) cityArea.get("pli:name")));

            cityArea.set("cityareauseit:urbanAreaDestinationOfUse", UriHelper.buildUri(containerUrl,
                  "cityareauseit:urbanAreaDestinationOfUse_0", (String) cityArea.get("cityareauseit:normalizedCode")
                  + '/' + (String) cityArea.get("pli:name") + '/' + (String) cityArea.get("cityareauseit:sigla")));

            
            // filling other Models that this table is a source of :
            
            try {
               DCResource city = resourceService.get((String) cityArea.get("pli:city"), "pli:city_0");
               if (city != null) {
                  if (!cityArea.get("pliit:ISTAT").equals(city.get("pliit:ISTAT"))) { // TODO generic diff
                     // TODO if not owner, submit change IF NOT YET SUBMITTED !!!!!!!
                     city.set("pliit:ISTAT", cityArea.get("pliit:ISTAT"));
                     /*datacoreApiClient.*/postDataInType(city);
                     //resourcesToPost.add(city); // could be rather scheduled
                  }
               }
            } catch (ResourceNotFoundException rnfex) {
               ///if (Response.Status.NOT_FOUND.getStatusCode() != waex.getResponse().getStatus()) {
               ///   throw new RuntimeException("Unexpected error", waex.getResponse().getEntity());
               ///}
               DCResource city = DCResource.create((String) cityArea.get("pli:city"))
                     .set("pli:name", cityArea.get("pli:name"))
                     .set("pliit:ISTAT", cityArea.get("pliit:ISTAT"))
                     .set("plo:name", cityArea.get("plo:name"))
                     .set("pli:name_i18n", DCResource.listBuilder()
                           .add(DCResource.propertiesBuilder().put("l", "it").put("v", cityArea.get("pli:name")).build())
                           .build())
                     .set("plo:country", cityArea.get("plo:country"));
               // once props are complete, build URI out of them and (schedule) post :
               ///city.setUriFromId(containerUrl, (String) company.get("plo:name") + '/' + (String) company.get("pli:name"));
               /*datacoreApiClient.*/postDataInType(city);
               //resourcesToPost.add(city); // BEWARE must be posted before company else resource reference check fails
            }
            
            try {
               resourceService.get((String) cityArea.get("cityareauseit:urbanAreaDestinationOfUse"), "cityareauseit:urbanAreaDestinationOfUse_0");
            } catch (ResourceNotFoundException rnfex) {
               /*if (Response.Status.NOT_FOUND.getStatusCode() != waex.getResponse().getStatus()) {
                  throw new RuntimeException("Unexpected error", waex.getResponse().getEntity());
               }*/
               DCResource cityAreaUseIt = DCResource.create((String) cityArea.get("cityareauseit:urbanAreaDestinationOfUse"))
                     .set("cityareauseit:normalizedCode", cityArea.get("cityareauseit:normalizedCode"))
                     .set("cityareauseit:description", cityArea.get("cityareauseit:description"))
                     .set("cityareauseit:sigla", cityArea.get("cityareauseit:sigla"))
                     .set("pli:name", cityArea.get("pli:name"))
                     .set("pli:city", cityArea.get("pli:city"))
                     .set("plo:name", cityArea.get("plo:name"))
                     .set("plo:country", cityArea.get("plo:country"));
               // once props are complete, build URI out of them and (schedule) post :
               ///city.setUriFromId(containerUrl, (String) company.get("plo:name") + '/' + (String) company.get("pli:name"));
               /*datacoreApiClient.*/postDataInType(cityAreaUseIt);
               //resourcesToPost.add(cityAreaUseIt); // BEWARE must be posted before company else resource reference check fails
            }

            
            // once props are complete, build URI out of them and schedule post :
            
            cityArea.setUri(UriHelper.buildUri(containerUrl, "cityarea:cityArea_0",
                  (String) cityArea.get("plo:name") + '/' + (String) cityArea.get("pli:name")
                  + '/' + generateId((String) cityArea.get("pls:geo")))); // OR only hash ?
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

   
}
