package org.oasis.datacore.core.meta.repo;

import java.net.URISyntaxException;

import org.oasis.datacore.core.entity.model.DCEntity;

/**
 * TODO TODOOOOOOOOOOOOO rather for (meta)model, for data can't be used because can't specify collection
 * Custom coded queries are defined here
 * 
 * @obsolete not used for now
 * 
 * @author mdutoo
 *
 */
public interface DCModelRepositoryCustom {

   DCEntity getSampleData() throws URISyntaxException;
   
}
