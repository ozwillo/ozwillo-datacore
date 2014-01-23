package org.oasis.datacore.core.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.model.DCURI;
import org.oasis.datacore.core.meta.model.DCModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;


/**
 * TODO or model cached : in custom context implementing "get" reused in both hasPermission & here ??
 * as transient param of resource ?? in EHCache (but beware of Resource obsolescence) ??
 * 
 * NB. doesn't use Spring MongoRepository (though it can be used for (meta)model),
 * because can't specify collection
 * 
 * @author mdutoo
 *
 */
@Component
public class EntityServiceImpl implements EntityService {
   
   //@Autowired
   //private DCDataEntityRepository dataRepo; // NO rather for (meta)model, for data can't be used because can't specify collection
   @Autowired
   private MongoOperations mgo;
   // NB. MongoTemplate would be required to check last operation result, but we rather use WriteConcerns
   @Autowired
   private EntityModelService entityModelService;


   private DCModel getModel(DCEntity dataEntity) {
      return entityModelService.getModel(dataEntity);
   }
   
   /* (non-Javadoc)
    * @see org.oasis.datacore.core.entity.DCEntityService#create(org.oasis.datacore.core.entity.model.DCEntity)
    */
   @Override
   public void create(DCEntity dataEntity) throws DuplicateKeyException, NonTransientDataAccessException {
      String collectionName = getModel(dataEntity).getCollectionName(); // TODO for view Models or weird type names ?!?
      
      // security : checking type default rights
      // TODO using annotated hasPermission
      /*if (dcModel.getDefaultWriters/Creators().intersect(currentUserRoles).isEmpty()) {
         throw new ForbiddenException();
      }*/
      
      // if exists, will fail (no need to enforce any version) 
      mgo.insert(dataEntity, collectionName);
   }
   
   /* (non-Javadoc)
    * @see org.oasis.datacore.core.entity.DCEntityService#getByUri(java.lang.String, org.oasis.datacore.core.meta.model.DCModel)
    */
   @Override
   public DCEntity getByUri(String uri, DCModel dcModel) throws NonTransientDataAccessException {
      String collectionName = dcModel.getCollectionName(); // TODO for view Models or weird type names ?!?
      //entityService.findById(uri, type/collectionName); // TODO
      //dataEntity = dataRepo.findOne(uri); // NO can't be used because can't specify collection
      //dataEntity = mgo.findById(uri, DCEntity.class, collectionName);
      DCEntity dataEntity = mgo.findOne(new Query(new Criteria("_uri").is(uri)), DCEntity.class, collectionName);
      if (dataEntity != null) {
         dataEntity.setCachedModel(dcModel);
      }
      return dataEntity;
   }

   @Override
   public DCEntity getByUriUnsecured(String uri, DCModel dcModel) throws NonTransientDataAccessException {
      return getByUri(uri, dcModel);
   }
   
   /* (non-Javadoc)
    * @see org.oasis.datacore.core.entity.DCEntityService#isUpToDate(java.lang.String, org.oasis.datacore.core.meta.model.DCModel, java.lang.Long)
    */
   @Override
   public boolean isUpToDate(String uri, DCModel dcModel, Long version)
         throws NonTransientDataAccessException {
      if (version == null) {
         return false;
      }
      
      String collectionName = dcModel.getCollectionName(); // TODO for view Models or weird type names ?!?
      //entityService.findById(uri, type/collectionName); // TODO
      //dataEntity = dataRepo.findOne(uri); // NO can't be used because can't specify collection
      //dataEntity = mgo.findById(uri, DCEntity.class, collectionName);
      Criteria criteria = new Criteria("_uri").is(uri).and("_v").is(version); // TODO or parse Long ??
      long count = mgo.count(new Query(criteria), collectionName);
      // NB. efficient because should not be more than 1 ; or TODO LATER or better execute ?
      return count != 0;
   }

   /* (non-Javadoc)
    * @see org.oasis.datacore.core.entity.DCEntityService#update(org.oasis.datacore.core.entity.model.DCEntity)
    */
   @Override
   public void update(DCEntity dataEntity) throws OptimisticLockingFailureException,
         NonTransientDataAccessException {
      String collectionName = getModel(dataEntity).getCollectionName(); // TODO for view Models or weird type names ?!?
      
      // security : checking rights
      /*if (dataEntity.getWriters().intersect(currentUserRoles).isEmpty() || (getOwners())) {
         throw new ForbiddenException();
      }*/
      // TODO better using annotated hasPermission ?
      // TODO or only as operation criteria ?? ($and _w $in currentUserRoles)
      
      mgo.save(dataEntity, collectionName);
   }
   
