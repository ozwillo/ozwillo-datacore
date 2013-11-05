package org.oasis.datacore.rest.client;

import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.client.ClientException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.interceptor.OutInterceptors;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.client.cxf.ETagClientOutInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Component;


/**
 * This wrapper adds caching to DatacoreApi client.
 * Putting in cache is done either using Spring Cacheable etc. annotations
 * if possible, or in wrapper logic.
 * Getting from cache is done in wrapper logic, when receiving HTTP 304
 * (not modified) response after having set If-None-Match header
 * in CXF Out interceptor on GET request.
 * 
 * NB. Spring Cacheable annotation CAN'T BE USED EVERYWHERE BECAUSE DON'T SUPPORT
 * item lists or generic methods (ex. serveData) 
 * 
 * @author mdutoo
 *
 */
@Component(value="datacoreApiCachedClient")
@OutInterceptors(classes={ ETagClientOutInterceptor.class })
//@InInterceptors(classes={ ETagClientInInterceptor.class }) // not used (this cached impl is
// required and enough to put data resources in cache)
public class DatacoreApiCachedClientImpl implements DatacoreClientApi/*DatacoreApi*/ {
   
   @Autowired
   @Qualifier("datacoreApiClient")
   private /*DatacoreApi*/DatacoreClientApi delegate;

   /** to be able to get from cache on 304 not modified exception, and other manual put & evict */
   @Autowired
   @Qualifier("datacore.rest.client.cache.rest.api.DCResource")
   private Cache resourceCache; // EhCache getNativeCache
   
   /** to be able to build a full uri to evict cached data */
   ///@Value("${datacoreApiClient.baseUrl}") 
   ///private String baseUrl; // useless
   @Value("${datacoreApiClient.containerUrl}") 
   private String containerUrl;


   /**
    * TODO in helper
    * @param type
    * @param iri
    * @return
    */
   private String buildUri(String type, String iri) {
      return UriHelper.buildUri(this.containerUrl, type, iri);
   }
   
   
   /**
    * Always evict (after invocation) and replace by updated data
    * done in wrapper logic (Spring CachePut annotation would use possibly not yet created uri from argument)
    * TODO LATER save if no diff (in non strict post mode)
    */
   //@CachePut(value={"org.oasis.datacore.rest.api.DCResource"}, key="resource.uri")
   @Override
   public DCResource postDataInType(DCResource resource, String modelType) {
      resource = delegate.postDataInType(resource, modelType); // TODO better than client side helper :
      /*
      LinkedList<DCResource> resources = new LinkedList<DCResource>();
      resources.add(resource);
      resource = delegate.postAllDataInType(resources, type).get(0);
      */

      // put in cache :
      resourceCache.put(resource.getUri(), resource); // NB. if no error, resource can't be null
      return resource;
   }

   /**
    * Always evict (after invocation) and replace by updated data,
    * done in wrapper logic (Spring CachePut annotation doesn't support item lists)
    * TODO LATER save if no diff (in non strict post mode)
    */
   @Override
   public List<DCResource> postAllDataInType(List<DCResource> resources, String modelType) {
      resources = delegate.postAllDataInType(resources, modelType);
      
      // put in cache :
      for (DCResource resource : resources) {
         resourceCache.put(resource.getUri(), resource); // NB. if no error, resource can't be null
      }
      return resources;
   }

   /**
    * Always evict (after invocation) and replace by updated data,
    * done in wrapper logic (Spring CachePut annotation doesn't support item lists)
    * TODO LATER save if no diff (in non strict post mode)
    */
   @Override
   public List<DCResource> postAllData(List<DCResource> resources) {
      resources = delegate.postAllData(resources);
      
      // put in cache :
      for (DCResource resource : resources) {
         resourceCache.put(resource.getUri(), resource); // NB. if no error, resource can't be null
      }
      return resources;
   }

   /**
    * Always evict (after invocation) and replace by updated data, using Spring CachePut annotation
    * TODO LATER save if no diff
    */
   @CachePut(value={"org.oasis.datacore.rest.api.DCResource"}, key="#resource.uri") // after invocation
   @Override
   public DCResource putDataInType(DCResource resource, String modelType, String iri) {
      return delegate.putDataInType(resource, modelType, iri);
   }

   /**
    * Always evict (after invocation) and replace by updated data,
    * done in wrapper logic (Spring CachePut annotation doesn't support item lists)
    * TODO LATER save if no diff
    */
   @Override
   public List<DCResource> putAllDataInType(List<DCResource> resources, String modelType) {
      resources = delegate.putAllDataInType(resources, modelType);
      
      // put in cache :
      for (DCResource resource : resources) {
         resourceCache.put(resource.getUri(), resource); // NB. if no error, resource can't be null
      }
      return resources;
   }

   /**
    * Always evict (after invocation) and replace by updated data,
    * done in wrapper logic (Spring CachePut annotation doesn't support item lists)
    * TODO LATER save if no diff
    */
   @Override
   public List<DCResource> putAllData(List<DCResource> resources) {
      resources = delegate.putAllData(resources);
      
      // put in cache :
      for (DCResource resource : resources) {
         resourceCache.put(resource.getUri(), resource); // NB. if no error, resource can't be null
      }
      return resources;
   }

