package camelagent;

import java.util.List;
import java.util.Vector;

import jason.JasonException;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;

import jason.asSyntax.Term;

public class syncAction extends DefaultInternalAction {
	static final long serialVersionUID =3;
    @Override
    public Object execute(final TransitionSystem ts, final Unifier un, final Term[] args) throws Exception {
        try {        	
        	String agentName= ts.getUserAgArch().getAgName();      
        	
        	Vector<SimpleJasonAgent> agents = AgentContainer.getAgents();
    		Object ret = true;

                Literal actionLiteral = (Literal) args[0];
                String actionName = actionLiteral.getFunctor();
                List<Term> params = actionLiteral.getTerms();
    		
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
                                        ret = myConsumer.agentInternallyActed(agentName, actionName, params, un);
                                        // break;
                                    }
                                }
    			}
    		}
    		return ret;
        } 
        catch (Exception e) {
        	throw new JasonException("Error in internal action 'syncAction': " + e, e);
        }    
    }
}