   /* (non-Javadoc)
    * @see org.oasis.datacore.core.entity.DCEntityService#deleteByUriId(java.lang.String, long, org.oasis.datacore.core.meta.model.DCModel)
    */
   @Override
   public void deleteByUriId(DCEntity dataEntity) throws NonTransientDataAccessException {
	   
	   String collectionName = getModel(dataEntity).getCollectionName(); // TODO for view Models or weird type names ?!?
      
      // NB. could first check 1. that uri exists & has version and 2. user has write rights
      // and fail if not, but in Mongo & REST spirit it's enough to merely ensure that
      // it doesn't exist at the end
      
      Query query = new Query(Criteria.where("_uri").is(dataEntity.getUri()).and("_v").is(dataEntity.getVersion())
            /*.and("_w").in(currentUserRoles)*/);
      mgo.remove(query, collectionName);
      // NB. obviously won't conflict / throw MongoDataIntegrityViolationException
      
      // NB. for proper error handling, we use WriteConcerns that ensures that errors are raised,
      // rather than barebone MongoTemplate.getDb().getLastError() (which they do),
      // see http://hackingdistributed.com/2013/01/29/mongo-ft/
      // However if we did it, it would be :
      // get operation result (using spring WriteResultChecking or native mt.getDb().getLastError())
      // then handle it / if failed throw error status :
      //if (notfound) {
      //   throw new WebApplicationException(Response.Status.NOT_FOUND);
      //   // rather than NO_CONTENT ; like Atol ex. deleteApplication in
      //   // https://github.com/pole-numerique/oasis/blob/master/oasis-webapp/src/main/java/oasis/web/apps/ApplicationDirectoryResource.java
      //}
   }
   

   /* (non-Javadoc)
    * @see org.oasis.datacore.core.entity.DCEntityService#getSampleData()
    */

	@Override
	public void changeRights(DCEntity dataEntity) {

		if (dataEntity == null) {
			throw new RuntimeException("Entity cannot be null while changing rights");
		}
		
		String collectionName = getModel(dataEntity).getCollectionName();
		mgo.save(dataEntity, collectionName);
		
	}
	
	@Override
	public void getRights(DCEntity dataEntity) {
		return;
	}
   
   @Override
   public DCEntity getSampleData() {
      String sampleCollectionName = "sample";
      
      DCEntity dcEntity = mgo.findOne(new Query(), DCEntity.class, sampleCollectionName);
      
      if (dcEntity != null) {
         return dcEntity;
      }
      dcEntity = new DCEntity();
      dcEntity.setUri("http://data.oasis-eu.org/sample/1");
      Map<String, Object> props = dcEntity.getProperties();
      
      props.put("string", "some text");
      props.put("int", 2);
      props.put("boolean", true);
      props.put("date", new DateTime());
      
      props.put("geoloc", "SAMPLE_GEOLOC_WKT_FORMAT");

      Map<String,String> i18nMap = new HashMap<String,String>();
      i18nMap.put("en", "London");
      i18nMap.put("fr", "Londres"); // NB. fallbacks might be defined on type data governance...
      props.put("i18nAlt1Field", i18nMap);
      props.put("i18nAlt2Field", "London"); // "real" value
      props.put("i18nAlt2Field__i18n", i18nMap); // TODO __i ??
      
      props.put("dcRef", new DCURI("http://data.oasis-eu.org", "city", "London").toString());
      props.put("scRef", new DCURI("http://social.oasis-eu.org", "user", "john").toString());
      
      List<String> stringList = new ArrayList<String>();
      stringList.add("a");
      stringList.add("b");
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("a", "a");
      map.put("b", 2);
      map.put("c", new ArrayList<String>(stringList));
      map.put("dcRef", new DCURI("http://data.oasis-eu.org", "city", "London").toString());
      map.put("scRef", new DCURI("http://social.oasis-eu.org", "user", "john").toString());
      List<Map<String,Object>> mapList = new ArrayList<Map<String,Object>>();
      mapList.add(new HashMap<String,Object>(map));
      props.put("stringList", stringList);
      props.put("map", map);
      props.put("mapList", mapList);
      
      // TODO partial copy
      // OPT if used / modeled : URL...
      // OPT if modeled : BigInteger/Decimal, Locale, Serializing (NOT mongo binary)
      
      mgo.insert(dcEntity, sampleCollectionName);
      
      return getSampleData();
   }

}
