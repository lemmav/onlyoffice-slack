ALTER TABLE team_settings 
ADD COLUMN demo_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE team_settings 
ADD COLUMN demo_started_date TIMESTAMP;