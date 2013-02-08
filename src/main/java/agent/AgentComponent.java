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

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;

public class AgentComponent extends DefaultComponent {	
	private AgentContainer container;
	public AgentComponent(AgentContainer container)
	{
		this.container = container;
	}
	
	public AgentContainer getContainer()
	{
		return this.container;
	}
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {    	
        Endpoint endpoint = new AgentEndpoint(uri, this);
        setProperties(endpoint, parameters);
        return endpoint;
    }      
}
