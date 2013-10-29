package org.oasis.datacore.data.meta;

public class DCField {

   private String name; // OPT also storage name (different from JSON name)
   
   private String type; // TODO possible constant values :
   // URI (isPrimary, tells which model or whether other container or social graph)
   // primitives (& joda time) ; Map ; List (WARNING no AND query, rather through join) ; i18n ? geoloc ??
   // optional alternate values (as boolean(s) or dedicated types ?) ??
   
   // TODO isPrimary ; isQueriable / indexable OPT field groups (submodels / aspects ?!) OPT with result size limit  
   private boolean isPrimary; // TODO NO rather model.getKeyFields()
   //private boolean isQueriable; // rather queryLimit == 0
   private int queryLimit = 50; // TODO OPT
   
   // TODO read & write rights, data quality level & source :
   // only on model, OPT or even there as boolean(s) or dedicated types ?

   /**
    * @return http://[container]/type/[type]/id
    */
   @Override
   public String toString() {
      // TODO Jackson (helper)
      return super.toString();
   }
   
}
