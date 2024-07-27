package org.example;

import org.example.job.Job;
import org.example.job.impl.SampleJob;
import org.example.library.MicroBatchingLibrary;
import org.example.processor.BatchProcessor;
import org.example.utils.Logger;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Main {

    private static final int NUMBER_OF_JOBS = 7;
    private static final int NUMBER_OF_THREADS = 100;

    private static final int BATCH_SIZE = 3;

    private static final int BATCH_INTERVAL_MILLIS = 5000;

    private static final int SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS = 50;

    private static final int SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS_AFTER_SHUTDOWN = 1;

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

        LocalDateTime endTime = LocalDateTime.now();
        long diff = ChronoUnit.NANOS.between(startTime, endTime);
        Logger.log(String.format("GENERAL FINAL STATS ARE   --- NUMBER OF JOBS %d  --  WILL RUN THIS NUMBER IN NUMBER OF THREADS  %d   --  WORKING THREADS  %d  -- BATCH SIZE %d   -- BATCH INTERVAL IN MILLIS  %d", NUMBER_OF_JOBS, SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS, NUMBER_OF_THREADS, BATCH_SIZE, BATCH_INTERVAL_MILLIS));
        Logger.log(String.format("GENERAL FINAL STATS ARE   RECONCILE --- number of jobs %d  -- number of jobs executed  %s   -- STARTTIME  %s     -- ENDTIME %s ", NUMBER_OF_JOBS * SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS, batchProcessor.ATOMIC_JOB_COUNT_EXECUTED_BY_BATCH_PROCESSOR.toString(), Logger.dtf.format(startTime), Logger.dtf.format(endTime)));
        Logger.log("GENERAL FINAL STATS ARE   ---  diff is " + diff + " in seconds it is  " + ((double) diff / 1_000_000_000.0));

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
            library.submitJob(new SampleJob(String.format("This Job has a Number %d from %d planned and been submitted from the Thread number %d", i, NUMBER_OF_JOBS, threadNumber)));
        }
    }

}