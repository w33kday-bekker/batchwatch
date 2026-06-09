package com.lucasb.batchwatch.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Database {

    public static Connection connect() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:batchwatch.db");
    }

    public static void initSchema() throws Exception {

        try (Connection conn = connect();
            Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS jobs (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "job_type TEXT NOT NULL," +
                "interval_seconds INTEGER NOT NULL," +
                "retry_limit INTEGER DEFAULT 2," +
                "enabled INTEGER DEFAULT 1)");

            stmt.execute("CREATE TABLE IF NOT EXISTS job_runs (" +
                "id INTEGER PRIMARY KEY," +
                "job_id INTEGER REFERENCES jobs(id)," +
                "status TEXT NOT NULL," +
                "started_at TEXT," +
                "ended_at TEXT," +
                "attempt INTEGER," +
                "output TEXT)");

     }
    }
    
}