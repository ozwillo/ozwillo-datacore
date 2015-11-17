package org.oasis.datacore.sample.social;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.oasis.exttest.datacore.sample.social.SocialTestConfiguration;
import org.oasis.social.data.City;
import org.oasis.social.data.CityRepository;
import org.oasis.social.data.Person;
import org.oasis.social.data.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Test Datacore-like uses with Neo4j as impl storage.
 * 
 * BEWARE uses Spring @Config init and not XML as the others.
 * 
 * Base neo4j on spring tests are copied from GRU prototype's.
 * 
 * Conclusion :
 * 
 * why not neo4j :
 * - rather queries than traversal
 * - sure there are transactions (will be mandatory for read in 2.0 !!), but they are logically mutually exclusive with sharding and write scaling
 * - index not compound, on Lucene
 * - properties : no map & list, date must be indexed as a long
 * - bad at analytics
 * - no auto index & labels (in 2.0 but still beta and regularly breaking api)
 * - ... even if there are weighted relationships, (XA) transactions, gis
 *
 * why not rdbms :
 * - can't scale to terabyte without sharding (manually) and avoiding transaction locks
 * - no complex / content-oriented properties
 * - setting up tables and indexes on demand is far easier in mongodb 
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SocialTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
//@ContextConfiguration(locations = { "classpath:oasis-datacore-crm-test-context.xml" }, loader = AnnotationConfigContextLoader.class)
public class SocialGraphTest {

    @Autowired private MongoTemplate template;
    @Autowired private MongoOperations mgo;
    @Autowired private CityRepository cityRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private GraphDatabaseService gdb; // native neo4j

    @Before
    public void setUp() {
        cityRepository.deleteAll();
        personRepository.deleteAll();

        // to delete nodes natively entered in neo4j : city, country, cityByCountry, insee_ville
        Transaction tx = gdb.beginTx();
        try { // trying tx
           
           IndexManager indexManager = gdb.index();
           for (String nodeIndexName : indexManager.nodeIndexNames()) {
              indexManager.forNodes(nodeIndexName).delete();
           }
           for (String relationshipIndexName : indexManager.relationshipIndexNames()) {
              indexManager.forRelationships(relationshipIndexName).delete();
           }

           tx.success();   
        } finally { tx.finish(); }
        
        // setup some cities and people
        // Archibald Haddock lives in Valence, but works for Montélimar.
        // He also has guardianship over Tintin (who is obviously young and immature!), who lives in Montélimar
        // Tournesol lives in Montélimar too.

        City valence = new City("Valence");
        City montélimar = new City("Montélimar");
        cityRepository.save(valence);
        cityRepository.save(montélimar);

        Person tintin = new Person("tintin");
        tintin.setFirstName("Tin");
        tintin.setLastName("Tin");
        tintin.setDateOfBirth(new LocalDate(2000, 2, 26)); // Tintin was born Feb 2nd 2000 if I say so!
        tintin.setPlaceOfResidence(montélimar);
        personRepository.save(tintin);

        assertNotNull(tintin.getId());

        Person haddock = new Person("haddock");
        haddock.setFirstName("Archibald");
        haddock.setLastName("Haddock");
        haddock.setDateOfBirth(new LocalDate(1970, 3, 12));
        haddock.addGuardee(tintin);
        haddock.setPlaceOfResidence(valence);
        haddock.setPlaceOfEmployment(montélimar);
        personRepository.save(haddock);

        assertNotNull(haddock.getId());

        Person tournesol = new Person("tournesol");
        tournesol.setFirstName("Tryphon");
        tournesol.setLastName("Tournesol");
        tournesol.setPlaceOfResidence(montélimar);
        personRepository.save(tournesol);
        assertNotNull(tournesol.getId());
    }

    private static enum RelTypes implements RelationshipType
    {
        KNOWS,
        IN_COUNTRY,
        PATCHES,
        PATCHED_BY
    }
    
