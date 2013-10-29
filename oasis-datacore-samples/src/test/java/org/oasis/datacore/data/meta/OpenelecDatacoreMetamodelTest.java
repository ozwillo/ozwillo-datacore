package org.oasis.datacore.data.meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.data.DCEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-crm-test-context.xml" })
public class OpenelecDatacoreMetamodelTest {
   
   @Autowired
   private MongoOperations mgo;

   @Test
   public void testByHand() {
      // cleanup
      mgo.dropCollection("city");
      mgo.dropCollection("bureauDeVote");
      mgo.dropCollection("insee.ville");
      
      // init datacore collections
      mgo.createCollection("city");
      mgo.createCollection("bureauDeVote");
      mgo.createCollection("insee.ville");
      
      // create new city in datacore (independently of OpenElec bureau's collectivite)
      DCEntity lyonCity = new DCEntity();
      lyonCity.setProperties(new HashMap<String,Object>());
      lyonCity.getProperties().put("name", "Lyon"); // TODO Q or subentity ? or even sub-resource ?????? and how to index, localized ??????????????,
      String lyonCityUri = "http://data.oasis-eu.org/city/France/Lyon"; // uri (from id query params !?) TODO type as field, not prefix
      lyonCity.setUri(lyonCityUri); // TODO Q not obligatory if embedded ? or then only sub-uri ??
      mgo.save(lyonCity, "city"); // TODO Q collection = use case != rdf:type ? or even several types ???
      Assert.assertNotNull(lyonCity.getId());

      // getting lyonCity as REST (in fr TODO more) :
      // { _uri:"...", _v:0, (type:"city",) name:"Lyon" } TODO Q also type / collection ? (yes easier for JAXRS etc.) ; TODO Q also _id ??
      // and as simili RDF : also type:"igeo:Commune", possible name mapped to rdf:label
      
      
      // create bureau in datacore
      DCEntity bureauDeVote = new DCEntity();
      bureauDeVote.setProperties(new HashMap<String,Object>());
      bureauDeVote.getProperties().put("code", "Lyon325"); // TODO index (in metamodel) obviously
      /*
      bureau.setLibelle_bureau("Lyon 325 École Antoine Charial");
      bureau.setAdresse1("École Antoine Charial");
      bureau.setAdresse2("25 rue Antoine Charial");
      bureau.setAdresse3("69003 Lyon");
      bureau.setCode_canton("canton1");
       */
      String bureauDeVoteUri = "http://data.oasis-eu.org/bureauDeVote/Lyon325"; // uri (from id query params !?)
      bureauDeVote.setUri(bureauDeVoteUri); // TODO Q not obligatory if embedded ? or then only sub-uri ??
      mgo.save(bureauDeVote, "bureauDeVote");
      Assert.assertNotNull(bureauDeVote.getId());
      Assert.assertEquals(new Long(0), bureauDeVote.getVersion());
      DCEntity foundBureauDeVote = mgo.findOne(new Query(new Criteria("id").is(bureauDeVote.getId())), DCEntity.class, "bureauDeVote");
      Assert.assertNotNull(foundBureauDeVote);
      Assert.assertEquals(new Long(0), foundBureauDeVote.getVersion());
      mgo.save(bureauDeVote, "bureauDeVote"); // NB. in spring data 1.2.0 would fail on bug https://jira.springsource.org/browse/DATAMONGO-620
      // (MongoTemplate.doSaveVersioned(...) does not consider collection handed into the method)
      Assert.assertEquals(new Long(1), bureauDeVote.getVersion());
      
      // setting collectivite of bureau as an embedded (TODO Q enriched ??) readonly copy
      bureauDeVote.getProperties().put("bureauCollectivite", lyonCity); // TODO shorter names (auto ??)
      mgo.save(bureauDeVote, "bureauDeVote");
      
      // getting bureauDeVote as REST (in fr TODO more) :
      // { _uri:"...", _v:1, (type:"bureauDeVote",) code:"Lyon325", bureauCollectivite:{ _uri:"...", (type:"city",) _v:0, name:"Lyon" } } TODO Q also type / collection ? (yes easier for JAXRS etc.)
      // and as simili RDF : also type:"igeo:Commune", possible name mapped to rdf:label
      
      // querying bureauDeVote by city :
      // query("bureauDeVote", { "bureauCollectivite.name":"Lyon" })
      
      // updating Lyon city :
      // (done by CityManager)
      // find all copies to update, using metamodel => list of refs (collection, id, path)
      // create CopiesUpdateRetriableCommand/Task (rather than Transaction) with their list, diff, undo & original Lyon city ref
      // lock original Lyon city ref (or whole CityManager i.e. city collection)
      // for each copy ref'd in task, apply diff and remove it from task
      // if it fails at somepoint, CityManager can do it (lazily on read / manage or auto) for all remaining ref'd ones in task
      // if another modif is asked in CityManager, it can't be done, save if it is all cancelled and undone first
      // finally, diff is applied on original, lock removed and task said "done"
      
      
      // now also INSEE city having additional fields (and TODO Q OPT possibly partial field copy) :
      
      // create new city in datacore (independently of OpenElec bureau's collectivite)
      DCEntity lyonInseeVille = new DCEntity();
      lyonInseeVille.setProperties(new HashMap<String,Object>());
      // TODO TODO partial copy : label fr only
      lyonInseeVille.getProperties().put("name", lyonCity.getProperties().get("name")); // TODO set
      // additional fields :
      lyonInseeVille.getProperties().put("inseeCode", "INSEE.Lyon"); // TODO set
      lyonInseeVille.setUri(lyonCityUri); // same uri...
      mgo.save(lyonInseeVille, "insee.ville"); // but different collection !! (which incidentally is also a separate rdf:type)
      Assert.assertNotNull(lyonInseeVille.getId());

      // getting lyonInseeVille as REST (in fr TODO more) :
      // { _uri:"...", _v:0, name:"Lyon", inseeCode:"INSEE.Lyon" } TODO Q also type / collection ? (yes easier for JAXRS etc.) ; TODO Q also _id ??
      // and as simili RDF : also type:"igeo:Commune", possible name mapped to rdf:label
      // OR using rather embedding for copies : { _uri:"...", (type:"insee.ville",) _v:2, inseeCode:"INSEE.Lyon", _src_0:{ _uri:"...", _v:0, (type:"city",) name:"Lyon" } }
      
      // NB. getting all info of all collections about a given city is possible but costly
      // => the right way to do it is another collection containing for each uri a copy of all sources fields (with metamodel checking cycles)
      // in REST, using embedding for copies could be done using _src_i OR __src[i] where i is the order in which the copy is done...
      
      // querying inseeVille :
      // query("insee.ville", { "inseeCode":"INSEE.Lyon" OR "_src0.name":"Lyon" OR "_src.name":"Lyon" })
      
      // NB. as is, there is no way to query bureauDeVote by inseeVille in a single query
      // however that could be done :
      // * either "by hand" using 2 queries,
      // * or if done often by a consumer by defining a new bureauDeVote collection with additional city fields copied from insee.ville
      // (ideally, the consumer would have to tell bureauDeVote's master (an OpenElec instance) about it,)
      // this way it could choose to enrich its own data rather than have heavier copy / synchronization jobs on its data)
      
      // NB. there must be no cycles !
      // reciprocal copy of different fields (could be done but) is better replaced by a commonly managed / updated reference collection
   }

   
   /**
    * Produces logs :
    *
Defined DCResourceModel[country]
with indexes [{ "v" : 1 , "key" : { "_id" : 1} , "ns" : "datacore_crm.country" , "name" : "_id_"},
   { "v" : 1 , "key" : { "_p.name" : 1} , "ns" : "datacore_crm.country" , "name" : "_p.name_1"}]
Defined DCResourceModel[city copies subresources : inCountry=DCResourceModel[country]]
with indexes [{ "v" : 1 , "key" : { "_id" : 1} , "ns" : "datacore_crm.city" , "name" : "_id_"},
   { "v" : 1 , "key" : { "_p.name" : 1 , "_p.inCountry._p.name" : 1} ,
   "ns" : "datacore_crm.city" , "name" : "_p.name_1__p.inCountry._p.name_1"},
   { "v" : 1 , "key" : { "_p.inCountry._p.name" : 1} , "ns" : "datacore_crm.city" , "name" : "_p.inCountry._p.name_1"}]
Defined DCResourceModel[bureauDeVote copies subresources : bureauCollectivite=DCResourceModel[city]]
with indexes [{ "v" : 1 , "key" : { "_id" : 1} , "ns" : "datacore_crm.bureauDeVote" , "name" : "_id_"},
   { "v" : 1 , "key" : { "code" : 1} , "ns" : "datacore_crm.bureauDeVote" , "name" : "code_1"},
   { "v" : 1 , "key" : { "_p.bureauCollectivite._p.name" : 1 , "_p.bureauCollectivite._p.inCountry._p.name" : 1} ,
   "ns" : "datacore_crm.bureauDeVote" , "name" : "collectivite_name__collectivite_inCountry_name"},
   { "v" : 1 , "key" : { "_p.bureauCollectivite._p.inCountry._p.name" : 1} , "ns" : "datacore_crm.bureauDeVote" ,
   "name" : "_p.bureauCollectivite._p.inCountry._p.name_1"}]
Defined DCResourceModel[insee.ville extends city]
with indexes [{ "v" : 1 , "key" : { "_id" : 1} , "ns" : "datacore_crm.insee.ville" , "name" : "_id_"},
   { "v" : 1 , "key" : { "_p.inseeCode" : 1} , "ns" : "datacore_crm.insee.ville" , "name" : "_p.inseeCode_1"},
   { "v" : 1 , "key" : { "_p.name" : 1 , "_p.inCountry._p.name" : 1} , "ns" : "datacore_crm.insee.ville" ,
   "name" : "_p.name_1__p.inCountry._p.name_1"}, { "v" : 1 , "key" : { "_p.inCountry._p.name" : 1} ,
   "ns" : "datacore_crm.insee.ville" , "name" : "_p.inCountry._p.name_1"}]
    * 
    * and :
    * 
For query on inseeVille { "_p.name" : "Lyon" , "_p.inCountry._p.name" : "France"} ,
   { "cursor" : "BtreeCursor _p.name_1__p.inCountry._p.name_1" , "isMultiKey" : false , "n" : 1 ,
   "nscannedObjects" : 1 , "nscanned" : 1 , "nscannedObjectsAllPlans" : 1 , "nscannedAllPlans" : 1 ,
   "scanAndOrder" : false , "indexOnly" : false , "nYields" : 0 , "nChunkSkips" : 0 , "millis" : 0 ,
   "indexBounds" : { "_p.name" : [ [ "Lyon" , "Lyon"]] , "_p.inCountry._p.name" : [ [ "France" , "France"]]} ,
   "allPlans" : [ { "cursor" : "BtreeCursor _p.name_1__p.inCountry._p.name_1" , "n" : 1 ,
   "nscannedObjects" : 1 , "nscanned" : 1 , "indexBounds" : { "_p.name" : [ [ "Lyon" , "Lyon"]] ,
   "_p.inCountry._p.name" : [ [ "France" , "France"]]}}] , "server" : "mdutoo-laptop2:27017"}
    * 
    * @throws Exception
    */
   @Test
   public void testMetamodel() throws Exception {

      // init metamodel (TODO LATER2 lock it also while doing it) :

      DCResourceModel countryModel = new DCResourceModel("country");
      
      DCResourceModel cityModel = new DCResourceModel();
      cityModel.setName("city"); // TODO in param or lyonCity DCEntity ?? TODO rather rdf_type:{x,y} list ???
      cityModel.getCopiedSubresourceModels().add(new CopiedSubresourceDCModel("inCountry", countryModel)); // external subresource sample
      
      DCResourceModel bureauDeVoteModel = new DCResourceModel();
      bureauDeVoteModel.setName("bureauDeVote"); // TODO in param or lyonCity DCEntity ?? TODO rather rdf_type:{x,y} list ???
      bureauDeVoteModel.getCopiedSubresourceModels().add(new CopiedSubresourceDCModel("bureauCollectivite", cityModel)); // external subresource sample
      
      Map<String,DCEntity> copiedSubresourceDCEntityMap = new HashMap<String,DCEntity>(0);
      ///copiedSubresourceDCEntities.put("mairie", lyonMairieDCEntity);
      // TODO put lock on them while copying them !!!!! (like at update, copy at creation should be done in an undoable (BUT not redoable since return must be synchronous) task model)
      // (by putting a lock that will prevent new copy tasks, waiting that current ones end or canceling them, then only allowing changes,
      // then removing the lock ; possibly in another "draft" model collection to allow changes to be done in parallel in "staging"-like mode)
      DCResourceModel inseeVilleModel = new DCResourceModel("insee.ville");
      inseeVilleModel.setExtendedResourceModel(new ExtendedResourceDCModel(cityModel));
      inseeVilleModel.setCopiedSubresourceModels(null); // TODO mairie:mairieModel...

      ///DCResourceModel lyonCityModel = dcMetamodel.getModel("city");
      ///DCResourceModel lyonInseeVilleModel = dcMetamodel.getModel("insee.ville");

      // TODO : complex props, embedded subresources, copied subresources & indexes on all

      // data cleanup
      mgo.dropCollection(countryModel.getName());
      mgo.dropCollection(cityModel.getName());
      mgo.dropCollection(bureauDeVoteModel.getName());
      mgo.dropCollection(inseeVilleModel.getName());
      
      // init datacore collections & indexes :
      // (TODO define indexes in metamodel / according to queries, possibly auto from extended / embedded)
      // TODO FAQ indexes and (spring data) Criteria queries must use actual mongo field names (rather than java ones) even when @Field !!
      
      DBCollection countryColl = mgo.createCollection(countryModel.getName());
      countryColl.createIndex(new BasicDBObject("_p.name", 1)); // query country by name
      System.out.println("Defined " + countryModel + "\nwith indexes " + mgo.getCollection(countryModel.getName()).getIndexInfo());
      
      DBCollection cityColl = mgo.createCollection(cityModel.getName());
      cityColl.createIndex(new BasicDBObject("_p.name", 1)
         .append("_p.inCountry._p.name", 1)); // query city by name and OPT (embedded) country name
      ///cityColl.createIndex(new BasicDBObject("precincts.name", 1)); // TODO embedded subresource sample
      cityColl.createIndex(new BasicDBObject("_p.inCountry._p.name", 1)); // query by (embedded) country name
      System.out.println("Defined " + cityModel + "\nwith indexes " + mgo.getCollection(cityModel.getName()).getIndexInfo());
      
      DBCollection bureauDeVoteColl = mgo.createCollection(bureauDeVoteModel.getName());
      bureauDeVoteColl.createIndex(new BasicDBObject("code", 1)); // by code
      bureauDeVoteColl.createIndex(new BasicDBObject("_p.bureauCollectivite._p.name", 1)
         .append("_p.bureauCollectivite._p.inCountry._p.name", 1), // query by (embedded) city name and OPT (doubly embedded) country name
         new BasicDBObject("name", "collectivite_name__collectivite_inCountry_name") // TODO & FAQ else index name too long (128 char max, no '(' etc.)
         .append("ns", bureauDeVoteColl.getFullName())); // FAQ db prefix ex. datacore.bureauDeVote. ! else error 10096 "invalid ns to index", though not required in js CLI
      bureauDeVoteColl.createIndex(new BasicDBObject("_p.bureauCollectivite._p.inCountry._p.name", 1)); // query by (doubly embedded) country name
      System.out.println("Defined " + bureauDeVoteModel + "\nwith indexes " + mgo.getCollection(bureauDeVoteModel.getName()).getIndexInfo());
      
      DBCollection inseeVilleColl = mgo.createCollection(inseeVilleModel.getName());
      inseeVilleColl.createIndex(new BasicDBObject("_p.inseeCode", 1)); // query by inseeCode
      inseeVilleColl.createIndex(new BasicDBObject("_p.name", 1)
         .append("_p.inCountry._p.name", 1)); // query by (extended) city name and OPT  (embedded) country name
      inseeVilleColl.createIndex(new BasicDBObject("_p.inCountry._p.name", 1)); // query by (embedded) country name TODO only french cities ???
      System.out.println("Defined " + inseeVilleModel + "\nwith indexes " + mgo.getCollection(inseeVilleModel.getName()).getIndexInfo());
      
      // create resource data instances :
      
      // create country in datacore
      String franceCountryUri = DCResourceModel.DATACORE_BASE_URI + countryModel.getName()
            + "/France"; // uri (from id query params !?) TODO type as field, not prefix
      DCEntity franceCountry = newResource(countryModel, franceCountryUri, null); // TODO also uri
      franceCountry.getProperties().put("name", "France");
      mgo.save(franceCountry, countryModel.getName());
      Assert.assertEquals(franceCountry.getModelName(), countryModel.getName());
      
      // create new city in datacore (independently of OpenElec bureau's collectivite)
      Map<String, DCEntity> lyonCityCopiedSubresources = new HashMap<String, DCEntity>(1);
      lyonCityCopiedSubresources.put("inCountry", franceCountry);
      String lyonCityUri = DCResourceModel.DATACORE_BASE_URI + cityModel.getName()
            + "/France/Lyon"; // uri (from id query params !?)
      DCEntity lyonCity = newResource(cityModel, lyonCityUri, lyonCityCopiedSubresources); // TODO also uri
      // TODO have a transient reference to model in entity ?? fill it in lifecycle event ??? AND / OR baseType ?
      ////lyonCity.setProperties(new HashMap<String,Object>()); // TODO does doing it by default it take (too much) place in mongodb ???
      lyonCity.getProperties().put("name", "Lyon"); // TODO Q or subentity ? or even sub-resource ?????? and how to index, localized ??????????????,
      ///lyonCity.getProperties().put("precincts", precinctMap); // embedded sample TODO Q uri not obligatory if embedded ? or then only sub-uri ??
      mgo.save(lyonCity, cityModel.getName()); // TODO Q collection = use case != rdf:type ? or even several types ???
      Assert.assertNotNull(lyonCity.getId());
      Assert.assertEquals(lyonCity.getUri(), lyonCityUri);
      Assert.assertEquals(lyonCity.getModelName(), cityModel.getName());
      Assert.assertEquals(lyonCity.getProperties().get("inCountry"), franceCountry);

      // getting lyonCity as REST (in fr TODO more) :
      // { _uri:"...", _v:0, (type:"city",) name:"Lyon" } TODO Q also type / collection ? (yes easier for JAXRS etc.) ; TODO Q also _id ??
      // and as simili RDF : also type:"igeo:Commune", possible name mapped to rdf:label
      
      
      // create bureau in datacore
      Map<String, DCEntity> bureauDeVoteSubresources = new HashMap<String, DCEntity>(1);
      bureauDeVoteSubresources.put("bureauCollectivite", lyonCity); // TODO shorter names (auto ??)
      String bureauDeVoteUri = DCResourceModel.DATACORE_BASE_URI + bureauDeVoteModel.getName()
            + "/Lyon325"; // uri (from id query params !?), TODO only suffix ??
      DCEntity bureauDeVote = newResource(bureauDeVoteModel, bureauDeVoteUri, bureauDeVoteSubresources); // TODO also uri
      // TODO have a transient reference to model in entity ?? fill it in lifecycle event ??? AND / OR baseType ?
      ////lyonCity.setProperties(new HashMap<String,Object>()); // TODO does doing it by default it take (too much) place in mongodb ???
      bureauDeVote.getProperties().put("code", "Lyon325"); // TODO index (in metamodel) obviously
      /*
      bureau.setLibelle_bureau("Lyon 325 École Antoine Charial");
      bureau.setAdresse1("École Antoine Charial");
      bureau.setAdresse2("25 rue Antoine Charial");
      bureau.setAdresse3("69003 Lyon");
      bureau.setCode_canton("canton1");
       */
      mgo.save(bureauDeVote, bureauDeVoteModel.getName());
      Assert.assertNotNull(bureauDeVote.getId());
      DCEntity foundBureauDeVote = mgo.findOne(new Query(new Criteria("id").is(bureauDeVote.getId())), DCEntity.class, "bureauDeVote");
      Assert.assertNotNull(foundBureauDeVote);
      Assert.assertNotNull(foundBureauDeVote.getId());
      Assert.assertEquals(foundBureauDeVote.getUri(), bureauDeVoteUri);
      Assert.assertEquals(foundBureauDeVote.getModelName(), bureauDeVoteModel.getName());
      Assert.assertTrue(foundBureauDeVote.getProperties().get("bureauCollectivite") instanceof DCEntity);
      Assert.assertTrue(((DCEntity) foundBureauDeVote.getProperties().get("bureauCollectivite"))
            .getProperties().get("inCountry") instanceof DCEntity);
      Assert.assertEquals(((DCEntity) ((DCEntity) foundBureauDeVote.getProperties().get("bureauCollectivite"))
            .getProperties().get("inCountry")).getId(), franceCountry.getId());
      
      // setting collectivite of bureau as an embedded (TODO Q enriched ??) readonly copy TODO allow optional ?
      ////bureauDeVote.getProperties().put("bureauCollectivite", lyonCity);
      ////mgo.save(bureauDeVote, bureauDeVoteModel.getName());
      
      // getting bureauDeVote as REST (in fr TODO more) :
      // { _uri:"...", _v:1, (type:"bureauDeVote",) code:"Lyon325", bureauCollectivite:{ _uri:"...", (type:"city",) _v:0, name:"Lyon" } } TODO Q also type / collection ? (yes easier for JAXRS etc.)
      // and as simili RDF : also type:"igeo:Commune", possible name mapped to rdf:label
      
      // querying bureauDeVote by city :
      // query("bureauDeVote", { "bureauCollectivite.name":"Lyon" })
      
      // updating Lyon city :
      // (done by CityManager)
      // find all copies to update, using metamodel => list of refs (collection, id, path)
      // create CopiesUpdateRetriableCommand/Task (rather than Transaction) with their list, diff, undo & original Lyon city ref
      // lock original Lyon city ref (or whole CityManager i.e. city collection)
      // for each copy ref'd in task, apply diff and remove it from task
      // if it fails at somepoint, CityManager can do it (lazily on read / manage or auto) for all remaining ref'd ones in task
      // if another modif is asked in CityManager, it can't be done, save if it is all cancelled and undone first
      // finally, diff is applied on original, lock removed and task said "done"
      
      
      // now also INSEE city having additional fields (and TODO Q OPT possibly partial field copy) :
      
      // create new city in datacore (independently of OpenElec bureau's collectivite)
       
      DCEntity lyonInseeVille = newResource(inseeVilleModel, lyonCity, copiedSubresourceDCEntityMap);
      // NB. uri stays same as extended
      // additional fields :
      lyonInseeVille.getProperties().put("inseeCode", "INSEE.Lyon"); // TODO set, TODO check that fields are not copied (readonly), TODO and not in model OR only when copiable / retrievable by others ??
      mgo.save(lyonInseeVille, inseeVilleModel.getName()); // but different collection !! (which incidentally is also a separate rdf:type)
      Assert.assertNotNull(lyonInseeVille.getId());
      Assert.assertEquals(lyonInseeVille.getUri(), lyonCityUri);
      Assert.assertEquals(lyonInseeVille.getModelName(), inseeVilleModel.getName());
      Assert.assertTrue(lyonInseeVille.getProperties().get("inCountry") instanceof DCEntity);
      Assert.assertEquals(((DCEntity) lyonInseeVille.getProperties().get("inCountry"))
            .getId(), franceCountry.getId());

      // getting lyonInseeVille as REST (in fr TODO more) :
      // { _uri:"...", _v:0, name:"Lyon", inseeCode:"INSEE.Lyon" } TODO Q also type / collection ? (yes easier for JAXRS etc.) ; TODO Q also _id ??
      // and as simili RDF : also type:"igeo:Commune", possible name mapped to rdf:label
      // OR using rather embedding for copies : { _uri:"...", (type:"insee.ville",) _v:2, inseeCode:"INSEE.Lyon", _src_0:{ _uri:"...", _v:0, (type:"city",) name:"Lyon" } }
      
      // NB. getting all info of all collections about a given city is possible but costly
      // => the right way to do it is another collection containing for each uri a copy of all sources fields (with metamodel checking cycles)
      // in REST, using embedding for copies could be done using _src_i OR __src[i] where i is the order in which the copy is done...
      
      // querying inseeVille :
      // query("insee.ville", { "inseeCode":"INSEE.Lyon" OR "_src0.name":"Lyon" OR "_src.name":"Lyon" })
      // by INSEE code :
      List<DCEntity> lyonInseeVilleRes = mgo.find(new Query(new Criteria("_p.inseeCode").is("INSEE.Lyon")),
            DCEntity.class, inseeVilleModel.getName());
      Assert.assertNotNull(lyonInseeVilleRes);
      Assert.assertEquals(1, lyonInseeVilleRes.size());
      Assert.assertEquals(lyonInseeVille.getId(), lyonInseeVilleRes.get(0).getId());
      // by (extended) city (using first part of city & country index) :
      lyonInseeVilleRes = mgo.find(new Query(new Criteria("_p.name").is("Lyon")),
            DCEntity.class, inseeVilleModel.getName());
      Assert.assertNotNull(lyonInseeVilleRes);
      Assert.assertEquals(1, lyonInseeVilleRes.size());
      Assert.assertEquals(lyonInseeVille.getId(), lyonInseeVilleRes.get(0).getId());
      // by (extended) city and (extended and embedded) country :
      lyonInseeVilleRes = mgo.find(new Query(new Criteria("_p.name").is("Lyon")
            .and("_p.inCountry._p.name").is("France")),
            DCEntity.class, inseeVilleModel.getName());
      Assert.assertNotNull(lyonInseeVilleRes);
      Assert.assertEquals(1, lyonInseeVilleRes.size());
      Assert.assertEquals(lyonInseeVille.getId(), lyonInseeVilleRes.get(0).getId());
      BasicDBObject lyonInseeVilleByCityAndCountryDBO = new BasicDBObject("_p.name", "Lyon")
         .append("_p.inCountry._p.name", "France");
      System.out.println("For query on inseeVille " + lyonInseeVilleByCityAndCountryDBO + " , "
         + mgo.getCollection(cityModel.getName()).find(lyonInseeVilleByCityAndCountryDBO).explain());
      // by (embedded) country :
      List<DCEntity> franceInseeVilleRes = mgo.find(new Query(new Criteria("_p.inCountry._p.name").is("France")),
            DCEntity.class, inseeVilleModel.getName());
      Assert.assertNotNull(franceInseeVilleRes);
      Assert.assertEquals(1, franceInseeVilleRes.size());
      Assert.assertEquals(lyonInseeVille.getId(), franceInseeVilleRes.get(0).getId());
      
      // NB. as is, there is no way to query bureauDeVote by inseeVille in a single query
      // however that could be done :
      // * either "by hand" using 2 queries,
      // * or if done often by a consumer by defining a new bureauDeVote collection with additional city fields copied from insee.ville
      // (ideally, the consumer would have to tell bureauDeVote's master (an OpenElec instance) about it,)
      // this way it could choose to enrich its own data rather than have heavier copy / synchronization jobs on its data)
      
      // querying bureauDeVote :
      // by code :
      List<DCEntity> bureauDeVoteRes = mgo.find(new Query(new Criteria("_p.code").is("Lyon325")),
            DCEntity.class, bureauDeVoteModel.getName());
      Assert.assertNotNull(bureauDeVoteRes);
      Assert.assertEquals(1, bureauDeVoteRes.size());
      Assert.assertEquals(bureauDeVote.getId(), bureauDeVoteRes.get(0).getId());
      // by (embedded) city (using first part of city and country index) :
      bureauDeVoteRes = mgo.find(new Query(new Criteria("_p.bureauCollectivite._p.name").is("Lyon")),
            DCEntity.class, bureauDeVoteModel.getName());
      Assert.assertNotNull(bureauDeVoteRes);
      Assert.assertEquals(1, bureauDeVoteRes.size());
      Assert.assertEquals(bureauDeVote.getId(), bureauDeVoteRes.get(0).getId());
      // by (embedded) city and (doubly embedded) country :
      bureauDeVoteRes = mgo.find(new Query(new Criteria("_p.bureauCollectivite._p.name").is("Lyon")
            .and("_p.bureauCollectivite._p.inCountry._p.name").is("France")),
            DCEntity.class, bureauDeVoteModel.getName());
      Assert.assertNotNull(bureauDeVoteRes);
      Assert.assertEquals(1, bureauDeVoteRes.size());
      Assert.assertEquals(bureauDeVote.getId(), bureauDeVoteRes.get(0).getId());
      // by (doubly embedded) country :
      bureauDeVoteRes = mgo.find(new Query(new Criteria("_p.bureauCollectivite._p.inCountry._p.name").is("France")),
            DCEntity.class, bureauDeVoteModel.getName());
      Assert.assertNotNull(bureauDeVoteRes);
      Assert.assertEquals(1, bureauDeVoteRes.size());
      Assert.assertEquals(bureauDeVote.getId(), bureauDeVoteRes.get(0).getId());

      // querying city :
      // query("city", { "name":"Lyon" OR "inCountry.name":"Lyon" })
      // by name (using first part of name and country index) :
      List<DCEntity> lyonCityRes = mgo.find(new Query(new Criteria("_p.name").is("Lyon")),
            DCEntity.class, cityModel.getName());
      Assert.assertNotNull(lyonCityRes);
      Assert.assertEquals(1, lyonCityRes.size());
      Assert.assertEquals(lyonCity.getId(), lyonCityRes.get(0).getId());
      // by name and (embedded) country :
      lyonCityRes = mgo.find(new Query(new Criteria("_p.name").is("Lyon")
            .and("_p.inCountry._p.name").is("France")),
            DCEntity.class, cityModel.getName());
      Assert.assertNotNull(lyonCityRes);
      Assert.assertEquals(1, lyonCityRes.size());
      Assert.assertEquals(lyonCity.getId(), lyonCityRes.get(0).getId());
      // by country :
      lyonCityRes = mgo.find(new Query(new Criteria("_p.inCountry._p.name").is("France")),
            DCEntity.class, cityModel.getName());
      Assert.assertNotNull(lyonCityRes);
      Assert.assertEquals(1, lyonCityRes.size());
      Assert.assertEquals(lyonCity.getId(), lyonCityRes.get(0).getId());
      

      // NB. there must be no cycles !
      // reciprocal copy of different fields (could be done but) is better replaced by a commonly managed / updated reference collection
      
      
      // i18n ALT other props :
      
      // indexes :
      // create one per query AND per language
      countryColl.createIndex(new BasicDBObject("_p.name@es", 1)); // query country by name
      countryColl.createIndex(new BasicDBObject("_p.name@fr", 1)); // query country by name
      countryColl.createIndex(new BasicDBObject("_p.name@default", 1)); // query country by name
      cityColl.createIndex(new BasicDBObject("_p.name@es", 1)
         .append("_p.inCountry._p.name@es", 1)); // query city by name and OPT (embedded) country name
      cityColl.createIndex(new BasicDBObject("_p.name@fr", 1)
         .append("_p.inCountry._p.name@fr", 1)); // query city by name and OPT (embedded) country name
      cityColl.createIndex(new BasicDBObject("_p.name@default", 1)
         .append("_p.inCountry._p.name@default", 1)); // "default" locale
      cityColl.createIndex(new BasicDBObject("_p.inCountry._p.name@es", 1)); // query by (embedded) country name
      cityColl.createIndex(new BasicDBObject("_p.inCountry._p.name@fr", 1)); // query by (embedded) country name
      cityColl.createIndex(new BasicDBObject("_p.inCountry._p.name@default", 1)); // "default" locale
      
      // samples
      franceCountry.getProperties().put("name@es", "Francia"); // name : [ "Francia@es", "France@fr", ?"France@default" ] ^France@.*$ OU France@default
      franceCountry.getProperties().put("name@default", "Francia"); // TODO auto add default : at first time AND / OR according to its model's default...
      franceCountry.getProperties().put("name@fr", "France"); // name@default:"France" OR name.default:"France" ; @i18n ; default conf dans model
      franceCountry.getProperties().put("name@en", "France");
      mgo.save(franceCountry, countryModel.getName());
      Assert.assertEquals(franceCountry.getProperties().get("name@es"), "Francia");
      
      updateResource(lyonCity, cityModel); // updating lyonCity withh latest embedded franceCountry changes
      lyonCity.getProperties().put("name@es", "Lyon");
      lyonCity.getProperties().put("name@default", "Lyon"); // TODO auto add default : at first time AND / OR according to its model's default...
      lyonCity.getProperties().put("name@fr", "Lyon");
      // NO EN VALUE
      mgo.save(lyonCity, cityModel.getName());
      Assert.assertEquals(lyonCity.getProperties().get("name@es"), "Lyon");
      Assert.assertTrue(lyonCity.getProperties().get("inCountry") instanceof DCEntity);
      Assert.assertEquals(((DCEntity) lyonCity.getProperties().get("inCountry"))
            .getProperties().get("name@es"), "Francia");
      
      // queries
      // by es name :
      List<DCEntity> franceCountryRes = mgo.find(new Query(new Criteria("_p.name@es").is("Francia")),
            DCEntity.class, countryModel.getName());
      Assert.assertNotNull(franceCountryRes);
      Assert.assertEquals(1, franceCountryRes.size());
      Assert.assertEquals(franceCountry.getId(), franceCountryRes.get(0).getId());
      // by es name and (embedded) es country :
      lyonCityRes = mgo.find(new Query(new Criteria("_p.name@es").is("Lyon")
            .and("_p.inCountry._p.name@es").is("Francia")),
            DCEntity.class, cityModel.getName());
      Assert.assertNotNull(lyonCityRes);
      Assert.assertEquals(1, lyonCityRes.size());
      Assert.assertEquals(lyonCity.getId(), lyonCityRes.get(0).getId());
      // default behaviour - by default name and (embedded) default country :
      lyonCityRes = mgo.find(new Query(new Criteria("_p.name@default").is("Lyon")
            .and("_p.inCountry._p.name@default").is("Francia")),
            DCEntity.class, cityModel.getName());
      Assert.assertNotNull(lyonCityRes);
      Assert.assertEquals(1, lyonCityRes.size());
      Assert.assertEquals(lyonCity.getId(), lyonCityRes.get(0).getId());
      // HOWEVER can't query by en name and (embedded) en country since no en city value
      // (SAVE IF all other translations are auto set to the default one)
      // in this case, must do it in several queries i.e. user-driven browsing : first find country in es,
      // then its city using default

      
      // i18n ALT map :
      
      // indexes :
      // create one per query AND per language (and not a a single generic BLOB index because several languages are "never" queried together, see http://edgystuff.tumblr.com/post/47178201123/mongodb-indexing-tip-3-too-many-fields-to-index-use )
      countryColl.createIndex(new BasicDBObject("_p.nameI18n.es", 1)); // query country by name
      countryColl.createIndex(new BasicDBObject("_p.nameI18n.fr", 1)); // query country by name
      countryColl.createIndex(new BasicDBObject("_p.nameI18n.default", 1)); // "default" locale
      cityColl.createIndex(new BasicDBObject("_p.nameI18n.es", 1)
         .append("_p.inCountry._p.nameI18n.es", 1)); // query city by name and OPT (embedded) country name
      cityColl.createIndex(new BasicDBObject("_p.nameI18n.fr", 1)
         .append("_p.inCountry._p.nameI18n.fr", 1)); // query city by name and OPT (embedded) country name
      cityColl.createIndex(new BasicDBObject("_p.nameI18n.default", 1)
         .append("_p.inCountry._p.nameI18n.default", 1)); // "default" locale
      cityColl.createIndex(new BasicDBObject("_p.inCountry._p.nameI18n.es", 1)); // query by (embedded) country name
      cityColl.createIndex(new BasicDBObject("_p.inCountry._p.nameI18n.fr", 1)); // query by (embedded) country name
      cityColl.createIndex(new BasicDBObject("_p.inCountry._p.nameI18n.default", 1)); // "default" locale
      
      // samples
      HashMap<String, String> franceCountryNameI18nMap = new HashMap<String,String>();
      franceCountryNameI18nMap.put("es", "Francia");
      franceCountryNameI18nMap.put("default", "Francia"); // TODO auto add default : at first time AND / OR according to its model's default...
      franceCountryNameI18nMap.put("fr", "France");
      franceCountryNameI18nMap.put("en", "France");
      franceCountry.getProperties().put("nameI18n", franceCountryNameI18nMap);
      mgo.save(franceCountry, countryModel.getName());
      Assert.assertTrue(franceCountry.getProperties().get("nameI18n") instanceof Map<?, ?>);
      Assert.assertEquals(((Map<String,String>) franceCountry.getProperties().get("nameI18n")).get("es"), "Francia");
      
      updateResource(lyonCity, cityModel); // updating lyonCity withh latest embedded franceCountry changes
      HashMap<String, String> lyonCityNameI18nMap = new HashMap<String,String>();
      lyonCityNameI18nMap.put("es", "Lyon");
      lyonCityNameI18nMap.put("default", "Lyon"); // TODO auto add default : at first time AND / OR according to its model's default...
      lyonCityNameI18nMap.put("fr", "Lyon");
      // NO EN VALUE
      lyonCity.getProperties().put("nameI18n", lyonCityNameI18nMap);
      mgo.save(lyonCity, cityModel.getName());
      Assert.assertTrue(lyonCity.getProperties().get("nameI18n") instanceof Map<?, ?>);
      Assert.assertEquals(((Map<String,String>) lyonCity.getProperties().get("nameI18n")).get("es"), "Lyon");
      Assert.assertTrue(lyonCity.getProperties().get("inCountry") instanceof DCEntity);
      Assert.assertTrue(((DCEntity) lyonCity.getProperties().get("inCountry"))
            .getProperties().get("nameI18n") instanceof Map<?, ?>);
      Assert.assertEquals(((Map<String,String>) ((DCEntity) lyonCity.getProperties().get("inCountry"))
            .getProperties().get("nameI18n")).get("es"), "Francia");
      
      // queries
      // by es name :
      franceCountryRes = mgo.find(new Query(new Criteria("_p.name@es").is("Francia")),
            DCEntity.class, countryModel.getName());
      Assert.assertNotNull(franceCountryRes);
      Assert.assertEquals(1, franceCountryRes.size());
      Assert.assertEquals(franceCountry.getId(), franceCountryRes.get(0).getId());
      // by es name and (embedded) es country :
      lyonCityRes = mgo.find(new Query(new Criteria("_p.name@es").is("Lyon")
            .and("_p.inCountry._p.name@es").is("Francia")),
            DCEntity.class, cityModel.getName());
      Assert.assertNotNull(lyonCityRes);
      Assert.assertEquals(1, lyonCityRes.size());
      Assert.assertEquals(lyonCity.getId(), lyonCityRes.get(0).getId());
      // default behaviour - by default name and (embedded) default country :
      lyonCityRes = mgo.find(new Query(new Criteria("_p.name@default").is("Lyon")
            .and("_p.inCountry._p.name@default").is("Francia")),
            DCEntity.class, cityModel.getName());
      Assert.assertNotNull(lyonCityRes);
      Assert.assertEquals(1, lyonCityRes.size());
      Assert.assertEquals(lyonCity.getId(), lyonCityRes.get(0).getId());
      // HOWEVER can't query by en name and (embedded) en country since no en city value
      // (SAVE IF all other translations are auto set to the default one)
      // in this case, must do it in several queries i.e. user-driven browsing : first find country in es,
      // then its city using default

      
      // i18n ALT suffixed values list :
      
      // indexes :
      // create one per query AND per language (and not a a single generic BLOB index because several languages are "never" queried together, see http://edgystuff.tumblr.com/post/47178201123/mongodb-indexing-tip-3-too-many-fields-to-index-use )
      countryColl.createIndex(new BasicDBObject("_p.nameI18n.es", 1)); // query country by name
      countryColl.createIndex(new BasicDBObject("_p.nameI18n.fr", 1)); // query country by name
      countryColl.createIndex(new BasicDBObject("_p.nameI18n.default", 1)); // "default" locale
      cityColl.createIndex(new BasicDBObject("_p.nameI18n.es", 1)
         .append("_p.inCountry._p.nameI18n.es", 1)); // query city by name and OPT (embedded) country name
      cityColl.createIndex(new BasicDBObject("_p.nameI18n.fr", 1)
         .append("_p.inCountry._p.nameI18n.fr", 1)); // query city by name and OPT (embedded) country name
      cityColl.createIndex(new BasicDBObject("_p.nameI18n.default", 1)
         .append("_p.inCountry._p.nameI18n.default", 1)); // "default" locale
      cityColl.createIndex(new BasicDBObject("_p.inCountry._p.nameI18n.es", 1)); // query by (embedded) country name
      cityColl.createIndex(new BasicDBObject("_p.inCountry._p.nameI18n.fr", 1)); // query by (embedded) country name
      cityColl.createIndex(new BasicDBObject("_p.inCountry._p.nameI18n.default", 1)); // "default" locale
      
      // samples
      ArrayList<String> franceCountryNameI18nList = new ArrayList<String>();
      franceCountryNameI18nList.add("Francia@es");
      franceCountryNameI18nList.add("France@fr");
      franceCountryNameI18nList.add("France@en");
      franceCountry.getProperties().put("name@i18nl", franceCountryNameI18nList);
      mgo.save(franceCountry, countryModel.getName());
      Assert.assertTrue(franceCountry.getProperties().get("name@i18nl") instanceof List<?>);
      Assert.assertEquals(((List<String>) franceCountry.getProperties().get("name@i18nl")).get(0), "Francia@es");
      
      updateResource(lyonCity, cityModel); // updating lyonCity withh latest embedded franceCountry changes
      ArrayList<String> lyonCityNameI18nList = new ArrayList<String>();
      lyonCityNameI18nList.add("Lyon@es");
      lyonCityNameI18nList.add("Lyon@fr");
      // NO EN VALUE
      lyonCity.getProperties().put("name@i18nl", lyonCityNameI18nList);
      mgo.save(lyonCity, cityModel.getName());
      Assert.assertTrue(lyonCity.getProperties().get("name@i18nl") instanceof List<?>);
      Assert.assertEquals(((List<String>) lyonCity.getProperties().get("name@i18nl")).get(0), "Lyon@es");
      Assert.assertTrue(lyonCity.getProperties().get("inCountry") instanceof DCEntity);
      Assert.assertTrue(((DCEntity) lyonCity.getProperties().get("inCountry"))
            .getProperties().get("name@i18nl") instanceof List<?>);
      Assert.assertEquals(((List<String>) ((DCEntity) lyonCity.getProperties().get("inCountry"))
            .getProperties().get("name@i18nl")).get(0), "Francia@es");
      
      // queries
      // by es name :
      franceCountryRes = mgo.find(new Query(new Criteria("_p.name@i18nl").is("Francia@es")),
            DCEntity.class, countryModel.getName());
      Assert.assertNotNull(franceCountryRes);
      Assert.assertEquals(1, franceCountryRes.size());
      Assert.assertEquals(franceCountry.getId(), franceCountryRes.get(0).getId());
      // by es name and (embedded) es country :
      lyonCityRes = mgo.find(new Query(new Criteria("_p.name@i18nl").is("Lyon@es")
            .and("_p.inCountry._p.name@i18nl").is("Francia@es")),
            DCEntity.class, cityModel.getName());
      Assert.assertNotNull(lyonCityRes);
      Assert.assertEquals(1, lyonCityRes.size());
      Assert.assertEquals(lyonCity.getId(), lyonCityRes.get(0).getId());
      // default behaviour - by (any) name prefix and (embedded) (any) country prefix :
      lyonCityRes = mgo.find(new Query(new Criteria("_p.name@i18nl").regex("^Lyon.*")
            .and("_p.inCountry._p.name@i18nl").regex("^Francia.*")),
            DCEntity.class, cityModel.getName());
      Assert.assertNotNull(lyonCityRes);
      Assert.assertEquals(1, lyonCityRes.size());
      Assert.assertEquals(lyonCity.getId(), lyonCityRes.get(0).getId());
      // NB. this allows to query on (all fields including) available en fields even if some are not there
      // which may be useful in a highly variously / sparsely localized environment like OASIS lay be
      // HOWEVER this doesn't allow to lookup in a given language and use default value if not available
      
      // so the only way to lookup in a given language and use default value if not available is
      // to set ALL translations to default value, but this is place-consuming !!
   }
   
   
   public void testDataQualityAndMergedInheritedProperties() {
      
      Assert.assertTrue(true);
      
      // test it manually in mongodc CLI :
      /*

db.testproplist.remove({})
db.testproplist.insert({ _mdln:"insee.ville", proplist:[{ rdftype:"city", name:"Lyon", inCountry:{ _mdln:"country", proplist:[{ name:"France", "_q":5 }] }, "_q":8 }, { rdftype:"insee.ville", inseeCode:"INSEE.Lyon", "_q":3 }] })
db.testproplist.ensureIndex({ "proplist.inCountry.proplist.name":1, "proplist.name":1, "proplist._q":1 })
db.testproplist.find({ "proplist.inCountry.proplist.name":"France", "proplist._q":{ "$gt" : 3} })

.explain() => OK

> db.testproplist.remove({})
> db.testproplist.insert({ _mdln:"insee.ville", proplist:[{ _src:"city", rdftype:"city", name:"Lyon", inCountry:{ _mdln:"country", proplist:[{ name:"France", "_q":5 }] }, "_q":8 }, { _src:"insee.ville", rdftype:"insee.ville", inseeCode:"INSEE.Lyon", "_q":3 }] })
> db.testproplist.ensureIndex({ "proplist.inCountry.proplist.name":1, "proplist.name":1, "proplist._q":1, "proplist._src":1 })
> db.testproplist.find({ "proplist.inCountry.proplist.name":"France", "proplist._q":{ "$gt" : 7}, "proplist._src":"insee.ville" })

=> WARNING returns a false positive that disappears at flattening

       */
   }

   
   
