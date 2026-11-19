ALTER TABLE tasks ADD COLUMN owner_id BIGINT;

UPDATE tasks
SET owner_id = (
    SELECT id
    FROM user_accounts
    WHERE user_accounts.username = tasks.owner
);

ALTER TABLE tasks ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_owner FOREIGN KEY (owner_id) REFERENCES user_accounts (id);
ALTER TABLE tasks DROP COLUMN owner;
