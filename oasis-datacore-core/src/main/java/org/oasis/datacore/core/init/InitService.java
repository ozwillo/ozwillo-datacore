package org.oasis.datacore.core.init;

import java.util.ArrayList;
import java.util.List;

import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Inits the registered initables (samples etc.) in the order they were registered (which, if
 * they register in @PostConstruct, can be controlled by @DependsOn).
 * 
 * 
 * Here's filling (sample) data must be done using InitService
 * (and not mere @PostConstruct or independent ApplicationListeners) :
 * 
 * Discussion on alternatives to trigger code at startup :
 * * implementing ApplicationListener<ContextRefreshedEvent> works, SAVE using client
 * on appserver (tomcat), because webapp startup has not ended when spring is still dispatching events
 * * using @PostConstruct works, SAVE using client (see below)
 * * implementing ApplicationContextAware DOES NOT WORK (too early in startup process)
 * 
 * Discussion on how Datacore is called :
 * * for now directly through injected DatacoreApiImpl, helped by shortcut / helper methods on DatacoreSampleBase
 * * (though it works in Junit tests) datacoreApiCachedClient (DatacoreClientApi) DOES NOT WORK
 * because it is too early, server is not yet deployed, which raises ConnectException
 * and adding @DependsOn({"datacoreApiImpl", "datacoreApiServer"}) triggers a deadlock :
 * 
 * ex. (using @PostConstruct) when AltTourismPlaceAddressSample does POST, on server side blocks on checking
 * whether the "markaInvestData" bean isSingleton() which calls getSingleton which is synchronized
 * on DefaultSingletonBeanRegistry.singletonObjects in the process of looking for its QueryHandlerRegistry extension :

   waiting for: ConcurrentHashMap<K,V>  (id=155)   
   DefaultListableBeanFactory(DefaultSingletonBeanRegistry).getSingleton(String, boolean) line: 184   
   DefaultListableBeanFactory(AbstractBeanFactory).isSingleton(String) line: 379 
   DefaultListableBeanFactory.doGetBeanNamesForType(Class<?>, boolean, boolean) line: 339 
   DefaultListableBeanFactory.getBeanNamesForType(Class<?>, boolean, boolean) line: 308   
   GenericApplicationContext(AbstractApplicationContext).getBeanNamesForType(Class<?>, boolean, boolean) line: 1166  
   SpringBeanLocator.getBeansOfType(Class<T>) line: 155  
   SpringBus(CXFBusImpl).getExtension(Class<T>) line: 108   
   JettyHTTPDestination.doService(ServletContext, HttpServletRequest, HttpServletResponse) line: 267  

 * while in main thread, init is still within the same DefaultSingletonBeanRegistry.getSingleton().
 * Seems related (since also talks of extension) to
 * https://mail-archives.apache.org/mod_mbox/cxf-users/201311.mbox/%3CA0BA72E3-9696-45EE-BA2D-916F76420769@apache.org%3E 
 * 
 * Using ApplicationListener instead works AT THE CONDITON that there isn't any MongoDB persistence at
 * spring init time (typically @PostConstruct, ex. MarkaInvestData), otherwise it emits
 * Spring mongo MappingContextEvent that are multicast toward ApplicationListener listeners which include
 * ApplicationListener-implementing AltTourismPlaceAddressSample and are gotten by :
 * ApplicationListener listener = beanFactory.getBean(listenerBeanName, ApplicationListener.class);
 * before being filtered out, which triggers AltTourismPlaceAddressSample init,
 * which triggers previous deadlock.
 * 
 * And to be compliant with dependencies, implementing ApplicationListener is restricted to the single
 * InitService, where samples to be inited must register on @PostConstruct in the order guided by their
 * @DependsOn annotations.
 * 
 * @author mdutoo
 *
 */
@Component
public class InitService implements ApplicationListener<ContextRefreshedEvent> {

   /** impl, to be able to modify it
    * TODO LATER extract interface */ 
   @Autowired
   private DataModelServiceImpl modelAdminService;
   @Autowired(required=false)
   @Qualifier("oasis.datacore.initService.enabled")
   private boolean enabled = true;

   private List<Initable> initables = new ArrayList<Initable>();
   
   
   @Override
   public void onApplicationEvent(ContextRefreshedEvent event) {
      if (modelAdminService.getModelMap().isEmpty() && enabled) {
         // NB. to rebootstrap, first empty models !

         if (initables != null) {
            for (Initable initable : initables) {
               initable.init();
            }
         }
      }
   }

   public void register(Initable bootstrappable) {
      this.initables.add(bootstrappable);
   }

}
