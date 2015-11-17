package org.oasis.datacore.sample.crm;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.sample.city.City;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-crm-test-context.xml" })
public class SharedDataTest {
	
	@Autowired
	private MongoOperations mgo;
	
	private static DB datacore;
	private static MongoClient datacoreMongoClient;
	
	@BeforeClass
	public static void setup() throws UnknownHostException {
		datacoreMongoClient = new MongoClient("localhost" , 27017);
		datacore = datacoreMongoClient.getDB("datacore");
      // if in secure mode, authenticate :
      //boolean auth = datacore.authenticate(myUserName, myPassword);
	}
	
	@AfterClass
	public static void teardown() throws UnknownHostException {
		datacoreMongoClient.close();
	}

	@Test
	public void testOrganizationCity() {
		// cleanup :
		DBCollection citiesColl = datacore.getCollection("cities"); // or france_cities ?????????
		WriteResult wr = citiesColl.remove(new BasicDBObject());
		if (wr.getError() != null) {
			System.err.println(wr.getError());
			System.err.println(wr.getLastError());
			Assert.fail("Error in last write");
		}
		Assert.assertTrue(!citiesColl.find().hasNext());
		DBCollection orgsColl = datacore.getCollection("organizations");
		orgsColl.remove(new BasicDBObject());
		Assert.assertTrue(!orgsColl.find().hasNext());

		
		// City app - create city
		City lyonCity = new City();
		lyonCity.setName("Lyon");
		lyonCity.setCountry("France");
		lyonCity.setPopulation(50000);
		// TODO precincts...
		mgo.save(lyonCity);
		Assert.assertNotNull(lyonCity.getId());
		
		// City app saves in Datacore (LATER using its own metamodel)
		// NB. datacore base info : id (tech), source, source id (?), uri (from id query params ?)
		// NB. datacore governance info : data quality level, cross versioning / approval, OPT other qualification justif (approved by source / user / governance workflow X...)
		// NB. datacore security info : owner (or can source or metamodel be used for that ?), policy, precomputed permissions...
		int cityAppMaxQuality = 7;
      String lyonCityUri = "http://data.ozwillo.com/city/France/Lyon"; // uri (from id query params !?)
		DBObject cityApp_LyonCity = new BasicDBObject(
				//"_id_source_city", lyonCity.getId()). // NOOOO source id, where "city" is the app id IN A DIFFERENT COLLECTION
				"_id_source", lyonCity.getId()). // source id (?) TODO index
                append("_source", "FranceCityApp"). // source app TODO index
                // TODO container : put it within uri (server or root path) ?!? (else uri would be container independent,
                // and would need a data locator to know its container server LATER WHY NOT AS AN ADDITIONAL REPRESENTATION OF IT)
                append("_uri", lyonCityUri). // uri (TODO Q from id query params !? with type, hierarchical when contained ?!)
                // TODO TODO owner : source ? IF "_rights_policy" = "source_is_owner" ? or dedicated "_rights"  & "_owner" ??
                append("_quality", cityAppMaxQuality).
                append("_cross_version", new Long(0)). ///
                append("_cross_approved_version", new Long(-1)). // same better than none or -1 for queries NOOOO ///
                //append("_cross_approved_id", "").
                // TODO either tx, or rather _base_version NO pb of changes that conflict or made on obsolete version
                // BUT if change c2 comes after unapproved / unapprovable change c1 it'll have a higher cross version ?! WHICH IS NOT A PROBLEM
                append("name", lyonCity.getName()).
                append("country", lyonCity.getCountry());
		// source : in metamodel, TODO maybe also in data for querying ?? or as db prefix ???
		citiesColl.insert(cityApp_LyonCity);
		Assert.assertNotNull(cityApp_LyonCity.get("_id"));
		
		
		// TODO RDF map fields to avoid ':' in mongodb (unpractical in js & cli client) ??
		
		// TODO RDF what for id & relationship values & names ?
		// subjectURI (rdf:about) - predicate - objectURI (rdf:resource), see http://stackoverflow.com/questions/7118326/differences-between-rdfresource-rdfabout-and-rdfid
		// TODO & how to do these mongodb queries WITH id version indexes etc. ?
		// TODO & allow denormalization / field copy to allow more in a single mongodb query ?? (yes, notably ref list counts http://fr.slideshare.net/kbanker/mongodb-schema-design )
		
		// TODO provide generic base for all schema with Dublin Core (notably dc:identifier)
		// TODO use rdfs (including rdf:type) to link to metamodel (without putting it all in datacore itself) http://www.w3.org/TR/rdf-schema/
		
		// TODO TODO define metamodel as rdfs (Class et Property rdfs:about it) (or/plus OWL...) ???? at least the results should be the same, D1.2 says yes, but not more (OWL) ; ex. http://www.w3.org/TR/rdf-schema/#ch_appendix_rdfs
		
		// TODO business provided id can't be used as _id (because if also mongodb or from many sources) so how ?
		
		// TODO how to let app B add domain-specific data to node whose schema was defined by A ? embed or ref coll ? (it depends, study cases)
		// => can't be on the same collection document, else can't have conflicting field values
		// NB. even approval can be seen as a domain-specific data contributed from a different source... => store / cache such (aggregared) info on data in "external (to source)" marked fields, which is exactly data quality level's point
		
		
		// data quality :
		// always relative to the data (qualification!) source
		// so justif of level should be kept (audited ?) elsewhere (in data collections of governance soft...)
		// what happens if another version from another source gets a higher quality than this source's ?? auto replaces it in queries ? overrides it ??? after alert & governance by this source's ?
		
		// TODO allow conflicting data ((OPT in a given context ex. source)) ? YES
		// if no there can be a single reference collection ((in this context)), else not i.e. there must be several (filled by aggregation ex. map/reduce)
		// (to avoid conflicts, narrow the context ex. source)
		// are conflicting data the exception or the rule ?
		// * if rule, maintain one different coll with all data per master source (or eve "else") ;
		// * => => else handle conflicts on client side after returning uri-ordered alternate versions YES
		//     - TODO in SDK and for those who can't still provide an simple single master cached / computed according to gov a policy
		//     - can be done by index with fields in order : exact, sort, range see http://blog.mongolab.com/2012/06/cardinal-ins/
		// is (global) external approval the exception or the rule ?
		// TODO allow all sources to be master ? then how can they still benefit from external data improvements ??

		// Competing sources for the same data :
		// a. either unapproved (according to governance policy : by master WHOSE JOB ? MUST BE REACTIVE or its delegate, or all UNPRACTICAL IF MANY, or majority BOF) changes are in an additional collection (OR MORE if want to reuse _id)
		// (pro : better when everything is well approved ; con : single point of failure governance, more queries when not approved or wanting a specific provider as reference)
		// b. or several versions, one per provider, always coexist ; and cross version & approval manage them
		// (pro : more flexible, handles better unperfect cases ; cons : slightly less perf in perfect case)
		// c. or both (collections copy are easy to do in mongodb) : one master governed collection with single objects, and one collection containing cross-versioned & approved objects
		// (client can choose mode a. or b. with full pros & cons)
		
		// so the solution is :
		// collection(s) dedicated to each "approved / qualified enough (by who)" data query policy (at most per app use TOO MANY, at least one ("master")),
		// and collection(s) storing all contributed / competing / conflicting data with their qualifications
		// which fill the first ones according to the auto & manual governance policies
		
		// Case of a competing city app : 
		int europeCitiesServiceMaxQuality = 5;
		BasicDBObject europeCitiesService_LyonCity = new BasicDBObject(
				// source : in metamodel, TODO maybe also in data for querying ?? or as db prefix ???
				//"_id_source_city", lyonCity.getId()). // NOOOO source id, where "city" is the app id
				"_id_source", "fr_Lyon"). // source id TODO index
                append("_source", "EuropeCitiesService"). // source app TODO index
                append("_uri", lyonCityUri). // uri (from id query params !?)
                append("_quality", europeCitiesServiceMaxQuality).
                append("name", "Lyon").
                append("country", "France").
                append("precinctsCount", 9); // another data field / domain TODO study better / more : how to allow provider B to change its own-provided approved field without saying it's inconsistent (only an enrichment) ?? BY COMPARING TO ITS OWN LAST APPROVED VERSION BUT IT HAS TO BE THERE...
		
		
		// TODO how to reconcile ????? i.e. GOUVERNANCE :
		
		// a metamodel should define "id query(ies ??)" (each source should contribute one / some ??) (or unique uri format ?)
		// which return a single result for each source
		// if an id query has several results across resources, its results are the same thing and must be reconciled
		
		// TODO allow a single result when reconciled by B and trigger alert to A ? (then how to know where comes each / additional field from ? or approve some fields and not some others / another domain ??)
		// or always one per source with fields tracking reconciliation ?? (_previous_version_id and _previous_version as cross-object version numbering field ; NB. that's not the optimistic locking version but anyway it is also updated manually)
		// or a designated data master service (or only for final approval) ? (but what if he doesn't handle its responsibilities in time enough)
		// TODO for alert ex. keep store list of previous contributors in object list field ?!?
		
		// a. when service B. contributes an object for the first time, it must look for possible previous contributions using id query
		// and find the highest _cross_version one (and _cross_approved_version ?? you have to sync before changing it...)
		// (i.e. DEDUPLICATION)
		DBCursor lyonCitiesIdCurs = citiesColl.find(new BasicDBObject("name", "Lyon")
			.append("country", "France"));
		DBObject highestCrossVersionCity = null;
		long highestCrossVersion = -1;
		for (; lyonCitiesIdCurs.hasNext();) {
			DBObject curCity = lyonCitiesIdCurs.next();
			Long curCrossVersion = (Long) curCity.get("_cross_version");
			if (highestCrossVersion < curCrossVersion) {
				highestCrossVersion = curCrossVersion;
				highestCrossVersionCity = curCity;
			}
		}
		Assert.assertNotNull(highestCrossVersionCity);
		
		// OPT then it checks data consistency, and if not
		// * either doesn't write for now but goes in auto or manual "becoming consistent" mode (local alert / workflow...)
		// * or writes its own city OPT then / which triggers an alert to conflicting contributor (& others ?)
		// and if yes, OPT says it on its own ("_cross_approved_version/id"=...) OPT and on other cities NO
		// then it creates / changes its own city with incremented _cross_version (& 2PC tx on it)
		for (String key : europeCitiesService_LyonCity.keySet()) {
			if (key.charAt(0) != '_' // not a technical field
					&& highestCrossVersionCity.containsField(key)) {
				Object highestCrossVersionCityValue = highestCrossVersionCity.get(key);
				Object value = europeCitiesService_LyonCity.get(key);
				if (value == null && highestCrossVersionCityValue != null
						|| value != null && !value.equals(highestCrossVersionCityValue)) {
					Assert.fail("not consistent");
				}
				// TODO OPT also allow when highestCrossVersionCity value is the same as previous europeCitiesService_LyonCity value ?? which requires it to be also there...
			}
		}

		europeCitiesService_LyonCity
        	.append("_cross_version", new Long(highestCrossVersion + 1))
        	.append("_cross_approved_version", new Long(-1)); // same better than none or -1 for queries NOOOO
			//.append("_cross_approved_id", "").
		citiesColl.insert(europeCitiesService_LyonCity);
		Assert.assertNotNull(europeCitiesService_LyonCity.get("_id"));
		
		// now there is a new change but which has not yet been approved by others
		// there are several alternatives of approval policies :
		// * OPT auto approve if consistent
		// * top down enforcement of approval on all sources by manager ???? NO manager has only the right to say that its own is the right one NOT EVEN being the "manager"/master is enough
		// * manual approval (sets _cross_version_approved/id ; which is the only field that can change without a new _cross_version ??)

		// CityApp provider handles it :
		// TODO TODO better (should be done for each city, hard to do this way)
		long _cross_approved_version = Math.max((Long) cityApp_LyonCity.get("_cross_version"), (Long) cityApp_LyonCity.get("_cross_approved_version"));
		DBCursor unapprovedHigherCrossVersionCities = citiesColl.find(new BasicDBObject("_cross_version",
				new BasicDBObject("$gt", _cross_approved_version))
				.append("name", "Lyon")
				.append("country", "France"));
		Assert.assertEquals(unapprovedHigherCrossVersionCities.count(), 1);
		Assert.assertEquals(unapprovedHigherCrossVersionCities.next().get("_id"), europeCitiesService_LyonCity.get("_id"));
		
		// approve latest (should be done according to governance policies & data quality...)
		///cityApp_LyonCity
        ///	.append("_cross_approved_version", europeCitiesService_LyonCity.get("_cross_version"));
		///citiesColl.insert(europeCitiesService_LyonCity);
		citiesColl.update(new BasicDBObject("_id", cityApp_LyonCity.get("_id")), new BasicDBObject("$set",
				new BasicDBObject("_cross_approved_version", europeCitiesService_LyonCity.get("_cross_version"))
				.append("_cross_approved_id", europeCitiesService_LyonCity.get("_id")))); // on the fly set
		cityApp_LyonCity = citiesColl.findOne(new BasicDBObject("_id", cityApp_LyonCity.get("_id")));
		Assert.assertEquals(cityApp_LyonCity.get("_cross_approved_version"), europeCitiesService_LyonCity.get("_cross_version"));
		Assert.assertEquals(cityApp_LyonCity.get("_cross_approved_id"), europeCitiesService_LyonCity.get("_id"));
		
		
		// b. when it contributes a change, it must look for the higher approved / versioned object

		
		

		// CRM - create organization (sets id)
		Organization owCompany = new Organization();
		owCompany.setShortName("Open Wide");
		owCompany.setLegalName("Open Wide");
		owCompany.setKind("SAS");
		owCompany.setCountry("France");
		// regular use : directly set city from its UI among a hardcoded list
		///owCompany.setCity("Lyon");
		mgo.save(owCompany);
		Assert.assertNotNull(owCompany.getId());
		
		
		// TODO how does using datacore instead of local data impact app ?
		// for reference data : TODO ; and often also contribution / sync at init
		// for contributed data, with possible improvements in quality
		// (plus in both cases, could keep a backup copy locally, but that's a mere cache pb)
		// in the case of new apps using the datacore from the start, they should use the uri as id and can use its generic (client) model directly
		
		// TODO allow to store an app's technical id in datacore ??????
		// easier for clients to adapt ; but then there should be an index on it, so heavier on writes
		// would require a schema / aspect reserved (private ??) to the app AND IF MANY HEAVY
		
		// TODO i.e. how can an app build a composite local (ex. hibernate) + remotely shared model ??
		// => by letting the DAO (or service) layer inject same local model and using same refid as before, but from remote data (having generic model containing all props) using builder pattern
		
		// TODO how can an app paginate remotely shared data ? VERY USEFUL
		// in mongodb see http://stackoverflow.com/questions/5525304/how-to-do-pagination-using-range-queries-in-mongodb
		// using .skip(offset).limit(pageSize) (but slow), using range queries (but hard to skip more than one page at a time), or using an incremented id index

		// CRM reads city from Datacore (LATER using its own Java model)
		List<DBObject> cities = new ArrayList<DBObject>();
		// either lists cities of its trusted source only ex. France's CityApp
		DBCursor franceCityApp_citiesCurs = citiesColl.find(new BasicDBObject("_source", "FranceCityApp"));
		// or only those of its trusted source that have been approved from (at least an) other source(s)
		/// HARD
		// or all approved ones (from at least one source) NOO (almost impossible to get a consensus)
		/// HARD :
		DBCursor approved_citiesCurs = citiesColl.find(new BasicDBObject("_source", new BasicDBObject("$ne", "FranceCityApp"))
			.append("_cross_approved_version", new BasicDBObject("$ne", new Long(-1))),
			//.append("$where", "this._cross_approved_version != this._cross_version"), // WARNING slow (executed on all)
			new BasicDBObject("_cross_approved_version", 1).append("_cross_approved_id", 1)); // fields to return
		HashMap<String,Long> approvedCrossVersions = new HashMap<String,Long>(approved_citiesCurs.size());
		for (; approved_citiesCurs.hasNext();) {
			DBObject curApprovedCity = approved_citiesCurs.next();
			String approvedCityId = (String) curApprovedCity.get("_cross_approved_id");
			Long approvedCityVersion = (Long) curApprovedCity.get("_cross_approved_version");
			if (!approvedCrossVersions.containsKey(approvedCityId)
					|| approvedCrossVersions.get(approvedCityId) < approvedCityVersion) {
				approvedCrossVersions.put(approvedCityId, approvedCityVersion);
			}
		}
		/// still need to be retrieved
		// or approved ones (from at least one, OR all) other source(s) SAME PB
		/// HARD citiesColl.find(new BasicDBObject("_source", new BasicDBObject("$ne", "FranceCityApp"))
		///	.append("_cross_approved_version", val));
		// or approved ones from at least one other source
		
		// plus those that don't need to be approved HOW KNOWN ?? ARE THERE EVEN ANY ??
		
		// TODO dissociate contributor and master source roles (which could also be single) :
		// for each contribution source, its approved reference source is defined in metamodel
		// (by governance policy, which could possibly delegate to contributor)
		// ex. could be same => p2p approval, or all the same => single master source
		
		// TODO dissociate change on same (conflict if differs) and different domain (if not source / maintained, only an enrichment)
		
		// TODO data quality / qualification see D2.1
		
		// TODO uri ; using id query params ?
		
		DBCursor citiesCurs = franceCityApp_citiesCurs;
		try {
		   while(citiesCurs.hasNext()) {
			   cities.add(citiesCurs.next());
		   }
		} finally {
			citiesCurs.close();
		}
		// user chooses in UI:
		// first country
		///owCompany.setCountry("France");
		// then city OPT TODO in this country
		DBObject selectedCity = cities.get(0);
		///owCompany.setCity((String) selectedCity.get("name")); // NOO rather uri or local model
      owCompany.setCity((String) selectedCity.get("_uri")); // TODO rather local model
      ///City city = cityBuilder.build(DBObject selectedCity); // local model : with @Transient oasisObject and other fields filled by builder
      ///cities = cityServiceOasisImpl.find("..."); // TODO alternatively, mapping  between local & generic remote client models may be in client side cache below CityServiceOasisImpl 
      ///owCompany.setCity(city); // @Transient field ; but may setCityUri...
      ///crmDao/Service.save(owCompany); // ... or DAO / service layer may do it before saving 
		mgo.save(owCompany);
		Assert.assertEquals(owCompany.getCity(), selectedCity.get("_uri"));

		
		// CRM now shares in datacore
		int crmAppMaxQuality = 6;
		DBObject crmApp_owCompany = new BasicDBObject(
				//"_id_source_city", lyonCity.getId()). // NOOOO source id, where "city" is the app id
				"_id_source", owCompany.getId()). // source id TODO index
		        append("_source", "XCRM"). // source app TODO index
		        append("_quality", crmAppMaxQuality).
		        append("_cross_version", new Long(0)). ///
		        append("_cross_approved_version", new Long(0)). // better than none or -1 for queries NOOOO ///
		        // TODO either tx, or rather _base_version NO pb of changes that conflict or made on obsolete version
		        // BUT if change c2 comes after unapproved / unapprovable change c1 it'll have a higher cross version ?! WHICH IS NOT A PROBLEM
		        append("shortName", owCompany.getShortName()).
		        append("legalName", owCompany.getLegalName()).
		        ///append("swpo:inCity" + "_id", selectedCity.get("_id")); // NOO rather on uri else can't support conflicting data versions
            append("swpo:inCity", selectedCity.get("_uri")); // uri
		// source : in metamodel, TODO maybe also in data for querying ?? or as db prefix ???
		orgsColl.insert(crmApp_owCompany);
		Assert.assertNotNull(crmApp_owCompany.get("_id"));
		
		
		// cross domain and app semantic query :
		/*
SELECT ?person ?organization ?project
WHERE { ?p  a foaf:Person .
        	?p foaf:name ?person .
	?p foaf:currentProject ?pr .
	?pr a foaf:Project .
        	?pr foaf:name ?project .
	?p org:memberOf ?org .
	?org a org:Organization .
	?org foaf:name ?organization .
	?org swpo:hasSite ?site .
	?site swpo:hasAddress _:b .
	_:b swpo:inCity  ?city .
		 */
		// TODO for this :
		// * Organizations should be maintained in datacore (Social Graph !!) and CRM Contact be in sync with it
		// * Organizations should be maintained in datacore and CRM's be in sync with it
		
		// simpler one : all organizations of a city named Lyon
		/*
SELECT ?org
WHERE {
	?org a org:Organization .
	?org swpo:inCity ?city .
	?city foaf:name "Lyon" .
		 */
		// TODO this query assumes a metamodel, it must also be defined
		// how to do it ?
		// datacore has no cross-coll(/metamodel??) auto joins (and even less if federated across containers) 
		// so :
		// * if organizations and cities are in the same coll, it can be mapped to a single mongodb query
		// (* if organizations and cities are in the metamodel, these can be done in the right order and merged according to the metamodel)
		// ((* could there be meta-metamodels unifying several metamodels ??))
		/// TODO
		// * else the client must break it down in single-coll(/metamodel??) queries, do them in the right order and merged manually :
		// query on cities
		DBObject lyonCityData = null;
		DBCursor lyonCitiesCurs = citiesColl.find(new BasicDBObject("name", "Lyon"));
		if (lyonCitiesCurs.hasNext()) {
			lyonCityData = lyonCitiesCurs.next();
			// TODO if more than one result, client should decide : according to source, data quality... (if not in query already)
		} // else abort
		Assert.assertNotNull(lyonCityData);
		Assert.assertNotNull(lyonCityData.get("_id"));
		// query on orgs
		//DBCursor lyonCityOrgsCurs = orgsColl.find(new BasicDBObject("swpo:inCity" + "_id", lyonCityData.get("_id"))); // NOO rather on uri else can't support conflicting data versions
      DBCursor lyonCityOrgsCurs = orgsColl.find(new BasicDBObject("swpo:inCity", lyonCityData.get("_uri")));
		Assert.assertTrue(lyonCityOrgsCurs.count() == 1);
		
		
		// TODO perfs : see dedicated test
	
		
		// TODO TODO each shared metamodel probably has its own specific data quality and governance policies and problematics
		// ex. sharing addresses !!
		
	}

}
