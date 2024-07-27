package org.example.job;

import java.util.concurrent.atomic.AtomicInteger;

// Interface for Job
public interface Job {

    AtomicInteger ATOMIC_JOB_COUNT = new AtomicInteger();

    void execute();

}