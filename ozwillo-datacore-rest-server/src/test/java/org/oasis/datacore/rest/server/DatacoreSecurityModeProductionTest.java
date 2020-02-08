package org.oasis.datacore.rest.server;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * 
 * @author mdutoo
 *
 */
@Ignore // doesn't work because no HTTP server (had already failed to add jetty)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-deploy-prod-context.xml" })
@TestPropertySource(properties = {"datacore.devmode = false"}) // or in static { https://stackoverflow.com/questions/11306951/how-to-set-environment-variable-or-system-property-in-spring-tests
//@FixMethodOrder(MethodSorters.NAME_ASCENDING) // else random since java 7 NOT REQUIRED ANYMORE
public class DatacoreSecurityModeProductionTest extends DatacoreSecurityModeDevTest {
   
   @Test
   public void ensureSecurityMode() {
      if (!"".equals(securitymode)) {
         Assert.fail("This test should be run with devmode=false");
      }
   }

}
