package agent;

import java.util.List;
import java.util.Vector;

import jason.JasonException;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;

import jason.asSyntax.Term;

public class syncInOutExchange extends DefaultInternalAction {
	static final long serialVersionUID =3;
    @Override
    public Object execute(final TransitionSystem ts, final Unifier un, final Term[] args) throws Exception {
        try {        	
        	String agentName= ts.getUserAgArch().getAgName();      
        	
        	Vector<SimpleJasonAgent> agents = AgentContainer.getAgents();
    		Object ret = true;
    		
    		for(SimpleJasonAgent agent: agents)
    		{
    			if(agent.getAgName().equals(agentName))
    			{    				
    				List<AgentConsumer> actionCons = agent.getValidConsumers("action");
    				
    				if (actionCons.size()==0)
    					ret = false;
    		        else
    		        {		        	
    		        	for(AgentConsumer myConsumer: actionCons)
    		        	{    		        		
    		        		ret = myConsumer.agentInternallyActed(agentName, args, un);
    		        		break;      			
    		        	}    		        	
    		        }
    			}
    		}
    		return ret;
        } 
        catch (Exception e) {
        	throw new JasonException("Error in internal action 'syncInOutExchange': " + e, e);          
        }    
    }
}
