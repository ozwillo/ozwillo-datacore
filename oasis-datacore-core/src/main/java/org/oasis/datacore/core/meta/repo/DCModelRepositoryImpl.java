package org.oasis.datacore.core.meta.repo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.model.DCURI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

/**
 * Custom coded queries are impl'd here.
 * WARNING impl class has to be named after interface class and not
 * custom interface class, see
 * http://stackoverflow.com/questions/17035419/spring-data-mongodb-custom-implementation-propertyreferenceexception
 * 
 * @author mdutoo
 *
 */
public class DCModelRepositoryImpl implements DCModelRepositoryCustom {

   @Autowired
   private MongoOperations mgo;
   
   public DCEntity getSampleData() {
      // TODO test
      DCEntity dcEntity = mgo.findOne(new Query(), DCEntity.class, "sample");
      
      if (dcEntity == null) {
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
         
         props.put("dcRef", new DCURI("data.oasis-eu.org", "city", "London"));
         props.put("scRef", new DCURI("social.oasis-eu.org", "user", "john"));
         
         List<String> stringList = new ArrayList<String>();
         stringList.add("a");
         stringList.add("b");
         Map<String,Object> map = new HashMap<String,Object>();
         map.put("a", "a");
         map.put("b", 2);
         map.put("c", new ArrayList<String>(stringList));
         map.put("dcRef", new DCURI("data.oasis-eu.org", "city", "London"));
         map.put("scRef", new DCURI("social.oasis-eu.org", "user", "john"));
         List<Map<String,Object>> mapList = new ArrayList<Map<String,Object>>();
         mapList.add(new HashMap<String,Object>(map));
         props.put("stringList", stringList);
         props.put("map", map);
         props.put("mapList", mapList);
         
         // TODO partial copy
         // OPT if used / modeled : URL...
         // OPT if modeled : BigInteger/Decimal, Locale, Serializing (NOT mongo binary)
         
         mgo.insert(dcEntity);
      }
      
      return dcEntity;
   }
   
}
