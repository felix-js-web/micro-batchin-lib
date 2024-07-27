package org.example.job.impl;

import org.example.job.Job;
import org.example.processor.BatchProcessor;
import org.example.utils.Logger;

public class SampleJob implements Job {

    final String request;

    public SampleJob(String request) {
        this.request = request;
    }


    @Override
    public void execute() {
        int jobId = ATOMIC_JOB_COUNT.getAndIncrement();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Logger.log(String.format("------   SAMPLE JOB EXECUTE  ---- Job executed by thread %s  for JOB ID %d  ==== %s ", Thread.currentThread().getName(), jobId, request));


        //Hack to make sure we can reconcile those allthough the above should be enough
        BatchProcessor.ATOMIC_JOB_COUNT_EXECUTED_BY_BATCH_PROCESSOR.getAndIncrement();
    }

}
