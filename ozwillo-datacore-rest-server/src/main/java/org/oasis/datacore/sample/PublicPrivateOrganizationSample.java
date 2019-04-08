package org.oasis.datacore.sample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.joda.time.DateTime;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.springframework.stereotype.Component;


/**
 * Draft of Public & Private Organization Models, to help test copy to one from the other.
 * 
 * @author mdutoo
 *
 */
@Component
public class PublicPrivateOrganizationSample extends DatacoreSampleMethodologyBase {
   
   public static final String ORG1 = "org1:Organization_0";
   public static final String ORG1_PROJECT = "samples_org1";
   public static final String ORGPRI1 = "orgpri1:Organization_0";
   public static final String ORG2 = "org2:Organization_0";
   public static final String ORG2_PROJECT = "samples_org2";
   public static final String ORGPRI2 = ORG2; /// "orgpri2:Organization_0";
   private static final String ORGPRI2_MIXIN = "orgpri2:Organization_0";
   public static final String ORGPRI2_PROJECT = ORG2_PROJECT + ".pri";
   public static final String AGRILOC_OFFER2 = "agriloco2:Offer_0";
   public static final String USER2 = "user2:User_0";
   public static final String ORG3 = "org3:Organization_0";
   public static final String ORG3_PROJECT = "samples_org3";
   public static final String ORGPRI3 = "orgpri3:Organization_0"; // ORG3; /// "orgpri2:Organization_0";
   ///public static final String ORGPRI3_PROJECT = "samples_orgpri3"; // transparent, only to give project scope
   public static final String AGRILOC_OFFER3 = "agriloco3:Offer_0";
   public static final String USER3 = "user3:User_0";
   public static final String ORG3_SANDBOX_PROJECT = ORG3_PROJECT + ".sandbox";
   

   @Override
   public void doOSelectPerimeter() {
      
   }
   

   // see doc in overriden
   @Override
   public void do1DesignFlatModel() {
   }


   // see doc in overriden
   @Override
   public void do2ExternalizeModelAsMixins() {
   }

