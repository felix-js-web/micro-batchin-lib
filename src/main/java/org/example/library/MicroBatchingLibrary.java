package org.example.library;

import org.example.library.job.Job;
import org.example.library.job.JobResult;
import org.example.library.job.impl.SampleJobResult;
import org.example.library.processor.BatchProcessor;
import org.example.library.utils.Logger;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// MicroBatchingLibrary
public class MicroBatchingLibrary {

    private final int batchSize;
    private final int numberOfThreads;
    private final long batchIntervalMillis;
    private final BatchProcessor batchProcessor;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService workerPool;
    private final List<Job> currentBatch;
    private final AtomicBoolean isShuttingDown;
    private static final List<Future<JobResult>> listSync = Collections.synchronizedList(new ArrayList<>());

    LocalDateTime startTime = LocalDateTime.now();
    LocalDateTime endTime = LocalDateTime.now();

    static AtomicInteger ATOMIC_SUBMISSION_COUNT = new AtomicInteger();

    public MicroBatchingLibrary(int batchSize,
                                long batchIntervalMillis,
                                BatchProcessor batchProcessor,
                                int numberOfThreads,
                                boolean useVirtualThreads) {

        this.numberOfThreads = numberOfThreads;
        this.batchSize = batchSize;
        this.batchIntervalMillis = batchIntervalMillis;
        this.batchProcessor = batchProcessor;
        // TODO NEXT FEATURE this can become an option of Non Virtual Pre Java 21 standard threads implementation
        if (!useVirtualThreads) {
            this.scheduler = Executors.newScheduledThreadPool(numberOfThreads);
            this.workerPool = Executors.newFixedThreadPool(numberOfThreads);
        } else {
            ThreadFactory factory = Thread.ofVirtual().factory();
            this.scheduler = Executors.newScheduledThreadPool(0, factory);
            this.workerPool = Executors.newVirtualThreadPerTaskExecutor();
        }
        this.currentBatch = new ArrayList<>();
        this.isShuttingDown = new AtomicBoolean(false);

        startBatchScheduler();

    }

    //TODO for now keeping it synchronised - warehouse problem with one input good sequence in and one batch sending point
    // do we need it to make it multiple ot accept and batch? will reuse it with concurrency safe data structure
    // later and make it an option use with synchronised or not
    public synchronized Future<JobResult> submitJob(Job job) {
        //        Logger.log("---   SUBMIT JOB   -----  current batch size at submission " + currentBatch.size());

        if (isShuttingDown.get()) {
            //UnComment only for testing purposes
            Logger.log("---   LIBRARY IS BEEN SHUTDOWN NEEDS A RESTART NO MORE JOBS CAN BE SUBMITTED ");
            return null;
            // throw new RejectedExecutionException("---   SUBMIT JOB   -----  MicroBatchingLibrary is shutting down, cannot accept new jobs");
        }

        CompletableFuture<JobResult> jobResultFuture = new CompletableFuture<>();

        //TODO NEXT FEATURE as you CAN SEE there is no linkage in IDs between SUBMITTED JOB and EXECUTED JOB results
        // Ideally JOB Result should have submitted Job Id looking at if I can still do it
        // but then it can be just one functional interface approach
        // I might need to go with more complex structure to keep ID Request or response string and success status
        currentBatch.add(() -> {
            String executionResult = job.execute();
            SampleJobResult sampleJobResult = new SampleJobResult(executionResult);
            jobResultFuture.complete(sampleJobResult);
            return executionResult;
        });

        if (currentBatch.size() >= batchSize) {
            Logger.log(String.format("---   SUBMITTING JOBS  ----- current batch list size is %d  batchSize %d", currentBatch.size() , batchSize));
            processBatch();
        }
        listSync.add(jobResultFuture);
        ATOMIC_SUBMISSION_COUNT.getAndIncrement();
        return jobResultFuture;

    }

    public boolean checkIfShutDown() {
        return scheduler.isShutdown() && workerPool.isShutdown() && currentBatch.isEmpty();
    }

