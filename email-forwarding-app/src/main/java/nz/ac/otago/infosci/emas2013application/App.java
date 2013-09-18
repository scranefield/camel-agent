package nz.ac.otago.infosci.emas2013application;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.processor.idempotent.MemoryIdempotentRepository;
import org.apache.zookeeper.CreateMode;

import org.apache.camel.spi.Registry;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.impl.PropertyPlaceholderDelegateRegistry;
import org.h2.jdbcx.JdbcDataSource;

//import org.apache.camel.component.jms.JmsComponent;
//import org.apache.activemq.ActiveMQConnectionFactory;
//import javax.jms.ConnectionFactory;

import camelagent.*;

import camelagent.util.ZookeeperContainerNamingStrategy;

import java.util.Properties;
import java.util.List;
import java.io.FileInputStream;
import javax.swing.JPasswordField;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class App {

	static AgentContainer container;
	static String containerId;
        final static String imapServer = "imap.gmail.com";
        final static String smtpServer = "localhost"; // Tested using smtp4dev
        final static String mailAccount = "stephen.cranefield@gmail.com";
        final static String mailPassword = getMailPassword(mailAccount);
        final static String mailDomain = "otago.ac.nz";

	 public static void main(String[] args) throws Exception {

                container = new AgentContainer(new ZookeeperContainerNamingStrategy("/containers/container", CreateMode.EPHEMERAL_SEQUENTIAL));
	        final CamelContext camel = new DefaultCamelContext();
	        camel.addComponent("agent", new AgentComponent(container));

               JdbcDataSource ds = new JdbcDataSource();
               ds.setURL("jdbc:h2:tcp://localhost/~/userinfo");
               ds.setUser("sa");
               ds.setPassword("");

               Registry registry = camel.getRegistry();
               if (registry instanceof PropertyPlaceholderDelegateRegistry)
                   registry = ((PropertyPlaceholderDelegateRegistry)registry).getRegistry();
               ((JndiRegistry) registry).bind("userinfo", ds);

                //ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
	        //camel.addComponent("jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
                                
                Properties props = new Properties();
                String zkServerProp;
		try {
			props.load(new FileInputStream("config.properties"));
                        zkServerProp = props.getProperty("zookeeper_server");
		} catch (Exception e) {
			zkServerProp = "127.0.0.1:2181";
		}
		final String zkserver = zkServerProp;

	        /* Create the routes */
	        camel.addRoutes(new RouteBuilder() {
	           @Override
	           public void configure() {
                      from("agent:action?exchangePattern=InOut&actionName=get_users&resultHeaderMap=result:0")
                      .setBody(constant("select username from users"))
                      .to("jdbc:userinfo")
                      .setHeader("result").groovy("exchange.in.body.collect{it['USERNAME']}");

                      from("agent:action?exchangePattern=InOut&actionName=get_rules&resultHeaderMap=result:1")
                      .log("Params are: ${header[params]}")
                      .setBody().groovy("\"select rule from rules where rules.username in (${request.getHeader('params')[0][1..-2].tokenize(',').collect({\"'${it}'\"}).join(',')})\"")
                      .to("jdbc:userinfo")
                      .setHeader("result").groovy("exchange.in.body.collect{'\"' + it['RULE'] + '\"'}");

	              // Implement registration by creating a new ZooKeeper sequence
                      // node with the agent name as its content
                      from("agent:action?actionName=register")
                      // Process only one register action from each agent
                      .idempotentConsumer(header("actor"),
                                          MemoryIdempotentRepository.memoryIdempotentRepository(100)
                                         ).eager(true)
                      .setBody(header("actor")) // Put actor name in message body
                      .to("zookeeper://" + zkserver + "/agents/agent?create=true&createMode=EPHEMERAL_SEQUENTIAL");

                      // Watch agents node in ZooKeeper for changes to list of children
                      from("zookeeper://" + zkserver + "/agents?listChildren=true&repeat=true")
                      .setHeader("numChildren", simple("${body.size}"))
                      .process(new Processor() {
                         public void process(Exchange exchange) throws Exception {
                            int numChildren = exchange.getIn().getBody(List.class).size();
                            if (numChildren > 0) {
                               if (exchange.getContext().getRouteStatus("fetchmail").isStartable())
                                   exchange.getContext().startRoute("fetchmail");
                               else if (exchange.getContext().getRouteStatus("fetchmail").isSuspended())
                                   exchange.getContext().resumeRoute("fetchmail");
                             }
                            else // numChildren == 0
                            if (exchange.getContext().getRouteStatus("fetchmail").isStarted())
                                exchange.getContext().suspendRoute("fetchmail");
                         }
                      })
                      .split(body()) // Split agent node list into separate messages
                      .process(new Processor() {
                         public void process(Exchange exchange) throws Exception {
                           // Map the ZooKeeper node name for an agent to the agent
                           // name by getting the content of the ZooKeeper node
                           ConsumerTemplate consumer = camel.createConsumerTemplate();
                           String agentName = consumer.receiveBody("zookeeper://"  +zkserver + "/agents/"+ exchange.getIn().getBody(), String.class);
                           exchange.getIn().setBody(agentName);
                         }})
                      // Aggregate mapped names into a single message containing a
                      // list of names. All messages will have the same headers - any
                      // will do as the message correlation id
                      .aggregate(header("numChildren"), new ArrayListAggregationStrategy())
                      .completionSize(header("numChildren"))
                      .setBody(simple("registered_agents(${bodyAs(String)})"))
                      .to("agent:percept?persistent=true&updateMode=replace");

                      // Poll for email messages
                      from("imaps://" + imapServer + "?username=" + mailAccount + "&password="+mailPassword+"&delete=false&closeFolder=false&connectionTimeout=60000")
                      .routeId("fetchmail")
                      .noAutoStartup()
                      .setHeader("id", simple("\"${id}\""))
                      .removeHeader("to")
                      .to("seda:forward-message", "direct:ask-agents");

                      // Request agents to evaluate message on behalf of their allocated users
                      from("direct:ask-agents")
                      .process(new Processor() {
                          public void process(Exchange exchange) throws Exception {
                              System.out.println("ask-agents route: " + exchange.getIn().getBody());
                        }
                       })
                       .convertBodyTo(String.class)
                      .process(new Processor() {
                          public void process(Exchange exchange) throws Exception {
                              System.out.println("ask-agents route: " + exchange.getIn().getBody());
                        }
                       })
                       .setBody(simple("check_relevance(${header.id}, " +
                                                       "\"${headerAs('from',nz.ac.otago.infosci.emas2013application.SanitisedString)}\", " +
                                                       "\"${headerAs('subject', nz.ac.otago.infosci.emas2013application.SanitisedString)}\", " +
                                                       "\"${bodyAs(nz.ac.otago.infosci.emas2013application.SanitisedString)}\")"))
                      .setHeader("receiver", constant("all"))
                      .setHeader("sender", constant("router"))
                      .to("agent:message?illoc_force=achieve");

                       // Receive responses from agents and aggregate them to get a single lists of relevant users
                       from("agent:message?illoc_force=tell&receiver=router&match=relevant\\((.*),(.*)\\)&replace=$1:$2")
                       .process(new Processor() {
                          public void process(Exchange exchange) throws Exception {
                              System.out.println("Agent reply: " + exchange.getIn().getBody());
                        }
                       })
                       .setHeader("id", simple("${body.split(\":\")[0]}"))
                       .setBody(simple("${body.split(\":\")[1]}"))
                       .aggregate(header("id"),
                                  new SetUnionAggregationStrategy()
                       ).completionTimeout(2000)
                       .setHeader("to").groovy("request.getBody(String)[1..-2].tokenize(',').collect({\"${it}@${nz.ac.otago.infosci.emas2013application.App.mailDomain}\"}).join(',')")
                       .process(new Processor() {
                          public void process(Exchange exchange) throws Exception {
                              System.out.println("Aggregated to header: " + exchange.getIn().getHeader("to"));
                        }
                       })
                       .to("seda:forward-message");

                        // Aggregate original mail message with message summarising interested users in "to" header, and send it
                        from("seda:forward-message")
                        .aggregate(header("id"),
                                   new CombineBodyAndHeaderAggregationStrategy("to")
                        ).completionSize(2)
                        .setHeader("from", constant(mailAccount))
                        .process(new Processor() {
                          public void process(Exchange exchange) throws Exception {
                              System.out.println("Outgoing mail headers: " + exchange.getIn().getHeaders());
                              System.out.println("Body type: " + exchange.getIn().getBody().getClass());
                        }
                        })
                        .to("smtp://" + smtpServer + "?username=" + mailAccount + "&password=" + mailPassword);
	            }
	        });

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

         private static String getMailPassword(String account) {
             // Password reading code from http://stackoverflow.com/questions/8881213/joptionpane-to-get-password:

             final JPasswordField jpf = new JPasswordField();
             JOptionPane jop = new JOptionPane(jpf, JOptionPane.QUESTION_MESSAGE,
             JOptionPane.OK_CANCEL_OPTION);
             JDialog dialog = jop.createDialog("Enter password for " + account);
             dialog.addComponentListener(new ComponentAdapter() {
                 @Override
                  public void componentShown(ComponentEvent e) {
                        SwingUtilities.invokeLater(new Runnable() {
                        @Override
                            public void run() {
                                jpf.requestFocusInWindow();
                            }
                        });
                 }
             });
             dialog.setVisible(true);
             int result = (Integer) jop.getValue();
             dialog.dispose();
             char[] password = null;
             if (result == JOptionPane.OK_OPTION) {
                password = jpf.getPassword();
             }

             return String.valueOf(password);
         }
}