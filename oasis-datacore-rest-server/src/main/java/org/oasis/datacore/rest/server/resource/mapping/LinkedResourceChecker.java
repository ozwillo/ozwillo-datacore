package org.oasis.datacore.rest.server.resource.mapping;

import java.util.List;
import java.util.Set;

import org.oasis.datacore.core.entity.EntityService;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class LinkedResourceChecker {
   
   @Autowired
   private DCModelService modelService;

   /** TODO replace by resourceService to check rights on refs (?!?) */
   @Autowired
   protected EntityService entityService;
   
   /** for error message only */
   protected abstract String getLinkType();

   protected abstract List<String> checkLinkedResource(DCURI dcUri, DCModelBase refModel)
         throws ResourceParsingException;

   /**
    * checkUriModel(), then resource exists if checkLinkedResource(Types)(), then checkTyping()
    * @param dcUri
    * @param dcResourceField
    * @throws ResourceParsingException
    */
   public void check(DCURI dcUri, DCResourceField dcResourceField)
         throws ResourceParsingException {

      // check type :
      DCModelBase refModel = checkUriModel(dcUri, dcResourceField);
      
      List<String> linkedResourceTypes = null; // TODO rather Set
      if (checkLinkedResourceExists() || checkLinkedResourceTypes()) {
         // (without rights ; only in checkMode, otherwise only warning)
         linkedResourceTypes = checkLinkedResource(dcUri, refModel);
      }
      
      checkTyping(dcUri, dcResourceField, refModel, linkedResourceTypes);
   }
   
   /**
    * Override it to check storage
    * @param dcUri
    * @param dcResourceField
    * @return
    * @throws ResourceParsingException
    */
   public DCModelBase checkUriModel(DCURI dcUri, DCResourceField dcResourceField) throws ResourceParsingException {
      DCModelBase refModel = modelService.getModelBase(dcUri.getType()); // TODO LATER from cached model ref in DCURI
      if (refModel == null) {
         throw new ResourceParsingException("Resource " + dcUri + " is " + getLinkType() + " in "
               + dcResourceField.getResourceType() + "-typed \"" + dcResourceField.getName()
               + "\" field but this type's model does not exist. Maybe it did at some point but it "
               + "(and its inherited mixins) has changed since (only in test, in which case "
               + "the missing model must first be created again before patching the entity).");
      }
      return refModel;
   }

   /**
    * 
    * @param dcUri
    * @param dcResourceField
    * @param valueModelOrMixin
    * @param linkedResourceTypes must be there if checkLinkedResourceTypes()
    * @throws ResourceParsingException
    */
   public void checkTyping(DCURI dcUri, DCResourceField dcResourceField,
         DCModelBase valueModelOrMixin, List<String> linkedResourceTypes)
         throws ResourceParsingException {

      if (checkLinkedResourceTypes()) {
         // check linked Resource model type using linkedEntity's types :
         if (!linkedResourceTypes.contains(dcResourceField.getResourceType())) { // TODO isCompatibleWith
            throw new ResourceParsingException("Resource " + dcUri + " is " + getLinkType() + " in "
                  + dcResourceField.getResourceType() + "-typed \"" + dcResourceField.getName()
                  + "\" field but has no compatible type but only " + linkedResourceTypes);
         }
         /*if (!dcUri.getType().equals(((DCResourceField) dcField).getResourceType())) {
         //if (!modelService.hasType(refEntity, ((DCResourceField) dcField).getTypeConstraints())) {
            throw new ResourceParsingException("Target resource referenced by resource Field of URI value "
                  + dcUri + " is not of required type " + ((DCResourceField) dcField).getResourceType());
            // does not match type constraints TODO LATER
         }*/
         
      } else if (checkLinkedResourceModelTypes()) {
         // check linked Resource model type using its URI :
         Set<String> linkedUriModelTypes = valueModelOrMixin.getGlobalMixinNames();
         if (!valueModelOrMixin.getName().equals(dcResourceField.getResourceType()) &&
               !linkedUriModelTypes.contains(dcResourceField.getResourceType())) { // TODO isCompatibleWith
            throw new ResourceParsingException("Resource " + dcUri + " is " + getLinkType() + " in "
                  + dcResourceField.getResourceType() + "-typed \"" + dcResourceField.getName()
                  + "\" field but its model " +  valueModelOrMixin.getName()
                  + " has no compatible type but only (in addition to itself) "
                  + linkedUriModelTypes);
         }
      }
   }

   /** conf */
   public abstract boolean checkLinkedResourceExists();
   /** conf */
   public abstract boolean checkLinkedResourceTypes();
   /** conf */
   public abstract boolean checkLinkedResourceModelTypes();
   
}