    public synchronized void shutdown() {

        Logger.log("---   SHUT DOWN   -----  starting library shutting down");
        Logger.log("---   SHUT DOWN   -----   current batch size is " + currentBatch.size());
        isShuttingDown.set(true);

        Logger.log("---   SHUT DOWN   -----   in synchronised current batch size is " + currentBatch.size());
        if (!currentBatch.isEmpty()) {
            processBatch();
        }

        scheduler.shutdown();
        workerPool.shutdown();

        try {
            scheduler.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            workerPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        endTime = LocalDateTime.now();
    }

    public void doReconcileAtTheEnd(int numberOfJobsPerThread, int numberOfSubmissionThreads) {
        long diff = ChronoUnit.NANOS.between(startTime, endTime);
        Logger.log(String.format("RECONCILIATION FINAL STATS ARE   PARAMS   --- NUMBER OF JOBS %d  --  WILL RUN THIS NUMBER IN NUMBER OF THREADS  %d   --  WORKING THREADS  %d  -- BATCH SIZE %d   -- BATCH INTERVAL IN MILLIS  %d", numberOfJobsPerThread, numberOfSubmissionThreads, numberOfThreads, batchSize, batchIntervalMillis));
        Logger.log(String.format("RECONCILIATION FINAL STATS ARE   RECONCILE/FACTS --- number of jobs planned to be submitted %d  -- number of jobs submitted %d  -- number of jobs executed  %s  -- number of BATCHES executed  %s   -- STARTTIME  %s     -- ENDTIME %s ", numberOfJobsPerThread * numberOfSubmissionThreads, ATOMIC_SUBMISSION_COUNT.get(), batchProcessor.ATOMIC_JOB_COUNT_EXECUTED_BY_BATCH_PROCESSOR.toString(), batchProcessor.ATOMIC_BATCHES_COUNT_EXECUTED_BY_BATCH_PROCESSOR.toString(), Logger.dtf.format(startTime), Logger.dtf.format(endTime)));
        Logger.log("RECONCILIATION FINAL STATS ARE   ---  diff is " + diff + " in seconds it is  " + ((double) diff / 1_000_000_000.0));
    }

    private void startBatchScheduler() {
        scheduler.scheduleAtFixedRate(this::scheduledProcessBatch, batchIntervalMillis, batchIntervalMillis, TimeUnit.MILLISECONDS);
    }

    private synchronized void scheduledProcessBatch() {
        Logger.log("---   SCHEDULED PROCESS  BATCH   -----  PER SCHEDULE submitting to worker pool the following number of items" + currentBatch.size());
        processBatch();
    }

    //TODO for now keeping it synchronised - warehouse problem with one goods in and one batch sending point
    // do we need it to make it multiple ot accept and batch? will reuse it with concurrency safe data structure
    // later and make it an option use with synchronised or not
    private synchronized void processBatch() {

        if (currentBatch.isEmpty()) {
            return;
        }

        List<Job> batchToProcess = new ArrayList<>(currentBatch);
        currentBatch.clear();

        //Logger.log("---   PROCESS  BATCH   -----  submitting to worker pool the following number of items" + batchToProcess.size());
        // TODO NEXT FEATURE
        //  here we might need a batches processor system - give each batch a number
        //  then store a history and then see results and reports by batch
        //  also we will need batch processor to give us stats of haw many been processed
        //  QUICK HACK atomic variable for number of batches in batch processor
        //Logger.log("---   PROCESS  BATCH   -----  BATCH NUMBER BEEN SENT TO IS " + batchProcessor.ATOMIC_BATCHES_COUNT_EXECUTED_BY_BATCH_PROCESSOR.getAndIncrement());
        Logger.log(String.format("---   PROCESS  BATCH   -----  starting batch process NUMBER  %d  with SIZE %d ", batchProcessor.ATOMIC_BATCHES_COUNT_EXECUTED_BY_BATCH_PROCESSOR.getAndIncrement(), batchToProcess.size()));
        workerPool.submit(() -> batchProcessor.process(batchToProcess));
        // TODO NEXT FEATURE
        //  We should make sure in summary the number of jobs ran is equal
        //  to the number of jobs submitted
        //  usually in EDA it is called a RECONCILE process
        //  Possible because I used list some jobs were lost between submit and clear - however method is synchronised still not a workaround for now
        //  not ideal can be a bottleneck - should be more flexible for being able to handle more load
        //  need to think how to make sue every submitted job is executed - relates to in MAIN class
        //  I will be analysing the FUTUREs until they complete - logically if some of them is not than
        //  somewhere I should have lost execution of it


        // TODO NEXT FEATURE
        //  We could also link each JobResult and Batch Job ID and Batch ID
        //  - for now only Batch Job ID is linked - I am looking IF I can get a catch ID there too
        //  - so that our results are known when and how they shipped - for now we just keeping those variable for numbers

    }

    private static void analyseJobResultsFutures(List<Future<JobResult>> jobResultList) {
        jobResultList.forEach(
                futureJobResult ->
                {
                    try {
                        Logger.log(String.format("FUTURE JOB RESULT IS  RESULT %s and RESULT IS %s ", String.valueOf(futureJobResult.isDone()), futureJobResult.get().getResponse()));
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    public void analyseJobResultsFutures() {
        analyseJobResultsFutures(listSync);
    }

}