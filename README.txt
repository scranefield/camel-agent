This project contains an "agent" component for Apache Camel (http://camel.apache.org), allowing agents programmed in Jason (http://jason.sourceforge.net) to send and receive messages to and from Camel routes as agent messages, action invocations and the receipt of percepts.

camel-agent is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

camel_jason is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.                            

You should have received a copy of the GNU Lesser General Public License along with camel_jason.  If not, see http://www.gnu.org/licenses/.

This is a maven project, containing these modules:

* camel-agent-component: This module contains the source code for the Camel agent component, and builds a jar file for that component

* email-forwarding-app: This module contains a sample Camel router that monitors a specific email account and asks agents to recommend who to forward messages to. The application contains Camel routes that interact with Apache ZooKeeper, the H2 database engine, and a mailer server (via IMAP and SMTP). See http://arxiv.org/abs/1302.1937 for more information about this application.

The email-forwarding-app module contains some modified code from Apache Camel, which is included here under the Apache License, Version 2.0. See the file ApacheLicence2.0.txt within that module.

