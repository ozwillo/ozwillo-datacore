package org.oasis.datacore.data.meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkMethod;
import org.oasis.datacore.data.DCEntity;
import org.oasis.datacore.data.meta.CopiedSubresourceDCModel;
import org.oasis.datacore.data.meta.DCResourceModel;
import org.oasis.datacore.data.meta.ExtendedResourceDCModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

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
      
      // init datacore collections
      DBCollection countryColl = mgo.createCollection(countryModel.getName());
      DBCollection cityColl = mgo.createCollection(cityModel.getName());
      cityColl.createIndex(new BasicDBObject("name", 1)); // TODO in metamodel / according to queries
      ///cityColl.createIndex(new BasicDBObject("precincts.name", 1)); // TODO embedded subresource sample
      ///cityColl.createIndex(new BasicDBObject("inCountry.name", 1)); // TODO external subresource sample
      DBCollection bureauDeVoteColl = mgo.createCollection(bureauDeVoteModel.getName());
      ///bureauDeVoteColl.createIndex(new BasicDBObject("code_canton", 1)); // TODO in metamodel / according to queries
      DBCollection inseeVilleColl = mgo.createCollection(inseeVilleModel.getName());
      inseeVilleColl.createIndex(new BasicDBObject("name", 1)); // TODO auto following extended metamodel ?!?
      ///inseeVilleColl.createIndex(new BasicDBObject("inseeCode", 1)); // TODO in metamodel / according to queries
      
      // create resource data instances :
      
      // create country in datacore
      String franceCountryUri = DCResourceModel.DATACORE_BASE_URI + countryModel.getName()
            + "/France"; // uri (from id query params !?) TODO type as field, not prefix
      DCEntity franceCountry = newResource(countryModel, franceCountryUri, null); // TODO also uri
      franceCountry.getProperties().put("name", "France");
      mgo.save(franceCountry, countryModel.getName());
      
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

      // getting lyonCity as REST (in fr TODO more) :
      // { _uri:"...", _v:0, (type:"city",) name:"Lyon" } TODO Q also type / collection ? (yes easier for JAXRS etc.) ; TODO Q also _id ??
      // and as simili RDF : also type:"igeo:Commune", possible name mapped to rdf:label
      
      
      // create bureau in datacore
      Map<String, DCEntity> bureauDeVoteSubresources = new HashMap<String, DCEntity>(1);
      lyonCityCopiedSubresources.put("bureauCollectivite", lyonCity); // TODO shorter names (auto ??)
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
      List<DCEntity> lyonInseeVilleRes = mgo.find(new Query(new Criteria("properties.inseeCode").is("INSEE.Lyon")),
            DCEntity.class, inseeVilleModel.getName());
      Assert.assertNotNull(lyonInseeVilleRes);
      Assert.assertEquals(1, lyonInseeVilleRes.size());
      Assert.assertEquals(lyonInseeVille.getId(), lyonInseeVilleRes.get(0).getId());
      // by (extended) name :
      lyonInseeVilleRes = mgo.find(new Query(new Criteria("properties.name").is("Lyon")),
            DCEntity.class, inseeVilleModel.getName());
      Assert.assertNotNull(lyonInseeVilleRes);
      Assert.assertEquals(1, lyonInseeVilleRes.size());
      Assert.assertEquals(lyonInseeVille.getId(), lyonInseeVilleRes.get(0).getId());
      // by (embedded) country :
      List<DCEntity> franceInseeVilleRes = mgo.find(new Query(new Criteria("properties.inCountry.properties.name").is("France")),
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
      // by canton code :
      List<DCEntity> bureauDeVoteRes = mgo.find(new Query(new Criteria("properties.code").is("Lyon325")),
            DCEntity.class, bureauDeVoteModel.getName());
      Assert.assertNotNull(bureauDeVoteRes);
      Assert.assertEquals(1, bureauDeVoteRes.size());
      Assert.assertEquals(bureauDeVote.getId(), bureauDeVoteRes.get(0).getId());
      // by (extended) name :
      bureauDeVoteRes = mgo.find(new Query(new Criteria("properties.bureauCollectivite.properties.name").is("Lyon")),
            DCEntity.class, bureauDeVoteModel.getName());
      Assert.assertNotNull(bureauDeVoteRes);
      Assert.assertEquals(1, bureauDeVoteRes.size());
      Assert.assertEquals(bureauDeVote.getId(), bureauDeVoteRes.get(0).getId());
      // by (embedded) country :
      bureauDeVoteRes = mgo.find(new Query(new Criteria("properties.bureauCollectivite.properties.inCountry.properties.name").is("France")),
            DCEntity.class, bureauDeVoteModel.getName());
      Assert.assertNotNull(bureauDeVoteRes);
      Assert.assertEquals(1, bureauDeVoteRes.size());
      Assert.assertEquals(bureauDeVote.getId(), bureauDeVoteRes.get(0).getId());
      
      // NB. there must be no cycles !
      // reciprocal copy of different fields (could be done but) is better replaced by a commonly managed / updated reference collection
   }

   /**
    * TODO check extended & copied types as in updateResource(...)
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
   /** To use (only) if no extended resource model 
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
   /** SHOULD NOT BE USED updates are rather down top-down per extended / copied model change */
   private void updateResource(DCEntity dcEntity) throws Exception {
      //DCResourceModel dcResourceModel = dcMetamodel.getModel(dcEntity.getBaseType()); // TODO
      DCResourceModel dcResourceModel = null;
      updateResource(dcEntity, dcResourceModel);
   }
   /** SHOULD NOT BE USED updates are rather down top-down per extended / copied model change */
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
   
}
