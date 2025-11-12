USE prismnetai;

CREATE TABLE IF NOT EXISTS `clients` (
  `client_id` VARCHAR(100) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `configuration` JSON NULL,
  PRIMARY KEY (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `api_keys` (
  `id` VARCHAR(100) NOT NULL,
  `client_id` VARCHAR(100) NOT NULL,
  `hashed_secret` VARCHAR(128) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `revoked` TINYINT(1) NOT NULL DEFAULT 0,
  `last_used` TIMESTAMP NULL DEFAULT NULL,
  `description` TEXT NULL,
  `metadata` JSON NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_api_keys_client_id` (`client_id`),
  CONSTRAINT `fk_api_keys_client` FOREIGN KEY (`client_id`) REFERENCES `clients`(`client_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_api_keys_last_used` ON `api_keys` (`last_used`);
CREATE INDEX `idx_api_keys_revoked` ON `api_keys` (`revoked`);


INSERT INTO `clients` (
  `client_id`,
  `name`,
  `created_at`,
  `last_modified`
)
VALUES (
  'admin',
  'admin',
  '2025-11-05 10:30:00',
  '2025-11-05 10:30:00'
);


INSERT INTO api_keys (id, client_id, hashed_secret, revoked, description) 
VALUES ('kb56963f102bb', 'admin', 'eda487408ac99a136cea46924d1d27beb9907cdc2100992fe4f4f4edbdbd659c', 0, 'manually created admin key');