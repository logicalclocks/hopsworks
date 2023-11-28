-- MySQL dump 10.13  Distrib 5.7.25-ndb-7.6.9, for linux-glibc2.12 (x86_64)
--
-- Host: localhost    Database: hopsworks
-- ------------------------------------------------------
-- Server version	5.7.25-ndb-7.6.9-cluster-gpl

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

DELIMITER $$

DROP PROCEDURE IF EXISTS CREATE_TABLE_SPACE$$

CREATE PROCEDURE CREATE_TABLE_SPACE ()
BEGIN
    DECLARE lc INTEGER;
    DECLARE tc INTEGER;

    SELECT count(LOGFILE_GROUP_NAME) INTO lc FROM INFORMATION_SCHEMA.FILES where LOGFILE_GROUP_NAME="lg_1";
    IF (lc = 0) THEN
        CREATE LOGFILE GROUP lg_1 ADD UNDOFILE 'undo_log_0.log' INITIAL_SIZE = 128M ENGINE ndbcluster;
    ELSE
    	select "The LogFile has already been created" as "";
    END IF;


    SELECT count(TABLESPACE_NAME) INTO tc FROM INFORMATION_SCHEMA.FILES where TABLESPACE_NAME="ts_1";
    IF (tc = 0) THEN
    	CREATE TABLESPACE ts_1 ADD datafile 'ts_1_data_file_0.dat' use LOGFILE GROUP lg_1 INITIAL_SIZE = 128M  ENGINE ndbcluster;
    ELSE
    	select "The DataFile has already been created" as "";
    END IF;
END$$

DELIMITER ;

CALL CREATE_TABLE_SPACE;


