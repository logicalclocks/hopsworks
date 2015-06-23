-- MySQL dump 10.13  Distrib 5.5.43, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: kthfs
-- ------------------------------------------------------
-- Server version	5.6.24-ndb-7.4.6-cluster-gpl

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
-- Table structure for table `activity`
--

DROP TABLE IF EXISTS `activity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `activity` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `activity` varchar(128) COLLATE utf8_unicode_ci NOT NULL,
  `user_id` int(10) NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `flag` varchar(128) COLLATE utf8_unicode_ci DEFAULT NULL,
  `project_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `project_id` (`project_id`),
  CONSTRAINT `activity_ibfk_2` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `activity_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`uid`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=1025 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `address`
--

DROP TABLE IF EXISTS `address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `address` (
  `address1` varchar(50) COLLATE utf8_unicode_ci DEFAULT NULL,
  `address2` varchar(50) COLLATE utf8_unicode_ci DEFAULT NULL,
  `address3` varchar(50) COLLATE utf8_unicode_ci DEFAULT NULL,
  `city` varchar(30) COLLATE utf8_unicode_ci DEFAULT NULL,
  `state` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
  `country` varchar(40) COLLATE utf8_unicode_ci DEFAULT NULL,
  `postalcode` varchar(10) COLLATE utf8_unicode_ci DEFAULT NULL,
  `address_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uid` int(10) NOT NULL,
  PRIMARY KEY (`address_id`),
  KEY `uid` (`uid`),
  CONSTRAINT `address_ibfk_1` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `anatomical_parts`
--

DROP TABLE IF EXISTS `anatomical_parts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `anatomical_parts` (
  `id` int(16) NOT NULL AUTO_INCREMENT,
  `ontology_name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `ontology_version` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `ontology_code` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `ontology_description` varchar(1000) COLLATE utf8_unicode_ci DEFAULT NULL,
  `explanation` varchar(2000) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bbc_group`
--

DROP TABLE IF EXISTS `bbc_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bbc_group` (
  `group_name` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
  `group_desc` varchar(200) COLLATE utf8_unicode_ci DEFAULT NULL,
  `gid` int(11) NOT NULL,
  PRIMARY KEY (`gid`)
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `collection_type`
--

DROP TABLE IF EXISTS `collection_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `collection_type` (
  `collection_id` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `type` varchar(128) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`collection_id`,`type`),
  CONSTRAINT `collection_type_ibfk_1` FOREIGN KEY (`collection_id`) REFERENCES `samplecollections` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `consent`
--

DROP TABLE IF EXISTS `consent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `consent` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `date` date DEFAULT NULL,
  `project_id` int(11) NOT NULL,
  `status` varchar(30) COLLATE utf8_unicode_ci DEFAULT NULL,
  `name` varchar(80) COLLATE utf8_unicode_ci DEFAULT NULL,
  `type` varchar(30) COLLATE utf8_unicode_ci DEFAULT NULL,
  `consent_form` longblob,
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`),
  CONSTRAINT `consent_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `diseases`
--

DROP TABLE IF EXISTS `diseases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `diseases` (
  `id` int(16) NOT NULL AUTO_INCREMENT,
  `ontology_name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `ontology_version` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `ontology_code` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `ontology_description` varchar(1000) COLLATE utf8_unicode_ci DEFAULT NULL,
  `explanation` varchar(1000) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `field_predefined_values`
--

DROP TABLE IF EXISTS `field_predefined_values`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `field_predefined_values` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `fieldid` int(11) NOT NULL,
  `valuee` varchar(250) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `field_types`
--

DROP TABLE IF EXISTS `field_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `field_types` (
  `id` mediumint(9) NOT NULL AUTO_INCREMENT,
  `description` varchar(50) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=ndbcluster AUTO_INCREMENT=4 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fields`
--

DROP TABLE IF EXISTS `fields`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fields` (
  `fieldid` int(11) NOT NULL AUTO_INCREMENT,
  `maxsize` int(11) DEFAULT NULL,
  `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `required` smallint(6) DEFAULT NULL,
  `searchable` smallint(6) DEFAULT NULL,
  `tableid` int(11) DEFAULT NULL,
  `type` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `description` varchar(250) CHARACTER SET utf8 NOT NULL,
  `fieldtypeid` int(11) NOT NULL,
  PRIMARY KEY (`fieldid`),
  KEY `FK_fields_tableid` (`tableid`),
  CONSTRAINT `FK_fields_tableid` FOREIGN KEY (`tableid`) REFERENCES `tables` (`tableid`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `job_execution_files`
--

DROP TABLE IF EXISTS `job_execution_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `job_execution_files` (
  `job_id` int(11) NOT NULL,
  `name` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `path` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`job_id`,`name`),
  CONSTRAINT `job_execution_files_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `job_input_files`
--

DROP TABLE IF EXISTS `job_input_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `job_input_files` (
  `job_id` int(11) NOT NULL,
  `path` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`job_id`,`name`),
  CONSTRAINT `job_input_files_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `job_output_files`
--

DROP TABLE IF EXISTS `job_output_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `job_output_files` (
  `job_id` int(11) NOT NULL,
  `path` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`job_id`,`name`),
  CONSTRAINT `job_output_files_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jobhistory`
--

DROP TABLE IF EXISTS `jobhistory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `jobhistory` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) COLLATE utf8_unicode_ci DEFAULT NULL,
  `submission_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `project_id` int(11) NOT NULL,
  `user` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `state` varchar(128) COLLATE utf8_unicode_ci NOT NULL,
  `execution_duration` bigint(20) DEFAULT NULL,
  `args` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `stdout_path` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `stderr_path` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `type` varchar(128) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`),
  KEY `user` (`user`),
  CONSTRAINT `jobhistory_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `jobhistory_ibfk_2` FOREIGN KEY (`user`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `organization`
--

DROP TABLE IF EXISTS `organization`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `organization` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uid` int(11) DEFAULT NULL,
  `org_name` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
  `website` varchar(200) COLLATE utf8_unicode_ci DEFAULT NULL,
  `contact_person` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
  `contact_email` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
  `department` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8_unicode_ci DEFAULT NULL,
  `fax` varchar(20) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_Organization` (`uid`),
  CONSTRAINT `fk_Organization` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=9 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `people_group`
--

DROP TABLE IF EXISTS `people_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `people_group` (
  `uid` int(10) NOT NULL,
  `gid` int(11) NOT NULL,
  PRIMARY KEY (`uid`,`gid`),
  KEY `gid` (`gid`),
  CONSTRAINT `people_group_ibfk_1` FOREIGN KEY (`gid`) REFERENCES `bbc_group` (`gid`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `people_group_ibfk_2` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project`
--

DROP TABLE IF EXISTS `project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `projectname` varchar(128) COLLATE utf8_unicode_ci NOT NULL,
  `username` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `retention_period` date DEFAULT NULL,
  `ethical_status` varchar(30) COLLATE utf8_unicode_ci DEFAULT NULL,
  `archived` tinyint(1) DEFAULT '0',
  `deleted` tinyint(1) DEFAULT '0',
  `description` varchar(3000) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`,`projectname`),
  CONSTRAINT `project_ibfk_1` FOREIGN KEY (`username`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=1025 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_design`
--

DROP TABLE IF EXISTS `project_design`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_design` (
  `project_id` int(11) NOT NULL,
  `design` varchar(128) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`project_id`),
  CONSTRAINT `project_design_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project_meta` (`project_id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_inclusion_criteria`
--

DROP TABLE IF EXISTS `project_inclusion_criteria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_inclusion_criteria` (
  `project_id` int(11) NOT NULL,
  `criterium` varchar(128) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`project_id`,`criterium`),
  CONSTRAINT `project_inclusion_criteria_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project_meta` (`project_id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_meta`
--

DROP TABLE IF EXISTS `project_meta`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_meta` (
  `project_id` int(11) NOT NULL,
  `description` varchar(2000) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`project_id`),
  CONSTRAINT `project_meta_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_services`
--

DROP TABLE IF EXISTS `project_services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_services` (
  `project_id` int(11) NOT NULL,
  `service` varchar(32) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`project_id`,`service`),
  CONSTRAINT `project_services_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_team`
--

DROP TABLE IF EXISTS `project_team`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_team` (
  `project_id` int(11) NOT NULL,
  `team_member` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `team_role` varchar(32) COLLATE utf8_unicode_ci NOT NULL,
  `added` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`project_id`,`team_member`),
  KEY `team_member` (`team_member`),
  CONSTRAINT `project_team_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `project_team_ibfk_2` FOREIGN KEY (`team_member`) REFERENCES `users` (`email`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `raw_data`
--

DROP TABLE IF EXISTS `raw_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `raw_data` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `data` longtext COLLATE utf8_unicode_ci,
  `fieldid` int(11) DEFAULT NULL,
  `tupleid` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_raw_data_fieldid` (`fieldid`),
  CONSTRAINT `FK_raw_data_fieldid` FOREIGN KEY (`fieldid`) REFERENCES `fields` (`fieldid`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sample_material`
--

DROP TABLE IF EXISTS `sample_material`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sample_material` (
  `sample_id` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `type` varchar(128) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`sample_id`,`type`),
  CONSTRAINT `sample_material_ibfk_1` FOREIGN KEY (`sample_id`) REFERENCES `samples` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `samplecollection_disease`
--

DROP TABLE IF EXISTS `samplecollection_disease`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `samplecollection_disease` (
  `collection_id` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `disease_id` int(16) NOT NULL,
  PRIMARY KEY (`collection_id`,`disease_id`),
  KEY `disease_id` (`disease_id`),
  CONSTRAINT `samplecollection_disease_ibfk_1` FOREIGN KEY (`disease_id`) REFERENCES `diseases` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `samplecollection_disease_ibfk_2` FOREIGN KEY (`collection_id`) REFERENCES `samplecollections` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `samplecollection_type`
--

DROP TABLE IF EXISTS `samplecollection_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `samplecollection_type` (
  `collection_id` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `type` varchar(128) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`collection_id`,`type`),
  CONSTRAINT `samplecollection_type_ibfk_1` FOREIGN KEY (`collection_id`) REFERENCES `samplecollections` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `samplecollections`
--

DROP TABLE IF EXISTS `samplecollections`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `samplecollections` (
  `id` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `acronym` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `name` varchar(1024) COLLATE utf8_unicode_ci NOT NULL,
  `description` varchar(2000) COLLATE utf8_unicode_ci DEFAULT NULL,
  `contact` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `project_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `acronym` (`acronym`),
  KEY `project_id` (`project_id`),
  KEY `contact` (`contact`),
  CONSTRAINT `samplecollections_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `samplecollections_ibfk_2` FOREIGN KEY (`contact`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `samples`
--

DROP TABLE IF EXISTS `samples`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `samples` (
  `samplecollection_id` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `id` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `parent_id` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `sampled_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `anatomical_site` int(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `anatomical_site` (`anatomical_site`),
  KEY `parent_id` (`parent_id`),
  KEY `samplecollection_id` (`samplecollection_id`),
  CONSTRAINT `samples_ibfk_1` FOREIGN KEY (`anatomical_site`) REFERENCES `anatomical_parts` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `samples_ibfk_3` FOREIGN KEY (`samplecollection_id`) REFERENCES `samplecollections` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `samples_ibfk_2` FOREIGN KEY (`parent_id`) REFERENCES `samples` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tables`
--

DROP TABLE IF EXISTS `tables`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tables` (
  `tableid` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `templateid` int(11) NOT NULL,
  PRIMARY KEY (`tableid`),
  KEY `FK_tables_templateid` (`templateid`),
  CONSTRAINT `FK_tables_templateid` FOREIGN KEY (`templateid`) REFERENCES `templates` (`templateid`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `templates`
--

DROP TABLE IF EXISTS `templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `templates` (
  `templateid` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(250) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`templateid`)
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tuple_to_file`
--

DROP TABLE IF EXISTS `tuple_to_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tuple_to_file` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `inodeid` int(11) DEFAULT NULL,
  `tupleid` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userlogins`
--

DROP TABLE IF EXISTS `userlogins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userlogins` (
  `login_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ip` varchar(16) COLLATE utf8_unicode_ci DEFAULT NULL,
  `os` varchar(30) COLLATE utf8_unicode_ci DEFAULT NULL,
  `browser` varchar(40) COLLATE utf8_unicode_ci DEFAULT NULL,
  `action` varchar(80) COLLATE utf8_unicode_ci DEFAULT NULL,
  `outcome` varchar(20) COLLATE utf8_unicode_ci DEFAULT NULL,
  `uid` int(10) NOT NULL,
  `login_date` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`login_id`),
  KEY `login_date` (`login_date`),
  KEY `uid` (`uid`),
  CONSTRAINT `userlogins_ibfk_1` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster AUTO_INCREMENT=2055 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `uid` int(10) NOT NULL DEFAULT '1000',
  `username` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `password` varchar(128) COLLATE utf8_unicode_ci NOT NULL,
  `email` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `fname` varchar(30) COLLATE utf8_unicode_ci DEFAULT NULL,
  `lname` varchar(30) COLLATE utf8_unicode_ci DEFAULT NULL,
  `activated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `title` varchar(10) COLLATE utf8_unicode_ci DEFAULT NULL,
  `orcid` varchar(20) COLLATE utf8_unicode_ci DEFAULT NULL,
  `false_login` int(11) NOT NULL DEFAULT '-1',
  `isonline` tinyint(1) NOT NULL DEFAULT '0',
  `secret` varchar(20) COLLATE utf8_unicode_ci DEFAULT NULL,
  `validation_key` varchar(128) COLLATE utf8_unicode_ci DEFAULT NULL,
  `security_question` varchar(20) COLLATE utf8_unicode_ci DEFAULT NULL,
  `security_answer` varchar(128) COLLATE utf8_unicode_ci DEFAULT NULL,
  `yubikey_user` int(11) NOT NULL DEFAULT '0',
  `password_changed` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `notes` varchar(500) COLLATE utf8_unicode_ci DEFAULT NULL,
  `mobile` varchar(20) COLLATE utf8_unicode_ci DEFAULT NULL,
  `status` int(11) NOT NULL DEFAULT '-1',
  PRIMARY KEY (`uid`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`)
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary table structure for view `users_groups`
--

DROP TABLE IF EXISTS `users_groups`;
/*!50001 DROP VIEW IF EXISTS `users_groups`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `users_groups` (
  `username` tinyint NOT NULL,
  `password` tinyint NOT NULL,
  `secret` tinyint NOT NULL,
  `email` tinyint NOT NULL,
  `group_name` tinyint NOT NULL
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `yubikey`
--

DROP TABLE IF EXISTS `yubikey`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yubikey` (
  `serial` varchar(10) COLLATE utf8_unicode_ci DEFAULT NULL,
  `version` varchar(15) COLLATE utf8_unicode_ci DEFAULT NULL,
  `notes` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
  `counter` int(11) DEFAULT NULL,
  `low` int(11) DEFAULT NULL,
  `high` int(11) DEFAULT NULL,
  `session_use` int(11) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `aes_secret` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
  `public_id` varchar(40) COLLATE utf8_unicode_ci DEFAULT NULL,
  `accessed` timestamp NULL DEFAULT NULL,
  `status` int(11) DEFAULT '-1',
  `yubidnum` int(11) NOT NULL AUTO_INCREMENT,
  `uid` int(11) NOT NULL,
  PRIMARY KEY (`yubidnum`),
  UNIQUE KEY `uid` (`uid`),
  UNIQUE KEY `serial` (`serial`),
  UNIQUE KEY `public_id` (`public_id`),
  UNIQUE KEY `public_id_2` (`public_id`),
  CONSTRAINT `yubikey_ibfk_1` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `zeppelin_notes`
--

DROP TABLE IF EXISTS `zeppelin_notes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `zeppelin_notes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`),
  CONSTRAINT `zeppelin_notes_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `zeppelin_paragraph`
--

DROP TABLE IF EXISTS `zeppelin_paragraph`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `zeppelin_paragraph` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `note_id` int(11) NOT NULL,
  `content` text COLLATE utf8_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `note_id` (`note_id`),
  CONSTRAINT `zeppelin_paragraph_ibfk_1` FOREIGN KEY (`note_id`) REFERENCES `zeppelin_notes` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Final view structure for view `users_groups`
--

/*!50001 DROP TABLE IF EXISTS `users_groups`*/;
/*!50001 DROP VIEW IF EXISTS `users_groups`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `users_groups` AS select `u`.`username` AS `username`,`u`.`password` AS `password`,`u`.`secret` AS `secret`,`u`.`email` AS `email`,`g`.`group_name` AS `group_name` from ((`people_group` `ug` join `users` `u` on((`u`.`uid` = `ug`.`uid`))) join `bbc_group` `g` on((`g`.`gid` = `ug`.`gid`))) */;
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

-- Dump completed on 2015-06-05 16:40:32
