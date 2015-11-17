package org.oasis.datacore.rest.server.resource.mapping;

import java.util.List;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("datacore.server.linkedResourceChecker.local")
public class LocalDatacoreLinkedResourceChecker extends LinkedResourceChecker {

   /** Checks that linked Resource exists locally, WARNING prevents circular references for now */
   @Value("${datacore.server.linkedResourceCheck.local.exists}")
   private boolean checkLinkedResource = true;
   /** Checks that linked Resource exists locally and its types are compliant (with resource field type) */
   @Value("${datacore.server.linkedResourceCheck.local.types}")
   private boolean checkLinkedResourceTypes = false;
   /** Checks that linked Resource URI's model's types are compliant (with resource field type) */
   @Value("${datacore.server.linkedResourceCheck.local.modelTypes}")
   private boolean checkLinkedResourceModelTypes = true;

   @Autowired
   private DCModelService modelService;
   
   @Override
   public DCModelBase checkUriModel(DCURI dcUri, DCResourceField dcResourceField) throws ResourceParsingException {
      DCModelBase refModel = super.checkUriModel(dcUri, dcResourceField);
      
      if (modelService.getStorageModel(refModel) == null) {
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
   protected String getLinkType() {
      return "linked";
   }

   @Override
   protected List<String> checkLinkedResource(DCURI dcUri, DCModelBase refModel) throws ResourceParsingException {
      // (without rights ; only in checkMode, otherwise only warning)
      DCEntity linkedEntity = entityService.getByUriUnsecured(dcUri.toString(), refModel);
      if (linkedEntity == null) {
         // TODO rather still allow update / only warning ? & add /check API keeping it as error ??
         throw new ResourceParsingException("Can't find data of resource type " + refModel.getName()
               + " " + getLinkType() + " by resource Field of URI value " + dcUri.toString());
      }
      return linkedEntity.getTypes(); // TODO rather Set
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
