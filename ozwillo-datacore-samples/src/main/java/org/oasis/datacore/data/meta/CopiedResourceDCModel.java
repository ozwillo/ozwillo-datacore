package org.oasis.datacore.data.meta;

import java.util.List;
import java.util.Set;

public class CopiedResourceDCModel {

   private DCResourceModel targetModel; // TODO or id & retrieval using service ?? NO will let spring data handle it
   private List<String> fieldsToCopy; // for partial copy
   private Set<String> fieldsNotToCopy; // TODO helps for partial copy ?? other helpers ?
   
   /** for persistence fw */
   public CopiedResourceDCModel() {
      
   }
   
   public CopiedResourceDCModel(DCResourceModel targetModel) {
      this.targetModel = targetModel;
   }

   public DCResourceModel getTargetModel() {
      return targetModel;
   }

   public void setTargetModel(DCResourceModel targetModel) {
      this.targetModel = targetModel;
   }
   
}
