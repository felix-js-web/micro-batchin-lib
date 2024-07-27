package org.example.job.impl;

import org.example.job.JobResult;

// Implementation of JobResult
public class SampleJobResult implements JobResult {

    private final boolean success;

    public SampleJobResult(boolean success) {
        this.success = success;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

}