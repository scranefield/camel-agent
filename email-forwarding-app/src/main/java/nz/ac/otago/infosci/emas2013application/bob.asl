users([a,b,c,d,e,f,crast78p]).

/* index(+Item, +List, -Index).
   Fails if List is empty
*/
index(A, [A|_], 0).
index(A, [H|T], I) :-
  A \== H &
  index(A, T, I2) &
  I = I2 + 1.

/* sublist(+L, +Start, +NumToTake, -SubList) */
sublist([], _, _, []).
sublist([_|_], 0, 0, []).
sublist([H|T], 0, NumToTake, SubList) :-
  NumToTake > 0 &
  NumToTake2 = NumToTake - 1 &
  SubList = [H | SubList2] &
  sublist(T, 0, NumToTake2, SubList2).
sublist([H|T], Start, NumToTake, SubList) :-
   Start > 0 &
   Start2 = Start - 1 &
   sublist(T, Start2, NumToTake, SubList).

!start.

+!start
   <- .my_name(Name);
      .println("Hi from ", Name);
      rules.add_rule("relevant(crast78p, From, _, _) :- .substring('Cranefield', From)");
      .wait(4000);
      register.

+agents(L) : .length(L, 0) <-
  .println("There are no agents detected (not even me!)");
  .abolish(my_users(_)).

+agents(L) : .length(L, NumAgents) & NumAgents > 0 <-
  .my_name(Me);
  ?index(Me, L, I);
  .println("Agents: ", L);
  .println("My index: ", I);
  ?users(Users);
  .length(Users, NumUsers);
  Div = NumUsers div NumAgents;
  ?sublist(Users, I*Div, Div, MainDeal);
  MyLeftOverIndex = NumAgents*Div + I;
  if ( MyLeftOverIndex < NumUsers ) {  // Last index is NumUsers-1
    .nth(MyLeftOverIndex, Users, LeftOver);
    MyUsers = [LeftOver | MainDeal ];
  } else {
    MyUsers = MainDeal;
  }
  -+my_users(MyUsers);
  .println("My users: ", MyUsers).

+!check_relevance(ID, From, Subject, Body) <-
    ?my_users(Users);
    .findall(User, (.member(User, Users) & relevant(User, From, Subject, Body)), RelevantUsers);
    .println("Relevant users: ", RelevantUsers);
     if (RelevantUsers \== []) {
       .send(router, tell, relevant(ID, RelevantUsers))
     }.
