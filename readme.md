Micro-batching library
Micro-batching is a technique used in processing pipelines where individual tasks are grouped
together into small batches. This can improve throughput by reducing the number of requests made
to a downstream system. Your task is to implement a micro-batching library, with the following
requirements:
● it should allow the caller to submit a single Job, and it should return a JobResult
● it should process accepted Jobs in batches using a BatchProcessor
○ Don't implement BatchProcessor. This should be a dependency of your library.
● it should provide a way to configure the batching behaviour i.e. size and frequency
● it should expose a shutdown method which returns after all previously accepted Jobs are
processed
We will be looking for:
● a well designed API
● usability as an external library
● good documentation/comments
● a well written, maintainable test suite
The requirements above leave some unanswered questions, so you will need to use your judgement
to design the most useful library






Analyse Virtual threads vs Real thread -  Done 4-5 times speed difference just using  
virtual threads Java 21 - Mind BLowing

Find if you can visualise a memory usage and cpu usage in threads vs Virtual Threads
Yes With Grafana and Prometheus - did not have time to set up

For UnSynch we can use Queue - linked blocking queue 

STATS SYNCHRONIZED and UNSYNCHRONISED
then we will think of THREAD SAFE STRUCTURE


SYNCH

2024/07/28 06:13:59.467 ||| GENERAL FINAL STATS ARE   --- NUMBER OF JOBS 7000  --  WILL RUN THIS NUMBER IN NUMBER OF THREADS  9   --  WORKING THREADS  100  -- BATCH SIZE 25   -- BATCH INTERVAL IN MILLIS  5000
2024/07/28 06:13:59.467 ||| GENERAL FINAL STATS ARE   RECONCILE --- number of jobs 63000  -- number of jobs executed  63000   -- STARTTIME  2024/07/28 06:03:06.997     -- ENDTIME 2024/07/28 06:13:59.466
2024/07/28 06:13:59.476 ||| GENERAL FINAL STATS ARE   ---  diff is 652469696000 in seconds it is  652.469696



UNSYNCH WITH LOSSES

2024/07/28 06:25:46.705 ||| GENERAL FINAL STATS ARE   --- NUMBER OF JOBS 7000  --  WILL RUN THIS NUMBER IN NUMBER OF THREADS  9   --  WORKING THREADS  100  -- BATCH SIZE 25   -- BATCH INTERVAL IN MILLIS  5000
2024/07/28 06:25:46.705 ||| GENERAL FINAL STATS ARE   RECONCILE --- number of jobs 63000  -- number of jobs executed  62232   -- STARTTIME  2024/07/28 06:15:07.780     -- ENDTIME 2024/07/28 06:25:46.703
2024/07/28 06:25:46.713 ||| GENERAL FINAL STATS ARE   ---  diff is 638922831000 in seconds it is  638.922831






UNSYNCH MANY THREADS


2024/07/28 06:39:02.426 ||| GENERAL FINAL STATS ARE   --- NUMBER OF JOBS 7  --  WILL RUN THIS NUMBER IN NUMBER OF THREADS  50   --  WORKING THREADS  100  -- BATCH SIZE 3   -- BATCH INTERVAL IN MILLIS  5000
2024/07/28 06:39:02.426 ||| GENERAL FINAL STATS ARE   RECONCILE --- number of jobs 350  -- number of jobs executed  349   -- STARTTIME  2024/07/28 06:38:10.192     -- ENDTIME 2024/07/28 06:39:02.425
2024/07/28 06:39:02.443 ||| GENERAL FINAL STATS ARE   ---  diff is 52233674000 in seconds it is  52.233674

UNSYNCH - INTERESTING BEHAVIOUR SAME WORK 50 Sec - SYNCH is 12 SECS  BIT OF A SHOCK - MAY BE CONTEXT SWITCH
2024/07/28 06:44:50.297 ||| GENERAL FINAL STATS ARE   --- NUMBER OF JOBS 7  --  WILL RUN THIS NUMBER IN NUMBER OF THREADS  50   --  WORKING THREADS  100  -- BATCH SIZE 3   -- BATCH INTERVAL IN MILLIS  5000
2024/07/28 06:44:50.298 ||| GENERAL FINAL STATS ARE   RECONCILE --- number of jobs 350  -- number of jobs executed  346   -- STARTTIME  2024/07/28 06:43:58.604     -- ENDTIME 2024/07/28 06:44:50.296
2024/07/28 06:44:50.312 ||| GENERAL FINAL STATS ARE   ---  diff is 51692697000 in seconds it is  51.692697

SYNCH

2024/07/28 06:41:23.500 ||| GENERAL FINAL STATS ARE   --- NUMBER OF JOBS 7  --  WILL RUN THIS NUMBER IN NUMBER OF THREADS  50   --  WORKING THREADS  100  -- BATCH SIZE 3   -- BATCH INTERVAL IN MILLIS  5000
2024/07/28 06:41:23.500 ||| GENERAL FINAL STATS ARE   RECONCILE --- number of jobs 350  -- number of jobs executed  350   -- STARTTIME  2024/07/28 06:41:11.480     -- ENDTIME 2024/07/28 06:41:23.500
2024/07/28 06:41:23.508 ||| GENERAL FINAL STATS ARE   ---  diff is 12019728000 in seconds it is  12.019728