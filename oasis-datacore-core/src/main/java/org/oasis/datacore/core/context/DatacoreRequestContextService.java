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
   
   boolean getPutRatherThanPatchMode();

}
