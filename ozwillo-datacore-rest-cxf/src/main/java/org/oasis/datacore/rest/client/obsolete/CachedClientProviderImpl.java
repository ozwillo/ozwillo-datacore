package org.oasis.datacore.rest.client.obsolete;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.oasis.datacore.rest.api.DCResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;


/**
 * NOT USED (might have been to put returned object(s) in cache, but operation not known)
 * 
 * WARNING MessageBodyWriter can't be used on request when no body ex. GET !
 * @author mdutoo
 * @obsolete not used
 *
 */
public class CachedClientProviderImpl implements
      MessageBodyWriter<Object>, MessageBodyReader<Object> {

   private MessageBodyWriter<Object> delegateWriter;
   private MessageBodyReader<Object> delegateReader;

   @Autowired // TODO NO or autowire delegates
   @Qualifier("datacore.rest.client.cache.rest.api.DCResource")
   private Cache resourceCache; // EhCache getNativeCache
   
   public CachedClientProviderImpl() {
      
   }
   
   public boolean isWriteable(Class<?> type, Type genericType,
         Annotation[] annotations, MediaType mediaType) {
      return delegateWriter.isWriteable(type, genericType, annotations,
            mediaType);
   }
   
   public long getSize(Object t, Class<?> type, Type genericType,
         Annotation[] annotations, MediaType mediaType) {
      return delegateWriter.getSize(t, type, genericType, annotations,
            mediaType);
   }
   
   public void writeTo(Object t, Class<?> type, Type genericType,
         Annotation[] annotations, MediaType mediaType,
         MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
         throws IOException, WebApplicationException {

      // CAN'T BE USED since called only when there is a body (so not on DELETE, GET 304 response)
      
      delegateWriter.writeTo(t, type, genericType, annotations, mediaType,
            httpHeaders, entityStream);
   }
   
   public boolean isReadable(Class<?> type, Type genericType,
         Annotation[] annotations, MediaType mediaType) {
      return delegateReader.isReadable(type, genericType, annotations,
            mediaType);
   }
   
   public Object readFrom(Class<Object> type, Type genericType,
         Annotation[] annotations, MediaType mediaType,
         MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
         throws IOException, WebApplicationException {
      Object res = delegateReader.readFrom(type, genericType, annotations, mediaType,
            httpHeaders, entityStream);
      
      List<String> etagList = httpHeaders.get(HttpHeaders.ETAG);
      if (etagList != null) {
         // must cache
         if (List.class.isAssignableFrom(type)) {
            // TODO LATER cache data list-returning methods (multi-GET ; though not find / queries ??)
         } else {
            // case of getData method :
            DCResource resource = (DCResource) res;
            resourceCache.put(resource.getUri(), resource);
         }
      }
      // TODO LATER evict & cache also DCData (LATER & list) returned by POST, PUT, PATCH (?)
      // TODO LATER evict DCData at DELETE (?)
      
      /*if ("getData".equals(method.getName())) {
         String uri = (String) requestContext.get("org.apache.cxf.request.uri");
         // see AbstractClient.createMessage() (de ClientProxyImpl.doChainedInvocation()) l.921 : m.setContent(List.class, getContentsList(body));
         List contentList = clientInResponseMessage.getContent(List.class);
         DCData data = (DCData) contentList.get(0);
         //String key = type + '/' + iri;
         //DCData cachedData = cache.get(uri);
         ValueWrapper cachedDataWrapper = cache.get(uri); // NB. ValueWrapper wraps cached null
         if (cachedDataWrapper != null) {
            DCData cachedData = (DCData) cachedDataWrapper.get();
            if (cachedData != null
                  && cachedData.getVersion() != null) { // TODO should not happen
               setETag(clientInResponseMessage, cachedData.getVersion().toString());
            }
         }
         // then in clientIn / response interceptor, data should be cached
         
      }*//* else if ("postDataInType".equals(method.getName())) {
         // see AbstractClient.createMessage() (de ClientProxyImpl.doChainedInvocation()) l.921 : m.setContent(List.class, getContentsList(body));
         List<?> contentList = clientOutMessage.getContent(List.class);
         DCData postedData = (DCData) contentList.get(0);
         Long etag = postedData.getVersion();
         if (etag != null) {
            setETag(clientOutMessage, etag.toString());
         } // else creation (or forgotten, in which case the server will abort TODO THIS IS TOO MUCH !!!!!!!!!!!!!!!!!!!!!)
         
      } else if ("postAllDataInType".equals(method.getName())) {
         // see AbstractClient.createMessage() (de ClientProxyImpl.doChainedInvocation()) l.921 : m.setContent(List.class, getContentsList(body));
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
      // TODO list
      
      return res;
   }
   
   
   /**
    * 
    * @param delegate provider to set as delegate reader & writer
    */
   public void setDelegate(Object delegate) {
      this.setDelegateWriter((MessageBodyWriter<Object>) delegate);
      this.setDelegateReader((MessageBodyReader<Object>) delegate);
   }
   
   public MessageBodyWriter<Object> getDelegateWriter() {
      return delegateWriter;
   }
   public void setDelegateWriter(MessageBodyWriter<Object> delegateWriter) {
      this.delegateWriter = delegateWriter;
   }
   public MessageBodyReader<Object> getDelegateReader() {
      return delegateReader;
   }
   public void setDelegateReader(MessageBodyReader<Object> delegateReader) {
      this.delegateReader = delegateReader;
   }

}
