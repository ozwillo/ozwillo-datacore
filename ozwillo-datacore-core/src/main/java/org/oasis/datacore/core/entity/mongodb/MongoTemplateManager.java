package org.oasis.datacore.core.entity.mongodb;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.internal.OverridableUuidRepresentationCodecRegistry;
import org.bson.internal.ProvidersCodecRegistry;
import org.oasis.datacore.core.entity.mongodb.joda.DateTimeCodec;
import org.oasis.datacore.core.entity.mongodb.joda.DateTimeCodecProvider;
import org.oasis.datacore.core.entity.mongodb.joda.DateTimeTransformer;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ServerSettings;


/**
 * Provides MongoTemplate (= MongoOperations implementation)
 * that may have custom configuration, according to the current project's configuration :
 * - allows to use a specific secondary for read queries instead
 * NB. DCProject.isDbRobust is applied by DatacoreWriteConcernResolver
 * @author mdutoo
 *
 */
@Component
public class MongoTemplateManager {

   private static final Logger logger = LoggerFactory.getLogger(MongoTemplateManager.class);

   private static final String WILDCARD = "*";

   /** empty or null means none ; instead of java #{${oasis.datacore.mongodb.username}}
    * i.e. quoted string or null, which is cleaner but impacting (pre)prod conf */
   @Value("${oasis.datacore.mongodb.username}")
   private String username;

   /** empty or null means none ; instead of java #{${oasis.datacore.mongodb.username}}
    * i.e. quoted string or null, which is cleaner but impacting (pre)prod conf */
   @Value("${oasis.datacore.mongodb.password}")
   private String password;

   @Value("${oasis.datacore.mongodb.dbname}")
   private String dbName;

   /** host:port allowed to only be secondaries (comma-separated, * is wildcard) */
   @Value("${oasis.datacore.mongodb.allowedSecondaryOnlyServerAddresses}")
   private String allowedSecondaryOnlyServerAddresses;
   private Set<String> allowedSecondaryOnlyServerAddressSet;
   
   /** default mongo, if there is a cluster it is it */
   @Autowired
   private MongoTemplate mgo; // TODO remove it by hiding it in services
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
   private MongoDatabaseFactory mongoDbFactory;

   /** caches default & custom mongos ONLY (not default because can be cluster so not only host prop !) */
   private Map<String,MongoTemplate> customMongoTemplateCacheMap = new HashMap<>();


   @PostConstruct
   protected void init() {
      allowedSecondaryOnlyServerAddressSet = StringUtils.commaDelimitedListToSet(allowedSecondaryOnlyServerAddresses);
      
      if (username == null || username.trim().isEmpty() || username.trim().equals("null")) {
         username = null; // no auth
      } else {
         username = unquote(username);
         password = unquote(password);
      }
      
      configureMongoForJoda(mgo);
   }

   @SuppressWarnings("deprecation")
   private void configureMongoForJoda(MongoTemplate mgo) {
      /////org.bson.BSON.addEncodingHook(org.joda.time.DateTime.class, new DateTimeTransformer());
      try {
         Field f = ProvidersCodecRegistry.class.getDeclaredField("codecProviders");
         f.setAccessible(true);
         CodecRegistry codecRegistry = mgo.getDb().getCodecRegistry();
         if (codecRegistry instanceof OverridableUuidRepresentationCodecRegistry) {
            codecRegistry = (CodecRegistry) ((OverridableUuidRepresentationCodecRegistry) codecRegistry).getWrapped();
         }
         @SuppressWarnings("unchecked")
         List<CodecProvider> codecProviders = (List<CodecProvider>) f.get(codecRegistry);
         codecProviders.add(new DateTimeCodecProvider());
      } catch (Exception e) {
         throw new RuntimeException("Error configuring Mongo for Joda DateTime", e);
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
   
   
   public MongoTemplate getDefaultMongoTemplate() {
      return mgo;
   }
   
   public MongoTemplate getMongoTemplate() {
      return getMongoTemplate(modelService.getProject());
   }

   /**
    * allows to check conf with a not yet saved project
    */
   public MongoTemplate getMongoTemplate(DCProject project) {
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
      
      if (isDefault) {
         return getDefaultMongoTemplate(); // default when not set
      }
      
      project.checkConfig();
      
      String mgoId = buildCustomMgoId(dbUri);
      MongoTemplate mgo = customMongoTemplateCacheMap.get(mgoId);
      if (mgo == null) {
         mgo = createMongoTemplate(dbUri);
         
         synchronized(customMongoTemplateCacheMap) {
            MongoTemplate justSetMgo = customMongoTemplateCacheMap.get(mgoId);
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
    */
   private String buildCustomMgoId(MongoUri dbUri) {
      return "uri=" + dbUri;
   }

   private MongoTemplate createMongoTemplate(MongoUri dbUri) {
      MongoTemplate mgo;
      if (dbUri == null) { // we can reuse the default mongoDbFactory :
         mgo = new MongoTemplate(mongoDbFactory, this.mgo.getConverter());
         
      } else {
         MongoClient mongoClient;
         try {
            MongoCredential mongoCredential = username == null || username.trim().isEmpty()
                    || username.trim().equals("null") ? null
                    : MongoCredential.createPlainCredential(username, dbUri.getDatabase(), password.toCharArray());
            ServerAddress serverAddress = new ServerAddress(dbUri.getHost(), dbUri.getPort());
            Builder clientSettingsBuilder = MongoClientSettings.builder()
                  .codecRegistry(
                        CodecRegistries.fromRegistries(
                              MongoClientSettings.getDefaultCodecRegistry(),
                              CodecRegistries.fromCodecs(new DateTimeCodec())
                              )
                        )
                  .readPreference(ReadPreference.secondaryPreferred())
                  //.writeConcern(legacyMongoClient.getWriteConcern())
                  ///.readConcern(legacyMongoClient.getReadConcern())
                  ///.retryWrites(mongoClientOptions.getRetryWrites())
                  .applyToClusterSettings(clusterSettings -> {
                     clusterSettings.hosts(Collections.singletonList(serverAddress));
                  });
            if (mongoCredential != null) {
               clientSettingsBuilder.credential(mongoCredential);
            }
            mongoClient = MongoClients.create(clientSettingsBuilder.build());
         } catch (Exception e) {
            // configuration problem, don't hide it :
            throw new RuntimeException("Error creating custom mongo " + dbUri, e);
         }
         MongoDatabaseFactory mongoDbFactory = new SimpleMongoClientDatabaseFactory(mongoClient, dbUri.getDatabase());
         mgo = new MongoTemplate(mongoDbFactory, mongoConverter);
      }
      
      configureMongoForJoda(mgo);

      // NB. DCProject.isDbRobust is applied by DatacoreWriteConcernResolver
      
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
