package org.oasis.datacore.core.security.mock;

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Clears security context.
 * Done as CXF interceptor because login is done as one.
 * 
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
