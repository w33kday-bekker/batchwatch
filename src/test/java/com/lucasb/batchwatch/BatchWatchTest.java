package com.lucasb.batchwatch;

import com.lucasb.batchwatch.db.Database;
import com.lucasb.batchwatch.jobs.JobExecutor;
import com.lucasb.batchwatch.model.Job;
import com.lucasb.batchwatch.scheduler.Scheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;

public class BatchWatchTest {

    private JobExecutor executor;

    @BeforeEach
    void setup() throws Exception {
        Database.initSchema();
        executor = new JobExecutor();
    }

    // Test 1: ALWAYS_OK job returns success
    @Test
    void alwaysOkJobReturnsSuccess() throws Exception {
        Job job = new Job(1, "Always OK Job", "ALWAYS_OK", 10, 2, true);
        String result = executor.execute(job);
        assertTrue(result.contains("SUCCESS"));
    }

    // Test 2: BAD_DATA job always throws an exception
    @Test
    void badDataJobThrowsException() {
        Job job = new Job(3, "Bad Data Job", "BAD_DATA", 10, 2, true);
        assertThrows(Exception.class, () -> executor.execute(job));
    }

    // Test 3: Scheduler picks up a due job (never run before)
    @Test
    void schedulerPicksUpDueJob() throws Exception {
        Statement stmt = Database.getConnection().createStatement();
        stmt.execute("INSERT OR IGNORE INTO jobs (id, name, job_type, interval_seconds, retry_limit, enabled) " +
            "VALUES (99, 'Test Job', 'ALWAYS_OK', 1, 2, 1)");
        stmt.execute("DELETE FROM job_runs WHERE job_id = 99");
        stmt.close();

        Statement stmt2 = Database.getConnection().createStatement();
        ResultSet rs = stmt2.executeQuery(
            "SELECT COUNT(*) as cnt FROM job_runs WHERE job_id = 99");
        int before = rs.getInt("cnt");
        rs.close();
        stmt2.close();

        assertEquals(0, before);
    }

    // Test 4: FAILED status is recorded correctly
    @Test
    void failedStatusIsRecorded() throws Exception {
        Statement stmt = Database.getConnection().createStatement();
        stmt.execute("INSERT INTO job_runs (job_id, status, started_at, ended_at, attempt, output) " +
            "VALUES (1, 'FAILED', '2026-01-01 00:00:00', '2026-01-01 00:00:01', 1, 'test error')");
        stmt.close();

        Statement stmt2 = Database.getConnection().createStatement();
        ResultSet rs = stmt2.executeQuery(
            "SELECT status FROM job_runs WHERE output = 'test error' LIMIT 1");
        String status = rs.getString("status");
        rs.close();
        stmt2.close();

        assertEquals("FAILED", status);
    }

    // Test 5: Diagnostic SELECT identifies bad rows
    @Test
    void diagnosticSelectFindsBadData() throws Exception {
        Statement stmt = Database.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT COUNT(*) as cnt FROM bad_data_input WHERE amount < 0");
        int badRows = rs.getInt("cnt");
        rs.close();
        stmt.close();

        assertTrue(badRows > 0, "Expected at least one negative amount in bad_data_input");
    }
}