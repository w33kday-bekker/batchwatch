# BatchWatch

BatchWatch is a lightweight batch job scheduler built in Java with SQLite, 
designed to demonstrate production support skills including batch scheduling, 
failure handling, retry logic, structured logging, and operational runbook 
documentation. It was built as a portfolio project targeting roles in 
production support and backend development.

---

## Quickstart

**1. Clone the repository:**
```cmd
git clone https://github.com/lucasb/batchwatch.git
cd batchwatch
```

**2. Run the scheduler:**
```cmd
mvn exec:java
```

**3. Check the status report:**
```cmd
mvn exec:java -Dexec.args="report"
```

---

## Sample Status Report Output
========== BATCHWATCH STATUS REPORT ==========
Generated: 2026-06-09 14:38:54
Job:          Always OK Job
Last Status:  SUCCESS
Last Run:     2026-06-09 13:53:26
Success Rate: 100.0% over last 19 runs
Job:          Flaky Job
Last Status:  SUCCESS
Last Run:     2026-06-09 13:53:26
Success Rate: 68.4% over last 19 runs
Job:          Bad Data Job
Last Status:  FAILED  *** FAILING ***
Last Run:     2026-06-09 13:53:26
Success Rate: 0.0% over last 19 runs

---

## Schema

```sql
CREATE TABLE jobs (
    id               INTEGER PRIMARY KEY,
    name             TEXT NOT NULL,
    job_type         TEXT NOT NULL,
    interval_seconds INTEGER NOT NULL,
    retry_limit      INTEGER DEFAULT 2,
    enabled          INTEGER DEFAULT 1
);

CREATE TABLE job_runs (
    id         INTEGER PRIMARY KEY,
    job_id     INTEGER REFERENCES jobs(id),
    status     TEXT NOT NULL,
    started_at TEXT,
    ended_at   TEXT,
    attempt    INTEGER,
    output     TEXT
);
```

---

## Demo Jobs

| Job | Type | Behavior |
|-----|------|----------|
| Always OK Job | ALWAYS_OK | Succeeds every run |
| Flaky Job | FLAKY | Fails randomly ~30% of the time |
| Bad Data Job | BAD_DATA | Always fails due to seeded negative amount |

---

## Failure Handling

- Each failed job is retried up to its `retry_limit` (default 2 retries)
- Every attempt is recorded individually in `job_runs` with an attempt number
- When retries are exhausted an alert line is written to `alerts.log`
- The status report flags any job whose last run was FAILED

---

## Design Decisions

**SQLite over a full database:** This project runs locally with no installation 
beyond the JDK and Maven. SQLite is sufficient for a single-process scheduler 
and keeps the setup simple.

**Single shared connection:** SQLite does not handle multiple simultaneous 
connections well. A single shared connection in Database.java avoids locking issues.

**Simple interval scheduling over cron expressions:** Cron parsing adds 
significant complexity without adding value for a demo project. Fixed intervals 
in seconds are easy to reason about and easy to test.

**Switch statement over Strategy Pattern:** JobExecutor uses a switch statement 
for simplicity. A Strategy Pattern would be appropriate if the number of job 
types grew significantly, but for three fixed types it would be over-engineering.

**Out of scope:** Web UI, authentication, cron expressions, multi-threading.

---

## Project Structure
batchwatch/
├── src/
│   ├── main/java/com/lucasb/batchwatch/
│   │   ├── Main.java
│   │   ├── db/Database.java
│   │   ├── jobs/JobExecutor.java
│   │   ├── model/Job.java
│   │   ├── model/JobRun.java
│   │   └── scheduler/Scheduler.java
│   └── test/java/com/lucasb/batchwatch/
│       └── BatchWatchTest.java
├── fix_bad_amounts.sql
├── RUNBOOK.md
└── pom.xml

---

## Running Tests

```cmd
mvn test
```

Expected output: 5 tests, 0 failures.
