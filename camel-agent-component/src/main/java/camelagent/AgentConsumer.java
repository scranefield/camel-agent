///////////////////////////////////////////////////////////////////////////////////////
// Copyright (C) 2013 Cranefield S., Ranathunga S. All rights reserved.               /
// ---------------------------------------------------------------------------------- /
// This file is part of camel_jason.                                                  /

//    camel_jason is free software: you can redistribute it and/or modify             /
//   it under the terms of the GNU Lesser General Public License as published by      /
//    the Free Software Foundation, either version 3 of the License, or               /
//    (at your option) any later version.                                             /

//    camel_jason is distributed in the hope that it will be useful,                  /
//    but WITHOUT ANY WARRANTY; without even the implied warranty of                  /
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   /
//    GNU Lesser General Public License for more details.                             /

//    You should have received a copy of the GNU Lesser General Public License        /
//    along with camel_jason.  If not, see <http://www.gnu.org/licenses/>.            /  
///////////////////////////////////////////////////////////////////////////////////////

package camelagent;


import jason.asSemantics.ActionExec;
import jason.asSemantics.Message;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Term;
import jason.asSyntax.Literal;
import jason.asSyntax.ASSyntax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.commons.lang.StringUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author surangika
 * Contains the Jason-specific implementation of the Camel Consumer component 
 *
 */
public class AgentConsumer extends DefaultConsumer {
    
    private static Logger logger = Logger.getLogger(SimpleJasonAgent.class.getName());
    
    private final AgentEndpoint endpoint;

    public AgentConsumer(AgentEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
    }
    
    /**
     * @param agentName
     * @param actionName
     * @param parameters
     * @param unifier
     * @return
     * Maps internal Jason agent actions to camel messages
     */
    @Deprecated
    public Object agentInternallyActed(String agentName, String actionName, List<Term> parameters, Unifier unifier) {
        return agentInternallyActed(agentName, "", actionName, parameters, unifier);
    }
    
    /**
     * @param agentName
     * @param actionString
     * @param actionName
     * @param parameters
     * @param unifier
     * @return
     * Maps internal Jason agent actions to camel messages
     */
    public Object agentInternallyActed(String agentName, String actionString, String actionName, List<Term> parameters, Unifier unifier)
    {       
    	Object actionsucceeded = true; 
	Exchange exchange = endpoint.createExchange();
		
		// Agent action can only be processed by an endpoint of type "action"
    	if (endpoint.getUriOption().contains("action")) {

    		try {
    			
    			List<String> paramsAsStrings = new ArrayList<String>();
                        for (Term t : parameters)
    			{
    			    paramsAsStrings.add(t.toString());
    			}    			    			    			
    			sendActionToCamel(agentName, actionString, actionName, paramsAsStrings, exchange, "");
    			
    		} catch (Exception e) {
    			
    		} finally {
    			    			
    			// log exception if an exception occurred and was not handled
    			if (exchange.getException() != null) {
    				getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
    				actionsucceeded = false;    				
    			} 
    			else
    			{
    				String ep = endpoint.getExchangePattern().toString();  
    				//Just send out camel message and inform success to agent
    				if (ep.equals("InOnly")) {
                                    actionsucceeded = true;
                                    logger.info("InOnly case");
                                }
    				//Unification is applicable to InOut exchanges only. This waits for the return message from the exchange 
    				else if (ep.equals("InOut"))
    				{       logger.info("InOut case");
    					if (exchange.hasOut())
    					{       System.out.println("InOut case");
    						List<String[]> mappings = getResultHeaderSplitted();
    						for(String[] mapping : mappings)
    						{
                                                        int unPos = Integer.parseInt(mapping[1]);
    							String headerVal = mapping[0]; 
    							
								//ArrayList<String> l = (ArrayList<String>)exchange.getIn().getHeader(headerVal);
    							//Iterator<String> it = l.iterator();
    							String unVal = exchange.getIn().getHeader(headerVal).toString();
                                                        try {
                                                            unifier.unifies(parameters.get(unPos), ASSyntax.parseTerm(unVal));
                                                        }
                                                        catch(jason.asSyntax.parser.ParseException e) {
                                                            System.out.println("Error parsing result header from synchronous InOut action: " + unVal);
                                                            return false;
                                                        }
    						}    						
    						return true;
    					}
    					else
    						actionsucceeded = false;
   					}
    			}      			
    		}
    	}
    	return actionsucceeded;
    }
    
    /**
     * @param jasonAction
     * @return
     * Maps external Jason agent actions to camel messages
     */
    public boolean agentActed(JasonAction jasonAction)
    {    
        boolean actionsucceeded = true; 
	Exchange exchange = endpoint.createExchange();
    	if (endpoint.getUriOption().contains("action")) {    		
    		try {    			
    			ActionExec action = jasonAction.getAction();
    			String agName = jasonAction.getAgName();
                        List<String> paramsAsStrings = new ArrayList<>();
                        List<Term> args = action.getActionTerm().getTerms();
                        if (args != null) // Bug in Jason allows null rather than empty list
                        {
                            for (Term t : action.getActionTerm().getTerms()) {
                                paramsAsStrings.add(t.toString());
                            }
                        }
    			//extract annotations
    			List<Term> ann =  action.getActionTerm().getAnnots();    	
    			String annots = "";
    			if (ann != null)
    				annots = ann.toString();//StringUtils.join(ann, ',');
                        Literal actionTerm = action.getActionTerm();
                        sendActionToCamel(agName, actionTerm.toString(), actionTerm.getFunctor(), paramsAsStrings, exchange, annots);
    			
    		} catch (Exception e) {
    			System.out.println(e.getMessage());
    		} finally {
    			    			
    			// log exception if an exception occurred and was not handled
    			if (exchange.getException() != null) {
    				getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
    				actionsucceeded = false;    				
    			} 
    			else
    			{
    				String ep = endpoint.getExchangePattern().toString();
    				
    				//Just send out camel message and inform success to agent
    				if (ep.equals("InOnly"))
    					actionsucceeded = true;  
    				else if (ep.equals("InOut"))
    				{    					
    					if (exchange.getOut() != null)
    						actionsucceeded = true;
    					else
    						actionsucceeded = false;
   					}
    			}      			
    		}
    	}
    	return actionsucceeded;
    }
    
