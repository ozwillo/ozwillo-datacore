package org.oasis.datacore.sample.crm;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.mongodb.WriteResult;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-crm-test-context.xml" })
public class CrmTest {
	
	@Autowired
	private MongoOperations mgo;

	/**
	 * tests :
	 * CRUD, find, update on the fly
	 * optimistic locking & auto version
	 */
	@Test
	public void testContact() {
		// db cleanup (TODO in setup / teardown)
		mgo.remove(new Query(), Contact.class);
		Assert.assertEquals(0,  mgo.findAll(Contact.class).size());
		
		// build new contact model
		Contact contact1 = new Contact();
		contact1.setFirstname("John");
		contact1.setLastname("Doe");
		Assert.assertEquals("John", contact1.getFirstname());
		 
		// create contact (sets id)
		mgo.save(contact1);
		Assert.assertNotNull(contact1.getId());
		System.out.println("1. contact : " + contact1.getId());
      Assert.assertEquals(new Long(0), contact1.getVersion());
      Assert.assertEquals("createdAt and lastModifiedAt should be the same",
            contact1.getCreatedAt(), contact1.getLastModified());
      
      // checking auto version increment (with no diff handling)
      mgo.save(contact1);
      Assert.assertEquals(new Long(1), contact1.getVersion());
		
		// list
		List<Contact> contacts = mgo.findAll(Contact.class);
		Assert.assertEquals(1, contacts.size());
	 
		// find it by lastname
		Query searchUserDoeQuery = new Query(Criteria.where("lastname").is("Doe"));
		Contact existingContact = mgo.findOne(searchUserDoeQuery, Contact.class);
		Assert.assertNotNull("Contact should have been created in db", existingContact);
		Assert.assertEquals("John", existingContact.getFirstname());
		Assert.assertNotNull("audit user should have been filled", existingContact.getCreatedBy());
		Assert.assertNotNull("audit user should have been filled", existingContact.getLastModifiedBy());
      Assert.assertNotEquals("createdAt and lastModifiedAt should be different",
            existingContact.getCreatedAt(), existingContact.getLastModified());
		System.out.println("2. find - existingContact : " + existingContact.getFirstname());
	 
		// update firstname using on the fly mode and find it again
		String newFirstname = "JohnUpdated";
		WriteResult wr = mgo.updateFirst(searchUserDoeQuery, Update.update("firstname", newFirstname), Contact.class);
		if (wr.getError() != null) {
			System.err.println(wr.getError());
			System.err.println(wr.getLastError());
			Assert.fail("Error in last write");
		}
		existingContact = mgo.findOne(searchUserDoeQuery, Contact.class);
		Assert.assertEquals(newFirstname, existingContact.getFirstname());
		Assert.assertEquals("Version should (alas) not change when updated on the fly",
				contact1.getVersion(), existingContact.getVersion());
		Assert.assertEquals("lastModified should (alas) not change on update",
				contact1.getLastModified(), existingContact.getLastModified());
		System.out.println("3. updated Contact : " + existingContact.getFirstname());
		
		// update using entity (using current version, optimistic locking should let it pass)
		newFirstname = "JohnOptimistic";
		existingContact.setFirstname(newFirstname);
		Long oldVersion = existingContact.getVersion();
		mgo.save(existingContact);
		Assert.assertTrue("Entity ersion should have incremented at save",
				oldVersion < existingContact.getVersion());
		existingContact = mgo.findOne(searchUserDoeQuery, Contact.class);
		Assert.assertEquals(newFirstname, existingContact.getFirstname());
		Assert.assertEquals("createdAt should not change on update",
				contact1.getCreatedAt(), existingContact.getCreatedAt());
		Assert.assertNotSame("lastModified should change on update",
				contact1.getLastModified(), existingContact.getLastModified());
		
		// update using older version of entity and check that optimistic locking makes it fail
		newFirstname = "JohnOptimisticFails";
		contact1.setFirstname(newFirstname);
		Assert.assertTrue("Older version should have lower version number",
				contact1.getVersion() < existingContact.getVersion());
		try {
			mgo.save(contact1);
			Assert.fail("Error in last write");
		} catch (OptimisticLockingFailureException e) {
			Assert.assertTrue("Optimistic locking should make saving older version save", true);
		}
	 
		// delete
		mgo.remove(searchUserDoeQuery, Contact.class);
		contacts = mgo.findAll(Contact.class);
		Assert.assertEquals(0, contacts.size());
		System.out.println("4. Number of contacts after deletion = " + contacts.size());
	}

}
