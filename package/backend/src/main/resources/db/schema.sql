CREATE TABLE IF NOT EXISTS organization (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_code VARCHAR(40) NOT NULL UNIQUE,
  org_name VARCHAR(120) NOT NULL,
  org_type VARCHAR(30) NOT NULL,
  region_code VARCHAR(30) DEFAULT NULL,
  msp_id VARCHAR(40) DEFAULT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(60) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  display_name VARCHAR(80) NOT NULL,
  org_id BIGINT NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_user_org FOREIGN KEY (org_id) REFERENCES organization(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(50) NOT NULL UNIQUE,
  role_name VARCHAR(80) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS trace_batch (
  id VARCHAR(40) PRIMARY KEY,
  batch_no VARCHAR(60) NOT NULL UNIQUE,
  trace_code VARCHAR(80) DEFAULT NULL UNIQUE,
  batch_type VARCHAR(30) NOT NULL,
  owner_org_id BIGINT NOT NULL,
  custodian_org_id BIGINT NOT NULL,
  product_name VARCHAR(120) DEFAULT NULL,
  spec VARCHAR(120) DEFAULT NULL,
  quantity DECIMAL(16,3) NOT NULL DEFAULT 0,
  unit VARCHAR(20) NOT NULL DEFAULT 'kg',
  current_stage VARCHAR(40) NOT NULL,
  status VARCHAR(30) NOT NULL,
  production_date DATE DEFAULT NULL,
  expiry_date DATE DEFAULT NULL,
  last_event_id VARCHAR(40) DEFAULT NULL,
  last_hash VARCHAR(64) DEFAULT NULL,
  chain_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  tx_id VARCHAR(120) DEFAULT NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_batch_owner FOREIGN KEY (owner_org_id) REFERENCES organization(id),
  CONSTRAINT fk_batch_custodian FOREIGN KEY (custodian_org_id) REFERENCES organization(id),
  CONSTRAINT fk_batch_user FOREIGN KEY (created_by) REFERENCES sys_user(id),
  INDEX idx_batch_status (status),
  INDEX idx_batch_stage (current_stage),
  INDEX idx_batch_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS batch_party (
  batch_id VARCHAR(40) NOT NULL,
  org_id BIGINT NOT NULL,
  party_type VARCHAR(30) NOT NULL,
  visible_scope VARCHAR(40) NOT NULL DEFAULT 'FULL',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (batch_id, org_id, party_type),
  CONSTRAINT fk_party_batch FOREIGN KEY (batch_id) REFERENCES trace_batch(id) ON DELETE CASCADE,
  CONSTRAINT fk_party_org FOREIGN KEY (org_id) REFERENCES organization(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS batch_relation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_batch_id VARCHAR(40) NOT NULL,
  child_batch_id VARCHAR(40) NOT NULL,
  relation_type VARCHAR(30) NOT NULL,
  quantity DECIMAL(16,3) DEFAULT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_relation (parent_batch_id, child_batch_id),
  CONSTRAINT fk_rel_parent FOREIGN KEY (parent_batch_id) REFERENCES trace_batch(id),
  CONSTRAINT fk_rel_child FOREIGN KEY (child_batch_id) REFERENCES trace_batch(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS trace_event (
  id VARCHAR(40) PRIMARY KEY,
  batch_id VARCHAR(40) NOT NULL,
  event_type VARCHAR(40) NOT NULL,
  stage VARCHAR(40) NOT NULL,
  result_status VARCHAR(30) NOT NULL,
  issuer_org_id BIGINT NOT NULL,
  operator_user_id BIGINT NOT NULL,
  business_time DATETIME(6) NOT NULL,
  location_code VARCHAR(80) DEFAULT NULL,
  payload_json LONGTEXT NOT NULL,
  content_hash VARCHAR(64) NOT NULL,
  evidence_root VARCHAR(64) DEFAULT NULL,
  prev_event_id VARCHAR(40) DEFAULT NULL,
  prev_hash VARCHAR(64) DEFAULT NULL,
  request_id VARCHAR(80) NOT NULL UNIQUE,
  chain_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  tx_id VARCHAR(120) DEFAULT NULL,
  block_number BIGINT DEFAULT NULL,
  chain_error VARCHAR(500) DEFAULT NULL,
  correction_of VARCHAR(40) DEFAULT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_event_batch FOREIGN KEY (batch_id) REFERENCES trace_batch(id),
  CONSTRAINT fk_event_org FOREIGN KEY (issuer_org_id) REFERENCES organization(id),
  CONSTRAINT fk_event_user FOREIGN KEY (operator_user_id) REFERENCES sys_user(id),
  INDEX idx_event_batch_time (batch_id, business_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS evidence (
  id VARCHAR(40) PRIMARY KEY,
  batch_id VARCHAR(40) NOT NULL,
  event_id VARCHAR(40) DEFAULT NULL,
  uploaded_by BIGINT NOT NULL,
  object_key VARCHAR(255) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(120) DEFAULT NULL,
  file_size BIGINT NOT NULL,
  sha256 VARCHAR(64) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_evidence_batch FOREIGN KEY (batch_id) REFERENCES trace_batch(id),
  CONSTRAINT fk_evidence_event FOREIGN KEY (event_id) REFERENCES trace_event(id),
  CONSTRAINT fk_evidence_user FOREIGN KEY (uploaded_by) REFERENCES sys_user(id),
  INDEX idx_evidence_batch (batch_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS event_review (
  event_id VARCHAR(40) PRIMARY KEY,
  submitted_role_code VARCHAR(50) NOT NULL,
  review_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',
  reviewer_user_id BIGINT DEFAULT NULL,
  reviewed_at DATETIME(6) DEFAULT NULL,
  review_comment VARCHAR(500) DEFAULT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_review_event FOREIGN KEY (event_id) REFERENCES trace_event(id) ON DELETE CASCADE,
  CONSTRAINT fk_review_user FOREIGN KEY (reviewer_user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS custody_transfer (
  id VARCHAR(40) PRIMARY KEY,
  batch_id VARCHAR(40) NOT NULL,
  event_id VARCHAR(40) DEFAULT NULL,
  from_org_id BIGINT NOT NULL,
  to_org_id BIGINT NOT NULL,
  carrier_org_id BIGINT DEFAULT NULL,
  status VARCHAR(30) NOT NULL,
  outbound_at DATETIME(6) DEFAULT NULL,
  received_at DATETIME(6) DEFAULT NULL,
  discrepancy_code VARCHAR(80) DEFAULT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_transfer_batch FOREIGN KEY (batch_id) REFERENCES trace_batch(id),
  CONSTRAINT fk_transfer_from FOREIGN KEY (from_org_id) REFERENCES organization(id),
  CONSTRAINT fk_transfer_to FOREIGN KEY (to_org_id) REFERENCES organization(id),
  CONSTRAINT fk_transfer_carrier FOREIGN KEY (carrier_org_id) REFERENCES organization(id),
  INDEX idx_transfer_batch_status (batch_id, status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS inspection_record (
  id VARCHAR(40) PRIMARY KEY,
  batch_id VARCHAR(40) NOT NULL,
  event_id VARCHAR(40) NOT NULL,
  inspection_type VARCHAR(50) NOT NULL,
  sample_no VARCHAR(80) NOT NULL,
  indicators_json LONGTEXT NOT NULL,
  overall_result VARCHAR(30) NOT NULL,
  report_no VARCHAR(80) DEFAULT NULL,
  report_hash VARCHAR(64) DEFAULT NULL,
  issued_by BIGINT NOT NULL,
  issued_at DATETIME(6) NOT NULL,
  decision VARCHAR(30) NOT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_inspection_batch FOREIGN KEY (batch_id) REFERENCES trace_batch(id),
  CONSTRAINT fk_inspection_event FOREIGN KEY (event_id) REFERENCES trace_event(id),
  CONSTRAINT fk_inspection_org FOREIGN KEY (issued_by) REFERENCES organization(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS access_grant (
  id VARCHAR(40) PRIMARY KEY,
  batch_id VARCHAR(40) NOT NULL,
  grantor_org_id BIGINT NOT NULL,
  grantee_org_id BIGINT NOT NULL,
  field_scope VARCHAR(80) NOT NULL,
  valid_from DATETIME(6) NOT NULL,
  valid_until DATETIME(6) DEFAULT NULL,
  revoked TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_grant_batch FOREIGN KEY (batch_id) REFERENCES trace_batch(id),
  CONSTRAINT fk_grant_grantor FOREIGN KEY (grantor_org_id) REFERENCES organization(id),
  CONSTRAINT fk_grant_grantee FOREIGN KEY (grantee_org_id) REFERENCES organization(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT DEFAULT NULL,
  org_id BIGINT DEFAULT NULL,
  action VARCHAR(80) NOT NULL,
  object_type VARCHAR(50) NOT NULL,
  object_id VARCHAR(80) DEFAULT NULL,
  detail_json LONGTEXT DEFAULT NULL,
  ip_address VARCHAR(64) DEFAULT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS consumer_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  phone VARCHAR(20) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  nickname VARCHAR(40) DEFAULT NULL,
  gender VARCHAR(20) DEFAULT NULL,
  region_code VARCHAR(30) DEFAULT NULL,
  profile_completed TINYINT(1) NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  last_login_at DATETIME(6) DEFAULT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS consumer_trace_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  consumer_id BIGINT NOT NULL,
  batch_id VARCHAR(40) NOT NULL,
  trace_code VARCHAR(80) NOT NULL,
  viewed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_consumer_trace (consumer_id, batch_id),
  CONSTRAINT fk_consumer_history_user FOREIGN KEY (consumer_id) REFERENCES consumer_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_consumer_history_batch FOREIGN KEY (batch_id) REFERENCES trace_batch(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS consumer_favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  consumer_id BIGINT NOT NULL,
  batch_id VARCHAR(40) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_consumer_favorite (consumer_id, batch_id),
  CONSTRAINT fk_consumer_favorite_user FOREIGN KEY (consumer_id) REFERENCES consumer_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_consumer_favorite_batch FOREIGN KEY (batch_id) REFERENCES trace_batch(id)
) ENGINE=InnoDB;
