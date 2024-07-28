package org.example;

import org.example.library.job.Job;
import org.example.library.job.JobResult;
import org.example.library.MicroBatchingLibrary;
import org.example.library.processor.BatchProcessor;
import org.example.library.utils.Logger;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class MainFullTest {

    private static final int NUMBER_OF_JOBS = 1;
    private static final int NUMBER_OF_THREADS = 100;

    private static final int BATCH_SIZE = 10;
    private static final boolean USE_VIRTUAL_THREADS = true;

    private static final int BATCH_INTERVAL_MILLIS = 2000;

    private static final int NUMBER_OF_MILLIS_BEFORE_SHUTDOWN_CALLED = 3000;

    private static final int SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS = 1;

    private static final int SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS_AFTER_SHUTDOWN = 1;

    private static final List<Future<JobResult>> listSync = Collections.synchronizedList(new ArrayList<>());

    static AtomicInteger ATOMIC_SUBMISSION_COUNT = new AtomicInteger();

    public static void main(String[] args) {
        // Some Fake implementation of Batch Processor for testing purposes
        BatchProcessor batchProcessor = (jobs) -> jobs.forEach(Job::execute);


        MicroBatchingLibrary library = new MicroBatchingLibrary(BATCH_SIZE,
                BATCH_INTERVAL_MILLIS, batchProcessor, NUMBER_OF_THREADS,USE_VIRTUAL_THREADS);
        LocalDateTime startTime = LocalDateTime.now();

        submitInNumberOfThreadsToLibrary(library, SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS);

        try {
            Thread.sleep(NUMBER_OF_MILLIS_BEFORE_SHUTDOWN_CALLED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        library.shutdown();

        submitInNumberOfThreadsToLibrary(library, SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS_AFTER_SHUTDOWN);

        analyseJobResultsFutures(listSync);

        LocalDateTime endTime = LocalDateTime.now();
        long diff = ChronoUnit.NANOS.between(startTime, endTime);
        Logger.log(String.format("GENERAL FINAL STATS ARE   PARAMS   --- NUMBER OF JOBS %d  --  WILL RUN THIS NUMBER IN NUMBER OF THREADS  %d   --  WORKING THREADS  %d  -- BATCH SIZE %d   -- BATCH INTERVAL IN MILLIS  %d", NUMBER_OF_JOBS, SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS, NUMBER_OF_THREADS, BATCH_SIZE, BATCH_INTERVAL_MILLIS));
        Logger.log(String.format("GENERAL FINAL STATS ARE   RECONCILE/FACTS --- number of jobs planned to be submitted %d  -- number of jobs submitted %d  -- number of jobs executed  %s  -- number of BATCHES executed  %s   -- STARTTIME  %s     -- ENDTIME %s ", NUMBER_OF_JOBS * SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS, ATOMIC_SUBMISSION_COUNT.get(), batchProcessor.ATOMIC_JOB_COUNT_EXECUTED_BY_BATCH_PROCESSOR.toString(), batchProcessor.ATOMIC_BATCHES_COUNT_EXECUTED_BY_BATCH_PROCESSOR.toString(), Logger.dtf.format(startTime), Logger.dtf.format(endTime)));
        Logger.log("GENERAL FINAL STATS ARE   ---  diff is " + diff + " in seconds it is  " + ((double) diff / 1_000_000_000.0));

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
            // make sure there are submissions in delays otherwise it will not look natural and CPU only submits and then executes
            // TODO NEXT FEATURE
            //  This one very interesting - it might seem to be breaking the code initially
            //  and that was my first feeling but actually it stretches the submission and there are moments when shutdown been called
            //  but submissions still ongoing even if they not accepted
            //  here we can add logging plus in future we can add more visibility for the process
            //  Clean the delay if you want less complex case example
//            try {
//                Thread.sleep(720 * i);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
            Logger.log("    PRE SUBMISSION " + String.format("This Job has a Number %d from %d planned and been submitted from the Thread number %d", i, NUMBER_OF_JOBS, threadNumber));
            Future<JobResult> jobResultFuture = library.submitJob(new SampleJob(String.format("This Job has a Number %d from %d planned and been submitted from the Thread number %d", i, NUMBER_OF_JOBS, threadNumber)));
            if (jobResultFuture != null) {
                // TODO NEXT FEATURE Avoid the SHUTDOWN return for Null Values I know it can be handled more gracefully
                //  job been submitted lets increase submission counter
                ATOMIC_SUBMISSION_COUNT.getAndIncrement();
                listSync.add(jobResultFuture);
            }
        }
    }

}