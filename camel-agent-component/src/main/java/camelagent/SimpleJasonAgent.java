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

import jason.architecture.AgArch;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Agent;
import jason.asSemantics.Circumstance;
import jason.asSemantics.Message;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.runtime.Settings;


import java.io.Serializable;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


/**
 * @author surangika
 * This class extends the logic in the SimpleJasonAgent class of the Jason distribution
 */
public class SimpleJasonAgent extends AgArch implements Serializable{
    static final long serialVersionUID =2;
    private static Logger logger = Logger.getLogger(SimpleJasonAgent.class.getName());
    private AgentContainer container;
    Agent ag;
    private String name;
    Queue<Message> localMsgQueue;
    Queue<Literal> pers_percepts;
    Queue<Literal> temp_percepts;
    private boolean hasStarted;
    private Vector<AgentConsumer> myConsumers;  

    public SimpleJasonAgent(AgentContainer container, String filePathOrURL, String name) {
        this.container = container;
        this.name = name;
        hasStarted = false;
    	localMsgQueue = new ConcurrentLinkedQueue<Message>();
    	pers_percepts = new ConcurrentLinkedQueue<Literal>();
    	temp_percepts = new ConcurrentLinkedQueue<Literal>();
    	myConsumers = new Vector<AgentConsumer>();
        
        try {
            ag = new Agent();
            new TransitionSystem(ag, new Circumstance(), new Settings(), this);  
            //Initialise agent from the .asl file
            ag.initAg(filePathOrURL);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Init error", e);
        }
    }

    public void addToMyConsumers(AgentConsumer myConsumer)
    {
    	synchronized (myConsumers) {    		
    		myConsumers.add( myConsumer);				
		}    	
    }   
    
    /**
     * Agent execution cycle, runs in its own thread
     */
    public void run() {
    	hasStarted = true;
    	 Thread thread = new Thread(){
    		    public void run(){
    		    	try {
    		            while (isRunning()) {
    		                // calls the Jason engine to perform one reasoning cycle
    		                logger.fine("Reasoning....");
    		                getTS().reasoningCycle();
    		            }
    		        } catch (Exception e) {
    		            logger.log(Level.SEVERE, "Run error", e);
    		            hasStarted = false;
    		        }
    		    }
    		  };    		 
    		  thread.start();
    }

    public String getAgName() {
        return name;
    }
    
    public void updateMsgQueue(Message m)
    {
    	localMsgQueue.offer(m);       
    }
    
    public boolean getHasStarted()
    {
    	return hasStarted;
    }
    
    /**
     * @param percept
     * @param annotations
     * @param updateMode
     * @param nafFunctors
     * @param persistent
     * Updates the transient and persistent percepts using messages received from the camel exchange
     */
    public void updatePerceptList(Collection<Literal> newPercepts, String annotations, String updateMode, Set<String> nafFunctors, String persistent)
    {    	
    	List<String> annots = Arrays.asList(annotations.split(","));
        
        for (Literal l : newPercepts) {
            if (l != null && annots != null)
		 {
    		 for(String as : annots)
			 {
				 try {
					if (!as.equals(""))
						l.addAnnot(ASSyntax.parseTerm(as));
				} catch (Exception e) {
					System.err.println("Exception in SimpleJasonAgent.updatePerceptList: " + e.getMessage());
				}
			 }			
		 }    
        }    	
    	
    	if(persistent.equals("false"))
    	{
    	    updatePercepts(updateMode, nafFunctors, newPercepts, temp_percepts);
    	}
    	else if(persistent.equals("true"))
        {
            updatePercepts(updateMode, nafFunctors, newPercepts, pers_percepts);
        }
    }

    private void updatePercepts(String updateMode, Set<String> nafFunctors, Collection<Literal> newPercepts, Queue<Literal> perceptQueue) {
        //If updateMode is "add" (default option) and an identical percept is not already in the persistent queue, add the percept to the persistent queue
        if (updateMode.equals("add")) {
            List<Literal> perceptsToAdd = new LinkedList<>(newPercepts);
            synchronized (perceptQueue) {
                Iterator<Literal> it = perceptsToAdd.iterator();
                while (it.hasNext()) {
                    Literal l = it.next();
                    if (perceptQueue.contains(l)) {
                        it.remove();
                    }
                }
                perceptQueue.addAll(perceptsToAdd);
            }
        } else if (updateMode.equals("replace")) {
            Set<String> newPerceptFunctors = new HashSet<String>();
            for (Literal l : newPercepts) {
                newPerceptFunctors.add(l.getFunctor());
            }
            synchronized (perceptQueue) {
                Iterator<Literal> it = perceptQueue.iterator();
                while (it.hasNext()) {
                    Literal lit = it.next();
                    String f = lit.getFunctor();
                    if (nafFunctors.contains(f) && !newPerceptFunctors.contains(f)) { // Negation as failure applies
                        // @todo Allow naf endpoint parameter to specify arity as well as functor
                        perceptQueue.remove(lit); // Don't change to it.remove()!!! Can get IllegalStateException if lit already removed, but "weakly consistent" iterator doesn't know.
                    }
                    else {
                        // Negation as failure does not apply
                        for (Literal newPercept : newPercepts) {
                            if (f.equals(newPercept.getFunctor()) &&
                                lit.getArity() == newPercept.getArity()) {
                                    perceptQueue.remove(lit); // Don't change to it.remove()!!!           
                            }
                        }
                    }
                }
                perceptQueue.addAll(newPercepts);
            }
        } else {
            Matcher m = Pattern.compile("delete(\\([=_](,[=_])*\\))?").matcher(updateMode);
            if (m.matches()) {
                // delete mode
                if (newPercepts.size() > 1) {                  
                   logger.log(Level.SEVERE, "Percept delete mode not compatible with non-singleton collection of percepts");
                   return;
                }
                for (Literal l : newPercepts) {  // Should have 0 or 1 iterations (not sure if 0 can occur)
                    String[] argMatchMode;
                    if (m.start(1) == -1) {
                        // No arguments provided for 'delete'. Assume all args of queued percept must match new percept for queued percept to be deleted
                        argMatchMode = new String[l.getArity()];
                        Arrays.fill(argMatchMode, "=");
                    } else {
                        String deleteArgsWithoutParens = updateMode.substring(m.start(1) + 1, m.end(1) - 1);
                        argMatchMode = deleteArgsWithoutParens.split(",");
                    }
                    synchronized (perceptQueue) {
                        Iterator<Literal> it = perceptQueue.iterator();
                        while (it.hasNext()) {
                            Literal lit = it.next();
                            //System.out.println("*** Queued percept being checked: " + lit);
                            if (lit.getFunctor().equals(l.getFunctor()) && lit.getArity() == l.getArity()) {
                                boolean delete = true;
                                for (int i = 0; i < lit.getArity(); i++) {
                                    if (argMatchMode[i].equals("=") && !l.getTerm(i).equals(lit.getTerm(i))) {
                                          delete = false;
                                    }
                                }
                                if (delete) {
                                    //System.out.println("*** Deleting percept: " + lit);
                                    it.remove();
                                }
                            }
                        }
                    }
                }
            } else {
                System.out.println("Bad value for updateMode " + updateMode + "(percepts: " + newPercepts + ")");
            }
        }
    }

