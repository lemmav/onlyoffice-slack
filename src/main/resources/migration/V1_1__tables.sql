CREATE TABLE team_settings (
    team_id VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    header VARCHAR(255),
    secret VARCHAR(1000),
    PRIMARY KEY (team_id)
);

CREATE TABLE bot_users (
    team_id VARCHAR(255) NOT NULL,
    bot_id VARCHAR(255) NOT NULL,
    bot_user_id VARCHAR(255),
    bot_access_token VARCHAR(1000),
    bot_refresh_token VARCHAR(1000),
    bot_token_expires_at BIGINT,
    bot_scope VARCHAR(255),
    PRIMARY KEY (team_id, bot_id),
    CONSTRAINT uk_bot_users_team_id UNIQUE (team_id)
);

CREATE INDEX idx_team_bot ON bot_users(team_id, bot_id);
CREATE INDEX idx_team_bot_user ON bot_users(team_id, bot_user_id);

ALTER TABLE team_settings 
ADD CONSTRAINT fk_team_settings_bot_users 
    FOREIGN KEY (team_id) 
    REFERENCES bot_users(team_id) 
    ON DELETE CASCADE;

CREATE TABLE installer_users (
    team_id VARCHAR(255) NOT NULL,
    installer_user_id VARCHAR(255) NOT NULL,
    app_id VARCHAR(255) NOT NULL,
    token_type VARCHAR(255) NOT NULL,
    installer_user_scope VARCHAR(255),
    installer_user_access_token VARCHAR(1000),
    installer_user_refresh_token VARCHAR(1000),
    installer_user_token_expires_at BIGINT,
    installed_at BIGINT NOT NULL,
    bot_id VARCHAR(255),
    PRIMARY KEY (team_id, installer_user_id),
    CONSTRAINT fk_installer_users_bot_users 
        FOREIGN KEY (team_id, bot_id) 
        REFERENCES bot_users(team_id, bot_id)
);

CREATE INDEX idx_team_user ON installer_users(team_id, installer_user_id);

CREATE TABLE active_file_sessions (
    file_id VARCHAR(255) NOT NULL,
    key VARCHAR(255) NOT NULL,
    channel_id VARCHAR(255) NOT NULL,
    message_ts VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (file_id)
);