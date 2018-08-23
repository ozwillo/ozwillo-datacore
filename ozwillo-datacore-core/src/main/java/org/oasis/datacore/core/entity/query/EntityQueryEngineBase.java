package org.oasis.datacore.core.entity.query;

import org.oasis.datacore.core.entity.EntityQueryService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

public abstract class EntityQueryEngineBase implements EntityQueryService {
   
   private String language;
   
   @Autowired
   private EntityQueryServiceImpl entityQueryServiceImpl;
   
   public EntityQueryEngineBase(String language) {
      this.language = language;
   }
   
   @PostConstruct
   public void init() {
      this.entityQueryServiceImpl.registerQueryEngine(this.language, this);
   }

}
