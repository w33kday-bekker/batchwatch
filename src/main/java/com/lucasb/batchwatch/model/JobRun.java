package com.lucasb.batchwatch.model;

public class JobRun {
    private int id;
    private int jobId;
    private String status;
    private String startedAt;
    private String endedAt;
    private int attempt;
    private String output;

    public JobRun(int jobId, String status, String startedAt, String endedAt, int attempt, String output) {
        this.jobId = jobId;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.attempt = attempt;
        this.output = output;
    }

    public int getId() { return id; }
    public int getJobId() { return jobId; }
    public String getStatus() { return status; }
    public String getStartedAt() { return startedAt; }
    public String getEndedAt() { return endedAt; }
    public int getAttempt() { return attempt; }
    public String getOutput() { return output; }
}