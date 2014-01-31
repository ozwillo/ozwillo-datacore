package org.oasis.datacore.rest.client;

import org.oasis.datacore.rest.api.DCResource;
import org.springframework.cache.Cache;


/**
 * Use this interface to use the Datacore cached client.
 * 
 * Extends DatacoreClientApi with local-only, non-JAXRS client helper methods
 * especially to take advantage of client local caching.
 * 
 * Along with DCResource's builder methods, it eases up the client developer's
 * job.
 * 
 * @author mdutoo
 *
 */
public interface DatacoreCachedClient extends DatacoreClientApi {
   
   /**
    * Shortcut to postDataInType using resource's first type
    * (does not parse uri for that ; if none, lets server explode)
    * @param resource
    * @return
    */
   DCResource postDataInType(DCResource resource);

   /**
    * Shortcut to putDataInType using resource's first type & id
    * (does not parse uri for that ; if none, lets server explode)
    * @param resource
    * @return
    */
   DCResource putDataInType(DCResource resource);

   /**
    * Shortcut to getData (modelType, iri, version) using provided resource's
    */
   DCResource getData(DCResource resource);

   /**
    * Shortcut to deleteData (modelType, iri, version) using provided resource's
    * @return 
    */
   void deleteData(DCResource resource);
   
   /**
    * Provide access to cache, ex. to clear it or evict some Resources from it
    */
   Cache getCache();
   
}
