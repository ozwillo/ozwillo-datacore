package org.oasis.datacore.sample.openelec;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.sample.openmairie.Collectivite;
import org.oasis.datacore.sample.tx.Account;
import org.oasis.datacore.sdk.tx.spring.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-crm-test-context.xml" })
public class OpenElecMongoImplTest {

   @Autowired
   private OpenElecServiceImpl openElecService;
   
   // TODO remove them
   @Autowired
   private BureauDao bureauDao;
   @Autowired
   private CollectiviteDao collectiviteDao;
   @Autowired
   private MongoOperations mgo;
   
   @Test
   public void test() throws Exception {
      Assert.assertNotNull(openElecService);
    
      // cleanup
      // db cleanup (TODO in setup / teardown)
      mgo.remove(new Query(), Bureau.class);
      Assert.assertEquals(0,  mgo.findAll(Bureau.class).size());
      mgo.remove(new Query(), Collectivite.class);
      Assert.assertEquals(0,  mgo.findAll(Collectivite.class).size());
      
      // test db init
      Collectivite collectivite = new Collectivite();
      collectivite.setLibelle("Lyon");
      collectivite.setNiveau(Collectivite.NIVEAU_COMCOM);
      openElecService.saveCollectivite(collectivite);
      Assert.assertNotNull(collectivite.getId());
      Bureau bureau = new Bureau();
      bureau.setCode("Lyon325");
      bureau.setLibelle_bureau("Lyon 325 École Antoine Charial");
      bureau.setAdresse1("École Antoine Charial");
      bureau.setAdresse2("25 rue Antoine Charial");
      bureau.setAdresse3("69003 Lyon");
      bureau.setCode_canton("canton1");
      openElecService.saveBureau(bureau);
      Assert.assertNotNull(bureau.getId());
      // reloading bureau and checking it
      bureau = bureauDao.getById(bureau.getId());
      Assert.assertEquals(bureau.getCode(),"Lyon325");
      
      // getting and displaying available collectivites to users
      List<Collectivite> availableCollectivites = openElecService.getCollectivites();
      Collectivite selectedCollectivite = availableCollectivites.get(0);
      
      // setting selected collectivite on bureau and saving it
      bureau.setCollectivite(selectedCollectivite);
      openElecService.saveBureau(bureau);
      
      // reloading bureau and checking collectivite
      bureau = openElecService.getBureauById(bureau.getId());
      Assert.assertEquals(bureau.getCollectivite(), selectedCollectivite);
      Assert.assertEquals(bureau.getCollectivite().getLibelle(), "Lyon");
      
   }
   
}