   // see doc in overriden
   @Override
   public void do2CreateModels(List<DCModelBase> modelsToCreate) {

      DCProject sampleMainProject = modelAdminService.getProject(DCProject.OASIS_SAMPLE);
      DCProject metamodelProject = modelAdminService.getProject(DCProject.OASIS_META);

      // org(pri)1 : orgpri (reference) and inherits org (else can't query on both pub & pri fields) and links org, both storage

      // first create the project
      DCProject org1Project = new DCProject(ORG1_PROJECT);
      org1Project.addLocalVisibleProject(metamodelProject); // else metamodel not visible
      sampleMainProject.addLocalVisibleProject(org1Project); // show among main samples
      ///org1.setPointOfViewNames(Arrays.asList(mainProject.getName()));
      modelAdminService.addProject(org1Project);
      ///mainProject.addLocalVisibleProject(org1Project); // ... rather this i.e. defaults !
      // only then add model (else dep models not visible)
      
      DCModelBase org1 = new DCModel(ORG1, ORG1_PROJECT)
         .addMixin(modelAdminService.getModelBase("odisp:Displayable_0")) // TODO ??
         .addField(new DCField("org1:jurisdiction", "string", true, 100)) // TODO actually geoco, in oasis.main or visible geo.main
         .addField(new DCField("org1:regNumber", "string", true, 100))
         .addField(new DCField("org1:activity", "string", true, 100)) // actually orgact
         .addField(new DCField("org1:name", "string", true, 100));
      org1.setDocumentation("see xls : id = org:jurisdiction/org:name");
      org1.setStorage(true);
      org1.setInstanciable(true);
      
      DCModelBase orgpri1 = new DCModel(ORGPRI1, ORG1_PROJECT)
         .addMixin(org1)
         // fields to help publishing to pub org :
         .addField(new DCResourceField("orgpri1:org", org1.getName(), false, 100)) // link to pub org
         .addField(new DCField("orgpri1:publishedVersion", "long", false, 0)) // version of orgpri last copied to pub org
         .addField(new DCField("orgpri1:state", "string", true, 100)) // required for queries, TODO Q field alias of o:state ? ; "published" or "modified" (or "new", "deleted" ??)
         // orgpri specific business fields :
         .addField(new DCField("orgpri1:employeeNb", "int", false, 0)); // or any other private field, TODO Q which ones ??
      orgpri1.setDocumentation("see xls : id = org:jurisdiction/org:name");
      orgpri1.setStorage(true);
      orgpri1.setInstanciable(true);
      
      /*datacoreApiClient.*//*postDataInType(Arrays.asList(org1, orgpri1).stream().map(
            m -> mrMappingService.modelToResource(model, null)).collect(Collectors.toList()));*/
         
      
      // org(pri)2 : idem but same name and orgpub hides orgpri when accessed from pov or "front" project
      // (orgpri could be in "back" project visible from oasis.main but then its deps ex. geo must be made visible
      // from their own project, and beware when upgrading them to new versions)
      
      // first create the project
      DCProject org2Project = new DCProject(ORG2_PROJECT);
      org2Project.addLocalVisibleProject(metamodelProject); // else metamodel not visible
      sampleMainProject.addLocalVisibleProject(org2Project); // show among main samples
      ///org2.setPointOfViewNames(Arrays.asList(mainProject.getName()));
      modelAdminService.addProject(org2Project);
      ///mainProject.addLocalVisibleProject(org2Project); // ... rather this i.e. defaults !
      // only then add model (else dep models not visible)
      
      DCModelBase org2 = new DCModel(ORG2, ORG2_PROJECT)
         .addMixin(modelAdminService.getModelBase("odisp:Displayable_0")) // TODO ??
         .addField(new DCField("org2:jurisdiction", "string", true, 100)) // TODO actually geoco, in oasis.main or visible geo.main
         .addField(new DCField("org2:regNumber", "string", true, 100))
         .addField(new DCField("org2:activity", "string", true, 100)) // actually orgact
         .addField(new DCField("org2:name", "string", true, 100)); // NB. not field alias odisp:name because also contains geo...
      org2.setDocumentation("see xls : id = org:jurisdiction/org:name");
      org2.setStorage(true);
      org2.setInstanciable(true);
      org2Project.addLocalModel(org2); // TODO TODO TODO else error On model orgpri2:Organization_0 in project orgpri2: Unable to add model to project orgpri2 with visible projects {}, this model has unknown mixins : [org2:Organization_0]
      
      DCModelBase offer2 = new DCModel(AGRILOC_OFFER2, ORG2_PROJECT) // TODO or in orgpri2 project ??
         .addMixin(modelAdminService.getModelBase("odisp:Displayable_0")) // TODO ??
         .addField(new DCField("agriloco2:id", "string", true, 100))
         .addField(new DCField("agriloco2:type", "string", true, 100)) // TODO LATER rather resource refMixin
         .addField(new DCField("agriloco2:amount", "float", false, 0)) // also float for ex. weight, TODO Q required ??
         //.addField(new DCField("agriloco2:maxPrice", "float", true, queryLimitIfNotDev)) // not in actual model
         //.addField(new DCField("agriloco2:publishedDate", "date", true, queryLimitIfNotDev)) // for now rather dc:created
         .addField(new DCField("agriloco2:answerDeadline", "date", true, 100))
         .addField(new DCField("agriloco2:deliveryDeadline", "date", true, 100))
         .addField(new DCResourceField("agriloco2:requester", ORG2, true, 100))
         .addField(new DCField("agriloco2:state", "string", true, 100)); // required for queries, TODO Q field alias of o:state ? ; "new" or "published", also "accepted" 
         //.addField(new DCResourceField("agriloco2:provider", ORG2, false, 100)); // TODO Q NO because would be publicly visible ?
      offer2.setDocumentation("see xls : id = opaque");
      offer2.setStorage(true);
      offer2.setInstanciable(true);

      // TODO would need dynamic & available mixins
      DCModelBase agrilocorgpri2 = new DCMixin("agrilocorg2:Organization_0", ORGPRI2_PROJECT) // NB. in another project !
         .addField(new DCListField("agrilocorg2:proposedOffers", // TODO TODO TODO TODO not visible to cantina !!!!!!!!!!
               new DCResourceField("useless", offer2.getName(), true, 100), true));
      agrilocorgpri2.setDocumentation("see xls");
      agrilocorgpri2.setStorage(false);
      agrilocorgpri2.setInstanciable(false);
      

      DCModelBase orgpri2mixin = new DCModel(ORGPRI2_MIXIN, ORGPRI2_PROJECT) // to allow separate orgpri2: prefix
         // fields to help publishing to pub org :
         ///.addField(new DCResourceField("orgpri2:org", org2.getName(), false, 100)) // link to pub org NOOO ARE THE SAME
         .addField(new DCField("orgpri2:publishedVersion", "long", false, 0)) // version of orgpri last copied to pub org
         .addField(new DCField("orgpri2:state", "string", true, 100)) // required for queries, TODO Q field alias of o:state ? ; "published" or "modified" (or "new", "deleted" ??)
         // orgpri specific business fields :
         .addField(new DCField("orgpri2:employeeNb", "int", false, 0)); // or any other private field, TODO Q which ones ??
      orgpri2mixin.setDocumentation("pure mixin to add on org2 in org2.pri project, to allow separate orgpri2: prefix");
      orgpri2mixin.setStorage(false);
      orgpri2mixin.setInstanciable(false);
      
      DCModelBase orgpri2 = new DCModel(ORGPRI2, ORGPRI2_PROJECT) // NB. in another project !
         .addMixin(org2)
         .addMixin(orgpri2mixin)
         .addMixin(agrilocorgpri2); // TODO TODO rather available & dynamic mixins !!! + on orgpri allow ANY mixins but on org only available ones !!!
      orgpri2.setDocumentation("see xls : id = org:jurisdiction/org:name");
      orgpri2.setStorage(true);
      orgpri2.setInstanciable(true);
      
      DCModelBase user2 = new DCModel(USER2, ORG2_PROJECT)
         .addMixin(modelAdminService.getModelBase("odisp:Displayable_0")) // TODO ??
         .addField(new DCField("user2:country", "string", true, 100)) // TODO LATER rather resource refMixin
         .addField(new DCField("user2:nationalId", "string", true, 100)) // ex. fr SSN ; TODO Q or user2:ssn and field alias nationaId ?
         .addField(new DCField("user2:firstName", "string", true, 0))
         .addField(new DCField("user2:familyName", "string", true, 0))
         .addField(new DCField("user2:birthDate", "date", true, 100))
         .addField(new DCResourceField("user2:organization", ORG2, true, 100)); // TODO list ?!
      user2.setDocumentation("see xls : id = opaque");
      user2.setStorage(true);
      user2.setInstanciable(true);

      // ori (or fork) : first create the project
      DCProject orgpri2Project = new DCProject(ORGPRI2_PROJECT);
      orgpri2Project.addLocalVisibleProject(org2Project); // mainProject // means inherits its visible projects
      ///orgpri2.setPointOfViewNames(Arrays.asList(mainProject.getName()));
      modelAdminService.addProject(orgpri2Project);
      // only then add model (else dep models not visible)
      /*DCModelBase orgpri2storageMetamodel = this.fork(
            modelAdminService.getModelBase(ResourceModelIniter.MODEL_MODEL_NAME), // TODO TODO NO NEED TO FORK META
            orgpri2Project, false, false, true, false);*/
      modelAdminService.addLocalModel(orgpri2Project, orgpri2mixin); // else not visible to orgpri2 when adding it
      modelAdminService.addLocalModel(orgpri2Project, agrilocorgpri2); // else not visible to orgpri2 when adding it
      modelAdminService.addLocalModel(orgpri2Project, orgpri2, true); // NOOOOOOOOOOOOOOOOOOOOOO or at least in orgpri2.Security.requiredProject/Scope
      ///modelAdminService.addForkedUri(orgpri2Project, UriHelper.buildUri(containerUrl,
      ///      ResourceModelIniter.MODEL_NAME_PROP, ORG2)); // allowing fork of org2:Organization_0
      
      
      // org3 : org is model-visible (to authentified users since pub) and has the orgpri mixin which
      // has its own DCSecurity (NOT model-visible to authentified etc.) and uses the Resource-level
      // rights for itself to achieve "personal data" behaviour
      // REQUIRES to decouple "required" i.e. validation per mixin OR dynamic mixins (else not yet pri org incomplete because already has pri mixin) 

      // first create the project
      DCProject org3Project = new DCProject(ORG3_PROJECT);
      org3Project.addLocalVisibleProject(metamodelProject); // else metamodel not visible
      sampleMainProject.addLocalVisibleProject(org3Project); // show among main samples
      ///org3.setPointOfViewNames(Arrays.asList(mainProject.getName()));
      modelAdminService.addProject(org3Project);
      ///mainProject.addLocalVisibleProject(org3Project); // ... rather this i.e. defaults !
      // only then add model (else dep models not visible)
      
      DCModelBase offer3 = new DCModel(AGRILOC_OFFER3, ORG3_PROJECT) // TODO or in orgpri2 project ??
         .addMixin(modelAdminService.getModelBase("odisp:Displayable_0")) // TODO ??
         .addField(new DCField("agriloco3:id", "string", true, 100))
         .addField(new DCField("agriloco3:type", "string", true, 100)) // TODO LATER rather resource refMixin
         .addField(new DCField("agriloco3:amount", "float", false, 0)) // also float for ex. weight, TODO Q required ??
         //.addField(new DCField("agriloco3:maxPrice", "float", true, queryLimitIfNotDev)) // not in actual model
         //.addField(new DCField("agriloco3:publishedDate", "date", true, queryLimitIfNotDev)) // for now rather dc:created
         .addField(new DCField("agriloco3:answerDeadline", "date", true, 100))
         .addField(new DCField("agriloco3:deliveryDeadline", "date", true, 100))
         .addField(new DCResourceField("agriloco3:requester", ORG3, true, 100))
         .addField(new DCField("agriloco3:state", "string", true, 100)); // required for queries, TODO Q field alias of o:state ? ; "new" or "published", also "accepted" 
         //.addField(new DCResourceField("agriloco3:provider", ORG2, false, queryLimitIfNotDev)); // TODO Q NO because would be publicly visible ?
      offer3.setDocumentation("see xls : id = opaque");
      offer3.setStorage(true);
      offer3.setInstanciable(true);

      // TODO would need dynamic & available mixins
      DCModelBase agrilocorgpri3 = new DCMixin("agrilocorg3:Organization_0", ORG3_PROJECT) // in same project !
         .addField(new DCListField("agrilocorg3:proposedOffers", // TODO TODO TODO TODO not visible to cantina !!!!!!!!!!
               new DCResourceField("useless", offer3.getName(), true, 100), false)); // CAN'T BE REQUIRED since already on not yet pri org
      agrilocorgpri3.setDocumentation("see xls");
      agrilocorgpri3.setStorage(false);
      agrilocorgpri3.setInstanciable(false);
      
      DCModelBase orgpri3 = new DCModel(ORGPRI3, ORG3_PROJECT) // in same project
         .addMixin(agrilocorgpri3) // TODO TODO rather available & dynamic mixins !!! + on orgpri allow ANY mixins but on org only available ones !!!
         // fields to help publishing to pub org :
         ///.addField(new DCResourceField("orgpri2:org", org2.getName(), false, 100)) // link to pub org NOOO ARE THE SAME
         .addField(new DCField("orgpri3:publishedVersion", "long", false, 0)) // version of orgpri last copied to pub org
         .addField(new DCField("orgpri3:state", "string", false, 100)) // CAN'T BE REQUIRED since already on not yet pri org ; required for queries, TODO Q field alias of o:state ? ; "published" or "modified" (or "new", "deleted" ??)
         // orgpri specific business fields :
         .addField(new DCField("orgpri3:employeeNb", "int", false, 0)); // or any other private field, TODO Q which ones ??
      orgpri3.setDocumentation("see xls : id = org:jurisdiction/org:name");

      DCModelBase org3 = new DCModel(ORG3, ORG3_PROJECT)
         .addMixin(modelAdminService.getModelBase("odisp:Displayable_0")) // TODO ??
         .addMixin(orgpri3)
         .addField(new DCField("org3:jurisdiction", "string", true, 100)) // TODO actually geoco, in oasis.main or visible geo.main
         .addField(new DCField("org3:regNumber", "string", true, 100))
         .addField(new DCField("org3:activity", "string", true, 100)) // actually orgact
         .addField(new DCField("org3:name", "string", true, 100)); // NB. not field alias odisp:name because also contains geo...
      org3.setDocumentation("see xls : id = org:jurisdiction/org:name");
      org3.setStorage(true);
      org3.setInstanciable(true);
      
      /*// ori (or fork) : first create the project
      DCProject orgpri3Project = new DCProject(ORGPRI3_PROJECT); // transparent, only to give project scope
      orgpri3Project.addLocalVisibleProject(org3Project); // mainProject // means inherits its visible projects
      ///orgpri3.setPointOfViewNames(Arrays.asList(mainProject.getName()));
      modelAdminService.addProject(orgpri3Project);*/
      
      DCModelBase user3 = new DCModel(USER3, ORG3_PROJECT)
         .addMixin(modelAdminService.getModelBase("odisp:Displayable_0")) // TODO ??
         .addField(new DCField("user3:country", "string", true, 100)) // TODO LATER rather resource refMixin
         .addField(new DCField("user3:nationalId", "string", true, 100)) // ex. fr SSN ; TODO Q or user2:ssn and field alias nationaId ?
         .addField(new DCField("user3:firstName", "string", true, 0))
         .addField(new DCField("user3:familyName", "string", true, 0))
         .addField(new DCField("user3:birthDate", "date", true, 100))
         .addField(new DCResourceField("user3:organization", ORG3, true, 100)); // TODO list ?!
      user3.setDocumentation("see xls : id = opaque");
      user3.setStorage(true);
      user3.setInstanciable(true);
      
      // sample data sandbox fork :
      // first create the project
      DCProject org3SandboxProject = new DCProject(ORG3_SANDBOX_PROJECT);
      org3SandboxProject.addLocalVisibleProject(org3Project); // mainProject // means inherits its visible projects
      ///org3Sandbox.setPointOfViewNames(Arrays.asList(mainProject.getName()));
      modelAdminService.addProject(org3SandboxProject);
      // only then add model (else dep models not visible)
      DCModelBase org3SandboxStorageMetamodel = this.fork(
            modelAdminService.getModelBase(ResourceModelIniter.MODEL_MODEL_NAME),
            org3SandboxProject, false, false, true, false);
      org3Project.addLocalModel(agrilocorgpri3); // else not visible to org3 when adding it
      org3Project.addLocalModel(orgpri3); // else not visible to org3 when adding it
      org3Project.addLocalModel(org3); // else not visible to org3SandboxProject when forking it
      DCModelBase org3Sandbox = this.fork(org3,
            org3SandboxProject, false, false, true, false);
      
      
      // orgx : pov ?

      
      // NB. for now mixins must be added BEFORE models they're added to !!!
      modelsToCreate.addAll(Arrays.asList(
            org1, orgpri1,
            
            ///orgpri2storageMetamodel,
            org2, // even if already added
            offer2, // TODO Q or in agrilocal-specific project that sees main or even orgpri2 ?
            orgpri2mixin, agrilocorgpri2, orgpri2, // NB. has to be created in another project !
            user2,
            
            agrilocorgpri3, orgpri3, // NB. has to be in another scope / project
            org3, // even if already added
            offer3, // TODO Q or in agrilocal-specific project that sees main or even orgpri2 ?
            org3SandboxStorageMetamodel, org3Sandbox, // NB. has to be created in another project !
            user3));
   }


