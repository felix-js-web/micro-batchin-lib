package org.example.job.impl;

import org.example.job.Job;
import org.example.utils.Logger;

public class SampleJob  implements Job {

    @Override
    public void execute() {
        int jobId = ATOMIC_JOB_COUNT.getAndIncrement();
        Logger.log("------   JOB EXECUTE before SLEEP ---- Job executed by thread for JOB ID " + Thread.currentThread().getName() + "   " + jobId);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Logger.log("------   JOB EXECUTE  ---- Job executed by thread for JOB ID  " + Thread.currentThread().getName() + "   " + jobId);

    }

}