    @Test
    public void testDatacoreNative() {
       Transaction tx = gdb.beginTx(); try { // trying tx
          
          Index<Node> countryIndex = gdb.index().forNodes("country");
          
          // WARNING ':' is not supported in field names by index (lucene), so replacing them by '__'
          Node franceCountry = gdb.createNode();
          franceCountry.setProperty("country__name", "France"); // also says it has the "country" aspect
          countryIndex.add(franceCountry, "country__name", "France"); // index (legacy, manual)
   
          assertNotNull(franceCountry.getId());
   
          Node italyCountry = gdb.createNode();
          italyCountry.setProperty("country__name", "Italia");
          italyCountry.setProperty("country__name_fr", "Italie"); // i18n
          countryIndex.add(italyCountry, "country__name", "Italia"); // index (legacy, manual)

          Index<Node> cityIndex = gdb.index().forNodes("city");
          Index<Node> cityByCountryIndex = gdb.index().forNodes("cityByCountry");
          Index<Node> inseeVilleIndex = gdb.index().forNodes("insee_ville");
          
          Node torinoCity = gdb.createNode();
          torinoCity.setProperty("city__name", "Torino"); // also says it has the "city" aspect
          torinoCity.setProperty("city__name_fr", "Turin" );
          Relationship torinoInItalyRel = torinoCity.createRelationshipTo(italyCountry, RelTypes.IN_COUNTRY);
          torinoInItalyRel.setProperty("_q", 9);
          cityIndex.add(torinoCity, "city__name", "Torino"); // index (legacy, manual)
          
          Node lyonCity = gdb.createNode();
          lyonCity.setProperty("city__name", "Lyon");
          Relationship lyonInFranceRel = lyonCity.createRelationshipTo(franceCountry, RelTypes.IN_COUNTRY);
          lyonInFranceRel.setProperty("_q", 9);
          cityIndex.add(lyonCity, "city__name", "Lyon"); // index (legacy, manual)
          cityByCountryIndex.add(lyonCity, "country__name", "France"); // index (legacy, manual)
          lyonCity.setProperty("insee_ville__code", "INSEE.Lyon"); // inheritance / aspect "indee_ville"
          lyonCity.setProperty("insee_ville__population", 500000); // inheritance / aspect
          inseeVilleIndex.add(lyonCity, "insee_ville__code", "INSEE.Lyon"); // index (legacy, manual)
          
          Node valenceCity = gdb.createNode();
          valenceCity.setProperty("city__name", "Valence");
          Relationship valenceInFranceRel = valenceCity.createRelationshipTo(franceCountry, RelTypes.IN_COUNTRY);
          valenceInFranceRel.setProperty("_q", 9);
          cityIndex.add(valenceCity, "city__name", "Valence"); // index (legacy, manual)
          cityByCountryIndex.add(valenceCity, "country__name", "France"); // index (legacy, manual)
          valenceCity.setProperty("insee_ville__code", "INSEE.Valence"); // inheritance / aspect "indee_ville"
          valenceCity.setProperty("insee_ville__population", 80000); // inheritance / aspect
          inseeVilleIndex.add(valenceCity, "insee_ville__code", "INSEE.Valence"); // index (legacy, manual)

          IndexHits<Node> franceHits = countryIndex.get("country__name", "France");
          Assert.assertEquals(franceHits.next().getId(), franceCountry.getId());
          IndexHits<Node> franceCityHits = cityByCountryIndex.get("country__name", "France");
          Assert.assertEquals(2, franceCityHits.size());
          

          Index<Node> patchedCityIndex = gdb.index().forNodes("patched_city");
          Index<Node> patchedCityByCountryIndex = gdb.index().forNodes("patched_cityByCountry");
          Index<Node> patchedInseeVilleIndex = gdb.index().forNodes("patched_insee_ville");
          
          
          Node patchedLyonCity = gdb.createNode();
          // starting from a copy of the node :
          for (String key : lyonCity.getPropertyKeys()) {
             patchedLyonCity.setProperty(key, lyonCity.getProperty(key));
          }
          for (Relationship rel : lyonCity.getRelationships(Direction.OUTGOING)) {
             Relationship patchedRel = patchedLyonCity.createRelationshipTo(rel.getEndNode(), rel.getType());
             for (String key : rel.getPropertyKeys()) {
                patchedRel.setProperty(key, rel.getProperty(key));
             }
          }
          patchedCityIndex.add(patchedLyonCity, "city__name", "Lyon"); // index (legacy, manual)
          patchedCityByCountryIndex.add(patchedLyonCity, "country__name", "France"); // index (legacy, manual)
          patchedInseeVilleIndex.add(patchedLyonCity, "insee_ville__code", "INSEE.Lyon"); // index (legacy, manual)
          // NB. INCOMING relationships are either forbidden or its subgraph/subtree must also be copied
          // patching some props :
          patchedLyonCity.setProperty("insee_ville__population", 500500); // inheritance / aspect
          // OPT patch relationship :
          Relationship lyonPatchesRel = patchedLyonCity.createRelationshipTo(lyonCity, RelTypes.PATCHES);
          // OPT2 inverse patch relationship :
          Relationship lyonPatchedRel = lyonCity.createRelationshipTo(patchedLyonCity, RelTypes.PATCHED_BY);


          IndexHits<Node> patchedFranceCityHits = patchedCityByCountryIndex.get("country__name", "France");
          Assert.assertEquals(1, patchedFranceCityHits.size());
          
       tx.success();
       
       } finally { tx.finish(); }
    }

    @Test
    public void testCityRepository() {
        City turin = new City("Turin");
        assertNull(turin.getId());

        cityRepository.save(turin);
        assertNotNull(turin.getId());

        City torino = cityRepository.findByName("Turin");
        assertNotNull(torino);
        assertEquals(turin.getId(), torino.getId());

        cityRepository.delete(torino);

        torino = cityRepository.findByName("Turin");
        assertNull(torino);
    }

    @Test
    public void testPersonRepository() {



        Person haddock = personRepository.findByUsername("haddock");
        assertEquals("Archibald", haddock.getFirstName());

        Person tintin = personRepository.findByUsername("tintin");
        City montélimar = cityRepository.findByName("Montélimar");

//        // now let's find everyone who lives in Valence
//        City valence = cityRepository.findByName("Valence");
//        assertEquals(1, valence.getCitizens().size());
//        System.out.println(valence.getCitizens());
//        assertTrue(valence.getCitizens().contains(haddock));

        // test won't work since at this stage the transaction is ended - so we'd need to either have a
        // longer-running tx or eagerly fetch data - neither solution is pleasing, in fact.

        // for person-fields we do have @Fetch, so that's ok
        // let's find where haddock lives
        assertEquals("Valence", haddock.getPlaceOfResidence().getName());
        assertTrue(haddock.getGuardees().contains(tintin));
        assertEquals(montélimar, haddock.getPlaceOfEmployment());
        assertEquals(montélimar, haddock.getGuardees().iterator().next().getPlaceOfResidence());
    }

}
