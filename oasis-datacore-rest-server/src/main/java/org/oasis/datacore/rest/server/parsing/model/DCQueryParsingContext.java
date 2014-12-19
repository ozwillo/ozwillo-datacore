package org.oasis.datacore.rest.server.parsing.model;

import java.util.ArrayList;
import java.util.List;

import org.oasis.datacore.core.meta.model.DCModelBase;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.Criteria;

public class DCQueryParsingContext extends DCResourceParsingContext {
   
   // TODO privilege indexed fields !!!!!!!!!!
   private Criteria criteria = new Criteria();
   private String currentEntityFieldPath;
   private int criteriaNb = -1;
   private List<Criteria> currentMultiCriteria = null;

   // TODO sort, on INDEXED (Queriable) field
   private Sort sort = null;
   private int aggregatedQueryLimit = 0;
   private boolean hasNoIndexedField = false;
   
   public DCQueryParsingContext(DCModelBase model, DCModelBase storageModel) {
      super(model, storageModel, null);
   }
   
   public void enterCriteria(String entityFieldPath, int criteriaNb) {
      if (currentMultiCriteria != null) {
         throw new IllegalArgumentException("Can't enter criteria when there is already one");
      }
      this.currentEntityFieldPath = entityFieldPath;
      this.criteriaNb = criteriaNb;
      if (criteriaNb > 1) {
         currentMultiCriteria = new ArrayList<Criteria>(criteriaNb);
      }
   }
   
   public void patchCriteria(String entityFieldPath) {
      this.currentEntityFieldPath = null;
      this.currentMultiCriteria = null;
      this.enterCriteria(entityFieldPath, criteriaNb);
   }
   
   public void exitCriteria() {
      if (this.currentMultiCriteria != null) {
         // parsing multiple values (of a field that is mentioned several times) :
         // (such as {limit=[10], founded=[>"-0143-04-01T00:00:00.000Z", <"-0043-04-02T00:00:00.000Z"]}
         // or /dc/type/sample.city.city?city:founded=>="0043-04-02T00:00:00.000Z"&city:founded=<="2043-04-02T00:00:00.000Z" )
         // NB. can't be done by merely chaining .and(...)'s because of mongo BasicDBObject limitations, see
         // http://www.mkyong.com/java/due-to-limitations-of-the-basicdbobject-you-cant-add-a-second-and/
         this.getCriteria().andOperator(this.currentMultiCriteria
               .toArray(new Criteria[this.currentMultiCriteria.size()]));
      }
      this.currentEntityFieldPath = null;
      this.criteriaNb = -1;
      this.currentMultiCriteria = null;
   }

   public Criteria addCriteria() {
      if (currentMultiCriteria != null) {
         Criteria newCriteria = new Criteria(this.currentEntityFieldPath);
         currentMultiCriteria.add(newCriteria);
         return newCriteria;
      } else {
         return getCriteria().and(this.currentEntityFieldPath);
      }
   }

   
   /**
    * 
    * @param sortEnum if null, does nothing
    */
   public void addSort(QueryOperatorsEnum sortEnum) {
      if (sortEnum != null) {
         switch (sortEnum) {
         case SORT_ASC:
            this.addSort(new Sort(Direction.ASC, this.currentEntityFieldPath));
            break;
         case SORT_DESC:
            this.addSort(new Sort(Direction.DESC, this.currentEntityFieldPath));
            break;
         default:
            break;
         }
      }
   }
   
   private void addSort(Sort currentSort) {
      this.sort = (this.sort == null) ? currentSort : this.sort.and(currentSort);
   }

   public Criteria getCriteria() {
      return this.criteria;
   }

   public Sort getSort() {
      return this.sort;
   }

   public int getAggregatedQueryLimit() {
      return aggregatedQueryLimit;
   }

   public void setAggregatedQueryLimit(int aggregatedQueryLimit) {
      this.aggregatedQueryLimit = aggregatedQueryLimit;
   }

   public boolean isHasNoIndexedField() {
      return hasNoIndexedField;
   }

   public void setHasNoIndexedField(boolean hasNoIndexedField) {
      this.hasNoIndexedField = hasNoIndexedField;
   }

   public String getEntityFieldPath() {
      return currentEntityFieldPath;
   }
   
}