   public void testListeDesMairesRightsAndUserConfidentiality() {
      
      Assert.assertTrue(true);
      
      // test it manually in mongodc CLI :
      /*

db.maire.remove()
db.maire.insert({ _mdln:"maire", proplist:[{ _w:"mairie_Lyon", electedIn:2010, ofCity:{ _mdln:"city", proplist:[{ name:"Lyon", _q:6 }] }, isUser:{ _mdln:"user", proplist:[{ name:"Gerard Collomb", "_q":5 }, { _r:"user_confidential_GerardCollomb", mastercard:"8111812356899876" }] }}] })
db.maire.find({ "proplist.ofCity.proplist.name":"Lyon", "proplist._w":{ $in:[ "mairie_Lyon", "user_confidential_JeanPloye" ] } })

=> returns "maire" document that Jean Ploye rightly has write rights on
HOWEVER also returns copied confidential info ! so :
* TODO don't copy confidential info (not useful to more that a single "person group", TODO Q others ??)
* TODO remove at flattening time in service other copied info that the user has no right on (write or read depending on the request) 

db.maire.find({ "proplist.ofCity.proplist.name":"Lyon", "proplist.isUser.proplist._r":{ $in:[ "user_confidential_GerardCollomb" ] } })

=> returns "maire" document that Gerard Collomb rightly has read (and write) rights on
NB. this requires to put criteria on all level of copied resources, but not to add indexes on them
(because rights criteria should not improve query perfs) 
 
       */
   }
   
   
   
