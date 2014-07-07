package org.oasis.datacore.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;


/**
 * 
 * @author mdutoo
 *
 */
@Component
public class ResourceModelIniter extends DatacoreSampleBase {
   
   /** to be able to build a full uri, to avoid using ResourceService */
   ///@Value("${datacoreApiClient.baseUrl}") 
   ///private String baseUrl; // useless
   /////@Value("${datacoreApiClient.containerUrl}") // DOESN'T WORK 
   @Value("${datacoreApiServer.containerUrl}")
   private String containerUrl = "";

   
   /** after all models */
   @Override
   public int getOrder() {
      return 100000;
   }

   @Override
   public void doInit() {
      DCModel metaModel = (DCModel) new DCModel("dcmo:model")
         // TODO security
         .addField(new DCField("dcmo:name", "string", true, 100))
         .addField(new DCField("dcmo:majorVersion", "long", true, 100)) // don't index and rather lookup on URI ??
         .addField(new DCField("dcmo:collectionName", "string", true, 100))
         .addField(new DCField("dcmo:maxScan", "int")) // not "required"
         // NB. Resource version is finer but NOT the minorVersion of the majorVersion
         .addField(new DCField("dcmo:documentation", "string", true, 100)) // TODO in another collection for performance
         .addField(new DCListField("dcmo:fields", addFieldFields(new DCMapField("useless"))))
         .addField(new DCListField("dcmo:mixins", new DCField("useless", "string")))
         .addField(new DCField("dcmo:isHistorizable", "boolean", true, 100))
         .addField(new DCField("dcmo:isContributable", "boolean", true, 100))
         
         // caches :
         .addField(new DCListField("dcmo:globalFields", addFieldFields(new DCMapField("useless")))) // TODO polymorphism
         .addField(new DCListField("dcmo:globalMixins", new DCField("useless", "string")))
         // embedded mixins, globalMixins ???
         // & listeners ??
         ;
      metaModel.setDocumentation("id = name + '/' + version");
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
      + "\"name\": \"France\" }");*/

      DCModel mixinModel = (DCModel) new DCModel("dcmi:name")
         // TODO maxScan, security, collectionName
         .addField(new DCField("dcmi:name", "string", true, 100));
      
      // TODO prefixes & namespaces, fields ???
      
      super.createModelsAndCleanTheirData(metaModel, mixinModel); // TODO also Mixins ???
      
