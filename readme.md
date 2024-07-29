# Micro-batching library
	Micro-batching is a technique used in processing pipelines where individual tasks are grouped
	together into small batches. This can improve throughput by reducing the number of requests made to a downstream system.

Your task is to implement a micro-batching library, with the following
requirements:
- it should allow the caller to submit a single Job, and it should return a JobResult
- it should process accepted Jobs in batches using a BatchProcessor
  - Don't implement BatchProcessor. This should be a dependency of your library.
- it should provide a way to configure the batching behaviour i.e. size and frequency
- it should expose a shutdown method which returns after all previously accepted Jobs are processed

We will be looking for:
- a well designed API
- usability as an external library
- good documentation/comments
- a well written, maintainable test suite

The requirements above leave some unanswered questions, so you will need to use your judgement to design the most useful library.

EXECUTION
the tools used
- intellij, Java 21, Gradle - standard project generated in Intellij. (please do gradle clean and build before running classes or tests in Intellij)
  the resources used
- Internet, google, chat gpt
- links [Baeldung batching](https://www.baeldung.com/java-smart-batching "Baeldung batching")
- links [Jenkov microbatching Principles](https://jenkov.com/tutorials/java-performance/micro-batching.html "Jenkov microbatching Principles")
- FYI there is a GO repository with this task and working version - candidae to dig when GO UpSkilled

DETAILS - HIGH LEVEL
Delivery central class **MicroBatchingLibrary class and package library **is something can be separated and abstracted as library
- Single class doing all atm
- for future improvements can be a central workflow class running multiple services e.g. job management service, batch talking service, reconciliation service.
- Helper classes like logger class, Job JobResult and other small ones also provided.

DETAILS - DEEP DIVE **MicroBatchingLibrary class **
- constructor accepts **numberOfThreads** number of worker threads for executor, **batchIntervalMillis**  of millis for scheduled executor, **batchSize**, **useVirtualThreads** boolean for Virtual Threads usage (Using Java 21 Virtual Threads or Normal threads as an option - speed analysis on basic end to end test 3X to 4X virtual threads are quicker to execute things with executors in batches than usual threads - numbers provided)
- **submitJob(Job job) ** does submission into arraylist (note method is synchronised, non synchronised version been used 5% of submitted works get lost between submit and clear in race condition) (another note - Baeldung is using Queue Blocking and LinkedQueues which are thread safe - in future versions plan was to go from SYNCHRONISED to using the proper data structures - TODO commented in code) , after submission batchsize is chacked and if needed batch proccess is called. Before submitting shutdown thread safe variable check done.
- **void shutdown()**  does start up shutdown process and after falsing atomic variable goes to draining the worker pools and shutting them down, public check shutdown also available to be called from outside for those purposes - used in unit testing.
- **public void doReconcileAtTheEnd** this one might seem unnecessary but it is not. Once I started test thigns end to end with 100Ks in numbers - I needed a reconcile - similar warehouse problem - how many items submitted, how many items planned to be submitted, how many items (jobs been) executed, in how many threads, for how much time. ( This one was very crticial for me to be able clearly see e.g. how Executor worker pool works with 1 thread or 100 thread - you can regulate it for your type of job and fidn the most optimal path, time execution take was used e.g. how much time same things done with Virtual threads and old style - was mind blowing to see 3x to 4x difference - simply shocking. My guess my job was bit of CPU usage may be thats why ). Another important thing to note - Job Submitted - have a counter - I used kinda. closure style there -), and jobs and batches executed also have their counters - so printing that out at summary in reconcile helped me to see and fix whats wrong and I believe for anyone using library the sample of usage would be very imporant.
- **analyseJobResultsFutures** simply analyses all executed jobs futures results and prints them out - incase it is not needed we just might not call it - however also might be important for end to end testing purposes to debug thigns easier

TODOS - OVERVIEW
- **MicroBatchingLibrary** is do it all class - definitely a candidate for a future splitting for SOLID principles
- Data Structures - was so willing to use Thread Safe Queues or Lists similar to Baeldung style - but a was trying to minimise the time spend and kinda have achieved what was required I believe with whats in hand - so for future improvements (Principle of multiple submission points to warehouse and multiple points of batch shipping in warehouse - I believed those ones are bigger topic and did not deviate atm)
- Logger ideally would need SLF4J fo rolling files to have all the logs
- jobs when executed not labelled for batch ids they are executed in - would been ideal if this one added in the future. ( Principle is Every job submitted - had Every Batch Job Id and Batch Id assigned to it - so that goods not lost in warehouse and very clear by which number and in which batch they left warehouse  )
- Reconcile of course could do more then just printing lines - analysing what was missed or not based on numbers - but thats for future growth
- **MainFullEndToEndTest** class could be extended to be like a full suite of end to end tests with different options not just setup data once and run once view results and then repeat again option.

Testing
- Unit tests written for key methods in MicroBatchLibrary
- Main class which runs all end to end with sample job implementation is MainFullEndToEndTest
- please use MainFullEndToEndTest class to run with different params like number of threads, time in millis for scheduled execution, number of jobs, you can submit jobs in different threads in different times after thread sleep.
- Suggestion would be start with smaller numbers to get ok with logging and reconcile numbers, personally tested on 100K up to 500K was working fine was no problems. (NOTE when want to try to use not SYNCHRONISED main methods - please dont analyse futures cause analysis is blocking - and when not SYNCHRONISED - 5 to10 % jobs lost and those futures never complete so it hangs, to see reconcile is fine just comment analyse futures or it will hang)

Big thank you so much for such a beautiful weekend I had thinking and learning those - I would appreciate your feedback - agree probably more time I would have spent but those were the ones I had to not miss to - biiiiiig apologies. (Had a citizenship ceremony and Java CoPilot meetup speech in front of 30+ people all kept me busy and not able to focus on coding till last days of week duration)
P.S. If allowed happy to take more time and improve on suggested comments.