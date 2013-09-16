package org.oasis.datacore.sample.crm;

import java.util.ArrayList;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.sample.tx.Account;
import org.oasis.datacore.sample.tx.AccountRepo;
import org.oasis.datacore.sdk.tx.spring.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.mongodb.DBRef;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-crm-test-context.xml" })
public class TransactionTest {

   @Autowired
   private MongoTemplate mgt; // only for manual DBRef
	@Autowired
	private MongoOperations mgo;
	@Autowired
	private AccountRepo accountRepo;

	/**
	 * 
	 */
	@Test
	public void testMultiTransaction() {
		// db cleanup (TODO in setup / teardown)
		mgo.remove(new Query(), Transaction.class);
		Assert.assertEquals(0,  mgo.findAll(Transaction.class).size());
      mgo.remove(new Query(), Account.class);
      Assert.assertEquals(0,  mgo.findAll(Account.class).size());
		
		// creates initial accounts
		Account accountA = new Account();
		accountA.setName("A");
      accountA.setBalance(1000);
      mgo.save(accountA);
      Assert.assertNotNull(accountA.getId());
      Account accountB = new Account();
      accountB.setName("B");
      accountB.setBalance(1000);
      mgo.save(accountB);
      Assert.assertNotNull(accountB.getId());
		
      // init tx 100 A -> B
      Transaction tx = new Transaction();
      tx.setState(Transaction.STATE_INITIAL);
      mgo.save(tx);
      Assert.assertNotNull(tx.getId());
      double transactionValue = 100; // not put in transaction1 (as well as source & destination),
      // rather must be maintained by application OR extend Transaction
      // BUT FAILS IF SYSTEM FAILURE
      // TODO TODO also think of change history !!!!!

      /////////////////////
      // switch to pending
      tx = mgo.findOne(new BasicQuery("{ id:\"" + tx.getId()
            + "\", state:\"" + Transaction.STATE_INITIAL + "\"}"), Transaction.class); // OPT reget tx
      Assert.assertEquals(tx.getState(), Transaction.STATE_INITIAL);
      tx.setState(Transaction.STATE_PENDING);
      mgo.save(tx); // TODO or using update $set
      
      // until each party has not this transaction as pending, add it and perform it on it
      
      // ALT 0 update on the fly with condition (only if tx not already pending)
      // ALT 0.1 spring condition NO BUG tx object is pushed instead of its ref https://jira.springsource.org/browse/DATAMONGO-404
      ///mgo.updateFirst(new Query(new Criteria("id").is(accountA.getId()))
      ///   .addCriteria(new Criteria("pendingTransactions").ne(tx)),
      ///   new Update().set("balance", accountA.getBalance() - transactionValue) // could use $inc
      ///   .push("pendingTransactions", new DBRef(mgt.getDb(), "transactions", new ObjectId(tx.getId()))), Account.class);
      // ALT 0.2 spring + mongo condition
      mgo.updateFirst(new Query(new Criteria("id").is(accountA.getId()))
         .addCriteria(new Criteria("pendingTransactions").ne(tx)),
         new Update().set("balance", accountA.getBalance() - transactionValue) // could use $inc
         .push("pendingTransactions", new DBRef(mgt.getDb(), "transactions", new ObjectId(tx.getId()))), Account.class);
      // ALT 1 (OPT reget then) update after manual condition check ("only if tx pending", still consistent because of optimistic locking)
      // OPT reget ALT 1.1 with mongo condition
      ///accountA = mgo.findOne(new BasicQuery("{ id:\"" + accountA.getId()
      ///      + "\", pendingTransactions:{ $ne:new ObjectId(\"" + tx.getId() + "\")} }"), Account.class);
      // NB. $elemMatch only necessary when trying to match against multiple fields on an array element http://stackoverflow.com/questions/12505121/finding-an-element-in-array-of-objectid-in-mongodb
      // OPT reget ALT 1.2 with spring condition
      /// OK though $ne probably not more efficient than get + if
      ///accountA = mgo.findOne(new Query(new Criteria("id").is(accountA.getId()))
      ///   .addCriteria(new Criteria("pendingTransactions").ne(tx)), Account.class); // OPT reget tx
      // OPT reget ALT 1.3 repo, no condition (let to optimistic locking)
      /// accountA = accountRepo.findOne(accountA.getId());
      /*if (accountA.getPendingTransactions() == null) {
         accountA.setPendingTransactions(new ArrayList<Transaction>());
      }
      accountA.getPendingTransactions().add(tx); // doesn't contain yet because of query, and if changes before update optimistic locking will make save fail
      accountA.setBalance(accountA.getBalance() - transactionValue);
      mgo.save(accountA);*/
      
      // IF FAILS HERE CAN'T UNDO IN A FAILURE-PROOF WAY or retry, therefore put old (or new) value
      // in either TransactableBase<T> (below pending tx to support several transactions) BUT CAN ONLY CLEAN ON READ AND NOT BY READING TX AND CANT PERFORM FAILED TX save if tx contains their refs,
      // or new values AND THEIR COLLECTIONS in tx (BUT not in spring binding mode) and can clean tx (and perform failed ones) in a scheduled manner
      // in both cases : WHAT ABOUT HISTORY ? & old tx should be moved to a separate tx_history collection...
      
      // ALT 0.2 spring + mongo condition
      ///mgo.updateFirst(new Query(new Criteria("id").is(accountB.getId()))
      ///   .addCriteria(new Criteria("pendingTransactions").ne(tx)),
      ///   new Update().set("balance", accountB.getBalance() + transactionValue) // could use $inc
      ///   .push("pendingTransactions", new DBRef(mgt.getDb(), "transactions", new ObjectId(tx.getId()))), Account.class);
      // OPT reget ALT 1.2 with spring condition
      accountB = mgo.findOne(new Query(new Criteria("id").is(accountB.getId()))
            .addCriteria(new Criteria("pendingTransactions").ne(tx)), Account.class); // OPT reget tx
      // OPT reget ALT 1.3 repo, no condition (let to optimistic locking)
      ///accountB = accountRepo.findOne(accountB.getId());
      if (accountB.getPendingTransactions() == null) {
         accountB.setPendingTransactions(new ArrayList<Transaction>());
      }
      accountB.getPendingTransactions().add(tx); // doesn't contain yet because of query, and if changes before update optimistic locking will make save fail
      accountB.setBalance(accountB.getBalance() + transactionValue);
      mgo.save(accountB);
      
      ///////////////////
      // commit
      tx = mgo.findOne(new BasicQuery("{ id:\"" + tx.getId()
            + "\", state:\"" + Transaction.STATE_PENDING + "\"}"), Transaction.class); // OPT reget tx
      Assert.assertNotNull(tx);
      tx.setState(Transaction.STATE_COMMITTED);
      mgo.save(tx);
      
      // cleanup
      ///accountA = mgo.findOne(new BasicQuery("{ id:\"" + accountA.getId()
      ///      + "\", pendingTransactions:\"" + tx.getId() + "\"}"), Account.class); // OPT reget tx
      accountA = mgo.findOne(new Query(new Criteria("id").is(accountA.getId()))
            .addCriteria(new Criteria("pendingTransactions").is(tx)), Account.class); // OPT reget tx
      // TODO other pending tx not returned ?????
      Transaction accountATx = null;
      for (Transaction accountTx : accountA.getPendingTransactions()) { 
         if (tx.getId().equals(accountTx.getId())) {
            Assert.assertEquals(accountTx.getState(), Transaction.STATE_COMMITTED);
            accountATx = accountTx;
            break; // TODO not multiple because checked at add ?!?
         }
      }
      accountA.getPendingTransactions().remove(tx);
      // TODO other pending tx not returned ?????
      mgo.save(accountA);
      ///accountB = mgo.findOne(new BasicQuery("{ id:\"" + accountB.getId()
      ///      + "\", pendingTransactions:\"" + tx.getId() + "\"}"), Account.class); // OPT reget tx
      accountB = mgo.findOne(new Query(new Criteria("id").is(accountB.getId()))
            .addCriteria(new Criteria("pendingTransactions").is(tx)), Account.class); // OPT reget tx
      // TODO other pending tx not returned ?????
      Transaction accountBTx = null;
      for (Transaction accountTx : accountB.getPendingTransactions()) { 
         if (tx.getId().equals(accountTx.getId())) {
            Assert.assertEquals(accountTx.getState(), Transaction.STATE_COMMITTED);
            accountBTx = accountTx;
            break; // TODO not multiple because checked at add ?!?
         }
      }
      accountB.getPendingTransactions().remove(tx);
      // TODO other pending tx not returned ?????
      mgo.save(accountB);
      
      // done
      tx = mgo.findOne(new BasicQuery("{ id:\"" + tx.getId()
            + "\", state:\"" + Transaction.STATE_COMMITTED + "\"}"), Transaction.class); // OPT reget tx
      tx.setState(Transaction.STATE_DONE);
      mgo.save(tx);
      
      accountA = mgo.findOne(new BasicQuery("{ id:\"" + accountA.getId() + "\"}"), Account.class);
      Assert.assertEquals(1000 - transactionValue, accountA.getBalance(), 0);
      accountB = mgo.findOne(new BasicQuery("{ id:\"" + accountB.getId() + "\"}"), Account.class);
      Assert.assertEquals(1000 + transactionValue, accountB.getBalance(), 0);
	}


