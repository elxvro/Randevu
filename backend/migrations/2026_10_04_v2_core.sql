-- Randevu v1.3.1 -> v2.0.0 core migration.
-- Run once after taking a database backup.

ALTER TABLE businesses
    ADD COLUMN slug VARCHAR(96) NULL AFTER name,
    ADD COLUMN opening_time TIME NOT NULL DEFAULT '09:00:00' AFTER timezone,
    ADD COLUMN closing_time TIME NOT NULL DEFAULT '18:00:00' AFTER opening_time,
    ADD COLUMN active TINYINT(1) NOT NULL DEFAULT 1 AFTER closing_time,
    ADD COLUMN version INT UNSIGNED NOT NULL DEFAULT 1 AFTER active;

UPDATE businesses
SET slug = CONCAT('isletme-', id)
WHERE slug IS NULL OR slug = '';

ALTER TABLE businesses
    MODIFY slug VARCHAR(96) NOT NULL,
    ADD UNIQUE KEY uq_businesses_slug (slug);

ALTER TABLE users
    MODIFY role ENUM('customer','business','owner','staff') NOT NULL DEFAULT 'customer',
    ADD COLUMN email VARCHAR(190) NULL AFTER name,
    MODIFY phone VARCHAR(32) NULL,
    DROP INDEX uq_users_phone,
    ADD UNIQUE KEY uq_users_email (email),
    ADD KEY idx_users_phone (phone);

ALTER TABLE services
    ADD COLUMN external_id CHAR(36) NULL AFTER business_id,
    ADD COLUMN version INT UNSIGNED NOT NULL DEFAULT 1 AFTER active;
UPDATE services SET external_id = UUID() WHERE external_id IS NULL OR external_id = '';
ALTER TABLE services
    MODIFY external_id CHAR(36) NOT NULL,
    ADD UNIQUE KEY uq_services_external_id (external_id),
    ADD KEY idx_services_business (business_id);

ALTER TABLE staff
    ADD COLUMN external_id CHAR(36) NULL AFTER business_id,
    ADD COLUMN user_id BIGINT UNSIGNED NULL AFTER external_id,
    ADD COLUMN title VARCHAR(120) NULL AFTER name,
    ADD COLUMN public_booking TINYINT(1) NOT NULL DEFAULT 1 AFTER active,
    ADD COLUMN version INT UNSIGNED NOT NULL DEFAULT 1 AFTER public_booking;
UPDATE staff SET external_id = UUID() WHERE external_id IS NULL OR external_id = '';
ALTER TABLE staff
    MODIFY external_id CHAR(36) NOT NULL,
    ADD UNIQUE KEY uq_staff_external_id (external_id),
    ADD KEY idx_staff_business (business_id),
    ADD CONSTRAINT fk_staff_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS staff_leaves (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    business_id BIGINT UNSIGNED NOT NULL,
    external_id CHAR(36) NOT NULL,
    staff_id BIGINT UNSIGNED NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    start_time TIME NULL,
    end_time TIME NULL,
    reason VARCHAR(255) NULL,
    version INT UNSIGNED NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_staff_leaves_external_id (external_id),
    KEY idx_staff_leaves_business (business_id),
    KEY idx_staff_leaves_staff_dates (staff_id, start_date, end_date),
    CONSTRAINT fk_staff_leaves_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_staff_leaves_staff FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE appointments
    ADD COLUMN external_id CHAR(36) NULL AFTER business_id,
    ADD COLUMN source ENUM('android','public_web','system') NOT NULL DEFAULT 'android' AFTER status,
    ADD COLUMN version INT UNSIGNED NOT NULL DEFAULT 1 AFTER source;
UPDATE appointments SET external_id = UUID() WHERE external_id IS NULL OR external_id = '';
ALTER TABLE appointments
    MODIFY external_id CHAR(36) NOT NULL,
    ADD UNIQUE KEY uq_appointments_external_id (external_id);

CREATE TABLE IF NOT EXISTS rate_limit_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(64) NOT NULL,
    key_hash CHAR(64) NOT NULL,
    occurred_at DATETIME NOT NULL,
    KEY idx_rate_limit_lookup (action, key_hash, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


ALTER TABLE whatsapp_reminder_queue
    DROP INDEX uq_whatsapp_reminder,
    ADD UNIQUE KEY uq_whatsapp_reminder (business_id, appointment_external_id, offset_minutes);
