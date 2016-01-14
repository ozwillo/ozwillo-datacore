package org.oasis.datacore.rest.client;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.NotImplementedException;
import org.apache.cxf.interceptor.OutInterceptors;
import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.DCRequestContextProviderFactory;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.client.cxf.ETagClientOutInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

/**
 * This wrapper adds caching to DatacoreApi client, to use it inject
 * DatacoreCachedClient.
 * 
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
public class DatacoreApiCachedClientImpl implements DatacoreCachedClient/*DatacoreApi*/ {

   @Autowired
   @Qualifier("datacoreApiClient")
   private /*DatacoreApi*/DatacoreClientApi delegate;

   /** to be able to get from cache on 304 not modified exception, and other manual put & evict */
   @Autowired
   @Qualifier("datacore.rest.client.cache.rest.api.DCResource")
   private Cache resourceCache; // EhCache getNativeCache

   /** to get project ; not accessed statically to get CXF-based impl */
   @Autowired
   private DCRequestContextProviderFactory requestContextProviderFactory;

   /** to be able to build a full uri to evict cached data */
   ///@Value("${datacoreApiClient.baseUrl}")
   ///private String baseUrl; // useless
   @Value("${datacoreApiClient.containerUrl}") //:http://data-test.ozwillo.com/
   private String containerUrlString;
   @Value("#{new java.net.URI('${datacoreApiClient.containerUrl}')}")
   //@Value("#{uriService.getContainerUrl()}")
   private URI containerUrl;

   /**
    * TODO in helper
    * @param type
    * @param iri
    * @return
    */
   private String buildUri(String type, String iri) {
      return UriHelper.buildUri(this.containerUrl, type, iri);
   }
   
   public String getProject() {
      String project = (String) requestContextProviderFactory.get(DatacoreApi.PROJECT_HEADER);
      return project == null ? "oasis.main" : project;
   }

   /* (non-Javadoc)
    * @see org.oasis.datacore.rest.client.DatacoreCachedClient#postDataInType(org.oasis.datacore.rest.api.DCResource)
    */
   @CachePut(value={"org.oasis.datacore.rest.api.DCResource"}, key="#root.target.getProject() + '.' + #resource.uri")
   @Override
   public DCResource postDataInType(DCResource resource) {
      String modelType = resource.getModelType(); // NB. if null lets server explode
      return this.postDataInType(resource, modelType);
   }
   
   /**
    * Always evict (after invocation) and replace by updated data
    * done in wrapper logic (Spring CachePut annotation would use possibly not yet created uri from argument)
    * TODO LATER save if no diff (in non strict post mode)
    */
   @CachePut(value={"org.oasis.datacore.rest.api.DCResource"}, key="#root.target.getProject() + '.' + #resource.uri")
   @Override
   public DCResource postDataInType(DCResource resource, String modelType) {
      resource = delegate.postDataInType(resource, modelType); // TODO better than client side helper :
      /*
      LinkedList<DCResource> resources = new LinkedList<DCResource>();
      resources.add(resource);
      resource = delegate.postAllDataInType(resources, type).get(0);
      */

      ///putInCache(resource);
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

      putInCache(resources);
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

      putInCache(resources);
      return resources;
   }

   /**
    * Always evict (after invocation) and replace by updated data, using Spring CachePut annotation
    * TODO LATER save if no diff
    */
   @CachePut(value={"org.oasis.datacore.rest.api.DCResource"}, key="#root.target.getProject() + '.' + #resource.uri") // after invocation
   @Override
   public DCResource patchDataInType(DCResource resource, String modelType, String iri) {
      return delegate.patchDataInType(resource, modelType, iri);
   }

   /**
    * Always evict (after invocation) and replace by updated data, using Spring CachePut annotation
    * TODO LATER save if no diff
    */
   @CachePut(value={"org.oasis.datacore.rest.api.DCResource"}, key="#root.target.getProject() + '.' + #resource.uri") // after invocation
   @Override
   public DCResource putDataInType(DCResource resource, String modelType, String iri) {
      return delegate.putDataInType(resource, modelType, iri);
   }

