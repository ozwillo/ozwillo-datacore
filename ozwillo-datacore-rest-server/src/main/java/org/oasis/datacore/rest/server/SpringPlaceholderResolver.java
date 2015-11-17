package org.oasis.datacore.rest.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanFactory;


/**
 * Allows to resolve spring properties placeholders in spring XML
 * (using <bean factory-bean="<this>" factory-method="resolveEmbeddedValue")
 * @author mdutoo
 *
 */
public class SpringPlaceholderResolver {

   @Autowired
   private AbstractBeanFactory beanFactory;
   
   public String resolveEmbeddedValue(String value) {
      return beanFactory.resolveEmbeddedValue(value);
   }
   
}