   // see doc in overriden
   @Override
   public void do3FillReferenceData() throws Exception {
      
   }


   // see doc in overriden
   @Override
   public void do4FillSampleData() throws WebApplicationException {
      try {
         doInitSampleDataOrg1();
      } catch (RuntimeException e) {
         throw e;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      try {
         doInitSampleDataOrg2();
      } catch (RuntimeException e) {
         throw e;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      try {
         doInitSampleDataOrg3();
      } catch (RuntimeException e) {
         throw e;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }


   private void doInitSampleDataOrg1() throws Exception {
      String owUri = UriHelper.buildUri(containerUrl, ORG1, "FR/Open Wide");
      postDataInTypeIfNotExists(DCResource.create(owUri)
            .set("org1:name", "Open Wide")
            .set("org1:jurisdiction", "FR")
            .set("org1:regNumber", "12347366")
            .set("org1:activity", "722"), ORG1_PROJECT);

      String owPriUri = UriHelper.buildUri(containerUrl, ORGPRI1, "FR/Open Wide");
      postDataInTypeIfNotExists(DCResource.create(owPriUri)
            .set("org1:name", "Open Wide")
            .set("org1:jurisdiction", "FR")
            .set("org1:regNumber", "12347366")
            .set("org1:activity", "722")
            .set("orgpri1:org", owUri)
            .set("orgpri1:publishedVersion", 0) // i.e. owPri's !!!
            .set("orgpri1:state", "published")
            .set("orgpri1:employeeNb", 130), ORG1_PROJECT);

      String pnUri = UriHelper.buildUri(containerUrl, ORG1, "Pôle Numérique");
      postDataInTypeIfNotExists(DCResource.create(pnUri)
            .set("org1:name", "Pôle Numérique")
            .set("org1:jurisdiction", "FR")
            .set("org1:regNumber", "12347367")
            .set("org1:activity", "111")/*
            .set("orgpri1:org", pnUri)
            .set("orgpri1:publishedVersion", 0) // i.e. pnPri's !!!
            .set("orgpri1:state", "published")
            .set("orgpri1:employeeNb", 10)*/, ORG1_PROJECT);
   }
   



   private void doInitSampleDataOrg2() throws Exception {
      String owUri = UriHelper.buildUri(containerUrl, ORG2, "FR/Open Wide");
      postDataInTypeIfNotExists(DCResource.create(owUri)
            .set("org2:name", "Open Wide")
            .set("org2:jurisdiction", "FR")
            .set("org2:regNumber", "12347366")
            .set("org2:activity", "722"), ORG2_PROJECT);

      String owPriUri = owUri; // UriHelper.buildUri(containerUrl, ORGPRI2, "FR/Open Wide");
      postDataInTypeIfNotExists(DCResource.create(owPriUri)
            .set("org2:name", "Open Wide")
            .set("org2:jurisdiction", "FR")
            .set("org2:regNumber", "12347366")
            .set("org2:activity", "722")
            ///.set("orgpri2:org", owUri) /// NOO ARE THE SAME
            .set("orgpri2:publishedVersion", 0l) // i.e. owPri's !!!
            .set("orgpri2:state", "published")
            .set("orgpri2:employeeNb", 130)
            .set("agrilocorg2:proposedOffers", new ArrayList<String>(0)), ORGPRI2_PROJECT);

      String pnUri = UriHelper.buildUri(containerUrl, ORG2, "Pôle Numérique");
      postDataInTypeIfNotExists(DCResource.create(pnUri)
            .set("org2:name", "Pôle Numérique")
            .set("org2:jurisdiction", "FR")
            .set("org2:regNumber", "12347367")
            .set("org2:activity", "111")/*
            ///.set("orgpri2:org", pnUri) /// NOOO ARE THE SAME
            .set("orgpri2:publishedVersion", 0) // i.e. pnPri's !!!
            .set("orgpri2:state", "published")
            .set("orgpri2:employeeNb", 10)*/, ORG2_PROJECT);

      String mdUri = UriHelper.buildUri(containerUrl, USER2, "0678");
      postDataInTypeIfNotExists(DCResource.create(mdUri)
            .set("odisp:name", "M D")
            .set("user2:country", "FR")
            .set("user2:nationalId", "0678")
            .set("user2:firstName", "M")
            .set("user2:familyName", "D")
            .set("user2:birthDate", new DateTime())
            .set("user2:organization", owUri), ORG2_PROJECT);

      String webAppOfferUri = UriHelper.buildUri(containerUrl, AGRILOC_OFFER2, "5678");
      postDataInTypeIfNotExists(DCResource.create(webAppOfferUri)
            .set("odisp:name", "3 webapps")
            .set("agriloco2:id", "5678")
            .set("agriloco2:type", "webapp")
            .set("agriloco2:amount", 3)
            //.set("agriloco2:maxPrice", 15.99)
            .set("agriloco2:answerDeadline", new DateTime())
            .set("agriloco2:deliveryDeadline", new DateTime())
            .set("agriloco2:requester", pnUri)
            .set("agriloco2:state", "accepted"),
            //.set("agriloco2:provider", owUri); // TODO Q NO would be visible publicly !!
            ORG2_PROJECT);

      // OW answers to offer :
      DCResource owPri = getData(DCResource.create(owPriUri), ORGPRI2_PROJECT);
      owPri.set("agrilocorg2:proposedOffers", DCResource.listBuilder().add(webAppOfferUri).build());
      postDataInType(owPri , ORGPRI2_PROJECT);
   }

   
   
   private void doInitSampleDataOrg3() throws Exception {
      String owUri = UriHelper.buildUri(containerUrl, ORG3, "FR/Open Wide");
      postDataInTypeIfNotExists(DCResource.create(owUri)
            .set("org3:name", "Open Wide")
            .set("org3:jurisdiction", "FR")
            .set("org3:regNumber", "12347366")
            .set("org3:activity", "722"), ORG3_PROJECT);

      DCResource ow = getData(DCResource.create(owUri), ORG3_PROJECT);
      postDataInType(ow
            /*.set("org3:name", "Open Wide")
            .set("org3:jurisdiction", "FR")
            .set("org3:regNumber", "12347366")
            .set("org3:activity", "722")*/ // NOO ALREADY ON THE SAME RESOURCE
            ///.set("orgpri3:org", owUri) /// NOO ARE THE SAME
            .set("orgpri3:publishedVersion", 0l) // i.e. owPri's !!!
            .set("orgpri3:state", "published")
            .set("orgpri3:employeeNb", 130)
            .set("agrilocorg3:proposedOffers", new ArrayList<String>(0)), ORG3_PROJECT); // TODO privately as ow

      String pnUri = UriHelper.buildUri(containerUrl, ORG3, "Pôle Numérique");
      postDataInTypeIfNotExists(DCResource.create(pnUri)
            .set("org3:name", "Pôle Numérique")
            .set("org3:jurisdiction", "FR")
            .set("org3:regNumber", "12347367")
            .set("org3:activity", "111")/*
            ///.set("orgpri3:org", pnUri) /// NOOO ARE THE SAME
            .set("orgpri3:publishedVersion", 0) // i.e. pnPri's !!!
            .set("orgpri3:state", "published")
            .set("orgpri3:employeeNb", 10)*/, ORG3_PROJECT);

      String mdUri = UriHelper.buildUri(containerUrl, USER3, "0678");
      postDataInTypeIfNotExists(DCResource.create(mdUri)
            .set("odisp:name", "M D")
            .set("user3:country", "FR")
            .set("user3:nationalId", "0678")
            .set("user3:firstName", "M")
            .set("user3:familyName", "D")
            .set("user3:birthDate", new DateTime())
            .set("user3:organization", owUri), ORG3_PROJECT);

      String webAppOfferUri = UriHelper.buildUri(containerUrl, AGRILOC_OFFER3, "5678");
      postDataInTypeIfNotExists(DCResource.create(webAppOfferUri)
            .set("odisp:name", "3 webapps")
            .set("agriloco3:id", "5678")
            .set("agriloco3:type", "webapp")
            .set("agriloco3:amount", 3)
            //.set("agriloco3:maxPrice", 15.99)
            .set("agriloco3:answerDeadline", new DateTime())
            .set("agriloco3:deliveryDeadline", new DateTime())
            .set("agriloco3:requester", pnUri)
            .set("agriloco3:state", "accepted"),
            //.set("agriloco3:provider", owUri); // TODO Q NO would be visible publicly !!
            ORG3_PROJECT);

      // OW answers to offer :
      DCResource owPri = getData(DCResource.create(owUri), ORG3_PROJECT);
      owPri.set("agrilocorg3:proposedOffers", DCResource.listBuilder().add(webAppOfferUri).build());
      postDataInType(owPri , ORG3_PROJECT); // TODO privately as ow
   }
   
}
