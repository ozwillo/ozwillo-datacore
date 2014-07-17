package org.oasis.datacore.core.meta.model;

public class DCI18nField extends DCListField{
   
   public DCI18nField() {
      
   }
   
   /**
    * TODO LATER define how "required" applies to i18n (ex. required only for model/resource "default" language)
    * @param name
    */
   public DCI18nField(String name, int queryLimit) {
      super(name, "i18n", createI18nMap(queryLimit));
      /*super(name, "i18", false, 0);
      
      DCMapField i18Map = new DCMapField("zzz");
      i18Map.addField(new DCField("v", "string", true, 100));
      i18Map.addField(new DCField("l", "string", false, 0));
      this.list = new DCListField("i18List", i18Map);*/
   }
   
   private static DCMapField createI18nMap(int queryLimit) {
      DCMapField i18Map = new DCMapField("i18nMap"); // NB. this map name is meaningless
      i18Map.addField(new DCField("v", "string", true, queryLimit));
      i18Map.addField(new DCField("l", "string", false, 0));
      return i18Map;
   }
   
   
   // NOO rather in DCResource & ResourceService
   public void addTranslation(String lang, String text) {
      //HashMap<String, String> mapField = new HashMap<String, String>();
      //new DCField("t", "string", false, 10);
      //new DCField("v", "string", false, 10);
   }
   
   
}
