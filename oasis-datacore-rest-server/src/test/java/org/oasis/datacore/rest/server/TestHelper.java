package org.oasis.datacore.rest.server;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryServiceImpl;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;

public class TestHelper {

   public static String getDebugCursor(List<DCResource> debugResult) {
      @SuppressWarnings("unchecked")
      Map<String, Object> queryExplain = (Map<String,Object>) getDebug(debugResult)
         .get(LdpEntityQueryServiceImpl.DEBUG_QUERY_EXPLAIN);
      String cursor = (String) queryExplain.get("cursor");
      Assert.assertNotNull(cursor);
      return cursor;
   }

   public static Map<String, Object> getDebug(List<DCResource> debugResult) {
      Assert.assertEquals(1, debugResult.size());
      @SuppressWarnings("unchecked")
      Map<String, Object> debug = (Map<String,Object>) debugResult.get(0).get(DatacoreApi.DEBUG_RESULT);
      return debug;
   }

   public static List<Map<String, Object>> getDebugResources(List<DCResource> debugResult) {
      Assert.assertEquals(1, debugResult.size());
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> resources = (List<Map<String, Object>>) debugResult.get(0).get(DatacoreApi.RESOURCES_RESULT);
      return resources;
   }

}
