package org.oasis.datacore.rest.server.resource.mapping;

import java.util.List;

import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("datacore.server.linkedResourceChecker.external")
public class ExternalDatacoreLinkedResourceChecker extends LinkedResourceChecker {

   /** LATER will check that linked external Datacore Resource URI returns HTTP 200 */
   @Value("${datacore.server.linkedResourceCheck.external.exists}")
   private boolean checkLinkedResource = false;
   /** LATER will check linked external Datacore Resource types */
   @Value("${datacore.server.linkedResourceCheck.external.types}")
   private boolean checkLinkedResourceTypes = false;
   /** checks linked external Datacore Resource URI model types */
   @Value("${datacore.server.linkedResourceCheck.external.modelTypes}")
   private boolean checkLinkedResourceModelTypes = true;

   @Autowired
   private DCModelService modelService;
   
   @Override
   protected String getLinkType() {
      return "linked from external Datacore";
   }

   @Override
   public DCModelBase checkUriModel(DCURI dcUri, DCResourceField dcResourceField) throws ResourceParsingException {
      DCModelBase refModel = super.checkUriModel(dcUri, dcResourceField);

      if (modelService.getStorageModel(refModel.getName()) == null) {
         // TODO LATER OPT client side might deem it a data health / governance problem,
         // and put it in the corresponding inbox
         throw new ResourceParsingException("Resource model type " + refModel.getName()
               + " " + getLinkType() + " by resource Field of URI value " + dcUri.toString()
               + " has a model that doesn't define any storage, which is not allowed "
               + "(would by highly inefficient). Maybe it had one at some point and this model "
               + "(and its inherited mixins) has changed since (only in test, in which case "
               + "the missing model must first be created again before patching the entity).");
      }
      
      return refModel;
   }

   @Override
   protected List<String> checkLinkedResource(DCURI dcUri, DCModelBase refModel) throws ResourceParsingException {
      // TODO LATER OPT check using (dynamic ??) CXF Datacore client that propagates auth
      // TODO LATER OPT2 store and check also version, to allow consistent references ??
      throw new UnsupportedOperationException("Not implemented yet");
   }
   
   @Override
   public boolean checkLinkedResourceExists() {
      return checkLinkedResource;
   }

   @Override
   public boolean checkLinkedResourceTypes() {
      return checkLinkedResourceTypes;
   }

   @Override
   public boolean checkLinkedResourceModelTypes() {
      return checkLinkedResourceModelTypes;
   }

}
