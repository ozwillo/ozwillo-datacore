package org.oasis.datacore.core.context;

import java.util.LinkedHashSet;
import java.util.Map;


/**
 * TODO cache
 * @author mdutoo
 *
 */
public interface DatacoreRequestContextService {

   boolean isDebug();

   Map<String, Object> getDebug();

   String getAcceptContentType();

   LinkedHashSet<String> getViewMixinNames();
   
   /**
    * i.e. replace rather than merge
    * (for now) set in ResourceService.resourceToEntity() according to putRatherThanPatchMode
    * (rather than in DatacoreApiImpl, so that it can be used everywhere including outside REST)
    * @return
    */
   boolean getPutRatherThanPatchMode();

}