   /**
    * 
    */
   @Test
   public void testSingleTransaction() {
      // db cleanup (TODO in setup / teardown)
      mgo.remove(new Query(), Transaction.class);
      Assert.assertEquals(0,  mgo.findAll(Transaction.class).size());
      mgo.remove(new Query(), Account.class);
      Assert.assertEquals(0,  mgo.findAll(Account.class).size());
      
      // creates initial accounts
      Account accountA = new Account();
      accountA.setName("A");
      accountA.setBalance(1000);
      mgo.save(accountA);
      Assert.assertNotNull(accountA.getId());
      Account accountB = new Account();
      accountB.setName("B");
      accountB.setBalance(1000);
      mgo.save(accountB);
      Assert.assertNotNull(accountB.getId());
      
      // init tx 100 A -> B
      Transaction tx = new Transaction();
      tx.setState(Transaction.STATE_INITIAL);
      mgo.save(tx);
      Assert.assertNotNull(tx.getId());
      double transactionValue = 100; // not put in transaction1 (as well as source & destination),
      // rather must be maintained by application OR extend Transaction
      // BUT FAILS IF SYSTEM FAILURE
      // TODO TODO also think of change history !!!!!

      /////////////////////
      // switch to pending
      tx = mgo.findOne(new BasicQuery("{ id:\"" + tx.getId()
            + "\", state:\"" + Transaction.STATE_INITIAL + "\"}"), Transaction.class); // OPT reget tx
      Assert.assertEquals(tx.getState(), Transaction.STATE_INITIAL);
      tx.setState(Transaction.STATE_PENDING);
      tx.setNewValues(new ArrayList<Object>(3));
      
      // SWITCHING A TO PENDING I.E. LOCK IT (but don't perform yet else won't be able to rollback)
      // ALT 0 update on the fly with condition (only if tx not already pending)
      // ALT 0.1 spring condition NO BUG tx object is pushed instead of its ref https://jira.springsource.org/browse/DATAMONGO-404
      ///mgo.updateFirst(new Query(new Criteria("id").is(accountA.getId()))
      ///   .addCriteria(new Criteria("pendingTransactions").ne(tx)),
      ///   new Update().set("balance", accountA.getBalance() - transactionValue) // could use $inc
      ///   .set("pendingTransaction", new DBRef(mgt.getDb(), "transactions", new ObjectId(tx.getId()))), Account.class);
      // NO else bidirectional tx - account relationship which spring does not support (stackoverflow at commit) https://jira.springsource.org/browse/DATAMONGO-488
      ///mgo.updateFirst(new Query(new Criteria("id").is(accountA.getId()))
      ///   .addCriteria(new Criteria("pendingTransactions").ne(tx)),
      ///   new Update().set("pendingTransaction", new DBRef(mgt.getDb(), "transactions", new ObjectId(tx.getId()))), Account.class);
      mgo.updateFirst(new Query(new Criteria("id").is(accountA.getId()))
         .addCriteria(new Criteria("pendingTransactionId").ne(tx.getId())),
         new Update().set("pendingTransactionId", tx.getId()), Account.class);
      // ALT 0.2 spring + mongo condition
      ///mgo.updateFirst(new Query(new Criteria("id").is(accountA.getId()))
      ///   .addCriteria(new Criteria("pendingTransaction").ne(tx)),
      ///   new Update().set("balance", accountA.getBalance() - transactionValue) // could use $inc
      ///   .set("pendingTransaction", new DBRef(mgt.getDb(), "transactions", new ObjectId(tx.getId()))), Account.class);
      // ALT 1 (OPT reget then) update after manual condition check ("only if tx pending", still consistent because of optimistic locking)
      // OPT reget ALT 1.1 with mongo condition
      ///accountA = mgo.findOne(new BasicQuery("{ id:\"" + accountA.getId()
      ///      + "\", pendingTransaction:{ $ne:new ObjectId(\"" + tx.getId() + "\")} }"), Account.class);
      // NB. $elemMatch only necessary when trying to match against multiple fields on an array element http://stackoverflow.com/questions/12505121/finding-an-element-in-array-of-objectid-in-mongodb
      // OPT reget ALT 1.2 with spring condition
      /// OK though $ne probably not more efficient than get + if
      ///accountA = mgo.findOne(new Query(new Criteria("id").is(accountA.getId()))
      ///   .addCriteria(new Criteria("pendingTransaction").ne(tx)), Account.class); // OPT reget tx
      // OPT reget ALT 1.3 repo, no condition (let to optimistic locking)
      accountA = accountRepo.findOne(accountA.getId());
      if (!tx.equals(accountA.getPendingTransactionId())) {
         Assert.fail("Bad transaction on " + accountA); // TODO can't happen ?!?
      }
      accountA.setBalance(accountA.getBalance() - transactionValue);
      accountA.setPendingTransactionId(null); // preparing cleanup REQUIRES HACK IN MappingMongoConverter ELSE WON'T BE SAVED
      // (because MappingMongoConverter skip null props & assos)
      // so TODO how to handle unsets ?? => ALT 1 MappingMongoConverter hack, ALT 2 separate Transaction.unsetValues for unset (and others...), ALT 3 in onBeforeSave lifecycle event http://static.springsource.org/spring-data/data-document/docs/current/reference/html/#mongodb.mapping-usage.events
      tx.getNewValues().add(accountA);

      // SWITCHING B TO PENDING I.E. LOCK IT (but don't perform yet else won't be able to rollback)
      // ALT 0.2 spring + mongo condition
      ///mgo.updateFirst(new Query(new Criteria("id").is(accountB.getId()))
      ///   .addCriteria(new Criteria("pendingTransaction").ne(tx)),
      ///   new Update().set("balance", accountB.getBalance() + transactionValue) // could use $inc
      ///   .set("pendingTransaction", new DBRef(mgt.getDb(), "transactions", new ObjectId(tx.getId()))), Account.class);

      // NO else bidirectional tx - account relationship which spring does not support (stackoverflow at commit) https://jira.springsource.org/browse/DATAMONGO-488
      ///mgo.updateFirst(new Query(new Criteria("id").is(accountB.getId()))
      ///   .addCriteria(new Criteria("pendingTransaction").ne(tx)),
      ///   new Update().set("pendingTransaction", new DBRef(mgt.getDb(), "transactions", new ObjectId(tx.getId()))), Account.class);
      mgo.updateFirst(new Query(new Criteria("id").is(accountB.getId()))
         .addCriteria(new Criteria("pendingTransactionId").ne(tx.getId())),
         new Update().set("pendingTransactionId", tx.getId()), Account.class);
      // OPT reget ALT 1.2 with spring condition
      ///accountB = mgo.findOne(new Query(new Criteria("id").is(accountB.getId()))
      ///      .addCriteria(new Criteria("pendingTransaction").ne(tx)), Account.class); // OPT reget tx
      // OPT reget ALT 1.3 repo, no condition (let to optimistic locking)
      accountB = accountRepo.findOne(accountB.getId());
      if (!tx.equals(accountB.getPendingTransactionId())) {
         Assert.fail("Bad transaction on " + accountB); // TODO can't happen ?!?
      }
      accountB.setBalance(accountB.getBalance() + transactionValue);
      accountB.setPendingTransactionId(null); // preparing cleanup REQUIRES HACK IN MappingMongoConverter ELSE WON'T BE SAVED
      // (because MappingMongoConverter skip null props & assos)
      // so TODO how to handle unsets ?? => ALT 1 MappingMongoConverter hack, ALT 2 separate Transaction.unsetValues for unset (and others...), ALT 3 in onBeforeSave lifecycle event http://static.springsource.org/spring-data/data-document/docs/current/reference/html/#mongodb.mapping-usage.events
      tx.getNewValues().add(accountB);

      // IF FAILS HERE OR BEFORE objects have to be cleaned from pending tx in initial state

      mgo.save(tx); // TODO or using update $set

      
      // IF FAILS FROM HERE ON, can reperform from tx or objects (on objects that have pending tx)
      // (optimistic locking failure could only happen from two threads / clients trying to reperform the same tx twice,
      // in which case the failed client only has to reget the object and check that it is now clean)
      // WHAT ABOUT HISTORY ? & old tx should be moved to a separate tx_history collection...

      // CHANGE ON A OR B IN BETWEEN FROM HERE ON is not possible because their pending tx prevents it 
      // NB. ANYWAY IF CHANGE ON A OR B IN BETWEEN HERE, their tx version won't be able to be saved because obsolete and tx will have to be rolled back
      
      // actual perform : save new values (without pending tx)
      for (Object newValue : tx.getNewValues()) {
         mgo.save(newValue);
      }
      // same as :
      ///mgo.save(accountA);
      ///mgo.save(accountB);
      
      ///////////////////
      // commit
      // NB. if bidirectional tx - account relationship, stackoverflow here, because not supported by spring https://jira.springsource.org/browse/DATAMONGO-488
      tx = mgo.findOne(new BasicQuery("{ id:\"" + tx.getId()
            + "\", state:\"" + Transaction.STATE_PENDING + "\"}"), Transaction.class); // OPT reget tx
      Assert.assertNotNull(tx);
      tx.setState(Transaction.STATE_COMMITTED);
      mgo.save(tx);
      
      
      // IF FAILS HERE only have to cleanup
      
      
      // cleanup
      // ONLY REQUIRED IF NOT YET DONE (requires MappingMongoConverter hack)
      /*
      ///accountA = mgo.findOne(new BasicQuery("{ id:\"" + accountA.getId()
      ///      + "\", pendingTransactions:\"" + tx.getId() + "\"}"), Account.class); // OPT reget tx
      accountA = mgo.findOne(new Query(new Criteria("id").is(accountA.getId()))
            .addCriteria(new Criteria("pendingTransactionId").is(tx.getId())), Account.class); // OPT reget tx
      accountA.setPendingTransactionId(null); // or using $set
      mgo.save(accountA);
      ///accountB = mgo.findOne(new BasicQuery("{ id:\"" + accountB.getId()
      ///      + "\", pendingTransactions:\"" + tx.getId() + "\"}"), Account.class); // OPT reget tx
      accountB = mgo.findOne(new Query(new Criteria("id").is(accountB.getId()))
            .addCriteria(new Criteria("pendingTransactionId").is(tx.getId())), Account.class); // OPT reget tx
      accountB.setPendingTransactionId(null); // or using $set
      mgo.save(accountB);
      */
      
      // done
      tx = mgo.findOne(new BasicQuery("{ id:\"" + tx.getId()
            + "\", state:\"" + Transaction.STATE_COMMITTED + "\"}"), Transaction.class); // OPT reget tx
      tx.setState(Transaction.STATE_DONE);
      mgo.save(tx);
      
      accountA = mgo.findOne(new BasicQuery("{ id:\"" + accountA.getId() + "\"}"), Account.class);
      Assert.assertEquals(1000 - transactionValue, accountA.getBalance(), 0);
      accountB = mgo.findOne(new BasicQuery("{ id:\"" + accountB.getId() + "\"}"), Account.class);
      Assert.assertEquals(1000 + transactionValue, accountB.getBalance(), 0);
   }
   

}
