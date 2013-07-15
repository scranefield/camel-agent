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

import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

/**
 * @author surangika
 * Contains the Jason-specific implementation of the Camel Endpoint
 */
public class AgentEndpoint extends DefaultEndpoint {
	
	private String ilf;
	private String sender;
	private String receiver;
        private String actionName;
	private String actor;
	private String annotations;	
	private String uriOption;
	private String match;
	private String replace;
	private String updateMode;
	private String persistent;
	private String resultHeaderMap;
	private AgentComponent agent_component;
	
    public AgentEndpoint() {
    }

    public AgentEndpoint(String uri, AgentComponent component) {
        super(uri, component);
        agent_component = component;
        setUriOption();
        //defaults
        persistent = "false";
        updateMode = "+";
        annotations = "";
    }  
    
    public void setResultHeaderMap(String resultHeaderMap)
    {
    	this.resultHeaderMap = resultHeaderMap;
    }
    
    public String getResultHeaderMap()
    {
    	return resultHeaderMap;
    }
   
    public void setUpdateMode(String updateMode)
    {
    	this.updateMode = updateMode;
    }
    
    public String getUpdateMode()
    {
    	return updateMode;
    }
    
    public void setPersistent(String persistent)
    {
    	this.persistent = persistent;
    }
    
    public String getPersistent()
    {
    	return persistent;
    }
    public void setMatch(String match)
    {
    	this.match = match;
    }
    
    public String getMatch()
    {
    	return match;
    }
    
    public void setReplace(String replace)
    {
    	this.replace = replace;
    }
    
    public String getReplace()
    {
    	return replace;
    }
    
    public void setIlloc_force(String ilf)
    {
    	this.ilf = ilf;
    }
    
    public String getIlloc_force()
    {
    	return ilf;
    }
    public void setSender(String sender)
    {
    	this.sender = sender;
    }
    
    public String getSender()
    {
    	return sender;
    }
    public void setReceiver(String receiver)
    {
    	this.receiver = receiver;
    }
    
    public String getReceiver()
    {
    	return receiver;
    }

    public void setActionName(String actionName)
    {
    	this.actionName = actionName;
    }

    public String getActionName()
    {
    	return actionName;
    }

    
    public void setActor(String actor)
    {
    	this.actor = actor;
    }
    
    public String getActor()
    {
    	return actor;
    } 
    public void setAnnotations(String a)
    {
    	this.annotations = a;
    }
    
    public String getAnnotations()
    {
    	return annotations;
    }     
    
    /**
     * Identifies whether the uri refers to a message, action, or percept
     */
    private void setUriOption()
    {
    	
    	String uri = this.getEndpointUri().substring(this.getEndpointUri().indexOf(":"));
    	if (uri.contains("?"))
    		uriOption = uri.substring(0, uri.indexOf("?"));
    	else
    		uriOption = uri;
    }
    
    public String getUriOption()
    {    	
    	return uriOption;
    }
        
    public Producer createProducer() throws Exception {    	
        return new AgentProducer(this, agent_component);
    }

    public Consumer createConsumer(Processor processor) throws Exception {    	
    	AgentConsumer cons = new AgentConsumer(this, processor);  
    	
    	Enumeration<SimpleJasonAgent> e = AgentContainer.getAgents().elements();
    	
    	while (e.hasMoreElements()) {
    		SimpleJasonAgent j = e.nextElement();	    	
	    	j.addToMyConsumers(cons);
	    }
        return cons;
    }

    public boolean isSingleton() {
        return true;
    }
    
    /**
     * @param bodyContent
     * @return Matcher
     * Create the matcher object using message content and uri information 
     */
    public Matcher getBodyMatcher(String bodyContent)
    {
    	if (this.match != null)
    	{
    		Pattern pattern = Pattern.compile(this.match + "(?:\\[.+\\])?"); // This appends an extra pattern to allow for Jason annotations on body content
    		Matcher matcher = pattern.matcher(bodyContent);
    		return matcher;
    	}
    	else
    	{
    		if (this.replace != null)
    			throw new IllegalArgumentException("replace cannot have a value when match is not specified");
    		return null;
    	}		
    }
    
    /**
     * @param matcher
     * @param bodyContent
     * @return String
     * Match and replace message content, using the uri information
     */
    public String getReplacedContent(Matcher matcher, String bodyContent)
    {
        System.out.println();
        if (matcher == null) return bodyContent;

        String matchedString;
    	
    	if (this.replace != null)
        {

                matchedString = matcher.replaceFirst(this.replace);
/*
    		String[] replaces = this.replace.split(",");
    		StringBuilder sbuilder = new StringBuilder();
    		for (int i=0;i<replaces.length;i++)
    		{    			
    			Pattern pattern = Pattern.compile("\\$([1-9][0-9]*)");
        		Matcher m = pattern.matcher(replaces[i]);
        		String replaceS = "";
        		if(m.matches())
        		{
        			int g = Integer.parseInt(m.group(1));
        			replaceS = matcher.group(g);
        			
        		}
        		else
        			replaceS = replace;
        		sbuilder.append(replaceS + ":");

    		}
    		matchedString = sbuilder.toString();
 */

	}
	else matchedString = bodyContent;
        
  	return matchedString;
    }
}
