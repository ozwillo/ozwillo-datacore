package org.oasis.datacore.sample.tx;

import org.oasis.datacore.sdk.tx.spring.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

/**
 * see https://blog.codecentric.de/en/2012/02/spring-data-mongodb/
 * @author mdutoo
 *
 */
public interface AccountRepo extends MongoRepository<Account, String> {

   @Query("{ _id:?0.id, pendingTransactions:?1.id }")
   public Account getTxPending(Account account, Transaction tx);
   
}
