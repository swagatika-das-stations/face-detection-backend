-- Run this script once on 3C_faceDetection_App_DB
-- It preserves existing data while moving employee_data to employee_registration_data

-- 1) Rename employee table if old table exists and new table does not exist
SET @rename_stmt = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'employee_data'
    )
    AND NOT EXISTS (
      SELECT 1
      FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'employee_registration_data'
    ),
    'RENAME TABLE employee_data TO employee_registration_data',
    'SELECT ''employee table rename skipped'''
  )
);
PREPARE stmt FROM @rename_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) Add face_registry history columns if missing
SET @add_employee_entity_id = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'employee_registration_data' AND column_name = 'entity_id'
    ),
    'SELECT ''employee_registration_data.entity_id already exists''',
    'ALTER TABLE employee_registration_data ADD COLUMN entity_id VARCHAR(255) NULL'
  )
);
PREPARE stmt FROM @add_employee_entity_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2b) Add face_registry history columns if missing
SET @add_registration_status = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'face_registry' AND column_name = 'registration_status'
    ),
    'SELECT ''registration_status already exists''',
    'ALTER TABLE face_registry ADD COLUMN registration_status VARCHAR(20) NOT NULL DEFAULT ''FAILED'''
  )
);
PREPARE stmt FROM @add_registration_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_failure_reason = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'face_registry' AND column_name = 'failure_reason'
    ),
    'SELECT ''failure_reason already exists''',
    'ALTER TABLE face_registry ADD COLUMN failure_reason VARCHAR(500) NULL'
  )
);
PREPARE stmt FROM @add_failure_reason;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_attempted_at = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'face_registry' AND column_name = 'attempted_at'
    ),
    'SELECT ''attempted_at already exists''',
    'ALTER TABLE face_registry ADD COLUMN attempted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP'
  )
);
PREPARE stmt FROM @add_attempted_at;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_employee_register_id = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'face_registry' AND column_name = 'employee_register_id'
    ),
    'SELECT ''employee_register_id already exists''',
    'ALTER TABLE face_registry ADD COLUMN employee_register_id BIGINT NULL'
  )
);
PREPARE stmt FROM @add_employee_register_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) Add index for history lookups
SET @add_history_index = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'face_registry' AND index_name = 'idx_face_registry_email_attempted_at'
    ),
    'SELECT ''idx_face_registry_email_attempted_at already exists''',
    'CREATE INDEX idx_face_registry_email_attempted_at ON face_registry (email, attempted_at)'
  )
);
PREPARE stmt FROM @add_history_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) Add optional FK from face_registry to employee_registration_data if missing
SET @add_fk_registry_employee = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.table_constraints
      WHERE table_schema = DATABASE() AND table_name = 'face_registry' AND constraint_type = 'FOREIGN KEY' AND constraint_name = 'fk_face_registry_employee_register'
    ),
    'SELECT ''fk_face_registry_employee_register already exists''',
    'ALTER TABLE face_registry ADD CONSTRAINT fk_face_registry_employee_register FOREIGN KEY (employee_register_id) REFERENCES employee_registration_data(id)'
  )
);
PREPARE stmt FROM @add_fk_registry_employee;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5) Rename face_images.employee_id (registration row FK) to employee_registration_id
--    and add a new face_images.employee_id column for the business employee number.
SET @rename_face_images_fk = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'face_images' AND column_name = 'employee_id'
    )
    AND NOT EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'face_images' AND column_name = 'employee_registration_id'
    ),
    'ALTER TABLE face_images CHANGE COLUMN employee_id employee_registration_id BIGINT NULL',
    'SELECT ''face_images registration FK rename skipped'''
  )
);
PREPARE stmt FROM @rename_face_images_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_face_images_business_employee_id = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'face_images' AND column_name = 'employee_registration_id'
    )
    AND NOT EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'face_images' AND column_name = 'employee_id'
    ),
    'ALTER TABLE face_images ADD COLUMN employee_id VARCHAR(255) NULL AFTER employee_registration_id',
    'SELECT ''face_images.employee_id already exists or registration FK missing'''
  )
);
PREPARE stmt FROM @add_face_images_business_employee_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE face_images fi
JOIN employee_registration_data erd ON erd.id = fi.employee_registration_id
SET fi.employee_id = erd.employee_id
WHERE fi.employee_id IS NULL OR fi.employee_id = '';
