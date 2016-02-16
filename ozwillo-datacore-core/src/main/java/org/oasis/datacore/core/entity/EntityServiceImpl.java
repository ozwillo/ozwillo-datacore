package org.oasis.datacore.core.entity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import com.mongodb.DuplicateKeyException;
import org.joda.time.DateTime;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.SimpleUriService;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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


   @Override
   public boolean isCreation(Long version) {
      return (version == null || version < 0);
   }

   private void addMultiProjectStorageValue(DCEntity dataEntity, DCModelBase storageModel) {
      if (storageModel.isMultiProjectStorage()) {
         if (dataEntity.getProjectName() == null) {
            // recompute to make sure its up-to-date : (TODO LATER optimize / cache)
            dataEntity.setProjectName(entityModelService.getProject().getName());
         } // else don't change, else it would not be visible anymore from projects the current one depends on
         // NOO EntityPermissionEvaluator prevents it from being written from another project
      } else {
         dataEntity.setProjectName(null);
      }
   }

   /* (non-Javadoc)
    * @see org.oasis.datacore.core.entity.DCEntityService#create(org.oasis.datacore.core.entity.model.DCEntity)
    */
   @Override
   public void create(DCEntity dataEntity) throws NonTransientDataAccessException {
      Long version = dataEntity.getVersion();
      if (version != null && version >= 0) { // version < 0 not allowed (though it is in Resources)
         throw new OptimisticLockingFailureException("Trying to create entity with version");
      }
      DCModelBase storageModel = entityModelService.getStorageModel(dataEntity); // TODO for view Models or weird type names ?!?
      String collectionName = storageModel.getCollectionName(); // TODO for view Models or weird type names ?!?
      addMultiProjectStorageValue(dataEntity, storageModel);
      
      // security : checking type default rights
      // TODO using annotated hasPermission
      /*if (dcModel.getDefaultWriters/Creators().intersect(currentUserRoles).isEmpty()) {
         throw new ForbiddenException();
      }*/
      
      // if exists (with same uri, and if multiProjectStorage same project),
      // will fail (no need to enforce any version) 
      mgo.insert(dataEntity, collectionName);
   }
   
   /* (non-Javadoc)
    * @see org.oasis.datacore.core.entity.DCEntityService#getByUri(java.lang.String, org.oasis.datacore.core.meta.model.DCModel)
    */
   @Override
   public DCEntity getByUri(String uri, DCModelBase dcModel)
         throws NonTransientDataAccessException, EntityException {
      DCModelBase storageModel = entityModelService.getStorageModel(dcModel);
      // (explodes if none or it URI has been forked below)
      String collectionName = storageModel.getCollectionName(); // TODO for view Models or weird type names ?!?
      Criteria criteria = new Criteria(DCEntity.KEY_URI).is(uri);
      entityModelService.addMultiProjectStorageCriteria(criteria, storageModel, uri);
      //entityService.findById(uri, type/collectionName); // TODO
      //dataEntity = dataRepo.findOne(uri); // NO can't be used because can't specify collection
      //dataEntity = mgo.findById(uri, DCEntity.class, collectionName);
      List<DCEntity> entitiesFound = mgo.find(new Query(criteria) , DCEntity.class, collectionName);
      switch (entitiesFound.size()) {
      case 0:
         return null;
      case 1:
         break;
      default:
         throw new EntityException("Can't get Resource, more than one with same URI "
               + uri + " (in projects " + entitiesFound.stream()
               .map(e -> e.getProjectName()).collect(Collectors.toList())
               + "): has been forked implicitly, should also be forked explicitly "
               + "(add it in its project's dcmp:forkedUris)");
      }
      DCEntity dataEntity = entitiesFound.get(0);
      entityModelService.fillDataEntityCaches(dataEntity, dcModel, storageModel, null); // TODO def
      return dataEntity;
   }

   @Override
   public DCEntity getByUriUnsecured(String uri, DCModelBase dcModel)
         throws NonTransientDataAccessException, EntityException {
      return getByUri(uri, dcModel);
   }
   
   /* (non-Javadoc)
    * @see org.oasis.datacore.core.entity.DCEntityService#isUpToDate(java.lang.String, org.oasis.datacore.core.meta.model.DCModel, java.lang.Long)
    */
   @Override
   public boolean isUpToDate(String uri, DCModelBase dcModel, Long version)
         throws NonTransientDataAccessException, EntityException {
      return getIfUpToDate(uri, dcModel, version) != null;
   }
   private DCEntity getIfUpToDate(String uri, DCModelBase dcModel, Long version) {
      if (version == null || version < 0) {
         return null;
      }

      DCModelBase storageModel = entityModelService.getStorageModel(dcModel);
      String collectionName = storageModel.getCollectionName();
      Criteria criteria = new Criteria(DCEntity.KEY_URI).is(uri).and(DCEntity.KEY_V).is(version)
              .and(DCEntity.KEY_ALIAS_OF).exists(false);
      entityModelService.addMultiProjectStorageCriteria(criteria, storageModel, uri);
      //entityService.findById(uri, type/collectionName); // TODO
      //dataEntity = dataRepo.findOne(uri); // NO can't be used because can't specify collection
      //dataEntity = mgo.findById(uri, DCEntity.class, collectionName);
      long count = mgo.count(new Query(criteria), collectionName);
      // NB. efficient because should not be more than 1 ; or TODO LATER or better execute ?
      if (count == 0) {
         return null;
      }
      
      // must first check for possible implicit only fork :
      return this.getByUri(uri, dcModel); // will explode in this case
      // (actually unsecured since called from the same place)
      // (not using mgo.count() otherwise no info about which projects it has been forked in)
   }

   /* (non-Javadoc)
    * @see org.oasis.datacore.core.entity.DCEntityService#update(org.oasis.datacore.core.entity.model.DCEntity)
    */
   @Override
   public void update(DCEntity dataEntity) throws OptimisticLockingFailureException,
         NonTransientDataAccessException {
      if (dataEntity.getVersion() == null) { // (version < 0 not allowed, could be checked but won't be found anyway)
         throw new OptimisticLockingFailureException("Trying to update entity without version >= 0");
      }
      addMultiProjectStorageValue(dataEntity, entityModelService.getStorageModel(dataEntity));
      String collectionName = entityModelService.getCollectionName(dataEntity); // TODO for view Models or weird type names ?!?
      
      // security : checking rights
      /*if (dataEntity.getWriters().intersect(currentUserRoles).isEmpty() || (getOwners())) {
         throw new ForbiddenException();
      }*/
      // TODO better using annotated hasPermission ?
      // TODO or only as operation criteria ?? ($and _w $in currentUserRoles)

      mgo.save(dataEntity, collectionName); // (spring data ensures atomically same version)
   }
   
   /* (non-Javadoc)
    * @see org.oasis.datacore.core.entity.DCEntityService#deleteByUriId(java.lang.String, long, org.oasis.datacore.core.meta.model.DCModel)
    */
   @Override
   public void deleteByUriId(DCEntity dataEntity) throws NonTransientDataAccessException, EntityException {

      DCModelBase storageModel = entityModelService.getStorageModel(dataEntity);
      String collectionName = storageModel.getCollectionName(); // TODO for view Models or weird type names ?!?
      
      // NB. could first check that uri exists & has version
      // and fail if not, but in Mongo & REST spirit it's enough to merely ensure that
      // it doesn't exist at the end
      
      Criteria criteria = Criteria.where(DCEntity.KEY_URI).is(dataEntity.getUri()).and(DCEntity.KEY_V).is(dataEntity.getVersion());
      entityModelService.addMultiProjectStorageCriteria(criteria, storageModel, dataEntity); // in EXACT same project
      mgo.remove(new Query(criteria), collectionName);
      // NB. NOT remove(dataEntity) which DOESN'T check version through Spring Data @Version !!!! (tested)
      // NB. obviously won't conflict / throw MongoDataIntegrityViolationException
      
      // NB. for proper error handling, we use WriteConcerns that ensures that errors are raised,
      // rather than barebone MongoTemplate.getDb().getLastError() (which they do),
      // see http://hackingdistributed.com/2013/01/29/mongo-ft/
      // However if we did it, it would be :
      // get operation result (using spring WriteResultChecking or native mt.getDb().getLastError())
      // then handle it / if failed throw error status :
      //if (notfound) {
      //   throw new WebApplicationException(Response.Status.NOT_FOUND);
      //   // rather than NO_CONTENT ; like Atol
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
		
		String collectionName = entityModelService.getCollectionName(dataEntity);
      addMultiProjectStorageValue(dataEntity, entityModelService.getStorageModel(dataEntity));
		mgo.save(dataEntity, collectionName);
		
	}
	
	@Override
	public void getRights(DCEntity dataEntity) {
		return;
	}
   
   @Override
   public DCEntity getSampleData() throws URISyntaxException {
      String sampleCollectionName = "sample";
      
      DCEntity dcEntity = mgo.findOne(new Query(), DCEntity.class, sampleCollectionName);
      
      if (dcEntity != null) {
         return dcEntity;
      }
      dcEntity = new DCEntity();
      dcEntity.setUri("http://data.ozwillo.com/sample/1");
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
      
      props.put("dcRef", SimpleUriService.buildUri(null, "city", "London"));
      props.put("scRef", SimpleUriService.buildUri(new URI("http://social.ozwillo.com"), "user", "john"));
      
      List<String> stringList = new ArrayList<String>();
      stringList.add("a");
      stringList.add("b");
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("a", "a");
      map.put("b", 2);
      map.put("c", new ArrayList<String>(stringList));
      map.put("dcRef", SimpleUriService.buildUri(null, "city", "London"));
      map.put("scRef", SimpleUriService.buildUri(new URI("http://social.ozwillo.com"), "user", "john"));
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


   @Override
   public void aliasOf(DCEntity target, String uri) throws NonTransientDataAccessException {
      DCEntity alias = new DCEntity();
      alias.setAliasOf(target.getUri());
      alias.setUri(uri);
      alias.setVersion(null);

      DCModelBase storageModel = entityModelService.getStorageModel(target); // TODO for view Models or weird type names ?!?
      String collectionName = storageModel.getCollectionName(); // TODO for view Models or weird type names ?!?
      addMultiProjectStorageValue(alias, storageModel);

      mgo.insert(alias, collectionName);

   }

   @Override
   public String getAliased(String uri, DCEntity rawEntity) {
      DCModelBase storageModel = entityModelService.getStorageModel(rawEntity);
      return getAliasedByStorageModel(uri, storageModel);
   }

   @Override
   public String getAliased(String uri, DCModelBase model) {
      DCModelBase storageModel = entityModelService.getStorageModel(model);
      return getAliasedByStorageModel(uri, storageModel);
   }

   private String getAliasedByStorageModel(String uri, DCModelBase storageModel) {
      String collectionName = storageModel.getCollectionName();

      return mgo.findOne(new Query(new Criteria(DCEntity.KEY_URI).is(uri)), DCEntity.class, collectionName).getAliasOf();
   }



   @Override
   public boolean isAlias(String uri, DCEntity rawEntity) {
      if (uri == null) {
         return false;
      }
      DCModelBase storageModel = entityModelService.getStorageModel(rawEntity);
      return isAliasByStorageModel(uri, storageModel);
   }

   @Override
   public boolean isAlias(String uri, DCModelBase model) {
      if (uri == null) {
         return false;
      }

      DCModelBase storageModel = entityModelService.getStorageModel(model);
      return isAliasByStorageModel(uri, storageModel);
   }

   private boolean isAliasByStorageModel(String uri, DCModelBase storageModel) {
      String collectionName = storageModel.getCollectionName();

      return mgo.count(new Query(new Criteria(DCEntity.KEY_URI).is(uri).and(DCEntity.KEY_ALIAS_OF).exists(true)), collectionName) > 0;
   }


   @Override
   public void deleteAliases(String uri, DCModelBase model) {
      DCModelBase storageModel = entityModelService.getStorageModel(model);
      String collectionName = storageModel.getCollectionName();

      recurseDeleteAliases(uri, collectionName);
   }

   private void recurseDeleteAliases(String uri, String collectionName) {
      List<DCEntity> entities = mgo.find(new Query(new Criteria(DCEntity.KEY_ALIAS_OF).is(uri)), DCEntity.class, collectionName);
      for (DCEntity entity : entities) {
         recurseDeleteAliases(entity.getUri(), collectionName);
         mgo.remove(entity, collectionName); // NB here it's okay that we don't check version numbers: aliases aren't versioned.
      }

   }

   @Override
   public void unwindAliases(DCEntity dataEntity, String previousUri) {
      String collectionName = entityModelService.getStorageModel(dataEntity).getCollectionName();

      DCEntity existing = mgo.findOne(new Query(new Criteria(DCEntity.KEY_URI).is(dataEntity.getUri()).and(DCEntity.KEY_ALIAS_OF).exists(true)), DCEntity.class, collectionName);
      if (existing != null) {
         // OK, we have an alias where we want to put our entity.
         // check that it points to our entity before deleting it.
         String uri = walkDownAliasChain(existing, collectionName);
         if (uri != null && uri.equals(previousUri)) {
            // remove the alias
            mgo.remove(existing, collectionName);
         }
      }

   }

   /**
    * Find the final uri that the alias points to
    */
   private String walkDownAliasChain(DCEntity alias, String collectionName) {
      if (alias.isAlias()) {
         DCEntity target = mgo.findOne(new Query(new Criteria(DCEntity.KEY_URI).is(alias.getAliasOf())), DCEntity.class, collectionName);
         if (target == null) {
            return null;
         }
         return walkDownAliasChain(target, collectionName);
      }

      return alias.getUri();
   }

}
