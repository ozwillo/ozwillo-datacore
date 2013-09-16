package org.oasis.datacore.data.meta;

public class CopiedSubresourceDCModel extends CopiedResourceDCModel {

   private String attribute;

   /** for persistence fw */
   public CopiedSubresourceDCModel() {
      super();
   }

   public CopiedSubresourceDCModel(String attribute, DCResourceModel targetModel) {
      super(targetModel);
      this.attribute = attribute;
   }

   public String getAttribute() {
      return attribute;
   }

   public void setAttribute(String attribute) {
      this.attribute = attribute;
   }
   
}
