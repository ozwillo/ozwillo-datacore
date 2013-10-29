package org.oasis.datacore.rest.client.cxf;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.oasis.datacore.rest.api.DatacoreApi;

/**
 * Used to put as bare query parameters what is provided in header param #queryParameters
 * 
 * @author mdutoo
 *
 */

public class QueryParametersClientOutInterceptor extends AbstractPhaseInterceptor<Message> {
   private static final Logger LOG = LogUtils.getLogger(QueryParametersClientOutInterceptor.class);
   
   
   public QueryParametersClientOutInterceptor() {
      super(Phase.SETUP);
   }

   @Override
   public void handleMessage(Message clientOutRequestMessage) throws Fault {
      List<String> queryParamList = (List<String>) CxfMessageHelper.getRequestHeaders(
            clientOutRequestMessage).get(DatacoreApi.QUERY_PARAMETERS);
      if (queryParamList != null && queryParamList.size() != 0) {
         String uri = (String) clientOutRequestMessage.get(Message.REQUEST_URI);
         String queryParams = queryParamList.get(0);
         if (queryParams != null && queryParams.length() != 0) {
            uri += (uri.contains("?") ? "&" : "?") + queryParams;
            
            // setting it everywhere :
            clientOutRequestMessage.put(Message.REQUEST_URI, uri);
            clientOutRequestMessage.put(Message.ENDPOINT_ADDRESS, uri);
            Map<Object,Object> requestContext = CxfMessageHelper.getRequestContext(clientOutRequestMessage);
            requestContext.put(Message.REQUEST_URI, uri);
            requestContext.put(Message.ENDPOINT_ADDRESS, uri);
            Exchange messageExchange = clientOutRequestMessage.getExchange();
            messageExchange.put(Message.REQUEST_URI, uri);
            messageExchange.put(Message.ENDPOINT_ADDRESS, uri);
            // NB. no in message yet
            
            // removing useless protocol header :
            CxfMessageHelper.getRequestHeaders(clientOutRequestMessage).remove(DatacoreApi.QUERY_PARAMETERS);
            /*@SuppressWarnings("unchecked")
            Map<String, List<String>> requestContextHeaders = (Map<String, List<String>>) requestContext.get(Message.PROTOCOL_HEADERS);
            requestContextHeaders.remove(DatacoreApi.QUERY_PARAMETERS);*/
         }
      }
   }

}
