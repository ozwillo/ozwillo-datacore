package org.oasis.datacore.rest.api.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.server.cxf.JaxrsServerBase;
import org.springframework.beans.factory.annotation.Value;


/**
 * Used to test client : version & cache
 * 
 * TODO could be put rather in api core ?
 * 
 * @author mdutoo
 *
 */
public class DatacoreApiMockServerImpl extends JaxrsServerBase implements DatacoreApi {
   
   public static final String TEST_HEADER_MODEL_TYPE_QUERY_TRIGGER = "testUserJohnModelTypeQueryTrigger";

   /** to be able to build a full uri */
   ///@Value("${datacoreApiClient.baseUrl}") 
   ///private String baseUrl; // useless
   @Value("${datacoreApiClient.containerUrl}") 
   private String containerUrlString;
   @Value("#{new java.net.URI('${datacoreApiClient.containerUrl}')}")
   //@Value("#{uriService.getContainerUrl()}")
   private URI containerUrl;
   
   /** to be able to follow version for cache etc. tests */
   private long franceBordeauxCityVersion;
   
   ///@Override
   public DCResource postDataInType(DCResource resource, String modelType) {
      Long version = resource.getVersion();
      if (version == null) {
         version = 0l;
      } // else TODO boum
      version++;
      if (resource.getUri().endsWith("France/Bordeaux")) {
         franceBordeauxCityVersion = version;
      }
      resource.setVersion(version);
      return resource;
   }

   @Override
   public List<DCResource> postAllDataInType(List<DCResource> resources, String modelType) {
      // NB. must be implemented, because used by POST of single resource
      // (through Jackson's accept single value as array)
      List<DCResource> res = new ArrayList<DCResource>();
      for (DCResource resource : resources) {
         res.add(postDataInType(resource, modelType));
      }
      return res;
   }

   @Override
   public List<DCResource> postAllData(List<DCResource> resources) {
      return resources;
   }

   @Override
   public DCResource patchDataInType(DCResource resource, String modelType, String iri) {
      return this.postDataInType(resource, modelType);
   }

   @Override
   public DCResource putDataInType(DCResource resource, String modelType, String iri) {
      return this.postDataInType(resource, modelType);
   }

   @Override
   public List<DCResource> putAllDataInType(List<DCResource> resources, String modelType) {
      return resources;
   }

   @Override
   public List<DCResource> putAllData(List<DCResource> resources) {
      return resources;
   }

   @Override
   public DCResource getData(String modelType, String iri, Long version) {
      DCResource resource = new DCResource();
      resource.setUri(UriHelper.buildUri(containerUrl, modelType, iri));
      resource.setVersion(0l);
      /*resource.setProperty("type", type);
      resource.setProperty("iri", iri);*/
      String[] iriElements = iri.split("/");
      resource.setProperty("name", iriElements[1]);
      String countryType = "country";
      String countryName = iriElements[0];
      DCResource countryData = new DCResource();
      /*countryData.setProperty("type", countryType);
      countryData.setProperty("iri", countryName);*/
      countryData.setProperty("name", countryName);
      countryData.setUri(UriHelper.buildUri(containerUrl, countryType, countryName));
      countryData.setVersion(0l);
      resource.setProperty("inCountry", countryData);
      
      if ("France/Bordeaux".equals(iri)) {
         resource.setVersion(franceBordeauxCityVersion);


         // ETag caching :
         // on GET, If-None-Match precondition else return 304 Not Modified http://stackoverflow.com/questions/2021882/is-my-implementation-of-http-conditional-get-answers-in-php-is-ok/2049723#2049723
         // see for jaxrs https://devcenter.heroku.com/articles/jax-rs-http-caching#conditional-cache-headers
         // and for http http://odino.org/don-t-rape-http-if-none-match-the-412-http-status-code/
         // also http://stackoverflow.com/questions/2085411/how-to-use-cxf-jax-rs-and-http-caching
         String httpEntity = resource.getVersion().toString(); // no need of additional uri because only for THIS resource
         EntityTag eTag = new EntityTag(httpEntity);
         //ResponseBuilder builder = request.evaluatePreconditions(dataEntity.getUpdated(), eTag);
         ResponseBuilder builder = jaxrsRequest.evaluatePreconditions(eTag);
         
         if (builder == null) {
            // (if provided) If-None-Match precondition OK (resource did change), so serve updated content
            builder = Response.ok(resource).tag(eTag); // .lastModified(dataEntity.getLastModified().toDate())
         } // else provided If-None-Match precondition KO (resource didn't change),
         // so return 304 Not Modified (and don't send the dcData back)

         // NB. no cache control max age, else HTTP clients won't even send new requests until period ends !
         //CacheControl cc = new CacheControl();
         //cc.setMaxAge(600);
         //builder.cacheControl(cc);
         
         // NB. lastModified would be pretty but not used at HTTP level
         //builder.lastModified(person.getUpdated());
         
         throw new WebApplicationException(builder.build());
      }
      
      return resource;
   }

   
   @Override
   public List<DCResource> updateDataInTypeWhere(DCResource dcDataDiff,
         String modelType, UriInfo uriInfo) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void deleteDataInTypeWhere(String modelType, UriInfo uriInfo) {
      // TODO Auto-generated method stub
   }
   

   @Override
   public void deleteData(String modelType, String iri, Long version) {
      // TODO Auto-generated method stub
   }

   @Override
   public DCResource postDataInTypeOnGet(String modelType, String method, UriInfo uriInfo) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public DCResource putPatchDeleteDataOnGet(String modelType, String iri,
         String method, Long version, UriInfo uriInfo) {
      // TODO Auto-generated method stub
      return null;
   }
   

   @Override
   public List<DCResource> findDataInType(String modelType, UriInfo uriInfo, Integer start, Integer limit, boolean debug) {
      if (TEST_HEADER_MODEL_TYPE_QUERY_TRIGGER.equals(modelType)) {
         ArrayList<DCResource> res = new ArrayList<DCResource>();
         DCResource responseResource = new DCResource();
         String header = "testUser";
         responseResource.setUri(UriHelper.buildUri(containerUrl, modelType, header));
         responseResource.set(header, httpHeaders.getHeaderString(header));
         res.add(responseResource);
         return res;
      }
      return null;
   }

   @Override
   public List<DCResource> findData(UriInfo uriInfo, Integer start, Integer limit) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<DCResource> queryDataInType(String modelType, String query,
         String language) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<DCResource> queryData(String query, String language) {
      // TODO Auto-generated method stub
      return null;
   }

	@Override
	public DCResource findHistorizedResource(String modelType, String iri, Integer version) throws BadRequestException, NotFoundException {

		return null;

	}

}