      doInitData();
   }

   private DCMapField addFieldFields(DCMapField mapField) { // TODO polymorphism
      return addFieldFields(mapField, 3);
   }
   private DCMapField addFieldFields(DCMapField mapField, int depth) { // TODO polymorphism
      if (depth == 0) {
         return mapField;
      }
      mapField
         .addField(new DCField("dcmf:name", "string", true, 100))
         .addField(new DCField("dcmf:type", "string", true, 100))
         .addField(new DCField("dcmf:required", "boolean")) // defaults to false, indexing would bring not much
         .addField(new DCField("dcmf:queryLimit", "int")) // defaults to 0, indexing would bring not much
         .addField(addFieldFields(new DCMapField("dcmf:listElementField"),
               depth - 1)) // TODO allow (map) field to reuse Mixin to allow trees
         .addField(new DCListField("dcmf:mapFields", addFieldFields(new DCMapField("useless"),
               depth - 1))) // TODO allow map field to reuse Mixin to allow trees
         .addField(new DCField("dcmf:resourceType", "string", false, 100)) // "required" would required polymorphism ; TODO rather "resource" type ?!
      ;
      return mapField;
   }

   
   
   @Override
   public void doInitData() {
      List<DCResource> resourcesToPost = new ArrayList<DCResource>();
      
      for (DCModel model : modelAdminService.getModels()) {
         
         // filling company's provided props :
         final DCResource modelResource = DCResource.create(null, "dcmo:model")
               .set("dcmo:name", model.getName())
               .set("dcmo:majorVersion", model.getMajorVersion())
               // NB. minor version is Resource version
               .set("dcmo:documentation", model.getDocumentation())// TODO in another collection for performance
               .set("dcmo:collectionName", model.getCollectionName())
               .set("dcmo:maxScan", model.getMaxScan())
               // TODO security
               .set("dcmo:fields", fieldsToProps(model.getFieldMap()))
               .set("dcmo:isHistorizable", model.isHistorizable())
               .set("dcmo:isContributable", model.isContributable())
               ;
         //modelResource.setVersion(model.getVersion()); // not at creation (or < 0),
         // rather update DCModel.version from its resource after each put
         
         ImmutableList.Builder<Object> mixinsPropBuilder = DCResource.listBuilder();
         for (DCModelBase mixin : model.getMixins()) {
            mixinsPropBuilder.add(mixin.getName());
         }
         modelResource.set("dcmo:mixins", mixinsPropBuilder.build());
         
         // caches :
         modelResource.set("dcmo:globalMixins",
               new ArrayList<String>(model.getGlobalMixinNameSet())); // TODO order
         modelResource.set("dcmo:globalFields", fieldsToProps(model.getGlobalFieldMap()));
         
         // filling company's resource props and missing referencing Mixin props : 
         // - by building URI from known id/iri if no missing referencing Mixin prop,
         // - or by looking up referenced resource with another field as criteria
         
         /*
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
            /datacoreApiClient./postDataInType(ateco);
            //resourcesToPost.add(ateco); // post and NOT schedule else can't be found in loop
         }
         company.set("!coita:atecoCode", ateco.get("!coita:atecoCode"));
         company.set("!coita:ateco", ateco.getUri());
         */

         
         // filling other Models that this table is a source of :
         // TODO mixins ; fields ??
         /*
         try {
            resourceService.get((String) company.get("!pli:city"), "!pli:city");
         } catch (ResourceNotFoundException rnfex) {
            /if (Response.Status.NOT_FOUND.getStatusCode() != waex.getResponse().getStatus()) {
               throw new RuntimeException("Unexpected error", waex.getResponse().getEntity());
            }/
            DCResource city = DCResource.create((String) company.get("!pli:city"))
                  .set("!pli:city_name", company.get("!pli:city_name"))
                  .set("!plo:country_name", (String) company.get("!plo:country_name"))
                  .set("!plo:country", (String) company.get("!plo:country"));
            // once props are complete, build URI out of them and (schedule) post :
            ///city.setUriFromId(containerUrl, (String) company.get("!plo:country_name") + '/' + (String) company.get("!pli:city_name"));
            /datacoreApiClient./postDataInType(city);
            //resourcesToPost.add(city); // BEWARE must be posted before company else resource reference check fails
         }
         */

         
         // once props are complete, build URI out of them and schedule post :
         
         modelResource.setUri(UriHelper.buildUri(containerUrl, "dcmo:model",
               (String) modelResource.get("dcmo:name") + '/' + modelResource.get("dcmo:majorVersion")));
         resourcesToPost.add(modelResource);
         
      }

      for (DCResource resource : resourcesToPost) {
         if ("dcmo:model".equals(resource.get("dcmo:name"))) {
            // for now non-recursive metaModel can't be posted,
            // LATER do it to document it (what is queriable etc.)
            continue;
         }
         /*datacoreApiClient.*/postDataInType(resource);
      }
   }


   private ImmutableList<Object> fieldsToProps(Map<String, DCField> fieldMap) {
      // TODO order !!
      return fieldsToProps(fieldMap, new ArrayList<String>(fieldMap.keySet()));
   }
   private ImmutableList<Object> fieldsToProps(Map<String, DCField> mapFields,
         List<String> mapFieldNames) {
      ImmutableList.Builder<Object> fieldsPropBuilder = DCResource.listBuilder();
      for (String fieldName : mapFieldNames) {
         DCField field = mapFields.get(fieldName);
         fieldsPropBuilder.add(fieldToProps(field));
      }
      return fieldsPropBuilder.build();
   }
   private Map<String, Object> fieldToProps(DCField field) {
      ImmutableMap.Builder<String, Object> fieldPropBuilder = DCResource.propertiesBuilder()
            .put("dcmf:name", field.getName())
            .put("dcmf:type", field.getType())
            .put("dcmf:required", field.isRequired()) // TODO nor for list ; for map ?!?
            .put("dcmf:queryLimit", field.getQueryLimit()); // TODO not for list, map
      switch(field.getType()) {
      case "map" :
         DCMapField mapField = (DCMapField) field;
         fieldPropBuilder.put("dcmf:mapFields",
               fieldsToProps(mapField.getMapFields(), mapField.getMapFieldNames()));
         break;
      case "list" :
         fieldPropBuilder.put("dcmf:listElementField",
               fieldToProps(((DCListField) field).getListElementField()));
         break;
      case "resource" :
         if (!(field instanceof DCResourceField)) { // to debug pb
            throw new RuntimeException("Not a DCResourceField : " + field.toString());
         }
         fieldPropBuilder.put("dcmf:resourceType", ((DCResourceField) field).getResourceType());
         break;
      }
      return fieldPropBuilder.build();
   }


   
}
