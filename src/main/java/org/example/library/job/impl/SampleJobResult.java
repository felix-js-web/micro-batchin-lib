package org.example.library.job.impl;

import org.example.library.job.JobResult;

// Implementation of JobResult
public class SampleJobResult implements JobResult {

    //TODO a quick hack for now - Ideally class should have an ID field , Submitted Job Id feild , Status
    // also it should have a status field - like NEW EXECUTED and etcs
    // and methods should be according to fields - for sake of time squashed all in one method and one response variable
    // I know it could be basic lambda and we would not have needed the class
    // - just thought of base where the future fields to come out
    private final String response;

    public SampleJobResult(String response) {
        this.response = response;
    }

    @Override
    public String getResponse() {
        return response;
    }

}