package org.oasis.datacore.core.entity.query.ldp;

import java.util.List;
import java.util.Map;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.query.QueryException;

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

   /**
    * 
    * @param modelType
    * @param params see API doc. Use new ImmutableMap/List.Builder to easily fill it in Java
    * @param start must be < conf'd maxStart. Else use a ranged query, that is
    * typically with a > or < sorted criteria on an indexed required Resource (ex. dcmo:name)
    * or native (ex. dc:modified) field ex. my:field=>\"lastValueReturned\"+ 
    * (with the first query having been sort only ex. my:field=\"lastValueReturned\"+).
    * @param limit if > maxLimit reset to maxLimit
    */
   List<DCEntity> findDataInType(String modelType, Map<String, List<String>> params,
         Integer start, Integer limit) throws QueryException;
   
   int getMaxStart();
   int getMaxLimit();
   int getMaxScan();
}
