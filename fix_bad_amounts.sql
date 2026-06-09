-- STEP 1: DIAGNOSE
-- Run this first to see exactly what will be fixed
-- before making any changes
SELECT id, amount
FROM bad_data_input
WHERE amount < 0;

-- STEP 2: VERIFY COUNT
-- Confirm how many rows will be affected
SELECT COUNT(*) as rows_to_fix
FROM bad_data_input
WHERE amount < 0;

-- STEP 3: CORRECT
-- Wrapped in a transaction so it can be rolled back
-- if something looks wrong
BEGIN TRANSACTION;

    UPDATE bad_data_input
    SET amount = ABS(amount)
    WHERE amount < 0;

    -- Verify the fix looks correct before committing
    -- If output looks wrong, run ROLLBACK instead
    SELECT id, amount
    FROM bad_data_input;

COMMIT;

-- STEP 4: CONFIRM
-- Run after commit to confirm no negative amounts remain
SELECT COUNT(*) as remaining_bad_rows
FROM bad_data_input
WHERE amount < 0;
-- Expected result: 0