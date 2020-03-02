package org.oasis.datacore.rest.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryServiceImpl;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;

/**
 * Helper for testing mongo "$explain" of indexes
 * @author mdutoo
 *
 */
public class TestHelper {

   /**
    * returns names of fields used efficiently in query (in FETCH or IXSCAN)
    * according to https://docs.mongodb.com/manual/reference/explain-results/
    * @param debugResult
    * @return
    */
   @SuppressWarnings("unchecked")
   public static List<String> getDebugWinningPlanFilterFields(List<DCResource> debugResult) {
      Map<String, Object> queryExplain = (Map<String,Object>) getDebug(debugResult)
         .get(LdpEntityQueryServiceImpl.DEBUG_QUERY_EXPLAIN);
      Map<String, Object> queryPlanner = (Map<String, Object>) queryExplain.get("queryPlanner");
      Map<String, Object> winningPlan = (Map<String, Object>) queryPlanner.get("winningPlan");
      Map<String, Object> inputStage = (Map<String, Object>) winningPlan.get("inputStage");
      if ("LIMIT".equals(inputStage.get("stage"))) { // skip
         inputStage = (Map<String, Object>) inputStage.get("inputStage");
      }
      ArrayList<String> filterFields = new ArrayList<>(3);
      addFilterFields(filterFields, inputStage);
      Assert.assertNotEquals(0, filterFields.size());
      return filterFields;
   }

   /**
    * @param filterFields adds to it the names of the field(s) of the given inputStage
    * IF it is not a COLLSCAN
    * @param inputStage
    */
   @SuppressWarnings("unchecked")
   private static void addFilterFields(ArrayList<String> filterFields, Map<String, Object> inputStage) {
      if ("COLLSCAN".equals(inputStage.get("stage"))) {
         return; // not index ;_;
      }
      
      if ("FETCH".equals(inputStage.get("stage"))) {
         Map<String, Object> filter = (Map<String, Object>) inputStage.get("filter");
         List<Map<String, Object>> filters = (List<Map<String, Object>>) filter.get("$and");
         if (filters != null) {
            for (Map<String, Object> curFilter : filters) {
               filterFields.add(curFilter.keySet().iterator().next());
            }
         } else {
            filterFields.addAll(filter.keySet());
         }
         addFilterFields(filterFields, (Map<String, Object>) inputStage.get("inputStage"));
         
      } else if ("IXSCAN".equals(inputStage.get("stage"))) {
         Map<String, Object> keyPattern = (Map<String, Object>) inputStage.get("keyPattern");
         filterFields.addAll(keyPattern.keySet());  
      }
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

   public static int getDebugResourcesNb(List<DCResource> debugResult) {
      List<Map<String, Object>> resources = getDebugResources(debugResult);
      if (resources == null) {
         return 0;
      }
      return resources.size();
   }

}
