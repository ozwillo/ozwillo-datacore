package org.oasis.datacore.rest.api;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.oasis.datacore.rest.api.binding.DatacoreObjectMapper;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.api.util.UriHelper;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;


/**
 * To easily create new Resources FOR TESTING, do :
 * <p/>
 * new DCResource(containerUrl, "my.app.type", "aName").set("name", "aName").set("count", 3);
 * <p/>
 * 
 * A Datacore data Resource.
 * This class is the Java support for producing JSON-LD-like JSON out of Datacore data.
 * 
 * TODO patch date support by Jackson else Caused by: com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException: Unrecognized field "weekOfWeekyear" (class org.joda.time.DateTime)
 *  
 * @author mdutoo
 *
 */
@ApiModel(value = "A Datacore data Resource")
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class DCResource {
   
   @ApiModelProperty(value = "URI", position=0, required=true)
   @JsonProperty
   private String uri;
   @ApiModelProperty(value = "version", position=1, notes="The server's up-to-date version must "
         + "be provided (save when creating it), otherwise it will fail due to optimistic locking.")
   @JsonProperty
   private Long version;

   /** types : model (first one) plus type mixins */
   @JsonProperty("rdf:type")
   private List<String> types;
   //@JsonProperty
   //private String type; // or this ? in addition to uri ? or model ?!? test conflict !
   /** type-relative id ; only available after (ResourceService) inits it or parses it from uri */
   //@JsonProperty
   private transient String id; // TODO or iri ? transient not required... ? in addition to uri ?? test conflict !

   // creation / last modified date, author ? (readonly !)
   @JsonProperty
   private DateTime created;
   @JsonProperty
   private DateTime lastModified;
   @JsonProperty
   private String createdBy;
   @JsonProperty
   private String lastModifiedBy;
   
   /** Other (business) properties. They are of the types supported by JSON (on Jackson) :
    * String, Boolean, Double, Map, List
    * see http://en.wikipedia.org/wiki/JSON#Data_types.2C_syntax_and_example */
   //@JsonIgnore // NO error 204 no content, rather not visible and explicitly @JsonProperty actual fields
   private Map<String,Object> properties;
   
   public DCResource() {
      this.properties = new HashMap<String,Object>();
      this.types = new ArrayList<String>();
   }
   public DCResource(Map<String,Object> properties) {
      this.properties = properties;
      this.types = new ArrayList<String>();
   }
   
   /** helper method to build new DCResources FOR TESTING
    * TODO or in builder instance ? */
   public static DCResource create(String containerUrl, String modelType, String id) {
      DCResource resource = new DCResource();
      resource.setUri(UriHelper.buildUri(containerUrl, modelType, id));
      resource.types.add(modelType); // TODO add mixins !?!
      resource.setId(id);
      return resource;
   }
   /** helper method to build new DCResources FOR TESTING
    * requires id (and uri) to be set later or by event listener in build or POST
    * TODO or in builder instance ? */
   public static DCResource create(String containerUrl, String modelType) {
      DCResource resource = new DCResource();
      resource.types.add(modelType); // TODO add mixins !?!
      return resource;
   }
   /** helper method to build new DCResources FOR TESTING
    * TODO or in builder instance ?
    * @throws URISyntaxException 
    * @throws MalformedURLException */
   public static DCResource create(String uri) throws MalformedURLException, URISyntaxException {
      DCResource resource = new DCResource();
      DCURI dcUri = UriHelper.parseUri(uri);
      String modelType = dcUri.getType();
      resource.getTypes().add(modelType);
      resource.setUri(UriHelper.buildUri(dcUri.getContainer(), modelType, dcUri.getId()));
      return resource;
   }
   /**
    * Helps set URI once props are set
    * @param containerUrl
    * @param id
    * @return
    */
   public DCResource setUriFromId(String containerUrl, String id) {
      this.setUri(UriHelper.buildUri(containerUrl, this.getTypes().get(0), id));
      return this;
   }
   /** helper method to build new DCResources FOR TESTING
    * TODO or in builder instance ? */
   public DCResource addType(String mixinType) {
      this.types.add(mixinType);
      return this;
   }
   /** helper method to build new DCResources FOR TESTING
    * TODO or in builder instance ? */
   public DCResource set(String fieldName, Object fieldValue) {
      this.properties.put(fieldName, fieldValue);
      return this;
   }
   /** shortcut to getProperties().get() */
   public Object get(String fieldName) {
      return this.properties.get(fieldName);
   }
   /***
    * helper method to build new DCResources FOR TESTING ; 
    * copies the given Resource's field that are among the given modelOrMixins
    * (or all if modelOrMixins is null or empty) to this Resource
    * @param source
    * @param modelOrMixins
    * @return
    */
   public DCResource copy(DCResource source, Object ... modelOrMixins) {
      // TODO copy it from IGN sample !!!!!!!!!!!!!!!!!!!!!!
      return this;
   }
   
   /**
    * Helper for building Datacore maps
    * ex. resourceService.propertiesBuilder().put("name", "John").put("age", 18).build()
    * @return
    */
   public static ImmutableMap.Builder<String, Object> propertiesBuilder() {
      return new ImmutableMap.Builder<String, Object>();
   }

   /**
    * Helper for building Datacore lists
    * ex. resourceService.listBuilder().add(landscape.getUri()).add(monument.getUri()).build()
    * @return
    */
   public static ImmutableList.Builder<Object> listBuilder() {
      return new ImmutableList.Builder<Object>();
   }
   

   /**
    * shortcut method
    * @return the first of types if any, or null
    */
   public String getModelType() {
      if (this.types != null && !this.types.isEmpty()) {
         String modelType = this.types.get(0);
         return modelType;
      }
      return null;
   }

   
   // TODO to unmarshall embedded resources as DC(Sub)Resource rather than HashMaps
   // (and if possible same for embedded maps ???) BUT can't know when is embedded resource or map
   @JsonSubTypes({ @JsonSubTypes.Type(String.class), @JsonSubTypes.Type(Boolean.class),
      @JsonSubTypes.Type(Double.class), @JsonSubTypes.Type(DateTime.class),
      @JsonSubTypes.Type(Map.class), @JsonSubTypes.Type(List.class) })
   ///   @JsonSubTypes.Type(DCSubResource(Map).class), @JsonSubTypes.Type(DCList.class) })
   @JsonAnyGetter
   public Map<String, Object> getProperties() {
      return this.properties;
   }
   @JsonSubTypes({ @JsonSubTypes.Type(String.class), @JsonSubTypes.Type(Boolean.class),
      @JsonSubTypes.Type(Double.class), @JsonSubTypes.Type(DateTime.class),
      @JsonSubTypes.Type(Map.class), @JsonSubTypes.Type(List.class) })
   ///   @JsonSubTypes.Type(DCSubResource(Map).class), @JsonSubTypes.Type(DCList.class) })
   @JsonAnySetter
   public void setProperty(String name, Object value) {
      this.properties.put(name, value);
   }
   
   public String getUri() {
      return uri;
   }
   public void setUri(String uri) {
      this.uri = uri;
   }
   public String getId() {
      return id;
   }
   public void setId(String id) {
      this.id = id;
   }
   /*public String getType() {
      return type;
   }
   public void setType(String type) {
      this.type = type;
   }*/
   public Long getVersion() {
      return version;
   }
   public void setVersion(Long version) {
      this.version = version;
   }
   public List<String> getTypes() {
      return types;
   }
   public void setTypes(List<String> types) {
      this.types = types;
   }
   public DateTime getCreated() {
      return created;
   }
   public void setCreated(DateTime created) {
      this.created = created;
   }
   public DateTime getLastModified() {
      return lastModified;
   }
   public void setLastModified(DateTime lastModified) {
      this.lastModified = lastModified;
   }
   public String getCreatedBy() {
      return createdBy;
   }
   public void setCreatedBy(String createdBy) {
      this.createdBy = createdBy;
   }
   public String getLastModifiedBy() {
      return lastModifiedBy;
   }
   public void setLastModifiedBy(String lastModifiedBy) {
      this.lastModifiedBy = lastModifiedBy;
   }

   private static ObjectMapper resourceObjectMapper = new DatacoreObjectMapper();
   public String toString() {
      try {
         return resourceObjectMapper.writeValueAsString(this);
      } catch (JsonProcessingException e) {
         return "DCResource[" + this.uri + " , bad json]";
      }
   }
   
}
