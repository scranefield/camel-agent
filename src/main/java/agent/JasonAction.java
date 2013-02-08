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

import java.io.Serializable;
import java.util.List;

/**
 * @author surangika
 *Wrapper class for Jason external actions
 */
public class JasonAction implements Serializable{
	static final long serialVersionUID =1;
	private ActionExec action;
	private List<ActionExec> feedback;
	private String agName;
	
	public JasonAction(ActionExec action, List<ActionExec> feedback, String agName)
	{
		this.action = action;
		this.feedback = feedback;
		this.agName = agName;
	}
	
	public ActionExec getAction()
	{
		return action;
	}
	
	public void setFeedback(boolean b)
	{
		 action.setResult(b);
		 feedback.add(action);
	}	
	
	public String getAgName()
	{
		return agName;
	}
}
