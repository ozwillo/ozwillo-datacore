package org.oasis.datacore.core.entity.query.ldp;

import java.util.List;
import java.util.Map;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.query.QueryException;
import org.oasis.datacore.core.meta.model.DCModel;

/**
 * Internal interface that is used by DatacoreApiImpl and LATER by possible LdpQueryEngine
 * to implement W3C LDP (Linked Data Platform)-like query on top of Datacore MongoDB storage.
 * 
 * TODO LATER replace Map by object model of parsed query ?
 * 
 * @author mdutoo
 *
 */
public interface LdpEntityQueryService {

   List<DCEntity> findDataInType(DCModel dcModel, Map<String, List<String>> params,
         Integer start, Integer limit) throws QueryException;

}
