package org.oasis.datacore.rest.server.cxf;

import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.oasis.datacore.rest.api.DCResource;

/**
 * Helps supporting POST of single DCResource.
 * For more details, see ArrayServerInInterceptor.
 * NB. Alternative : do it in DatacoreApiImpl.postAllData* using injected CXF-specific MessageContext
 * 
 * @author mdutoo
 *
 */

public class ArrayServerOutInterceptor extends AbstractPhaseInterceptor<Message> {
   private static final Logger LOG = LogUtils.getLogger(ArrayServerOutInterceptor.class);
   
   public ArrayServerOutInterceptor() {
      super(Phase.PRE_MARSHAL);
   }

   @Override
   public void handleMessage(Message serverOutResponseMessage) throws Fault {
      Message serverInRequestMessage = serverOutResponseMessage.getExchange().getInMessage();
      if (!ArrayServerInInterceptor.isPostDcTypeOperation(serverInRequestMessage)) {
         // NB. other failed alternatives :
         // org.apache.cxf.jaxrs.model.OperationResourceInfo has java method & uriTemplate
         // org.apache.cxf.resource.operation.name is DatacoreApiImpl#postAllDataInType
         // (though only postAllDataInType on client side because resource impl is not known there) 
         return;
      }
      
      Object requestIsJsonArray = serverOutResponseMessage.getExchange().getInMessage()
            .get(ArrayServerInInterceptor.REQUEST_IS_JSON_ARRAY);
      if (requestIsJsonArray == null) {
         LOG.warning("requestIsJsonArray was not set, probably because of error "
               + "in ArrayServerInInterceptor");
         return;
      }
      
      if (!((Boolean) requestIsJsonArray).booleanValue()) {
         // request had no array BUT was deserialized by Jackson as one
         // so let's remove the array here

         MessageContentsList objs = MessageContentsList.getContentsList(serverOutResponseMessage);
         if (objs == null || objs.size() == 0) {
             return;
         }
         Object resFound = objs.get(0);
         if (resFound instanceof Response) {
            // result is already a response (built in server impl code, ex. to return custom status)
            Response response = (Response) resFound;
            Object resEntityFound = response.getEntity();

            if (resEntityFound instanceof List<?>) {
               @SuppressWarnings("unchecked")
               List<DCResource> resEntityList = (List<DCResource>) resEntityFound;
               // let's replace DCResource list by its first item as entity :
               ResponseBuilder rb = Response.fromResponse(response);
               rb.entity(resEntityList.get(0));
               objs.remove(0);
               objs.add(0, rb.build());

               //[Monitoring] Extract model type of the entity passed back in response
               serverOutResponseMessage.getExchange().put("res.model", resEntityList.get(0).getModelType());          
            } // else should not happen
            
         } else if (resFound instanceof List<?>) {
            // result entity is a list (ex. in mock)
            @SuppressWarnings("unchecked")
            List<Object> resEntityList = (List<Object>) resFound;
            // let's replace DCResource list by its first item as entity :
            objs.remove(0);
            objs.add(0, resEntityList.get(0));
         }
      }
   }
   
}
