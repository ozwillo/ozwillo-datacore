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
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;


/**
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
   @Autowired
   private DCModelService dcModelService; // ???

   public DCEntity getByUriId(String uri, DCModel dcModel) {
      String collectionName = dcModel.getCollectionName(); // TODO getType() or getCollectionName() for weird type names ?!?
      //entityService.findById(uri, type/collectionName); // TODO
      //dataEntity = dataRepo.findOne(uri); // NO can't be used because can't specify collection
      //dataEntity = mgo.findById(uri, DCEntity.class, collectionName);
      DCEntity dataEntity = mgo.findOne(new Query(new Criteria("_uri").is(uri)), DCEntity.class, collectionName);
      return dataEntity;
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
