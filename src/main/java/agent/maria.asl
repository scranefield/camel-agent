// Agent maria

vl(1).
vl(2).

!start.
/* Plan triggered when an achieve message is received.
   It is like a new goal, but with a different source.
*/
+!goto(X,Y)[source(Ag)] : true
   <- .println("------------------------Received achieve ",goto(X,Y)," from ", Ag).

+vl(X)[source(Ag)] : X=10
   <- .println("_______________Received tell ", vl(X), " from ", Ag).

+x(X)[source(Ag)] : X=5
   <- .println("_______________Received tell ", x(X), " from ", Ag).

+test: true
<- .println("+++++++ received test").

+tick(X):true
<- .println("++++++++++++++++**********************++++ tick", X).


//+!check_relevance(Id,From,Subject,Body)[source(Ag)]:true
 //   <- agent.getValidUsers(From,Subject,Id,X);
    //   .send(Ag, tell, X).

+!start : true 
<- // agent.syncInOutExchange("get_users", X);   
   .println("++++++++++++++++**********************").

