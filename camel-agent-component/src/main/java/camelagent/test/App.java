package camelagent.test;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

//import org.apache.camel.component.jms.JmsComponent;
//import org.apache.activemq.ActiveMQConnectionFactory;
//import javax.jms.ConnectionFactory;

import camelagent.*;

import camelagent.util.SingletonContainerNamingStrategy;

public class App {

	static AgentContainer container;
	static String containerId;

	 public static void main(String[] args) throws Exception {

                container = new AgentContainer(App.class.getClassLoader(), App.class.getPackage());
	        final CamelContext camel = new DefaultCamelContext();
	        camel.addComponent("agent", new AgentComponent(container));

	        /* Create the routes */
	        camel.addRoutes(new RouteBuilder() {
	           @Override
	           public void configure() {
                        from("timer:test?period=200")
                        .transform(simple("tick(${property.CamelTimerCounter})"))
                        .to("agent:percept?persistent=false&updateMode=replace");
	           }});

	        /* Start the router */

	        // turn exchange tracing on or off (false is off)
//	        camel.setTracing(true);

	        // start routing
	        camel.start();
	        System.out.println("Starting router...");

	        //Start the agents after starting the routes
	        container.startAllAgents();

	        System.out.println("... ready.");
	    }

}