CREATE TABLE IF NOT EXISTS scans
(
    id            BIGSERIAL PRIMARY KEY,
    domain        VARCHAR(255) NOT NULL,
    tool          VARCHAR(50)  NOT NULL,
    start_time    TIMESTAMP    NOT NULL,
    end_time      TIMESTAMP,
    status        VARCHAR(50)  NOT NULL,
    results       JSONB,
    display_order INT       DEFAULT 0,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_scans_domain ON scans (domain);
CREATE INDEX idx_scans_status ON scans (status);
CREATE INDEX idx_scans_display_order ON scans (display_order);
