package org.example;

import org.example.job.Job;
import org.example.job.JobResult;
import org.example.job.impl.SampleJob;
import org.example.library.MicroBatchingLibrary;
import org.example.processor.BatchProcessor;
import org.example.utils.Logger;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Main {

    private static final int NUMBER_OF_JOBS = 1559;
    private static final int NUMBER_OF_THREADS = 100;

    private static final int BATCH_SIZE = 13;

    private static final int BATCH_INTERVAL_MILLIS = 2000;

    private static final int SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS = 9;

    private static final int SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS_AFTER_SHUTDOWN = 1;

    private static final List<Future<JobResult>> listSync = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        // Some Fake implementation of Batch Processor for testing purposes
        BatchProcessor batchProcessor = (jobs) -> jobs.forEach(Job::execute);


        MicroBatchingLibrary library = new MicroBatchingLibrary(BATCH_SIZE, BATCH_INTERVAL_MILLIS, batchProcessor, NUMBER_OF_THREADS);
        LocalDateTime startTime = LocalDateTime.now();

        submitInNumberOfThreadsToLibrary(library, SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        library.shutdown();

        submitInNumberOfThreadsToLibrary(library, SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS_AFTER_SHUTDOWN);

        analyseJobResultsFutures(listSync);

        LocalDateTime endTime = LocalDateTime.now();
        long diff = ChronoUnit.NANOS.between(startTime, endTime);
        Logger.log(String.format("GENERAL FINAL STATS ARE   --- NUMBER OF JOBS %d  --  WILL RUN THIS NUMBER IN NUMBER OF THREADS  %d   --  WORKING THREADS  %d  -- BATCH SIZE %d   -- BATCH INTERVAL IN MILLIS  %d", NUMBER_OF_JOBS, SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS, NUMBER_OF_THREADS, BATCH_SIZE, BATCH_INTERVAL_MILLIS));
        Logger.log(String.format("GENERAL FINAL STATS ARE   RECONCILE --- number of jobs %d  -- number of jobs executed  %s  -- number of BATCHES executed  %s   -- STARTTIME  %s     -- ENDTIME %s ", NUMBER_OF_JOBS * SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS, batchProcessor.ATOMIC_JOB_COUNT_EXECUTED_BY_BATCH_PROCESSOR.toString(), batchProcessor.ATOMIC_BATCHES_COUNT_EXECUTED_BY_BATCH_PROCESSOR.toString(), Logger.dtf.format(startTime), Logger.dtf.format(endTime)));
        Logger.log("GENERAL FINAL STATS ARE   ---  diff is " + diff + " in seconds it is  " + ((double) diff / 1_000_000_000.0));

    }

    private static void analyseJobResultsFutures(List<Future<JobResult>> jobResultList) {
        jobResultList.forEach(
                futureJobResult ->
                {
                    try {
                        Logger.log(String.format("FUTURE JOB RESULT IS  RESULT %s and RESULT IS %s ", String.valueOf(futureJobResult.isDone()), futureJobResult.get().getResponse()));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    private static void submitInNumberOfThreadsToLibrary(MicroBatchingLibrary library, int numberOfThreadsSubmitting) {
        for (int i = 0; i < numberOfThreadsSubmitting; i++) {
            final int threadNumber = i;
            Runnable submitToLibraryInThread = () -> {
                submitJobsToLibrary(library, numberOfThreadsSubmitting, threadNumber);
            };
            new Thread(submitToLibraryInThread).start();
        }
    }

    private static void submitJobsToLibrary(MicroBatchingLibrary library, int numberOfThreadsSubmitting, int threadNumber) {
        for (int i = 0; i < NUMBER_OF_JOBS; i++) {
            Future<JobResult> jobResultFuture = library.submitJob(new SampleJob(String.format("This Job has a Number %d from %d planned and been submitted from the Thread number %d", i, NUMBER_OF_JOBS, threadNumber)));
            if (jobResultFuture != null)
                // TODO NEXT FEATURE Avoid the SHUTDOWN return for Null Values I know it can be handled more gracefully
                listSync.add(jobResultFuture);
        }
    }

}