package org.oasis.datacore.kernel.client;

import com.aphyr.riemann.client.RiemannClient;

public class RiemannClientLog {
	
	private RiemannClient client;
	
	public RiemannClientLog() {
		try {
			//TODO Allow configuration
			client = RiemannClient.tcp("127.0.0.1", 5555);
		} catch(Exception e) {
			//TODO Log unable to start RiemannClient
		}		
	}

	public void sendEvent(String service, String desc, String tags) {
		try {
	      	client.connect();
	      	client.event().
	      	  service(service).
	      	  state("running").
	      	  description(desc).
	      	  metric(1).
	      	  tags(tags).
	      	  ttl(30).
	      	  send();
	      	client.disconnect();
		} catch(Exception e) {
			
		}
	}
	
}
