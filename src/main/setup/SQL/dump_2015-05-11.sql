-- MySQL dump 10.13  Distrib 5.6.23, for Win64 (x86_64)
--
-- Host: localhost    Database: kthfs
-- ------------------------------------------------------
-- Server version	5.6.23-log

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
  `activity` varchar(128) NOT NULL,
  `user_id` int(10) NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `flag` varchar(128) DEFAULT NULL,
  `project_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `project_id` (`project_id`),
  CONSTRAINT `activity_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`uid`),
  CONSTRAINT `activity_ibfk_2` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=125 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `activity`
--

LOCK TABLES `activity` WRITE;
/*!40000 ALTER TABLE `activity` DISABLE KEYS */;
INSERT INTO `activity` VALUES (1,' created a new project ',10000,'2015-04-28 20:00:37','project',2),(2,' created a new project ',10000,'2015-04-28 20:06:35','project',3),(3,' created a new project ',10000,'2015-04-28 20:06:51','project',4),(4,'added a new dataset ',10000,'2015-04-29 08:06:51','project',3),(5,' created a new project ',10000,'2015-04-29 11:45:02','project',5),(12,' created a new project ',10000,'2015-04-29 14:12:07','project',12),(15,' created a new project ',10000,'2015-04-29 14:18:49','project',13),(19,' created a new project ',10000,'2015-04-29 14:21:02','project',14),(23,' created a new project ',10000,'2015-04-29 14:25:15','project',15),(27,' created a new project ',10000,'2015-04-29 14:28:40','project',16),(28,' added a member ',10000,'2015-04-29 14:28:40','project',16),(29,' added a member ',10000,'2015-04-29 14:28:40','project',16),(30,' added a member ',10000,'2015-04-29 14:28:40','project',16),(31,' created a new project ',10000,'2015-04-29 14:36:08','project',17),(32,' added a member ',10000,'2015-04-29 14:36:08','project',17),(33,' created a new project ',10000,'2015-04-29 17:19:37','project',18),(34,' added a member ',10000,'2015-04-29 17:19:37','project',18),(35,' created a new project ',10000,'2015-04-29 17:34:55','project',19),(36,' added a member ',10000,'2015-04-29 17:34:55','project',19),(37,' added a member ',10000,'2015-04-29 17:34:55','project',19),(38,' created a new project ',10007,'2015-04-29 17:44:17','project',20),(39,' added a member ',10007,'2015-04-29 17:44:17','project',20),(40,' created a new project ',10000,'2015-04-29 18:02:50','project',21),(41,' added a member test3@kth.se',10000,'2015-04-29 18:02:50','project',21),(42,' created a new project ',10000,'2015-04-29 18:42:03','project',22),(43,' added a member asd@kth.se',10000,'2015-04-29 18:42:03','project',22),(44,' added a member test3@kth.se',10000,'2015-04-29 18:42:03','project',22),(45,' created a new project ',10000,'2015-04-29 18:49:58','project',23),(46,' added a member test6@kth.se',10000,'2015-04-29 18:49:58','project',23),(47,' created a new project ',10000,'2015-04-29 19:01:12','project',24),(48,' added a member test1@kth.se',10000,'2015-04-29 19:01:13','project',24),(49,' added a member test3@kth.se',10000,'2015-04-29 19:01:13','project',24),(50,' created a new project ',10000,'2015-04-29 19:08:08','project',25),(53,' created a new project ',10000,'2015-04-29 19:18:05','project',26),(56,' created a new project ',10000,'2015-04-29 19:21:14','project',27),(57,' added a member test3@kth.se',10000,'2015-04-29 19:21:14','project',27),(58,' added a member test1@kth.se',10000,'2015-04-29 19:21:14','project',27),(59,' created a new project ',10000,'2015-04-29 19:40:42','project',28),(60,' added a member test1@kth.se',10000,'2015-04-29 19:40:42','project',28),(61,' added a member asefs@kth.se',10000,'2015-04-29 19:40:42','project',28),(62,' added a member test2@kth.se',10000,'2015-04-29 19:40:42','project',28),(63,' created a new project ',10000,'2015-04-29 19:43:48','project',29),(64,' added a member test1@kth.se',10000,'2015-04-29 19:43:48','project',29),(65,' added a member test2@kth.se',10000,'2015-04-29 19:43:49','project',29),(66,' created a new project ',10000,'2015-04-30 11:54:20','project',30),(67,' added a member test2@kth.se',10000,'2015-04-30 11:54:20','project',30),(68,' changed projectname ',10000,'2015-04-30 13:43:21','project',24),(69,' added new services ',10000,'2015-04-30 13:43:21','project',24),(70,' added new services ',10000,'2015-05-08 10:23:29','project',3),(71,' added new services ',10000,'2015-05-08 10:24:10','project',3),(72,' created a new project ',10000,'2015-05-08 11:12:41','project',31),(73,' added new services ',10000,'2015-05-08 11:12:41','project',31),(74,' added new services ',10000,'2015-05-08 11:13:32','project',31),(75,' added new services ',10000,'2015-05-08 12:07:15','project',12),(76,' added new services ',10000,'2015-05-08 12:07:41','project',12),(77,' created a new project ',10000,'2015-05-08 12:18:16','project',32),(78,' added new services ',10000,'2015-05-08 12:18:16','project',32),(79,' added a member test1@kth.se',10000,'2015-05-08 12:18:16','project',32),(80,' added new services ',10000,'2015-05-08 13:28:09','project',3),(81,' added new services ',10000,'2015-05-08 13:28:23','project',3),(82,' added new services ',10000,'2015-05-08 13:28:31','project',3),(83,' added new services ',10000,'2015-05-08 13:28:37','project',3),(84,' added new services ',10000,'2015-05-08 13:28:43','project',3),(85,' added new services ',10000,'2015-05-08 13:30:14','project',3),(86,' added new services ',10000,'2015-05-08 13:30:21','project',3),(87,' added new services ',10000,'2015-05-08 13:50:06','project',16),(88,' created a new project ',10000,'2015-05-08 13:56:06','project',33),(89,' added new services ',10000,'2015-05-08 13:56:06','project',33),(90,' added a member test1@kth.se',10000,'2015-05-08 13:56:06','project',33),(91,' added a member test3@kth.se',10000,'2015-05-08 13:56:06','project',33),(92,' added a member test4@kth.se',10000,'2015-05-08 13:56:06','project',33),(93,' added new services ',10000,'2015-05-08 14:01:57','project',17),(94,' added a member test3@kth.se',10000,'2015-05-08 16:56:19','project',2),(95,' added a member test2@kth.se',10000,'2015-05-08 16:57:55','project',2),(96,' removed team member test2@kth.se',10000,'2015-05-08 17:02:02','project',2),(97,' added a member test2@kth.se',10000,'2015-05-08 17:10:45','project',2),(98,' removed team member test3@kth.se',10000,'2015-05-08 17:10:45','project',2),(99,' removed team member test2@kth.se',10000,'2015-05-08 17:15:36','project',2),(100,' added a member test2@kth.se',10000,'2015-05-08 17:15:36','project',2),(101,' added a member test3@kth.se',10000,'2015-05-08 17:19:07','project',2),(102,' removed team member test2@kth.se',10000,'2015-05-08 17:19:07','project',2),(103,' added a member test2@kth.se',10000,'2015-05-08 17:27:12','project',2),(104,' removed team member test2@kth.se',10000,'2015-05-08 17:27:12','project',2),(105,' added a member test2@kth.se',10000,'2015-05-10 11:48:22','project',3),(106,' added a member test2@kth.se',10000,'2015-05-10 12:02:48','project',2),(107,' removed team member test2@kth.se',10000,'2015-05-10 12:03:30','project',2),(108,' added a member test4@kth.se',10000,'2015-05-10 12:25:15','project',2),(109,' added a member test2@kth.se',10000,'2015-05-10 12:37:06','project',2),(110,' removed team member test2@kth.se',10000,'2015-05-10 12:39:50','project',2),(111,' added a member test2@kth.se',10000,'2015-05-10 12:39:50','project',2),(112,' changed the role of test3@kth.se',10000,'2015-05-10 12:52:59','project',2),(113,' added new services ',10000,'2015-05-11 11:44:54','PROJECT',33),(114,' added new services ',10000,'2015-05-11 11:45:04','PROJECT',33),(115,' added new services ',10000,'2015-05-11 12:54:25','PROJECT',13),(116,' added a member test2@kth.se',10000,'2015-05-11 12:55:06','PROJECT',13),(117,' added a member test3@kth.se',10000,'2015-05-11 12:55:06','PROJECT',13),(118,' added a member test4@kth.se',10000,'2015-05-11 12:55:06','PROJECT',13),(119,' changed the role of test3@kth.se',10000,'2015-05-11 12:55:06','PROJECT',13),(120,' changed project description.',10000,'2015-05-11 13:41:10','PROJECT',12),(121,' changed project description.',10000,'2015-05-11 13:47:05','PROJECT',12),(122,' changed project description.',10000,'2015-05-11 13:52:04','PROJECT',15),(123,' changed project description.',10000,'2015-05-11 14:01:04','PROJECT',12),(124,' changed project description.',10000,'2015-05-11 14:28:13','PROJECT',12);
/*!40000 ALTER TABLE `activity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `address`
--

DROP TABLE IF EXISTS `address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `address` (
  `address1` varchar(50) DEFAULT NULL,
  `address2` varchar(50) DEFAULT NULL,
  `address3` varchar(50) DEFAULT NULL,
  `city` varchar(30) DEFAULT NULL,
  `state` varchar(100) DEFAULT NULL,
  `country` varchar(40) DEFAULT NULL,
  `postalcode` varchar(10) DEFAULT NULL,
  `address_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uid` int(10) NOT NULL,
  PRIMARY KEY (`address_id`),
  KEY `uid` (`uid`),
  CONSTRAINT `address_ibfk_1` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `address`
--

LOCK TABLES `address` WRITE;
/*!40000 ALTER TABLE `address` DISABLE KEYS */;
INSERT INTO `address` VALUES ('Admin Office','Teknikringen 14','Vallhalevägen','Stockholm län','Stockholm','Sverige','10044',1,10000),('Test1','Street Nr1','Street1','City1','State1','Sverige','10041',2,10001),('Test2','Street Nr2','Street2','City2','State2','Sverige','10042',3,10002),('Test3','Street Nr3','Street3','City3','State3','Sverige','10043',4,10003),('Test4','Street Nr4','Street4','City4','State4','Sverige','10044',5,10004),('Test5','Street Nr5','Street5','City5','State5','Sverige','10045',6,10005),('Test6','Street Nr6','Street6','City6','State6','Sverige','10046',7,10006);
/*!40000 ALTER TABLE `address` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `anatomical_parts`
--

DROP TABLE IF EXISTS `anatomical_parts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `anatomical_parts` (
  `id` int(16) NOT NULL AUTO_INCREMENT,
  `ontology_name` varchar(255) DEFAULT NULL,
  `ontology_version` varchar(255) DEFAULT NULL,
  `ontology_code` varchar(255) DEFAULT NULL,
  `ontology_description` varchar(1000) DEFAULT NULL,
  `explanation` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `anatomical_parts`
--

LOCK TABLES `anatomical_parts` WRITE;
/*!40000 ALTER TABLE `anatomical_parts` DISABLE KEYS */;
/*!40000 ALTER TABLE `anatomical_parts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bbc_group`
--

DROP TABLE IF EXISTS `bbc_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bbc_group` (
  `group_name` varchar(20) NOT NULL,
  `group_desc` varchar(200) DEFAULT NULL,
  `gid` int(11) NOT NULL,
  PRIMARY KEY (`gid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bbc_group`
--

LOCK TABLES `bbc_group` WRITE;
/*!40000 ALTER TABLE `bbc_group` DISABLE KEYS */;
INSERT INTO `bbc_group` VALUES ('BBC_ADMIN','Data Owner',1001),('BBC_RESEARCHER','Users to run experiment',1002),('BBC_GUEST','New users',1003),('AUDITOR','To audit the platform',1004),('SYS_ADMIN','Platform administrator',1005),('BBC_USER','Registered users in the system',1006);
/*!40000 ALTER TABLE `bbc_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `collection_type`
--

DROP TABLE IF EXISTS `collection_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `collection_type` (
  `collection_id` varchar(255) NOT NULL,
  `type` varchar(128) NOT NULL,
  PRIMARY KEY (`collection_id`,`type`),
  CONSTRAINT `collection_type_ibfk_1` FOREIGN KEY (`collection_id`) REFERENCES `samplecollections` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `collection_type`
--

LOCK TABLES `collection_type` WRITE;
/*!40000 ALTER TABLE `collection_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `collection_type` ENABLE KEYS */;
UNLOCK TABLES;

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
  `status` varchar(30) DEFAULT NULL,
  `name` varchar(80) DEFAULT NULL,
  `type` varchar(30) DEFAULT NULL,
  `consent_form` longblob,
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`),
  CONSTRAINT `consent_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `consent`
