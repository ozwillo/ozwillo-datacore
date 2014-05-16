package org.oasis.datacore.rest.server.parsing.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.oasis.datacore.core.meta.model.DCModel;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.Criteria;

public class DCQueryParsingContext extends DCResourceParsingContext {

   private Stack<DCModel> modelStack;
   
   // TODO privilege indexed fields !!!!!!!!!!
   private Criteria criteria = new Criteria();
   private String currentEntityFieldPath;
   private List<Criteria> currentMultiCriteria = null;

   // TODO sort, on INDEXED (Queriable) field
   private Sort sort = null;
   private int aggregatedQueryLimit = 0;
   private boolean hasNoIndexedField = false;
   
   public DCQueryParsingContext(DCModel model, String uri) {
      super(model, null);
   }
   
   public DCModel peekModel() {
      return this.modelStack.peek();
   }

   @Override
   public void enter(DCModel model, String uri) {
      super.enter(model, uri);
      if (modelStack == null) {
         modelStack = new Stack<DCModel>();
         // NB. can't be done in declaration else can't be called in constructor
      }
      this.modelStack.push(model);
   }

   @Override
   public void exit() {
      super.exit();
      if ("resource".equals(this.resourceValueStack.peek().getField().getType())) {
         this.modelStack.pop();
      }
   }
   
   public void enterCriteria(String entityFieldPath, int criteriaNb) {
      if (currentMultiCriteria != null) {
         throw new IllegalArgumentException("Can't enter criteria when there is already one");
      }
      this.currentEntityFieldPath = entityFieldPath;
      if (criteriaNb > 1) {
         currentMultiCriteria = new ArrayList<Criteria>(criteriaNb);
      }
   }
   
   public void exitCriteria() {
      if (this.currentMultiCriteria != null) {
         // parsing multiple values (of a field that is mentioned several times) :
         // (such as {limit=[10], founded=[>"-0143-04-01T00:00:00.000Z", <"-0043-04-02T00:00:00.000Z"]})
         // NB. can't be done by merely chaining .and(...)'s because of mongo BasicDBObject limitations, see
         // http://www.mkyong.com/java/due-to-limitations-of-the-basicdbobject-you-cant-add-a-second-and/
         this.getCriteria().andOperator(this.currentMultiCriteria
               .toArray(new Criteria[this.currentMultiCriteria.size()]));
      }
      this.currentEntityFieldPath = null;
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

}