    // Agent calls this method to receive the transient and persistent percepts 
    @Override
    public List<Literal> perceive() {
        List<Literal> l = new ArrayList<Literal>();       
        
        //Remove transient percepts from the transient queue, and add them to agent's internal percept list 
        while (!temp_percepts.isEmpty())
        {        	
        	Literal li = temp_percepts.poll();
            l.add(li);
        }
        //Add persistent percepts to agent's internal percept list, but do not remove them from the persistent queue 
        Iterator<Literal> i = pers_percepts.iterator();
        while (i.hasNext()){
        	Literal li = i.next();
        	l.add(li);
        }        
        return l;
    }

    // this method get the agent actions
    @Override
    public void act(ActionExec action) {
        JasonAction a = new JasonAction(action, getAgName());
        
        Structure actionTerm = action.getActionTerm();
        String functor = actionTerm.getFunctor();
        int arity = actionTerm.getArity();
        
        if (functor.equals("true") && arity == 0) {
            action.setResult(true);
            actionExecuted(action);
            return;
        }
        if (functor.equals("false") && arity == 0) {
            action.setResult(false);
            actionExecuted(action);
            return;
        }
        
        //Find all consumers that can handle "action"s
        List<AgentConsumer> actionCons = getValidConsumers("action");
       
        //if there is no consumer to handle this action
        if (actionCons.isEmpty())
        {
        	action.setResult(false);
        	actionExecuted(action);
        }
        else
        {
        	boolean r = false;
        
        	for(AgentConsumer myConsumer: actionCons)
        	{
        		if(myConsumer.agentActed(a))//if at least one action succeeded
        		{        			
        			r = true;
        			action.setResult(true);
                                actionExecuted(action);
                	//break;
        		}        			
        	}
        	if (!r)
        	{
        		action.setResult(false);
                        actionExecuted(action);
        	}
        }    		        
    }

    @Override
    public boolean canSleep() {
        return false;
    	//return localMsgQueue.isEmpty() && isRunning();
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    // a very simple implementation of sleep
    public void sleep() {
        try {
        Thread.sleep(100);
        } catch (InterruptedException e) {}
    }
    
    @Override
    public void sendMsg(jason.asSemantics.Message m) throws Exception {   
    	//Send to Camel exchange if the receipient name starts with container
        //if (m.getReceiver().startsWith("container"))
        {
            for(AgentConsumer myConsumer: getValidConsumers("message"))
                myConsumer.agentMessaged(m);
    	}
    	//Else send as a local Jason message
    	//else
    	//	container.getCamelMessages(m, m.getReceiver());
    }

    @Override
    public void broadcast(jason.asSemantics.Message m) throws Exception {
    	//Send to Camel exchange if the recipient name starts with container
    	if (name.startsWith("container")) 
    	{
    		m.setReceiver("all");
    		for(AgentConsumer myConsumer: getValidConsumers("message"))
    			myConsumer.agentMessaged(m); 
    	}   
    	else
    		container.getCamelMessages(m, "all");
    }

    @Override
    public void checkMail() {
    	while (!localMsgQueue.isEmpty()) {
            Message im = localMsgQueue.poll();
            getTS().getC().getMailBox().offer(im); 
        }
    }  
    
    /**
     * @param consumerType
     * @return
     * Returns the set of consumers for a given consumer type (messages or actions)
     */
    public List<AgentConsumer> getValidConsumers(String consumerType)
    {
    	List<AgentConsumer> consumers = new ArrayList<AgentConsumer>();
    	for(AgentConsumer myConsumer: myConsumers)
    	{    		
    		if (myConsumer.toString().contains(consumerType))
    			consumers.add(myConsumer);
    	}
    	return consumers;
    }
}