--

LOCK TABLES `consent` WRITE;
/*!40000 ALTER TABLE `consent` DISABLE KEYS */;
/*!40000 ALTER TABLE `consent` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `diseases`
--

DROP TABLE IF EXISTS `diseases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `diseases` (
  `id` int(16) NOT NULL AUTO_INCREMENT,
  `ontology_name` varchar(255) DEFAULT NULL,
  `ontology_version` varchar(255) DEFAULT NULL,
  `ontology_code` varchar(255) DEFAULT NULL,
  `ontology_description` varchar(1000) DEFAULT NULL,
  `explanation` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `diseases`
--

LOCK TABLES `diseases` WRITE;
/*!40000 ALTER TABLE `diseases` DISABLE KEYS */;
/*!40000 ALTER TABLE `diseases` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fields`
--

DROP TABLE IF EXISTS `fields`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fields` (
  `fieldid` int(11) NOT NULL AUTO_INCREMENT,
  `maxsize` int(11) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `required` smallint(6) DEFAULT NULL,
  `searchable` smallint(6) DEFAULT NULL,
  `tableid` int(11) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`fieldid`),
  KEY `FK_fields_tableid` (`tableid`),
  CONSTRAINT `FK_fields_tableid` FOREIGN KEY (`tableid`) REFERENCES `tables` (`tableid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fields`
--

LOCK TABLES `fields` WRITE;
/*!40000 ALTER TABLE `fields` DISABLE KEYS */;
/*!40000 ALTER TABLE `fields` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `job_execution_files`
--

DROP TABLE IF EXISTS `job_execution_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `job_execution_files` (
  `job_id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `path` varchar(255) NOT NULL,
  PRIMARY KEY (`job_id`,`name`),
  CONSTRAINT `job_execution_files_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `job_execution_files`
--

LOCK TABLES `job_execution_files` WRITE;
/*!40000 ALTER TABLE `job_execution_files` DISABLE KEYS */;
/*!40000 ALTER TABLE `job_execution_files` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `job_input_files`
--

DROP TABLE IF EXISTS `job_input_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `job_input_files` (
  `job_id` int(11) NOT NULL,
  `path` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`job_id`,`name`),
  CONSTRAINT `job_input_files_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `job_input_files`
--

LOCK TABLES `job_input_files` WRITE;
/*!40000 ALTER TABLE `job_input_files` DISABLE KEYS */;
/*!40000 ALTER TABLE `job_input_files` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `job_output_files`
--

DROP TABLE IF EXISTS `job_output_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `job_output_files` (
  `job_id` int(11) NOT NULL,
  `path` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`job_id`,`name`),
  CONSTRAINT `job_output_files_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `job_output_files`
--

LOCK TABLES `job_output_files` WRITE;
/*!40000 ALTER TABLE `job_output_files` DISABLE KEYS */;
/*!40000 ALTER TABLE `job_output_files` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jobhistory`
--

DROP TABLE IF EXISTS `jobhistory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `jobhistory` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) DEFAULT NULL,
  `submission_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `project_id` int(11) NOT NULL,
  `user` varchar(45) NOT NULL,
  `state` varchar(128) NOT NULL,
  `execution_duration` bigint(20) DEFAULT NULL,
  `args` varchar(255) DEFAULT NULL,
  `stdout_path` varchar(255) DEFAULT NULL,
  `stderr_path` varchar(255) DEFAULT NULL,
  `type` varchar(128) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`),
  KEY `user` (`user`),
  CONSTRAINT `jobhistory_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `jobhistory_ibfk_2` FOREIGN KEY (`user`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jobhistory`
--

LOCK TABLES `jobhistory` WRITE;
/*!40000 ALTER TABLE `jobhistory` DISABLE KEYS */;
/*!40000 ALTER TABLE `jobhistory` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `organization`
--

DROP TABLE IF EXISTS `organization`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `organization` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uid` int(11) DEFAULT NULL,
  `org_name` varchar(100) DEFAULT NULL,
  `website` varchar(200) DEFAULT NULL,
  `contact_person` varchar(100) DEFAULT NULL,
  `contact_email` varchar(100) DEFAULT NULL,
  `department` varchar(100) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `fax` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_Organization` (`uid`),
  CONSTRAINT `fk_Organization` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `organization`
--

LOCK TABLES `organization` WRITE;
/*!40000 ALTER TABLE `organization` DISABLE KEYS */;
INSERT INTO `organization` VALUES (1,10000,'KI','www.ki.se','Admin Admin','admin@ki.se','MEB','+4670080010','+4670080015'),(2,10001,'KI','www.ki.se','Admin Admin','admin@ki.se','MEB','+4670080010','+4670080015'),(3,10002,'KI','www.ki.se','Admin Admin','admin@ki.se','MEB','+4670080010','+4670080015'),(4,10003,'KI','www.ki.se','Admin Admin','admin@ki.se','MEB','+4670080010','+4670080015'),(5,10004,'KI','www.ki.se','Admin Admin','admin@ki.se','MEB','+4670080010','+4670080015'),(6,10005,'KI','www.ki.se','Admin Admin','admin@ki.se','MEB','+4670080010','+4670080015'),(7,10006,'KI','www.ki.se','Admin Admin','admin@ki.se','MEB','+4670080010','+4670080015');
/*!40000 ALTER TABLE `organization` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `people_group`
--

LOCK TABLES `people_group` WRITE;
/*!40000 ALTER TABLE `people_group` DISABLE KEYS */;
INSERT INTO `people_group` VALUES (10000,1003),(10001,1003),(10002,1003),(10003,1003),(10004,1003),(10005,1003),(10006,1003),(10000,1005),(10007,1006);
/*!40000 ALTER TABLE `people_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project`
--

DROP TABLE IF EXISTS `project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `projectname` varchar(128) NOT NULL,
  `username` varchar(45) NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `retention_period` date DEFAULT NULL,
  `ethical_status` varchar(30) DEFAULT NULL,
  `archived` tinyint(1) DEFAULT '0',
  `deleted` tinyint(1) DEFAULT '0',
  `description` varchar(3000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`,`projectname`),
  CONSTRAINT `project_ibfk_1` FOREIGN KEY (`username`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project`
--

LOCK TABLES `project` WRITE;
/*!40000 ALTER TABLE `project` DISABLE KEYS */;
INSERT INTO `project` VALUES (2,'project','ermiasg@kth.se','2015-04-28 20:00:37',NULL,NULL,0,NULL,NULL),(3,'project1','ermiasg@kth.se','2015-04-28 20:06:35',NULL,NULL,0,NULL,NULL),(4,'project2','ermiasg@kth.se','2015-04-28 20:06:51',NULL,NULL,0,NULL,NULL),(5,'project3','ermiasg@kth.se','2015-04-29 11:45:02',NULL,NULL,0,NULL,NULL),(12,'project4','ermiasg@kth.se','2015-04-29 14:12:07',NULL,NULL,0,NULL,'this is a test update!'),(13,'project5','ermiasg@kth.se','2015-04-29 14:18:49',NULL,NULL,0,NULL,NULL),(14,'project6','ermiasg@kth.se','2015-04-29 14:21:02',NULL,NULL,0,NULL,NULL),(15,'project7','ermiasg@kth.se','2015-04-29 14:25:15',NULL,NULL,0,NULL,NULL),(16,'project8','ermiasg@kth.se','2015-04-29 14:28:40',NULL,NULL,0,NULL,NULL),(17,'project9','ermiasg@kth.se','2015-04-29 14:36:08',NULL,NULL,0,NULL,NULL),(18,'proj','ermiasg@kth.se','2015-04-29 17:19:37',NULL,NULL,0,NULL,NULL),(19,'proj1','ermiasg@kth.se','2015-04-29 17:34:55',NULL,NULL,0,NULL,NULL),(20,'project','asd@kth.se','2015-04-29 17:44:17',NULL,NULL,0,NULL,NULL),(21,'proj3','ermiasg@kth.se','2015-04-29 18:02:50',NULL,NULL,0,NULL,NULL),(22,'proj28','ermiasg@kth.se','2015-04-29 18:42:03',NULL,NULL,0,NULL,NULL),(23,'project20','ermiasg@kth.se','2015-04-29 18:49:58',NULL,NULL,0,NULL,NULL),(24,'project223','ermiasg@kth.se','2015-04-29 19:01:12',NULL,NULL,0,NULL,NULL),(25,'proj100','ermiasg@kth.se','2015-04-29 19:08:08',NULL,NULL,0,NULL,NULL),(26,'proj101','ermiasg@kth.se','2015-04-29 19:18:05',NULL,NULL,0,NULL,NULL),(27,'proj102','ermiasg@kth.se','2015-04-29 19:21:14',NULL,NULL,0,NULL,NULL),(28,'project103','ermiasg@kth.se','2015-04-29 19:40:42',NULL,NULL,0,NULL,NULL),(29,'project104','ermiasg@kth.se','2015-04-29 19:43:48',NULL,NULL,0,NULL,NULL),(30,'proje100','ermiasg@kth.se','2015-04-30 11:54:20',NULL,NULL,0,NULL,NULL),(31,'Ermias','ermiasg@kth.se','2015-05-08 11:12:41',NULL,NULL,0,NULL,NULL),(32,'testProject','ermiasg@kth.se','2015-05-08 12:18:16',NULL,NULL,0,NULL,NULL),(33,'seif','ermiasg@kth.se','2015-05-08 13:56:06',NULL,NULL,0,NULL,NULL);
/*!40000 ALTER TABLE `project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_design`
--

DROP TABLE IF EXISTS `project_design`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_design` (
  `project_id` int(11) NOT NULL,
  `design` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`project_id`),
  CONSTRAINT `project_design_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project_meta` (`project_id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_design`
--

LOCK TABLES `project_design` WRITE;
/*!40000 ALTER TABLE `project_design` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_design` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_inclusion_criteria`
--

DROP TABLE IF EXISTS `project_inclusion_criteria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_inclusion_criteria` (
  `project_id` int(11) NOT NULL,
  `criterium` varchar(128) NOT NULL,
  PRIMARY KEY (`project_id`,`criterium`),
  CONSTRAINT `project_inclusion_criteria_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project_meta` (`project_id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_inclusion_criteria`
--

LOCK TABLES `project_inclusion_criteria` WRITE;
/*!40000 ALTER TABLE `project_inclusion_criteria` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_inclusion_criteria` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_meta`
--

DROP TABLE IF EXISTS `project_meta`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_meta` (
  `project_id` int(11) NOT NULL,
  `description` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`project_id`),
  CONSTRAINT `project_meta_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_meta`
--

LOCK TABLES `project_meta` WRITE;
/*!40000 ALTER TABLE `project_meta` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_meta` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_services`
--

DROP TABLE IF EXISTS `project_services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_services` (
  `project_id` int(11) NOT NULL,
  `service` varchar(32) NOT NULL,
  PRIMARY KEY (`project_id`,`service`),
  CONSTRAINT `project_services_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_services`
--

LOCK TABLES `project_services` WRITE;
/*!40000 ALTER TABLE `project_services` DISABLE KEYS */;
INSERT INTO `project_services` VALUES (2,'ADAM'),(2,'SAMPLES'),(2,'SPARK'),(2,'ZEPPELIN'),(3,'ADAM'),(3,'CUNEIFORM'),(3,'MAPREDUCE'),(3,'SAMPLES'),(3,'SPARK'),(3,'YARN'),(3,'ZEPPELIN'),(4,'SAMPLES'),(4,'ZEPPELIN'),(5,'MAPREDUCE'),(5,'SPARK'),(5,'YARN'),(12,'ADAM'),(12,'MAPREDUCE'),(12,'SAMPLES'),(12,'SPARK'),(12,'YARN'),(13,'ADAM'),(13,'SAMPLES'),(13,'SPARK'),(13,'YARN'),(14,'ADAM'),(14,'SAMPLES'),(14,'SPARK'),(15,'ADAM'),(15,'SAMPLES'),(15,'SPARK'),(16,'ADAM'),(16,'SAMPLES'),(16,'SPARK'),(16,'ZEPPELIN'),(17,'ADAM'),(17,'SAMPLES'),(17,'ZEPPELIN'),(18,'SPARK'),(19,'ADAM'),(19,'SPARK'),(20,'SAMPLES'),(20,'ZEPPELIN'),(24,'ADAM'),(24,'CUNEIFORM'),(24,'SPARK'),(24,'ZEPPELIN'),(25,'MAPREDUCE'),(25,'YARN'),(26,'MAPREDUCE'),(26,'YARN'),(27,'MAPREDUCE'),(27,'YARN'),(28,'ADAM'),(28,'MAPREDUCE'),(28,'YARN'),(29,'ADAM'),(29,'MAPREDUCE'),(29,'YARN'),(30,'ADAM'),(30,'MAPREDUCE'),(30,'YARN'),(31,'ADAM'),(31,'MAPREDUCE'),(31,'SPARK'),(32,'SAMPLES'),(32,'YARN'),(32,'ZEPPELIN'),(33,'ADAM'),(33,'CUNEIFORM'),(33,'MAPREDUCE'),(33,'SAMPLES'),(33,'SPARK'),(33,'YARN'),(33,'ZEPPELIN');
/*!40000 ALTER TABLE `project_services` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_team`
--

DROP TABLE IF EXISTS `project_team`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_team` (
  `project_id` int(11) NOT NULL,
  `team_member` varchar(45) NOT NULL,
  `team_role` varchar(32) NOT NULL,
  `added` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`project_id`,`team_member`),
  KEY `team_member` (`team_member`),
  CONSTRAINT `project_team_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `project_team_ibfk_2` FOREIGN KEY (`team_member`) REFERENCES `users` (`email`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_team`
--

LOCK TABLES `project_team` WRITE;
/*!40000 ALTER TABLE `project_team` DISABLE KEYS */;
INSERT INTO `project_team` VALUES (2,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(2,'test2@kth.se','Data owner','2015-05-11 12:45:15'),(2,'test3@kth.se','Data scientist','2015-05-11 12:47:01'),(2,'test4@kth.se','Data owner','2015-05-11 12:45:15'),(3,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(3,'test2@kth.se','Data owner','2015-05-11 12:45:15'),(4,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(5,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(12,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(13,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(13,'test2@kth.se','Data owner','2015-05-11 12:55:06'),(13,'test3@kth.se','Data scientist','2015-05-11 12:55:06'),(13,'test4@kth.se','Data owner','2015-05-11 12:55:06'),(14,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(15,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(16,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(16,'test1@kth.se','Data scientist','2015-05-11 12:47:01'),(16,'test2@kth.se','Data scientist','2015-05-11 12:47:01'),(16,'test3@kth.se','Data scientist','2015-05-11 12:47:01'),(17,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(17,'test1@kth.se','Data scientist','2015-05-11 12:47:01'),(18,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(18,'test1@kth.se','Data scientist','2015-05-11 12:47:01'),(19,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(19,'test1@kth.se','Data scientist','2015-05-11 12:47:01'),(19,'test3@kth.se','Data scientist','2015-05-11 12:47:01'),(20,'asd@kth.se','Data owner','2015-05-11 12:45:15'),(20,'ermiasg@kth.se','Data scientist','2015-05-11 12:47:01'),(21,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(21,'test3@kth.se','Data scientist','2015-05-11 12:47:01'),(22,'asd@kth.se','Data scientist','2015-05-11 12:47:01'),(22,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(22,'test3@kth.se','Data scientist','2015-05-11 12:47:01'),(23,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(23,'test6@kth.se','Data scientist','2015-05-11 12:47:01'),(24,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(24,'test1@kth.se','Data scientist','2015-05-11 12:47:01'),(24,'test3@kth.se','Data scientist','2015-05-11 12:47:01'),(25,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(26,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(27,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(27,'test1@kth.se','Data scientist','2015-05-11 12:47:01'),(27,'test3@kth.se','Data scientist','2015-05-11 12:47:01'),(28,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(28,'test1@kth.se','Data scientist','2015-05-11 12:47:01'),(28,'test2@kth.se','Data scientist','2015-05-11 12:47:01'),(29,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(29,'test1@kth.se','Data scientist','2015-05-11 12:47:01'),(29,'test2@kth.se','Data scientist','2015-05-11 12:47:01'),(30,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(30,'test2@kth.se','Data scientist','2015-05-11 12:47:01'),(31,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(32,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(32,'test1@kth.se','Data scientist','2015-05-11 12:47:01'),(33,'ermiasg@kth.se','Data owner','2015-05-11 12:45:15'),(33,'test1@kth.se','Data scientist','2015-05-11 12:47:01'),(33,'test3@kth.se','Data scientist','2015-05-11 12:47:01'),(33,'test4@kth.se','Data scientist','2015-05-11 12:47:01');
/*!40000 ALTER TABLE `project_team` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `raw_data`
--

DROP TABLE IF EXISTS `raw_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `raw_data` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `data` longtext,
  `fieldid` int(11) DEFAULT NULL,
  `tupleid` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_raw_data_fieldid` (`fieldid`),
  CONSTRAINT `FK_raw_data_fieldid` FOREIGN KEY (`fieldid`) REFERENCES `fields` (`fieldid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `raw_data`
--

LOCK TABLES `raw_data` WRITE;
/*!40000 ALTER TABLE `raw_data` DISABLE KEYS */;
/*!40000 ALTER TABLE `raw_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sample_material`
--

DROP TABLE IF EXISTS `sample_material`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sample_material` (
  `sample_id` varchar(255) NOT NULL,
  `type` varchar(128) NOT NULL,
  PRIMARY KEY (`sample_id`,`type`),
  CONSTRAINT `sample_material_ibfk_1` FOREIGN KEY (`sample_id`) REFERENCES `samples` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sample_material`
--

LOCK TABLES `sample_material` WRITE;
/*!40000 ALTER TABLE `sample_material` DISABLE KEYS */;
/*!40000 ALTER TABLE `sample_material` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `samplecollection_disease`
--

DROP TABLE IF EXISTS `samplecollection_disease`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `samplecollection_disease` (
  `collection_id` varchar(255) NOT NULL,
  `disease_id` int(16) NOT NULL,
  PRIMARY KEY (`collection_id`,`disease_id`),
  KEY `disease_id` (`disease_id`),
  CONSTRAINT `samplecollection_disease_ibfk_1` FOREIGN KEY (`disease_id`) REFERENCES `diseases` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `samplecollection_disease_ibfk_2` FOREIGN KEY (`collection_id`) REFERENCES `samplecollections` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `samplecollection_disease`
--

LOCK TABLES `samplecollection_disease` WRITE;
/*!40000 ALTER TABLE `samplecollection_disease` DISABLE KEYS */;
/*!40000 ALTER TABLE `samplecollection_disease` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `samplecollection_type`
--

DROP TABLE IF EXISTS `samplecollection_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `samplecollection_type` (
  `collection_id` varchar(255) NOT NULL,
  `type` varchar(128) NOT NULL,
  PRIMARY KEY (`collection_id`,`type`),
  CONSTRAINT `samplecollection_type_ibfk_1` FOREIGN KEY (`collection_id`) REFERENCES `samplecollections` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `samplecollection_type`
--

LOCK TABLES `samplecollection_type` WRITE;
/*!40000 ALTER TABLE `samplecollection_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `samplecollection_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `samplecollections`
--

DROP TABLE IF EXISTS `samplecollections`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `samplecollections` (
  `id` varchar(255) NOT NULL,
  `acronym` varchar(255) NOT NULL,
  `name` varchar(1024) NOT NULL,
  `description` varchar(2000) DEFAULT NULL,
  `contact` varchar(45) NOT NULL,
  `project_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `acronym` (`acronym`),
  KEY `project_id` (`project_id`),
  KEY `contact` (`contact`),
  CONSTRAINT `samplecollections_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `samplecollections_ibfk_2` FOREIGN KEY (`contact`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `samplecollections`
--

LOCK TABLES `samplecollections` WRITE;
/*!40000 ALTER TABLE `samplecollections` DISABLE KEYS */;
/*!40000 ALTER TABLE `samplecollections` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `samples`
--

DROP TABLE IF EXISTS `samples`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `samples` (
  `samplecollection_id` varchar(255) NOT NULL,
  `id` varchar(255) NOT NULL,
  `parent_id` varchar(255) DEFAULT NULL,
  `sampled_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `anatomical_site` int(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `anatomical_site` (`anatomical_site`),
  KEY `parent_id` (`parent_id`),
  KEY `samplecollection_id` (`samplecollection_id`),
  CONSTRAINT `samples_ibfk_1` FOREIGN KEY (`anatomical_site`) REFERENCES `anatomical_parts` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `samples_ibfk_2` FOREIGN KEY (`parent_id`) REFERENCES `samples` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `samples_ibfk_3` FOREIGN KEY (`samplecollection_id`) REFERENCES `samplecollections` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `samples`
--

LOCK TABLES `samples` WRITE;
/*!40000 ALTER TABLE `samples` DISABLE KEYS */;
/*!40000 ALTER TABLE `samples` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tables`
--

DROP TABLE IF EXISTS `tables`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tables` (
  `tableid` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `templateid` int(11) NOT NULL,
  PRIMARY KEY (`tableid`),
  KEY `FK_tables_templateid` (`templateid`),
  CONSTRAINT `FK_tables_templateid` FOREIGN KEY (`templateid`) REFERENCES `templates` (`templateid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tables`
--

LOCK TABLES `tables` WRITE;
/*!40000 ALTER TABLE `tables` DISABLE KEYS */;
/*!40000 ALTER TABLE `tables` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `templates`
--

DROP TABLE IF EXISTS `templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `templates` (
  `templateid` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(250) NOT NULL,
  PRIMARY KEY (`templateid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `templates`
--

LOCK TABLES `templates` WRITE;
/*!40000 ALTER TABLE `templates` DISABLE KEYS */;
/*!40000 ALTER TABLE `templates` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tuple_to_file`
--

LOCK TABLES `tuple_to_file` WRITE;
/*!40000 ALTER TABLE `tuple_to_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `tuple_to_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `userlogins`
--

DROP TABLE IF EXISTS `userlogins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userlogins` (
  `login_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ip` varchar(16) DEFAULT NULL,
  `os` varchar(30) DEFAULT NULL,
  `browser` varchar(40) DEFAULT NULL,
  `action` varchar(80) DEFAULT NULL,
  `outcome` varchar(20) DEFAULT NULL,
  `uid` int(10) NOT NULL,
  `login_date` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`login_id`),
  KEY `login_date` (`login_date`),
  KEY `uid` (`uid`),
  CONSTRAINT `userlogins_ibfk_1` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `userlogins`
--

LOCK TABLES `userlogins` WRITE;
/*!40000 ALTER TABLE `userlogins` DISABLE KEYS */;
INSERT INTO `userlogins` VALUES (1,'127.0.0.1',NULL,'Firefox','AUTHENTICATION',NULL,10000,'2015-04-28 15:18:42'),(2,'127.0.0.1',NULL,'Firefox','AUTHENTICATION',NULL,10001,'2015-04-28 15:19:07'),(3,'127.0.0.1',NULL,'Firefox','AUTHENTICATION',NULL,10002,'2015-04-28 15:19:07'),(4,'127.0.0.1',NULL,'Firefox','AUTHENTICATION',NULL,10003,'2015-04-28 15:19:07'),(5,'127.0.0.1',NULL,'Firefox','AUTHENTICATION',NULL,10004,'2015-04-28 15:19:07'),(6,'127.0.0.1',NULL,'Firefox','AUTHENTICATION',NULL,10005,'2015-04-28 15:19:07'),(7,'127.0.0.1',NULL,'Firefox','AUTHENTICATION',NULL,10006,'2015-04-28 15:19:07'),(8,'127.0.0.1',NULL,'Chrome','False login',NULL,10000,'2015-04-30 17:20:00'),(9,'127.0.0.1',NULL,'Chrome','False login',NULL,10000,'2015-04-30 17:21:50'),(10,'127.0.0.1',NULL,'Chrome','False login',NULL,10000,'2015-04-30 17:22:25'),(11,'127.0.0.1',NULL,'Chrome','False login',NULL,10000,'2015-04-30 17:22:47'),(12,'127.0.0.1',NULL,'Chrome','False login',NULL,10000,'2015-04-30 17:22:48'),(13,'127.0.0.1',NULL,'Chrome','False login',NULL,10000,'2015-04-30 17:23:00'),(14,'127.0.0.1',NULL,'Chrome','False login',NULL,10000,'2015-04-30 17:23:15'),(15,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-04-30 17:26:39'),(16,'127.0.0.1',NULL,'Safari','AUTHENTICATION',NULL,10000,'2015-04-30 19:24:53'),(17,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-04-30 19:25:21'),(18,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-06 11:33:44'),(19,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-06 14:08:51'),(20,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-06 15:33:28'),(21,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-06 16:31:33'),(22,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-06 17:32:44'),(23,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-06 18:46:08'),(24,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-07 07:13:15'),(25,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-07 10:30:57'),(26,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-07 13:10:37'),(27,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-07 15:22:38'),(28,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-07 17:20:03'),(29,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-08 10:22:54'),(30,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-08 12:57:34'),(31,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-08 13:27:55'),(32,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-08 13:52:13'),(33,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-08 15:12:44'),(34,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-08 16:52:25'),(35,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-10 11:47:37'),(36,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-10 13:25:05'),(37,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-11 09:49:49'),(38,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-11 10:20:16'),(39,'127.0.0.1',NULL,'Firefox','Successful login',NULL,10000,'2015-05-11 10:23:27'),(40,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-11 10:49:07'),(41,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-11 11:42:41'),(42,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-11 12:51:57'),(43,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-11 13:38:35'),(44,'127.0.0.1',NULL,'Chrome','Successful login',NULL,10000,'2015-05-11 13:51:12');
/*!40000 ALTER TABLE `userlogins` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `uid` int(10) NOT NULL DEFAULT '1000',
  `username` varchar(10) NOT NULL,
  `password` varchar(128) NOT NULL,
  `email` varchar(45) DEFAULT NULL,
  `fname` varchar(30) DEFAULT NULL,
  `lname` varchar(30) DEFAULT NULL,
  `activated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `title` varchar(10) DEFAULT NULL,
  `orcid` varchar(20) DEFAULT NULL,
  `false_login` int(11) NOT NULL DEFAULT '-1',
  `isonline` tinyint(1) NOT NULL DEFAULT '0',
  `secret` varchar(20) DEFAULT NULL,
  `validation_key` varchar(128) DEFAULT NULL,
  `security_question` varchar(20) DEFAULT NULL,
  `security_answer` varchar(128) DEFAULT NULL,
  `yubikey_user` int(11) NOT NULL DEFAULT '0',
  `password_changed` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `notes` varchar(500) DEFAULT NULL,
  `mobile` varchar(20) DEFAULT NULL,
  `status` int(11) NOT NULL DEFAULT '-1',
  PRIMARY KEY (`uid`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (10000,'meb10000','8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','ermiasg@kth.se','Ermias','Gebremskel','2015-04-30 19:25:20','Mr','1234-5678-1234-5678',0,0,'V3WBPS4G2WMQ53VA',NULL,'FRIEND','8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918',12,'2015-04-28 15:18:42',NULL,'046737390119',4),(10001,'meb10001','8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','test1@kth.se','Test1','Test1','2015-04-28 15:20:34','Mr','1234-5678-1234-5678',-1,0,'V3WBPS4G2WMQ53VA',NULL,'FRIEND','8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918',12,'2015-04-28 15:19:07',NULL,'04672223200',4),(10002,'meb10002','8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','test2@kth.se','Test2','Test2','2015-04-28 15:20:34','Mr','1234-5678-1234-5678',-1,0,'V3WBPS4G2WMQ53VA',NULL,'FRIEND','8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918',12,'2015-04-28 15:19:07',NULL,'04672223200',4),(10003,'meb10003','8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','test3@kth.se','Test3','Test3','2015-04-28 15:20:34','Mr','1234-5678-1234-5678',-1,0,'V3WBPS4G2WMQ53VA',NULL,'FRIEND','8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918',12,'2015-04-28 15:19:07',NULL,'04672223200',4),(10004,'meb10004','8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','test4@kth.se','Test4','Test4','2015-04-28 15:20:34','Mr','1234-5678-1234-5678',-1,0,'V3WBPS4G2WMQ53VA',NULL,'FRIEND','8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918',12,'2015-04-28 15:19:07',NULL,'04672223200',4),(10005,'meb10005','8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','test5@kth.se','Test5','Test5','2015-04-28 15:20:34','Mr','1234-5678-1234-5678',-1,0,'V3WBPS4G2WMQ53VA',NULL,'FRIEND','8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918',12,'2015-04-28 15:19:07',NULL,'04672223200',4),(10006,'meb10006','8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','test6@kth.se','Test6','Test6','2015-04-28 15:20:34','Mr','1234-5678-1234-5678',-1,0,'V3WBPS4G2WMQ53VA',NULL,'FRIEND','8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918',12,'2015-04-28 15:19:07',NULL,'04672223200',4),(10007,'meb10007','435a4c16af7a9b3bfe2784e34e5dca1538bb59ca3b05756ef500728326d5ce60','asd@kth.se','asd','dsa','2015-04-29 17:50:06',NULL,NULL,0,0,NULL,NULL,'PHONE','114bd151f8fb0c58642d2170da4ae7d7c57977260ac2cc8905306cab6b2acabc',0,'2015-04-29 17:41:01',NULL,'46000000000',4);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `users_groups`
--

DROP TABLE IF EXISTS `users_groups`;
/*!50001 DROP VIEW IF EXISTS `users_groups`*/;
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
-- Table structure for table `yubikey`
--

DROP TABLE IF EXISTS `yubikey`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yubikey` (
  `serial` varchar(10) DEFAULT NULL,
  `version` varchar(15) DEFAULT NULL,
  `notes` varchar(100) DEFAULT NULL,
  `counter` int(11) DEFAULT NULL,
  `low` int(11) DEFAULT NULL,
  `high` int(11) DEFAULT NULL,
  `session_use` int(11) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `aes_secret` varchar(100) DEFAULT NULL,
  `public_id` varchar(40) DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `yubikey`
--

LOCK TABLES `yubikey` WRITE;
/*!40000 ALTER TABLE `yubikey` DISABLE KEYS */;
/*!40000 ALTER TABLE `yubikey` ENABLE KEYS */;
UNLOCK TABLES;

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
  CONSTRAINT `zeppelin_notes_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `zeppelin_notes`
--

LOCK TABLES `zeppelin_notes` WRITE;
/*!40000 ALTER TABLE `zeppelin_notes` DISABLE KEYS */;
/*!40000 ALTER TABLE `zeppelin_notes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `zeppelin_paragraph`
--

DROP TABLE IF EXISTS `zeppelin_paragraph`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `zeppelin_paragraph` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `note_id` int(11) NOT NULL,
  `content` text,
  PRIMARY KEY (`id`),
  KEY `note_id` (`note_id`),
  CONSTRAINT `zeppelin_paragraph_ibfk_1` FOREIGN KEY (`note_id`) REFERENCES `zeppelin_notes` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `zeppelin_paragraph`
--

LOCK TABLES `zeppelin_paragraph` WRITE;
/*!40000 ALTER TABLE `zeppelin_paragraph` DISABLE KEYS */;
/*!40000 ALTER TABLE `zeppelin_paragraph` ENABLE KEYS */;
UNLOCK TABLES;

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

-- Dump completed on 2015-05-11 16:31:20
