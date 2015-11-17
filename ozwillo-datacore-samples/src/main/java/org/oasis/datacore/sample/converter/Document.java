package org.oasis.datacore.sample.converter;

import java.util.List;

import org.springframework.data.annotation.Id;


/**
 * Sample for custom converter
 * 
 * @author mdutoo
 *
 */
@org.springframework.data.mongodb.core.mapping.Document(collection = "documents")
public class Document {

   @Id
   private String id;
   private String uri;
   private String name;
   private List<Comment> comments; // To test embedded / subelements and how mongo lifecycle events pan out with them
   
   public String getId() {
      return id;
   }
   public void setId(String id) {
      this.id = id;
   }
   public String getUri() {
      return uri;
   }
   public void setUri(String uri) {
      this.uri = uri;
   }
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public List<Comment> getComments() {
      return comments;
   }
   public void setComments(List<Comment> comments) {
      this.comments = comments;
   }
   
}
