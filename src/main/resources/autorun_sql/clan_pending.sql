CREATE TABLE IF NOT EXISTS `sh_clan_pending` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `userid` INT NOT NULL,
    `clanid` INT NOT NULL,
    `request_time` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;