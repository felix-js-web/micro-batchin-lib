package org.example.processor;

import org.example.job.Job;

import java.util.List;

// Interface for BatchProcessor
public interface BatchProcessor {

    void process(List<Job> jobs);

}

