package org.oasis.datacore.rest.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;


/**
 * A Datacore data Resource.
 * This class is the Java support for producing JSON-LD-like JSON out of Datacore data.
 * @author mdutoo
 *
 */
@ApiModel(value = "A Datacore data Resource")
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class DCResource {
   
   @ApiModelProperty(value = "URI", position=0, required=true)
   @JsonProperty
   private String uri;
   /*@JsonProperty
   private String iri; // TODO in addition to uri ?? test conflict !
   @JsonProperty
   private String type; // TODO types ??? in addition to uri ? or model ?!? test conflict !*/
   @ApiModelProperty(value = "version", position=1, notes="The server's up-to-date version must "
         + "be provided (save when creating it), otherwise it will fail due to optimistic locking.")
   @JsonProperty
   private Long version;

   /** types : model plus type mixins */
   @JsonProperty
   private List<String> types;

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
   }
   public DCResource(Map<String,Object> properties) {
      this.properties = properties;
   }

   // TODO to unmarshall embedded resources as DC(Sub)Resource rather than HashMaps
   // (and if possible same for embedded maps ???) BUT can't know when is embedded resource or map
   ///@JsonSubTypes({ @JsonSubTypes.Type(String.class), @JsonSubTypes.Type(Boolean.class),
   ///   @JsonSubTypes.Type(Double.class),
   ///   @JsonSubTypes.Type(DCSubResource(Map).class), @JsonSubTypes.Type(DCList.class) })
   @JsonAnyGetter
   public Map<String, Object> getProperties() {
      return this.properties;
   }
   ///@JsonSubTypes({ @JsonSubTypes.Type(String.class), @JsonSubTypes.Type(Boolean.class),
   ///   @JsonSubTypes.Type(Double.class),
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
   /*public String getIri() {
      return iri;
   }
   public void setIri(String iri) {
      this.iri = iri;
   }
   public String getType() {
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
   
   public String toString() {
      try {
         return new ObjectMapper().writeValueAsString(this);
      } catch (JsonProcessingException e) {
         return "DCResource[" + this.uri + " , bad json]";
      }
   }

}