   /**
    * TODO check extended & copied types as in updateResource(...).
    * Doesn't save in db.
    * @param dcResourceModel TODO not null
    * @param extendedResourceDCEntity not null, else use String param'd alternate method
    * @param copiedSubresourceDCEntityMap
    * @return
    * @throws Exception 
    */
   public DCEntity newResource(DCResourceModel dcResourceModel, DCEntity extendedResourceDCEntity,
         Map<String,DCEntity> copiedSubresourceDCEntityMap) throws Exception {
      ExtendedResourceDCModel extendedResourceModel = dcResourceModel.getExtendedResourceModel();
      DCEntity extendedResourceDCEntityFound = mgo.findOne(new Query(new Criteria("uri").is(extendedResourceDCEntity.getUri())),
            DCEntity.class, extendedResourceModel.getTargetModel().getName());
      // TODO better checks : not twice, everywhere, locks...
      if (extendedResourceDCEntityFound == null) {
         throw new Exception("New DCEntity is missing extended resource of type "
               + extendedResourceModel.getTargetModel().getName() + " and uri "
               + extendedResourceDCEntity.getUri());
      } else if (!extendedResourceDCEntityFound.getVersion().equals(extendedResourceDCEntity.getVersion())) {
         throw new Exception("New DCEntity has extended resource with inconsistent version (of type "
               + extendedResourceModel.getTargetModel().getName() + " and uri "
               + extendedResourceDCEntity.getUri() + ")");
      }
      
      DCEntity dcEntity = new DCEntity(extendedResourceDCEntity);
      dcEntity.setId(null); // to allow generation of new id
      dcEntity.setVersion(null); // to init of version to 0
      dcEntity.setModelName(dcResourceModel.getName()); // to override extended one
      // NB. dcEntity is the same as extendedResourceDCEntity.uri
      // TODO extendedResourceDCEntity.id (more like DBRef actually) (and same for _v, _t) could be put
      // in dcEntity HOWEVER would have to be renamed at each inheritance stage
      
      ////dcEntity.setProperties(new HashMap<String,Object>()); // TODO does doing it by default it take (too much) place in mongodb ???
      if (copiedSubresourceDCEntityMap != null) {
         // TODO checks, locks...
         for (String key : copiedSubresourceDCEntityMap.keySet()) {
            dcEntity.getProperties().put(key, new DCEntity(copiedSubresourceDCEntityMap.get(key)));
         }
      }
      //////////////////updateResource(dcEntity, dcResourceModel);
      return dcEntity;
   }
   /** To use (only) if no extended resource model.
    * Doesn't save in db.
    * @throws Exception */
   public DCEntity newResource(DCResourceModel dcResourceModel, String uri,
         Map<String,DCEntity> copiedSubresourceDCEntityMap) throws Exception {
      DCEntity dcEntity = new DCEntity();
      if (uri == null || uri.length() == 0) {
         throw new Exception("URI should never be null");
      }
      dcEntity.setUri(uri);
      dcEntity.setModelName(dcResourceModel.getName());
      // NB. dcEntity is the same as extendedResourceDCEntity.uri
      // TODO extendedResourceDCEntity.id (more like DBRef actually) (and same for _v, _t) could be put
      // in dcEntity HOWEVER would have to be renamed at each inheritance stage
      
      ////dcEntity.setProperties(new HashMap<String,Object>()); // TODO does doing it by default it take (too much) place in mongodb ???
      dcEntity.setProperties(new HashMap<String,Object>());
      if (copiedSubresourceDCEntityMap != null) {
         // TODO checks, locks...
         for (String key : copiedSubresourceDCEntityMap.keySet()) {
            dcEntity.getProperties().put(key, new DCEntity(copiedSubresourceDCEntityMap.get(key)));
         }
      }
      //////////////////updateResource(dcEntity, dcResourceModel);
      return dcEntity;
   }
   /** SHOULD NOT BE USED updates are rather down top-down per extended / copied model change.
    * Doesn't save in db. */
   private void updateResource(DCEntity dcEntity) throws Exception {
      //DCResourceModel dcResourceModel = dcMetamodel.getModel(dcEntity.getBaseType()); // TODO
      DCResourceModel dcResourceModel = null;
      updateResource(dcEntity, dcResourceModel);
   }
   /** SHOULD NOT BE USED updates are rather down top-down per extended / copied model change.
    * Doesn't save in db. */
   private void updateResource(DCEntity dcEntity, DCResourceModel dcResourceModel) throws Exception {
      ExtendedResourceDCModel extendedResourceModel = dcResourceModel.getExtendedResourceModel();
      if (extendedResourceModel != null) {
         DCEntity extendedResourceDCEntity = mgo.findOne(new Query(new Criteria("uri").is(dcEntity.getUri())),
               DCEntity.class, extendedResourceModel.getTargetModel().getName());
         if (extendedResourceDCEntity == null) {
            throw new Exception("DCEntity " + dcEntity.getUri() + " is missing extended resource (of type "
                  + extendedResourceModel.getTargetModel().getName() + ")");
         }
         
         // copy (NB. overriding any possible illegal change in them) :
         copy(extendedResourceDCEntity, dcEntity);
         
         // adding and changing other (additional) attributes are allowed though
         // TODO rather / or definable in models ?
         // TODO check for removed attributes ?!? and put warning for added / newly overriding attributes ??!!??
         
         // NB. recursion has already been done on extendedResourceDCEntity itself (according to
         // extendedResourceModel's own extendedResources and copiedSubresources)
      }
      
      if (dcResourceModel.getCopiedSubresourceModels() != null) {
         for (CopiedSubresourceDCModel copiedSubresourceModel : dcResourceModel.getCopiedSubresourceModels()) {
            String copiedSubresourceAttr = copiedSubresourceModel.getAttribute();

            Object foundValue = dcEntity.getProperties().get(copiedSubresourceAttr);
            if (!(foundValue instanceof DCEntity)) {
               if (foundValue == null) {
                  throw new Exception("DCEntity model " + dcResourceModel.getName()
                        + " should have for attribute "  + copiedSubresourceAttr
                        + " a copied subresource (of type " + copiedSubresourceModel.getTargetModel().getName()
                        + ") but has none");
               } else {
                  throw new Exception("DCEntity model " + dcResourceModel.getName()
                        + " should have for attribute "  + copiedSubresourceAttr
                        + " a copied subresource (of type " + copiedSubresourceModel.getTargetModel().getName()
                        + ") but instead has " + foundValue);
               }
            }
            DCEntity copiedSubresourceDCEntityCopy = (DCEntity) foundValue;
            if (!isOfModel(copiedSubresourceDCEntityCopy, copiedSubresourceModel.getTargetModel())) { // TODO in param or DCEntity ??
               throw new Exception("Copied resource of DCEntity model " + dcResourceModel.getName()
                     + " should be of type " + copiedSubresourceModel.getTargetModel().getName() + " but is "
                     + copiedSubresourceDCEntityCopy);
            }
            
            DCEntity copiedSubresourceDCEntity = mgo.findOne(new Query(new Criteria("uri").is(copiedSubresourceDCEntityCopy.getUri())),
                  DCEntity.class, copiedSubresourceModel.getTargetModel().getName());
            if (copiedSubresourceDCEntity == null) {
               throw new Exception("Can't find DCEntity of type " + copiedSubresourceModel.getTargetModel().getName()
                     + " with uri " + copiedSubresourceDCEntityCopy.getUri()
                     + " (as original of copied subresource for attribute "  + copiedSubresourceAttr
                     + " in DCEntity model " + dcResourceModel.getName() + ")");
            }

            // copy (NB. overriding any possible illegal change in them) :
            copy(copiedSubresourceDCEntity, copiedSubresourceDCEntityCopy);

            // DCEntity base fields copy (besides uri) (to prevent illegal changes) :
            // TODO or rather clone copy, then apply other attributes ???
            copiedSubresourceDCEntityCopy.setId(copiedSubresourceDCEntity.getId());
            copiedSubresourceDCEntityCopy.setVersion(copiedSubresourceDCEntity.getVersion());
            copiedSubresourceDCEntityCopy.setUri(copiedSubresourceDCEntity.getUri());
            copiedSubresourceDCEntityCopy.setCreated(copiedSubresourceDCEntity.getCreated());
            copiedSubresourceDCEntityCopy.setLastModified(copiedSubresourceDCEntity.getLastModified());
            copiedSubresourceDCEntityCopy.setCreatedBy(copiedSubresourceDCEntity.getCreatedBy());
            copiedSubresourceDCEntityCopy.setLastModifiedBy(copiedSubresourceDCEntity.getLastModifiedBy());
            copiedSubresourceDCEntityCopy.setModelName(copiedSubresourceDCEntity.getModelName());
            
            // adding and changing other (additional) attributes are allowed though
            // TODO rather / or definable in models ?
            // TODO check for removed attributes ?!? and put warning for added / newly overriding attributes ??!!??
            
            // NB. recursion has already been done on extendedResourceDCEntity itself (according to
            // copiedSubresourceModel's own extendedResources and copiedSubresources)
         }
      }
      // TODO TODO partial copy : label fr only
   }

