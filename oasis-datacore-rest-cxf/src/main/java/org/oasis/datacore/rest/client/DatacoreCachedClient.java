package org.oasis.datacore.rest.client;

import org.oasis.datacore.rest.api.DCResource;


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
   public DCResource postDataInType(DCResource resource);

   /**
    * Shortcut to putDataInType using resource's first type & id
    * (does not parse uri for that ; if none, lets server explode)
    * @param resource
    * @return
    */
   public DCResource putDataInType(DCResource resource);

   DCResource getData(DCResource resource);

   void deleteData(DCResource resource);
   
   void clearCache();
   
}
