package org.oasis.datacore.rest.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This class helps client provide query parameters to Datacore proxy client.
 * It notably does URL encoding of values and preserves param order using an
 * additional list.
 * It is auto converted to and from String as per JAXRS "implicit" conversion,
 * see http://stackoverflow.com/questions/9520716/cxf-jaxrs-how-do-i-pass-date-as-queryparam
 * 
 * @author mdutoo
 *
 */
public class QueryParameters {

   private static final String DEFAULT_UTF8_ENCODING = "UTF-8";
   
   private Map<String,String> parameterMap = new HashMap<String,String>();
   private List<String> parameterNames = new ArrayList<String>(5);

   public QueryParameters() {
      
   }

   /**
    * NOT IMPLEMENTED
    * deserialization - not used for now (but would be if used on server side)
    * @param encodedQuery
    */
   public QueryParameters(String encodedQuery) {
      
   }

   /**
    * Adds a query parameter. Allows fluent consecutive calls.
    * TODO LATER maybe also allow to add JSON quoting, format DateTime according to model etc. ??
    * @param name
    * @param value operator followed by value then optional sort. Simple unquoted string value is accepted,
    * but ideally and formally value should be JSON (i.e. int, quoted string, array, map / Object...)
    * @return
    */
   public QueryParameters add(String name, String operatorValueSort) {
      if (this.parameterMap.containsKey(name)) {
         this.parameterNames.remove(name);
      }
      this.parameterMap.put(name, operatorValueSort);
      this.parameterNames.add(name); // add at list end
      return this;
   }

   /**
    * Removes a named query parameter. Allows fluent consecutive calls.
    * @param name
    * @return
    */
   public QueryParameters remove(String name) {
      if (this.parameterMap.containsKey(name)) {
         this.parameterMap.remove(name);
         this.parameterNames.remove(name);
      }
      return this;
   }
   
   /**
    * Ordered.
    * @return
    */
   public List<String> getParameterNames() {
      return new ArrayList<String>(this.parameterNames);
   }

   /**
    * serialization, to a=encodedAValue&b=encodedBValue form
    * where encoding is done by URLEncoder
    */
   public String toString() {
      if (this.parameterMap.isEmpty()) {
         return "";
      }
      
      StringBuilder sb = new StringBuilder();
      // serializing 
      for (String key : this.parameterNames) {
         sb.append(key);
         sb.append("=");
         String operatorValueSort = this.parameterMap.get(key);
         try {
            String encodedOperatorValueSort = URLEncoder.encode(operatorValueSort, DEFAULT_UTF8_ENCODING);
            sb.append(encodedOperatorValueSort);
         } catch (UnsupportedEncodingException e) {
            // should never happens, UTF-8 is always supported
            throw new RuntimeException(e);
         }
         sb.append("&");
      }
      // deleting last & (there is at least one)
      sb.deleteCharAt(sb.length() - 1);
      return sb.toString();
   }
   
}
