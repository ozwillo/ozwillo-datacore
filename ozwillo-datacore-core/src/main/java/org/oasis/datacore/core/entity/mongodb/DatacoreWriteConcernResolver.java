package org.oasis.datacore.core.entity.mongodb;

import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoAction;
import org.springframework.data.mongodb.core.WriteConcernResolver;
import org.springframework.stereotype.Component;

import com.mongodb.WriteConcern;

/**
 * Provides custom configuration to Mongo, according to the current project's configuration :
 * - allows to write without waiting for confirmation (DCProject.isDbRobust)
 * @author mdutoo
 *
 */
@Component
public class DatacoreWriteConcernResolver implements WriteConcernResolver {

   /** default WriteConcern */
   @Value("#{T(com.mongodb.WriteConcern).valueOf(\"${oasis.datacore.mongodb.writeConcern}\")}")
   private WriteConcern writeConcern;
   /** to access request-specific mongo conf */
   @Autowired
   protected DCModelService modelService;

	@Override
	public WriteConcern resolve(MongoAction action) {
		return resolve(modelService.getProject());
	}
   public WriteConcern resolve(DCProject project) {
      return (project.isDbRobust()) ? writeConcern : WriteConcern.UNACKNOWLEDGED;
   }

}
