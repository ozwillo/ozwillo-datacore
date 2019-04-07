package org.oasis.datacore.rest.server;

import static org.oasis.datacore.sample.PublicPrivateOrganizationSample.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
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
import org.oasis.datacore.core.entity.EntityModelService;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryServiceImpl;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.core.security.mock.LocalAuthenticationService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.rest.client.QueryParameters;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.sample.PublicPrivateOrganizationSample;
import org.oasis.datacore.sample.ResourceModelIniter;
import org.oasis.datacore.server.uri.UriService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableMap;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })
//@FixMethodOrder(MethodSorters.NAME_ASCENDING) // else random since java 7 NOT REQUIRED ANYMORE
public class ProjectTest {

   @Autowired
   @Qualifier("datacoreApiCachedJsonClient")
   private /*DatacoreApi*/DatacoreCachedClient datacoreApiClient;
   
   @Autowired
   private LocalAuthenticationService authenticationService;
   
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
   /** for testing purpose */
   @Autowired
   private EntityModelService entityModelService;
   
   @Autowired
   private PublicPrivateOrganizationSample orgSample;
   
   
   @Before
   public void reset() {
      // cleanDataAndCache :
      orgSample.cleanDataOfCreatedModels(); // (was already called but this first cleans up data)
      datacoreApiClient.getCache().clear(); // to avoid side effects
   }
   
