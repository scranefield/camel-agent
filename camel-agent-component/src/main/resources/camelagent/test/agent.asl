+tick(Count) : not last_tick(_) <-
  +last_tick(Count);
  .println("!!! First tick received: ", Count).
+tick(Count) : last_tick(OldCount) & Count = OldCount + 1 <-
  -+last_tick(Count);
  .println("!!! Got tick ", Count).
+tick(Count) : last_tick(OldCount) <-
  -+last_tick(Count);
  .println("!!! Missed ticks from ", OldCount+1, " to ", Count-1);
  .println("!!! Got tick ", Count).

+foo <- .println("!!! Got foo").
+bar <- .println("!!! Got bar").