   /**
    * Shortcut to putAllDataInType(List<DCResource> resources, String modelType).
    * Copies that method's cache annotations, because they are not applied when calling it
    * (because it doesn't call the cached wrapper but the impl instance).
    */
   @CachePut(value={"org.oasis.datacore.rest.api.DCResource"}, key="#root.target.getProject() + '.' + #resource.uri") // after invocation
   @Override
   public DCResource putDataInType(DCResource resource) throws IllegalArgumentException {
      String modelType = resource.getModelType(); // NB. if null lets server explode
      String id = resource.getId();
      if (id == null) {
         // init id to make it easier to reuse POSTed & returned resources :
         // TODO rather make id transient ? or auto set id on unmarshall ??
         try { 
            id = UriHelper.parseUri(resource.getUri(), this.containerUrlString).getId();
         } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalArgumentException("Bad Resource URI " + resource.getUri(), e);
         }
      }
      return this.putDataInType(resource, modelType, id); // TODO or parse id from uri ??
   }

   /**
    * Always evict (after invocation) and replace by updated data,
    * done in wrapper logic (Spring CachePut annotation doesn't support item lists)
    * TODO LATER save if no diff
    */
   @Override
   public List<DCResource> putAllDataInType(List<DCResource> resources, String modelType) {
      resources = delegate.putAllDataInType(resources, modelType);

      putInCache(resources);
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

      putInCache(resources);
      return resources;
   }

   /* (non-Javadoc)
    * @see org.oasis.datacore.rest.client.DatacoreCachedClient#getData(org.oasis.datacore.rest.api.DCResource)
    */
   //@CachePut(condition="#resource != null and (resourceCache.get(#resource.getUri) == null "
   //      + "or #resource.version != resourceCache.get(#resource.getUri).version)",
   //      value={"org.oasis.datacore.rest.api.DCResource"}, key="#resource.uri") // NO would never return cached object
   //@Cacheable(value={"org.oasis.datacore.rest.api.DCResource"}, key="buildUri(#modelType, #iri)")
   // so NOT Cacheable because returning from cache is triggered from HTTP 304 reponse (complex caching decision)
   @Override
   public DCResource getData(final DCResource resource) throws IllegalArgumentException {
      try { 
         final DCURI dcUri = UriHelper.parseUri(resource.getUri(), this.containerUrlString);
         return new AbstractCachedGetDataPerformer() {
            @Override
            public DCResource doGetData() {
               return delegate.getData(dcUri.getType(), dcUri.getId(), resource.getVersion());
            }
         }.performCachedGetData(resource.getUri());
      } catch (MalformedURLException | URISyntaxException e) {
         throw new IllegalArgumentException("Bad Resource URI " + resource.getUri(), e);
      }
   }

   /**
    * Same as TODO but with header version ETag
    * coming from cache
    */
   //@CachePut(condition="#resource != null and (resourceCache.get(#resource.getUri) == null "
   //      + "or #resource.version != resourceCache.get(#resource.getUri).version)",
   //      value={"org.oasis.datacore.rest.api.DCResource"}, key="#resource.uri") // NO would never return cached object
   //@Cacheable(value={"org.oasis.datacore.rest.api.DCResource"}, key="buildUri(#modelType, #iri)")
   // so NOT Cacheable because returning from cache is triggered from HTTP 304 reponse (complex caching decision)
   @Override
   public DCResource getData(final String modelType, final String iri) {
      String uri = buildUri(modelType, iri); // for caching
      return new AbstractCachedGetDataPerformer() {
         @Override
         public DCResource doGetData() {
            return delegate.getData(modelType, iri);
         }
      }.performCachedGetData(uri);
      //Response response = this.delegate.getData(type, iri, method, request);
      //DCResource resource = (DCResource) response.getEntity();
      //return resource;
   }

   /* (non-Javadoc)
    * @see org.oasis.datacore.rest.api.DatacoreApi#getData(java.lang.String, java.lang.String, java.lang.Long)
    */
   //@CachePut(condition="#resource != null and (resourceCache.get(#resource.getUri) == null "
   //      + "or #resource.version != resourceCache.get(#resource.getUri).version)",
   //      value={"org.oasis.datacore.rest.api.DCResource"}, key="#resource.uri") // NO would never return cached object
   //@Cacheable(value={"org.oasis.datacore.rest.api.DCResource"}, key="buildUri(#modelType, #iri)")
   // so NOT Cacheable because returning from cache is triggered from HTTP 304 reponse (complex caching decision)
   @Override
   public DCResource getData(final String modelType, final String iri, final Long version) {
      String uri = buildUri(modelType, iri); // for caching
      return new AbstractCachedGetDataPerformer() {
         @Override
         public DCResource doGetData() {
            return delegate.getData(modelType, iri, version);
         }
      }.performCachedGetData(uri);
      //Response response = this.delegate.getData(type, iri, method, request);
      //DCResource resource = (DCResource) response.getEntity();
      //return resource;
   }
   
   /**
    * Common code to all cached getData
    * @author mdutoo
    */
   private abstract class AbstractCachedGetDataPerformer {
      protected abstract DCResource doGetData();
      public DCResource performCachedGetData(String uri) {
         // NB. request is only for server etag, on client side it is done rather in interceptor
         String cacheId = getProject() + '.' + uri;
         try {
            DCResource resource = doGetData();

            // put in cache :
            if (resource != null) {
               resourceCache.put(cacheId, resource);
            } else {
               // server still has null it costs nothing to send it back,
               // so no ETag support in this case, but still empty cache
               resourceCache.evict(cacheId);
            }
            return resource;

         } catch (RedirectionException rex) {
            if (Status.NOT_MODIFIED.getStatusCode() == rex.getResponse().getStatus()) {
               // HTTP 304 (not modified) : get from cache
               ValueWrapper cachedDataWrapper = resourceCache.get(cacheId); // NB. ValueWrapper wraps cached null
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
      }
   }

   private void putInCache(List<DCResource> resources) {
      String projectPrefix = getProject() + '.';
      for (DCResource resource : resources) {
         resourceCache.put(projectPrefix + resource.getUri(), resource); // NB. if no error, resource can't be null
      }
   }
   private void putInCache(DCResource resource) {
      resourceCache.put(getProject() + '.' + resource.getUri(), resource); // NB. if no error, resource can't be null
   }

   /* (non-Javadoc)
    * @see org.oasis.datacore.rest.client.DatacoreCachedClient#deleteData(org.oasis.datacore.rest.api.DCResource)
    */
   @Override
   public void deleteData(DCResource resource) throws IllegalArgumentException {
      String modelType = resource.getModelType(); // NB. if null lets server explode
      String id = resource.getId();
      if (id == null) {
         // init id to make it easier to reuse POSTed & returned resources :
         // TODO rather make id transient ? or auto set id on unmarshall ??
         try { 
            id = UriHelper.parseUri(resource.getUri(), this.containerUrlString).getId();
         } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalArgumentException("Bad Resource URI " + resource.getUri(), e);
         }
      }
      this.deleteData(modelType, id, resource.getVersion());
      resource.setVersion(null); // doesn't exist anymore
   }

   /**
    * Same as deleteData(modelType, iri, version) but with header version ETag
    * coming from cache
    */
   @Override
   public void deleteData(String modelType, String iri) {
      delegate.deleteData(modelType, iri);

      // evict from cache :
      String uri = buildUri(modelType, iri);
      resourceCache.evict(getProject() + '.' + uri);
   }
   
   /**
    * Always evict (after invocation) and replace by updated data, using Spring CacheEvict annotation
    * done in wrapper logic (Spring CacheEvict needs full uri, no return value to use by Spring CachePut)
    * TODO LATER save if no diff
    */
   //@CacheEvict(value={"org.oasis.datacore.rest.api.DCResource"}, key="resource.uri") // NO key not fully known
   @Override
   public void deleteData(String modelType, String iri, Long version) {
      delegate.deleteData(modelType, iri, version);

      // evict from cache :
      String uri = buildUri(modelType, iri);
      resourceCache.evict(getProject() + '.' + uri);
   }


   /**
    * NOT IMPLEMENTED YET
    */
   @Override
   public List<DCResource> updateDataInTypeWhere(DCResource resourceDiff,
         String modelType, UriInfo uriInfo) {
      return this.delegate.updateDataInTypeWhere(resourceDiff, modelType, uriInfo);
   }
   /**
    * NOT IMPLEMENTED YET
    */
   @Override
   public void deleteDataInTypeWhere(String modelType, UriInfo uriInfo) {
      this.delegate.deleteDataInTypeWhere(modelType, uriInfo);
   }


   /**
    * Does the work of POST ; can't be done using Spring annotations
    */
   @Override
   public DCResource postDataInTypeOnGet(String modelType, String method,
         UriInfo uriInfo/*, @Context Request request*/) {
      DCResource resource = delegate.postDataInTypeOnGet(modelType, method, uriInfo);

      putInCache(resource);
      return resource;
   }

   /**
    * Same as TODO but with header version ETag
    * coming from cache
    */
   @Override
   public DCResource putPatchDeleteDataOnGet(String modelType, String iri, String method, UriInfo uriInfo) {
      DCResource resource = delegate.putPatchDeleteDataOnGet(modelType, iri, method, uriInfo);

      if (HttpMethod.DELETE.equals(method)) {
         // evict from cache :
         String uri = buildUri(modelType, iri);
         resourceCache.evict(getProject() + '.' + uri);
      } else { // PUT, PATCH
         putInCache(resource);
      }
      return resource;
   }
   /**
    * Does the work of PUT, PATCH, DELETE ; can't be done using Spring annotations
    */
   @Override
   public DCResource putPatchDeleteDataOnGet(String modelType, String iri,
         String method, Long version, UriInfo uriInfo) {
      DCResource resource = delegate.putPatchDeleteDataOnGet(modelType, iri, method, version, uriInfo);

      if (HttpMethod.DELETE.equals(method)) {
         // evict from cache :
         String uri = buildUri(modelType, iri);
         resourceCache.evict(getProject() + '.' + uri);
      } else { // PUT, PATCH
         putInCache(resource);
      }
      return resource;
   }
   
  	/* (non-Javadoc)
  	 * @see org.oasis.datacore.rest.api.DatacoreApi#findHistorizedResource(java.lang.String, java.lang.String, java.lang.Integer)
  	 */
  	@Override
	public DCResource findHistorizedResource(String modelType, String iri, Integer version) throws BadRequestException, NotFoundException {
		DCResource resource = this.delegate.findHistorizedResource(modelType, iri, version);
		return resource;
	}

   /* (non-Javadoc)
    * @see org.oasis.datacore.rest.api.DatacoreApi#findDataInType(java.lang.String, javax.ws.rs.core.UriInfo, java.lang.Integer, java.lang.Integer)
    */
   @Override
   public List<DCResource> findDataInType(String modelType, UriInfo uriInfo, Integer start, Integer limit, boolean debug) {
      //return delegate.findDataInType(type, uriInfo, start, limit, sort); // TODO cache ??
      throw new UnsupportedOperationException("Use rather findDataInType(String queryParams ...) on client side");
   }

   /* (non-Javadoc)
    * @see org.oasis.datacore.rest.api.DatacoreApi#findData(javax.ws.rs.core.UriInfo, java.lang.Integer, java.lang.Integer)
    */
   @Override
   public List<DCResource> findData(UriInfo uriInfo, Integer start, Integer limit) {
      //return delegate.findData(uriInfo, start, limit, sort); // TODO cache ??
      throw new NotImplementedException("Use rather findData(String queryParams ...) on client side");
   }

   /* (non-Javadoc)
    * @see org.oasis.datacore.rest.client.DatacoreClientApi#findDataInType(java.lang.String, org.oasis.datacore.rest.client.QueryParameters, java.lang.Integer, java.lang.Integer)
    */
   @Override
   public List<DCResource> findDataInType(String modelType, QueryParameters queryParams,
         Integer start, Integer limit) {
      return delegate.findDataInType(modelType, queryParams, start, limit);
   }
   /* (non-Javadoc)
    * @see org.oasis.datacore.rest.client.DatacoreClientApi#findData(org.oasis.datacore.rest.client.QueryParameters, java.lang.Integer, java.lang.Integer)
    */
   @Override
   public List<DCResource> findData(QueryParameters queryParams, Integer start,
         Integer limit) {
      return delegate.findData(queryParams, start, limit);
   }

   /* (non-Javadoc)
    * @see org.oasis.datacore.rest.api.DatacoreApi#queryDataInType(java.lang.String, java.lang.String, java.lang.String)
    */
   @Override
   public List<DCResource> queryDataInType(String modelType, String query, String language) {
      return delegate.queryDataInType(modelType, query, language); // TODO cache ??
   }

   /* (non-Javadoc)
    * @see org.oasis.datacore.rest.api.DatacoreApi#queryData(java.lang.String, java.lang.String)
    */
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
   
   
   ///////////////////////////////////////////////////////////////////////
   // HELPERS WITH PROJECT

   @Override
   public DCResource postDataInTypeInProject(DCResource r, String projectName) {
      return new SimpleRequestContextProvider<DCResource>() {
         protected DCResource executeInternal() {
            return postDataInType(r);
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DCRequestContextProvider.PROJECT, projectName).build());
   }
   @Override
   public DCResource putDataInTypeInProject(DCResource r, String projectName) {
      return new SimpleRequestContextProvider<DCResource>() {
         protected DCResource executeInternal() {
            return putDataInType(r);
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DCRequestContextProvider.PROJECT, projectName).build());
   }

   /*public String getBaseUrl() {
      return baseUrl;
   }

   public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
   }*/

}
