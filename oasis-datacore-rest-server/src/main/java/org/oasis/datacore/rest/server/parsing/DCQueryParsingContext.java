package org.oasis.datacore.rest.server.parsing;

import org.oasis.datacore.core.meta.model.DCModel;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

public class DCQueryParsingContext extends DCResourceParsingContext {

   // TODO privilege indexed fields !!!!!!!!!!
   private Criteria criteria = new Criteria();

   // TODO sort, on INDEXED (Queriable) field
   private Sort sort = null;
   
   public DCQueryParsingContext(DCModel model, String uri) {
      super(model, null);
   }

   public void addSort(Sort currentSort) {
      this.sort = (this.sort == null) ? currentSort : this.sort.and(currentSort);
   }

   public Criteria getCriteria() {
      return this.criteria;
   }

   public void setCriteria(Criteria criteria) {
      this.criteria = criteria;
   }

   public Sort getSort() {
      return this.sort;
   }

   public void setSort(Sort sort) {
      this.sort = sort;
   }

}
