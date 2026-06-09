package com.lucasb.batchwatch.jobs;

import com.lucasb.batchwatch.model.Job;
import com.lucasb.batchwatch.db.Database;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Random;

public class JobExecutor {

    private static final Random random = new Random();

    public String execute(Job job) throws Exception {
        switch (job.getJobType()) {
            case "ALWAYS_OK":
                return executeAlwaysOk();
            case "FLAKY":
                return executeFlaky();
            case "BAD_DATA":
                return executeBadData();
            default:
                throw new Exception("Unknown job type: " + job.getJobType());
        }
    }

    private String executeAlwaysOk() {
        return "SUCCESS: job completed normally";
    }

    private String executeFlaky() throws Exception {
        if (random.nextDouble() < 0.3) {
            throw new Exception("FLAKY job failed randomly");
        }
        return "SUCCESS: flaky job completed this time";
    }

    private String executeBadData() throws Exception {
        Statement stmt = Database.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT amount FROM bad_data_input WHERE amount < 0 LIMIT 1");
        if (rs.next()) {
            double amount = rs.getDouble("amount");
            rs.close();
            stmt.close();
            throw new Exception("BAD_DATA: negative amount found: " + amount);
        }
        rs.close();
        stmt.close();
        return "SUCCESS: no bad data found";
    }
}
