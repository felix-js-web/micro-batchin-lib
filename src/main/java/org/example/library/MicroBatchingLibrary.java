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

    public synchronized Future<JobResult> submitJob(Job job) {
        Logger.log("---   SUBMIT JOB   -----  current batch size at submission " + currentBatch.size());

        if (isShuttingDown.get()) {
            Logger.log("---   LIBRARY IS BEEN SHOUTDOWN NEEDS A RESTART NO MORE JOBS CAN BE SUBMITTED " + currentBatch.size());
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

    private synchronized void processBatch() {
        Logger.log("---   PROCESS  BATCH   -----  starting batch process");
        Logger.log("---   PROCESS  BATCH   -----  current batch size " + currentBatch.size());

        if (currentBatch.isEmpty()) {
            return;
        }

        List<Job> batchToProcess = new ArrayList<>(currentBatch);
        currentBatch.clear();

        Logger.log("---   PROCESS  BATCH   -----  submitting to worker pool " + batchToProcess.size());
        workerPool.submit(() -> batchProcessor.process(batchToProcess));

    }

}