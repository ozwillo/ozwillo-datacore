package org.oasis.datacore.data.meta;

import java.util.ArrayList;
import java.util.List;


/**
 * Models a single use context / case of an (RDF-like) (Open Data) Datacore resource.
 * 
 * @author mdutoo
 *
 */
public class DCResourceModel {

   public static final String DATACORE_BASE_URI = "http://data.ozwillo.com/";
   
   /** Unique ; also serves as collection name (so a model serves for a single collection,
    * because it models a single use context / case, which is the base type of the entity). */
   private String name;
   private ExtendedResourceDCModel extendedResourceModel;
   //private List<ExtendedResourceDCModel> extendedResourceModel; // TODO rather several ones ???!!!
   private List<CopiedSubresourceDCModel> copiedSubresourceModels;
   private List<DCField> fields; // TODO boolean to allow optional alternate values (as boolean(s) or dedicated types ?) ?? 
   private List<DCField> allFields; // TODO OPT (for copy model) is only a cache ; TODO field to orig model inverse link
   // TODO "field groups" (submodels / aspects) of AND / OR same quality, source, rights, queriable / index ??????? 
   
   /** for persistence fw */
   public DCResourceModel() {
      
   }
   
   public DCResourceModel(String name) {
      this.setName(name);
   }
   
   public String toString() {
      StringBuffer sbuf = new StringBuffer(DCResourceModel.class.getSimpleName());
      sbuf.append("[");
      sbuf.append(this.name);
      if (extendedResourceModel != null) {
         sbuf.append(" extends " + extendedResourceModel.getTargetModel().getName());
      }
      if (copiedSubresourceModels != null && !copiedSubresourceModels.isEmpty()) {
         if (extendedResourceModel != null) {
            sbuf.append(" and ");
         }
         sbuf.append(" copies subresources : ");
         toString(sbuf, copiedSubresourceModels.get(0));
         for (int i = 1; i < copiedSubresourceModels.size(); i++) {
            sbuf.append(", ");
            toString(sbuf, copiedSubresourceModels.get(i));
         }
      }
      sbuf.append("]");
      return sbuf.toString();
   }
   private void toString(StringBuffer sbuf, CopiedSubresourceDCModel copiedSubresourceDCModel) {
      sbuf.append(copiedSubresourceDCModel.getAttribute());
      sbuf.append("=");
      sbuf.append(DCResourceModel.class.getSimpleName());
      sbuf.append("[");
      sbuf.append(copiedSubresourceDCModel.getTargetModel().getName());
      sbuf.append("]");
   }
   
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public ExtendedResourceDCModel getExtendedResourceModel() {
      return extendedResourceModel;
   }
   public void setExtendedResourceModel(ExtendedResourceDCModel extendedResourceModel) {
      this.extendedResourceModel = extendedResourceModel;
   }
   public List<CopiedSubresourceDCModel> getCopiedSubresourceModels() {
      if (this.copiedSubresourceModels == null) {
         this.copiedSubresourceModels = new ArrayList<CopiedSubresourceDCModel>();
      }
      return copiedSubresourceModels;
   }
   public void setCopiedSubresourceModels(
         List<CopiedSubresourceDCModel> copiedSubresourceModels) {
      this.copiedSubresourceModels = copiedSubresourceModels;
   }

}
