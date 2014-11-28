package org.oasis.datacore.core.meta.model;

import org.oasis.datacore.core.meta.pov.DCPointOfView;
import org.oasis.datacore.core.meta.pov.DCProject;


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
   
   /** @obsolete */
   public DCMixin(String name) {
      this(name, DCProject.OASIS_MAIN);
   }

   /*public DCMixin(String name, DCPointOfView ... pointOfViews) {
      super(name, pointOfViews);
   }*/

   public DCMixin(String name, DCPointOfView pointOfView) {
      this(name, pointOfView.getName());
   }
   public DCMixin(String name, String pointOfViewAbsoluteName) {
      super(name, pointOfViewAbsoluteName);
      this.setStorage(false);
      this.setInstanciable(false);
   }


}
