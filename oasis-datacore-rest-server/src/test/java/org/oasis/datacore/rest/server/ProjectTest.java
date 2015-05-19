package org.oasis.datacore.rest.server;

import java.net.URI;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryServiceImpl;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.sample.PublicPrivateOrganizationSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })
//@FixMethodOrder(MethodSorters.NAME_ASCENDING) // else random since java 7 NOT REQUIRED ANYMORE
public class ProjectTest {

   @Autowired
   @Qualifier("datacoreApiCachedJsonClient")
   private /*DatacoreApi*/DatacoreCachedClient datacoreApiClient;
   
   /** to init models */
   @Autowired
   private /*static */DataModelServiceImpl modelServiceImpl;
   
   /** to be able to build a full uri, to check in tests
    * TODO rather client-side DCURI or rewrite uri in server */
   ///@Value("${datacoreApiClient.baseUrl}") 
   ///private String baseUrl; // useless
   @Value("${datacoreApiClient.containerUrl}") 
   private String containerUrlString;
   @Value("#{new java.net.URI('${datacoreApiClient.containerUrl}')}")
   //@Value("#{uriService.getContainerUrl()}")
   private URI containerUrl;

   /** for testing purpose */
   @Autowired
   @Qualifier("datacoreApiImpl") 
   private DatacoreApiImpl datacoreApiImpl;
   /** for testing purpose */
   @Autowired
   private LdpEntityQueryServiceImpl ldpEntityQueryServiceImpl;
   
   @Autowired
   private PublicPrivateOrganizationSample orgSample;
   
   
   @Before
   public void reset() {
      // cleanDataAndCache :
      orgSample.cleanDataOfCreatedModels(); // (was already called but this first cleans up data)
      datacoreApiClient.getCache().clear(); // to avoid side effects
      
      // resetDefaults :
      ldpEntityQueryServiceImpl.setMaxScan(0); // unlimited, default in test
   }
   
   @Test
   public void test() {
      // test models :
      DCProject orgpri2Project = modelServiceImpl.getProject(PublicPrivateOrganizationSample.ORGPRI2_PROJECT);
      Assert.assertNotNull(orgpri2Project);
      Assert.assertEquals(PublicPrivateOrganizationSample.ORGPRI2_PROJECT, orgpri2Project.getAbsoluteName());
      Assert.assertTrue(orgpri2Project.getLocalVisibleProjects().size() == 1
            && DCProject.OASIS_MAIN.equals(orgpri2Project.getLocalVisibleProjects().iterator().next().getName()));
      Assert.assertTrue(orgpri2Project.getLocalModels().size() > 1 // also dcmo:model_0 fork (TODO TODO OR dcmi:mixin_0 ??????????)
            && orgpri2Project.getLocalModels().stream().map(m -> m.getName()).collect(Collectors.toSet()).contains(PublicPrivateOrganizationSample.ORGPRI2));
      
      DCModelBase orgpri2Model = orgpri2Project.getModel(PublicPrivateOrganizationSample.ORGPRI2);
      Assert.assertNotNull(orgpri2Model);
      Assert.assertEquals(PublicPrivateOrganizationSample.ORGPRI2_PROJECT + "."
            + orgpri2Model.getName(), orgpri2Model.getAbsoluteName());
      
      DCProject mainProject = modelServiceImpl.getProject(DCProject.OASIS_MAIN);
      DCModelBase org2Model = mainProject.getModel(PublicPrivateOrganizationSample.ORG2);
      Assert.assertNotNull(org2Model);
      Assert.assertEquals(DCProject.OASIS_MAIN + "."
            + orgpri2Model.getName(), org2Model.getAbsoluteName());
      
      //Assert.assertNull("Should not be visible from oasis.main project",
      //      modelServiceImpl.getModel(PublicPrivateOrganizationSample.ORG2));
      
      // test workflow of example use of org2  :
      
      
      // test adding a field and checking for changes & indexes in another project :
      
   }
   
}
