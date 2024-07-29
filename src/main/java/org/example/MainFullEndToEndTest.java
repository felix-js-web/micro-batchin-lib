package org.example;

import org.example.library.job.Job;
import org.example.library.MicroBatchingLibrary;
import org.example.library.processor.BatchProcessor;
import org.example.library.utils.Logger;

public class MainFullEndToEndTest {

    // this is number of jobs you want to submit in End to End test - will be submitted by each  thread numbers below
    private static final int NUMBER_OF_JOBS = 1230;

    // the above number of jobs will be submitted for library executors to use as a number of threads
    private static final int NUMBER_OF_EXECUTOR_THREADS = 20;

    // Batch Size before batch sending kicks in
    private static final int BATCH_SIZE = 25;

    // if set to false old style threads are used - suggested to keep it to true, but set it to false for testing purposes
    private static final boolean USE_VIRTUAL_THREADS = true;

    // number of millis before the scheduled executor kicks in to send batches
    private static final int BATCH_INTERVAL_MILLIS = 2000;

    // number of millis our End to End test freezes for before it calls shutdown
    private static final int NUMBER_OF_MILLIS_BEFORE_SHUTDOWN_CALLED = 3000;

    // the above number of jobs will be submitted by this number of threads - so overall jobs = NUMBER_OF_JOBS*NUMBER_OF_THREADS
    private static final int SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS = 1;

    // the above number of jobs will be submitted by this number of threads  after shutdown
    // called - this one for testing purposes usually 1 is enough
    private static final int SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS_AFTER_SHUTDOWN = 1;

    public static void main(String[] args) {
        // Some Fake implementation of Batch Processor for testing purposes
        BatchProcessor batchProcessor = (jobs) -> jobs.forEach(Job::execute);


        MicroBatchingLibrary library = new MicroBatchingLibrary(BATCH_SIZE,
                BATCH_INTERVAL_MILLIS, batchProcessor, NUMBER_OF_EXECUTOR_THREADS, USE_VIRTUAL_THREADS);

        submitInNumberOfThreadsToLibrary(library, SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS);

        freezeForMillisAndCallShutdown(library, NUMBER_OF_MILLIS_BEFORE_SHUTDOWN_CALLED);

        // this one will assure that none been accepted after shutdown
        submitInNumberOfThreadsToLibrary(library, SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS_AFTER_SHUTDOWN);

        // TODO call a reconciliation process to log the data at the minimum
        //  at the minimum helps visually to see any bugs you have - like hanging or submitted-unsubmitted
        //  difference and etcs - Those numbers helped me a lot to identify my issues and what was not working or working not as expected
        library.doReconcileAtTheEnd(NUMBER_OF_JOBS, SUBMIT_TO_LIBRARY_IN_NUMBER_OF_THREADS);

        // TODO optional for you to analyse it or not but library to provide you full
        //  analysis and lock on it till future is resolved with printing out the response
        library.analyseJobResultsFutures();
    }

    private static void freezeForMillisAndCallShutdown(MicroBatchingLibrary library, int numberOfMillisBeforeShutdownCall) {
        try {
            Thread.sleep(numberOfMillisBeforeShutdownCall);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        library.shutdown();
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
            library.submitJob(new SampleJob(String.format("This Job has a Number %d from %d planned and been submitted from the Thread number %d", i, NUMBER_OF_JOBS, threadNumber)));
        }
    }

}