package org.oasis.datacore.sample;

import java.net.URI;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.oasis.datacore.core.meta.model.DCModelBase;
import org.springframework.beans.factory.annotation.Value;


/**
 * Draft of OpenElec Models.
 * 
 * Used by tests & demo
 * 
 * Feedback to provider (Atreal) :
 * TODO
 * * as always, please check choices made for : Model/Mixin & field names, uri format,
 * overall modelling (perimeter of each Model and Mixin, links between them, types of fields)
 * * if everything is OK, start using those Models in your demo. This means writing code that
 * first creates new Resources in them. For this, copy / paste / adapt to your language of choice
 * code from fillData(), whose comments say all that must be taken care of.
 * 
 * @author mdutoo
 *
 */
public abstract class DatacoreSampleMethodologyBase extends DatacoreSampleBase {
   
   /** to be able to build a full uri, to avoid using ResourceService */
   ///@Value("${datacoreApiClient.baseUrl}") 
   ///private String baseUrl; // useless
   /////@Value("${datacoreApiClient.containerUrl}") // DOESN'T WORK 
   @Value("${datacoreApiServer.containerUrl}")
   protected String containerUrlString;
   //@Value("#{new java.net.URI('${datacoreApiClient.containerUrl}')}")
   @Value("#{uriService.getContainerUrl()}")
   protected URI containerUrl;

   @Override
   protected void buildModels(List<DCModelBase> models) {
      do2CreateModels(models);
   }

   @Override
   public void fillData() {
      try {
         do3FillReferenceData();
      } catch (WebApplicationException waex) {
         throw new RuntimeException("HTTP " + waex.getResponse().getStatus()
               + " web app error initing reference data :\n" + waex.getResponse().getStringHeaders() + "\n"
               + ((waex.getResponse().getEntity() != null) ? waex.getResponse().getEntity() + "\n\n" : ""), waex);
      } catch (RuntimeException e) {
         throw e;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      do4FillSampleData();
   }

   
   /**
    * Modelling methodology step 0 :
    * 
    * 0. select data of your application to share, 
    *   - either data required by interoperability with other applications
    *   - or your application's core business data
    *   (as its provider, you now best in which data reside the most value potential)
    */
   public abstract void doOSelectPerimeter();
   
   
   /**
    * Modelling methodology step 1.a & 1.b :
    * 
    * 1.a provide a denormalized view of your data :
    * i.e. flat, including fields of related classifications
    * ex. https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-rest-server/src/main/resources/samples/provto/economicalActivity/economicalActivity.csv
    * 
    * and write a Model for it :
    * (the simplest one : no outside references, all fields queriable and string).
    * See Model specification at https://github.com/pole-numerique/oasis-datacore/tree/master/oasis-datacore-samples/src/main/java/org/oasis/datacore/data/meta
    * tips :
    * - give version "0" to your Models (and Mixins) since they are new, and refer to them
    * (from Resource Model type and Resource fields) using modelType = model name + '/' + model version,
    * ex. plo:country/0 .
    * - give a namespace to each of your Model, and a prefix that shortens it and will prefix field names,
    * that contains what it is about and if need be who is responsible for defining it, ex:
    * place.city => pli: , place.city.italia => pliit:
    * This way, we can know from a field nale what it is about and who (which data community) is responsible,
    * and here is why this is important :
    * For instance, a referencingMixin doesn't really define its fields besides the URI,
    * it rather reuses those of the canonical Model that it derives from.
    * Moreover, it's better not to define too many fields in order to have cross Model queries that are doable
    * (ex. o:displayName, pl:name, pl:address, pl:geo, LATER or even full text index...)
    * So the question to be asked is : are those two fields the same or not,
    * are they useful in the same use case (i.e. queries) ?
    * (NB. namespaces are not versioned, because they only occur in the context of well defined
    * versions of Models and Mixins).
    * TEMPORARY FOR NOW use '_' instead of '/' and set model.name to modelType
    * ex. plo:country_0, to avoid URI parsing problems and let older Model samples still work.
    * - put in their documentation the rules that govern Resources or each of your Models
    * - and first of all, the rules that govern their URIs. Some good practices :
    *    - avoid adding internal / technical / business id (unique) fields to allow
    * applications to reconcile with it. Rather, build your Resources' URIs out of those ids,
    * ex. atecoCode "myCode" => uri : "http://data.ozwillo.com/dc/modelType/ateco/myCode"
    * - available field types : string, boolean, int, float, long, double, date, map, list, i18n, resource
    *    - "queryLimit":100 means that this field is indexed (wouldn't be if rather 0)
    * and may therefore be queried but at most 100 results will be returned (which can be detected
    * by using the "debug" query switch). Its purpose is to guide usage of your Model, by following
    * a "guide the user but prevent him from wrecking everything" (i.e. unoptimized / too big queries)
    * philosophy rather than a "magic join & optimization query engine" philosophy
    * that doesn't work when distributed.
    * 
    * 1.b group together fields that have been denormalized from another table,
    * and for each add a URI resource reference to a Model corresponding to said table.
    * 
    */
   public abstract void do1DesignFlatModel();


   /**
    * Modelling methodology step 1.c :
    * 
    * 1.c extract said groups out of it as Mixins (i.e. reusable & referenced parts.
    * NB. a common base Mixin for each referenced Model and referencing Mixin
    * could be defined, but since there could be many different referencing Mixing,
    * it's not really useful => rather promote reuse of Model fields in
    * referencing Mixins ex. check that referencing Mixin fields are the Model's ;
    * the only reason to have different fields would be to have different uses
    * i.e. query limit or index.
    * 
    */
   public abstract void do2ExternalizeModelAsMixins();

   
   /**
    * Modelling methodology step 2. (define final Models) :
    * 
    * 2. find out which Models already exist, including "this" ex. economicalActivity,
    * and reconcile with them :
    * - reuse existing fields ; if format is different, then if bijective convert them on the fly,
    * else use another field(s) and maintain it (LATER link both and if the source one changes :
    *    - either put the change only as an alternate version of the Resource in the contribution derived Model and collection
    * (which should then be made queriable i.e. indexed and supported by the query engine) ;
    *    - or mark it obsolete at resource level in fields such as o:obsoleteFields,
    * o:lastApprovedFieldValues, o:unapprovedFieldValues ;
    *    - or put the change in the same Model and collection but with a contribution id
    * (source and version) as uri suffix (i.e. a polymorphic alternative to contributions)
    * - or else add new ones in new Mixins (your referenced Models become Mixins)
    * 
    */
   public abstract void do2CreateModels(List<DCModelBase> modelsToCreate);


   /**
    * Modelling methodology step 3. :
    * 
    * 3. init reference Data (i.e. that is not managed by the application and provided among sample data).
    * 
    * BEWARE, the person / organization doing this operation becomes the data "owner"
    * and must maintain it over time by re-doing this operation regularly !
    * 
    * That's why, in an ideal world, all data should be managed by the application including
    * reference data, or have a clearly defined, existing provider that the application knowingly
    * depends on (but here we're trying to define samples without such dependencies).
    * 
    * However, until this time, reference Data is allowed be inited separately,
    * typically at application "install" (at worst init, if possible deploy) time.
    * 
    * @throws Exception
    */
   public abstract void do3FillReferenceData() throws WebApplicationException, Exception;


   /**
    * Modelling methodology step 4. :
    * 
    * 4. fill sample data
    */
   public abstract void do4FillSampleData() throws WebApplicationException;
   
}
