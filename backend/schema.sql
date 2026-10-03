CREATE TABLE IF NOT EXISTS businesses (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    phone VARCHAR(32) NULL,
    address VARCHAR(255) NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'Europe/Istanbul',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    business_id BIGINT UNSIGNED NULL,
    role ENUM('customer','business','staff') NOT NULL DEFAULT 'customer',
    name VARCHAR(160) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    password_hash VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_users_phone (phone),
    CONSTRAINT fk_users_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS services (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    business_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(160) NOT NULL,
    duration_minutes SMALLINT UNSIGNED NOT NULL DEFAULT 30,
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_services_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS staff (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    business_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(160) NOT NULL,
    phone VARCHAR(32) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    business_id BIGINT UNSIGNED NOT NULL,
    customer_id BIGINT UNSIGNED NULL,
    service_id BIGINT UNSIGNED NOT NULL,
    staff_id BIGINT UNSIGNED NOT NULL,
    customer_name VARCHAR(160) NOT NULL,
    customer_phone VARCHAR(32) NOT NULL,
    starts_at DATETIME NOT NULL,
    ends_at DATETIME NOT NULL,
    status ENUM('pending','confirmed','completed','cancelled') NOT NULL DEFAULT 'pending',
    note TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_appointments_business_start (business_id, starts_at),
    KEY idx_appointments_staff_start (staff_id, starts_at),
    CONSTRAINT fk_appointments_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_appointments_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_appointments_service FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_staff FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS api_tokens (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_api_tokens_hash (token_hash),
    KEY idx_api_tokens_user (user_id),
    CONSTRAINT fk_api_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS business_settings (
    business_id BIGINT UNSIGNED PRIMARY KEY,
    whatsapp_enabled TINYINT(1) NOT NULL DEFAULT 0,
    reminder_24h TINYINT(1) NOT NULL DEFAULT 1,
    reminder_2h TINYINT(1) NOT NULL DEFAULT 1,
    template_name VARCHAR(128) NOT NULL DEFAULT 'appointment_reminder',
    template_language VARCHAR(16) NOT NULL DEFAULT 'tr',
    meta_phone_number_id VARCHAR(64) NULL,
    last_connection_test_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_business_settings_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS whatsapp_reminder_queue (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    business_id BIGINT UNSIGNED NOT NULL,
    appointment_external_id VARCHAR(128) NOT NULL,
    offset_minutes SMALLINT UNSIGNED NOT NULL,
    recipient_phone VARCHAR(24) NOT NULL,
    scheduled_at_utc DATETIME NOT NULL,
    next_attempt_at DATETIME NOT NULL,
    status ENUM('pending','processing','sent','failed','cancelled') NOT NULL DEFAULT 'pending',
    attempt_count SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    last_error VARCHAR(500) NULL,
    meta_message_id VARCHAR(255) NULL,
    template_name VARCHAR(128) NOT NULL,
    template_language VARCHAR(16) NOT NULL,
    parameters_json JSON NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_whatsapp_reminder (appointment_external_id, offset_minutes),
    KEY idx_whatsapp_due (status, next_attempt_at, scheduled_at_utc),
    KEY idx_whatsapp_business_appointment (business_id, appointment_external_id),
    CONSTRAINT fk_whatsapp_reminder_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
