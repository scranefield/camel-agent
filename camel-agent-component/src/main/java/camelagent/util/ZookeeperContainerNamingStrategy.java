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

package camelagent.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;

/**
 * @author surangika
 * Implements the logic to generate AgentContainer name using Zookeeper 
 */
public class ZookeeperContainerNamingStrategy implements ContainerNamingStrategy{
	private Properties prop;
	protected static String HOST;
    protected static ZooKeeper zk;
    protected static final byte[] NO_DATA = new byte[0];
    protected static final Integer INITIAL_VERSION = 0;
    private String containerId;
    
	/**
	 * @param path
	 * @param createMode
	 * @throws IOException
	 * @throws KeeperException
	 * @throws InterruptedException
	 * Connects to the Zookeepr server and create a node using the given path
	 */
	public ZookeeperContainerNamingStrategy(String path, CreateMode createMode) throws IOException, KeeperException,InterruptedException
	{		
		prop = new Properties();
		try {
			prop.load(new FileInputStream("config.properties"));
		} catch (Exception e) {
			
		}
		HOST = prop.getProperty("zookeeper_server");
		zk = new ZooKeeper(HOST, 3000, new ZooKeeperWatcher());
		
		if (zk.exists(path, false) == null) 
			containerId =  zk.create(path, NO_DATA, Ids.OPEN_ACL_UNSAFE, createMode);
	}
		
	public String getName()
	{		
		//Return the last part of the container as the name
		String[] nameparts = containerId.split("/");
		return nameparts[nameparts.length-1];
	}	
	
	protected class ZooKeeperWatcher implements Watcher {
         @Override
         public void process(WatchedEvent event) {
         }
	 }
}