   //@CachePut(value={"org.oasis.datacore.rest.api.DCResource"}, key="resource.uri") // NO key not fully known
   //@Cacheable(value={"org.oasis.datacore.rest.api.DCResource"}, key="resource.uri")
   // NOT Cacheable because returning from cache is triggered from HTTP 304 reponse
   @Override
   public DCResource getData(String modelType, String iri, Request request) {
      // NB. request is only for server etag, on client side it is done rather in interceptor
      try {
         DCResource resource = this.delegate.getData(modelType, iri, request);
         
         // put in cache :
         if (resource != null) {
            resourceCache.put(resource.getUri(), resource);
         } // else if server still has null it costs nothing to send it back,
         // so no ETag support in this case (though Spring Cache could cache null)
         return resource;
         
      } catch (RedirectionException rex) {
         if (Status.NOT_MODIFIED.getStatusCode() == rex.getResponse().getStatus()) {
            // HTTP 304 (not modified) : get from cache
            String uri = buildUri(modelType, iri);
            ValueWrapper cachedDataWrapper = resourceCache.get(uri); // NB. ValueWrapper wraps cached null
            if (cachedDataWrapper != null) {
               DCResource cachedData = (DCResource) cachedDataWrapper.get();
               if (cachedData != null) {
                  return cachedData;
               }
            }
            
            throw new RuntimeException("Received HTTP 304 (not modified) but no more data "
                  + "in cache, maybe has just been evicted by DELETE ?", rex); // should not happen
         }
         
         throw rex;
      }
      //Response response = this.delegate.getData(type, iri, method, request);
      //DCResource resource = (DCResource) response.getEntity();
      //return resource;
   }

   /**
    * Always evict (after invocation) and replace by updated data, using Spring CacheEvict annotation
    * done in wrapper logic (Spring CacheEvict needs full uri)
    * done in wrapper logic (Spring CachePut annotation doesn't support item lists)
    * TODO LATER save if no diff
    */
   //@CacheEvict(value={"org.oasis.datacore.rest.api.DCResource"}, key="resource.uri") // NO key not fully known
   @Override
   public void deleteData(String modelType, String iri, HttpHeaders headers) {
      delegate.deleteData(modelType, iri, headers);
      
      // evict from cache :
      String uri = buildUri(modelType, iri);
      resourceCache.evict(uri);
   }

   
   @Override
   public List<DCResource> updateDataInTypeWhere(DCResource resourceDiff,
         String modelType, UriInfo uriInfo) {
      return this.delegate.updateDataInTypeWhere(resourceDiff, modelType, uriInfo);
   }
   @Override
   public void deleteDataInTypeWhere(String modelType, UriInfo uriInfo) {
      this.delegate.deleteDataInTypeWhere(modelType, uriInfo);
   }
   
   
   /**
    * Does the work of POST ; can't be done using Spring annotations
    */
   @Override
   public DCResource postDataInTypeOnGet(@PathParam("type") String modelType, @QueryParam("method") String method,
         @Context UriInfo uriInfo/*, @Context Request request*/) {
      DCResource resource = delegate.postDataInTypeOnGet(modelType, method, uriInfo);
      
      // put in cache :
      resourceCache.put(resource.getUri(), resource); // NB. if no error, resource can't be null
      return resource;
   }

   /**
    * Does the work of PUT, PATCH, DELETE ; can't be done using Spring annotations
    */
   @Override
   public DCResource putPatchDeleteDataOnGet(@PathParam("type") String modelType, @PathParam("iri") String iri,
         @QueryParam("method") String method, @Context UriInfo uriInfo, @Context HttpHeaders httpHeaders) {
      DCResource resource = delegate.putPatchDeleteDataOnGet(modelType, iri, method, uriInfo, httpHeaders);

      if (HttpMethod.DELETE.equals(method)) {
         // evict from cache :
         String uri = buildUri(modelType, iri);
         resourceCache.evict(uri);
      } else { // PUT, PATCH
         // put in cache :
         resourceCache.put(resource.getUri(), resource); // NB. if no error, resource can't be null
      }
      return resource;
   }

   @Override
   public List<DCResource> findDataInType(String modelType, UriInfo uriInfo, Integer start, Integer limit) {
      //return delegate.findDataInType(type, uriInfo, start, limit, sort); // TODO cache ??
      throw new ClientException("Use rather findDataInType(String queryParams ...) on client side");
   }

   @Override
   public List<DCResource> findData(UriInfo uriInfo, Integer start, Integer limit) {
      //return delegate.findData(uriInfo, start, limit, sort); // TODO cache ??
      throw new ClientException("Use rather findData(String queryParams ...) on client side");
   }

   @Override
   public List<DCResource> findDataInType(String modelType, String queryParams,
         Integer start, Integer limit) {
      return delegate.findDataInType(modelType, queryParams, start, limit);
   }
   @Override
   public List<DCResource> findData(String queryParams, Integer start,
         Integer limit) {
      return delegate.findData(queryParams, start, limit);
   }
   
   @Override
   public List<DCResource> queryDataInType(String modelType, String query, String language) {
      return delegate.queryDataInType(modelType, query, language); // TODO cache ??
   }
   
   @Override
   public List<DCResource> queryData(String query, String language) {
      return delegate.queryData(query, language); // TODO cache ??
   }

   public /*DatacoreApi*/DatacoreClientApi getDelegate() {
      return delegate;
   }

   public void setDelegate(/*DatacoreApi*/DatacoreClientApi delegate) {
      this.delegate = delegate;
   }

   public Cache getCache() {
      return resourceCache;
   }

   public void setCache(Cache cache) {
      this.resourceCache = cache;
   }

   /*public String getBaseUrl() {
      return baseUrl;
   }

   public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
   }*/

}
