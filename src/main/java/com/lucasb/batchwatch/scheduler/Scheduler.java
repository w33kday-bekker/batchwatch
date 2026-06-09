package com.lucasb.batchwatch.scheduler;

import com.lucasb.batchwatch.db.Database;
import com.lucasb.batchwatch.jobs.JobExecutor;
import com.lucasb.batchwatch.model.Job;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Scheduler {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JobExecutor executor = new JobExecutor();

    public void start() throws Exception {
        System.out.println("Scheduler started...");

        while (true) {
            runDueJobs();
            Thread.sleep(5000); // wait 5 seconds before checking again
        }
    }

    private void runDueJobs() throws Exception {
        Connection conn = Database.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM jobs WHERE enabled = 1");

        while (rs.next()) {
            Job job = new Job(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("job_type"),
                rs.getInt("interval_seconds"),
                rs.getInt("retry_limit"),
                rs.getInt("enabled") == 1
            );

            if (isDue(job)) {
                runJob(job);
            }
        }
        rs.close();
        stmt.close();
    }

    private boolean isDue(Job job) throws Exception {
        Statement stmt = Database.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT MAX(started_at) as last_run FROM job_runs WHERE job_id = " + job.getId());

        if (rs.next() && rs.getString("last_run") != null) {
            LocalDateTime lastRun = LocalDateTime.parse(rs.getString("last_run"), FMT);
            LocalDateTime nextRun = lastRun.plusSeconds(job.getIntervalSeconds());
            rs.close();
            stmt.close();
            return LocalDateTime.now().isAfter(nextRun);
        }
        rs.close();
        stmt.close();
        return true;
    }
    private void runJob(Job job) throws Exception {
        String startedAt = LocalDateTime.now().format(FMT);
        String status = "FAILED";
        String output = "";

        for (int attempt = 1; attempt <= job.getRetryLimit() + 1; attempt++) {
            startedAt = LocalDateTime.now().format(FMT);
            try {
                output = executor.execute(job);
                status = "SUCCESS";
                String endedAt = LocalDateTime.now().format(FMT);
                insertJobRun(job.getId(), status, startedAt, endedAt, attempt, output);
                System.out.println("[" + endedAt + "] " + job.getName() + 
                    " (attempt " + attempt + ") -> " + status + ": " + output);
                return; // success, stop retrying
            } catch (Exception e) {
                output = e.getMessage();
                String endedAt = LocalDateTime.now().format(FMT);
                insertJobRun(job.getId(), status, startedAt, endedAt, attempt, output);
                System.out.println("[" + endedAt + "] " + job.getName() + 
                    " (attempt " + attempt + ") -> FAILED: " + output);
            }
        }

        // all retries exhausted
        writeAlert(job, output);
    }

    private void writeAlert(Job job, String output) throws Exception {
    String timestamp = LocalDateTime.now().format(FMT);
    String alertLine = "[ALERT] " + timestamp + " | Job: " + job.getName() + 
        " | Attempts: " + (job.getRetryLimit() + 1) + " | Error: " + output + "\n";

    java.nio.file.Files.write(
        java.nio.file.Paths.get("alerts.log"),
        alertLine.getBytes(),
        java.nio.file.StandardOpenOption.CREATE,
        java.nio.file.StandardOpenOption.APPEND
    );

    System.out.println("*** ALERT WRITTEN FOR: " + job.getName() + " ***");
    }

    private void insertJobRun(int jobId, String status, String startedAt,
                            String endedAt, int attempt, String output) throws Exception {
        Statement stmt = Database.getConnection().createStatement();
        stmt.execute("INSERT INTO job_runs (job_id, status, started_at, ended_at, attempt, output) VALUES (" +
            jobId + ", '" + status + "', '" + startedAt + "', '" + endedAt + "', " + attempt + ", '" + output + "')");
        stmt.close();
    }

    public void printReport() throws Exception {
        System.out.println("\n========== BATCHWATCH STATUS REPORT ==========");
        System.out.println("Generated: " + LocalDateTime.now().format(FMT));
        System.out.println("===============================================\n");

        Statement stmt = Database.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM jobs");

        while (rs.next()) {
            int jobId = rs.getInt("id");
            String jobName = rs.getString("name");

            // get last run
            Statement stmt2 = Database.getConnection().createStatement();
            ResultSet rs2 = stmt2.executeQuery(
                "SELECT status, started_at FROM job_runs WHERE job_id = " + jobId +
                " ORDER BY started_at DESC LIMIT 1");

            String lastStatus = "NEVER RUN";
            String lastRun = "N/A";
            if (rs2.next()) {
                lastStatus = rs2.getString("status");
                lastRun = rs2.getString("started_at");
            }
            rs2.close();
            stmt2.close();

            // get success rate over last 20 runs
            Statement stmt3 = Database.getConnection().createStatement();
            ResultSet rs3 = stmt3.executeQuery(
                "SELECT COUNT(*) as total, SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as successes " +
                "FROM (SELECT status FROM job_runs WHERE job_id = " + jobId +
                " ORDER BY started_at DESC LIMIT 20)");

            int total = 0;
            int successes = 0;
            if (rs3.next()) {
                total = rs3.getInt("total");
                successes = rs3.getInt("successes");
            }
            rs3.close();
            stmt3.close();

            double successRate = total > 0 ? (successes * 100.0 / total) : 0;
            boolean failing = lastStatus.equals("FAILED");

            System.out.println("Job:          " + jobName);
            System.out.println("Last Status:  " + lastStatus + (failing ? "  *** FAILING ***" : ""));
            System.out.println("Last Run:     " + lastRun);
            System.out.println("Success Rate: " + String.format("%.1f", successRate) + "% over last " + total + " runs");
            System.out.println("-----------------------------------------------\n");
        }
        rs.close();
        stmt.close();
    }
}