   @Test
   public void testMultiStorageProjectAllowsAltModels() throws MalformedURLException, URISyntaxException {
      String org2ModelUri = UriService.buildUri("dcmo:model_0", ORG2);
      
      LinkedHashSet<String> org2ModelUriProjectNames = modelServiceImpl.getForkedUriProjectNames(org2ModelUri);
      Assert.assertTrue(org2ModelUriProjectNames.contains("samples_org2.pri") && org2ModelUriProjectNames.size() == 1);

      LinkedHashSet<String> org2ModelUriInvisibleProjectNames = new SimpleRequestContextProvider<LinkedHashSet<String>>() {
         protected LinkedHashSet<String> executeInternal() throws MalformedURLException, URISyntaxException {
            return entityModelService.getForkedUriInvisibleProjectNames(org2ModelUri);
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DatacoreApi.PROJECT_HEADER, ORG2_PROJECT).build());
      Assert.assertEquals("no invisible project", org2ModelUriInvisibleProjectNames, null);
      LinkedHashSet<String> org2ModelUriVisibleProjectNames = new SimpleRequestContextProvider<LinkedHashSet<String>>() {
         protected LinkedHashSet<String> executeInternal() throws MalformedURLException, URISyntaxException {
            return entityModelService.getVisibleProjectNames(org2ModelUri);
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DatacoreApi.PROJECT_HEADER, ORG2_PROJECT).build());
      Assert.assertEquals("regular visible projects", org2ModelUriVisibleProjectNames,
            new LinkedHashSet<String>() {{ add("samples_org2"); add("oasis.meta"); }});
      DCResource org2Org2ModelResource = new SimpleRequestContextProvider<DCResource>() {
         protected DCResource executeInternal() throws MalformedURLException, URISyntaxException {
            return datacoreApiClient.getData(ResourceModelIniter.MODEL_MODEL_NAME, ORG2);
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DatacoreApi.PROJECT_HEADER, ORG2_PROJECT).build());
      Assert.assertNotNull(org2Org2ModelResource);
      
      LinkedHashSet<String> org2ModelPri2UriInvisibleProjectNames = new SimpleRequestContextProvider<LinkedHashSet<String>>() {
         protected LinkedHashSet<String> executeInternal() throws MalformedURLException, URISyntaxException {
            return entityModelService.getForkedUriInvisibleProjectNames(org2ModelUri);
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DatacoreApi.PROJECT_HEADER, ORGPRI2_PROJECT).build());
      Assert.assertEquals("invisible projects", org2ModelPri2UriInvisibleProjectNames,
            new LinkedHashSet<String>() {{ add("samples_org2"); add("oasis.meta"); }});
      LinkedHashSet<String> org2ModelUriPri2VisibleProjectNames = new SimpleRequestContextProvider<LinkedHashSet<String>>() {
         protected LinkedHashSet<String> executeInternal() throws MalformedURLException, URISyntaxException {
            return entityModelService.getVisibleProjectNames(org2ModelUri);
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DatacoreApi.PROJECT_HEADER, ORGPRI2_PROJECT).build());
      Assert.assertEquals("fork visible projects", org2ModelUriPri2VisibleProjectNames,
            new LinkedHashSet<String>() {{ add("samples_org2.pri"); }});
      DCResource orgpri2Org2ModelResource = new SimpleRequestContextProvider<DCResource>() {
         protected DCResource executeInternal() throws MalformedURLException, URISyntaxException {
            return datacoreApiClient.getData(ResourceModelIniter.MODEL_MODEL_NAME, ORG2);
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DatacoreApi.PROJECT_HEADER, ORGPRI2_PROJECT).build());
      Assert.assertNotNull(orgpri2Org2ModelResource);
      Assert.assertEquals("same uri", org2Org2ModelResource.getUri(), orgpri2Org2ModelResource.getUri());
      Assert.assertNotEquals("but not same resource (because belongs to different project in same "
            + "multiProjectStorage model", org2Org2ModelResource.get("dcmo:pointOfViewAbsoluteName"),
            orgpri2Org2ModelResource.get("dcmo:pointOfViewAbsoluteName"));
      ///Assert.assertNotEquals(org2Org2ModelResource.get("dcmo:mixins"), orgpri2Org2ModelResource.getUri());

      // same test on LDP query service :
      List<DCResource> org2Org2ModelResourceFound = new SimpleRequestContextProvider<List<DCResource>>() {
         protected List<DCResource> executeInternal() throws MalformedURLException, URISyntaxException {
            return datacoreApiClient.findDataInType(ResourceModelIniter.MODEL_MODEL_NAME,
                  new QueryParameters().add("dcmo:name", ORG2), null, 10);
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DatacoreApi.PROJECT_HEADER, ORG2_PROJECT).build());
      Assert.assertEquals(1, org2Org2ModelResourceFound.size());
      org2Org2ModelResource = org2Org2ModelResourceFound.get(0);
      List<DCResource> orgpri2Org2ModelResourceFound = new SimpleRequestContextProvider<List<DCResource>>() {
         protected List<DCResource> executeInternal() throws MalformedURLException, URISyntaxException {
            return datacoreApiClient.findDataInType(ResourceModelIniter.MODEL_MODEL_NAME,
                  new QueryParameters().add("dcmo:name", ORG2), null, 10);
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DatacoreApi.PROJECT_HEADER, ORGPRI2_PROJECT).build());
      Assert.assertEquals(2, orgpri2Org2ModelResourceFound.size());
      /* TODO make forkedUris work also in LDP query using aggregation...
      Assert.assertEquals(1, orgpri2Org2ModelResourceFound.size());
      orgpri2Org2ModelResource = orgpri2Org2ModelResourceFound.get(0);
      Assert.assertEquals("same uri", org2Org2ModelResource.getUri(), orgpri2Org2ModelResource.getUri());
      Assert.assertNotEquals("but not same resource (because belongs to different project in same "
            + "multiProjectStorage model", org2Org2ModelResource.get("dcmo:pointOfViewAbsoluteName"),
            orgpri2Org2ModelResource.get("dcmo:pointOfViewAbsoluteName"));
      ///Assert.assertNotEquals(org2Org2ModelResource.get("dcmo:mixins"), orgpri2Org2ModelResource.getUri());
      */
      // TODO same tests on write, delete...
   }

   @Test
   public void testMetamodelStorageModelContainsMetamodel() {
      DCModelBase metaModel = modelServiceImpl.getProject("oasis.meta").getModel("dcmo:model_0");
      DCModelBase mixinBackwardCompatibilityModel = modelServiceImpl.getStorageModel(metaModel);
      Assert.assertEquals("dcmi:mixin_0", mixinBackwardCompatibilityModel.getName());
      Assert.assertEquals(1, modelServiceImpl.getStoredModels(mixinBackwardCompatibilityModel).stream()
         .filter(m -> metaModel.getName().equals(m.getName())).count());
   }

