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


import jason.asSemantics.Message;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.util.Vector; //TODO: Is there a reason to use this obsolete class?
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import jason.asSyntax.Literal;
import java.lang.Package;
import java.io.InputStreamReader;
import net.sf.corn.cps.*;

import camelagent.util.ContainerNamingStrategy;
import camelagent.util.SingletonContainerNamingStrategy;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.Collections;

/**
 * @author surangika
 * Manages Jason agents in a Camel context
 */
public class AgentContainer {
	protected static Vector<SimpleJasonAgent> agentList= new Vector<SimpleJasonAgent> ();
	private Set<String> agentNames = new HashSet<String>();
        private Map<String,String> agentTypes = new HashMap<String,String>();
	
	public AgentContainer(ClassLoader cldr, Package pkg)
	{
		this(new SingletonContainerNamingStrategy(), cldr, pkg);
	}
        
	public AgentContainer(ContainerNamingStrategy strategy, ClassLoader cldr, Package pkg)
	{	
                String containerId = strategy.getName();
                Map<String,List<Map>> agentTypeSpecs = new HashMap<String,List<Map>>();
                boolean iniFileExists = true;
                try {
                        URL iniFileURL = cldr.getResource(pkg.getName().replace('.','/') + "/agents.ini");
                        if (iniFileURL == null) throw new IOException("");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(iniFileURL.openStream()));
                
                        try {
                                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                                    String[] lineParts = line.trim().split("\\s++", 2);
                                    String agentType = lineParts[0];
                                    Map typeSpec = (lineParts.length > 1 ? stringToMap(lineParts[1]): new HashMap());
                                    if (!typeSpec.containsKey("n")) typeSpec.put("n","1");
                                    String nstring = (String)typeSpec.get("n");
                                    try {
                                        int i = Integer.parseInt(nstring);
                                        typeSpec.put("n", i);
                                    } catch (NumberFormatException e) {
                                        // TODO: Use a logger
                                        System.out.println("Invalid number of agents ('" + nstring + "') in agents.ini. Substituting '1'.");
                                        typeSpec.put("n", 1);
                                    }
                                    if (typeSpec.containsKey("src")) {
                                        typeSpec.put("type", agentType);
                                        String src = (String)typeSpec.get("src");
                                        if (src.endsWith(".asl")) {
                                            typeSpec.put("src", src);
                                            typeSpec.put("name_stem", src.substring(0,src.lastIndexOf(".asl"))
                                                                  + "_" + agentType                       );
                                        } else {
                                            typeSpec.put("src", src + ".asl");
                                            typeSpec.put("name_stem", src + "_" + agentType);
                                        }
                                    } else {
                                        if (agentType.endsWith(".asl")) {
                                            typeSpec.put("type", agentType.substring(0,agentType.lastIndexOf(".asl")));
                                            typeSpec.put("src", agentType);
                                            typeSpec.put("name_stem", typeSpec.get("type"));
                                        } else {
                                            typeSpec.put("type", agentType);
                                            typeSpec.put("src", agentType + ".asl");
                                            typeSpec.put("name_stem", agentType);
                                        }
                                    }
                                    String src = (String)typeSpec.get("src");
                                    if (agentTypeSpecs.containsKey(src)) {
                                        List specsForSrc = agentTypeSpecs.get(src);
                                        specsForSrc.add(typeSpec);
                                    } else {
                                        List specsForSrc = new ArrayList();
                                        specsForSrc.add(typeSpec);
                                        agentTypeSpecs.put(src, specsForSrc);
                                    }
                                }
                        } finally {
                                if (reader != null) reader.close();
                        }
                } catch(IOException e) {
                    // TODO: Should use a logger!
                    System.out.println("Warning: Optional file agents.ini not found in package " + pkg.getName() + " in specified classpath");
                    iniFileExists = false;
                }                
               
                List<URL> aslFileURLs = CPScanner.scanResources(new ResourceFilter().packageName(pkg.getName()).resourceName("*.asl"));
                for (URL aslFileURL : aslFileURLs)
		{
                    String aslFilePath = aslFileURL.getPath();
                    String fName = aslFilePath.substring(aslFilePath.lastIndexOf("/")+1);
                    List<Map> typeSpecs;
                    if (agentTypeSpecs.containsKey(fName)) {
                        typeSpecs = agentTypeSpecs.get(fName);
                    } else {
                        if (!iniFileExists) {
                            Map typeSpec = new HashMap();
                            typeSpec.put("src", fName);
                            typeSpec.put("type", fName.substring(0, fName.indexOf(".asl")));
                            typeSpec.put("n", 1);
                            typeSpecs = Collections.singletonList(typeSpec);
                        } else {
                            typeSpecs = Collections.EMPTY_LIST;
                        }
                    }
                    for (Map typeSpec : typeSpecs) {
                        Integer n = (Integer) typeSpec.get("n");
                        SimpleJasonAgent ag = null;
                        for (int i = 1; i <= n; i++) {
                                String src = (String) typeSpec.get("src");
                                String nameStem = (String) typeSpec.get("name_stem");
                                //String name = src.substring(0, src.indexOf(".asl")) + "_" + i;
                                String name = nameStem + "_" + i;
                                try {
                                    ag = new SimpleJasonAgent(this, aslFileURL.toURI().toString(), 
                                                              qualifiedName(containerId, name));
                                } catch (URISyntaxException e) {} // Should never happen for a classpath resource URI ???
                                if (ag != null && !agentNames.contains(name)) {
                                        agentList.add(ag);
                                        agentNames.add(name);
                                        agentTypes.put(name, (String) typeSpec.get("type"));
                                } else {
                                    // TODO: Use a logger
                                    System.out.println("Warning: Agent name '" + name + "' has already been used");
                                }

                        }
                    }
                }
        }

        // Adapted from http://stackoverflow.com/questions/14768171/convert-string-representing-key-value-pairs-to-map
        private Map<String, String> stringToMap(String str) {
                String[] tokens = str.split(" |=");
                Map<String, String> map = new HashMap<String,String>();
                for (int i=0; i<tokens.length-1; ) map.put(tokens[i++], tokens[i++]);
                return map;
        }

        public Set<String> getAgentNames() {
            return agentNames;
        }
        
        public Map<String,String> getAgentTypes() {
            return agentTypes;
        }
	
	public void createAgent()
	{
		
	}
	
	public void startAllAgents()
	{
		Enumeration<SimpleJasonAgent> e = getAgents().elements();
                
        while (e.hasMoreElements()) {
 	    	SimpleJasonAgent j = e.nextElement();	  	    	
 	    	if (!j.getHasStarted())
 	    		j.run();
 	    }
	}
	
	public static Vector<SimpleJasonAgent> getAgents()
	{
		return agentList;
	}
	
	/**
	 * @param content
	 * @param receiver
	 * @param annotations
	 * @param updateMode
         * @param nafFunctors
	 * @param persistent
	 * receive the percepts from the camel exchange, and pass it to a particular agent or all the agents, based on the value of the receiver parameter
	 */
	public void getCamelpercepts(Collection<Literal> content, String receiver, String annotations, String updateMode, Set<String> nafFunctors, String persistent)
	{
            try {	
                Iterator<SimpleJasonAgent> it = agentList.iterator();
                for (; it.hasNext();) { 
                    SimpleJasonAgent a = it.next();
                    if (!receiver.equals("")) {
                        if (a.getAgName().equals(receiver)) {
                            a.updatePerceptList(content, annotations, updateMode, nafFunctors, persistent);
                        }
                    } else {
                        System.out.println("Updating percepts for agent " + a.getAgName() + "; content = " + content);
                        a.updatePerceptList(content, annotations, updateMode, nafFunctors, persistent);
                    }
                }
            } catch(Exception e) {
                System.out.println("Exception in AgentContainer.getCamelPercepts: " + e.getMessage());
                e.printStackTrace();
            }
	}
		
	/**
	 * @param message
	 * @param receiver
	 * 
	 * receive the messages from the camel exchange, and pass it to a particular agent or all the agents, based on the value of the receiver parameter
	 */
	public void getCamelMessages(Message message, String receiver)
	{	
		try
		{	
			Iterator<SimpleJasonAgent> it = agentList.iterator();
            for (; it.hasNext();)
            {
            	SimpleJasonAgent a = it.next();
            	
            	//if the receiver value is "all", send the message to all the agents in the context
            	if (receiver.equals("all"))
            	{
            		Message tm = message;
            		if (!a.getAgName().equals(message.getSender())) {
						tm.setReceiver(a.getAgName());
						a.updateMsgQueue(tm);
					}
            	}
            	else if (a.getAgName().equals(receiver))
            		a.updateMsgQueue(message);
            }
		}
		catch(Exception e)
		{			
		}		
	}	

    private String qualifiedName(String containerId, String name) {
        //Prepend containerId if it has a non-empty name, indicating there may be multiple containers
        if (containerId != "")
                return containerId+"__"+name;
        else
                return name;
    }
}
