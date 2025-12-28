USE healthinsurancemanagement;   -- replace with your DB name if different
SHOW TABLES;
SHOW COLUMNS FROM policy;
SHOW COLUMNS FROM claim;
SHOW COLUMNS FROM users;
SHOW COLUMNS FROM support_ticket;
ALTER TABLE policy
  ADD COLUMN userId INT NULL,
  ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE policy
  ADD INDEX idx_policy_userId (userId);
ALTER TABLE policy
  ADD CONSTRAINT fk_policy_user FOREIGN KEY (userId) REFERENCES users(user_id) ON DELETE SET NULL;
  SHOW COLUMNS FROM policy LIKE 'policy_status';
  ALTER TABLE policy
  MODIFY COLUMN policy_status ENUM('ACTIVE','INACTIVE','RENEWED','EXPIRED') NOT NULL DEFAULT 'ACTIVE';
  ALTER TABLE claim ADD INDEX idx_claim_policyId (policy_id);
ALTER TABLE payment ADD INDEX idx_payment_policyId (policy_id);
-- policy_renewal
CREATE TABLE IF NOT EXISTS policy_renewal (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  policy_id INT NOT NULL,
  renewal_due_date DATE NOT NULL,
  renewed_at DATETIME NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_policyrenewal_policy_id (policy_id),
  CONSTRAINT fk_policyrenewal_policy FOREIGN KEY (policy_id) REFERENCES policy(policy_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- claim_action_history
CREATE TABLE IF NOT EXISTS claim_action_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  claim_id INT NOT NULL,
  acted_by_user_id INT NULL,
  action VARCHAR(32) NOT NULL,
  comments TEXT NULL,
  action_timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_claimaction_claim_id (claim_id),
  INDEX idx_claimaction_user_id (acted_by_user_id),
  CONSTRAINT fk_claimaction_claim FOREIGN KEY (claim_id) REFERENCES claim(claim_id) ON DELETE CASCADE,
  CONSTRAINT fk_claimaction_user FOREIGN KEY (acted_by_user_id) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- support_response
CREATE TABLE IF NOT EXISTS support_response (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  support_ticket_id INT NOT NULL,
  responder_user_id INT NULL,
  message TEXT NOT NULL,
  attachments JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_supportresponse_ticket_id (support_ticket_id),
  INDEX idx_supportresponse_responder_id (responder_user_id),
  CONSTRAINT fk_supportresponse_ticket FOREIGN KEY (support_ticket_id) REFERENCES support_ticket(ticket_id) ON DELETE CASCADE,
  CONSTRAINT fk_supportresponse_user FOREIGN KEY (responder_user_id) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SHOW TABLES LIKE 'policy_renewal';
SHOW CREATE TABLE policy_renewal;
SHOW CREATE TABLE claim_action_history;
SHOW CREATE TABLE support_response;

SELECT COUNT(*) FROM policy_renewal;
SELECT COUNT(*) FROM claim_action_history;
SELECT COUNT(*) FROM support_response;

SELECT * FROM policy_renewal WHERE renewal_due_date < CURDATE() AND status = 'PENDING';

ALTER TABLE policy
DROP FOREIGN KEY fk_policy_user;

ALTER TABLE policy
DROP INDEX idx_policy_userId;

ALTER TABLE policy
DROP COLUMN userId;

SHOW CREATE TABLE policy;

ALTER TABLE policy
DROP FOREIGN KEY FKr73s68d08m2d97l3b98rkxjtt;

ALTER TABLE policy
ADD CONSTRAINT fk_policy_user
FOREIGN KEY (user_id) REFERENCES users(user_id);


ALTER TABLE policy
RENAME COLUMN userId TO user_id;
ALTER TABLE policy
DROP COLUMN user_id;

