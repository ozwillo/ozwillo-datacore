package org.oasis.datacore.core.meta.repo;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;


/**
 * TODO TODOOOOOOOOOOOOO rather for (meta)model, for data can't be used because can't specify collection
 * 
 * see https://blog.codecentric.de/en/2012/02/spring-data-mongodb/
 * 
 * how to enrich it with custom methods :
 * http://docs.spring.io/spring-data/data-jpa/docs/current/reference/html/repositories.html#repositories.single-repository-behaviour
 * 
 * @author mdutoo
 *
 */
public interface DCModelRepository extends MongoRepository<DCEntity, String>,
      DCModelRepositoryCustom {

   @Query("{ '_id' : '?0.id' }")
   public DCEntity getTheSame(DCEntity account);
   
}
