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

package agent;


import jason.asSemantics.ActionExec;
import jason.asSemantics.Message;
import jason.asSemantics.Unifier;
import jason.asSyntax.Atom;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Term;
import jason.asSyntax.Literal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.commons.lang.StringUtils;

/**
 * @author surangika
 * Contains the Jason-specific implementation of the Camel Consumer component 
 *
 */
public class AgentConsumer extends DefaultConsumer {
    private final AgentEndpoint endpoint;

    public AgentConsumer(AgentEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
    }
    
    /**
     * @param agentName
     * @param parameters
     * @param unifier
     * @return
     * Maps internal Jason agent actions to camel messages
     */
    public Object agentInternallyActed(String agentName, Term[] parameters, Unifier unifier)
    {       
    	
    	Object actionsucceeded = true; 
		Exchange exchange = endpoint.createExchange();
		
		// Agent action can only be processed by an endpoint of type "action"
    	if (endpoint.getUriOption().contains("action")) {    		
    		try {
    			
    			List<String> params = new ArrayList<String>();
    			for(int i=1;i<parameters.length;i++)
    			{
    				if (!parameters[i].isVar())
    					params.add(parameters[i].toString());
    			}    			    			    			
    			sendActionToCamel(agentName, params,  parameters[0].toString(), exchange, "");
    			
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
    				if (ep.equals("InOnly"))
    					actionsucceeded = true; 
    				
    				//Unification is applicable to InOut exchanges only. This waits for the return message from the exchange 
    				else if (ep.equals("InOut"))
    				{    					
    					if (exchange.hasOut())
    					{
    						List<String[]> mappings = getResultHeaderSplitted();
    						for(String[] mapping : mappings)
    						{
    							int unPos = Integer.parseInt(mapping[1]);    							
    							String headerVal = mapping[0]; 
    							
								//ArrayList<String> l = (ArrayList<String>)exchange.getIn().getHeader(headerVal);
    							//Iterator<String> it = l.iterator();
    							String unVal = exchange.getIn().getHeader(headerVal).toString();    
    							unifier.unifies(parameters[unPos], new Atom(unVal));
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
    			List<String> params = new ArrayList<String>();
    			for(Term t : action.getActionTerm().getTerms())
    				params.add(t.toString());
    			
    			//extract annotations
    			List<Term> ann =  action.getActionTerm().getAnnots();    	
    			String annots = "";
    			if (ann != null)
    				annots = ann.toString();//StringUtils.join(ann, ',');    					
    			sendActionToCamel(agName, params, action.getActionTerm().getFunctor(), exchange, annots);    			    			   			
    			
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
				} catch(ClassCastException e) {}
				
				//COmpare the message content with uri options
				Matcher matcher =endpoint.getBodyMatcher(message.getPropCont().toString());
							
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
    private void processMatchedMessage (Matcher matcher, Exchange exchange, String messageBody)throws Exception
    {	
    	if (matcher == null)
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
     * @param params
     * @param actionName
     * @param exchange
     * @param annotations
     * @throws Exception
     * Contains common logic to send internal and external actions as camel messages
     */
    private void sendActionToCamel (String agName, List<String> params, String actionName, Exchange exchange, String annotations) throws Exception
    {
    	//create action header
		HashMap<String, Object> headerInfo = new HashMap<String, Object>();
		headerInfo.put("actor", agName);
		headerInfo.put("actionName", actionName);
		headerInfo.put("params", params);
		headerInfo.put("annotations", annotations);
		exchange.getIn().setHeaders(headerInfo);
		
		String ea = endpoint.getActor();
		Pattern pattern = Pattern.compile(endpoint.getMatch());
		Matcher matcher = pattern.matcher(actionName);
		
		if ((agName.equals(ea) || ea==null) && matcher.find())
		{
			if (endpoint.getReplace() != null)
				headerInfo.put("actionName", endpoint.getReplace());
			getProcessor().process(exchange);
		}
    }
}
