-- MySQL dump 10.13  Distrib 5.6.22, for Linux (x86_64)
--
-- Host: localhost    Database: kthfs
-- ------------------------------------------------------
-- Server version	5.6.22

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
-- Dumping data for table `Address`
--

LOCK TABLES `Address` WRITE;
/*!40000 ALTER TABLE `Address` DISABLE KEYS */;
/*!40000 ALTER TABLE `Address` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Alerts`
--

DROP TABLE IF EXISTS `Alerts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Alerts` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `CURRENTVALUE` varchar(32) DEFAULT NULL,
  `FAILUREMAX` varchar(32) DEFAULT NULL,
  `FAILUREMIN` varchar(32) DEFAULT NULL,
  `WARNINGMAX` varchar(32) DEFAULT NULL,
  `WARNINGMIN` varchar(32) DEFAULT NULL,
  `AGENTTIME` bigint(20) DEFAULT NULL,
  `ALERTTIME` datetime DEFAULT NULL,
  `DATASOURCE` varchar(128) DEFAULT NULL,
  `HOSTID` varchar(256) DEFAULT NULL,
  `MESSAGE` varchar(1024) NOT NULL,
  `PLUGIN` varchar(128) DEFAULT NULL,
  `PLUGININSTANCE` varchar(128) DEFAULT NULL,
  `PROVIDER` int(11) DEFAULT NULL,
  `SEVERITY` int(11) DEFAULT NULL,
  `TYPE` varchar(128) DEFAULT NULL,
  `TYPEINSTANCE` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Alerts`
--

LOCK TABLES `Alerts` WRITE;
/*!40000 ALTER TABLE `Alerts` DISABLE KEYS */;
/*!40000 ALTER TABLE `Alerts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ClusterEntities`
--

DROP TABLE IF EXISTS `ClusterEntities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ClusterEntities` (
  `ID` bigint(20) NOT NULL,
  `CLUSTERNAME` varchar(255) DEFAULT NULL,
  `CLUSTERTYPE` varchar(255) DEFAULT NULL,
  `YAMLCONTENT` text,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ClusterEntities`
--

LOCK TABLES `ClusterEntities` WRITE;
/*!40000 ALTER TABLE `ClusterEntities` DISABLE KEYS */;
/*!40000 ALTER TABLE `ClusterEntities` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Commands`
--

DROP TABLE IF EXISTS `Commands`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Commands` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `CLUSTER` varchar(48) NOT NULL,
  `COMMAND` varchar(256) NOT NULL,
  `END_TIME` datetime DEFAULT NULL,
  `HOSTID` varchar(128) NOT NULL,
  `ROLE_` varchar(48) NOT NULL,
  `SERVICE` varchar(48) NOT NULL,
  `START_TIME` datetime DEFAULT NULL,
  `STATUS` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Commands`
--

LOCK TABLES `Commands` WRITE;
/*!40000 ALTER TABLE `Commands` DISABLE KEYS */;
/*!40000 ALTER TABLE `Commands` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Env_vars`
--

DROP TABLE IF EXISTS `Env_vars`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Env_vars` (
  `name` varchar(128) NOT NULL,
  `value` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Env_vars`
--

LOCK TABLES `Env_vars` WRITE;
/*!40000 ALTER TABLE `Env_vars` DISABLE KEYS */;
INSERT INTO `Env_vars` VALUES ('HADOOP_CLASSPATH','/home/stig/cuneiform/hadoop-2.4.0/etc/hadoop:/home/stig/cuneiform/hadoop-2.4.0/share/hadoop/common/lib/*:/home/stig/cuneiform/hadoop-2.4.0/share/hadoop/common/*:/home/stig/cuneiform/hadoop-2.4.0/share/hadoop/hdfs:/home/stig/cuneiform/hadoop-2.4.0/share/hadoop/hdfs/lib/*:/home/stig/cuneiform/hadoop-2.4.0/share/hadoop/hdfs/*:/home/stig/cuneiform/hadoop-2.4.0/share/hadoop/yarn/lib/*:/home/stig/cuneiform/hadoop-2.4.0/share/hadoop/yarn/*:/home/stig/cuneiform/hadoop-2.4.0/share/hadoop/mapreduce/lib/*:/home/stig/cuneiform/hadoop-2.4.0/share/hadoop/mapreduce/*:/home/stig/cuneiform/hadoop-2.4.0/contrib/capacity-scheduler/*.jar'),('YARN_CONF_DIR','/home/stig/projects/hadoop-2.4.0/etc/hadoop');
/*!40000 ALTER TABLE `Env_vars` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Graphs`
--

DROP TABLE IF EXISTS `Graphs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Graphs` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `CHARTS` longblob,
  `GRAPHID` varchar(64) NOT NULL,
  `GROUP_` varchar(64) NOT NULL,
  `GROUPRANK` int(11) DEFAULT NULL,
  `PLUGIN` varchar(48) DEFAULT NULL,
  `PLUGININSTANCE` varchar(48) DEFAULT NULL,
  `RANK` int(11) DEFAULT NULL,
  `SELECTED` tinyint(1) DEFAULT '0',
  `TARGET` varchar(64) NOT NULL,
  `TITLE` varchar(48) DEFAULT NULL,
  `VAR` varchar(64) DEFAULT NULL,
  `VERTICALLABEL` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Graphs`
--

LOCK TABLES `Graphs` WRITE;
/*!40000 ALTER TABLE `Graphs` DISABLE KEYS */;
/*!40000 ALTER TABLE `Graphs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Group`
--

DROP TABLE IF EXISTS `Group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Group` (
  `gid` int(10) NOT NULL,
  `group_name` varchar(20) NOT NULL,
  `group_desc` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`gid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Group`
--

LOCK TABLES `Group` WRITE;
/*!40000 ALTER TABLE `Group` DISABLE KEYS */;
/*!40000 ALTER TABLE `Group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Hosts`
--

DROP TABLE IF EXISTS `Hosts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Hosts` (
  `HOSTID` varchar(128) NOT NULL,
  `CORES` int(11) DEFAULT NULL,
  `DISKCAPACITY` bigint(20) DEFAULT NULL,
  `DISKUSED` bigint(20) DEFAULT NULL,
  `HOSTNAME_` varchar(128) NOT NULL,
  `LASTHEARTBEAT` bigint(20) DEFAULT NULL,
  `LOAD1` double DEFAULT NULL,
  `LOAD15` double DEFAULT NULL,
  `LOAD5` double DEFAULT NULL,
  `MEMORYCAPACITY` bigint(20) DEFAULT NULL,
  `MEMORYUSED` bigint(20) DEFAULT NULL,
  `PRIVATEIP` varchar(15) DEFAULT NULL,
  `PUBLICIP` varchar(15) DEFAULT NULL,
  `REGISTERED` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`HOSTID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Hosts`
--

LOCK TABLES `Hosts` WRITE;
/*!40000 ALTER TABLE `Hosts` DISABLE KEYS */;
/*!40000 ALTER TABLE `Hosts` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=281 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Inodes`
--

LOCK TABLES `Inodes` WRITE;
/*!40000 ALTER TABLE `Inodes` DISABLE KEYS */;
INSERT INTO `Inodes` VALUES (12,'Projects',NULL,'2014-10-23 12:07:44',1,0,'available'),(277,'1',12,'2014-12-10 17:17:08',1,0,'available'),(278,'Results',277,'2014-12-10 17:17:08',1,0,'available'),(279,'Cuneiform',277,'2014-12-10 17:17:08',1,0,'available'),(280,'Samples',277,'2014-12-10 17:17:08',1,0,'available');
/*!40000 ALTER TABLE `Inodes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Jobs`
--

DROP TABLE IF EXISTS `Jobs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Jobs` (
  `ID` bigint(20) NOT NULL,
  `COMPLETED` tinyint(1) DEFAULT '0',
  `COMPLETIONTIME` bigint(20) DEFAULT NULL,
  `DATERUN` varchar(255) DEFAULT NULL,
  `EXECUTEDBY` varchar(255) DEFAULT NULL,
  `GRAPHDOT` text,
  `NAME` varchar(255) DEFAULT NULL,
  `TABLEJOB` text,
  `UNIQUEID` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Jobs`
--

LOCK TABLES `Jobs` WRITE;
/*!40000 ALTER TABLE `Jobs` DISABLE KEYS */;
/*!40000 ALTER TABLE `Jobs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Login`
--

DROP TABLE IF EXISTS `Login`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Login` (
  `loginid` bigint(20) NOT NULL AUTO_INCREMENT,
  `People_uid` int(10) DEFAULT NULL,
  `last_login` timestamp NULL DEFAULT NULL,
  `last_ip` varchar(45) DEFAULT NULL,
  `os_platform` int(11) DEFAULT NULL,
  `logout` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`loginid`),
  KEY `fk_login_people` (`People_uid`),
  CONSTRAINT `FK_Login_People_uid` FOREIGN KEY (`People_uid`) REFERENCES `People` (`uid`),
  CONSTRAINT `fk_login_people` FOREIGN KEY (`People_uid`) REFERENCES `People` (`uid`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Login`
--

LOCK TABLES `Login` WRITE;
/*!40000 ALTER TABLE `Login` DISABLE KEYS */;
/*!40000 ALTER TABLE `Login` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `NodeProgress`
--

DROP TABLE IF EXISTS `NodeProgress`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `NodeProgress` (
  `ID` bigint(20) NOT NULL,
  `CLUSTER` varchar(255) DEFAULT NULL,
  `IP` varchar(255) DEFAULT NULL,
  `LOGINUSER` varchar(255) DEFAULT NULL,
  `NODEID` varchar(255) DEFAULT NULL,
  `NODEROLE` varchar(255) DEFAULT NULL,
  `PHASE` varchar(255) DEFAULT NULL,
  `PREVIOUSPHASE` varchar(255) DEFAULT NULL,
  `PRIVATEKEY` text,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `NodeProgress`
--

LOCK TABLES `NodeProgress` WRITE;
/*!40000 ALTER TABLE `NodeProgress` DISABLE KEYS */;
/*!40000 ALTER TABLE `NodeProgress` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `PaasCredentials`
--

DROP TABLE IF EXISTS `PaasCredentials`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PaasCredentials` (
  `ID` bigint(20) NOT NULL,
  `ACCESSKEY` varchar(255) DEFAULT NULL,
  `ACCOUNTID` varchar(255) DEFAULT NULL,
  `DASHBOARDIP` varchar(255) DEFAULT NULL,
  `EMAIL` varchar(255) DEFAULT NULL,
  `KEYSTONEURL` varchar(255) DEFAULT NULL,
  `PRIVATEKEY` text,
  `PROVIDER` varchar(255) DEFAULT NULL,
  `PUBLICKEY` text,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `PaasCredentials`
--

LOCK TABLES `PaasCredentials` WRITE;
/*!40000 ALTER TABLE `PaasCredentials` DISABLE KEYS */;
/*!40000 ALTER TABLE `PaasCredentials` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `People`
--

DROP TABLE IF EXISTS `People`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `People` (
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
  `active` int(11) NOT NULL DEFAULT '-1',
  `isonline` int(11) NOT NULL DEFAULT '-1',
  `secret` varchar(20) DEFAULT NULL,
  `security_question` varchar(20) DEFAULT NULL,
  `security_answer` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `People`
--

LOCK TABLES `People` WRITE;
/*!40000 ALTER TABLE `People` DISABLE KEYS */;
INSERT INTO `People` VALUES (10001,'meb10001','d7e6ee64d871d3fdfc6e5acb6f83a60f7c1c273ac1d35ec740f1a6e1ca42f548','stigviaene@sics.se','Stig','Viaene','2014-12-08 10:37:54',NULL,NULL,'0123456789',NULL,0,-1,-1,'GDRZ7FEOIOWMSDSE','history','d30af076b0dc85cc5b0074b403f74770b73af84a70c2f77f7a3609d0db23112e');
/*!40000 ALTER TABLE `People` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `People_Group`
--

DROP TABLE IF EXISTS `People_Group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `People_Group` (
  `uid` int(10) NOT NULL,
  `gid` int(10) NOT NULL,
  PRIMARY KEY (`uid`,`gid`),
  KEY `fk_people_has_group_group1` (`gid`),
  KEY `fk_people_has_groups_people` (`uid`),
  CONSTRAINT `FK_People_Group_uid` FOREIGN KEY (`uid`) REFERENCES `People` (`uid`),
  CONSTRAINT `fk_group` FOREIGN KEY (`gid`) REFERENCES `Group` (`gid`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_people` FOREIGN KEY (`uid`) REFERENCES `People` (`uid`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `People_Group`
--

LOCK TABLES `People_Group` WRITE;
/*!40000 ALTER TABLE `People_Group` DISABLE KEYS */;
/*!40000 ALTER TABLE `People_Group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Roles`
--

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
-- Dumping data for table `Roles`
--

LOCK TABLES `Roles` WRITE;
/*!40000 ALTER TABLE `Roles` DISABLE KEYS */;
/*!40000 ALTER TABLE `Roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `SEQUENCE`
--

DROP TABLE IF EXISTS `SEQUENCE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SEQUENCE` (
  `SEQ_NAME` varchar(50) NOT NULL,
  `SEQ_COUNT` decimal(38,0) DEFAULT NULL,
  PRIMARY KEY (`SEQ_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `SEQUENCE`
--

LOCK TABLES `SEQUENCE` WRITE;
/*!40000 ALTER TABLE `SEQUENCE` DISABLE KEYS */;
INSERT INTO `SEQUENCE` VALUES ('SEQ_GEN',0);
/*!40000 ALTER TABLE `SEQUENCE` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `STUDY_GROUPS`
--

DROP TABLE IF EXISTS `STUDY_GROUPS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `STUDY_GROUPS` (
  `email` varchar(255) NOT NULL,
  `groupname` varchar(64) NOT NULL,
  PRIMARY KEY (`email`,`groupname`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `STUDY_GROUPS`
--

LOCK TABLES `STUDY_GROUPS` WRITE;
/*!40000 ALTER TABLE `STUDY_GROUPS` DISABLE KEYS */;
/*!40000 ALTER TABLE `STUDY_GROUPS` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Samples`
--

DROP TABLE IF EXISTS `Samples`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Samples` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `DATAADMINSTRATOR` longblob,
  `DESCRIPTION` varchar(1024) DEFAULT NULL,
  `DIAGNOSIS` longblob,
  `ENGLISHNAME` varchar(512) DEFAULT NULL,
  `LASTUPDATED` datetime DEFAULT NULL,
  `ORGANIZATION` varchar(512) DEFAULT NULL,
  `OTHERINFORMATION` varchar(1024) DEFAULT NULL,
  `RESPONSIBLE` varchar(512) DEFAULT NULL,
  `SAMPLECOLLECTION` longblob,
  `SAMPLEDONORS` longblob,
  `SAMPLES` longblob,
  `STUDY` longblob,
  `SWEDISHNAME` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Samples`
--

LOCK TABLES `Samples` WRITE;
/*!40000 ALTER TABLE `Samples` DISABLE KEYS */;
/*!40000 ALTER TABLE `Samples` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `StudyRoles`
--

LOCK TABLES `StudyRoles` WRITE;
/*!40000 ALTER TABLE `StudyRoles` DISABLE KEYS */;
/*!40000 ALTER TABLE `StudyRoles` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `StudyTeam`
--

LOCK TABLES `StudyTeam` WRITE;
/*!40000 ALTER TABLE `StudyTeam` DISABLE KEYS */;
INSERT INTO `StudyTeam` VALUES ('1','jdowling@sics.se','Master','2014-12-10 17:17:06');
/*!40000 ALTER TABLE `StudyTeam` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `USERS`
--

DROP TABLE IF EXISTS `USERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `USERS` (
  `EMAIL` varchar(255) NOT NULL,
  `MOBILENUM` varchar(255) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `PASSWORD` varchar(128) NOT NULL,
  `REGISTEREDON` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `SALT` longblob NOT NULL,
  `STATUS` int(11) NOT NULL,
  PRIMARY KEY (`EMAIL`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `USERS`
--

LOCK TABLES `USERS` WRITE;
/*!40000 ALTER TABLE `USERS` DISABLE KEYS */;
INSERT INTO `USERS` VALUES ('aragorn@whitree.go','0123456789','Aragorn Arathornson','61646D696E','2014-09-25 11:49:03',']tQP‹QM',0),('boromir.steward@gondor.me','0123456789','Boromir','61646D696E','2014-09-25 11:49:47','/”∆ÉFÈ˚',0),('faramir.steward@gondor.me','0123456789','Faramir','61646D696E','2014-09-25 11:50:06','ìﬁ\"àZ!Éæ',0),('frodo@underhill.sh','0123456789','Frodo Baggins','61646D696E','2014-09-25 11:51:18','≤;˚ù,XÉ',0),('gandalf@grey.me','0123456789','Gandalf','61646D696E','2014-09-25 11:51:41','(Ç€5ÒÃY(',0),('jdowling@sics.se','0123456789','Admin','61646D696E','2014-09-08 12:05:03','slqiehzl',0),('johnny@slager.be','0258963147','Johnny Turbo','61646D696E','2014-09-30 08:24:00','ﬁa¯≤*BV',0),('pipi@sterk.se','0123456789','Pipi Langkous','61646D696E','2014-10-13 10:40:29','$›/¨Nø<',-1);
/*!40000 ALTER TABLE `USERS` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `USERS_GROUPS`
--

DROP TABLE IF EXISTS `USERS_GROUPS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `USERS_GROUPS` (
  `email` varchar(255) NOT NULL,
  `groupname` varchar(64) NOT NULL,
  PRIMARY KEY (`email`,`groupname`),
  UNIQUE KEY `UNQ_USERS_GROUPS_0` (`email`,`groupname`),
  CONSTRAINT `FK_USERS_GROUPS_email` FOREIGN KEY (`email`) REFERENCES `USERS` (`EMAIL`),
  CONSTRAINT `USERS_GROUPS_ibfk_1` FOREIGN KEY (`email`) REFERENCES `USERS` (`EMAIL`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `USERS_GROUPS`
--

LOCK TABLES `USERS_GROUPS` WRITE;
/*!40000 ALTER TABLE `USERS_GROUPS` DISABLE KEYS */;
INSERT INTO `USERS_GROUPS` VALUES ('aragorn@whitree.go','USER'),('boromir.steward@gondor.me','USER'),('faramir.steward@gondor.me','USER'),('frodo@underhill.sh','USER'),('gandalf@grey.me','USER'),('jdowling@sics.se','ADMIN'),('johnny@slager.be','ADMIN'),('pipi@sterk.se','GUEST');
/*!40000 ALTER TABLE `USERS_GROUPS` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Yubikey`
--

DROP TABLE IF EXISTS `Yubikey`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Yubikey` (
  `yubidnum` varchar(45) NOT NULL,
  `serial` varchar(10) DEFAULT NULL,
  `version` varchar(15) DEFAULT NULL,
  `active` tinyint(1) DEFAULT NULL,
  `notes` varchar(100) DEFAULT NULL,
  `counter` int(11) DEFAULT NULL,
  `low` int(11) DEFAULT NULL,
  `high` int(11) DEFAULT NULL,
  `session_use` int(11) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `aes_secret` varchar(100) DEFAULT NULL,
  `public_id` varchar(40) DEFAULT NULL,
  `People_uid` int(10) DEFAULT NULL,
  PRIMARY KEY (`yubidnum`),
  KEY `fk_Yubikey_People_idx` (`People_uid`),
  CONSTRAINT `fk_Yubikey_People1` FOREIGN KEY (`People_uid`) REFERENCES `People` (`uid`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Yubikey`
--

LOCK TABLES `Yubikey` WRITE;
/*!40000 ALTER TABLE `Yubikey` DISABLE KEYS */;
/*!40000 ALTER TABLE `Yubikey` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=364 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `activity`
--

LOCK TABLES `activity` WRITE;
/*!40000 ALTER TABLE `activity` DISABLE KEYS */;
INSERT INTO `activity` VALUES (361,' created new study ','jdowling@sics.se','2014-12-10 16:28:15','STUDY','TestStudy'),(362,' removed study ','jdowling@sics.se','2014-12-10 16:28:20','STUDY','TestStudy'),(363,' created new study ','jdowling@sics.se','2014-12-10 17:17:06','STUDY','1');
/*!40000 ALTER TABLE `activity` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Table structure for table `dataset`
--

DROP TABLE IF EXISTS `dataset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dataset` (
  `name` varchar(255) NOT NULL,
  `owner` varchar(255) DEFAULT NULL,
  `timestamp` datetime DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dataset`
--

LOCK TABLES `dataset` WRITE;
/*!40000 ALTER TABLE `dataset` DISABLE KEYS */;
/*!40000 ALTER TABLE `dataset` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dataset_requests`
--

DROP TABLE IF EXISTS `dataset_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dataset_requests` (
  `reason` varchar(255) DEFAULT NULL,
  `study_id` int(11) NOT NULL,
  `dataset_id` int(11) NOT NULL,
  PRIMARY KEY (`study_id`,`dataset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dataset_requests`
--

LOCK TABLES `dataset_requests` WRITE;
/*!40000 ALTER TABLE `dataset_requests` DISABLE KEYS */;
/*!40000 ALTER TABLE `dataset_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dataset_study`
--

DROP TABLE IF EXISTS `dataset_study`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dataset_study` (
  `id` int(11) NOT NULL,
  `Dataset_id` int(11) NOT NULL,
  PRIMARY KEY (`id`,`Dataset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dataset_study`
--

LOCK TABLES `dataset_study` WRITE;
/*!40000 ALTER TABLE `dataset_study` DISABLE KEYS */;
/*!40000 ALTER TABLE `dataset_study` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sample`
--

DROP TABLE IF EXISTS `sample`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sample` (
  `id` int(11) NOT NULL,
  `person_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sample`
--

LOCK TABLES `sample` WRITE;
/*!40000 ALTER TABLE `sample` DISABLE KEYS */;
/*!40000 ALTER TABLE `sample` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sample_study`
--

DROP TABLE IF EXISTS `sample_study`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sample_study` (
  `study_id` int(11) NOT NULL,
  `sample_id` int(11) NOT NULL,
  PRIMARY KEY (`study_id`,`sample_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sample_study`
--

LOCK TABLES `sample_study` WRITE;
/*!40000 ALTER TABLE `sample_study` DISABLE KEYS */;
/*!40000 ALTER TABLE `sample_study` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `study`
--

LOCK TABLES `study` WRITE;
/*!40000 ALTER TABLE `study` DISABLE KEYS */;
INSERT INTO `study` VALUES ('1','jdowling@sics.se','2014-12-10 17:17:06');
/*!40000 ALTER TABLE `study` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `study_dataset_permissions`
--

DROP TABLE IF EXISTS `study_dataset_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `study_dataset_permissions` (
  `study_id` int(11) NOT NULL,
  `dataset_id` int(11) NOT NULL,
  PRIMARY KEY (`study_id`,`dataset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `study_dataset_permissions`
--

LOCK TABLES `study_dataset_permissions` WRITE;
/*!40000 ALTER TABLE `study_dataset_permissions` DISABLE KEYS */;
/*!40000 ALTER TABLE `study_dataset_permissions` ENABLE KEYS */;
UNLOCK TABLES;

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
  CONSTRAINT `FK_study_group_members_studyname` FOREIGN KEY (`studyname`) REFERENCES `study` (`name`),
  CONSTRAINT `fk_study_members` FOREIGN KEY (`studyname`) REFERENCES `study` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `study_group_members`
