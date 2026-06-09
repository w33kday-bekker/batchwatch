package com.lucasb.batchwatch;

import com.lucasb.batchwatch.db.Database;

public class Main {
    public static void main(String[] args) throws Exception {
        Database.initSchema();
        System.out.println("Database initialized successfully");
    }
}