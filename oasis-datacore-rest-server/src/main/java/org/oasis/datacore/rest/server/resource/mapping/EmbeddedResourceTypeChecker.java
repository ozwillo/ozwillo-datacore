package org.oasis.datacore.rest.server.resource.mapping;

import java.util.List;

import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("datacore.server.linkedResourceChecker.embedded")
public class EmbeddedResourceTypeChecker extends LinkedResourceChecker {

   /** USELESS */
   ///@Value("${datacore.server.linkedResourceCheck.embedded.exists}")
   private boolean checkLinkedResource = false;
   /** Checks that sub Resource types are compliant (with resource field type) */
   @Value("${datacore.server.linkedResourceCheck.embedded.types}")
   private boolean checkLinkedResourceTypes = false;
   /** Checks that sub Resource URI's model's types are compliant (with resource field type) */
   @Value("${datacore.server.linkedResourceCheck.embedded.modelTypes}")
   private boolean checkLinkedResourceModelTypes = true;
   
   @Override
   protected String getLinkType() {
      return "embedded";
   }

   @Override
   protected List<String> checkLinkedResource(DCURI dcUri, DCModelBase refModel) throws ResourceParsingException {
      throw new UnsupportedOperationException("Embedded resource already exists, "
            + "don't use check() but rather other methods");
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
