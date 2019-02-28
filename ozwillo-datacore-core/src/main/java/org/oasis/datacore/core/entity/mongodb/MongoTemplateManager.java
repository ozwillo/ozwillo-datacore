package org.oasis.datacore.core.entity.mongodb;

import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import com.mongodb.MongoClient;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.authentication.UserCredentials;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.mongodb.Mongo;
import com.mongodb.WriteConcern;


/**
 * Provides DatacoreMongoTemplate (= MongoOperations implementation)
 * that may have custom configuration, according to the current project's configuration :
 * - allows to use a specific secondary for read queries instead
 * - allows to write without waiting for confirmation
 * @author mdutoo
 *
 */
@Component
public class MongoTemplateManager {

   private static final Logger logger = LoggerFactory.getLogger(MongoTemplateManager.class);

   public static final String WILDCARD = "*";

   /** empty or null means none ; instead of java #{${oasis.datacore.mongodb.username}}
    * i.e. quoted string or null, which is cleaner but impacting (pre)prod conf */
   @Value("${oasis.datacore.mongodb.username}")
   private String username;

   /** empty or null means none ; instead of java #{${oasis.datacore.mongodb.username}}
    * i.e. quoted string or null, which is cleaner but impacting (pre)prod conf */
   @Value("${oasis.datacore.mongodb.password}")
   private String password;

   /** default WriteConcern */
   @Value("#{T(com.mongodb.WriteConcern).valueOf(\"${oasis.datacore.mongodb.writeConcern}\")}")
   private WriteConcern writeConcern;

   /** host:port allowed to only be secondaries (comma-separated, * is wildcard) */
   @Value("${oasis.datacore.mongodb.allowedSecondaryOnlyServerAddresses}")
   private String allowedSecondaryOnlyServerAddresses;
   private Set<String> allowedSecondaryOnlyServerAddressSet;
   
   /** default mongo, if there is a cluster it is it */
   @Autowired
   private /*MongoOperations*/DatacoreMongoTemplate mgo; // TODO remove it by hiding it in services
   /** to access request-specific mongo conf */
   @Autowired
   protected DCModelService modelService;

   /** used to build custom mongos */
   @Qualifier("mappingConverter")
   @Autowired
   private MongoConverter mongoConverter;

   /** used to build default URI-mongos (with another customized param) */
   @Qualifier("mongoDbFactory")
   @Autowired
   private MongoDbFactory mongoDbFactory;

   /** caches default & custom mongos ONLY (not default because can be cluster so not only host prop !) */
   private Map<String,DatacoreMongoTemplate> customMongoTemplateCacheMap = new HashMap<String,DatacoreMongoTemplate>();


   @PostConstruct
   protected void init() throws URISyntaxException {
      allowedSecondaryOnlyServerAddressSet = StringUtils.commaDelimitedListToSet(allowedSecondaryOnlyServerAddresses);
      
      if (username == null || username.trim().isEmpty() || username.trim().equals("null")) {
         username = null; // no auth
      } else {
         username = unquote(username);
         password = unquote(password);
      }
   }
   
   private String unquote(String s) {
      if (s == null || s.length() <= 2) {
         return s;
      }
      s = s.trim();
      if (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
         return s.substring(1, s.length() - 1); 
      }
      return s;
   }
   
   
   public DatacoreMongoTemplate getDefaultMongoTemplate() {
      return mgo;
   }
   
