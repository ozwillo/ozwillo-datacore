package org.oasis.datacore.core.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.model.DCURI;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.prepost.PostAuthorize;
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
public class DCEntityService {
   
   //@Autowired
   //private DCDataEntityRepository dataRepo; // NO rather for (meta)model, for data can't be used because can't specify collection
   @Autowired
   private MongoOperations mgo;
   // NB. MongoTemplate would be required to check last operation result, but we rather use WriteConcerns
   @Autowired
   private DCModelService dcModelService; // ???


   /**
    * Helper using entity cached transient model (mainainted over the course
    * of a request only) if possible
    * @param dataEntity
    * @return
    */
   public DCModel getModel(DCEntity dataEntity) {
      DCModel cachedModel = dataEntity.getCachedModel();
      if (cachedModel != null) {
         return cachedModel;
      }
      cachedModel = dcModelService.getModel(this.getModelName(dataEntity));
      dataEntity.setCachedModel(cachedModel);
      return cachedModel;
   }
   public String getModelName(DCEntity dataEntity) {
      List<String> types = dataEntity.getTypes();
      if (types != null && !types.isEmpty()) {
         return types.get(0);
      }
      return null;
   }


   /**
    * TODO cachedModel
    * @param dataEntity
    * @param dcModel
    */
   public void create(DCEntity dataEntity, DCModel dcModel) {
      String collectionName = dcModel.getCollectionName(); // TODO for view Models or weird type names ?!?
      
      // security : checking type default rights
      // TODO using annotated hasPermission
      /*if (dcModel.getDefaultWriters/Creators().intersect(currentUserRoles).isEmpty()) {
         throw new ForbiddenException();
      }*/
      
      // if exists, will fail (no need to enforce any version) 
      mgo.insert(dataEntity, collectionName);
   }
   
   /**
    * TODO or model cached : in custom context implementing "get" reused in both hasPermission & here ??
    * as transient param of resource ?? in EHCache (but beware of Resource obsolescence) ??
    * This method does not change state (else @PostAuthorize would not work)
    * @param uri
    * @param dcModel
    * @return
    */
   @PostAuthorize("hasPermission(returnObject, 'read')")
   public DCEntity getByUriId(String uri, DCModel dcModel) {
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
   /**
    * Used for more efficient If-None-Match=versionETag conditional GET :
    * allows not to load entity (nor resource) if same version.
    * BEWARE no rights check, so must ALWAYS be followed by ex. getByUriId
    * to check them.
    * @param uri
    * @param dcModel
    * @param version
    * @return null if given version matches
    */
   public boolean isUpToDate(String uri, DCModel dcModel, Long version) {
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

   /**
    * TODO cachedModel
    * @param dataEntity must exist and its ACLs be in sync with saved ones
    * @param dcModel
    * @throws OptimisticLockingFailureException
    */
   @PreAuthorize("hasPermission(#dataEntity, 'write')")
   // NB. authorize before works because calling resourceService has loaded existing ACLs in dataEntity
   //@PreAuthorize("hasRole('admin') or hasRole('t_' + #dcModel.getName() + '_admin') or hasPermission(#dataEntity, 'write')")
   // NB. (model type) admin role check is done in hasPermission so it's never forgotten
   // (but could be done explicitly if no call to hasPermission is done)
   // NB. methods available in security el are (Method)SecurityExpressionRoot's
   // TODO on interface ; with default annot @PreAuthorize(“hasRole(‘ROLE_EXCLUDE_ALL’)”)
   // see http://www.disasterarea.co.uk/blog/protecting-service-methods-with-spring-security-annotations/
   public void update(DCEntity dataEntity, DCModel dcModel) throws OptimisticLockingFailureException {
      String collectionName = dcModel.getCollectionName(); // TODO for view Models or weird type names ?!?
      
      // security : checking rights
      /*if (dataEntity.getWriters().intersect(currentUserRoles).isEmpty() || (getOwners())) {
         throw new ForbiddenException();
      }*/
      // TODO better using annotated hasPermission ?
      // TODO or only as operation criteria ?? ($and _w $in currentUserRoles)
      
      mgo.save(dataEntity, collectionName);
   }
   
   //@PreAuthorize("hasPermission(#dataEntity, 'write')")
   public void deleteByUriId(String uri, long version, DCModel dcModel) {
      String collectionName = dcModel.getCollectionName(); // TODO for view Models or weird type names ?!?
      
      // NB. could first check 1. that uri exists & has version and 2. user has write rights
      // and fail if not, but in Mongo & REST spirit it's enough to merely ensure that
      // it doesn't exist at the end
      
      Query query = new Query(Criteria.where("_uri").is(uri).and("_v").is(version)
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
