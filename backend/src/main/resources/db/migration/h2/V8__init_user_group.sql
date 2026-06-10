CREATE TABLE IF NOT EXISTS user_group (
    group_id      INT PRIMARY KEY,
    group_name    VARCHAR(64) NOT NULL,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP
);

INSERT INTO user_group (group_id, group_name, created_at, updated_at)
SELECT DISTINCT u.group_id, CONCAT('Group ', u.group_id), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM "user" u
WHERE u.group_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM user_group g WHERE g.group_id = u.group_id
);
