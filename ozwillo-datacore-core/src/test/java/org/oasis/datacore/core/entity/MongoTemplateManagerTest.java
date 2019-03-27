package org.oasis.datacore.core.entity;


import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.DCRequestContextProviderFactory;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.oasis.datacore.core.entity.mongodb.DatacoreMongoTemplate;
import org.oasis.datacore.core.entity.mongodb.MongoTemplateManager;
import org.oasis.datacore.core.entity.mongodb.MongoUri;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.CannotGetMongoDbConnectionException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import com.google.common.collect.ImmutableMap;


/**
 *
 * @author mdutoo
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-core-test-context.xml" })
@TestExecutionListeners(listeners = { DependencyInjectionTestExecutionListener.class,
      DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class }) // else (harmless) error :
// TestContextManager [INFO] Could not instantiate TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener]. Specify custom listener classes or make the default listener classes (and their required dependencies) available. Offending class: [javax/servlet/ServletContext]
// see http://stackoverflow.com/questions/26125024/could-not-instantiate-testexecutionlistener
@FixMethodOrder // else random since java 7
public class MongoTemplateManagerTest {

   @Autowired
   private MongoTemplate mt; // to check mongo conf, last operation result...

   @Autowired
   private MongoTemplateManager mtm; // to check MongoTemplateManager
   @Autowired
   private DataModelServiceImpl modelAdminService;
   @Autowired
   ///@Qualifier("datacore.cxfJaxrsApiProvider")
   //protected DCRequestContextProvider requestContextProvider;
   protected DCRequestContextProviderFactory requestContextProviderFactory; // to check MongoTemplateManager


   @Test
   public void testMongoUri() {
      String localhostMongoUri = "mongodb://localhost:27017/datacore";
      MongoUri normalizedMongoUri = MongoUri.parse(localhostMongoUri);
      Assert.assertEquals("datacore", normalizedMongoUri.getDatabase());
   }

   @Test
   public void testMongoTemplateManager() {
      DCProject testProject = new DCProject("mtmTest");
      SimpleRequestContextProvider.setSimpleRequestContext(new ImmutableMap.Builder<String, Object>()
            .put(DCRequestContextProvider.PROJECT, testProject.getName()) // else no write
            // NB. auth doesn't work like that
            .build()); // else "There is no context" in CxfRequestContextProvider l63, TODO simpler ex. in client
      modelAdminService.removeProject(testProject); // in case of previous test (?)
      modelAdminService.addProject(testProject);

      MongoTemplate defaultMongoTemplate = mtm.getMongoTemplate();
//      Assert.assertEquals(mt, defaultMongoTemplate);
      Assert.assertEquals(null, testProject.isDbRobust());

      testProject.setDbRobust(false);
      Assert.assertEquals(testProject.isDbRobust(), false);
      MongoOperations notRobustMongoTemplate = mtm.getMongoTemplate();
//      Assert.assertNotEquals(mt, notRobustMongoTemplate);
      // TODO test

      String localhostMongoUri = "mongodb://localhost:27017/datacore";
      testProject.setDbUri(localhostMongoUri);
      try {
         mtm.getMongoTemplate();
      } catch (RuntimeException rex) {
         Assert.assertTrue(rex.getMessage().contains("robust and uri can't be set both"));
      }
      testProject.setDbRobust(true);
      MongoOperations localhostMongoTemplate = mtm.getMongoTemplate();
      Assert.assertNotEquals(mt, localhostMongoTemplate);

      String localhostNoPortMongoUri = "mongodb://localhost/datacore";
      testProject.setDbUri(localhostNoPortMongoUri);
//      Assert.assertEquals(localhostMongoTemplate, mtm.getMongoTemplate());
      String loopbackIpMongoUri = "mongodb://127.0.0.1:27017/datacore";
      testProject.setDbUri(loopbackIpMongoUri);
      MongoOperations loopbackIpMongoTemplate = mtm.getMongoTemplate();
      Assert.assertNotEquals(localhostMongoTemplate, loopbackIpMongoTemplate);

      // test connection of loopbackIp mongoTemplate :
      try {
         loopbackIpMongoTemplate.collectionExists("test.index");
      } catch (CannotGetMongoDbConnectionException cgmdbex) {
         Assert.assertTrue(cgmdbex.getMessage().contains("not configured among secondary only ones"));
      } catch (Exception rex) {
         Assert.assertTrue(rex.getMessage().contains("not configured among secondary only ones"));
      }

      String notallowedMongoUri = "mongodb://notallowed:27017/datacore";
      testProject.setDbUri(notallowedMongoUri);
      try {
         mtm.getMongoTemplate();
      } catch (RuntimeException rex) {
         Assert.assertTrue(rex.getMessage().contains("not configured among secondary only ones"));
      }

      String wrongMongoUri = "mongodb://nohost/datacore";
      testProject.setDbUri(wrongMongoUri);
      /*try { // disabling else too slow
         mtm.getMongoTemplate();
         Assert.fail("should not be able to connect");
      } catch (Exception e) {
         //com.mongodb.MongoTimeoutException: Timed out after 10000 ms while waiting for a server that matches AnyServerSelector{}. Client view of cluster state is {type=Unknown, servers=[{address=nohost:27017, type=Unknown, state=Connecting, exception={com.mongodb.MongoException$Network: Exception opening the socket}, caused by {java.net.UnknownHostException: nohost: unknown error}}]
         Assert.assert(e instanceof MongoTimeoutException);
      }*/
   }

}
