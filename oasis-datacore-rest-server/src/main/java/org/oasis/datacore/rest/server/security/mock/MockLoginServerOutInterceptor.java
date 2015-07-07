package org.oasis.datacore.rest.server.security.mock;

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * USED ONLY BY TESTS (not for now) i.e. fails if localauthdevmode=falls
 * Clears security context. Has to be called at the end of ALL requests received
 * by Datacore. 
 * Done as CXF interceptor because login is done as one.
 * NB. CXF calls it even if a Fault has happened (even though CXF has FaultOutInterceptor).
 * 
 * @author mdutoo
 *
 */

public class MockLoginServerOutInterceptor extends AbstractPhaseInterceptor<Message> {
   private static final Logger LOG = LogUtils.getLogger(MockLoginServerOutInterceptor.class);

   @Value("${datacore.localauthdevmode}")
   private boolean localauthdevmode;
   
   public MockLoginServerOutInterceptor() {
      super(Phase.POST_PROTOCOL);
      
      if (!localauthdevmode) {
         throw new IllegalArgumentException(this.getClass().getName() + " must never be used when localauthdevmode=false");
      }
   }

   @Override
   public void handleMessage(Message serverInRequestMessage) throws Fault {
      if (!localauthdevmode) {
         throw new IllegalArgumentException(this.getClass().getName() + " must never be used when localauthdevmode=false");
      }
      SecurityContextHolder.clearContext();
   }
   
}
