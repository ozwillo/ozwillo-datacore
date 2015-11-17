package org.oasis.datacore.sample.tx;

import java.util.List;

import org.oasis.datacore.sdk.tx.spring.Transaction;
import org.springframework.data.mongodb.core.mapping.DBRef;

public class TransactableBase<T> {

   /** to do single transactions */
   // NB. NOT @DBref Transaction else bidirectional tx - account relationship and stackoverflow
   // at commit, because not supported by spring https://jira.springsource.org/browse/DATAMONGO-488
   private String pendingTransactionId;
   public String getPendingTransactionId() {
      return pendingTransactionId;
   }
   public void setPendingTransactionId(String pendingTransactionId) {
      this.pendingTransactionId = pendingTransactionId;
   }
   
   /** to do multiple transactions (too complex & specific :
    * needs to check old value before changing it, or even possibly any kind of consistency with other fields) */
   @DBRef // else embedded and not a ref (even though Transaction has its own collection)) see http://stackoverflow.com/questions/17696848/query-based-on-matching-elements-in-dbref-list-for-mongodb-using-spring-data-mon
   private List<Transaction> pendingTransactions;
   public List<Transaction> getPendingTransactions() {
      return pendingTransactions;
   }
   public void setPendingTransactions(List<Transaction> pendingTransactions) {
      this.pendingTransactions = pendingTransactions;
   }
   
}
