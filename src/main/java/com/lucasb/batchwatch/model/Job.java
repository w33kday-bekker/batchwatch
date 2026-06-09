package com.lucasb.batchwatch.model;

public class Job {
    private int id;
    private String name;
    private String jobType;
    private int intervalSeconds;
    private int retryLimit;
    private boolean enabled;

    public Job(int id, String name, String jobType, int intervalSeconds, int retryLimit, boolean enabled) {
        this.id = id;
        this.name = name;
        this.jobType = jobType;
        this.intervalSeconds = intervalSeconds;
        this.retryLimit = retryLimit;
        this.enabled = enabled;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getJobType() { return jobType; }
    public int getIntervalSeconds() { return intervalSeconds; }
    public int getRetryLimit() { return retryLimit; }
    public boolean isEnabled() { return enabled; }
}