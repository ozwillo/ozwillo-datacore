package org.oasis.datacore.core.meta.model;


/**
 * To easily create new Models FOR TESTING, do :
 * <p/>
 * new DCModel("my.app.type").addField(new DCField("name"))
 *       .addMixin(modelService.getMixin("oasis.address"))
 *       .addMixin(new DCMixin("my.app.mixin").addField(new DCField("count", "int")));
 * <p/>
 * 
 * TODO support for builtin samples and documentation, probably in another collection for performance
 * 
 * TODO TODO rather no field in root DCModel and rather all in mixinTypes !!!!!!!!!!!
 * 
 * TODO readonly : setters only for tests, LATER admin using another (inheriting) model ?!?
 * TODO common abstract / inherit with DCMapField
 * @author mdutoo
 *
 */
public class DCModel extends DCModelBase {

   private DCSecurity security = new DCSecurity();

   /** for unmarshalling only */
   public DCModel() {
      super();
   }

   public DCModel(String name) {
      super(name);
   }
   
   public String getCollectionName() {
      return this.getName();
   }

   /** TODO or in DCModelBase (i.e. mixins) ?? on fields ????? */
   public DCSecurity getSecurity() {
      return security;
   }

   public void setSecurity(DCSecurity security) {
      this.security = security;
   }
   
}
