package org.oasis.datacore.rest.server.resource.mapping;

import java.util.List;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCModel;
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
      
      if (modelService.getStorageModel(refModel.getName()) == null) {
         throw new ResourceParsingException("Resource type " + refModel.getName()
               + " " + getLinkType() + " by resource Field of URI value " + dcUri.toString()
               + " has a model that doesn't define any storage");
      }
      
      return refModel;
   }
   
   @Override
   protected String getLinkType() {
      return "linked";
   }

   @Override
   protected List<String> checkLinkedResource(DCURI dcUri, DCModelBase refModelBase) throws ResourceParsingException {
      DCModel refModel = (DCModel) refModelBase; // checked before
      
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
