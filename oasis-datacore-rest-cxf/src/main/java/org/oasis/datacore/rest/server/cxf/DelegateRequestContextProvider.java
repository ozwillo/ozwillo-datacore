package org.oasis.datacore.rest.server.cxf;

import java.util.Map;

import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.RequestContextProviderBase;

public class DelegateRequestContextProvider extends RequestContextProviderBase {

   protected DCRequestContextProvider delegate;

   public DelegateRequestContextProvider(DCRequestContextProvider requestContextProvider) {
      this.delegate = requestContextProvider;
   }

   @Override
   public Map<String, Object> getRequestContext() {
      return delegate.getRequestContext();
   }

}
