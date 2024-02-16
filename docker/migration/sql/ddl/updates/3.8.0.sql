-- HWORKS-987
ALTER TABLE `hopsworks`.`model_version`
    ADD CONSTRAINT `model_version_key` UNIQUE (`model_id`, `version`);
ALTER TABLE `hopsworks`.`model_version`
    DROP PRIMARY KEY;
ALTER TABLE `hopsworks`.`model_version`
    ADD COLUMN id int(11) AUTO_INCREMENT PRIMARY KEY;

--FSTORE-1190
ALTER TABLE `hopsworks`.`embedding_feature`
    ADD COLUMN `model_version_id` INT(11) NULL;

ALTER TABLE `hopsworks`.`embedding_feature`
    ADD CONSTRAINT `embedding_feature_model_version_fk` FOREIGN KEY (`model_version_id`) REFERENCES `model_version` (`id`) ON DELETE SET NULL ON UPDATE NO ACTION;