package com.lucasb.batchwatch.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Database {

    private static Connection connection;

    public static Connection getConnection() throws Exception {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:batchwatch.db");
        }
        return connection;
    }

    public static void initSchema() throws Exception {
        try (Statement stmt = getConnection().createStatement()) {
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

            stmt.execute("CREATE TABLE IF NOT EXISTS bad_data_input (" +
                "id INTEGER PRIMARY KEY," +
                "amount REAL NOT NULL)");

            stmt.execute("INSERT INTO bad_data_input (amount) " +
                "SELECT -99.99 WHERE NOT EXISTS (SELECT 1 FROM bad_data_input)");
        }
    }
}