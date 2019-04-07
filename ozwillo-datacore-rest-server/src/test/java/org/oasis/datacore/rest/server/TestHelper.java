package org.oasis.datacore.rest.server;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryServiceImpl;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;

public class TestHelper {

   public static List<Map<String, Object>> getDebugResources(List<DCResource> debugResult) {
      Assert.assertEquals(1, debugResult.size());
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> resources = (List<Map<String, Object>>) debugResult.get(0).get(DatacoreApi.RESOURCES_RESULT);
      return resources;
   }

   public static int getDebugResourcesNb(List<DCResource> debugResult) {
      List<Map<String, Object>> resources = getDebugResources(debugResult);
      if (resources == null) {
         return 0;
      }
      return resources.size();
   }

}
