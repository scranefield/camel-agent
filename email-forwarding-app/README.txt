Need to have Apache ZooKeeper running on port 2181 - can change expected port using config.properties), as well as the H2 database engine.

TODO: describe H2 database structure and sample entries.

Zookeeper must contain a node /containers.

The zookeeper routes require a version of Camel later than 2.9.4, 2.10.2 or 2.11.0 (see https://issues.apache.org/jira/browse/CAMEL-5627).
