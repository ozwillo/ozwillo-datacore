package org.oasis.datacore.sample.crm;

import java.net.UnknownHostException;
import java.util.Set;

import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class NativeTest {

	@Test
	public void test() throws UnknownHostException {

		MongoClient mongoClient = new MongoClient("localhost" , 27017);
		
		// TODO test replicat set alternative locally :
		// or, to connect to a replica set, with auto-discovery of the primary, supply a seed list of members
		/*MongoClient mongoClient = new MongoClient(Arrays.asList(new ServerAddress("localhost", 27017),
		                                      new ServerAddress("localhost", 27018),
		                                      new ServerAddress("localhost", 27019)));*/
		
		DB db = mongoClient.getDB("datacore_crm");
		
		// if in secure mode, authenticate :
		//boolean auth = db.authenticate(myUserName, myPassword);
		
		Set<String> colls = db.getCollectionNames();

	    System.out.println("Collections:");
		for (String s : colls) {
		    System.out.println(s);
		}


		DBCollection coll = db.getCollection("testCollection");
		//mongoClient.setWriteConcern(WriteConcern.JOURNALED); // TODO test persistence
		//coll.createIndex(new BasicDBObject("i", 1));  // TODO test create index on "i", ascending

		BasicDBObject doc = new BasicDBObject("city", "John").
                append("lastname", "Doe").
                append("count", 1).
                append("info", new BasicDBObject("x", 203).append("y", 102));
		coll.insert(doc);
		
		DBObject myDoc = coll.findOne();
		System.out.println(myDoc);
		
		// TODO native (or spring like at tutorial ??) hierarchical generic model
		
		// TODO sparql-like query
		// TODO metamodel, converters
	}

}
