package org.example;

import org.example.job.Job;
import org.example.library.MicroBatchingLibrary;
import org.example.processor.BatchProcessor;
import org.example.utils.Logger;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.example.job.Job.ATOMIC_JOB_COUNT;

public class Main {

    private static final int NUMBER_OF_JOBS = 1200;
    private static final int NUMBER_OF_THREADS = 100;

    public static void main(String[] args) {
        BatchProcessor batchProcessor = jobs -> jobs.forEach(Job::execute);

        MicroBatchingLibrary library = new MicroBatchingLibrary(10, 5000, batchProcessor, NUMBER_OF_THREADS);
        LocalDateTime startTime = LocalDateTime.now();


        // Lambda Runnable
        Runnable submitToLibraryInThread1 = () -> { submitJobsToLibrary(library); };
        Runnable submitToLibraryInThread2 = () -> { submitJobsToLibrary(library); };
        Runnable submitToLibraryInThread3 = () -> { submitJobsToLibrary(library); };
        Runnable submitToLibraryInThread4 = () -> { submitJobsToLibrary(library); };
        Runnable submitToLibraryInThread5 = () -> { submitJobsToLibrary(library); };
        Runnable submitToLibraryInThread6 = () -> { submitJobsToLibrary(library); };
        Runnable submitToLibraryInThread7 = () -> { submitJobsToLibrary(library); };
        Runnable submitToLibraryInThread8 = () -> { submitJobsToLibrary(library); };
        Runnable submitToLibraryInThread9 = () -> { submitJobsToLibrary(library); };
        Runnable submitToLibraryInThread10 = () -> { submitJobsToLibrary(library); };
        // start the thread
        new Thread(submitToLibraryInThread1).start();
        new Thread(submitToLibraryInThread2).start();
        new Thread(submitToLibraryInThread3).start();
        new Thread(submitToLibraryInThread4).start();
        new Thread(submitToLibraryInThread5).start();
        new Thread(submitToLibraryInThread6).start();
        new Thread(submitToLibraryInThread7).start();
        new Thread(submitToLibraryInThread8).start();
        new Thread(submitToLibraryInThread9).start();
        new Thread(submitToLibraryInThread10).start();


        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        library.shutdown();
        new Thread(submitToLibraryInThread1).start();
        LocalDateTime endTime = LocalDateTime.now();

        long diff = ChronoUnit.NANOS.between(startTime, endTime);

        Logger.log("  number of jobs " + NUMBER_OF_JOBS + "  STARTTIME " + Logger.dtf.format(startTime) + "  ENDTIME " + Logger.dtf.format(endTime));
        Logger.log("  diff is " + diff + " in seconds it is  " + ((double) diff / 1_000_000_000.0));
    }

    private static void submitJobsToLibrary(MicroBatchingLibrary library) {
        for (int i = 0; i < NUMBER_OF_JOBS; i++) {
            library.submitJob(() -> {
                Integer currentJobId = ATOMIC_JOB_COUNT.getAndIncrement();
                Logger.log("------   JOB EXECUTE before SLEEP ---- Job executed by thread for JOB ID " + Thread.currentThread().getName() + "   " + currentJobId);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                Logger.log("------   JOB EXECUTE  ---- Job executed by thread for JOB ID  " + Thread.currentThread().getName() + "   " + currentJobId);
            });
        }
    }

}