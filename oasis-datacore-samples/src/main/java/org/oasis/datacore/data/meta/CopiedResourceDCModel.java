package org.oasis.datacore.data.meta;

public class CopiedResourceDCModel {

   private DCResourceModel targetModel; // TODO or id & retrieval using service ?? NO will let spring data handle it

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
