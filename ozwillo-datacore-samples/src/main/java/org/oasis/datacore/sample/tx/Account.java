package org.oasis.datacore.sample.tx;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

public class Account extends TransactableBase<Account> {

   @Id
   private String id; // TODO or ObjectId ??
   @Version // required for optimistic locking
   private String version;
   // TODO possibly more audit
   private String name;
   private double balance;
   
   public String getId() {
      return id;
   }
   public void setId(String id) {
      this.id = id;
   }
   public String getVersion() {
      return version;
   }
   public void setVersion(String version) {
      this.version = version;
   }
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public double getBalance() {
      return balance;
   }
   public void setBalance(double balance) {
      this.balance = balance;
   }
   
}