--

LOCK TABLES `study_group_members` WRITE;
/*!40000 ALTER TABLE `study_group_members` DISABLE KEYS */;
/*!40000 ALTER TABLE `study_group_members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `study_services`
--

DROP TABLE IF EXISTS `study_services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `study_services` (
  `study` varchar(128) NOT NULL,
  `service` enum('CUNEIFORM','FLINK','SAMPLES','STUDY_INFO','SPARK','ADAM','MAPREDUCE','YARN') NOT NULL DEFAULT 'CUNEIFORM',
  PRIMARY KEY (`study`,`service`),
  CONSTRAINT `study_services_ibfk_1` FOREIGN KEY (`study`) REFERENCES `study` (`name`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `study_services`
--

LOCK TABLES `study_services` WRITE;
/*!40000 ALTER TABLE `study_services` DISABLE KEYS */;
INSERT INTO `study_services` VALUES ('1','CUNEIFORM'),('1','FLINK'),('1','SAMPLES'),('1','STUDY_INFO'),('1','SPARK'),('1','ADAM'),('1','MAPREDUCE'),('1','YARN');
/*!40000 ALTER TABLE `study_services` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `v_People_Group`
--

DROP TABLE IF EXISTS `v_People_Group`;
/*!50001 DROP VIEW IF EXISTS `v_People_Group`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE VIEW `v_People_Group` AS SELECT 
 1 AS `username`,
 1 AS `password`,
 1 AS `secret`,
 1 AS `email`,
 1 AS `group_name`*/;
SET character_set_client = @saved_cs_client;

--
-- Final view structure for view `StudyDetails`
--

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
/*!50001 VIEW `activitydetails` AS select `activity`.`id` AS `id`,`activity`.`performed_By` AS `performed_by_email`,`USERS`.`NAME` AS `performed_by_name`,`activity`.`activity` AS `description`,`activity`.`activity_on` AS `studyname`,`activity`.`timestamp` AS `timestamp` from (`activity` join `USERS` on((`activity`.`performed_By` = `USERS`.`EMAIL`))) */;
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

-- Dump completed on 2014-12-10 18:22:04
