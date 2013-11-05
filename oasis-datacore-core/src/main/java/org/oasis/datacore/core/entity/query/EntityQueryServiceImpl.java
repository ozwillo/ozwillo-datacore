package org.oasis.datacore.core.entity.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.oasis.datacore.core.entity.EntityQueryService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


/**
 * This impl dispatches between query engines (implementing EntityQueryService)
 * according to language
 * @author mdutoo
 *
 */
@Component
@Qualifier("datacore.entityQueryService")
public class EntityQueryServiceImpl implements EntityQueryService {
   
   private Map<String,EntityQueryService> queryEngineMap = new HashMap<String,EntityQueryService>();

   @Override
   public List<DCEntity> queryInType(String modelType, String query, String language) throws QueryException {
      return this.getQueryEngine(language).queryInType(modelType, query, language);
   }
   
   @Override
   public List<DCEntity> query(String query, String language) throws QueryException {
      return this.getQueryEngine(language).query(query, language);
   }
   
   private EntityQueryService getQueryEngine(String language) throws QueryException {
      if (language == null || language.trim().length() == 0) {
         throw new QueryException("Missing language"); // TODO rather business exception ?
      }
      EntityQueryService queryEngine = this.queryEngineMap.get(language);
      if (queryEngine == null) {
         throw new QueryException("No query engine for language " + language); // TODO rather business exception ?
      }
      return queryEngine;
   }

   // TODO rather in (admin ?) interface ?
   public void registerQueryEngine(String language, EntityQueryService queryEngine) {
      this.queryEngineMap.put(language, queryEngine);
   }
   
   /**
    * 
    * @param language
    * @return whether a corresponding query engine has been unregistered
    */
   // TODO rather in (admin ?) interface ?
   public boolean unregisterQueryEngine(String language) {
      return this.queryEngineMap.remove(language) != null;
   }
   
   /**
    * only for knowing what's there and helping unregister
    * @return a copy
    */
   // TODO rather in (admin ?) interface ?
   public Map<String,EntityQueryService> getQueryEngines() {
      return new HashMap<String,EntityQueryService>(queryEngineMap);
   }

}
