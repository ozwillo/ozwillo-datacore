package org.oasis.datacore.core.meta.model;


/**
 * TODO extends shared DCFieldsBase without getCollectionName()
 * 
 * To easily create new Mixins FOR TESTING, do :
 * <p/>
 * new DCMixin("my.app.mixin").addField(new DCField("count", "int")))
 *       .addMixin(modelService.getMixin("oasis.address");
 * <p/>
 * 
 * @author mdutoo
 *
 */
public class DCMixin extends DCModelBase {

   /** for unmarshalling only */
   public DCMixin() {
      super();
   }

   public DCMixin(String name) {
      super(name);
   }

}
