CREATE TABLE IF NOT EXISTS `sh_clan_denied` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `userid` INT NOT NULL,
    `clanid` INT NOT NULL,
    `deny_time` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;