   public DatacoreMongoTemplate getMongoTemplate() {
      return getMongoTemplate(modelService.getProject());
   }
   /**
    * allows to check conf with a not yet saved project
    * @param project
    * @return
    */
   public DatacoreMongoTemplate getMongoTemplate(DCProject project) {
      boolean isDefault = true;
      
      MongoUri dbUri = MongoUri.parse(project.getDbUri());
      if (dbUri != null) {
         isDefault = false;
         // check that is not primary :
         if (!isSecondaryOnly(dbUri)) {
            // configuration problem, don't hide it :
            throw new RuntimeException("Custom mongo " + dbUri
                  + " host:port is not configured among secondary only ones in project "
                  + project.getName() + "(which should have cfg.members[i].priority = 0)");
         }
         // TODO LATER also check that it belongs to the cluster (or has a valid / sync'd copy of its data) :
         // by comparing server address to replica set addresses injected from conf
      }
      
      Boolean isDbRobust = project.isDbRobust();
      if (isDbRobust == null) {
         isDbRobust = true;
      } else if (!isDbRobust) {
         isDefault = false;
      }
      // (isDbRobust and dbUri should never occur both at the same time anyway)
      
      if (isDefault) {
         return mgo; // default when not set
      }
      
      if (dbUri != null && !isDbRobust) {
         throw new RuntimeException("DB's robust and uri can't be set both in project "
               + project.getName());
      }
      
      String mgoId = buildCustomMgoId(dbUri, isDbRobust);
      DatacoreMongoTemplate mgo = customMongoTemplateCacheMap.get(mgoId);
      if (mgo == null) {
         mgo = createMongoTemplate(dbUri, isDbRobust);
         
         synchronized(customMongoTemplateCacheMap) {
            DatacoreMongoTemplate justSetMgo = customMongoTemplateCacheMap.get(mgoId);
            if (justSetMgo != null) {
               mgo = justSetMgo; // rather reuse mgo that has been created in between
            } else {
               customMongoTemplateCacheMap.put(mgoId, mgo);
            }
         }
      }
      
      logger.debug("Using custom mongo " + dbUri);
      return mgo;
   }
   

   /**
    * 
    * @param dbUri can be null if default (and another param is not default)
    * @param isDbRobust
    * @return
    */
   private String buildCustomMgoId(MongoUri dbUri, Boolean isDbRobust) {
      return "uri=" + dbUri + "," + "robust=" + isDbRobust;
   }


   @SuppressWarnings("deprecation")
   private DatacoreMongoTemplate createMongoTemplate(MongoUri dbUri, Boolean isDbRobust) {
      DatacoreMongoTemplate mgo;
      if (dbUri == null) { // we can reuse the default mongoDbFactory :
         mgo = new DatacoreMongoTemplate(mongoDbFactory, this.mgo.getConverter());
         
      } else {
//         Mongo mongo;
         MongoClient mongoClient;
         try {
//            mongo = new Mongo(dbUri.getHost(), dbUri.getPort());
            mongoClient = new MongoClient(dbUri.getHost(), dbUri.getPort());
            mongoClient.slaveOk();
         } catch (Exception e) {
            // configuration problem, don't hide it :
            throw new RuntimeException("Error creating custom mongo " + dbUri, e);
         }
         
         UserCredentials credentials = username == null || username.trim().isEmpty()
               || username.trim().equals("null") ? null :
               new UserCredentials(username, password);
         MongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(mongoClient, dbUri.getDatabase());
         NoIndexCreationMongoConverter readonlyMongoConverter = new NoIndexCreationMongoConverter(mongoConverter);
         mgo = new DatacoreMongoTemplate(mongoDbFactory, readonlyMongoConverter);
         readonlyMongoConverter.completeInit(mgo);
      }

      if (isDbRobust) {
         mgo.setWriteConcern(writeConcern);
      } else {
         mgo.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
      }
      
      return mgo;
   }


   /**
    * check that is not primary :
    * (so that it can be targeted as custom secondary by an LDDatabaseLink without risk)
    * the best way is to only allow mongo servers that have been configured
    * to never become primary (cfg.members[i].priority = 0) :
    * https://docs.mongodb.com/v2.6/tutorial/configure-secondary-only-replica-set-member/
    * because otherwise the Java driver provides very little insight in the replset conf :
    * (and this info would have to be refreshed too often anyway)
    * @param cleanDbUri
    * @return
    */
   private boolean isSecondaryOnly(MongoUri cleanDbUri) {
      String serverAddress = cleanDbUri.getHost() + ':' + cleanDbUri.getPort();
      return allowedSecondaryOnlyServerAddressSet.contains(serverAddress)
            || allowedSecondaryOnlyServerAddressSet.contains(WILDCARD);
   }
   

}
