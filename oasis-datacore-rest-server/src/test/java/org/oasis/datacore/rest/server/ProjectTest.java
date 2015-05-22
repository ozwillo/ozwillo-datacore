package org.oasis.datacore.rest.server;

import static org.oasis.datacore.sample.PublicPrivateOrganizationSample.AGRILOC_OFFER2;
import static org.oasis.datacore.sample.PublicPrivateOrganizationSample.ORG2;
import static org.oasis.datacore.sample.PublicPrivateOrganizationSample.ORGPRI2;
import static org.oasis.datacore.sample.PublicPrivateOrganizationSample.ORGPRI2_PROJECT;
import static org.oasis.datacore.sample.PublicPrivateOrganizationSample.USER2;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryServiceImpl;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.core.security.mock.MockAuthenticationService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.rest.client.QueryParameters;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.sample.PublicPrivateOrganizationSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableMap;

@Ignore // TODO
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })
//@FixMethodOrder(MethodSorters.NAME_ASCENDING) // else random since java 7 NOT REQUIRED ANYMORE
public class ProjectTest {

   @Autowired
   @Qualifier("datacoreApiCachedJsonClient")
   private /*DatacoreApi*/DatacoreCachedClient datacoreApiClient;
   
   @Autowired
   private MockAuthenticationService authenticationService;
   
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
   private ResourceService resourceService;
   
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
   public void test() throws MalformedURLException, URISyntaxException {
      // test models :
      DCProject orgpri2Project = modelServiceImpl.getProject(ORGPRI2_PROJECT);
      Assert.assertNotNull(orgpri2Project);
      Assert.assertEquals(ORGPRI2_PROJECT, orgpri2Project.getAbsoluteName());
      Assert.assertTrue(orgpri2Project.getLocalVisibleProjects().size() == 1
            && DCProject.OASIS_MAIN.equals(orgpri2Project.getLocalVisibleProjects().iterator().next().getName()));
      Assert.assertTrue(orgpri2Project.getLocalModels().size() > 1 // also dcmo:model_0 fork (TODO TODO OR dcmi:mixin_0 ??????????)
            && orgpri2Project.getLocalModels().stream().map(m -> m.getName()).collect(Collectors.toSet()).contains(ORGPRI2));
      
      DCModelBase orgpri2Model = orgpri2Project.getModel(ORGPRI2);
      Assert.assertNotNull(orgpri2Model);
      Assert.assertEquals(ORGPRI2_PROJECT + "."
            + orgpri2Model.getName(), orgpri2Model.getAbsoluteName());
      
      DCProject mainProject = modelServiceImpl.getProject(DCProject.OASIS_MAIN);
      DCModelBase org2Model = mainProject.getModel(ORG2);
      Assert.assertNotNull(org2Model);
      Assert.assertEquals(DCProject.OASIS_MAIN + "."
            + orgpri2Model.getName(), org2Model.getAbsoluteName());
      
      //Assert.assertNull("Should not be visible from oasis.main project",
      //      modelServiceImpl.getModel(ORG2));
      
      // test workflow of example use of org2  :
      // 0. pole numerique imports pub org (TODO Q or not, or also pri ???)
      authenticationService.loginAs("pn");
      DCResource lyceeJeanMace2 = DCResource.create(
            UriHelper.buildUri(containerUrl, ORG2, "FR/Lycee Jean Mace"))
            .set("org2:name", "Lycée Jean Macé")
            .set("org2:jurisdiction", "FR")
            .set("org2:regNumber", "82347366")
            .set("org2:activity", "333");
      try { datacoreApiClient.deleteData(lyceeJeanMace2); } catch (NotFoundException nfex) {}
      lyceeJeanMace2 = datacoreApiClient.postDataInType(lyceeJeanMace2);
      DCResource lyceeJeanMacePri2 = DCResource.create(
            UriHelper.buildUri(containerUrl, ORGPRI2, "FR/Lycee Jean Mace"))
            .set("org2:name", "Lycée Jean Macé")
            .set("org2:jurisdiction", "FR")
            .set("org2:regNumber", "82347366")
            .set("org2:activity", "333")
            .set("orgpri2:publishedVersion", lyceeJeanMace2.getVersion())
            .set("orgpri2:state", "approved")
            .set("orgpri2:employeeNb", 104);
      try { datacoreApiClient.deleteData(lyceeJeanMacePri2); } catch (NotFoundException nfex) {}
      lyceeJeanMacePri2 = datacoreApiClient.postDataInType(lyceeJeanMacePri2);
      authenticationService.logout();
      // TODO TODO how does PN know to who / when give their ownership ? through the portal ??????????????????????????
      
      // 1. a cantina, through agrilocal app :
      authenticationService.loginAs("ljm");
      DCResource checkedDromeAgriCoopPri2 = new SimpleRequestContextProvider<DCResource>() {
         protected DCResource executeInternal() throws MalformedURLException, URISyntaxException {

      // collaboratively creates producer private org in pri project
      DCResource dromeAgriCoopPri2 = DCResource.create(
            UriHelper.buildUri(containerUrl, ORGPRI2, "FR/Drome Agri Coop"))
            .set("org2:name", "Drôme Agri Coop")
            .set("org2:jurisdiction", "FR")
            .set("org2:regNumber", "92347366")
            .set("org2:activity", "444")
            .set("orgpri2:publishedVersion", null)
            .set("orgpri2:state", "changed") // or "new", null
            .set("orgpri2:employeeNb", 96);
      try { datacoreApiClient.deleteData(dromeAgriCoopPri2); } catch (NotFoundException nfex) {}
      DCResource createdDromeAgriCoopPri2 = datacoreApiClient.postDataInType(dromeAgriCoopPri2);
      Assert.assertNotNull(createdDromeAgriCoopPri2);
      // fills it some more
      createdDromeAgriCoopPri2.set("orgpri2:employeeNb", 97);
      createdDromeAgriCoopPri2.set("orgpri2:state", "checked"); // or complete, valid
      DCResource checkedDromeAgriCoopPri2 = datacoreApiClient.postDataInType(createdDromeAgriCoopPri2);
      return checkedDromeAgriCoopPri2;
      
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DatacoreApi.PROJECT_HEADER, ORGPRI2_PROJECT).build());
      // but not even itself can see it from the pub (main) project
      try {
         datacoreApiClient.getData(checkedDromeAgriCoopPri2);
         Assert.fail("Should not be able to see " + checkedDromeAgriCoopPri2.getUri());
      } catch (NotFoundException nfex) {
      }
      authenticationService.logout();
      // and nobody else (not even admins) can see it in the pri project
      authenticationService.loginAs("admin");
      new SimpleRequestContextProvider<DCResource>() {
         protected DCResource executeInternal() {
            try {
               datacoreApiClient.getData(checkedDromeAgriCoopPri2);
               Assert.fail("Should not be able to see " + checkedDromeAgriCoopPri2.getUri());
            } catch (NotFoundException nfex) {
            }
            return null;
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DatacoreApi.PROJECT_HEADER, ORGPRI2_PROJECT).build());
      authenticationService.logout();
      // decides to publish it to producer public org (or immediate publication ??)
      // i.e. creates a pub org resource : same uri but pub project & copy pri org on the perimeter of pub org mixin
      authenticationService.loginAs("ljm");
      DCResource dromeAgriCoop2 = copy(checkedDromeAgriCoopPri2, modelServiceImpl.getModelBase(ORG2));
      try { datacoreApiClient.deleteData(dromeAgriCoop2); } catch (NotFoundException nfex) {}
      DCResource createdDromeAgriCoop2 = datacoreApiClient.postDataInType(dromeAgriCoop2);
      checkedDromeAgriCoopPri2.set("orgpri2:state", "approved"); // or "published"
      checkedDromeAgriCoopPri2.set("orgpri2:publishedVersion", createdDromeAgriCoop2.getVersion()); // TODO LATER transaction
      DCResource publishedDromeAgriCoopPri2 = datacoreApiClient.postDataInType(checkedDromeAgriCoopPri2);
      // publishes an offer linked to public cantina org
      DCResource offer246bananes = DCResource.create(
            UriHelper.buildUri(containerUrl, AGRILOC_OFFER2, "5678"))
            .set("odisp:name", "46 bananes")
            .set("agriloco:type", "banane")
            .set("agriloco:amount", 46)
            .set("agriloco:maxPrice", 19.99)
            .set("agriloco:deadline", new DateTime())
            .set("agriloco:requester", lyceeJeanMace2.getUri());
      try { datacoreApiClient.deleteData(offer246bananes); } catch (NotFoundException nfex) {}
      DCResource postedOffer246bananes = datacoreApiClient.postDataInType(offer246bananes);
      authenticationService.logout();
      // (which actually has the same uri, but linked from model that sees public rather than private org)
      // 2. producer :
      // answers offer, which asks him to create an ozwillo account, where he has to choose his (public) org in the portal
      authenticationService.loginAs("jb");
      QueryParameters org2DromeAgriCoopParams = new QueryParameters().add("org2:name", "$regex\"Drome.*\"");
      List<DCResource> org2DromeAgriCoopRes = datacoreApiClient.findDataInType(ORG2,
            org2DromeAgriCoopParams, null, null);
      Assert.assertEquals(1, org2DromeAgriCoopRes.size());
      Assert.assertEquals(createdDromeAgriCoop2.getUri(), org2DromeAgriCoopRes.get(0).getUri());
      Assert.assertNotNull(datacoreApiClient.getData(createdDromeAgriCoop2));
      // he links it from its profile, as a way to ask for ownership
      DCResource jeanBonUser = DCResource.create(UriHelper.buildUri(containerUrl, USER2, "4678"))
            .set("odisp:name", "Jean Bon")
            .set("user2:country", "FR")
            .set("user2:nationalId", "4678")
            .set("user2:firstName", "Jean")
            .set("user2:familyName", "Bon")
            .set("user2:birthDate", new DateTime())
            .set("user2:organization", org2DromeAgriCoopRes.get(0).getUri());
      try { datacoreApiClient.deleteData(jeanBonUser); } catch (NotFoundException nfex) {}
      DCResource createdJeanBonUser = datacoreApiClient.postDataInType(jeanBonUser);
      // BUT can't change said org data
      try {
         datacoreApiClient.postDataInType(createdDromeAgriCoop2);
         Assert.fail("Should not be able to change " + createdDromeAgriCoop2.getUri());
      } catch (Exception fex) {
      }
      // and can't even see private org
      new SimpleRequestContextProvider<DCResource>() {
         protected DCResource executeInternal() {
            Assert.assertEquals(0, datacoreApiClient.findDataInType(ORGPRI2,
                  org2DromeAgriCoopParams, null, null));
            try {
               datacoreApiClient.getData(publishedDromeAgriCoopPri2);
               Assert.fail("Should not be able to see " + publishedDromeAgriCoopPri2.getUri());
            } catch (NotFoundException nfex) {
            }
            return null;
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DatacoreApi.PROJECT_HEADER, ORGPRI2_PROJECT).build());
      authenticationService.logout();
      // 3. cantina, using agrilocal app :
      // sees the producer in its "public org chosen, asking rights on private org" inbox
      // i.e. "users linking to an organization among public org stil owned by cantina"
      authenticationService.loginAs("ljm");
      QueryParameters lyceeJeanMace2OrgInboxParams = new QueryParameters().add("user2:organization", createdDromeAgriCoop2.getUri());
      List<DCResource> lyceeJeanMace2OrgInboxRes = datacoreApiClient.findDataInType(USER2,
            lyceeJeanMace2OrgInboxParams, null, null);
      Assert.assertEquals(1, lyceeJeanMace2OrgInboxRes.size());
      Assert.assertEquals(createdJeanBonUser.getUri(), lyceeJeanMace2OrgInboxRes.get(0).getUri());
      // and gives him rights
      // TODO using API
      authenticationService.logout();
      // 4. producer :
      // can now see and change some info in private org using portal
      // (TODO Q or agrilocal ?), including answering to offer by linking to it, which at once updates public org
      authenticationService.loginAs("jb");
      DCResource jbPublishedDromeAgriCoopPri2 = new SimpleRequestContextProvider<DCResource>() {
         protected DCResource executeInternal() throws MalformedURLException,
               IllegalArgumentException, URISyntaxException, RuntimeException {
            List<DCResource> jbDromeAgriCoopPri2Res = datacoreApiClient.findDataInType(ORGPRI2,
                  org2DromeAgriCoopParams, null, null);
            Assert.assertEquals(1, jbDromeAgriCoopPri2Res.size());
            DCResource jbDromeAgriCoopPri2 = jbDromeAgriCoopPri2Res.get(0);
            jbDromeAgriCoopPri2.set("orgpri2:employeeNb", 106);
            jbDromeAgriCoopPri2.set("orgpri2:state", "checked"); // i.e. "correct (but changes to be published)" ; or only "changed" for now, or "published" at once
            DCResource jbCheckedDromeAgriCoopPri2 = datacoreApiClient.postDataInType(jbDromeAgriCoopPri2);
            // publishing :
            DCResource jbDromeAgriCoop2 = copy(jbCheckedDromeAgriCoopPri2,
                  datacoreApiClient.getData(jbCheckedDromeAgriCoopPri2), modelServiceImpl.getModelBase(ORG2));
            DCResource jbPublishedDromeAgriCoop2 = datacoreApiClient.postDataInType(jbDromeAgriCoop2);
            Assert.assertEquals(106, jbPublishedDromeAgriCoop2.get("orgpri2:employeeNb"));
            jbCheckedDromeAgriCoopPri2.set("orgpri2:state", "published");
            jbCheckedDromeAgriCoopPri2.set("orgpri2:publishedVersion", jbPublishedDromeAgriCoop2.getVersion());
            jbCheckedDromeAgriCoopPri2.set("agrilocorg:acceptedOffers", DCResource.listBuilder().add(offer246bananes.getUri()).build());
            DCResource jbPublishedDromeAgriCoopPri2 = datacoreApiClient.postDataInType(jbCheckedDromeAgriCoopPri2);
            Assert.assertEquals(jbPublishedDromeAgriCoop2.getVersion(), jbPublishedDromeAgriCoopPri2.get("orgpri2:publishedVersion"));
            return jbPublishedDromeAgriCoopPri2;
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DatacoreApi.PROJECT_HEADER, ORGPRI2_PROJECT).build());
      authenticationService.logout();
      // 5. cantina :
      authenticationService.loginAs("ljm");
      new SimpleRequestContextProvider<DCResource>() {
         protected DCResource executeInternal() {
            // now can't see private org anymore
            Assert.assertEquals(0, datacoreApiClient.findDataInType(ORGPRI2,
                  org2DromeAgriCoopParams, null, null));
            try {
               datacoreApiClient.getData(publishedDromeAgriCoopPri2);
               Assert.fail("Should not be able to see " + publishedDromeAgriCoopPri2.getUri());
            } catch (NotFoundException nfex) {
            }
            // but can see provider's answer to offer
            QueryParameters lyceeJeanMace2OfferInboxParams = new QueryParameters()
                  .add("agrilocorg:acceptedOffers", offer246bananes.getUri());
            List<DCResource> lyceeJeanMace2OfferInboxRes = datacoreApiClient.findDataInType(USER2,
                  lyceeJeanMace2OfferInboxParams, null, null);
            Assert.assertEquals(1, lyceeJeanMace2OfferInboxRes.size());
            Assert.assertEquals(jbPublishedDromeAgriCoopPri2.getUri(), lyceeJeanMace2OfferInboxRes.get(0).getUri());
            // and accepts offer by updating it (TODO Q showing accepted provider or not ??)
            offer246bananes.set("agriloco:state", "accepted"); // or boolean agriloco:state, or agriloco:provider link
            DCResource acceptedOffer246bananes = datacoreApiClient.postDataInType(offer246bananes);
            return acceptedOffer246bananes;
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DatacoreApi.PROJECT_HEADER, ORGPRI2_PROJECT).build());
      authenticationService.logout();
      
      // test adding a field and checking for changes & indexes in another project :
      // (in same project, done in ResourceModelTest.testImpactedModelAndIndexUpdate())
      
   }

   public static DCResource copy(DCResource source, DCModelBase... mixins)
         throws MalformedURLException, URISyntaxException, RuntimeException {
      return copy(source, null, mixins);
   }
   public static DCResource copy(DCResource source, DCResource target, DCModelBase... mixins)
         throws MalformedURLException, URISyntaxException, RuntimeException {
      if (target == null) {
         target = DCResource.create(source.getUri());
      } else if (source.getUri() == null || !source.getUri().equals(target.getUri())) {
         throw new RuntimeException("not same URIs");
      }
      target.setTypes(source.getTypes());
      target.setVersion(source.getVersion());
      // NB. dc fields are not required
      for (DCModelBase mixin : mixins) {
         for (DCField field : mixin.getGlobalFields()) {
            target.set(field.getName(), source.get(field.getName()));
         }
      }
      return target;
   }
   
}
