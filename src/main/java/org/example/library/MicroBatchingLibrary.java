package org.example.library;

import org.example.job.Job;
import org.example.job.JobResult;
import org.example.job.impl.SampleJobResult;
import org.example.processor.BatchProcessor;
import org.example.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

// MicroBatchingLibrary
public class MicroBatchingLibrary {

    private final int batchSize;
    private final long batchIntervalMillis;
    private final BatchProcessor batchProcessor;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService workerPool;
    private final List<Job> currentBatch;
    private final AtomicBoolean isShuttingDown;

    public MicroBatchingLibrary(int batchSize,
                                long batchIntervalMillis,
                                BatchProcessor batchProcessor,
                                int numberOfThreads) {

        this.batchSize = batchSize;
        this.batchIntervalMillis = batchIntervalMillis;
        this.batchProcessor = batchProcessor;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.workerPool = Executors.newFixedThreadPool(numberOfThreads);
        this.currentBatch = new ArrayList<>();
        this.isShuttingDown = new AtomicBoolean(false);

        startBatchScheduler();

    }

    public Future<JobResult> submitJob(Job job) {
            //        Logger.log("---   SUBMIT JOB   -----  current batch size at submission " + currentBatch.size());

            if (isShuttingDown.get()) {
                //UnComment only for testing purposes
            //            Logger.log("---   LIBRARY IS BEEN SHOUTDOWN NEEDS A RESTART NO MORE JOBS CAN BE SUBMITTED " + currentBatch.size());
            return null;
            // throw new RejectedExecutionException("---   SUBMIT JOB   -----  MicroBatchingLibrary is shutting down, cannot accept new jobs");
        }

        CompletableFuture<JobResult> jobResultFuture = new CompletableFuture<>();

        currentBatch.add(() -> {
            job.execute();
            jobResultFuture.complete(new SampleJobResult(true));
        });

        if (currentBatch.size() >= batchSize) {
            Logger.log("---   SUBMIT JOB   -----  batchSize" + batchSize);
            Logger.log("---   SUBMIT JOB   -----  current batch" + currentBatch.size());
            processBatch();
        }

        return jobResultFuture;

    }

    public void shutdown() {

        Logger.log("---   SHUT DOWN   -----  starting library shutting down");
        Logger.log("---   SHUT DOWN   -----   current batch size is " + currentBatch.size());
        isShuttingDown.set(true);

        synchronized (this) {
            Logger.log("---   SHUT DOWN   -----   in synchronised current batch size is " + currentBatch.size());
            if (!currentBatch.isEmpty()) {
                processBatch();
            }
        }

        scheduler.shutdown();
        workerPool.shutdown();

        try {
            scheduler.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            workerPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    private void startBatchScheduler() {

        //scheduler.scheduleAtFixedRate(this::processBatch, batchIntervalMillis, batchIntervalMillis, TimeUnit.MILLISECONDS);

    }

    private void processBatch() {
        //lets add batches number and show when we exiting and when we entering
        Logger.log("---   PROCESS  BATCH   -----  starting batch process");
        Logger.log("---   PROCESS  BATCH   -----  current batch size " + currentBatch.size());

        if (currentBatch.isEmpty()) {
            return;
        }

        List<Job> batchToProcess = new ArrayList<>(currentBatch);
        currentBatch.clear();

        Logger.log("---   PROCESS  BATCH   -----  submitting to worker pool " + batchToProcess.size());
        // NEXT FEATURE
        // here we might need a batches processor system - give each batch a number
        // then store a history and then see results and reports by batch
        // also we will need batch processor to give us stats of haw many been processed
        workerPool.submit(() -> batchProcessor.process(batchToProcess));
        // NEXT FEATURE
        // We should make sure in summary the number of jobs ran is equal
        // to the number of jobs submitted
        // usually in EDA it is called a RECONCILE process
        // Possible because I used list some jobs were lost between submit and clear - however method is synchronised still not a workaround for now
        // not ideal can be a bottleneck - should be more flexible for being able to handle more load
        // need to think how to make sue every submitted job is executed - relates to in MAIN class
        // I will be analysing the FUTUREs until they complete - logically if some of them is not than
        // somewhere I should have lost execution of it

    }

}