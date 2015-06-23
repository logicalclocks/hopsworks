-- phpMyAdmin SQL Dump
-- version 4.0.6deb1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: May 15, 2015 at 10:26 AM
-- Server version: 5.5.37-0ubuntu0.13.10.1
-- PHP Version: 5.5.3-1ubuntu2.6

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `kthfs`
--

DELIMITER $$
--
-- Procedures
--
CREATE DEFINER=`vmetahops`@`localhost` PROCEDURE `emptyTables`()
    NO SQL
BEGIN
	
	--TODO EMPTY ALL THE CORRESPONDING META TABLES

END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `activity`
--

CREATE TABLE IF NOT EXISTS `activity` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `activity` varchar(128) NOT NULL,
  `user_id` int(10) NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `flag` varchar(128) DEFAULT NULL,
  `project_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `project_id` (`project_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=15 ;

-- --------------------------------------------------------

--
-- Table structure for table `address`
--

CREATE TABLE IF NOT EXISTS `address` (
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
  KEY `uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `anatomical_parts`
--

CREATE TABLE IF NOT EXISTS `anatomical_parts` (
  `id` int(16) NOT NULL AUTO_INCREMENT,
  `ontology_name` varchar(255) DEFAULT NULL,
  `ontology_version` varchar(255) DEFAULT NULL,
  `ontology_code` varchar(255) DEFAULT NULL,
  `ontology_description` varchar(1000) DEFAULT NULL,
  `explanation` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `bbc_group`
--

CREATE TABLE IF NOT EXISTS `bbc_group` (
  `group_name` varchar(20) NOT NULL,
  `group_desc` varchar(200) DEFAULT NULL,
  `gid` int(11) NOT NULL,
  PRIMARY KEY (`gid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `bbc_group`
--

INSERT INTO `bbc_group` (`group_name`, `group_desc`, `gid`) VALUES
('BBC_ADMIN', 'Data Owner', 1001),
('BBC_RESEARCHER', 'Users to run experiment', 1002),
('BBC_GUEST', 'New users', 1003),
('AUDITOR', 'To audit the platform', 1004),
('SYS_ADMIN', 'Platform administrator', 1005),
('BBC_USER', 'Registered users in the system', 1006);

-- --------------------------------------------------------

--
-- Table structure for table `collection_type`
--

CREATE TABLE IF NOT EXISTS `collection_type` (
  `collection_id` varchar(255) NOT NULL,
  `type` varchar(128) NOT NULL,
  PRIMARY KEY (`collection_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `consent`
--

CREATE TABLE IF NOT EXISTS `consent` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `date` date DEFAULT NULL,
  `project_id` int(11) NOT NULL,
  `status` varchar(30) DEFAULT NULL,
  `name` varchar(80) DEFAULT NULL,
  `type` varchar(30) DEFAULT NULL,
  `consent_form` longblob,
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `diseases`
--

CREATE TABLE IF NOT EXISTS `diseases` (
  `id` int(16) NOT NULL AUTO_INCREMENT,
  `ontology_name` varchar(255) DEFAULT NULL,
  `ontology_version` varchar(255) DEFAULT NULL,
  `ontology_code` varchar(255) DEFAULT NULL,
  `ontology_description` varchar(1000) DEFAULT NULL,
  `explanation` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `meta_fields`
--

CREATE TABLE IF NOT EXISTS `meta_fields` (
  `fieldid` int(11) NOT NULL AUTO_INCREMENT,
  `maxsize` int(11) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `required` smallint(6) DEFAULT NULL,
  `searchable` smallint(6) DEFAULT NULL,
  `tableid` int(11) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `description` varchar(250) CHARACTER SET utf8 NOT NULL,
  `fieldtypeid` int(11) NOT NULL,
  PRIMARY KEY (`fieldid`),
  KEY `FK_fields_tableid` (`tableid`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `meta_field_predefined_values`
--

CREATE TABLE IF NOT EXISTS `meta_field_predefined_values` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `fieldid` int(11) NOT NULL,
  `valuee` varchar(250) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `meta_field_types`
--

CREATE TABLE IF NOT EXISTS `meta_field_types` (
  `id` mediumint(9) NOT NULL AUTO_INCREMENT,
  `description` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

--
-- Dumping data for table `meta_field_types`
--

INSERT INTO `meta_field_types` (`id`, `description`) VALUES
(1, 'text'),
(2, 'value list'),
(3, 'yes/no');

-- --------------------------------------------------------

--
-- Table structure for table `jobhistory`
--

CREATE TABLE IF NOT EXISTS `jobhistory` (
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
  KEY `user` (`user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `job_execution_files`
--

CREATE TABLE IF NOT EXISTS `job_execution_files` (
  `job_id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `path` varchar(255) NOT NULL,
  PRIMARY KEY (`job_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `job_input_files`
--

CREATE TABLE IF NOT EXISTS `job_input_files` (
  `job_id` int(11) NOT NULL,
  `path` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`job_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `job_output_files`
--

CREATE TABLE IF NOT EXISTS `job_output_files` (
  `job_id` int(11) NOT NULL,
  `path` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`job_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `organization`
--

CREATE TABLE IF NOT EXISTS `organization` (
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
  KEY `fk_Organization` (`uid`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=9 ;

--
-- Dumping data for table `organization`
--

INSERT INTO `organization` (`id`, `uid`, `org_name`, `website`, `contact_person`, `contact_email`, `department`, `phone`, `fax`) VALUES
(1, 10000, 'KI', 'www.ki.se', 'Admin Admin', 'admin@ki.se', 'MEB', '+4670080010', '+4670080015'),
(2, 10001, 'KI', 'www.ki.se', 'Admin Admin', 'admin@ki.se', 'MEB', '+4670080010', '+4670080015'),
(3, 10002, 'KI', 'www.ki.se', 'Admin Admin', 'admin@ki.se', 'MEB', '+4670080010', '+4670080015'),
(4, 10003, 'KI', 'www.ki.se', 'Admin Admin', 'admin@ki.se', 'MEB', '+4670080010', '+4670080015'),
(8, 10000, 'KI', 'www.ki.se', 'Admin Admin', 'admin@ki.se', 'MEB', '+4670080010', '+4670080015');

-- --------------------------------------------------------

--
-- Table structure for table `people_group`
--

CREATE TABLE IF NOT EXISTS `people_group` (
  `uid` int(10) NOT NULL,
  `gid` int(11) NOT NULL,
  PRIMARY KEY (`uid`,`gid`),
  KEY `gid` (`gid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `people_group`
--

INSERT INTO `people_group` (`uid`, `gid`) VALUES
(10000, 1003),
(10001, 1003),
(10002, 1003),
(10003, 1003),
(10000, 1005);

-- --------------------------------------------------------

--
-- Table structure for table `project`
--

CREATE TABLE IF NOT EXISTS `project` (
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
  UNIQUE KEY `username` (`username`,`projectname`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=40 ;

-- --------------------------------------------------------

--
-- Table structure for table `project_design`
--

CREATE TABLE IF NOT EXISTS `project_design` (
  `project_id` int(11) NOT NULL,
  `design` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `project_inclusion_criteria`
--

CREATE TABLE IF NOT EXISTS `project_inclusion_criteria` (
  `project_id` int(11) NOT NULL,
  `criterium` varchar(128) NOT NULL,
  PRIMARY KEY (`project_id`,`criterium`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `project_meta`
--

CREATE TABLE IF NOT EXISTS `project_meta` (
  `project_id` int(11) NOT NULL,
  `description` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `project_services`
--

CREATE TABLE IF NOT EXISTS `project_services` (
  `project_id` int(11) NOT NULL,
  `service` varchar(32) NOT NULL,
  PRIMARY KEY (`project_id`,`service`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `project_team`
--

CREATE TABLE IF NOT EXISTS `project_team` (
  `project_id` int(11) NOT NULL,
  `team_member` varchar(45) NOT NULL,
  `team_role` varchar(32) NOT NULL,
  `added` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`project_id`,`team_member`),
  KEY `team_member` (`team_member`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `meta_raw_data`
--

CREATE TABLE IF NOT EXISTS `meta_raw_data` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `data` longtext,
  `fieldid` int(11) DEFAULT NULL,
  `tupleid` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_raw_data_fieldid` (`fieldid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `samplecollections`
--

CREATE TABLE IF NOT EXISTS `samplecollections` (
  `id` varchar(255) NOT NULL,
  `acronym` varchar(255) NOT NULL,
  `name` varchar(1024) NOT NULL,
  `description` varchar(2000) DEFAULT NULL,
  `contact` varchar(45) NOT NULL,
  `project_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `acronym` (`acronym`),
  KEY `project_id` (`project_id`),
  KEY `contact` (`contact`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `samplecollection_disease`
--

CREATE TABLE IF NOT EXISTS `samplecollection_disease` (
  `collection_id` varchar(255) NOT NULL,
  `disease_id` int(16) NOT NULL,
  PRIMARY KEY (`collection_id`,`disease_id`),
  KEY `disease_id` (`disease_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `samplecollection_type`
--

CREATE TABLE IF NOT EXISTS `samplecollection_type` (
  `collection_id` varchar(255) NOT NULL,
  `type` varchar(128) NOT NULL,
  PRIMARY KEY (`collection_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `samples`
--

CREATE TABLE IF NOT EXISTS `samples` (
  `samplecollection_id` varchar(255) NOT NULL,
  `id` varchar(255) NOT NULL,
  `parent_id` varchar(255) DEFAULT NULL,
  `sampled_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `anatomical_site` int(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `anatomical_site` (`anatomical_site`),
  KEY `parent_id` (`parent_id`),
  KEY `samplecollection_id` (`samplecollection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `sample_material`
--

CREATE TABLE IF NOT EXISTS `sample_material` (
  `sample_id` varchar(255) NOT NULL,
  `type` varchar(128) NOT NULL,
  PRIMARY KEY (`sample_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `meta_inodes_ops`
--

CREATE TABLE IF NOT EXISTS `meta_inodes_ops` (
  `inodeid` int(11) NOT NULL,
  `inode_pid` int(11) NOT NULL,
  `inode_root` int(11) NOT NULL,
  `modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `operationn` smallint(6) NOT NULL,
  `processed` smallint(6) NOT NULL,
  PRIMARY KEY (`inodeid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `meta_inodes_ops_children_deleted`
--

CREATE TABLE IF NOT EXISTS `meta_inodes_ops_children_deleted` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `inodeid` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `meta_inodes_ops_parents_deleted`
--

CREATE TABLE IF NOT EXISTS `meta_inodes_ops_parents_deleted` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `inodeid` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------
--
-- Table structure for table `meta_tables`
--

CREATE TABLE IF NOT EXISTS `meta_tables` (
  `tableid` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `templateid` int(11) NOT NULL,
  PRIMARY KEY (`tableid`),
  KEY `FK_tables_templateid` (`templateid`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `meta_templates`
--

CREATE TABLE IF NOT EXISTS `meta_templates` (
  `templateid` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(250) NOT NULL,
  PRIMARY KEY (`templateid`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `meta_template_to_inode`
--

CREATE TABLE IF NOT EXISTS `meta_template_to_inode` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `template_id` int(11) NOT NULL,
  `inode_id` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;


--
-- Table structure for table `meta_tuple_to_file`
--

CREATE TABLE IF NOT EXISTS `meta_tuple_to_file` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `inodeid` int(11) DEFAULT NULL,
  `tupleid` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `userlogins`
--

CREATE TABLE IF NOT EXISTS `userlogins` (
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
  KEY `uid` (`uid`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=70 ;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE IF NOT EXISTS `users` (
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

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`uid`, `username`, `password`, `email`, `fname`, `lname`, `activated`, `title`, `orcid`, `false_login`, `isonline`, `secret`, `validation_key`, `security_question`, `security_answer`, `yubikey_user`, `password_changed`, `notes`, `mobile`, `status`) VALUES
(10000, 'meb10000', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'admin@kth.se', 'Admin', 'Admin', '2015-05-15 10:22:36', 'Mr', '1234-5678-1234-5678', -1, 0, 'V3WBPS4G2WMQ53VA', NULL, 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, '2015-04-28 15:18:42', NULL, '123456789', 4),
(10001, 'meb10001', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'test1@kth.se', 'Test1', 'Test1', '2015-04-28 15:20:34', 'Mr', '1234-5678-1234-5678', -1, 0, 'V3WBPS4G2WMQ53VA', NULL, 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, '2015-04-28 15:19:07', NULL, '04672223200', 4),
(10002, 'meb10002', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'test2@kth.se', 'Test2', 'Test2', '2015-04-28 15:20:34', 'Mr', '1234-5678-1234-5678', -1, 0, 'V3WBPS4G2WMQ53VA', NULL, 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, '2015-04-28 15:19:07', NULL, '04672223200', 4),
(10003, 'meb10003', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'test3@kth.se', 'Test3', 'Test3', '2015-04-28 15:20:34', 'Mr', '1234-5678-1234-5678', -1, 0, 'V3WBPS4G2WMQ53VA', NULL, 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, '2015-04-28 15:19:07', NULL, '04672223200', 4);

-- --------------------------------------------------------

--
-- Stand-in structure for view `users_groups`
--
CREATE TABLE IF NOT EXISTS `users_groups` (
`username` varchar(10)
,`password` varchar(128)
,`secret` varchar(20)
,`email` varchar(45)
,`group_name` varchar(20)
);
-- --------------------------------------------------------

--
-- Table structure for table `yubikey`
--

CREATE TABLE IF NOT EXISTS `yubikey` (
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
  UNIQUE KEY `public_id_2` (`public_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `zeppelin_notes`
--

CREATE TABLE IF NOT EXISTS `zeppelin_notes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `zeppelin_paragraph`
--

CREATE TABLE IF NOT EXISTS `zeppelin_paragraph` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `note_id` int(11) NOT NULL,
  `content` text,
  PRIMARY KEY (`id`),
  KEY `note_id` (`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS `inodes` (
  `id` mediumint(9) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `pid` mediumint(9) DEFAULT '0',
  `root` int(11) DEFAULT '0',
  `modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `isDir` tinyint(1) NOT NULL,
  `size` int(11) DEFAULT NULL,
  `status` enum('uploading','copying_to_hdfs','available') NOT NULL,
  `searchable` smallint(6) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `pid_3` (`pid`,`name`),
  KEY `pid` (`pid`),
  KEY `pid_2` (`pid`,`isDir`),
  KEY `name` (`name`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;


--
-- Structure for view `users_groups`
--
DROP TABLE IF EXISTS `users_groups`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `users_groups` AS select `u`.`username` AS `username`,`u`.`password` AS `password`,`u`.`secret` AS `secret`,`u`.`email` AS `email`,`g`.`group_name` AS `group_name` from ((`people_group` `ug` join `users` `u` on((`u`.`uid` = `ug`.`uid`))) join `bbc_group` `g` on((`g`.`gid` = `ug`.`gid`)));

--
-- Constraints for dumped tables
--

--
-- Constraints for table `activity`
--
ALTER TABLE `activity`
  ADD CONSTRAINT `activity_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`uid`),
  ADD CONSTRAINT `activity_ibfk_2` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`);

--
-- Constraints for table `address`
--
ALTER TABLE `address`
  ADD CONSTRAINT `address_ibfk_1` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `collection_type`
--
ALTER TABLE `collection_type`
  ADD CONSTRAINT `collection_type_ibfk_1` FOREIGN KEY (`collection_id`) REFERENCES `samplecollections` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `consent`
--
ALTER TABLE `consent`
  ADD CONSTRAINT `consent_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`);


--
-- Constraints for table `jobhistory`
--
ALTER TABLE `jobhistory`
  ADD CONSTRAINT `jobhistory_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  ADD CONSTRAINT `jobhistory_ibfk_2` FOREIGN KEY (`user`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Constraints for table `job_execution_files`
--
ALTER TABLE `job_execution_files`
  ADD CONSTRAINT `job_execution_files_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `job_input_files`
--
ALTER TABLE `job_input_files`
  ADD CONSTRAINT `job_input_files_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `job_output_files`
--
ALTER TABLE `job_output_files`
  ADD CONSTRAINT `job_output_files_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `organization`
--
ALTER TABLE `organization`
  ADD CONSTRAINT `fk_Organization` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE;

--
-- Constraints for table `people_group`
--
ALTER TABLE `people_group`
  ADD CONSTRAINT `people_group_ibfk_1` FOREIGN KEY (`gid`) REFERENCES `bbc_group` (`gid`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `people_group_ibfk_2` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `project`
--
ALTER TABLE `project`
  ADD CONSTRAINT `project_ibfk_1` FOREIGN KEY (`username`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Constraints for table `project_design`
--
ALTER TABLE `project_design`
  ADD CONSTRAINT `project_design_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project_meta` (`project_id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `project_inclusion_criteria`
--
ALTER TABLE `project_inclusion_criteria`
  ADD CONSTRAINT `project_inclusion_criteria_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project_meta` (`project_id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `project_meta`
--
ALTER TABLE `project_meta`
  ADD CONSTRAINT `project_meta_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`);

--
-- Constraints for table `project_services`
--
ALTER TABLE `project_services`
  ADD CONSTRAINT `project_services_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`);

--
-- Constraints for table `project_team`
--
ALTER TABLE `project_team`
  ADD CONSTRAINT `project_team_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  ADD CONSTRAINT `project_team_ibfk_2` FOREIGN KEY (`team_member`) REFERENCES `users` (`email`) ON DELETE CASCADE ON UPDATE NO ACTION;


--
-- Constraints for table `samplecollections`
--
ALTER TABLE `samplecollections`
  ADD CONSTRAINT `samplecollections_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  ADD CONSTRAINT `samplecollections_ibfk_2` FOREIGN KEY (`contact`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Constraints for table `samplecollection_disease`
--
ALTER TABLE `samplecollection_disease`
  ADD CONSTRAINT `samplecollection_disease_ibfk_1` FOREIGN KEY (`disease_id`) REFERENCES `diseases` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `samplecollection_disease_ibfk_2` FOREIGN KEY (`collection_id`) REFERENCES `samplecollections` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `samplecollection_type`
--
ALTER TABLE `samplecollection_type`
  ADD CONSTRAINT `samplecollection_type_ibfk_1` FOREIGN KEY (`collection_id`) REFERENCES `samplecollections` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `samples`
--
ALTER TABLE `samples`
  ADD CONSTRAINT `samples_ibfk_1` FOREIGN KEY (`anatomical_site`) REFERENCES `anatomical_parts` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `samples_ibfk_2` FOREIGN KEY (`parent_id`) REFERENCES `samples` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  ADD CONSTRAINT `samples_ibfk_3` FOREIGN KEY (`samplecollection_id`) REFERENCES `samplecollections` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `sample_material`
--
ALTER TABLE `sample_material`
  ADD CONSTRAINT `sample_material_ibfk_1` FOREIGN KEY (`sample_id`) REFERENCES `samples` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `meta_fields`
--
ALTER TABLE `meta_fields`
  ADD CONSTRAINT `FK_fields_tableid` FOREIGN KEY (`tableid`) REFERENCES `meta_tables` (`tableid`);

--
-- Constraints for table `meta_raw_data`
--
ALTER TABLE `meta_raw_data`
  ADD CONSTRAINT `FK_raw_data_fieldid` FOREIGN KEY (`fieldid`) REFERENCES `meta_fields` (`fieldid`);

--
-- Constraints for table `meta_tables`
--
ALTER TABLE `meta_tables`
  ADD CONSTRAINT `FK_tables_templateid` FOREIGN KEY (`templateid`) REFERENCES `meta_templates` (`templateid`);

--
-- Constraints for table `userlogins`
--
ALTER TABLE `userlogins`
  ADD CONSTRAINT `userlogins_ibfk_1` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `yubikey`
--
ALTER TABLE `yubikey`
  ADD CONSTRAINT `yubikey_ibfk_1` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `zeppelin_notes`
--
ALTER TABLE `zeppelin_notes`
  ADD CONSTRAINT `zeppelin_notes_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`);

--
-- Constraints for table `zeppelin_paragraph`
--
ALTER TABLE `zeppelin_paragraph`
  ADD CONSTRAINT `zeppelin_paragraph_ibfk_1` FOREIGN KEY (`note_id`) REFERENCES `zeppelin_notes` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
