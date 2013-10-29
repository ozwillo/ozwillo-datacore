package org.oasis.datacore.core.entity;

import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.core.entity.DCEntityService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * 
 * @author mdutoo
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-core-context.xml" })
@FixMethodOrder // else random since java 7
public class DatacoreEntityTest {
   
   @Autowired
   private DCEntityService dcEntityService;
   

   @Test
   public void testSampleData() {
      DCEntity sampleEntity = dcEntityService.getSampleData();
      Map<String, Object> props = sampleEntity.getProperties();
      Assert.assertEquals("http://data.oasis-eu.org/sample/1", sampleEntity.getUri());
      Assert.assertEquals("some text", props.get("string"));
      Assert.assertEquals(2, props.get("int"));
      Assert.assertEquals(true, props.get("boolean"));
      Assert.assertTrue(props.get("date") instanceof DateTime); // TODO Date
      System.out.println("sample data:\n" + sampleEntity);
   }

}