   @Ignore // TODO
   @Test
   public void testOrg2() throws MalformedURLException, URISyntaxException {
      // TODO TODO set project(s)
      //SimpleRequestContextProvider.setSimpleRequestContext(new ImmutableMap.Builder<String, Object>()
      //      .put(DatacoreApi.PROJECT_HEADER, DCProject.OASIS_SAMPLE).build());
      
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
      // 0. pole numerique imports pub org (TODO Q or not, or also pri ???) TODO TODO import rights !
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
            ///.set("orgpri2:publishedVersion", lyceeJeanMacePri2.getVersion()) // TODO TODO patch
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

      // (collaboratively) creates producer private org in pri project TODO NOOOO rather already done & has rights on it
      DCResource dromeAgriCoopPri2 = DCResource.create(
            UriHelper.buildUri(containerUrl, ORGPRI2, "FR/Drome Agri Coop"))
            .set("org2:name", "Drôme Agri Coop")
            .set("org2:jurisdiction", "FR")
            .set("org2:regNumber", "92347366")
            .set("org2:activity", "444")
            //.set("orgpri2:publishedVersion", null)
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
      checkedDromeAgriCoopPri2.set("orgpri2:publishedVersion", checkedDromeAgriCoopPri2.getVersion()); // TODO LATER transaction
      DCResource publishedDromeAgriCoopPri2 = datacoreApiClient.postDataInType(checkedDromeAgriCoopPri2);
      // publishes an offer linked to public cantina org
      DCResource offer246bananes = DCResource.create(
            UriHelper.buildUri(containerUrl, AGRILOC_OFFER2, "5678"))
            .set("odisp:name", "46 bananes")
            .set("agriloco:type", "banane")
            .set("agriloco:amount", 46)
            //.set("agriloco:maxPrice", 19.99)
            .set("agriloco:answerDeadline", new DateTime())
            .set("agriloco:deliveryDeadline", new DateTime())
            .set("agriloco:requester", lyceeJeanMace2.getUri())
            .set("agriloco:state", "published");
      try { datacoreApiClient.deleteData(offer246bananes); } catch (NotFoundException nfex) {}
      DCResource postedOffer246bananes = datacoreApiClient.postDataInType(offer246bananes);
      authenticationService.logout();
      // (which actually has the same uri, but linked from model that sees public rather than private org)
      // 2. producer :
      // wants to answer mail-sent offer, which asks him to create an ozwillo account, where he has to choose his (public) org in the portal
      authenticationService.loginAs("jb");
      QueryParameters org2DromeAgriCoopParams = new QueryParameters().add("org2:name", "$regex\"Drome.*\"");
      List<DCResource> org2DromeAgriCoopRes = datacoreApiClient.findDataInType(ORG2,
            org2DromeAgriCoopParams, null, null);
      Assert.assertEquals(1, org2DromeAgriCoopRes.size());
      Assert.assertEquals(createdDromeAgriCoop2.getUri(), org2DromeAgriCoopRes.get(0).getUri());
      Assert.assertNotNull(datacoreApiClient.getData(createdDromeAgriCoop2));
      // he links it (using the portal) from its user profile, as a way to ask for ownership
      // TODO TODO portal gives him rights right away !!! (confirm "already orga" TODO bthuillier maquette)
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
      // NOOOOOOOOOOOOOOOOOO here also portal gives right (rather than CG)
      authenticationService.loginAs("ljm");
      QueryParameters lyceeJeanMace2OrgInboxParams = new QueryParameters().add("user2:organization",
            "$in[\"" + createdDromeAgriCoop2.getUri() + "\"]"); // in current orgs without their own owner TODO then with link to it ??
      List<DCResource> lyceeJeanMace2OrgInboxRes = datacoreApiClient.findDataInType(USER2,
            lyceeJeanMace2OrgInboxParams, null, null);
      Assert.assertEquals(1, lyceeJeanMace2OrgInboxRes.size());
      Assert.assertEquals(createdJeanBonUser.getUri(), lyceeJeanMace2OrgInboxRes.get(0).getUri());
      // and gives him rights
      // TODO using API
      authenticationService.logout();
      // 4. producer :
      // can now see and change some info in private org using portal BUT only on pub mixin perimeter
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
            jbCheckedDromeAgriCoopPri2.set("orgpri2:publishedVersion", jbCheckedDromeAgriCoopPri2.getVersion());
            jbCheckedDromeAgriCoopPri2.set("agrilocorg:proposedOffers", DCResource.listBuilder().add(offer246bananes.getUri()).build());
            // TODO TODO TODO visible to agrilocal but not public, nicest way is in scope agri !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
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
            // still sees private org, TODO and even has rights on it
            Assert.assertEquals(1, datacoreApiClient.findDataInType(ORGPRI2,
                  org2DromeAgriCoopParams, null, null));
            datacoreApiClient.postDataInType(publishedDromeAgriCoopPri2);
            
            // and can see provider's answer to offer
            QueryParameters lyceeJeanMace2OfferInboxParams = new QueryParameters()
                  .add("agrilocorg:proposedOffers", "$in[\"" + offer246bananes.getUri() + "\"]"); // in current offers
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
