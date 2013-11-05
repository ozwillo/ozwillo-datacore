package org.oasis.datacore.core.entity.query.sparql;

import java.util.ArrayList;
import java.util.List;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.springframework.stereotype.Component;


@Component
public class JenaSparqlEntityQueryEngineImpl extends EntityQueryEngineBase {

   @Override
   public List<DCEntity> queryInType(String modelType, String query, String language) {
      // TODO SPARQL : parse query using ex. Jena parser,
      // translate it in JSON-LD OR directly in mongo,
      // add type criteria, default paging & sorting,
      // execute in mongo & translate back
      return new ArrayList<DCEntity>();
   }
   @Override
   public List<DCEntity> query(String query, String language) {
      // TODO SPARQL : same as queryDataInType, but don't add query criteria
      return new ArrayList<DCEntity>();
   }

}
