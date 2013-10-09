+tick(1) <-
  +last_tick(1);
  .println("!!! Got tick 1").
+tick(Count) : Count > 1 & last_tick(Count-1) <-
  -+last_tick(Count);
  .println("!!! Got tick ", Count-1).
+tick(Count) : Count > 1 & not last_tick(Count-1) <- 
  -+last_tick(Count);
  .println("!!! Missed tick ", Count-1).