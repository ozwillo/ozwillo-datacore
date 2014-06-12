package org.oasis.datacore.rest.server;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.cxf.phase.PhaseInterceptorChain;
import org.joda.time.Instant;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.oasis.datacore.kernel.client.AuditLogClientAPI;
import org.oasis.datacore.kernel.client.AuditLogClientAPI.RemoteEvent;
import org.oasis.datacore.rest.api.util.JaxrsApiProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class MonitoringLogServiceImpl {
	
	private boolean useRiemann = true;
	private boolean useAuditLogEndpoint = true;

	@Resource
	//@Resource, See https://stackoverflow.com/questions/15614786/could-not-autowire-jaxrs-client
	private AuditLogClientAPI auditLogAPIClient;
   
   @Autowired
   @Qualifier("datacore.cxfJaxrsApiProvider")
   protected JaxrsApiProvider jaxrsApiProvider;
   
	@Autowired
	@Qualifier("datacoreSecurityServiceImpl")
	private DatacoreSecurityService datacoreSecurityService;
	
	public void postLog(String type, String msg) {
		if(useAuditLogEndpoint) {
		    Map<String, Object> map = new HashMap<String, Object>();
		    map.put("type", type);
		    map.put("msg", msg);
		    
		    if(isInServerContext()) {
			    try {
			    	map.put("method", jaxrsApiProvider.getMethod());
			    	map.put("path", jaxrsApiProvider.getAbsolutePath());
			    	map.put("query", jaxrsApiProvider.getQueryParameters());
			    } catch(Exception e) {
			    	map.put("request", "No request context.");
			    }
		    }
		    
		    try {
			    map.put("userId", datacoreSecurityService.getCurrentUserId());
			    map.put("guest", datacoreSecurityService.getCurrentUser().isGuest());
			    map.put("admin", datacoreSecurityService.getCurrentUser().isAdmin());
		    } catch(Exception e) {
		    	map.put("security", "No security context.");
		    }
	    
		    try {
		    	auditLogAPIClient.json(new RemoteEvent(Instant.now(), map));
		    } catch(Exception e) {
		    	//TODO: Log
		    }
	    }
	}

	public boolean isInServerContext() {
 	   return PhaseInterceptorChain.getCurrentMessage() != null ? true : false;
    }
	 
}
