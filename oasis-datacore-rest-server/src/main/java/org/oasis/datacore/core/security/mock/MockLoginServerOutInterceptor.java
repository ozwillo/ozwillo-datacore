package org.oasis.datacore.core.security.mock;

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.springframework.security.core.context.SecurityContextHolder;

/**
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
   
   public MockLoginServerOutInterceptor() {
      super(Phase.POST_PROTOCOL);
   }

   @Override
   public void handleMessage(Message serverInRequestMessage) throws Fault {
      SecurityContextHolder.clearContext();
   }
   
}
