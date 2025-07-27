CREATE TABLE IF NOT EXISTS `sh_clan_audit` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `actor_id` INT NOT NULL,           -- кто сделал
  `action` VARCHAR(32) NOT NULL,     -- что сделал
  `target_id` INT DEFAULT NULL,      -- кому сделал (может быть null)
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- когда сделал
  PRIMARY KEY (`id`),
  KEY `actor_id_idx` (`actor_id`),
  KEY `target_id_idx` (`target_id`)
) ENGINE=InnoDB;
