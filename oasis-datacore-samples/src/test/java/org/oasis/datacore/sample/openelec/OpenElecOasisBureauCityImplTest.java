package org.oasis.datacore.sample.openelec;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.sample.openmairie.Collectivite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-crm-test-context.xml" })
public class OpenElecOasisBureauCityImplTest {

   @Autowired
   private OpenElecServiceOasisBureauCityImpl openElecService;
   @Autowired
   private OpenElecServiceOasisCityImpl openElecServiceOasisCityImpl; // only before migration of bureau to datacore

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
      // TODO datacore cleanup
      
      // test db init
      // TODO datacore init : Collectivite
      // TODO bureau init : save locally, then share in datacore
      Bureau bureau = new Bureau();
      bureau.setCode("Lyon325");
      bureau.setLibelle_bureau("Lyon 325 École Antoine Charial");
      bureau.setAdresse1("École Antoine Charial");
      bureau.setAdresse2("25 rue Antoine Charial");
      bureau.setAdresse3("69003 Lyon");
      bureau.setCode_canton("canton1");
      openElecServiceOasisCityImpl.saveBureau(bureau);
      openElecService.saveBureau(bureau);
      Assert.assertNotNull(bureau.getId());
      
      // getting and displaying available collectivites to users
      List<Collectivite> availableCollectivites = openElecService.getCollectivites();
      Collectivite selectedCollectivite = availableCollectivites.get(0);
      
      // setting selected collectivite on bureau and saving it
      bureau.setCollectivite(selectedCollectivite);
      openElecService.saveBureau(bureau);
      
      // reloading bureau and checking collectivite
      bureau = openElecService.getBureauById(bureau.getId()); // TODO ByUri
      //Assert.assertEquals(bureau.getCollectivite(), selectedCollectivite); // doesn't work once collectivite in oasis datacore => setId(getDcObject().getUri()) ???
      Assert.assertEquals(bureau.getCollectivite().getDcObject().getUri(), selectedCollectivite.getDcObject().getUri());
      
   }
   
}
