package org.oasis.datacore.rest.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class DatacoreApiImplTestStarter {
    
    @SuppressWarnings("unused")
	private ClassPathXmlApplicationContext context;

    protected DatacoreApiImplTestStarter() {
        System.out.println("Starting Server");
        
        this.context = new ClassPathXmlApplicationContext("oasis-datacore-rest-server-test-context.xml");
    }
    
    public static void main(String args[]) throws java.lang.Exception { 
        new DatacoreApiImplTestStarter();
        System.out.println("Server ready..."); 
        
        // stay up until...
        //Thread.sleep(5 * 60 * 1000); // ... delay
        System.in.read(); // ... input
        
        System.out.println("Server exiting");
        System.exit(0);
    }
}
