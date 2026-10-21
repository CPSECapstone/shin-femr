# --- !Ups

CREATE TABLE `language_codes` (
    `code` VARCHAR(5) NOT NULL,
    `language_name` VARCHAR(64) NOT NULL,
    `status` VARCHAR(64) NOT NULL,
    `updateScheduled` BOOLEAN NOT NULL,
    PRIMARY KEY (code)
);

CREATE  TABLE `burn_rates`
(
    `id`              INT NOT NULL AUTO_INCREMENT,
    `med_id`          INT,
    `trip_id`          INT,
    `a_s`       VARCHAR(255) NULL ,
    `calculated_time` DATETIME,

    PRIMARY KEY (`id`),
    UNIQUE INDEX `id_UNIQUE` (`id` ASC)
);

# --- !Downs

DROP TABLE IF EXISTS `language_codes`;

DROP TABLE IF EXISTS burn_rates;