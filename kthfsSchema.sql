-- MySQL dump 10.13  Distrib 5.6.23, for Linux (x86_64)
--
-- Host: localhost    Database: kthfs
-- ------------------------------------------------------
-- Server version	5.6.23

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


--
-- Table structure for table `BBCGroup`
--

DROP TABLE IF EXISTS `BBCGroup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `BBCGroup` (
  `gid` int(10) NOT NULL,
  `group_name` varchar(20) NOT NULL,
  `group_desc` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`gid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CONSENT`
--

DROP TABLE IF EXISTS `CONSENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CONSENT` (
  `id` int(11) NOT NULL,
  `date` date DEFAULT NULL,
  `study_name` varchar(128) DEFAULT NULL,
  `retention_period` date DEFAULT NULL,
  `consent_form` blob,
  `status` varchar(30) DEFAULT NULL,
  `name` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `LOGIN`
--
DROP TABLE IF EXISTS `LOGIN`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LOGIN` (
  `login_id` bigint(20) NOT NULL,
  `uid` varchar(8) DEFAULT NULL,
  `username` varchar(80) DEFAULT NULL,
  `role` varchar(20) DEFAULT NULL,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `ip` varchar(16) DEFAULT NULL,
  `os` varchar(30) DEFAULT NULL,
  `browser` varchar(40) DEFAULT NULL,
  `action` varchar(80) DEFAULT NULL,
  `outcome` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`login_id`),
  KEY `LOGIN_uid_idx` (`uid`),
  KEY `LOGIN_username_idx` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `ANATOMICAL_PARTS`
--

