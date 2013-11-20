package org.oasis.datacore.core;

import java.util.List;

import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * TODO or rather using @Config or @PostConstruct ??
 * 
 * @author mdutoo
 *
 */
@Component
public class DatacoreBootstrap implements ApplicationContextAware {

   /** impl, to be able to modify it
    * TODO LATER extract interface */ 
   @Autowired
   private DataModelServiceImpl modelAdminService;
   private List<Object> boostrappables;
   private boolean enabled = true;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (modelAdminService.getModelMap().isEmpty() && enabled) {
			// NB. to rebootstrap, first empty models !

			if (boostrappables != null) {
				for (Object boostrappable : boostrappables) {
					// boostrappables.init();
				}
			}
		}

	}

}
