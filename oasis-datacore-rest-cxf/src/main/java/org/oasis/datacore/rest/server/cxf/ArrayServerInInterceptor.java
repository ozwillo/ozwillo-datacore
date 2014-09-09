package org.oasis.datacore.rest.server.cxf;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.ws.rs.HttpMethod;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helps supporting POST of single DCResource.
 * Finds out if, in incoming server request, what is POSTed at /dc/type is an array
 * or a mere single DCResource, and remembers it in CXF MessageContext
 * so that response can be adapted accordingly (using ArrayServerOutInterceptor).
 * To work, this also requires JacksonJsonProvider to ACCEPT_SINGLE_VALUE_AS_ARRAY.
 * NB. An alternative would be hacking this part of JacksonJsonProvider to remember
 * when an array is found in a threaded context. 
 * 
 * @author mdutoo
 *
 */

public class ArrayServerInInterceptor extends AbstractPhaseInterceptor<Message> {
   private static final Logger LOG = LogUtils.getLogger(ArrayServerInInterceptor.class);

   public static final String REQUEST_IS_JSON_ARRAY = ArrayServerInInterceptor.class.getName()
         + "requestIsJsonArray";
   
   private static final Pattern SLASH_DC_TYPE_PATTERN = Pattern.compile(
         "/+" + DatacoreApi.DC_TYPE_PATH.replaceAll("/", "/+")); // "/?dc/+type"
   
   /** Used to peek in JSON stream to know whether root is an array or not.
    * NB. can't be autowired because may be used for mocks (and not only actual server impl) */
   private ObjectMapper objectMapper;
   
   
   public ArrayServerInInterceptor() {
      super(Phase.PRE_UNMARSHAL);
   }

   @Override
   public void handleMessage(Message serverInRequestMessage) throws Fault {
      if (!isPostDcTypeOperation(serverInRequestMessage)
            || !MediaType.APPLICATION_JSON.isCompatibleWith(
                  MediaType.valueOf((String) serverInRequestMessage.get(Message.CONTENT_TYPE)))) {
         // NB. no org.apache.cxf.resource.operation.name yet
         return;
      }
      
      InputStream is = serverInRequestMessage.getContent(InputStream.class);
      if (!is.markSupported()) {
         LOG.warning("Can't attempt to support POST /dc/type of single DCResource value "
               + "because mark not supported on content input stream");
         return;
      }
      
      boolean requestIsJsonArray = false;
      is.mark(100);
      try {
         JsonParser jp = objectMapper.getFactory().createParser(is);
         
         // see ObjectMapper._initForReading()
         JsonToken t = jp.getCurrentToken();
         if (t == null) {
            t = jp.nextToken();
            if (t == null) {
               throw JsonMappingException.from(jp, "No content to map due to end-of-input");
            }
         }
         
         // looping while finding Object or Array
         // TODO what else, null ?? primitives ???
         while (t != JsonToken.START_OBJECT) {
            if (t == JsonToken.START_ARRAY) {
               requestIsJsonArray = true;
               break;
            }
            t = jp.nextToken();
         }
         serverInRequestMessage.put(REQUEST_IS_JSON_ARRAY, requestIsJsonArray);
      } catch (JsonParseException jpex) {
         LOG.log(Level.WARNING, "JSON parsing error while attempting to support POST /dc/type"
               + " of single DCResource value", jpex);
      } catch (IOException ioex) {
         LOG.log(Level.WARNING, "IO error while attempting to support POST /dc/type"
               + " of single DCResource value", ioex);
      } finally {
         try {
            is.reset();
         } catch (IOException e) {
            // silent
         }
      }
   }

   public static boolean isPostDcTypeOperation(Message serverInRequestMessage) {
      String requestHttpMethod = (String) serverInRequestMessage.get(Message.HTTP_REQUEST_METHOD);
      if (HttpMethod.POST.equals(requestHttpMethod)) {
         String requestUri = (String) serverInRequestMessage.get(Message.REQUEST_URI);
         // NB. no org.apache.cxf.resource.operation.name yet
         if (requestUri != null && SLASH_DC_TYPE_PATTERN.matcher(requestUri).find()) {
            return true;
         }
      }
      return false;
   }

   public ObjectMapper getObjectMapper() {
      return objectMapper;
   }

   public void setObjectMapper(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
   }

}
