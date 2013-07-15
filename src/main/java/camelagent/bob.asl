!start.
+!check_relevance(Id,From,Subject,Body)[source(Ag)]:true
    <- //agent.getValidUsers(From,Subject,Id,X);
       .send(Ag, tell, X).
+!start : true 
   <- agent.syncInOutExchange("connect_to_sl", "rasika", X);
    //  .println("*************", X);	
      //.broadcast(tell, x(5)); 
     // .wait(1500);
     // create_zookeeper_node;
     .println("Sending tell vl(10)");
      .send(container0000000051_maria, tell, vl(10)[xx]).
	//.send(maria, tell, vl(10)[hi(a)]).
	