DROP TABLE IF EXISTS `ANATOMICAL_PARTS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ANATOMICAL_PARTS` (
  `id` int(16) NOT NULL AUTO_INCREMENT,
  `ontology_name` varchar(255) DEFAULT NULL,
  `ontology_version` varchar(255) DEFAULT NULL,
  `ontology_code` varchar(255) DEFAULT NULL,
  `ontology_description` varchar(2000) DEFAULT NULL,
  `explanation` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Address`
--

DROP TABLE IF EXISTS `Address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Address` (
  `address1` varchar(120) DEFAULT NULL,
  `address2` varchar(120) DEFAULT NULL,
  `address3` varchar(120) DEFAULT NULL,
  `city` varchar(100) DEFAULT NULL,
  `state` varchar(100) DEFAULT NULL,
  `country` varchar(100) DEFAULT NULL,
  `postalcode` varchar(16) DEFAULT NULL,
  `address_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uid` int(11) NOT NULL,
  PRIMARY KEY (`address_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `DISEASES`
--

DROP TABLE IF EXISTS `DISEASES`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DISEASES` (
  `id` int(16) NOT NULL AUTO_INCREMENT,
  `ontology_name` varchar(255) DEFAULT NULL,
  `ontology_version` varchar(255) DEFAULT NULL,
  `ontology_code` varchar(255) DEFAULT NULL,
  `ontology_description` varchar(2000) DEFAULT NULL,
  `explanation` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Group`
--
DROP TABLE IF EXISTS `Group`;

--
-- Table structure for table `Inodes`
--

DROP TABLE IF EXISTS `Inodes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Inodes` (
  `id` mediumint(9) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `pid` mediumint(9) DEFAULT NULL,
  `modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `isDir` tinyint(1) NOT NULL,
  `size` int(11) DEFAULT NULL,
  `status` enum('uploading','copying_to_hdfs','available') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `pid_3` (`pid`,`name`),
  KEY `pid` (`pid`),
  KEY `pid_2` (`pid`,`isDir`),
  KEY `name` (`name`),
  CONSTRAINT `Inodes_ibfk_1` FOREIGN KEY (`pid`) REFERENCES `Inodes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1592 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;



DROP TABLE IF EXISTS `Roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Roles` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `CLUSTER` varchar(48) NOT NULL,
  `HOSTID` varchar(128) NOT NULL,
  `PID` int(11) DEFAULT NULL,
  `ROLE_` varchar(48) NOT NULL,
  `SERVICE` varchar(48) NOT NULL,
  `STATUS` int(11) NOT NULL,
  `UPTIME` bigint(20) DEFAULT NULL,
  `WEBPORT` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SAMPLECOLLECTIONS`
--

DROP TABLE IF EXISTS `SAMPLECOLLECTIONS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SAMPLECOLLECTIONS` (
  `id` varchar(255) NOT NULL,
  `acronym` varchar(255) NOT NULL,
  `name` varchar(1024) NOT NULL,
  `description` varchar(2000) DEFAULT NULL,
  `contact` varchar(255) NOT NULL,
  `study` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `acronym` (`acronym`),
  KEY `FK_SAMPLECOLLECTIONS_study` (`study`),
  KEY `FK_SAMPLECOLLECTIONS_contact` (`contact`),
  CONSTRAINT `SAMPLECOLLECTIONS_ibfk_1` FOREIGN KEY (`contact`) REFERENCES `USERS` (`EMAIL`),
  CONSTRAINT `SAMPLECOLLECTIONS_ibfk_2` FOREIGN KEY (`study`) REFERENCES `study` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SAMPLECOLLECTION_DISEASE`
--

DROP TABLE IF EXISTS `SAMPLECOLLECTION_DISEASE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SAMPLECOLLECTION_DISEASE` (
  `collection_id` varchar(255) NOT NULL DEFAULT '',
  `disease_id` int(16) NOT NULL DEFAULT '0',
  PRIMARY KEY (`collection_id`,`disease_id`),
  KEY `FK_SAMPLECOLLECTION_DISEASE_disease_id` (`disease_id`),
  CONSTRAINT `FK_SAMPLECOLLECTION_DISEASE_disease_id` FOREIGN KEY (`disease_id`) REFERENCES `DISEASES` (`id`),
  CONSTRAINT `SAMPLECOLLECTION_DISEASE_ibfk_1` FOREIGN KEY (`collection_id`) REFERENCES `SAMPLECOLLECTIONS` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SAMPLECOLLECTION_TYPE`
--

DROP TABLE IF EXISTS `SAMPLECOLLECTION_TYPE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SAMPLECOLLECTION_TYPE` (
  `collection_id` varchar(255) NOT NULL,
  `type` varchar(128) NOT NULL,
  PRIMARY KEY (`collection_id`,`type`),
  CONSTRAINT `SAMPLECOLLECTION_TYPE_ibfk_1` FOREIGN KEY (`collection_id`) REFERENCES `SAMPLECOLLECTIONS` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SAMPLES`
--

DROP TABLE IF EXISTS `SAMPLES`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SAMPLES` (
  `samplecollection_id` varchar(255) NOT NULL,
  `id` varchar(255) NOT NULL,
  `parent_id` varchar(255) DEFAULT NULL,
  `sampled_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `anatomical_site` int(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_SAMPLES_parent_id` (`parent_id`),
  KEY `FK_SAMPLES_anatomical_site` (`anatomical_site`),
  KEY `FK_SAMPLES_samplecollection_id` (`samplecollection_id`),
  CONSTRAINT `SAMPLES_ibfk_2` FOREIGN KEY (`parent_id`) REFERENCES `SAMPLES` (`id`),
  CONSTRAINT `SAMPLES_ibfk_3` FOREIGN KEY (`anatomical_site`) REFERENCES `ANATOMICAL_PARTS` (`id`),
  CONSTRAINT `SAMPLES_ibfk_4` FOREIGN KEY (`samplecollection_id`) REFERENCES `SAMPLECOLLECTIONS` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SAMPLE_MATERIAL`
--

DROP TABLE IF EXISTS `SAMPLE_MATERIAL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SAMPLE_MATERIAL` (
  `sample_id` varchar(255) NOT NULL,
  `type` varchar(128) NOT NULL,
  PRIMARY KEY (`sample_id`,`type`),
  CONSTRAINT `SAMPLE_MATERIAL_ibfk_1` FOREIGN KEY (`sample_id`) REFERENCES `SAMPLES` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `STUDY_DESIGN`
--

DROP TABLE IF EXISTS `STUDY_DESIGN`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `STUDY_DESIGN` (
  `study_id` varchar(128) DEFAULT NULL,
  `design` varchar(128) DEFAULT NULL,
  KEY `FK_STUDY_DESIGN_study_id` (`study_id`),
  CONSTRAINT `STUDY_DESIGN_ibfk_1` FOREIGN KEY (`study_id`) REFERENCES `STUDY_META` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `STUDY_GROUPS`
--

DROP TABLE IF EXISTS `STUDY_GROUPS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `STUDY_GROUPS` (
  `email` varchar(255) NOT NULL,
  `groupname` varchar(64) NOT NULL,
  PRIMARY KEY (`email`,`groupname`),
  CONSTRAINT `STUDY_GROUPS_ibfk_1` FOREIGN KEY (`email`) REFERENCES `USERS` (`EMAIL`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `STUDY_INCLUSION_CRITERIA`
--

DROP TABLE IF EXISTS `STUDY_INCLUSION_CRITERIA`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `STUDY_INCLUSION_CRITERIA` (
  `study_id` varchar(128) NOT NULL DEFAULT '',
  `criterium` varchar(128) NOT NULL,
  PRIMARY KEY (`study_id`,`criterium`),
  CONSTRAINT `STUDY_INCLUSION_CRITERIA_ibfk_1` FOREIGN KEY (`study_id`) REFERENCES `STUDY_META` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `STUDY_META`
--

DROP TABLE IF EXISTS `STUDY_META`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `STUDY_META` (
  `id` varchar(128) NOT NULL,
  `studyname` varchar(128) NOT NULL,
  `description` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`studyname`),
  UNIQUE KEY `id` (`id`),
  CONSTRAINT `STUDY_META_ibfk_1` FOREIGN KEY (`studyname`) REFERENCES `study` (`name`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `StudyDetails`
--

DROP TABLE IF EXISTS `StudyDetails`;
/*!50001 DROP VIEW IF EXISTS `StudyDetails`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE VIEW `StudyDetails` AS SELECT 
 1 AS `studyName`,
 1 AS `email`,
 1 AS `creator`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `StudyRoles`
--

DROP TABLE IF EXISTS `StudyRoles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `StudyRoles` (
  `id` int(11) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `study_role` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `StudyTeam`
--

DROP TABLE IF EXISTS `StudyTeam`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `StudyTeam` (
  `name` varchar(255) NOT NULL,
  `team_member` varchar(255) NOT NULL,
  `team_role` enum('Master','Researcher','Auditor') NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`name`,`team_member`),
  KEY `fk_studyTeam_users` (`team_member`),
  CONSTRAINT `fk_studyTeam_users` FOREIGN KEY (`team_member`) REFERENCES `USERS` (`EMAIL`) ON DELETE CASCADE,
  CONSTRAINT `fk_study_studyTeam` FOREIGN KEY (`name`) REFERENCES `study` (`name`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `activity`
--

DROP TABLE IF EXISTS `activity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `activity` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `activity` varchar(128) DEFAULT NULL,
  `performed_By` varchar(255) DEFAULT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `flag` enum('DATA','STUDY','TEAM','USERS') DEFAULT NULL,
  `activity_on` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=526 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `activitydetails`
--

DROP TABLE IF EXISTS `activitydetails`;
/*!50001 DROP VIEW IF EXISTS `activitydetails`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE VIEW `activitydetails` AS SELECT 
 1 AS `id`,
 1 AS `performed_by_email`,
 1 AS `performed_by_name`,
 1 AS `description`,
 1 AS `studyname`,
 1 AS `timestamp`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `collection_type`
--

DROP TABLE IF EXISTS `collection_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `collection_type` (
  `collection_id` varchar(255) NOT NULL DEFAULT '',
  `type` varchar(128) NOT NULL,
  PRIMARY KEY (`collection_id`,`type`),
  CONSTRAINT `collection_type_ibfk_1` FOREIGN KEY (`collection_id`) REFERENCES `SAMPLECOLLECTIONS` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `job_execution_files`
--

DROP TABLE IF EXISTS `job_execution_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `job_execution_files` (
  `job_id` bigint(20) unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `path` varchar(255) NOT NULL,
  PRIMARY KEY (`job_id`,`name`),
  CONSTRAINT `job_execution_files_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `job_input_files`
--

DROP TABLE IF EXISTS `job_input_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `job_input_files` (
  `job_id` bigint(20) unsigned NOT NULL,
  `path` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`job_id`,`name`),
  CONSTRAINT `job_input_files_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `job_output_files`
--

DROP TABLE IF EXISTS `job_output_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `job_output_files` (
  `job_id` bigint(20) unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `path` varchar(255) NOT NULL,
  PRIMARY KEY (`job_id`,`name`),
  CONSTRAINT `job_output_files_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jobhistory`
--

DROP TABLE IF EXISTS `jobhistory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `jobhistory` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(128) DEFAULT NULL,
  `submission_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `study` varchar(128) NOT NULL,
  `user` varchar(255) NOT NULL,
  `state` varchar(128) NOT NULL,
  `execution_duration` bigint(20) DEFAULT NULL,
  `args` varchar(255) DEFAULT NULL,
  `stdout_path` varchar(255) DEFAULT NULL,
  `stderr_path` varchar(255) DEFAULT NULL,
  `type` varchar(128) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FK_jobhistory_user` (`user`),
  KEY `FK_jobhistory_study` (`study`),
  CONSTRAINT `FK_jobhistory_user` FOREIGN KEY (`user`) REFERENCES `USERS` (`EMAIL`),
  CONSTRAINT `jobhistory_ibfk_3` FOREIGN KEY (`study`) REFERENCES `study` (`name`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=367 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `study`
--

DROP TABLE IF EXISTS `study`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `study` (
  `name` varchar(128) NOT NULL,
  `username` varchar(255) NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`name`),
  UNIQUE KEY `name` (`name`),
  KEY `fk_study_users` (`username`),
  CONSTRAINT `fk_study_users` FOREIGN KEY (`username`) REFERENCES `USERS` (`EMAIL`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `study_group_members`
--

DROP TABLE IF EXISTS `study_group_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `study_group_members` (
  `studyname` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  `timeadded` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `added_by` varchar(255) NOT NULL,
  `team_role` enum('Master','Researcher','Guest') NOT NULL,
  PRIMARY KEY (`studyname`,`username`),
  CONSTRAINT `FK_study_group_members_studyname` FOREIGN KEY (`studyname`) REFERENCES `study` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `study_services`
--

DROP TABLE IF EXISTS `study_services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `study_services` (
  `study` varchar(128) NOT NULL,
  `service` varchar(32) NOT NULL,
  PRIMARY KEY (`study`,`service`),
  CONSTRAINT `study_services_ibfk_1` FOREIGN KEY (`study`) REFERENCES `study` (`name`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `People_Group`
--

DROP TABLE IF EXISTS `People_Group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `People_Group` (
  `uid` int(10) DEFAULT NULL,
  `Pgid` int(11) NOT NULL AUTO_INCREMENT,
  `gid` int(11) DEFAULT NULL,
  PRIMARY KEY (`Pgid`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `USERS`
--

DROP TABLE IF EXISTS `USERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `USERS` (
  `uid` int(10) NOT NULL DEFAULT '1000',
  `username` varchar(10) NOT NULL,
  `password` varchar(128) NOT NULL,
  `email` varchar(45) DEFAULT NULL,
  `fname` varchar(30) DEFAULT NULL,
  `lname` varchar(30) DEFAULT NULL,
  `activated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `home_org` varchar(100) DEFAULT NULL,
  `title` varchar(10) DEFAULT NULL,
  `mobile` varchar(20) DEFAULT NULL,
  `orcid` varchar(20) DEFAULT NULL,
  `false_login` int(11) NOT NULL DEFAULT '0',
  `status` int(11) NOT NULL DEFAULT '-1',
  `isonline` int(11) NOT NULL DEFAULT '-1',
  `secret` varchar(20) DEFAULT NULL,
  `security_question` varchar(20) DEFAULT NULL,
  `security_answer` varchar(128) DEFAULT NULL,
  `yubikey_user` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`uid`),
  UNIQUE KEY `email_idx` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary table structure for view `USERS_GROUPS`
--

DROP TABLE IF EXISTS `USERS_GROUPS`;
/*!50001 DROP VIEW IF EXISTS `USERS_GROUPS`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `USERS_GROUPS` (
  `username` tinyint NOT NULL,
  `password` tinyint NOT NULL,
  `secret` tinyint NOT NULL,
  `email` tinyint NOT NULL,
  `group_name` tinyint NOT NULL
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `Yubikey`
--

DROP TABLE IF EXISTS `Yubikey`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Yubikey` (
  `serial` varchar(10) DEFAULT NULL,
  `version` varchar(15) DEFAULT NULL,
  `notes` varchar(100) DEFAULT NULL,
  `counter` int(11) DEFAULT NULL,
  `low` int(11) DEFAULT NULL,
  `high` int(11) DEFAULT NULL,
  `session_use` int(11) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `aes_secret` varchar(100) DEFAULT NULL,
  `public_id` varchar(40) DEFAULT NULL,
  `accessed` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `status` int(11) DEFAULT '-1',
  `yubidnum` int(11) NOT NULL AUTO_INCREMENT,
  `uid` int(11) NOT NULL,
  PRIMARY KEY (`yubidnum`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;


/*!50001 DROP VIEW IF EXISTS `StudyDetails`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `StudyDetails` AS select `study`.`name` AS `studyName`,`study`.`username` AS `email`,`USERS`.`NAME` AS `creator` from (`study` join `USERS` on((`study`.`username` = `USERS`.`EMAIL`))) where `study`.`name` in (select `StudyTeam`.`name` from `StudyTeam`) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `activitydetails`
--

/*!50001 DROP VIEW IF EXISTS `activitydetails`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `activitydetails` AS select `activity`.`id` AS `id`,`activity`.`performed_By` AS `performed_by_email`,concat_ws(' ',`USERS`.`fname`,`USERS`.`lname`) AS `performed_by_name`,`activity`.`activity` AS `description`,`activity`.`activity_on` AS `studyname`,`activity`.`timestamp` AS `timestamp` from (`activity` join `USERS` on((`activity`.`performed_By` = `USERS`.`email`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_People_Group`
--

/*!50001 DROP VIEW IF EXISTS `v_People_Group`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_People_Group` AS select `u`.`username` AS `username`,`u`.`password` AS `password`,`u`.`secret` AS `secret`,`u`.`email` AS `email`,`g`.`group_name` AS `group_name` from ((`People_Group` `ug` join `People` `u` on((`u`.`uid` = `ug`.`uid`))) join `Group` `g` on((`g`.`gid` = `ug`.`gid`))) */;
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

-- Dump completed on 2015-03-04 14:28:12