--
-- Table structure for table `account_audit`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `account_audit` (
                                 `log_id` bigint(20) NOT NULL AUTO_INCREMENT,
                                 `initiator` int(11) NOT NULL,
                                 `target` int(11) NOT NULL,
                                 `action` varchar(45) COLLATE latin1_general_cs DEFAULT NULL,
                                 `action_timestamp` timestamp NULL DEFAULT NULL,
                                 `message` varchar(100) COLLATE latin1_general_cs DEFAULT NULL,
                                 `outcome` varchar(45) COLLATE latin1_general_cs DEFAULT NULL,
                                 `ip` varchar(45) COLLATE latin1_general_cs DEFAULT NULL,
                                 `useragent` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
                                 PRIMARY KEY (`log_id`),
                                 KEY `initiator` (`initiator`),
                                 KEY `target` (`target`),
                                 CONSTRAINT `FK_257_274` FOREIGN KEY (`initiator`) REFERENCES `users` (`uid`) ON DELETE NO ACTION ON UPDATE NO ACTION,
                                 CONSTRAINT `FK_257_275` FOREIGN KEY (`target`) REFERENCES `users` (`uid`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=515 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `activity`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `activity` (
                            `id` int(11) NOT NULL AUTO_INCREMENT,
                            `activity` varchar(255) COLLATE latin1_general_cs NOT NULL,
                            `user_id` int(10) NOT NULL,
                            `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            `flag` varchar(128) COLLATE latin1_general_cs DEFAULT NULL,
                            `project_id` int(11) NOT NULL,
                            PRIMARY KEY (`id`),
                            KEY `project_id` (`project_id`),
                            KEY `user_id` (`user_id`),
                            CONSTRAINT `FK_257_296` FOREIGN KEY (`user_id`) REFERENCES `users` (`uid`) ON DELETE NO ACTION ON UPDATE NO ACTION,
                            CONSTRAINT `FK_284_295` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=1547 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bbc_group`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bbc_group` (
                             `group_name` varchar(20) COLLATE latin1_general_cs NOT NULL,
                             `group_desc` varchar(200) COLLATE latin1_general_cs DEFAULT NULL,
                             `gid` int(11) NOT NULL,
                             PRIMARY KEY (`gid`)
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cluster_cert`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cluster_cert` (
                                `id` int(11) NOT NULL AUTO_INCREMENT,
                                `agent_id` int(11) NOT NULL,
                                `common_name` varchar(64) COLLATE latin1_general_cs NOT NULL,
                                `organization_name` varchar(64) COLLATE latin1_general_cs NOT NULL,
                                `organizational_unit_name` varchar(64) COLLATE latin1_general_cs NOT NULL,
                                `serial_number` varchar(45) COLLATE latin1_general_cs DEFAULT NULL,
                                `registration_status` varchar(45) COLLATE latin1_general_cs NOT NULL,
                                `validation_key` varchar(128) COLLATE latin1_general_cs DEFAULT NULL,
                                `validation_key_date` timestamp NULL DEFAULT NULL,
                                `registration_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `organization_name` (`organization_name`,`organizational_unit_name`),
                                UNIQUE KEY `serial_number` (`serial_number`),
                                KEY `agent_id` (`agent_id`),
                                CONSTRAINT `FK_257_552` FOREIGN KEY (`agent_id`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=7 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `conda_commands`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `conda_commands` (
                                  `id` int(11) NOT NULL AUTO_INCREMENT,
                                  `project_id` int(11) NOT NULL,
                                  `op` varchar(52) COLLATE latin1_general_cs NOT NULL,
                                  `channel_url` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
                                  `arg` varchar(11000) COLLATE latin1_general_cs DEFAULT NULL,
                                  `lib` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
                                  `version` varchar(52) COLLATE latin1_general_cs DEFAULT NULL,
                                  `status` varchar(52) COLLATE latin1_general_cs NOT NULL,
                                  `created` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                                  `install_type` varchar(52) COLLATE latin1_general_cs DEFAULT NULL,
                                  `environment_file` varchar(1000) COLLATE latin1_general_cs DEFAULT NULL,
                                  `custom_commands_file` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
                                  `install_jupyter` tinyint(1) NOT NULL DEFAULT '0',
                                  `git_api_key_name` VARCHAR(125) DEFAULT NULL,
                                  `git_backend` VARCHAR(45) DEFAULT NULL,
                                  `user_id` int(11) NOT NULL,
                                  `error_message` VARCHAR(10000) COLLATE latin1_general_cs DEFAULT NULL,
                                  PRIMARY KEY (`id`),
                                  KEY `project_id` (`project_id`),
                                  CONSTRAINT `user_fk_conda` FOREIGN KEY (`user_id`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=32 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `environment_history`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `environment_history` (
                           `id` int(11) NOT NULL AUTO_INCREMENT,
                           `project` int(11)  NOT NULL,
                           `docker_image` varchar(255) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
                           `downgraded` varchar(7000) CHARACTER SET latin1 COLLATE latin1_general_cs DEFAULT NULL,
                           `installed` varchar(7000) CHARACTER SET latin1 COLLATE latin1_general_cs DEFAULT NULL,
                           `uninstalled` varchar(7000) CHARACTER SET latin1 COLLATE latin1_general_cs DEFAULT NULL,
                           `upgraded` varchar(7000) CHARACTER SET latin1 COLLATE latin1_general_cs DEFAULT NULL,
                           `previous_docker_image` varchar(255) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
                           `user` int(11)  NOT NULL,
                           `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           PRIMARY KEY (`id`),
                           KEY `env_project_fk` (`project`),
                           CONSTRAINT `env_project_fk` FOREIGN KEY (`project`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dataset` (
                           `id` int(11) NOT NULL AUTO_INCREMENT,
                           `inode_name` varchar(255) COLLATE latin1_general_cs NOT NULL,
                           `projectId` int(11) NOT NULL,
                           `description` varchar(2000) COLLATE latin1_general_cs DEFAULT NULL,
                           `searchable` tinyint(1) NOT NULL DEFAULT '0',
                           `public_ds` tinyint(1) NOT NULL DEFAULT '0',
                           `public_ds_id` varchar(1000) COLLATE latin1_general_cs DEFAULT '0',
                           `dstype` int(11) NOT NULL DEFAULT '0',
                           `feature_store_id` int(11) DEFAULT NULL,
                           `permission` VARCHAR(45) NOT NULL DEFAULT 'READ_ONLY',
                           PRIMARY KEY (`id`),
                           KEY `projectId_name` (`projectId`,`inode_name`),
                           KEY `featurestore_fk` (`feature_store_id`),
                           KEY `dataset_name` (`inode_name`),
                           CONSTRAINT `featurestore_fk` FOREIGN KEY (`feature_store_id`) REFERENCES `feature_store` (`id`) ON DELETE SET NULL ON UPDATE NO ACTION,
                           CONSTRAINT `FK_284_434` FOREIGN KEY (`projectId`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=747 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset_shared_with`
--


/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dataset_shared_with` (
                                       `id` int(11) NOT NULL AUTO_INCREMENT,
                                       `dataset` int(11) NOT NULL,
                                       `project` int(11) NOT NULL,
                                       `accepted` tinyint(1) NOT NULL DEFAULT '0',
                                       `shared_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       `permission` VARCHAR(45) NOT NULL DEFAULT 'READ_ONLY',
                                       `shared_by` INT(11) DEFAULT NULL,
                                       `accepted_by` INT(11) DEFAULT NULL,
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `index2` (`dataset`,`project`),
                                       KEY `fk_dataset_shared_with_2_idx` (`project`),
                                       CONSTRAINT `fk_dataset_shared_with_1` FOREIGN KEY (`dataset`) REFERENCES `dataset` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                       CONSTRAINT `fk_dataset_shared_with_2` FOREIGN KEY (`project`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                       CONSTRAINT `fk_shared_by` FOREIGN KEY (`shared_by`) REFERENCES `users` (`uid`) ON DELETE NO ACTION ON UPDATE NO ACTION,
                                       CONSTRAINT `fk_accepted_by` FOREIGN KEY (`accepted_by`) REFERENCES `users` (`uid`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset_request`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dataset_request` (
                                   `id` int(11) NOT NULL AUTO_INCREMENT,
                                   `dataset` int(11) NOT NULL,
                                   `projectId` int(11) NOT NULL,
                                   `user_email` varchar(150) COLLATE latin1_general_cs NOT NULL,
                                   `requested` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   `message` varchar(3000) COLLATE latin1_general_cs DEFAULT NULL,
                                   `message_id` int(11) DEFAULT NULL,
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `index2` (`dataset`,`projectId`),
                                   KEY `projectId` (`projectId`,`user_email`),
                                   KEY `message_id` (`message_id`),
                                   CONSTRAINT `FK_429_449` FOREIGN KEY (`dataset`) REFERENCES `dataset` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                   CONSTRAINT `FK_438_452` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
                                   CONSTRAINT `FK_302_451` FOREIGN KEY (`projectId`,`user_email`) REFERENCES `project_team` (`project_id`,`team_member`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=3 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `executions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `executions` (
                              `id` int(11) NOT NULL AUTO_INCREMENT,
                              `submission_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              `user` varchar(150) COLLATE latin1_general_cs NOT NULL,
                              `state` varchar(128) COLLATE latin1_general_cs NOT NULL,
                              `execution_start` bigint(20) DEFAULT NULL,
                              `execution_stop` bigint(20) DEFAULT NULL,
                              `stdout_path` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
                              `stderr_path` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
                              `hdfs_user` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
                              `args` varchar(10000) COLLATE latin1_general_cs NOT NULL DEFAULT '',
                              `app_id` char(45) COLLATE latin1_general_cs DEFAULT NULL,
                              `job_id` int(11) NOT NULL,
                              `finalStatus` varchar(128) COLLATE latin1_general_cs NOT NULL DEFAULT 'UNDEFINED',
                              `progress` float NOT NULL DEFAULT '0',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `app_id` (`app_id`),
                              KEY `job_id` (`job_id`),
                              KEY `user` (`user`),
                              KEY `submission_time_idx` (`submission_time`,`job_id`),
                              KEY `state_idx` (`state`,`job_id`),
                              KEY `finalStatus_idx` (`finalStatus`,`job_id`),
                              KEY `progress_idx` (`progress`,`job_id`),
                              CONSTRAINT `FK_262_366` FOREIGN KEY (`user`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=23 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `feature_group`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `feature_group` (
                                 `id` int(11) NOT NULL AUTO_INCREMENT,
                                 `name` varchar(63) NOT NULL,
                                 `feature_store_id` int(11) NOT NULL,
                                 `created` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                                 `creator` int(11) NOT NULL,
                                 `version` int(11) NOT NULL,
                                 `description` VARCHAR(1000) NULL,
                                 `feature_group_type` INT(11) NOT NULL DEFAULT '0',
                                 `on_demand_feature_group_id` INT(11) NULL,
                                 `cached_feature_group_id` INT(11) NULL,
                                 `stream_feature_group_id` INT(11) NULL,
                                 `event_time` VARCHAR(63) DEFAULT NULL,
                                 `online_enabled` TINYINT(1) NULL,
                                 `topic_name` VARCHAR(255) DEFAULT NULL,
                                 `deprecated` BOOLEAN DEFAULT FALSE,
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `name_version` (`feature_store_id`, `name`, `version`),
                                 KEY `feature_store_id` (`feature_store_id`),
                                 KEY `creator` (`creator`),
                                 KEY `on_demand_feature_group_fk` (`on_demand_feature_group_id`),
                                 KEY `cached_feature_group_fk` (`cached_feature_group_id`),
                                 KEY `stream_feature_group_fk` (`stream_feature_group_id`),
                                 CONSTRAINT `FK_1012_790` FOREIGN KEY (`creator`) REFERENCES `users` (`uid`) ON DELETE NO ACTION ON UPDATE NO ACTION,
                                 CONSTRAINT `FK_656_740` FOREIGN KEY (`feature_store_id`) REFERENCES `feature_store` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                 CONSTRAINT `on_demand_feature_group_fk2` FOREIGN KEY (`on_demand_feature_group_id`) REFERENCES `on_demand_feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                 CONSTRAINT `cached_feature_group_fk` FOREIGN KEY (`cached_feature_group_id`) REFERENCES `cached_feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                 CONSTRAINT `stream_feature_group_fk` FOREIGN KEY (`stream_feature_group_id`) REFERENCES `stream_feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=13 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `statistics_config`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `statistics_config` (
                                     `id` int(11) NOT NULL AUTO_INCREMENT,
                                     `feature_group_id` int(11),
                                     `training_dataset_id` int(11),
                                     `descriptive` TINYINT(1) NOT NULL DEFAULT '1',
                                     `correlations` TINYINT(1) NOT NULL DEFAULT '1',
                                     `histograms` TINYINT(1) NOT NULL DEFAULT '1',
                                     `exact_uniqueness` TINYINT(1) NOT NULL DEFAULT '1',
                                     PRIMARY KEY (`id`),
                                     KEY `feature_group_id` (`feature_group_id`),
                                     KEY `training_dataset_id` (`training_dataset_id`),
                                     CONSTRAINT `fg_statistics_config_fk` FOREIGN KEY (`feature_group_id`) REFERENCES `feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                     CONSTRAINT `td_statistics_config_fk` FOREIGN KEY (`training_dataset_id`) REFERENCES `training_dataset` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `statistic_columns`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `statistic_columns` (
                                     `id` int(11) NOT NULL AUTO_INCREMENT,
                                     `statistics_config_id` int(11),
                                     `name` varchar(500) COLLATE latin1_general_cs DEFAULT NULL,
                                     PRIMARY KEY (`id`),
                                     KEY `statistics_config_id` (`statistics_config_id`),
                                     CONSTRAINT `statistics_config_fk` FOREIGN KEY (`statistics_config_id`) REFERENCES `statistics_config` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `feature_store`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `feature_store` (
                                 `id` int(11) NOT NULL AUTO_INCREMENT,
                                 `name` varchar(100) COLLATE latin1_general_cs NOT NULL,
                                 `project_id` int(11) NOT NULL,
                                 `created` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                                 PRIMARY KEY (`id`),
                                 KEY `project_id` (`project_id`),
                                 CONSTRAINT `FK_883_662` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=67 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `feature_store_statistic`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `feature_store_statistic` (
                                           `id` int(11) NOT NULL AUTO_INCREMENT,
                                           `commit_time` DATETIME(3) NOT NULL,
                                           `file_path` VARCHAR(1000) NOT NULL,
                                           `feature_group_id` INT(11),
                                           `feature_group_commit_id` BIGINT(20),
                                           `training_dataset_id`INT(11),
                                           `for_transformation` TINYINT(1) DEFAULT '0',
                                           PRIMARY KEY (`id`),
                                           KEY `feature_group_id` (`feature_group_id`),
                                           KEY `training_dataset_id` (`training_dataset_id`),
                                           KEY `feature_group_commit_id_fk` (`feature_group_id`, `feature_group_commit_id`),
                                           CONSTRAINT `fg_fk_fss` FOREIGN KEY (`feature_group_id`) REFERENCES `feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                           CONSTRAINT `td_fk_fss` FOREIGN KEY (`training_dataset_id`) REFERENCES `training_dataset` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `feature_store_code`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `feature_store_code` (
                                      `id` int(11) NOT NULL AUTO_INCREMENT,
                                      `commit_time` DATETIME(3) NOT NULL,
                                      `name` varchar(255) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
                                      `feature_group_id` INT(11),
                                      `feature_group_commit_id` BIGINT(20),
                                      `training_dataset_id` INT(11),
                                      `application_id`VARCHAR(50),
                                      PRIMARY KEY (`id`),
                                      KEY `feature_group_id` (`feature_group_id`),
                                      KEY `training_dataset_id` (`training_dataset_id`),
                                      KEY `feature_group_commit_id_fk` (`feature_group_id`, `feature_group_commit_id`),
                                      CONSTRAINT `fg_fk_fsc` FOREIGN KEY (`feature_group_id`) REFERENCES `feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                      CONSTRAINT `fg_ci_fk_fsc` FOREIGN KEY (`feature_group_id`, `feature_group_commit_id`) REFERENCES `feature_group_commit` (`feature_group_id`, `commit_id`) ON DELETE SET NULL ON UPDATE NO ACTION,
                                      CONSTRAINT `td_fk_fsc` FOREIGN KEY (`training_dataset_id`) REFERENCES `training_dataset` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

--
-- Table structure for table `files_to_remove`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `files_to_remove` (
                                   `execution_id` int(11) NOT NULL,
                                   `filepath` varchar(255) COLLATE latin1_general_cs NOT NULL,
                                   PRIMARY KEY (`execution_id`,`filepath`),
                                   CONSTRAINT `FK_361_376` FOREIGN KEY (`execution_id`) REFERENCES `executions` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `host_services`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `host_services` (
                                 `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                 `host_id` int(11) NOT NULL,
                                 `pid` int(11) DEFAULT NULL,
                                 `name` varchar(48) COLLATE latin1_general_cs NOT NULL,
                                 `group_name` varchar(48) COLLATE latin1_general_cs NOT NULL,
                                 `status` int(11) NOT NULL,
                                 `uptime` bigint(20) DEFAULT NULL,
                                 `startTime` bigint(20) DEFAULT NULL,
                                 `stopTime` bigint(20) DEFAULT NULL,
                                 PRIMARY KEY (`id`),
                                 KEY `host_id` (`host_id`),
                                 UNIQUE KEY `service_UNIQUE` (`host_id`, `name`),
                                 CONSTRAINT `FK_481_491` FOREIGN KEY (`host_id`) REFERENCES `hosts` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=42 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hosts`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `hosts` (
                         `id` int(11) NOT NULL AUTO_INCREMENT,
                         `hostname` varchar(128) COLLATE latin1_general_cs NOT NULL,
                         `cores` int(11) DEFAULT NULL,
                         `host_ip` varchar(128) COLLATE latin1_general_cs NOT NULL,
                         `last_heartbeat` bigint(20) DEFAULT NULL,
                         `memory_capacity` bigint(20) DEFAULT NULL,
                         `private_ip` varchar(15) COLLATE latin1_general_cs DEFAULT NULL,
                         `public_ip` varchar(15) COLLATE latin1_general_cs DEFAULT NULL,
                         `agent_password` varchar(25) COLLATE latin1_general_cs DEFAULT NULL,
                         `num_gpus` tinyint(1) DEFAULT '0',
                         `registered` tinyint(1) DEFAULT '0',
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `hostname` (`hostname`),
                         UNIQUE KEY `host_ip` (`host_ip`)
) ENGINE=ndbcluster AUTO_INCREMENT=6 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `invalid_jwt`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `invalid_jwt` (
                               `jti` varchar(45) COLLATE latin1_general_cs NOT NULL,
                               `expiration_time` datetime NOT NULL,
                               `renewable_for_sec` int(11) NOT NULL,
                               PRIMARY KEY (`jti`)
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jobs`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `jobs` (
                        `id` int(11) NOT NULL AUTO_INCREMENT,
                        `name` varchar(128) COLLATE latin1_general_cs DEFAULT NULL,
                        `creation_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        `project_id` int(11) NOT NULL,
                        `creator` varchar(150) COLLATE latin1_general_cs NOT NULL,
                        `type` varchar(128) COLLATE latin1_general_cs NOT NULL,
                        `json_config` varchar(12500) COLLATE latin1_general_cs NOT NULL,
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `name_project_idx` (`name`,`project_id`),
                        KEY `project_id` (`project_id`),
                        KEY `creator` (`creator`),
                        KEY `creator_project_idx` (`creator`,`project_id`),
                        KEY `creation_time_project_idx` (`creation_time`,`project_id`),
                        KEY `type_project_id_idx` (`type`,`project_id`),
                        CONSTRAINT `FK_262_353` FOREIGN KEY (`creator`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION,
                        CONSTRAINT `FK_284_352` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=37 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jupyter_project`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `jupyter_project` (
                                   `port` int(11) NOT NULL,
                                   `uid` int(11) NOT NULL,
                                   `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   `expires` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   `no_limit` tinyint(1) DEFAULT 0,
                                   `token` varchar(255) COLLATE latin1_general_cs NOT NULL,
                                   `secret` varchar(64) COLLATE latin1_general_cs NOT NULL,
                                   `cid` varchar(255) COLLATE latin1_general_cs NOT NULL,
                                   `project_id` int(11) NOT NULL,
                                   PRIMARY KEY (`port`),
                                   UNIQUE KEY `project_user` (`project_id`, `uid`),
                                   KEY `project_id` (`project_id`),
                                   CONSTRAINT `jp_uid_fk` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                   CONSTRAINT `FK_284_526` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jupyter_settings`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `jupyter_settings` (
                                    `project_id` int(11) NOT NULL,
                                    `team_member` varchar(150) COLLATE latin1_general_cs NOT NULL,
                                    `secret` varchar(255) COLLATE latin1_general_cs NOT NULL,
                                    `advanced` tinyint(1) DEFAULT '0',
                                    `shutdown_level` int(11) NOT NULL DEFAULT '6',
                                    `no_limit` tinyint(1) DEFAULT 0,
                                    `base_dir` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
                                    `job_config` varchar(11000) COLLATE latin1_general_cs DEFAULT NULL,
                                    `docker_config` varchar(1000) COLLATE latin1_general_cs DEFAULT NULL,
                                    `python_kernel` TINYINT(1) DEFAULT 1,
                                    PRIMARY KEY (`project_id`,`team_member`),
                                    KEY `team_member` (`team_member`),
                                    KEY `secret_idx` (`secret`),
                                    CONSTRAINT `FK_262_309` FOREIGN KEY (`team_member`) REFERENCES `users` (`email`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                    CONSTRAINT `FK_284_308` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jwt_signing_key`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `jwt_signing_key` (
                                   `id` int(11) NOT NULL AUTO_INCREMENT,
                                   `secret` varchar(128) COLLATE latin1_general_cs NOT NULL,
                                   `name` varchar(255) COLLATE latin1_general_cs NOT NULL,
                                   `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `jwt_signing_key_name_UNIQUE` (`name`)
) ENGINE=ndbcluster AUTO_INCREMENT=33 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `maggy_driver`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `maggy_driver` (
                                `id` mediumint(9) NOT NULL AUTO_INCREMENT,
                                `app_id` char(45) COLLATE latin1_general_cs NOT NULL,
                                `host_ip` varchar(128) COLLATE latin1_general_cs NOT NULL,
                                `port` int(11) NOT NULL,
                                `secret` varchar(128) COLLATE latin1_general_cs NOT NULL,
                                `created` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (`id`),
                                KEY `app_id` (`app_id`,`port`)
) ENGINE=ndbcluster AUTO_INCREMENT=2 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `materialized_jwt`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `materialized_jwt` (
                                    `project_id` int(11) NOT NULL,
                                    `user_id` int(11) NOT NULL,
                                    `usage` tinyint(4) NOT NULL,
                                    PRIMARY KEY (`project_id`,`user_id`,`usage`),
                                    KEY `jwt_material_user` (`user_id`),
                                    CONSTRAINT `jwt_material_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                    CONSTRAINT `jwt_material_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `message`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `message` (
                           `id` int(11) NOT NULL AUTO_INCREMENT,
                           `user_from` varchar(150) COLLATE latin1_general_cs DEFAULT NULL,
                           `user_to` varchar(150) COLLATE latin1_general_cs NOT NULL,
                           `date_sent` datetime NOT NULL,
                           `subject` varchar(128) COLLATE latin1_general_cs DEFAULT NULL,
                           `preview` varchar(128) COLLATE latin1_general_cs DEFAULT NULL,
                           `content` varchar(11000) COLLATE latin1_general_cs NOT NULL,
                           `unread` tinyint(1) NOT NULL,
                           `deleted` tinyint(1) NOT NULL,
                           `path` varchar(600) COLLATE latin1_general_cs DEFAULT NULL,
                           `reply_to_msg` int(11) DEFAULT NULL,
                           PRIMARY KEY (`id`),
                           KEY `user_from` (`user_from`),
                           KEY `user_to` (`user_to`),
                           KEY `reply_to_msg` (`reply_to_msg`),
                           CONSTRAINT `FK_262_441` FOREIGN KEY (`user_from`) REFERENCES `users` (`email`) ON DELETE SET NULL ON UPDATE NO ACTION,
                           CONSTRAINT `FK_262_442` FOREIGN KEY (`user_to`) REFERENCES `users` (`email`) ON DELETE CASCADE ON UPDATE NO ACTION,
                           CONSTRAINT `FK_438_443` FOREIGN KEY (`reply_to_msg`) REFERENCES `message` (`id`) ON DELETE SET NULL ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=3 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `message_to_user`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `message_to_user` (
                                   `message` int(11) NOT NULL,
                                   `user_email` varchar(150) COLLATE latin1_general_cs NOT NULL,
                                   PRIMARY KEY (`message`,`user_email`),
                                   KEY `user_email` (`user_email`),
                                   CONSTRAINT `FK_262_458` FOREIGN KEY (`user_email`) REFERENCES `users` (`email`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                   CONSTRAINT `FK_438_457` FOREIGN KEY (`message`) REFERENCES `message` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oauth_client`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `oauth_client` (
                                `id` int(11) NOT NULL AUTO_INCREMENT,
                                `client_id` varchar(256) COLLATE latin1_general_cs NOT NULL,
                                `client_secret` varchar(2048) COLLATE latin1_general_cs NOT NULL,
                                `provider_logo_uri` varchar(2048) COLLATE latin1_general_cs DEFAULT NULL,
                                `provider_uri` varchar(2048) COLLATE latin1_general_cs NOT NULL,
                                `provider_name` varchar(256) COLLATE latin1_general_cs NOT NULL,
                                `provider_display_name` varchar(45) COLLATE latin1_general_cs NOT NULL,
                                `authorisation_endpoint` varchar(1024) COLLATE latin1_general_cs DEFAULT NULL,
                                `token_endpoint` varchar(1024) COLLATE latin1_general_cs DEFAULT NULL,
                                `userinfo_endpoint` varchar(1024) COLLATE latin1_general_cs DEFAULT NULL,
                                `end_session_endpoint` VARCHAR(1024) COLLATE latin1_general_cs DEFAULT NULL,
                                `logout_redirect_param` VARCHAR(45) COLLATE latin1_general_cs DEFAULT NULL,
                                `jwks_uri` varchar(1024) COLLATE latin1_general_cs DEFAULT NULL,
                                `provider_metadata_endpoint_supported` tinyint(1) NOT NULL DEFAULT '0',
                                `offline_access` tinyint(1) NOT NULL DEFAULT '0',
                                `code_challenge` tinyint(1) NOT NULL DEFAULT '0',
                                `code_challenge_method` varchar(16) COLLATE latin1_general_cs DEFAULT NULL,
                                `verify_email` tinyint(1) NOT NULL DEFAULT '0',
                                `given_name_claim` varchar(256) COLLATE latin1_general_cs NOT NULL DEFAULT 'given_name',
                                `family_name_claim` varchar(256) COLLATE latin1_general_cs NOT NULL DEFAULT 'family_name',
                                `email_claim` varchar(256) COLLATE latin1_general_cs NOT NULL DEFAULT 'email',
                                `group_claim` varchar(256) COLLATE latin1_general_cs DEFAULT NULL,
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `client_id_UNIQUE` (`client_id`),
                                UNIQUE KEY `provider_name_UNIQUE` (`provider_name`)
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oauth_login_state`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `oauth_login_state` (
                                     `id` int(11) NOT NULL AUTO_INCREMENT,
                                     `state` varchar(256) COLLATE latin1_general_cs NOT NULL,
                                     `session_id` varchar(128) COLLATE latin1_general_cs NOT NULL,
                                     `client_id` varchar(256) COLLATE latin1_general_cs NOT NULL,
                                     `login_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     `id_token` varchar(8000) COLLATE latin1_general_cs DEFAULT NULL,
                                     `access_token` varchar(8000) COLLATE latin1_general_cs DEFAULT NULL,
                                     `refresh_token` varchar(8000) COLLATE latin1_general_cs DEFAULT NULL,
                                     `nonce` varchar(128) COLLATE latin1_general_cs NOT NULL,
                                     `scopes` VARCHAR(2048) COLLATE latin1_general_cs NOT NULL,
                                     `code_challenge` varchar(128) COLLATE latin1_general_cs DEFAULT NULL,
                                     `redirect_uri` varchar(1024) COLLATE latin1_general_cs NOT NULL,
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `login_state_UNIQUE` (`state`),
                                     KEY `fk_oauth_login_state_client` (`client_id`),
                                     CONSTRAINT `fk_oauth_login_state_client` FOREIGN KEY (`client_id`) REFERENCES `oauth_client` (`client_id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oauth_token`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `oauth_token` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `id_token` varchar(8000) COLLATE latin1_general_cs NOT NULL,
  `access_token` varchar(8000) COLLATE latin1_general_cs DEFAULT NULL,
  `refresh_token` varchar(8000) COLLATE latin1_general_cs DEFAULT NULL,
  `login_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `login_state_UNIQUE` (`user_id`),
  KEY `fk_oauth_token_user` (`user_id`),
  CONSTRAINT `fk_oauth_token_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`uid`) ON DELETE CASCADE
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ops_log`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ops_log` (
                           `id` int(11) NOT NULL AUTO_INCREMENT,
                           `op_id` int(11) NOT NULL,
                           `op_on` tinyint(1) NOT NULL,
                           `op_type` tinyint(1) NOT NULL,
                           `project_id` int(11) NOT NULL,
                           `dataset_id` bigint(20) NOT NULL,
                           `inode_id` bigint(20) NOT NULL,
                           PRIMARY KEY (`id`)
) ENGINE=ndbcluster AUTO_INCREMENT=308 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project` (
                           `id` int(11) NOT NULL AUTO_INCREMENT,
                           `projectname` varchar(100) COLLATE latin1_general_cs NOT NULL,
                           `username` varchar(150) COLLATE latin1_general_cs NOT NULL,
                           `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           `description` varchar(2000) COLLATE latin1_general_cs DEFAULT NULL,
                           `payment_type` varchar(255) COLLATE latin1_general_cs NOT NULL DEFAULT 'PREPAID',
                           `last_quota_update` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           `kafka_max_num_topics` int(11) NOT NULL DEFAULT '100',
                           `docker_image` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
                           `topic_name` VARCHAR(255) DEFAULT NULL,
                           `python_env_id` int(11) DEFAULT NULL,
                           `creation_status` tinyint(1) NOT NULL DEFAULT '0',
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `projectname` (`projectname`),
                           KEY `user_idx` (`username`),
                           CONSTRAINT `FK_262_290` FOREIGN KEY (`username`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=119 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `python_environment`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `python_environment` (
                                      `id` int(11) NOT NULL AUTO_INCREMENT,
                                      `project_id` int(11) NOT NULL,
                                      `python_version` VARCHAR(25) COLLATE latin1_general_cs NOT NULL,
                                      `jupyter_conflicts` TINYINT(1) NOT NULL DEFAULT '0',
                                      `conflicts` VARCHAR(12000) COLLATE latin1_general_cs DEFAULT NULL,
                                      UNIQUE KEY `project_env` (`project_id`),
                                      PRIMARY KEY (`id`),
                                      CONSTRAINT `FK_PYTHONENV_PROJECT` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `default_job_configuration`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `default_job_configuration` (
                                             `project_id` int(11) NOT NULL,
                                             `type` varchar(128) COLLATE latin1_general_cs NOT NULL,
                                             `config` VARCHAR(12500) COLLATE latin1_general_cs DEFAULT NULL,
                                             PRIMARY KEY (`project_id`, `type`),
                                             CONSTRAINT `FK_JOBCONFIG_PROJECT` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_pythondeps`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_pythondeps` (
                                      `project_id` int(11) NOT NULL,
                                      `dep_id` int(10) NOT NULL,
                                      PRIMARY KEY (`project_id`,`dep_id`),
                                      KEY `dep_id` (`dep_id`),
                                      CONSTRAINT `FK_284_513` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                      CONSTRAINT `FK_505_514` FOREIGN KEY (`dep_id`) REFERENCES `python_dep` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs
    /*!50100 PARTITION BY KEY (project_id) */;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_services`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_services` (
                                    `project_id` int(11) NOT NULL,
                                    `service` varchar(32) COLLATE latin1_general_cs NOT NULL,
                                    PRIMARY KEY (`project_id`,`service`),
                                    CONSTRAINT `FK_284_300` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_team`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_team` (
                                `project_id` int(11) NOT NULL,
                                `team_member` varchar(150) COLLATE latin1_general_cs NOT NULL,
                                `team_role` varchar(32) COLLATE latin1_general_cs NOT NULL,
                                `added` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                PRIMARY KEY (`project_id`,`team_member`),
                                KEY `team_member` (`team_member`),
                                CONSTRAINT `FK_262_304` FOREIGN KEY (`team_member`) REFERENCES `users` (`email`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                CONSTRAINT `FK_284_303` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- table structure for table `schemas`
--

/*!40101 set @saved_cs_client     = @@character_set_client */;
/*!40101 set character_set_client = utf8 */;
CREATE TABLE `schemas` (
                           `id` int(11) NOT NULL AUTO_INCREMENT,
                           `schema` varchar(29000) COLLATE latin1_general_cs NOT NULL,
                           `project_id` int(11) NOT NULL,
                           PRIMARY KEY (`id`),
                           CONSTRAINT `project_idx_schemas` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
--
-- table structure for table `subjects`
--

/*!40101 set @saved_cs_client     = @@character_set_client */;
/*!40101 set character_set_client = utf8 */;
CREATE TABLE `subjects` (
                            `id` int(11) NOT NULL AUTO_INCREMENT,
                            `subject` varchar(255) COLLATE latin1_general_cs NOT NULL,
                            `version` int(11) NOT NULL,
                            `schema_id` int(11) NOT NULL,
                            `project_id` int(11) NOT NULL,
                            `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (`id`),
                            KEY `project_id_idx` (`project_id`),
                            KEY `created_on_idx` (`created_on`),
                            CONSTRAINT `project_idx_subjects` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                            CONSTRAINT `schema_id_idx` FOREIGN KEY (`schema_id`) REFERENCES `schemas` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
                            CONSTRAINT `subjects__constraint_key` UNIQUE (`subject`, `version`, `project_id`)
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `subjects_compatibility`
--
CREATE TABLE `subjects_compatibility` (
                                          `id` int(11) NOT NULL AUTO_INCREMENT,
                                          `subject` varchar(255) COLLATE latin1_general_cs NOT NULL,
                                          `compatibility` ENUM('BACKWARD', 'BACKWARD_TRANSITIVE', 'FORWARD', 'FORWARD_TRANSITIVE', 'FULL', 'FULL_TRANSITIVE', 'NONE') NOT NULL DEFAULT 'BACKWARD',
                                          `project_id` int(11) NOT NULL,
                                          PRIMARY KEY (`id`),
                                          CONSTRAINT `subjects_compatibility__constraint_key` UNIQUE (`subject`, `project_id`),
                                          CONSTRAINT `project_idx_sub_comp` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `project_topics`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_topics` (
                                  `topic_name` varchar(255) COLLATE latin1_general_cs NOT NULL,
                                  `project_id` int(11) NOT NULL,
                                  `id` int(11) NOT NULL AUTO_INCREMENT,
                                  `subject_id` int(11) DEFAULT NULL,
                                  `num_partitions` int(11) DEFAULT NULL,
                                  `num_replicas` int(11) DEFAULT NULL,
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `topic_name_UNIQUE` (`topic_name`),
                                  UNIQUE KEY `topic_project` (`topic_name`,`project_id`),
                                  CONSTRAINT `project_idx_proj_topics` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                  CONSTRAINT `subject_idx` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `python_dep`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `python_dep` (
                              `id` int(11) NOT NULL AUTO_INCREMENT,
                              `dependency` varchar(128) COLLATE latin1_general_cs NOT NULL,
                              `version` varchar(128) COLLATE latin1_general_cs NOT NULL,
                              `repo_url` varchar(255) NOT NULL,
                              `preinstalled` tinyint(1) DEFAULT '0',
                              `install_type` int(11) NOT NULL,
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `dependency` (`dependency`,`version`,`install_type`,`repo_url`)
) ENGINE=ndbcluster AUTO_INCREMENT=31 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `remote_material_references`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `remote_material_references` (
                                              `username` varchar(128) COLLATE latin1_general_cs NOT NULL,
                                              `path` varchar(255) COLLATE latin1_general_cs NOT NULL,
                                              `references` int(11) NOT NULL DEFAULT '0',
                                              `lock` int(1) NOT NULL DEFAULT '0',
                                              `lock_id` varchar(30) COLLATE latin1_general_cs NOT NULL DEFAULT '',
                                              PRIMARY KEY (`username`,`path`)
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `remote_user`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `remote_user` (
                               `id` int(11) NOT NULL AUTO_INCREMENT,
                               `type` varchar(45) COLLATE latin1_general_cs NOT NULL,
                               `status` varchar(45) COLLATE latin1_general_cs NOT NULL DEFAULT '0',
                               `auth_key` varchar(64) COLLATE latin1_general_cs NOT NULL,
                               `uuid` varchar(128) COLLATE latin1_general_cs NOT NULL,
                               `uid` int(11) NOT NULL,
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uuid_UNIQUE` (`uuid`),
                               UNIQUE KEY `uid_UNIQUE` (`uid`),
                               CONSTRAINT `FK_257_557` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles_audit`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `roles_audit` (
                               `log_id` bigint(20) NOT NULL AUTO_INCREMENT,
                               `target` int(11) NOT NULL,
                               `initiator` int(11) NOT NULL,
                               `action` varchar(45) COLLATE latin1_general_cs DEFAULT NULL,
                               `action_timestamp` timestamp NULL DEFAULT NULL,
                               `message` varchar(45) COLLATE latin1_general_cs DEFAULT NULL,
                               `ip` varchar(45) COLLATE latin1_general_cs DEFAULT NULL,
                               `outcome` varchar(45) COLLATE latin1_general_cs DEFAULT NULL,
                               `useragent` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
                               PRIMARY KEY (`log_id`),
                               KEY `initiator` (`initiator`),
                               KEY `target` (`target`),
                               CONSTRAINT `FK_257_280` FOREIGN KEY (`initiator`) REFERENCES `users` (`uid`) ON DELETE NO ACTION ON UPDATE NO ACTION,
                               CONSTRAINT `FK_257_281` FOREIGN KEY (`target`) REFERENCES `users` (`uid`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rstudio_interpreter`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rstudio_interpreter` (
                                       `port` int(11) NOT NULL,
                                       `name` varchar(255) COLLATE latin1_general_cs NOT NULL,
                                       `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       `last_accessed` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       PRIMARY KEY (`port`,`name`),
                                       CONSTRAINT `FK_575_582` FOREIGN KEY (`port`) REFERENCES `rstudio_project` (`port`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rstudio_project`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rstudio_project` (
                                   `port` int(11) NOT NULL,
                                   `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   `last_accessed` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   `host_ip` varchar(255) COLLATE latin1_general_cs NOT NULL,
                                   `token` varchar(255) COLLATE latin1_general_cs NOT NULL,
                                   `secret` varchar(64) COLLATE latin1_general_cs NOT NULL,
                                   `pid` bigint(20) NOT NULL,
                                   `project_id` int(11) NOT NULL,
                                   PRIMARY KEY (`port`),
                                   KEY `project_id` (`project_id`),
                                   CONSTRAINT `FK_284_578` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rstudio_settings`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rstudio_settings` (
                                    `project_id` int(11) NOT NULL,
                                    `team_member` varchar(150) COLLATE latin1_general_cs NOT NULL,
                                    `num_tf_ps` int(11) DEFAULT '1',
                                    `num_tf_gpus` int(11) DEFAULT '0',
                                    `num_mpi_np` int(11) DEFAULT '1',
                                    `appmaster_cores` int(11) DEFAULT '1',
                                    `appmaster_memory` int(11) DEFAULT '1024',
                                    `num_executors` int(11) DEFAULT '1',
                                    `num_executor_cores` int(11) DEFAULT '1',
                                    `executor_memory` int(11) DEFAULT '1024',
                                    `dynamic_initial_executors` int(11) DEFAULT '1',
                                    `dynamic_min_executors` int(11) DEFAULT '1',
                                    `dynamic_max_executors` int(11) DEFAULT '1',
                                    `secret` varchar(255) COLLATE latin1_general_cs NOT NULL,
                                    `log_level` varchar(32) COLLATE latin1_general_cs DEFAULT 'INFO',
                                    `mode` varchar(32) COLLATE latin1_general_cs NOT NULL,
                                    `umask` varchar(32) COLLATE latin1_general_cs DEFAULT '022',
                                    `advanced` tinyint(1) DEFAULT '0',
                                    `archives` varchar(1500) COLLATE latin1_general_cs DEFAULT '',
                                    `jars` varchar(1500) COLLATE latin1_general_cs DEFAULT '',
                                    `files` varchar(1500) COLLATE latin1_general_cs DEFAULT '',
                                    `py_files` varchar(1500) COLLATE latin1_general_cs DEFAULT '',
                                    `spark_params` varchar(6500) COLLATE latin1_general_cs DEFAULT '',
                                    `shutdown_level` int(11) NOT NULL DEFAULT '6',
                                    PRIMARY KEY (`project_id`,`team_member`),
                                    KEY `team_member` (`team_member`),
                                    KEY `secret_idx` (`secret`),
                                    CONSTRAINT `RS_FK_USERS` FOREIGN KEY (`team_member`) REFERENCES `users` (`email`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                    CONSTRAINT `RS_FK_PROJS` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET character_set_client = utf8 */;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `serving`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `serving` (
                           `id` int(11) NOT NULL AUTO_INCREMENT,
                           `local_port` int(11) DEFAULT NULL,
                           `cid` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
                           `project_id` int(11) NOT NULL,
                           `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           `name` varchar(255) COLLATE latin1_general_cs NOT NULL,
                           `description` varchar(1000) COLLATE latin1_general_cs DEFAULT NULL,
                           `model_path` varchar(255) COLLATE latin1_general_cs NOT NULL,
                           `artifact_version` int(11) DEFAULT NULL,
                           `predictor` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
                           `transformer` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
                           `model_name` varchar(255) COLLATE latin1_general_cs NOT NULL,
                           `model_version` int(11) NOT NULL,
                           `model_framework` int(11) NOT NULL,
                           `local_dir` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
                           `batching_configuration` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
                           `optimized` tinyint(4) NOT NULL DEFAULT '0',
                           `instances` int(11) NOT NULL DEFAULT '0',
                           `transformer_instances` int(11) DEFAULT NULL,
                           `creator` int(11) DEFAULT NULL,
                           `lock_ip` varchar(15) COLLATE latin1_general_cs DEFAULT NULL,
                           `lock_timestamp` bigint(20) DEFAULT NULL,
                           `kafka_topic_id` int(11) DEFAULT NULL,
                           `inference_logging` int(11) DEFAULT NULL,
                           `model_server` int(11) NOT NULL DEFAULT '0',
                           `serving_tool` int(11) NOT NULL DEFAULT '0',
                           `deployed` timestamp DEFAULT NULL,
                           `revision` varchar(8) DEFAULT NULL,
                           `predictor_resources` varchar(1000) COLLATE latin1_general_cs DEFAULT NULL,
                           `transformer_resources` varchar(1000) COLLATE latin1_general_cs DEFAULT NULL,
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `Serving_Constraint` (`project_id`,`name`),
                           KEY `user_fk` (`creator`),
                           KEY `kafka_fk` (`kafka_topic_id`),
                           KEY `name_k` (`name`),
                           CONSTRAINT `user_fk_serving` FOREIGN KEY (`creator`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION,
                           CONSTRAINT `kafka_fk` FOREIGN KEY (`kafka_topic_id`) REFERENCES `project_topics` (`id`) ON DELETE SET NULL ON UPDATE NO ACTION,
                           CONSTRAINT `FK_284_315` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_commands`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `system_commands` (
                                   `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                   `host_id` int(11) NOT NULL,
                                   `op` varchar(50) COLLATE latin1_general_cs NOT NULL,
                                   `status` varchar(20) COLLATE latin1_general_cs NOT NULL,
                                   `priority` int(11) NOT NULL DEFAULT '0',
                                   `exec_user` varchar(50) COLLATE latin1_general_cs DEFAULT NULL,
                                   PRIMARY KEY (`id`),
                                   KEY `host_id` (`host_id`),
                                   CONSTRAINT `FK_481_349` FOREIGN KEY (`host_id`) REFERENCES `hosts` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=3816 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_commands_args`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `system_commands_args` (
                                        `id` int(11) NOT NULL AUTO_INCREMENT,
                                        `command_id` bigint(20) NOT NULL,
                                        `arguments` varchar(13900) COLLATE latin1_general_cs DEFAULT '',
                                        PRIMARY KEY (`id`),
                                        KEY `command_id_idx` (`command_id`),
                                        CONSTRAINT `command_id_fk` FOREIGN KEY (`command_id`) REFERENCES `system_commands` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=3816 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tensorboard`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tensorboard` (
                               `project_id` int(11) NOT NULL,
                               `user_id` int(11) NOT NULL,
                               `endpoint` varchar(100) COLLATE latin1_general_cs NOT NULL,
                               `ml_id` varchar(100) COLLATE latin1_general_cs NOT NULL,
                               `cid` varchar(255) COLLATE latin1_general_cs NOT NULL,
                               `last_accessed` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               `hdfs_logdir` varchar(10000) COLLATE latin1_general_cs NOT NULL,
                               `secret` varchar(255) COLLATE latin1_general_cs NOT NULL,
                               PRIMARY KEY (`project_id`,`user_id`),
                               KEY `user_id_fk` (`user_id`),
                               CONSTRAINT `user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION,
                               CONSTRAINT `project_id_fk` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;


CREATE TABLE `feature_view` (
                                 `id` int(11) NOT NULL AUTO_INCREMENT,
                                 `name` varchar(63) NOT NULL,
                                 `feature_store_id` int(11) NOT NULL,
                                 `created` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                                 `creator` int(11) NOT NULL,
                                 `version` int(11) NOT NULL,
                                 `description` varchar(10000) COLLATE latin1_general_cs DEFAULT NULL,
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `name_version` (`feature_store_id`, `name`, `version`),
                                 KEY `feature_store_id` (`feature_store_id`),
                                 KEY `creator` (`creator`),
                                 CONSTRAINT `fv_creator_fk` FOREIGN KEY (`creator`) REFERENCES `users` (`uid`) ON
                                     DELETE NO ACTION ON UPDATE NO ACTION,
                                 CONSTRAINT `fv_feature_store_id_fk` FOREIGN KEY (`feature_store_id`) REFERENCES
                                     `feature_store` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=9 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

--
-- Table structure for table `training_dataset`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `training_dataset` (
                                    `id` int(11) NOT NULL AUTO_INCREMENT,
                                    `name` varchar(63) NOT NULL,
                                    `feature_store_id` int(11) NOT NULL,
                                    `created` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                                    `creator` int(11) NOT NULL,
                                    `version` int(11) NOT NULL,
                                    `data_format` varchar(128) COLLATE latin1_general_cs NOT NULL,
                                    `description` varchar(10000) COLLATE latin1_general_cs DEFAULT NULL,
                                    `hopsfs_training_dataset_id` INT(11) NULL,
                                    `external_training_dataset_id` INT(11) NULL,
                                    `training_dataset_type`   INT(11) NOT NULL DEFAULT '0',
                                    `seed` BIGINT(11) NULL,
                                    `query` TINYINT(1) NOT NULL DEFAULT '0',
                                    `coalesce` TINYINT(1) NOT NULL DEFAULT '0',
                                    `train_split` VARCHAR(63) COLLATE latin1_general_cs DEFAULT NULL,
                                    `feature_view_id` INT(11) NULL,
                                    `start_time` TIMESTAMP NULL,
                                    `end_time` TIMESTAMP NULL,
                                    `sample_ratio` FLOAT NULL,
                                    `connector_id` INT(11) NULL,
                                    `connector_path` VARCHAR(1000) NULL,
                                    `tag_path` VARCHAR(1000) NULL,
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `name_version` (`feature_store_id`, `feature_view_id`, `name`, `version`),
                                    KEY `feature_store_id` (`feature_store_id`),
                                    KEY `creator` (`creator`),
                                    CONSTRAINT `td_feature_view_fk` FOREIGN KEY  (`feature_view_id`) REFERENCES
                                        `feature_view` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                    CONSTRAINT `FK_1012_877` FOREIGN KEY (`creator`) REFERENCES `users` (`uid`) ON DELETE NO ACTION ON UPDATE NO ACTION,
                                    CONSTRAINT `FK_656_817` FOREIGN KEY (`feature_store_id`) REFERENCES `feature_store` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                    CONSTRAINT `td_conn_fk` FOREIGN KEY (`connector_id`) REFERENCES `hopsworks`.`feature_store_connector` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=9 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `training_dataset_feature`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `training_dataset_feature` (
                                            `id` int(11) NOT NULL AUTO_INCREMENT,
                                            `training_dataset` int(11) NULL,
                                            `feature_group` int(11) NULL,
                                            `name` varchar(1000) COLLATE latin1_general_cs NOT NULL,
                                            `type` varchar(1000) COLLATE latin1_general_cs,
                                            `td_join`int(11) NULL,
                                            `idx` int(11) NULL,
                                            `label` tinyint(1) NOT NULL DEFAULT '0',
                                            `inference_helper_column` tinyint(1) NOT NULL DEFAULT '0',
                                            `training_helper_column` tinyint(1) NOT NULL DEFAULT '0',
                                            `transformation_function`  int(11) NULL,
                                            `feature_view_id` INT(11) NULL,
                                            PRIMARY KEY (`id`),
                                            KEY `td_key` (`training_dataset`),
                                            KEY `fg_key` (`feature_group`),
                                            CONSTRAINT `tdf_feature_view_fk` FOREIGN KEY  (`feature_view_id`)
                                                REFERENCES `feature_view` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                            CONSTRAINT `join_fk_tdf` FOREIGN KEY (`td_join`) REFERENCES `training_dataset_join` (`id`) ON DELETE SET NULL ON UPDATE NO ACTION,
                                            CONSTRAINT `td_fk_tdf` FOREIGN KEY (`training_dataset`) REFERENCES `training_dataset` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                            CONSTRAINT `fg_fk_tdf` FOREIGN KEY (`feature_group`) REFERENCES `feature_group` (`id`) ON DELETE SET NULL ON UPDATE NO ACTION,
                                            CONSTRAINT `tfn_fk_tdf` FOREIGN KEY (`transformation_function`) REFERENCES `transformation_function` (`id`) ON DELETE SET NULL ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `training_dataset_join` (
                                         `id` int(11) NOT NULL AUTO_INCREMENT,
                                         `training_dataset` int(11) NULL,
                                         `feature_group` int(11) NULL,
                                         `feature_group_commit_id` BIGINT(20) NULL,
                                         `type` tinyint(5) NOT NULL DEFAULT 0,
                                         `idx` int(11) NOT NULL DEFAULT 0,
                                         `prefix` VARCHAR(63) NULL,
                                         `feature_view_id` INT(11) NULL,
                                         PRIMARY KEY (`id`),
                                         KEY `fg_key` (`feature_group`),
                                         CONSTRAINT `tdj_feature_view_fk` FOREIGN KEY  (`feature_view_id`) REFERENCES
                                             `feature_view` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                         CONSTRAINT `td_fk_tdj` FOREIGN KEY (`training_dataset`) REFERENCES `training_dataset` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                         CONSTRAINT `fg_left` FOREIGN KEY (`feature_group`) REFERENCES `feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `training_dataset_join_condition` (
                                                   `id` int(11) NOT NULL AUTO_INCREMENT,
                                                   `td_join` int(11) NOT NULL,
                                                   `left_feature` VARCHAR(1000) NOT NULL DEFAULT "",
                                                   `right_feature` VARCHAR(1000) NOT NULL DEFAULT "",
                                                   PRIMARY KEY (`id`),
                                                   KEY `join_key` (`td_join`),
                                                   CONSTRAINT `join_fk_tdjc` FOREIGN KEY (`td_join`) REFERENCES `training_dataset_join` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `on_demand_feature`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `on_demand_feature` (
                                     `id` int(11) NOT NULL AUTO_INCREMENT,
                                     `on_demand_feature_group_id` int(11) NULL,
                                     `name` varchar(1000) COLLATE latin1_general_cs NOT NULL,
                                     `primary_column` tinyint(1) NOT NULL DEFAULT '0',
                                     `description` varchar(10000) COLLATE latin1_general_cs,
                                     `type` varchar(1000) COLLATE latin1_general_cs NOT NULL,
                                     `idx` int(11) NOT NULL DEFAULT 0,
                                     `default_value` VARCHAR(400) NULL,
                                     PRIMARY KEY (`id`),
                                     KEY `on_demand_feature_group_fk` (`on_demand_feature_group_id`),
                                     CONSTRAINT `on_demand_feature_group_fk1` FOREIGN KEY (`on_demand_feature_group_id`) REFERENCES `on_demand_feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `on_demand_option`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `on_demand_option` (
                                    `id` int(11) NOT NULL AUTO_INCREMENT,
                                    `on_demand_feature_group_id` int(11) NULL,
                                    `name` varchar(255) COLLATE latin1_general_cs NOT NULL,
                                    `value` varchar(255) COLLATE latin1_general_cs NOT NULL,
                                    PRIMARY KEY (`id`),
                                    KEY `on_demand_option_key` (`on_demand_feature_group_id`),
                                    CONSTRAINT `on_demand_option_fk` FOREIGN KEY (`on_demand_feature_group_id`) REFERENCES `on_demand_feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;



--
-- Table structure for table `training_dataset_split`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `training_dataset_split` (
                                                        `id` int(11) NOT NULL AUTO_INCREMENT,
                                                        `training_dataset_id` int(11) NOT NULL,
                                                        `name` varchar(63) COLLATE latin1_general_cs NOT NULL,
                                                        `percentage` float NULL,
                                                        `split_type` VARCHAR(40) NULL,
                                                        `start_time` TIMESTAMP NULL,
                                                        `end_Time` TIMESTAMP NULL,
                                                        PRIMARY KEY (`id`),
                                                        KEY `training_dataset_id` (`training_dataset_id`),
                                                        UNIQUE KEY `dataset_id_split_name` (`training_dataset_id`, `name`),
                                                        CONSTRAINT `training_dataset_fk` FOREIGN KEY (`training_dataset_id`) REFERENCES `training_dataset` (`id`) ON DELETE
                                                            CASCADE ON UPDATE NO ACTION

) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_certs`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_certs` (
                              `projectname` varchar(100) COLLATE latin1_general_cs NOT NULL,
                              `username` varchar(10) COLLATE latin1_general_cs NOT NULL,
                              `user_key` varbinary(7000) DEFAULT NULL,
                              `user_cert` varbinary(3000) DEFAULT NULL,
                              `user_key_pwd` varchar(200) COLLATE latin1_general_cs DEFAULT NULL,
                              PRIMARY KEY (`projectname`,`username`),
                              KEY `username` (`username`),
                              CONSTRAINT `FK_260_465` FOREIGN KEY (`username`) REFERENCES `users` (`username`) ON DELETE CASCADE ON UPDATE NO ACTION,
                              CONSTRAINT `FK_287_464` FOREIGN KEY (`projectname`) REFERENCES `project` (`projectname`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_group`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_group` (
                              `uid` int(11) NOT NULL,
                              `gid` int(11) NOT NULL,
                              PRIMARY KEY (`uid`,`gid`),
                              KEY `gid` (`gid`),
                              CONSTRAINT `FK_257_268` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION,
                              CONSTRAINT `FK_255_269` FOREIGN KEY (`gid`) REFERENCES `bbc_group` (`gid`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userlogins`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userlogins` (
                              `login_id` bigint(20) NOT NULL AUTO_INCREMENT,
                              `ip` varchar(45) COLLATE latin1_general_cs DEFAULT NULL,
                              `useragent` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
                              `action` varchar(80) COLLATE latin1_general_cs DEFAULT NULL,
                              `outcome` varchar(20) COLLATE latin1_general_cs DEFAULT NULL,
                              `uid` int(11) NOT NULL,
                              `login_date` timestamp NULL DEFAULT NULL,
                              PRIMARY KEY (`login_id`),
                              KEY `login_date` (`login_date`),
                              KEY `uid` (`uid`),
                              CONSTRAINT `FK_257_345` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=316 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
                         `uid` int(11) NOT NULL AUTO_INCREMENT,
                         `username` varchar(10) COLLATE latin1_general_cs NOT NULL,
                         `password` varchar(128) COLLATE latin1_general_cs NOT NULL,
                         `email` varchar(150) COLLATE latin1_general_cs DEFAULT NULL,
                         `fname` varchar(30) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
                         `lname` varchar(30) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
                         `activated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         `title` varchar(10) COLLATE latin1_general_cs DEFAULT '-',
                         `false_login` int(11) NOT NULL DEFAULT '-1',
                         `status` int(11) NOT NULL DEFAULT '-1',
                         `isonline` int(11) NOT NULL DEFAULT '-1',
                         `secret` varchar(20) COLLATE latin1_general_cs DEFAULT NULL,
                         `validation_key` varchar(128) COLLATE latin1_general_cs DEFAULT NULL,
                         `validation_key_updated` timestamp DEFAULT NULL,
                         `validation_key_type` VARCHAR(20) DEFAULT NULL,
                         `mode` int(11) NOT NULL DEFAULT '0',
                         `password_changed` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         `notes` varchar(500) COLLATE latin1_general_cs DEFAULT '-',
                         `max_num_projects` int(11) NOT NULL,
                         `num_active_projects` int(11) NOT NULL DEFAULT '0',
                         `two_factor` tinyint(1) NOT NULL DEFAULT '1',
                         `tours_state` tinyint(1) NOT NULL DEFAULT '0',
                         `salt` varchar(128) COLLATE latin1_general_cs NOT NULL DEFAULT '',
                         PRIMARY KEY (`uid`),
                         UNIQUE KEY `username` (`username`),
                         UNIQUE KEY `email` (`email`)
) ENGINE=ndbcluster AUTO_INCREMENT=10178 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary table structure for view `users_groups`
--

SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE VIEW `users_groups` AS SELECT
                                           1 AS `username`,
                                           1 AS `password`,
                                           1 AS `secret`,
                                           1 AS `email`,
                                           1 AS `group_name`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `variables`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `variables` (
                             `id` varchar(255) COLLATE latin1_general_cs NOT NULL,
                             `value` varchar(1024) COLLATE latin1_general_cs NOT NULL,
                             `visibility` TINYINT NOT NULL DEFAULT 0,
                             `hide` TINYINT NOT NULL DEFAULT 0,
                             PRIMARY KEY (`id`)
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Final view structure for view `users_groups`
--

/*!50001 DROP VIEW IF EXISTS `users_groups`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
    /*!50001 VIEW `users_groups` AS select `u`.`username` AS `username`,`u`.`password` AS `password`,`u`.`secret` AS `secret`,`u`.`email` AS `email`,`g`.`group_name` AS `group_name` from ((`user_group` `ug` join `users` `u` on((`u`.`uid` = `ug`.`uid`))) join `bbc_group` `g` on((`g`.`gid` = `ug`.`gid`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2019-06-14  8:07:38

CREATE TABLE IF NOT EXISTS `secrets` (
                                         `uid` INT NOT NULL,
                                         `secret_name` VARCHAR(200) NOT NULL,
                                         `secret` VARBINARY(10000) NOT NULL,
                                         `added_on` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         `visibility` TINYINT NOT NULL,
                                         `pid_scope` INT DEFAULT NULL,
                                         PRIMARY KEY (`uid`, `secret_name`),
                                         FOREIGN KEY `secret_uid` (`uid`) REFERENCES `users` (`uid`)
                                             ON DELETE CASCADE
                                             ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `api_key` (
                                         `id` int(11) NOT NULL AUTO_INCREMENT,
                                         `prefix` varchar(45) NOT NULL,
                                         `secret` varchar(512) NOT NULL,
                                         `salt` varchar(256) NOT NULL,
                                         `created` timestamp NOT NULL,
                                         `modified` timestamp NOT NULL,
                                         `name` varchar(45) NOT NULL,
                                         `user_id` int(11) NOT NULL,
                                         `reserved` tinyint(1) DEFAULT '0',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `prefix_UNIQUE` (`prefix`),
                                         UNIQUE KEY `index4` (`user_id`,`name`),
                                         KEY `fk_api_key_1_idx` (`user_id`),
                                         CONSTRAINT `fk_api_key_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION)
    ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `api_key_scope` (
                                               `id` int(11) NOT NULL AUTO_INCREMENT,
                                               `api_key` int(11) NOT NULL,
                                               `scope` varchar(45) NOT NULL,
                                               PRIMARY KEY (`id`),
                                               UNIQUE KEY `index2` (`api_key`,`scope`),
                                               CONSTRAINT `fk_api_key_scope_1` FOREIGN KEY (`api_key`) REFERENCES `api_key` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION)
    ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `feature_store_jdbc_connector` (
                                                              `id`                      INT(11)          NOT NULL AUTO_INCREMENT,
                                                              `connection_string`       VARCHAR(5000)    NOT NULL,
                                                              `arguments`               VARCHAR(2000)    NULL,
                                                              PRIMARY KEY (`id`)
) ENGINE = ndbcluster DEFAULT CHARSET = latin1 COLLATE = latin1_general_cs;

CREATE TABLE IF NOT EXISTS `feature_store_s3_connector` (
                                                            `id`                                  INT(11)         NOT NULL AUTO_INCREMENT,
                                                            `bucket`                              VARCHAR(5000)   NOT NULL,
                                                            `iam_role`                            VARCHAR(2048)   DEFAULT NULL,
                                                            `server_encryption_algorithm`         INT(11)         NULL,
                                                            `server_encryption_key`               VARCHAR(1000)   NULL,
                                                            `key_secret_uid`                      INT             DEFAULT NULL,
                                                            `key_secret_name`                     VARCHAR(200)    DEFAULT NULL,
                                                            `arguments`                            VARCHAR(2000)  DEFAULT NULL,
                                                            PRIMARY KEY (`id`),
                                                            KEY `fk_feature_store_s3_connector_1_idx` (`key_secret_uid`, `key_secret_name`),
                                                            CONSTRAINT `fk_feature_store_s3_connector_1` FOREIGN KEY (`key_secret_uid` , `key_secret_name`)
                                                                REFERENCES `hopsworks`.`secrets` (`uid` , `secret_name`) ON DELETE RESTRICT
) ENGINE = ndbcluster DEFAULT CHARSET = latin1 COLLATE = latin1_general_cs;

CREATE TABLE IF NOT EXISTS `feature_store_hopsfs_connector` (
                                                                `id`                      INT(11)         NOT NULL AUTO_INCREMENT,
                                                                `hopsfs_dataset`          INT(11)         NOT NULL,
                                                                PRIMARY KEY (`id`),
                                                                CONSTRAINT `hopsfs_connector_dataset_fk` FOREIGN KEY (`hopsfs_dataset`)
                                                                    REFERENCES `hopsworks`.`dataset` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE = ndbcluster DEFAULT CHARSET = latin1 COLLATE = latin1_general_cs;

CREATE TABLE `feature_store_redshift_connector` (
                                                    `id` int NOT NULL AUTO_INCREMENT,
                                                    `cluster_identifier` varchar(64) NOT NULL,
                                                    `database_driver` varchar(64) NOT NULL,
                                                    `database_endpoint` varchar(128) DEFAULT NULL,
                                                    `database_name` varchar(64) DEFAULT NULL,
                                                    `database_port` int DEFAULT NULL,
                                                    `table_name` varchar(128) DEFAULT NULL,
                                                    `database_user_name` varchar(128) DEFAULT NULL,
                                                    `auto_create` tinyint(1) DEFAULT 0,
                                                    `database_group` varchar(2048) DEFAULT NULL,
                                                    `iam_role` varchar(2048) DEFAULT NULL,
                                                    `arguments` varchar(2000) DEFAULT NULL,
                                                    `database_pwd_secret_uid` int DEFAULT NULL,
                                                    `database_pwd_secret_name` varchar(200) DEFAULT NULL,
                                                    PRIMARY KEY (`id`),
                                                    KEY `fk_feature_store_redshift_connector_2_idx` (`database_pwd_secret_uid`,`database_pwd_secret_name`),
                                                    CONSTRAINT `fk_feature_store_redshift_connector_2` FOREIGN KEY (`database_pwd_secret_uid`, `database_pwd_secret_name`)
                                                        REFERENCES `hopsworks`.`secrets` (`uid`, `secret_name`) ON DELETE RESTRICT
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `feature_store_adls_connector` (
                                                              `id`                    INT(11)         NOT NULL AUTO_INCREMENT,
                                                              `generation`            INT(11)       NOT NULL,
                                                              `directory_id`          VARCHAR(40)   NOT NULL,
                                                              `application_id`        VARCHAR(40)   NOT NULL,
                                                              `account_name`          VARCHAR(30)   NOT NULL,
                                                              `container_name`        VARCHAR(65),
                                                              `cred_secret_uid`       INT           NOT NULL,
                                                              `cred_secret_name`      VARCHAR(200)  NOT NULL,
                                                              PRIMARY KEY (`id`),
                                                              CONSTRAINT `adls_cred_secret_fk` FOREIGN KEY (`cred_secret_uid` , `cred_secret_name`)
                                                                  REFERENCES `hopsworks`.`secrets` (`uid` , `secret_name`) ON DELETE RESTRICT
) ENGINE = ndbcluster DEFAULT CHARSET = latin1 COLLATE = latin1_general_cs;

CREATE TABLE IF NOT EXISTS `feature_store_snowflake_connector` (
                                                                   `id`                       INT(11)       NOT NULL AUTO_INCREMENT,
                                                                   `url`                      VARCHAR(3000) NOT NULL,
                                                                   `database_user`            VARCHAR(128)  NOT NULL,
                                                                   `database_name`            VARCHAR(64)   NOT NULL,
                                                                   `database_schema`          VARCHAR(45)   NOT NULL,
                                                                   `table_name`               VARCHAR(128)  DEFAULT NULL,
                                                                   `role`                     VARCHAR(65)   DEFAULT NULL,
                                                                   `warehouse`                VARCHAR(128)  DEFAULT NULL,
                                                                   `arguments`                VARCHAR(8000) DEFAULT NULL,
                                                                   `application`              VARCHAR(50) DEFAULT NULL,
                                                                   `database_pwd_secret_uid`  INT DEFAULT NULL,
                                                                   `database_pwd_secret_name` VARCHAR(200) DEFAULT NULL,
                                                                   `oauth_token_secret_uid`   INT DEFAULT NULL,
                                                                   `oauth_token_secret_name`  VARCHAR(200) DEFAULT NULL,
                                                                   PRIMARY KEY (`id`),
                                                                   KEY `fk_feature_store_snowflake_connector_2_idx` (`database_pwd_secret_uid`,`database_pwd_secret_name`),
                                                                   CONSTRAINT `fk_feature_store_snowflake_connector_2` FOREIGN KEY (`database_pwd_secret_uid`, `database_pwd_secret_name`)
                                                                       REFERENCES `hopsworks`.`secrets` (`uid`, `secret_name`) ON DELETE RESTRICT,
                                                                   KEY `fk_feature_store_snowflake_connector_3_idx` (`oauth_token_secret_uid`,`oauth_token_secret_name`),
                                                                   CONSTRAINT `fk_feature_store_snowflake_connector_3` FOREIGN KEY (`oauth_token_secret_uid`, `oauth_token_secret_name`)
                                                                       REFERENCES `hopsworks`.`secrets` (`uid`, `secret_name`) ON DELETE RESTRICT
) ENGINE = ndbcluster DEFAULT CHARSET = latin1 COLLATE = latin1_general_cs;

CREATE TABLE IF NOT EXISTS `feature_store_kafka_connector` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `bootstrap_servers` VARCHAR(1000) NOT NULL,
    `security_protocol` VARCHAR(1000) NOT NULL,
    `ssl_secret_uid` INT NULL,
    `ssl_secret_name` VARCHAR(200) NULL,
    `ssl_endpoint_identification_algorithm` VARCHAR(100) NULL,
    `options` VARCHAR(2000) NULL,
    `truststore_path` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
    `keystore_path` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `fk_fs_storage_connector_kafka_idx` (`ssl_secret_uid`, `ssl_secret_name`),
    CONSTRAINT `fk_fs_storage_connector_kafka` FOREIGN KEY (`ssl_secret_uid`, `ssl_secret_name`) REFERENCES `hopsworks`.`secrets` (`uid`, `secret_name`) ON DELETE RESTRICT
) ENGINE = ndbcluster DEFAULT CHARSET = latin1 COLLATE = latin1_general_cs;


CREATE TABLE IF NOT EXISTS `feature_store_gcs_connector` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `algorithm` VARCHAR(10) NULL,
    `bucket` VARCHAR(1000) NOT NULL,
    `encryption_secret_uid` INT NULL,
    `encryption_secret_name` VARCHAR(200) NULL,
    `key_path` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `fk_fs_storage_connector_gcs_idx` (`encryption_secret_uid`, `encryption_secret_name`),
    CONSTRAINT `fk_fs_storage_connector_gcs` FOREIGN KEY (`encryption_secret_uid`, `encryption_secret_name`) REFERENCES `hopsworks`.`secrets` (`uid`, `secret_name`) ON DELETE RESTRICT
) ENGINE = ndbcluster DEFAULT CHARSET = latin1 COLLATE = latin1_general_cs;


-- Add bigquery connector
CREATE TABLE IF NOT EXISTS `feature_store_bigquery_connector`
(
    `id`                      int AUTO_INCREMENT,
    `parent_project`          varchar(1000) NOT NULL,
    `dataset`                 varchar(1000) NULL,
    `query_table`             varchar(1000) NULL,
    `query_project`           varchar(1000) NULL,
    `materialization_dataset` varchar(1000) NULL,
    `arguments`               varchar(2000) NULL,
    `key_path` varchar(255) COLLATE latin1_general_cs DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = ndbcluster DEFAULT CHARSET = latin1 COLLATE = latin1_general_cs;

CREATE TABLE IF NOT EXISTS `feature_store_connector` (
                                                         `id`                      INT(11)          NOT NULL AUTO_INCREMENT,
                                                         `feature_store_id`        INT(11)          NOT NULL,
                                                         `name`                    VARCHAR(150)     NOT NULL,
                                                         `description`             VARCHAR(1000)    NULL,
                                                         `type`                    INT(11)          NOT NULL,
                                                         `jdbc_id`                 INT(11),
                                                         `s3_id`                   INT(11),
                                                         `hopsfs_id`               INT(11),
                                                         `redshift_id`             INT(11),
                                                         `adls_id`                 INT(11),
                                                         `snowflake_id`            INT(11),
                                                         `kafka_id`                INT(11),
                                                         `gcs_id`                  INT(11),
                                                          `bigquery_id`            INT(11),
                                                         PRIMARY KEY (`id`),
                                                         UNIQUE KEY `fs_conn_name` (`name`, `feature_store_id`),
                                                         CONSTRAINT `fs_connector_featurestore_fk` FOREIGN KEY (`feature_store_id`) REFERENCES `hopsworks`.`feature_store` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                                         CONSTRAINT `fs_connector_jdbc_fk` FOREIGN KEY (`jdbc_id`) REFERENCES `hopsworks`.`feature_store_jdbc_connector` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                                         CONSTRAINT `fs_connector_s3_fk` FOREIGN KEY (`s3_id`) REFERENCES `hopsworks`.`feature_store_s3_connector` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                                         CONSTRAINT `fs_connector_hopsfs_fk` FOREIGN KEY (`hopsfs_id`) REFERENCES `hopsworks`.`feature_store_hopsfs_connector` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                                         CONSTRAINT `fs_connector_redshift_fk` FOREIGN KEY (`redshift_id`) REFERENCES `hopsworks`.`feature_store_redshift_connector` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                                         CONSTRAINT `fs_connector_adls_fk` FOREIGN KEY (`adls_id`) REFERENCES `hopsworks`.`feature_store_adls_connector` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                                         CONSTRAINT `fs_connector_snowflake_fk` FOREIGN KEY (`snowflake_id`) REFERENCES `hopsworks`.`feature_store_snowflake_connector` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                                         CONSTRAINT `fs_connector_kafka_fk` FOREIGN KEY (`kafka_id`) REFERENCES `hopsworks`.`feature_store_kafka_connector` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                                         CONSTRAINT `fs_connector_gcs_fk` FOREIGN KEY (`gcs_id`) REFERENCES `hopsworks`.`feature_store_gcs_connector` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                                        CONSTRAINT `fs_connector_bigquery_fk` FOREIGN KEY (`bigquery_id`) REFERENCES `hopsworks`.`feature_store_bigquery_connector` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE = ndbcluster DEFAULT CHARSET = latin1 COLLATE = latin1_general_cs;

CREATE TABLE IF NOT EXISTS `on_demand_feature_group` (
                                                         `id`                      INT(11)         NOT NULL AUTO_INCREMENT,
                                                         `query`                   VARCHAR(26000),
                                                         `connector_id`            INT(11)         NULL,
                                                         `data_format`             VARCHAR(10),
                                                         `path`                    VARCHAR(1000),
                                                         `spine`                  TINYINT(1) NOT NULL DEFAULT 0,
                                                         PRIMARY KEY (`id`),
                                                         CONSTRAINT `on_demand_conn_fk` FOREIGN KEY (`connector_id`) REFERENCES `hopsworks`.`feature_store_connector` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE = ndbcluster DEFAULT CHARSET = latin1 COLLATE = latin1_general_cs;


CREATE TABLE IF NOT EXISTS `cached_feature_group` (
                                                      `id`                             INT(11)         NOT NULL AUTO_INCREMENT,
                                                      `timetravel_format`              INT NOT NULL DEFAULT 1,
                                                      PRIMARY KEY (`id`)
) ENGINE = ndbcluster DEFAULT CHARSET = latin1 COLLATE = latin1_general_cs;

--
-- Table structure for table `stream_feature_group`
--
CREATE TABLE IF NOT EXISTS `stream_feature_group` (
                                                      `id`                             INT(11) NOT NULL AUTO_INCREMENT,
                                                      `timetravel_format`              INT NOT NULL DEFAULT 1,
                                                      PRIMARY KEY (`id`)
)
ENGINE = ndbcluster DEFAULT CHARSET = latin1 COLLATE = latin1_general_cs;

--
-- Table structure for table `cached_feature`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cached_feature` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cached_feature_group_id` int(11) NULL,
  `stream_feature_group_id` int(11) NULL,
  `name` varchar(63) COLLATE latin1_general_cs NOT NULL,
  `description` varchar(256) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `cached_feature_group_fk` (`cached_feature_group_id`),
  KEY `stream_feature_group_fk` (`stream_feature_group_id`),
  CONSTRAINT `cached_feature_group_fk2` FOREIGN KEY (`cached_feature_group_id`) REFERENCES `cached_feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `stream_feature_group_fk2` FOREIGN KEY (`stream_feature_group_id`) REFERENCES `stream_feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

CREATE TABLE `feature_store_job` (
                                     `id` int(11) NOT NULL AUTO_INCREMENT,
                                     `job_id` int(11) NOT NULL,
                                     `training_dataset_id` int(11) DEFAULT NULL,
                                     `feature_group_id` INT(11) DEFAULT NULL,
                                     PRIMARY KEY (`id`),
                                     CONSTRAINT `fs_job_job_fk` FOREIGN KEY (`job_id`) REFERENCES `jobs` (`id`)
                                         ON DELETE CASCADE
                                         ON UPDATE NO ACTION,
                                     CONSTRAINT `fs_job_td_fk` FOREIGN KEY (`training_dataset_id`) REFERENCES `training_dataset` (`id`)
                                         ON DELETE CASCADE
                                         ON UPDATE NO ACTION,
                                     CONSTRAINT `fs_job_fg_fk` FOREIGN KEY (`feature_group_id`) REFERENCES `feature_group` (`id`)
                                         ON DELETE CASCADE
                                         ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `feature_store_tag` (
                                                   `id` int(11) NOT NULL AUTO_INCREMENT,
                                                   `name` varchar(255) NOT NULL,
                                                   `tag_schema` varchar(13000) NOT NULL DEFAULT '{"type":"string"}',
                                                   PRIMARY KEY (`id`),
                                                   UNIQUE KEY `name_UNIQUE` (`name`)
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE `remote_group_project_mapping` (
                                                `id` int(11) NOT NULL AUTO_INCREMENT,
                                                `remote_group` varchar(256) NOT NULL,
                                                `project` int(11) NOT NULL,
                                                `project_role` varchar(32) NOT NULL,
                                                PRIMARY KEY (`id`),
                                                UNIQUE KEY `index3` (`remote_group`,`project`),
                                                KEY `fk_remote_group_project_mapping_1_idx` (`project`),
                                                CONSTRAINT `fk_remote_group_project_mapping_1` FOREIGN KEY (`project`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE `feature_group_commit` (
                                        `feature_group_id` int(11) NOT NULL, -- from hudi dataset name -> lookup feature_group
                                        `commit_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
                                        `committed_on` TIMESTAMP(6) NOT NULL,
                                        `num_rows_updated` int(11) DEFAULT '0',
                                        `num_rows_inserted` int(11) DEFAULT '0',
                                        `num_rows_deleted` int(11) DEFAULT '0',
                                        `archived` TINYINT(1) NOT NULL DEFAULT '0',
                                        PRIMARY KEY (`feature_group_id`, `commit_id`),
                                        KEY `commit_id_idx` (`commit_id`),
                                        KEY `commit_date_idx` (`committed_on`),
                                        CONSTRAINT `feature_group_fk` FOREIGN KEY (`feature_group_id`) REFERENCES `feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE `cloud_role_mapping` (
                                      `id` int(11) NOT NULL AUTO_INCREMENT,
                                      `project_id` int(11) NOT NULL,
                                      `project_role` varchar(32) NOT NULL,
                                      `cloud_role` varchar(2048) NOT NULL,
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `index3` (`project_id`,`cloud_role`),
                                      UNIQUE KEY `index4` (`id`,`project_id`,`project_role`),
                                      KEY `fk_cloud_role_mapping_1_idx` (`project_id`),
                                      CONSTRAINT `fk_cloud_role_mapping_1`
                                          FOREIGN KEY (`project_id`)
                                              REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE `cloud_role_mapping_default` (
                                              `id` int(11) NOT NULL AUTO_INCREMENT,
                                              `mapping_id` int(11) NOT NULL,
                                              `project_id` int(11) NOT NULL,
                                              `project_role` varchar(32) NOT NULL,
                                              PRIMARY KEY (`id`),
                                              UNIQUE KEY `index3` (`project_id`,`project_role`),
                                              UNIQUE KEY `index4` (`mapping_id`,`project_id`,`project_role`),
                                              KEY `fk_cloud_role_mapping_default_1_idx` (`mapping_id`,`project_id`,`project_role`),
                                              CONSTRAINT `fk_cloud_role_mapping_default_1`
                                                  FOREIGN KEY (`mapping_id`,`project_id`,`project_role`)
                                                      REFERENCES `cloud_role_mapping` (`id`,`project_id`,`project_role`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE `databricks_instance` (
                                       `id`              INT(11) NOT NULL AUTO_INCREMENT,
                                       `url`             VARCHAR(255) NOT NULL,
                                       `uid`             INT(11) NOT NULL,
                                       `secret_name`     VARCHAR(200) NOT NULL,
                                       PRIMARY KEY (`id`),
                                       FOREIGN KEY `db_secret_fk` (`uid`, `secret_name`) REFERENCES `secrets` (`uid`, `secret_name`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                       FOREIGN KEY `db_user_fk` (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE `cached_feature_extra_constraints` (
        `id` int(11) NOT NULL AUTO_INCREMENT,
        `cached_feature_group_id` int(11) NULL,
        `stream_feature_group_id` int(11) NULL,
        `name` varchar(63) COLLATE latin1_general_cs NOT NULL,
        `primary_column` tinyint(1) NOT NULL DEFAULT '0',
        `hudi_precombine_key` tinyint(1) NOT NULL DEFAULT '0',
        PRIMARY KEY (`id`),
        KEY `cached_feature_group_fk` (`cached_feature_group_id`),
        KEY `stream_feature_group_fk` (`stream_feature_group_id`),
        CONSTRAINT `stream_feature_group_constraint_fk` FOREIGN KEY (`stream_feature_group_id`) REFERENCES `stream_feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
        CONSTRAINT `cached_feature_group_constraint_fk` FOREIGN KEY (`cached_feature_group_id`) REFERENCES `cached_feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `alert_manager_config` (
                                                      `id` int(11) NOT NULL AUTO_INCREMENT,
                                                      `content` mediumblob NOT NULL,
                                                      `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                      PRIMARY KEY (`id`)
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `hopsworks`.`alert_receiver` (
                                                            `id` INT(11) NOT NULL AUTO_INCREMENT,
                                                            `name` VARCHAR(128) NOT NULL,
                                                            `config` BLOB NOT NULL,
                                                            PRIMARY KEY (`id`),
                                                            UNIQUE KEY `name_UNIQUE` (`name`)
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `job_alert` (
                                           `id` int(11) NOT NULL AUTO_INCREMENT,
                                           `job_id` int(11) NOT NULL,
                                           `status` varchar(45) NOT NULL,
                                           `type` varchar(45) NOT NULL,
                                           `severity` varchar(45) NOT NULL,
                                           `receiver` int(11) NOT NULL,
                                           `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           PRIMARY KEY (`id`),
                                           UNIQUE KEY `unique_job_alert` (`job_id`,`status`),
                                           KEY `fk_job_alert_2_idx` (`job_id`),
                                           KEY `fk_job_alert_1_idx` (`receiver`),
                                           CONSTRAINT `fk_job_alert_1` FOREIGN KEY (`receiver`) REFERENCES `alert_receiver` (`id`) ON DELETE CASCADE,
                                           CONSTRAINT `fk_job_alert_2` FOREIGN KEY (`job_id`) REFERENCES `jobs` (`id`) ON DELETE CASCADE
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `feature_group_alert` (
                                                     `id` int(11) NOT NULL AUTO_INCREMENT,
                                                     `feature_group_id` int(11) NOT NULL,
                                                     `status` varchar(45) COLLATE latin1_general_cs NOT NULL,
                                                     `type` varchar(45) COLLATE latin1_general_cs NOT NULL,
                                                     `severity` varchar(45) COLLATE latin1_general_cs NOT NULL,
                                                     `receiver` int(11) NOT NULL,
                                                     `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                     PRIMARY KEY (`id`),
                                                     UNIQUE KEY `unique_feature_group_alert` (`feature_group_id`,`status`),
                                                     KEY `fk_feature_group_alert_2_idx` (`feature_group_id`),
                                                     KEY `fk_feature_group_alert_1_idx` (`receiver`),
                                                     CONSTRAINT `fk_feature_group_alert_1` FOREIGN KEY (`receiver`) REFERENCES `alert_receiver` (`id`) ON DELETE CASCADE,
                                                     CONSTRAINT `fk_feature_group_alert_2` FOREIGN KEY (`feature_group_id`) REFERENCES `feature_group` (`id`) ON DELETE CASCADE
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `project_service_alert` (
                                                       `id` int(11) NOT NULL AUTO_INCREMENT,
                                                       `project_id` int(11) NOT NULL,
                                                       `service` VARCHAR(32) COLLATE latin1_general_cs NOT NULL,
                                                       `status` varchar(45) COLLATE latin1_general_cs NOT NULL,
                                                       `type` varchar(45) COLLATE latin1_general_cs NOT NULL,
                                                       `severity` varchar(45) COLLATE latin1_general_cs NOT NULL,
                                                       `receiver` int(11) NOT NULL,
                                                       `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                       PRIMARY KEY (`id`),
                                                       UNIQUE KEY `unique_project_service_alert` (`project_id`,`status`),
                                                       KEY `fk_project_service_2_idx` (`project_id`),
                                                       KEY `fk_project_service_alert_1_idx` (`receiver`),
                                                       CONSTRAINT `fk_project_service_alert_1` FOREIGN KEY (`receiver`) REFERENCES `alert_receiver` (`id`) ON DELETE CASCADE,
                                                       CONSTRAINT `fk_project_service_alert_2` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;


CREATE TABLE IF NOT EXISTS `transformation_function` (
                                                         `id`                                INT(11)         NOT NULL AUTO_INCREMENT,
                                                         `name`                              VARCHAR(255)    NOT NULL,
                                                         `output_type`                       varchar(32)     NOT NULL,
                                                         `version`                           INT(11)         NOT NULL,
                                                         `feature_store_id`                  INT(11)         NOT NULL,
                                                         `created` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                                                         `creator` int(11) NOT NULL,
                                                         PRIMARY KEY (`id`),
                                                         CONSTRAINT `feature_store_fn_fk` FOREIGN KEY (`feature_store_id`) REFERENCES `feature_store` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                                         CONSTRAINT `creator_fn_fk` FOREIGN KEY (`creator`) REFERENCES `users` (`uid`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE = ndbcluster DEFAULT CHARSET = latin1 COLLATE = latin1_general_cs;

CREATE TABLE IF NOT EXISTS `hopsworks`.`training_dataset_filter` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `training_dataset_id` INT(11) NULL,
    `feature_view_id` INT(11) NULL,
    `type` VARCHAR(63) NULL,
    `path` VARCHAR(63) NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `tdf_training_dataset_fk` FOREIGN KEY (`training_dataset_id`) REFERENCES `training_dataset` (`id`) ON
        DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT `tdfilter_feature_view_fk` FOREIGN KEY (`feature_view_id`) REFERENCES `feature_view` (`id`)
        ON DELETE CASCADE ON UPDATE NO ACTION
    ) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `hopsworks`.`training_dataset_filter_condition` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `training_dataset_filter_id` INT(11) NULL,
    `feature_group_id` INT(11) NULL,
    `feature_name` VARCHAR(63) NULL,
    `filter_condition` VARCHAR(128) NULL,
    `filter_value` VARCHAR(1024) NULL,
    `filter_value_fg_id` INT(11) NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `tdfc_training_dataset_filter_fk` FOREIGN KEY (`training_dataset_filter_id`) REFERENCES `training_dataset_filter` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT `tdfc_feature_group_fk` FOREIGN KEY (`feature_group_id`) REFERENCES `feature_group` (`id`) ON DELETE SET NULL ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `git_repositories` (
                                                    `id` int NOT NULL AUTO_INCREMENT,
                                                    `path` varchar(1000) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
                                                    `name` varchar(255) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
                                                    `project` int NOT NULL,
                                                    `provider` varchar(20) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
                                                    `current_branch` varchar(250) CHARACTER SET latin1 COLLATE latin1_general_cs DEFAULT NULL,
                                                    `current_commit` varchar(255) CHARACTER SET latin1 COLLATE latin1_general_cs DEFAULT NULL,
                                                    `cid` varchar(255) CHARACTER SET latin1 COLLATE latin1_general_cs DEFAULT NULL,
                                                    `creator` int NOT NULL,
                                                    PRIMARY KEY (`id`),
                                                    UNIQUE KEY `repository_path_constraint_unique` (`path`),
                                                    KEY `project_fk` (`project`),
                                                    CONSTRAINT `project_fk` FOREIGN KEY (`project`) REFERENCES `project` (`id`) ON DELETE CASCADE
 ) ENGINE=ndbcluster AUTO_INCREMENT=2061 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `git_executions` (
                                                `id` int NOT NULL AUTO_INCREMENT,
                                                `submission_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                `user` int NOT NULL,
                                                `repository` int NOT NULL,
                                                `execution_start` bigint DEFAULT NULL,
                                                `execution_stop` bigint DEFAULT NULL,
                                                `command_config` varchar(11000) CHARACTER SET latin1 COLLATE latin1_general_cs DEFAULT NULL,
                                                `state` varchar(128) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
                                                `final_result_message` varchar(11000) CHARACTER SET latin1 COLLATE latin1_general_cs DEFAULT NULL,
                                                `config_secret` varchar(255) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
                                                `hostname` varchar(128) NOT NULL DEFAULT "localhost",
                                                PRIMARY KEY (`id`),
                                                KEY `user` (`user`),
                                                KEY `git_exec_repo_fkc` (`repository`),
                                                CONSTRAINT `git_exec_repo_fkc` FOREIGN KEY (`repository`) REFERENCES `git_repositories` (`id`) ON DELETE CASCADE,
                                                CONSTRAINT `git_exec_usr_fkc` FOREIGN KEY (`user`) REFERENCES `users` (`uid`) ON DELETE CASCADE
) ENGINE=ndbcluster AUTO_INCREMENT=8251 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `git_commits` (
                                                `id` int NOT NULL AUTO_INCREMENT,
                                                `repository` int NOT NULL,
                                                `branch` varchar(250) CHARACTER SET latin1 COLLATE latin1_general_cs DEFAULT NULL,
                                                `hash` varchar(255) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
                                                `message` varchar(1000) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
                                                `committer_name` varchar(1000) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
                                                `committer_email` varchar(1000) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
                                                `date` TIMESTAMP NULL DEFAULT NULL,
                                                PRIMARY KEY (`id`),
                                                KEY `repository_fk` (`repository`),
                                                CONSTRAINT `repository_fk` FOREIGN KEY (`repository`) REFERENCES `git_repositories` (`id`) ON DELETE CASCADE
) ENGINE=ndbcluster AUTO_INCREMENT=4119 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `git_repository_remotes` (
                                          `id` int NOT NULL AUTO_INCREMENT,
                                          `repository` int NOT NULL,
                                          `remote_name` varchar(255) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
                                          `remote_url` varchar(1000) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
                                          PRIMARY KEY (`id`),
                                          KEY `git_repository_fk` (`repository`),
                                          CONSTRAINT `git_repository_fk` FOREIGN KEY (`repository`) REFERENCES `git_repositories` (`id`) ON DELETE CASCADE
) ENGINE=ndbcluster AUTO_INCREMENT=6164 DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `expectation_suite` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `feature_group_id` INT(11) NOT NULL,
    `name` VARCHAR(63) NOT NULL,
    `meta` VARCHAR(1000) DEFAULT "{}",
    `data_asset_type` VARCHAR(50),
    `ge_cloud_id` VARCHAR(200),
    `run_validation` BOOLEAN DEFAULT TRUE,
    `validation_ingestion_policy` VARCHAR(25),
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `feature_group_suite_fk` FOREIGN KEY (`feature_group_id`) REFERENCES `feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `expectation` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `expectation_suite_id` INT(11) NOT NULL,
    `expectation_type` VARCHAR(150) NOT NULL,
    `kwargs` VARCHAR(5000) NOT NULL,
    `meta` VARCHAR(1000) DEFAULT "{}",
    PRIMARY KEY (`id`),
    CONSTRAINT `suite_fk` FOREIGN KEY (`expectation_suite_id`) REFERENCES `expectation_suite` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE = ndbcluster DEFAULT CHARSET = latin1 COLLATE = latin1_general_cs;

CREATE TABLE IF NOT EXISTS `great_expectation` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `kwargs_template` VARCHAR(1000) NOT NULL,
    `expectation_type` VARCHAR(150) NOT NULL,
    UNIQUE KEY `unique_great_expectation` (`expectation_type`),
    PRIMARY KEY (`id`)
) ENGINE = ndbcluster DEFAULT CHARSET = latin1 COLLATE = latin1_general_cs;

CREATE TABLE IF NOT EXISTS `validation_report` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `feature_group_id` INT(11) NOT NULL,
    `success` BOOLEAN NOT NULL,
    `validation_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `evaluation_parameters` VARCHAR(1000) DEFAULT "{}",
    `meta` VARCHAR(2000) NULL DEFAULT "{}",
    `statistics` VARCHAR(1000) NOT NULL,
    `file_name` VARCHAR(255) COLLATE latin1_general_cs NOT NULL,
    `ingestion_result` VARCHAR(11) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `feature_group_report_fk` FOREIGN KEY (`feature_group_id`) REFERENCES `feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `validation_result` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `validation_report_id` INT(11) NOT NULL,
    `expectation_id` INT(11) NOT NULL,
    `success` BOOLEAN NOT NULL,
    `result` VARCHAR(1000) NOT NULL,
    `meta` VARCHAR(1000) DEFAULT "{}",
    `validation_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `ingestion_result` VARCHAR(11) NOT NULL,
    `expectation_config` VARCHAR(2150) NOT NULL,
    `exception_info` VARCHAR(1000) DEFAULT "{}",
    PRIMARY KEY (`id`),
    KEY (`expectation_id`),
    CONSTRAINT `report_fk_validation_result` FOREIGN KEY (`validation_report_id`) REFERENCES `validation_report` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE `feature_store_activity` (
                                          `id`                            INT(11) NOT NULL AUTO_INCREMENT,
                                          `event_time`                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          `uid`                           INT(11) NOT NULL,
                                          `type`                          INT(11) NOT NULL,
                                          `meta_type`                     INT(11) NULL,
                                          `meta_msg`                      VARCHAR(255) NULL,
                                          `execution_id`                  INT(11) NULL,
                                          `statistics_id`                 INT(11) NULL,
                                          `commit_id`                     BIGINT(20) NULL,
                                          `feature_group_id`              INT(11) NULL,
                                          `training_dataset_id`           INT(11) NULL,
                                          `feature_view_id`              INT(11) NULL,
                                          `expectation_suite_id`          INT(11) NULL,
                                          `validation_report_id`          INT(11) NULL,
                                          PRIMARY KEY (`id`),
                                          CONSTRAINT `fsa_feature_view_fk` FOREIGN KEY  (`feature_view_id`) REFERENCES
                                              `feature_view` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                          CONSTRAINT `fs_act_fg_fk` FOREIGN KEY (`feature_group_id`) REFERENCES `feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                          CONSTRAINT `fs_act_td_fk` FOREIGN KEY (`training_dataset_id`) REFERENCES `training_dataset` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                          CONSTRAINT `fs_act_uid_fk` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                          CONSTRAINT `fs_act_exec_fk` FOREIGN KEY (`execution_id`) REFERENCES `executions` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                          CONSTRAINT `fs_act_stat_fk` FOREIGN KEY (`statistics_id`) REFERENCES `feature_store_statistic` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                          CONSTRAINT `fs_act_commit_fk` FOREIGN KEY (`feature_group_id`, `commit_id`) REFERENCES `feature_group_commit` (`feature_group_id`, `commit_id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                          CONSTRAINT `fs_act_validationreport_fk` FOREIGN KEY (`validation_report_id`) REFERENCES `validation_report` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                                          CONSTRAINT `fs_act_expectationsuite_fk` FOREIGN KEY (`expectation_suite_id`) REFERENCES `expectation_suite` (`id`) ON DELETE SET NULL ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `tutorial` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `idx` INT(5) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `github_path` VARCHAR(200) NOT NULL,
    `description` VARCHAR(200) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = ndbcluster DEFAULT CHARSET = latin1 COLLATE = latin1_general_cs;

CREATE TABLE IF NOT EXISTS `pki_certificate` (
  `ca` TINYINT NOT NULL,
  `serial_number` BIGINT NOT NULL,
  `status` TINYINT NOT NULL,
  `subject` VARCHAR(255) NOT NULL,
  `certificate` VARBINARY(10000),
  `not_before` DATETIME NOT NULL,
  `not_after` DATETIME NOT NULL,
  PRIMARY KEY(`status`, `subject`) USING HASH,
  KEY `sn_index` (`serial_number`)
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `pki_crl` (
  `type` VARCHAR(20) NOT NULL,
  `crl` MEDIUMBLOB NOT NULL,
  PRIMARY KEY(`type`) USING HASH
) /*!50100 TABLESPACE `ts_1` STORAGE DISK */ ENGINE=ndbcluster COMMENT='NDB_TABLE=READ_BACKUP=1' DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `pki_key` (
	`owner` VARCHAR(100) NOT NULL,
	`type` TINYINT NOT NULL,
	`key` VARBINARY(8192) NOT NULL,
	PRIMARY KEY (`owner`, `type`) USING HASH
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `pki_serial_number` (
  `type` VARCHAR(20) NOT NULL,
  `number` BIGINT NOT NULL,
  PRIMARY KEY(`type`) USING HASH
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `feature_group_link` (
  `id` int NOT NULL AUTO_INCREMENT,
  `feature_group_id` int(11) NOT NULL,
  `parent_feature_group_id` int(11),
  `parent_feature_store` varchar(100) NOT NULL,
  `parent_feature_group_name` varchar(63) NOT NULL,
  `parent_feature_group_version` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `link_unique` (`feature_group_id`,`parent_feature_group_id`),
  KEY `feature_group_id_fkc` (`feature_group_id`),
  KEY `parent_feature_group_id_fkc` (`parent_feature_group_id`),
  CONSTRAINT `feature_group_id_fkc` FOREIGN KEY (`feature_group_id`) REFERENCES `feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `feature_group_parent_fkc` FOREIGN KEY (`parent_feature_group_id`) REFERENCES `feature_group` (`id`) ON DELETE SET NULL ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `feature_view_link` (
  `id` int NOT NULL AUTO_INCREMENT,
  `feature_view_id` int(11) NOT NULL,
  `parent_feature_group_id` int(11),
  `parent_feature_store` varchar(100) NOT NULL,
  `parent_feature_group_name` varchar(63) NOT NULL,
  `parent_feature_group_version` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `link_unique` (`feature_view_id`,`parent_feature_group_id`),
  KEY `feature_view_id_fkc` (`feature_view_id`),
  KEY `feature_view_parent_id_fkc` (`parent_feature_group_id`),
  CONSTRAINT `feature_view_id_fkc` FOREIGN KEY (`feature_view_id`) REFERENCES `feature_view` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `feature_view_parent_fkc` FOREIGN KEY (`parent_feature_group_id`) REFERENCES `feature_group` (`id`) ON DELETE SET NULL ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `hdfs_command_execution` (
  `id` int NOT NULL AUTO_INCREMENT,
  `execution_id` int NOT NULL,
  `command` varchar(45) NOT NULL,
  `submitted` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `src_path` varchar(1000) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_execution_id` (`execution_id`),
  UNIQUE KEY `uq_src_path` (`src_path`),
  KEY `fk_hdfs_file_command_1_idx` (`execution_id`),
  CONSTRAINT `fk_hdfs_file_command_1` FOREIGN KEY (`execution_id`) REFERENCES `executions` (`id`) ON DELETE CASCADE
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

-- FSTORE-921
CREATE TABLE `serving_key` (
                               `id` int(11) NOT NULL AUTO_INCREMENT,
                               `prefix` VARCHAR(63) NULL DEFAULT '',
                               `feature_name` VARCHAR(1000) NOT NULL,
                               `join_on` VARCHAR(1000) NULL,
                               `join_index` int(11) NOT NULL,
                               `feature_group_id` INT(11) NOT NULL,
                               `required` tinyint(1) NOT NULL DEFAULT '0',
                               `feature_view_id` INT(11) NULL,
                               PRIMARY KEY (`id`),
                               KEY `feature_view_id` (`feature_view_id`),
                               KEY `feature_group_id` (`feature_group_id`),
                               CONSTRAINT `feature_view_serving_key_fk` FOREIGN KEY (`feature_view_id`) REFERENCES `feature_view` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                               CONSTRAINT `feature_group_serving_key_fk` FOREIGN KEY (`feature_group_id`) REFERENCES `feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE IF NOT EXISTS `job_schedule` (
    `id` int NOT NULL AUTO_INCREMENT,
    `job_id` int NOT NULL,
    `start_date_time` timestamp NOT NULL,
    `end_date_time` timestamp,
    `enabled` BOOLEAN NOT NULL,
    `cron_expression` varchar(500) NOT NULL,
    `next_execution_date_time` timestamp,
    PRIMARY KEY (`id`),
    UNIQUE KEY `job_id` (`job_id`),
    CONSTRAINT `fk_schedule_job` FOREIGN KEY (`job_id`) REFERENCES `jobs` (`id`) ON DELETE CASCADE
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE `command_search_fs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `inode_id` bigint NOT NULL,
  `project_id` int,
  `op` VARCHAR(20) NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `feature_group_id` int(11),
  `feature_view_id` int(11),
  `training_dataset_id` int(11),
  `error_message` varchar(10000),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_command_search_feature_group` FOREIGN KEY (`feature_group_id`) REFERENCES `feature_group` (`id`) ON DELETE SET NULL ON UPDATE NO ACTION,
  CONSTRAINT `fk_command_search_feature_view` FOREIGN KEY (`feature_view_id`) REFERENCES `feature_view` (`id`) ON DELETE SET NULL ON UPDATE NO ACTION,
  CONSTRAINT `fk_command_search_training_dataset` FOREIGN KEY (`training_dataset_id`) REFERENCES `training_dataset` (`id`) ON DELETE SET NULL ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE `command_search_fs_history` (
  `h_id` bigint NOT NULL AUTO_INCREMENT,
  `executed` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `id` bigint NOT NULL,
  `inode_id` bigint NOT NULL,
  `project_id` int,
  `op` VARCHAR(20) NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `feature_group_id` int(11),
  `feature_view_id` int(11),
  `training_dataset_id` int(11),
  `error_message` varchar(10000),
  PRIMARY KEY (`h_id`)
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE `feature_store_tag_value` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `schema_id` int(11) NOT NULL,
  `feature_group_id` int(11),
  `feature_view_id` int(11),
  `training_dataset_id` int(11),
  `value` VARCHAR(29000) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_fs_tag_value_schema` FOREIGN KEY (`schema_id`) REFERENCES `feature_store_tag` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `fk_fs_tag_value_fg` FOREIGN KEY (`feature_group_id`) REFERENCES `feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `fk_fs_tag_value_fv` FOREIGN KEY (`feature_view_id`) REFERENCES `feature_view` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `fk_fs_tag_value_td` FOREIGN KEY (`training_dataset_id`) REFERENCES `training_dataset` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE `feature_store_keyword` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `feature_group_id` int(11),
  `feature_view_id` int(11),
  `training_dataset_id` int(11),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_fs_keyword_fg` FOREIGN KEY (`feature_group_id`) REFERENCES `feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `fk_fs_keyword_fv` FOREIGN KEY (`feature_view_id`) REFERENCES `feature_view` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `fk_fs_keyword_td` FOREIGN KEY (`training_dataset_id`) REFERENCES `training_dataset` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
