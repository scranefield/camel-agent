package camelagent;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.zookeeper.CreateMode;

import camelagent.util.ZookeeperContainerNamingStrategy;


public class MainApp {
	
	public static AgentContainer container;
	static String containerId;  

	 public static void main(String... args) throws Exception {	       
		    container = new AgentContainer(new ZookeeperContainerNamingStrategy("/containers/container", CreateMode.EPHEMERAL_SEQUENTIAL));
	        CamelContext camel = new DefaultCamelContext();	       
	        camel.addComponent("agent", new AgentComponent(container));
	         
	        /* Create the routes */
	        camel.addRoutes(new RouteBuilder() {

	           @Override
	           public void configure() {
	        	
	               from("agent:message")   
	               .process(new Processor() {
	                 public void process(Exchange exchange) throws Exception {	                    
	                     exchange.getIn().setHeader("annotations", "xx");
	               }})
	                 .to("agent:message");	                  	   
	             
	                from("timer://foo?fixedRate=true&repeatCount=5&period=100")
	              //.transform().constant("tick")
	              // replaced by this to get tick number as an argument:
	              .process(new Processor() {
	                 public void process(Exchange exchange) throws Exception {
	                     exchange.getIn().setBody("tick("+exchange.getProperty(Exchange.TIMER_COUNTER)+")[xx]");
	                    // exchange.getIn().setHeader("updateMode", "-+");
	               }})
	              .to("agent:percept?persistent=true&annotations=hi(a),bye(b),xx");
	        		               
	                from("agent:action?sender=bob").to("file://data/inbox?fileName=t.txt");
	            
	            }
	        });

	        /* Start the router */

	        // turn exchange tracing on or off (false is off)
	        camel.setTracing(true);

	        // start routing	        
	        camel.start();
	        System.out.println("Starting router...");
	        
	        //Start the agents after starting the routes
	        container.startAllAgents();

	        System.out.println("... ready.");
	    }
}