   /**
    * Copy (NB. overriding any possible illegal change in them)
    * @param fromDCEntity
    * @param dcEntity
    */
   private void copy(DCEntity fromDCEntity, DCEntity dcEntity) {
      // uri : WARNING also copy _uri because it's its own uri !! also copy _id (and _v, _t ???) NOT if at root save if renamed
      dcEntity.setUri(fromDCEntity.getUri()); // same uri...
      
      // attrs :
      Set<String> copiedAttrs = fromDCEntity.getProperties().keySet(); // TODO rather / or definable in insee.ville and / or city models
      for (String extendedResourceAttr : copiedAttrs) {
         // TODO also check & throw error if any illegal change in it ?
         dcEntity.getProperties().put(extendedResourceAttr,
               fromDCEntity.getProperties().get(extendedResourceAttr)); // TODO set
      }
      
      // adding and changing other (additional) attributes are allowed though
      // TODO rather / or definable in models ?
      // TODO check for removed attributes ?!? and put warning for added / newly overriding attributes ??!!??
   }

   private boolean isOfModel(DCEntity extendedResourceDCEntity, DCResourceModel dcResourceModel) {
      //  
      String extendedDCEntityBaseType = extendedResourceDCEntity.getModelName();
      ///String extendedDCEntityBaseType = "city"; // TODO in param or lyonCity DCEntity ?? TODO rather rdf_type:{x,y} list ???
      return dcResourceModel.getName().equals(extendedDCEntityBaseType);
   }


   @Test
   public void testI18nMetamodel() throws Exception {

   }
   
}
