package org.example.library;

import org.example.job.Job;
import org.example.job.JobResult;
import org.example.processor.BatchProcessor;
import org.example.utils.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MicroBatchingLibraryTest {

    private MicroBatchingLibrary library;
    private BatchProcessor batchProcessor;
    private Job job;
    private int batchSize = 5;
    private long batchIntervalMillis = 1000;
    private int numberOfThreads = 2;

    @BeforeEach
    void setUp() {
        batchProcessor = mock(BatchProcessor.class);
        job = mock(Job.class);
        library = new MicroBatchingLibrary(batchSize, batchIntervalMillis, batchProcessor, numberOfThreads, false);
    }

    @Test
    void submitJobAddsMockedJobToBatchAndProcessWithFakeProccessor() throws ExecutionException, InterruptedException {
        Logger.log("running test");
        // Some Fake implementation of Batch Processor for testing purposes
        BatchProcessor batchProcessor = (jobs) -> jobs.forEach(Job::execute);
        MicroBatchingLibrary libraryMocked = new MicroBatchingLibrary(batchSize,
                batchIntervalMillis*20, //wait 20 seconds
                batchProcessor, numberOfThreads, false);
        String requestResponse = "result";
        when(job.execute()).thenReturn(requestResponse);
        CompletableFuture<JobResult> future = (CompletableFuture<JobResult>) libraryMocked.submitJob(job);
        assertNotNull(future);

        String futureResponse = future.get().getResponse();
        assertEquals("result", futureResponse);
        //logged lines to have visibility scheduled timer executed after 10 seconds
        // and future returned the response
        Logger.log("running test finished" + futureResponse);
    }

    @Test
    void submitJobProcessesBatchWhenBatchSizeReached() throws InterruptedException {
        when(job.execute()).thenReturn("result");
        for (int i = 0; i < batchSize; i++) {
            library.submitJob(job);
        }
        Thread.sleep(batchIntervalMillis / 2);
        // Not instant proccessing
        verify(batchProcessor, times(1)).process(anyList());

    }

    @Test
    void submitJobReturnsNullWhenShuttingDown() {
        library.shutdown();
        assertNull(library.submitJob(job));
    }

    @Test
    void shutdownProcessesRemainingJobs() {
        when(job.execute()).thenReturn("result");
        library.submitJob(job);
        library.shutdown();
        verify(batchProcessor, times(1)).process(anyList());
    }

    @Test
    void shutdownWaitsForAllTasksToCompleteAndExecutorsBeingShutdown() throws InterruptedException, ExecutionException {
        Logger.log("running test");
        // Some Fake implementation of Batch Processor for testing purposes
        BatchProcessor batchProcessor = (jobs) -> jobs.forEach(Job::execute);
        MicroBatchingLibrary libraryMocked = new MicroBatchingLibrary(batchSize,
                batchIntervalMillis*20, //wait 20 seconds
                batchProcessor, numberOfThreads, false);
        String requestResponse = "result";
        when(job.execute()).thenReturn(requestResponse);
        CompletableFuture<JobResult> future = (CompletableFuture<JobResult>) libraryMocked.submitJob(job);

        libraryMocked.shutdown();

        Thread.sleep(batchIntervalMillis * 2);

        assertNotNull(future);

        String futureResponse = future.get().getResponse();
        assertEquals("result", futureResponse);
        //logged lines to have visibility scheduled timer executed after 10 seconds
        // and future returned the response
        Logger.log("running test finished" + futureResponse);
        assertTrue(libraryMocked.checkIfShutDown());
    }

    @Test
    void scheduledProcessBatchExecutesAtFixedRate() throws InterruptedException {
        when(job.execute()).thenReturn("result");
        library.submitJob(job);
        Thread.sleep(batchIntervalMillis * 2);
        verify(batchProcessor, atLeastOnce()).process(anyList());
    }

    @Test
    void submitJobDoesNotProcessBatchWhenBatchSizeNotReachedAndNoFixedTimeRateReached() throws InterruptedException {
        when(job.execute()).thenReturn("result");
        for (int i = 0; i < batchSize - 1; i++) {
            library.submitJob(job);
        }
        //I assume our scheduled time of 1 second did not pick up yet
        // if test fails on your side assure you give higher scheduled timeout like 20 sec e.g.
        verify(batchProcessor, never()).process(anyList());
    }

    @Test
    void submitJobProcessesBatchImmediatelyWhenBatchSizeReached() {
        when(job.execute()).thenReturn("result");
        for (int i = 0; i < batchSize; i++) {
            library.submitJob(job);
        }
        verify(batchProcessor, times(1)).process(anyList());
    }

}