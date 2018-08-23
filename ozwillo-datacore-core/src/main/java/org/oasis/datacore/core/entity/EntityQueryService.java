package org.oasis.datacore.core.entity;

import java.util.List;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.query.QueryException;
import org.oasis.datacore.core.meta.ModelNotFoundException;


/**
 * Pagination must be provided in language (ex. LIMIT & OFFSET), otherwise
 * a default limit is set (and there are model-configured maximums to
 * LIMIT & OFFSET as well).
 * Number of results (available according to pagination) must be computed
 * from returned list, because doing so underneath wouldn't improve performance
 * (however, the REST exposition of this service might precisely do so to
 * provide this information without returning all data to clients).
 * 
 * TODO "native" query (W3C LDP-like) also refactored within a dedicated query engine ??
 * 
 * @author mdutoo
 *
 */
public interface EntityQueryService {

   String LANGUAGE_LDPQL = "LDPQL";
   
   List<DCEntity> queryInType(String modelType, String query, String language)
         throws ModelNotFoundException, QueryException;
   List<DCEntity> query(String query, String language) throws QueryException;
   
}
