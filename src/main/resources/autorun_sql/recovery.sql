CREATE TABLE IF NOT EXISTS `sh_recovery` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `token` VARCHAR(50) NOT NULL,
  `user` INT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB;