    /**
     * @param message
     * Converts Jason Message to a camel Message and add to the exchange
     */
    public void agentMessaged(Message message)
    {   
        Exchange exchange = endpoint.createExchange();
    	
    	if (endpoint.getUriOption().contains("message")) {
			try {
				String ei = endpoint.getIlloc_force();
				String es = endpoint.getSender();
				String er = endpoint.getReceiver();				
								
				//create message header
				HashMap<String, Object> headerInfo = new HashMap<String, Object>();
				String s = message.getSender();
				String r = message.getReceiver();
				String i = message.getIlForce();
				headerInfo.put("sender", s);
				headerInfo.put("receiver", r);
				headerInfo.put("illoc_force", i);
				headerInfo.put("msg_id", message.getMsgId());
				
				//Extract out any annotations and add as "annotations" header
				try {
					 Literal lit = (Literal) message.getPropCont();
					 if (lit != null)
					 {
						 ListTerm lt = lit.getAnnots();
						 if(lt != null)							 
							 headerInfo.put("annotations",  StringUtils.join(lit.getAnnots(), ','));						 						 
					 }
					 else
						 headerInfo.put("annotations", "");
				} catch(ClassCastException e) {	}

				//COmpare the message content with uri options
				Matcher matcher = endpoint.getBodyMatcher(message.getPropCont().toString());
							
				exchange.getIn().setHeaders(headerInfo);												
				
				// send message to next processor in the route after checking route conditions
				if ((i.equals(ei) || ei==null) && (s.equals(es) || es==null)|| (r.equals(er) || er==null))
					processMatchedMessage(matcher, exchange, message.getPropCont().toString());
				
			} catch (Exception e) {
				
			} finally {
				// log exception if an exception occurred and was not handled
				if (exchange.getException() != null) 
					getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
				else
    			{
    				String ep = endpoint.getExchangePattern().toString();    				
    				if (ep.equals("InOut"))
    				{    /*					
    					if (exchange.getOut() != null)
    					{
    						Message m = new Message();
    					} */   					
   					}
    			}				
			}
		}
    }   
    
    /**
     * @param matcher
     * @param exchange
     * @param messageBody
     * @throws Exception
     * Sends out the camel message only if the uri information is matched with the message content
     */
    private void processMatchedMessage (Matcher matcher, Exchange exchange, String messageBody) throws Exception
    {	if (matcher == null)
    	{
    		exchange.getIn().setBody(messageBody);
                getProcessor().process(exchange);
    	}
        else if (matcher.matches())
        {
                exchange.getIn().setBody(endpoint.getReplacedContent(matcher, messageBody));
                getProcessor().process(exchange);
        }
    }
    
    /**
     * @return List<String[]>
     * Auxiliary method to interpret the result values sent back for agent actions
     */
    private List<String[]> getResultHeaderSplitted()
    {
    	
 	   List<String[]> mappings = new ArrayList<String[]>();
 	   if (endpoint.getResultHeaderMap() != null)
 	   {
 		   String[] temp = endpoint.getResultHeaderMap().split(",");	  
 		   for (String s : temp)
 			  mappings.add(s.split(":"));
 		   return mappings;
 	   }
 	   else
 		   return null;
    }
    
    /**
     * @param agName
     * @param actionString
     * @param actionName
     * @param paramsAStrings
     * @param exchange
     * @param annotations
     * @throws Exception
     * Contains common logic to send internal and external actions as camel messages
     */
    private void sendActionToCamel (String agName, String actionString, String actionName, List<String> paramsAsStrings, Exchange exchange, String annotations) throws Exception
    {
                exchange.getIn().setBody(actionString);
    	        //create action header
		HashMap<String, Object> headerInfo = new HashMap<String, Object>();
		headerInfo.put("actor", agName);
		headerInfo.put("actionName", actionName);
		headerInfo.put("params", paramsAsStrings);
		headerInfo.put("annotations", annotations);
		exchange.getIn().setHeaders(headerInfo);
		
		String ea = endpoint.getActor();
                String ean = endpoint.getActionName();
                String match = endpoint.getMatch();

                boolean matchOk = true;
                if (match != null) {
		  Pattern pattern = Pattern.compile(match);
		  Matcher matcher = pattern.matcher(actionName);
                  matchOk = matcher.find();
                }
		
		if ((agName.equals(ea) || ea==null) && (actionName.equals(ean) || ean==null) && matchOk)
		{
			if (endpoint.getReplace() != null)
				headerInfo.put("actionName", endpoint.getReplace());
			getProcessor().process(exchange);
		}
    }
}
