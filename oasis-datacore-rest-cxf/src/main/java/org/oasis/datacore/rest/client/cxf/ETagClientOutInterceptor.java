package org.oasis.datacore.rest.client.cxf;

import java.util.logging.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;

/**
 * Used to set If-None-Match=etag header on GET request
 * NB. not as CXF RequestHandler because can't enrich request (nor maybe work on client side),
 * not as JAXRS 2 RequestFilter because not supported yet (??)
 *
 * @author mdutoo
 *
 */
public class ETagClientOutInterceptor extends AbstractPhaseInterceptor<Message> {
   private static final Logger LOG = LogUtils.getLogger(ETagClientOutInterceptor.class);

   // TODO static object & list etag method names...

   @Autowired
   @Qualifier("datacore.rest.client.cache.rest.api.DCResource")
   private Cache resourceCache; // EhCache getNativeCache

   @Value("${datacoreApiClient.containerUrl}") //:http://data-test.oasis-eu.org/
   private String containerUrl;

   public ETagClientOutInterceptor() {
      super(Phase.SETUP); // TODO orig SETUP ; PREPARE_SEND_ENDING NOOOOOOOOOO
   }

   public ETagClientOutInterceptor(Cache cache, String containerUrl) {
      this();
      this.resourceCache = cache;
      this.containerUrl = containerUrl;
   }

   @Override
   public void handleMessage(Message clientOutRequestMessage) throws Fault {
      String operationName = (String) clientOutRequestMessage.getExchange().get("org.apache.cxf.resource.operation.name");
      if (operationName == null) {
         // when server out i.e. response
         return;
      }

      if ("getData".equals(operationName)) {
         if (CxfMessageHelper.getHeaderString(clientOutRequestMessage, HttpHeaders.IF_NONE_MATCH) == null) {
            // GET : send If-None-Match=version ETag precondition header if none yet
            ////Map<Object,Object> requestContext = getRequestContext(clientOutRequestMessage);
            String endpointUri = CxfMessageHelper.getUri(clientOutRequestMessage);
            String uri = toContainerUrl(endpointUri);
            //DCData cachedData = cache.get(uri);
            ValueWrapper cachedResourceWrapper = resourceCache.get(uri); // NB. ValueWrapper wraps cached null
            if (cachedResourceWrapper != null) {
               DCResource cachedResource = (DCResource) cachedResourceWrapper.get();
               if (cachedResource != null) {
                  Long cachedResourceVersion = cachedResource.getVersion();
                  if (cachedResourceVersion != null) {
                     String etag = cachedResourceVersion.toString();
                     CxfMessageHelper.setHeader(clientOutRequestMessage, HttpHeaders.IF_NONE_MATCH, etag);
                  } // else degraded way of asking to always receive full Resource 
               } // else cached null, but if server still has null it costs nothing
               // to send it back, so no ETag support in this case
            } // else no cache, optimization not possible, don't send etag header
            // then on response, CachedClientProviderImpl (MessageBodyReader) caches returned data
         } // else don't override explicitly set version ETag header

      } else if ("deleteData".equals(operationName)
            || ("putPatchDeleteDataOnGet".equals(operationName) && HttpMethod.DELETE.equals(
                  ((String) CxfMessageHelper.getJaxrsParameter(clientOutRequestMessage, "method")).toUpperCase()))) {
         if (CxfMessageHelper.getHeaderString(clientOutRequestMessage, HttpHeaders.IF_MATCH) == null) {
            // DELETE : send If-Match=version ETag precondition header if none yet
            ///Map<Object,Object> requestContext = getRequestContext(clientOutRequestMessage);
            String endpointUri = CxfMessageHelper.getUri(clientOutRequestMessage);
            String uri = toContainerUrl(endpointUri);
            //DCData cachedData = cache.get(uri);
            ValueWrapper cachedResourceWrapper = resourceCache.get(uri); // NB. ValueWrapper wraps cached null
            if (cachedResourceWrapper != null) {
               DCResource cachedResource = (DCResource) cachedResourceWrapper.get();
               String etag = cachedResource.getVersion().toString();
               if (cachedResource != null) {
                  if (cachedResource.getVersion() != null) { // TODO should not happen
                     CxfMessageHelper.setHeader(clientOutRequestMessage, HttpHeaders.IF_MATCH, etag);
                  } else { // trying to delete probably non existing Resource (without version)
                     throw new Fault(new IllegalArgumentException( // or any non-Fault exception, else blocks in
                           // abstractClient.checkClientException() (waits for missing response code)
                           // see http://stackoverflow.com/questions/8316354/cxf-ws-interceptor-stop-processing-respond-with-fault
                           "Resource to delete should have a non null version, "
                           + "otherwise it does (probably) not exist"),
                           Fault.FAULT_CODE_CLIENT);
                  }
               }
            } else {
               throw new Fault(
                     new IllegalArgumentException( // or any non-Fault exception, else blocks in
                     // abstractClient.checkClientException() (waits for missing response code)
                     // see http://stackoverflow.com/questions/8316354/cxf-ws-interceptor-stop-processing-respond-with-fault
                     "Can't delete a Data without having (gotten) and cached it first, "
                     + "so its version can be send as deletion precondition"),
                     Fault.FAULT_CODE_CLIENT);
            }
         } // else don't override explicitly set version ETag header

      }/* else if ("postDataInType".equals(method.getName())) {
         // see AbstractClient.createMessage() (from ClientProxyImpl.doChainedInvocation()) l.921 : m.setContent(List.class, getContentsList(body));
         List<?> contentList = clientOutMessage.getContent(List.class);
         DCData postedData = (DCData) contentList.get(0);
         Long etag = postedData.getVersion();
         if (etag != null) {
            setETag(clientOutMessage, etag.toString());
         } // else creation (or forgotten, in which case the server will abort TODO THIS IS TOO MUCH !!!!!!!!!!!!!!!!!!!!!)

      } else if ("postAllDataInType".equals(method.getName())) {
         // see AbstractClient.createMessage() (from ClientProxyImpl.doChainedInvocation()) l.921 : m.setContent(List.class, getContentsList(body));
         List<?> contentList = clientOutMessage.getContent(List.class);
         StringBuilder sb = new StringBuilder();
         for (Object dcData : contentList) {
            if (dcData != null) {
               sb.append(((DCData) dcData).getVersion().toString());
            } // else creation (or forgotten, see below)
            sb.append(','); // to avoid the possibility of different version numbers producing the same concatenation
         }
         if (sb.length() > contentList.size()) {
            // there is at least an existing data to tag
            sb.deleteCharAt(sb.length() - 1);
            int etag = sb.toString().hashCode();
            setETag(clientOutMessage, Integer.toString(etag));
         } // else creation (or forgotten, in which case the server will abort TODO THIS IS TOO MUCH !!!!!!!!!!!!!!!!!!!!!)
      }*/
      // TODO LATER2 list ??
   }

   private String toContainerUrl(String endpointUri) {
      try {
         return UriHelper.toContainerUrl(endpointUri, this.containerUrl);
      } catch (Exception e) {
         // should not happen
         throw new Fault(
               new IllegalArgumentException( // or any non-Fault exception, else blocks in
               // abstractClient.checkClientException() (waits for missing response code)
               // see http://stackoverflow.com/questions/8316354/cxf-ws-interceptor-stop-processing-respond-with-fault
               "Endpoint URI is not an URL : " + endpointUri, e),
               Fault.FAULT_CODE_CLIENT);
      }
   }

}
