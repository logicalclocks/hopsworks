-- FSTORE-1020
ALTER TABLE `hopsworks`.`training_dataset_filter_condition` DROP FOREIGN KEY `tdfc_feature_group_fk`;
ALTER TABLE `hopsworks`.`training_dataset_filter_condition` ADD FOREIGN KEY `tdfc_feature_group_fk`(`feature_group_id`)
    REFERENCES `hopsworks`.`feature_group` (`id`)
    ON DELETE SET NULL ON UPDATE NO ACTION;

ALTER TABLE `hopsworks`.`conda_commands` MODIFY COLUMN `created` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3);

DROP TABLE `hopsworks`.`pia`;

ALTER TABLE `hopsworks`.`oauth_client` ADD COLUMN `given_name_claim` VARCHAR(255) NOT NULL DEFAULT 'given_name';
ALTER TABLE `hopsworks`.`oauth_client` ADD COLUMN `family_name_claim` VARCHAR(255) NOT NULL DEFAULT 'family_name';
ALTER TABLE `hopsworks`.`oauth_client` ADD COLUMN `email_claim` VARCHAR(255) NOT NULL DEFAULT 'email';
ALTER TABLE `hopsworks`.`oauth_client` ADD COLUMN `group_claim` VARCHAR(255) DEFAULT NULL;

-- FSTORE-980: helper columns for feature view
ALTER TABLE `hopsworks`.`training_dataset_feature` ADD COLUMN `inference_helper_column` tinyint(1) DEFAULT '0';
ALTER TABLE `hopsworks`.`training_dataset_feature` ADD COLUMN `training_helper_column` tinyint(1) DEFAULT '0';
