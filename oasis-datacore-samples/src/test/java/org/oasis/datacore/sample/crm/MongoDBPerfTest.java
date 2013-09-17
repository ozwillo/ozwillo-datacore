package org.oasis.datacore.sample.crm;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

/**
 * Some performance tests ; and a simple framework for that with : average over
 * x runs, collection of size y, not counting the first (because of setup
 * discrepancies, else having indexes would appear actually worse than having
 * none), randomized value
 *
 * @author mdutoo
 *
 */
public class MongoDBPerfTest {

    private static DB perfs;
    private static MongoClient perfsMongoClient;

    @BeforeClass
    public static void setup() throws UnknownHostException {
        perfsMongoClient = new MongoClient("localhost", 27017);
        // TODO test replica set alternative locally :
        // or, to connect to a replica set, with auto-discovery of the primary, supply a seed list of members
        /*MongoClient mongoClient = new MongoClient(Arrays.asList(new ServerAddress("localhost", 27017),
         new ServerAddress("localhost", 27018),
         new ServerAddress("localhost", 27019)));*/
        perfs = perfsMongoClient.getDB("perfs");
        // if in secure mode, authenticate :
        //boolean auth = perfs.authenticate(myUserName, myPassword);
    }

    @AfterClass
    public static void teardown() throws UnknownHostException {
        perfsMongoClient.close();
    }

    /**
     *
     * no index: testing 10 times with size 10000 (not counting first run)
     * insertTime s: 862
     * findTime ms: 15
     * findExactTime ms: 8
     * findRangeTime ms: 14
     *
     * index on 'int': testing 10 times with size 10000 (not counting first run)
     * insertTime s: 913
     * findTime ms: 14
     * findExactTime ms: 2
     * findRangeTime ms: 3
     *
     * @throws UnknownHostException
     */
    @Test
    public void test() throws UnknownHostException {
        int size = 10000; // 10000000 takes too much time (10s of minutes)
        int runNb = 10;

        System.out.println("\nTest with single index:");

        System.out.println("\nno index:");
        DBCollection coll = resetCollection("noindex");
        tests(coll, size, runNb, false);

        System.out.println("\nindex on 'int':");
        coll = resetCollection("index");
        coll.createIndex(new BasicDBObject("int", 1));
        tests(coll, size, runNb, false);
    }

    private void tests(DBCollection coll, int size, int runNb, boolean compoundIndexes) throws UnknownHostException {
        System.out.println("testing " + runNb + " times with size " + size + " (not counting first run)");

        // first run doesn't count, to avoid setup discrepancies
        // (ex. first run with index actually takes more than than without !)
        test(coll, size, compoundIndexes);

        // second run & initiate times map
        Map<String, Long> times = test(coll, size, compoundIndexes);

        for (int i = 2; i < runNb; i++) {
            Map<String, Long> t = test(coll, size, compoundIndexes);
            for (String key : t.keySet()) {
                times.put(key, times.get(key) + t.get(key));
            }
        }

        System.out.println("insertTime s: " + times.get("insertTime") / runNb / 1000000);
        System.out.println("findTime ms: " + times.get("findTime") / runNb / 1000);
        System.out.println("findExactTime ms: " + times.get("findExactTime") / runNb / 1000);
        System.out.println("findRangeTime ms: " + times.get("findRangeTime") / runNb / 1000);
        if (compoundIndexes) {
            System.out.println("findCompoundRangeTime ms: " + times.get("findCompoundRangeTime") / runNb / 1000);
        }
    }

    public DBCollection resetCollection(String collName) throws UnknownHostException {
        if (perfs.collectionExists(collName)) {
            perfs.getCollection(collName).drop();
        }
        DBCollection coll = perfs.getCollection(collName);
        //perfsColl.remove(new BasicDBObject());
        return coll;
    }

    public Map<String, Long> test(DBCollection coll, int size, boolean compoundIndexes) throws UnknownHostException {
        HashMap<String, Long> times = new HashMap<String, Long>(4);
        long t = System.nanoTime();
        for (int i = 1; i < size; i++) {
            coll.insert(new BasicDBObject("int", (int) (size * Math.random())).append("string", "string" + i).append("rand", (int) (size * Math.random())));
        }
        times.put("insertTime", System.nanoTime() - t);

        t = System.nanoTime();
        coll.find();
        times.put("findTime", System.nanoTime() - t);

        t = System.nanoTime();
        coll.find(new BasicDBObject("int", 7000));
        times.put("findExactTime", System.nanoTime() - t);

        t = System.nanoTime();
        coll.find(new BasicDBObject("int", new BasicDBObject("$gt", 2000))
                .append("int", new BasicDBObject("$lt", 8000)));
        times.put("findRangeTime", System.nanoTime() - t);

        if (compoundIndexes) {
            t = System.nanoTime();
            DBObject explain = coll.find(new BasicDBObject("int", 5000)
                .append("rand", new BasicDBObject("$gt", 2000))
                .append("rand", new BasicDBObject("$lt", 8000))
                .append("string", "string4851")).sort(new BasicDBObject("rand", 1)).explain();
            // Another test request but take much more time to complete
            /*DBObject explain = coll.find(new BasicDBObject("int", new BasicDBObject("$gt", 2000))
             .append("int", new BasicDBObject("$lt", 8000))
             .append("rand", new BasicDBObject("$gt", 2000))
             .append("rand", new BasicDBObject("$lt", 8000))
             .append("string", new BasicDBObject("$regex", "string2*"))
             .append("string", new BasicDBObject("$regex", "string8*"))
             ).explain();*/
            times.put("findCompoundRangeTime", System.nanoTime() - t);
            System.out.println("explain request : " + explain);
        }

        return times;
    }

    /**
     * Performance test for a request with compound indexes and sorting Test
     *
     * with no index: testing 10 times with size 10000 (not counting first run)
     * insertTime s: 741
     * findTime ms: 15
     * findExactTime ms: 7
     * findRangeTime ms: 13
     * findCompoundRangeTime ms: 28012
     *
     * with index on 'int', 'rand' and 'string' fields : testing 10 times with size 10000 (not counting first run)
     * insertTime s: 832
     * findTime ms: 13
     * findExactTime ms: 2
     * findRangeTime ms: 3
     * findCompoundRangeTime ms: 516
     *
     * More efficient with indexes.
     *
     * NOTE : The explain method take a lot of time to complete so be carrefull : do not use it in application code
     * Same test without the explain method call
     *
     * with no index:
     * testing 10 times with size 10000 (not counting first run)
     * insertTime s: 751
     * findTime ms: 15
     * findExactTime ms: 7
     * findRangeTime ms: 13
     * findCompoundRangeTime ms: 23

     * with index on 'int', 'rand' and 'string' :
     * testing 10 times with size 10000 (not counting first run)
     * insertTime s: 910
     * findTime ms: 15
     * findExactTime ms: 2
     * findRangeTime ms: 4
     * findCompoundRangeTime ms: 3
     *
     * @throws UnknownHostException
     */
    @Test
    public void CompoundIndexTest() throws UnknownHostException {
        int size = 10000; // 10000000 takes too much time (10s of minutes)
        int runNb = 10;

        System.out.println("\nTest with compound indexes:");

        System.out.println("\nWith no index:");
        DBCollection coll = resetCollection("noindex");
        tests(coll, size, runNb, true);

        System.out.println("\nWith index on 'int', 'rand' and 'string' :");
        coll = resetCollection("compoundIndex");
        coll.createIndex(new BasicDBObject("int", 1).append("rand", 1).append("string", 1));
        tests(coll, size, runNb, true);
    }
}
