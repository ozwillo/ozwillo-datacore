package org.oasis.datacore.rest.client.cxf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.cxf.message.Message;


/**
 * Helper for working with various CXF processing info in map-like Message(Context) & Exchange 
 * 
 * @author mdutoo
 *
 */
public class CxfMessageHelper {
   
   public static String getUri(Message clientOutRequestMessage) {
      return (String) clientOutRequestMessage.get(Message.REQUEST_URI);
   }
   
   public static Object getJaxrsParameter(Message clientOutRequestMessage, String name) {
      @SuppressWarnings("unchecked")
      Map<String,Object> jaxrsParameters = (Map<String, Object>) clientOutRequestMessage.get("jaxrs.template.parameters");
      return jaxrsParameters.get(name);
   }

   public static Map<Object, Object> getRequestContext(Message message) {
      Message clientOutRequestMessage = message.getExchange().getOutMessage(); // in case of inMessage
      Map<?,?> invocationContext = (Map<?,?>) clientOutRequestMessage.get(Message.INVOCATION_CONTEXT);
      @SuppressWarnings("unchecked")
      Map<Object,Object> requestContext = (Map<Object,Object>) invocationContext.get("RequestContext");
      /*OperationResourceInfo ori = (OperationResourceInfo) requestContext.get("org.apache.cxf.jaxrs.model.OperationResourceInfo");
      method = ori.getAnnotatedMethod();*/
      return requestContext;
   }

   public static void setHeader(Message clientOutRequestMessage, String name, String value) {
      ArrayList<String> headerList = new ArrayList<String>(1);
      headerList.add(value);
      getRequestHeaders(clientOutRequestMessage).put(name, headerList); // (though HTTP headers are case-insensitive per spec)
   }

   public static Map<String, List<String>> getRequestHeaders(Message clientOutRequestMessage) {
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
   public static String getHeaderString(Message clientInResponseMessage, String header) {
      Map<?,?> httpHeaders = (Map<?, ?>) clientInResponseMessage.get("org.apache.cxf.message.Message.PROTOCOL_HEADERS");
      @SuppressWarnings("unchecked")
      List<String> httpHeaderList = (List<String>) httpHeaders.get(header);
      if (httpHeaderList != null && httpHeaderList.size() != 0) {
         return httpHeaderList.get(0);
      }
      return null;
   }
  
}
