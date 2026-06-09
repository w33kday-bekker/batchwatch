package com.lucasb.batchwatch;

import com.lucasb.batchwatch.db.Database;
import com.lucasb.batchwatch.scheduler.Scheduler;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) throws Exception {
        Database.initSchema();
        seedJobs();

        if (args.length > 0 && args[0].equals("report")) {
            new Scheduler().printReport();
        } else {
            new Scheduler().start();
        }
    }

    private static void seedJobs() throws Exception {
        Statement stmt = Database.getConnection().createStatement();
        stmt.execute("INSERT INTO jobs (name, job_type, interval_seconds, retry_limit, enabled) " +
            "SELECT 'Always OK Job', 'ALWAYS_OK', 10, 2, 1 " +
            "WHERE NOT EXISTS (SELECT 1 FROM jobs WHERE name = 'Always OK Job')");
        stmt.execute("INSERT INTO jobs (name, job_type, interval_seconds, retry_limit, enabled) " +
            "SELECT 'Flaky Job', 'FLAKY', 10, 2, 1 " +
            "WHERE NOT EXISTS (SELECT 1 FROM jobs WHERE name = 'Flaky Job')");
        stmt.execute("INSERT INTO jobs (name, job_type, interval_seconds, retry_limit, enabled) " +
            "SELECT 'Bad Data Job', 'BAD_DATA', 10, 2, 1 " +
            "WHERE NOT EXISTS (SELECT 1 FROM jobs WHERE name = 'Bad Data Job')");
        stmt.close();
    }
}