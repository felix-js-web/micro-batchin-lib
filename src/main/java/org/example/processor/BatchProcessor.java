package org.example.processor;

import org.example.job.Job;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// Interface for BatchProcessor
public interface BatchProcessor {
    AtomicInteger ATOMIC_JOB_COUNT_EXECUTED_BY_BATCH_PROCESSOR = new AtomicInteger();

    void process(List<Job> jobs);

}

//NEXT FEATURE
// Please make sure that batch processor able to have counts of what was submitted with bodies
// and also Batch Processor is able give us status of executed jobs! thats critical for reconcile
// for now for reconciling processes used the Atomic Variable it should have the cound at the end
