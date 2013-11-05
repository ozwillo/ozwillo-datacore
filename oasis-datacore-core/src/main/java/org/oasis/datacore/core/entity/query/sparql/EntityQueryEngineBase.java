package org.oasis.datacore.core.entity.query.sparql;

import javax.annotation.PostConstruct;

import org.oasis.datacore.core.entity.EntityQueryService;
import org.oasis.datacore.core.entity.query.EntityQueryServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class EntityQueryEngineBase implements EntityQueryService {
   
   @Autowired
   private EntityQueryServiceImpl entityQueryServiceImpl;
   
   @PostConstruct
   public void init() {
      this.entityQueryServiceImpl.registerQueryEngine(EntityQueryService.LANGUAGE_SPARQL, this);
   }

}
