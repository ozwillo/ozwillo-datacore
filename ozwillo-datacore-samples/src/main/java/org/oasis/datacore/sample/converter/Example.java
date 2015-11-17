package org.oasis.datacore.sample.converter;



/**
 * Sample for custom converter
 * 
 * @author mdutoo
 *
 */
public class Example {

   // no annotations because would be useless since saved by converter
   private String id;
   private String text;
   private Document about;
   private String aboutUri; // NB. not required, just to ease up testing
   
   public String getId() {
      return id;
   }
   public void setId(String id) {
      this.id = id;
   }
   public String getText() {
      return text;
   }
   public void setText(String text) {
      this.text = text;
   }
   public Document getAbout() {
      return about;
   }
   public void setAbout(Document about) {
      this.about = about;
   }
   public String getAboutUri() {
      return aboutUri;
   }
   public void setAboutUri(String aboutUri) {
      this.aboutUri = aboutUri;
   }
   
}
