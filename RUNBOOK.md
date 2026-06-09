# BatchWatch Operations Runbook

This runbook documents how to diagnose and recover from failures in BatchWatch.
Use this guide when a job appears in alerts.log or shows FAILING in the status report.

## How to Check Job Status

```cmd
mvn exec:java -Dexec.args="report"
```

## How to Check Alerts

```cmd
type alerts.log
```

---

## Job: Always OK Job
**Type:** ALWAYS_OK

**Symptom:**
This job should never fail. If it appears in alerts.log something is wrong
with the environment, not the job itself.

**Diagnose:**
```sql
SELECT * FROM job_runs
WHERE job_id = 1
ORDER BY started_at DESC
LIMIT 10;
```

**Possible Causes:**
- Database file is locked or corrupted
- JVM ran out of memory
- Disk is full

**Recovery:**
1. Check that no other BatchWatch process is running
2. Verify disk space is available
3. Restart the scheduler: `mvn exec:java`
4. Monitor the next 3 cycles to confirm recovery

**Root Cause:**
This job has no external dependencies. Any failure here points to
an infrastructure problem, not application logic.

---

## Job: Flaky Job
**Type:** FLAKY

**Symptom:**
Job appears in alerts.log after exhausting retries. Success rate
drops below 50% in the status report.

**Diagnose:**
```sql
SELECT attempt, status, output, started_at
FROM job_runs
WHERE job_id = 2
ORDER BY started_at DESC
LIMIT 20;
```

**Possible Causes:**
- Expected behavior — this job is designed to fail 30% of the time
- If failure rate exceeds 50% consistently, the random threshold may need tuning

**Recovery:**
1. Check the status report success rate: `mvn exec:java -Dexec.args="report"`
2. If success rate is above 50% over 20 runs, no action needed
3. If consistently below 50%, review the failure threshold in JobExecutor.java:
   `if (random.nextDouble() < 0.3)` — lower this value to reduce failure rate
4. Clear the alert and monitor: the job will self-recover on the next successful run

**Root Cause:**
Intentional random failure simulating an unreliable external dependency
such as a flaky third party API. Retry logic handles transient failures.
Persistent failure would indicate the dependency is down.

---

## Job: Bad Data Job
**Type:** BAD_DATA

**Symptom:**
Job always appears as FAILING in the status report with 0% success rate.
Error message: `BAD_DATA: negative amount found: -99.99`

**Diagnose:**
```sql
SELECT id, amount
FROM bad_data_input
WHERE amount < 0;
```

Expected output: one row with amount = -99.99

**Recovery:**
Run the data correction script following this procedure:

1. Run the diagnostic SELECT above and confirm the bad rows
2. Open `fix_bad_amounts.sql` and review the UPDATE statement
3. Connect to batchwatch.db and run the transaction:
```sql
BEGIN TRANSACTION;
    UPDATE bad_data_input
    SET amount = ABS(amount)
    WHERE amount < 0;
    SELECT id, amount FROM bad_data_input;
COMMIT;
```
4. Confirm the SELECT inside the transaction shows no negative amounts before committing
5. If anything looks wrong run ROLLBACK instead of COMMIT
6. After committing, verify with:
```sql
SELECT COUNT(*) FROM bad_data_input WHERE amount < 0;
-- Expected: 0
```
7. Restart the scheduler and confirm Bad Data Job shows SUCCESS in the next cycle

**Root Cause:**
A negative amount was seeded into the bad_data_input table simulating
corrupt source data from an upstream system. The job validates input
before processing and correctly rejects invalid records. The fix is
a data correction, not a code change.

---

## General Recovery Steps

1. Always run the status report first: `mvn exec:java -Dexec.args="report"`
2. Check alerts.log for the full error message: `type alerts.log`
3. Query job_runs for the failed job to see all attempts and outputs
4. Fix the root cause before restarting — restarting without fixing will just re-trigger the alert
5. After recovery, monitor the next 2-3 cycles to confirm stability
