ALTER TABLE teams ADD COLUMN join_code VARCHAR(6);

UPDATE teams
SET join_code = UPPER(LEFT(md5(random()::text), 6))
WHERE join_code IS NULL;

ALTER TABLE teams ALTER COLUMN join_code SET NOT NULL;
ALTER TABLE teams ADD CONSTRAINT uq_teams_join_code UNIQUE (join_code);