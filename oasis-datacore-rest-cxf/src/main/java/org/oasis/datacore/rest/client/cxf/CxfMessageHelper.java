package org.oasis.datacore.rest.client.cxf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;


/**
 * Helper for working with various CXF processing info in map-like Message(Context) & Exchange 
 * 
 * @author mdutoo
 *
 */
public class CxfMessageHelper {
   
   private static final String CTX_QUERY_PARAMETERS = "datacore.cxf.queryParameters";
   
   public static String getUri(Map<String, Object> clientOutRequestMessage) {
      return (String) clientOutRequestMessage.get(Message.REQUEST_URI);
   }
   
   /**
    * NB. for query parameters, rather use getSingleParameterValue()
    * @param clientOutRequestMessage
    * @param name
    * @return
    */
   public static Object getJaxrsParameter(Map<String, Object> clientOutRequestMessage, String name) {
      @SuppressWarnings("unchecked")
      Map<String,Object> jaxrsParameters = (Map<String, Object>) clientOutRequestMessage.get("jaxrs.template.parameters");
      return jaxrsParameters.get(name);
   }
   
   public static String getSingleParameterValue(Map<String, Object> clientInResponseMessage, String paramName) {
      MultivaluedMap<String, String> values = getQueryParameters(clientInResponseMessage);
      if (values == null) {
         return null; // to avoid npex, if applied without REST context ex. in ResourceEntityMapping toResources()
      }
      List<String> value = values.get(paramName);
      if (value == null || value.isEmpty()) {
         return null;
      }
      return value.get(value.size() - 1);
   }
   
   /** caches in message */
   public static MultivaluedMap<String, String> getQueryParameters(Map<String, Object> clientInRequestMessage) {
      @SuppressWarnings("unchecked")
      MultivaluedMap<String, String> queryParameters = (MultivaluedMap<String, String>) clientInRequestMessage.get(CTX_QUERY_PARAMETERS);
      if (queryParameters == null) {
         //queryParameters = new UriInfoImpl(clientInRequestMessage).getQueryParameters();
         String queryString = (String) clientInRequestMessage.get(Message.QUERY_STRING);
         if (queryString != null) {
            queryParameters = JAXRSUtils.getStructuredParams((String) queryString, 
                  "&", true, true);
            clientInRequestMessage.put(CTX_QUERY_PARAMETERS, queryParameters);
         } // else not in CXF case
      }
      return queryParameters;
   }

   /*public static Map<Object, Object> getRequestContext(Message message) {
      Message clientOutRequestMessage = message.getExchange().getOutMessage(); // in case of inMessage
      Map<?,?> invocationContext = (Map<?,?>) clientOutRequestMessage.get(Message.INVOCATION_CONTEXT);
      @SuppressWarnings("unchecked")
      Map<Object,Object> requestContext = (Map<Object,Object>) invocationContext.get("RequestContext");
      return requestContext;
   }*/

   public static void setHeader(Map<String, Object> clientOutRequestMessage, String name, String value) {
      ArrayList<String> headerList = new ArrayList<String>(2);
      headerList.add(value);
      getRequestHeaders(clientOutRequestMessage).put(name, headerList); // (though HTTP headers are case-insensitive per spec)
   }

   public static Map<String, List<String>> getRequestHeaders(Map<String, Object> clientOutRequestMessage) {
      @SuppressWarnings("unchecked")
      Map<String, List<String>> requestHeaders = (Map<String, List<String>>) clientOutRequestMessage.get(Message.PROTOCOL_HEADERS);
      if (requestHeaders == null) {
         requestHeaders = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
      }
      clientOutRequestMessage.put(Message.PROTOCOL_HEADERS, requestHeaders);
      return requestHeaders;
   }
   
   /**
    * Returns a single header, such as :
    * * ETag (HttpHeaders.ETAG), If-Match, If-None-Match ; An ETag header can only contain one ETag value
    * (see http://tools.ietf.org/html/draft-ietf-httpbis-p4-conditional-14#section-2.2 )
    * @param clientInResponseMessage
    * @param header ex. HttpHeaders.ETAG
    * @return
    */
   public static String getHeaderString(Map<String, Object> clientInResponseMessage, String header) {
      Map<?,?> httpHeaders = (Map<?, ?>) clientInResponseMessage.get(Message.PROTOCOL_HEADERS);
      if (httpHeaders == null) {
         return null; // to avoid npex, if applied without REST context ex. in ResourceEntityMapping toResources()
      }
      @SuppressWarnings("unchecked")
      List<String> httpHeaderList = (List<String>) httpHeaders.get(header);
      if (httpHeaderList != null && httpHeaderList.size() != 0) {
         return httpHeaderList.get(0);
      }
      return null;
   }

   public static boolean isCxfMessage(Map<String, Object> requestContext) {
      return requestContext.containsKey(Message.PROTOCOL_HEADERS); // or another one
   }
  
}
