package org.oasis.datacore.core.meta.model;

import org.oasis.datacore.core.meta.pov.DCPointOfView;
import org.oasis.datacore.core.meta.pov.DCProject;


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

	/** for unmarshalling only */
	public DCModel() {
		super();
      this.setStorage(true);
	}

	/** @obsolete */
   public DCModel(String name) {
      this(name, DCProject.OASIS_MAIN);
   }

	public DCModel(String name, DCPointOfView pointOfView) {
      this(name, pointOfView.getName());
	}

   public DCModel(String name, String pointOfViewAbsoluteName) {
      super(name, pointOfViewAbsoluteName);
      this.setStorage(true);
   }

}
