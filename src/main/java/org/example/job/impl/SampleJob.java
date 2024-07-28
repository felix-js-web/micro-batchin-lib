package org.example.job.impl;

import org.example.job.Job;
import org.example.processor.BatchProcessor;
import org.example.utils.Logger;

public class SampleJob implements Job {

    //TODO a quick hack for now - Ideally class should have an ID field
    // also it should have a status field - like NEW SUBMITTED BATCHED and etcs
    // and methods should be according to fields - for sake of time squashed all in one method and one response variable
    final String request;

    public SampleJob(String request) {
        this.request = request;
    }


    @Override
    public String execute() {
        int jobId = ATOMIC_JOB_COUNT.getAndIncrement();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long val = 0;
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            val += i;
        }
        //Hack to make sure we can reconcile those although the above should be enough
        int batchProcessorJobNumber = BatchProcessor.ATOMIC_JOB_COUNT_EXECUTED_BY_BATCH_PROCESSOR.getAndIncrement();

        //Hack to make sure we can reconcile those although the above should be enough
        int batchNumber = BatchProcessor.ATOMIC_BATCHES_COUNT_EXECUTED_BY_BATCH_PROCESSOR.get();
        String response = String.format("------   SAMPLE JOB EXECUTE  ---- Job executed by thread %s  for JOB ID %d  BATCH PROCESSOR JOB ID IS %d BATCH ID IS %d and request String was %s", Thread.currentThread().getName(), jobId, batchProcessorJobNumber, batchNumber, request);

        Logger.log(response);
        return response;
    }

}
