CREATE TABLE `anatomical_parts` (
  `id` INT(16) NOT NULL AUTO_INCREMENT,
  `ontology_name` VARCHAR(255) DEFAULT NULL,
  `ontology_version` VARCHAR(255) DEFAULT NULL,
  `ontology_code` VARCHAR(255) DEFAULT NULL,
  `ontology_description` VARCHAR(1000) DEFAULT NULL,
  `explanation` VARCHAR(2000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=ndbcluster;

CREATE TABLE `bbc_group` (
  `group_name` VARCHAR(20) NOT NULL,
  `group_desc` VARCHAR(200) DEFAULT NULL,
  `gid` INT(11) NOT NULL,
  PRIMARY KEY (`gid`)
) ENGINE=ndbcluster;

CREATE TABLE `diseases` (
    `id` INT(16) NOT NULL AUTO_INCREMENT,
    `ontology_name` VARCHAR(255) DEFAULT NULL,
    `ontology_version` VARCHAR(255) DEFAULT NULL,
    `ontology_code` VARCHAR(255) DEFAULT NULL,
    `ontology_description` VARCHAR(1000) DEFAULT NULL,
    `explanation` VARCHAR(1000) DEFAULT NULL,
     PRIMARY KEY (`id`)
) ENGINE=ndbcluster;

CREATE TABLE `users` (
  `uid` INT(10) NOT NULL DEFAULT '1000',
  `username` VARCHAR(10) NOT NULL,
  `password` VARCHAR(128) NOT NULL,
  `email` VARCHAR(45) DEFAULT NULL,
  `fname` VARCHAR(30) DEFAULT NULL,
  `lname` VARCHAR(30) DEFAULT NULL,
  `activated` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `title` VARCHAR(10)  DEFAULT NULL,
  `orcid` VARCHAR(20)  DEFAULT NULL,
  `false_login` int(11) NOT NULL DEFAULT '-1',
  `isonline` tinyint(1) NOT NULL DEFAULT '0',
  `secret` VARCHAR(20)  DEFAULT NULL,
  `validation_key` VARCHAR(128)  DEFAULT NULL,
  `security_question` VARCHAR(20)  DEFAULT NULL,
  `security_answer` VARCHAR(128)  DEFAULT NULL,
  `yubikey_user` int(11) NOT NULL DEFAULT '0',
  `password_changed` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `notes` VARCHAR(500)  DEFAULT NULL,
  `mobile` VARCHAR(20)  DEFAULT NULL,
  `status` int(11) NOT NULL DEFAULT '-1',
  PRIMARY KEY (`uid`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`)
) ENGINE=ndbcluster AUTO_INCREMENT=10000;

CREATE TABLE `yubikey` (
  `serial` VARCHAR(10)  DEFAULT NULL,
  `version` VARCHAR(15)  DEFAULT NULL,
  `notes` VARCHAR(100)  DEFAULT NULL,
  `counter` int(11) DEFAULT NULL,
  `low` int(11) DEFAULT NULL,
  `high` int(11) DEFAULT NULL,
  `session_use` int(11) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `aes_secret` VARCHAR(100)  DEFAULT NULL,
  `public_id` VARCHAR(40)  DEFAULT NULL,
  `accessed` timestamp NULL DEFAULT NULL,
  `status` int(11) DEFAULT '-1',
  `yubidnum` int(11) NOT NULL AUTO_INCREMENT,
  `uid` int(11) NOT NULL,
  PRIMARY KEY (`yubidnum`),
  UNIQUE KEY `uid` (`uid`),
  UNIQUE KEY `serial` (`serial`),
  UNIQUE KEY `public_id` (`public_id`),
  UNIQUE (`public_id`),
  FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `address` (
  `address1` VARCHAR(50) DEFAULT NULL,
  `address2` VARCHAR(50) DEFAULT NULL,
  `address3` VARCHAR(50) DEFAULT NULL,
  `city` VARCHAR(30) DEFAULT NULL,
  `state` VARCHAR(100) DEFAULT NULL,
  `country` VARCHAR(40) DEFAULT NULL,
  `postalcode` VARCHAR(10) DEFAULT NULL,
  `address_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `uid` INT(10) NOT NULL,
  PRIMARY KEY (`address_id`),
  FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `people_group` (
  `uid` INT(10) NOT NULL,
  `gid` INT(11) NOT NULL,
  PRIMARY KEY (`uid`,`gid`),
  FOREIGN KEY (`gid`) REFERENCES `bbc_group` (`gid`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `study` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `studyname` VARCHAR(128) NOT NULL,
  `username` VARCHAR(45) NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `retention_period` date DEFAULT NULL,
  `ethical_status` VARCHAR(30) DEFAULT NULL,
  `archived` tinyint(1) DEFAULT '0',
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE (`username`,`studyname`),
  FOREIGN KEY (`username`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `activity` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `activity` VARCHAR(128) NOT NULL,
  `user_id` INT(10) NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `flag` VARCHAR(128) DEFAULT NULL,
  `study_id` INT NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`user_id`) REFERENCES `users` (`uid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=ndbcluster;

CREATE TABLE `study_meta` (
  `study_id` INT NOT NULL,
  `description` VARCHAR(2000) DEFAULT NULL,
  PRIMARY KEY (`study_id`),
  FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=ndbcluster;

CREATE TABLE `study_services` (
  `study_id` INT NOT NULL,
  `service` VARCHAR(32) NOT NULL,
  PRIMARY KEY (`study_id`,`service`),
  FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=ndbcluster;

CREATE TABLE `study_team` (
  `study_id` INT NOT NULL,
  `team_member` VARCHAR(45) NOT NULL,
  `team_role` VARCHAR(32) NOT NULL,
  `added` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`study_id`,`team_member`),
  FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  FOREIGN KEY (`team_member`) REFERENCES `users` (`email`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `userlogins` (
  `login_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `ip` VARCHAR(16) DEFAULT NULL,
  `os` VARCHAR(30) DEFAULT NULL,
  `browser` VARCHAR(40) DEFAULT NULL,
  `action` VARCHAR(80) DEFAULT NULL,
  `outcome` VARCHAR(20) DEFAULT NULL,
  `uid` INT(10) NOT NULL,
  `login_date` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`login_id`),
  KEY (`login_date`),
  FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `zeppelin_notes` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `study_id` INT NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=ndbcluster;

CREATE TABLE `zeppelin_paragraph` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `note_id` INT(11) NOT NULL,
  `content` text,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`note_id`) REFERENCES `zeppelin_notes` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `samplecollections` (
  `id` VARCHAR(255) NOT NULL,
  `acronym` VARCHAR(255) NOT NULL,
  `name` VARCHAR(1024) NOT NULL,
  `description` VARCHAR(2000) DEFAULT NULL,
  `contact` VARCHAR(45) NOT NULL,
  `study_id` INT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE (`acronym`),
  FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  FOREIGN KEY (`contact`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `samples` (
  `samplecollection_id` VARCHAR(255) NOT NULL,
  `id` VARCHAR(255) NOT NULL,
  `parent_id` VARCHAR(255) DEFAULT NULL,
  `sampled_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `anatomical_site` INT(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`anatomical_site`) REFERENCES `anatomical_parts` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY (`parent_id`) REFERENCES `samples` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  FOREIGN KEY (`samplecollection_id`) REFERENCES `samplecollections` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `samplecollection_type` (
  `collection_id` VARCHAR(255) NOT NULL,
  `type` VARCHAR(128) NOT NULL,
  PRIMARY KEY (`collection_id`,`type`),
  FOREIGN KEY (`collection_id`) REFERENCES `samplecollections` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `samplecollection_disease` (
    `collection_id` VARCHAR(255) NOT NULL,
    `disease_id` INT(16) NOT NULL,
    PRIMARY KEY (`collection_id`,`disease_id`),
    FOREIGN KEY (`disease_id`) REFERENCES `diseases` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
    FOREIGN KEY (`collection_id`) REFERENCES `samplecollections` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `study_design` (
  `study_id` INT NOT NULL,
  `design` VARCHAR(128) DEFAULT NULL,
  PRIMARY KEY (`study_id`),
  FOREIGN KEY (`study_id`) REFERENCES `study_meta` (`study_id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `study_inclusion_criteria` (
  `study_id` INT NOT NULL,
  `criterium` VARCHAR(128) NOT NULL,
  PRIMARY KEY (`study_id`,`criterium`),
  FOREIGN KEY (`study_id`) REFERENCES `study_meta` (`study_id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `sample_material` (
  `sample_id` VARCHAR(255) NOT NULL,
  `type` VARCHAR(128) NOT NULL,
  PRIMARY KEY (`sample_id`,`type`),
  FOREIGN KEY (`sample_id`) REFERENCES `samples` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `jobhistory` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) DEFAULT NULL,
  `submission_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `study_id` INT NOT NULL,
  `user` VARCHAR(45) NOT NULL,
  `state` VARCHAR(128) NOT NULL,
  `execution_duration` BIGINT(20) DEFAULT NULL,
  `args` VARCHAR(255) DEFAULT NULL,
  `stdout_path` VARCHAR(255) DEFAULT NULL,
  `stderr_path` VARCHAR(255) DEFAULT NULL,
  `type` VARCHAR(128) NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  FOREIGN KEY (`user`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `job_output_files` (
  `job_id` INT(11) NOT NULL,
  `path` VARCHAR(255) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`job_id`,`name`),
  FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `job_input_files` (
  `job_id` INT(11) NOT NULL,
  `path` VARCHAR(255) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`job_id`,`name`),
  FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `job_execution_files` (
  `job_id` INT(11) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `path` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`job_id`,`name`),
  FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `consent` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `date` date DEFAULT NULL,
  `study_id` INT NOT NULL,
  `status` VARCHAR(30) DEFAULT NULL,
  `name` VARCHAR(80) DEFAULT NULL,
  `type` VARCHAR(30) DEFAULT NULL,
  `consent_form` longblob DEFAULT NULL,
   PRIMARY KEY (`id`),
  FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=ndbcluster;

CREATE TABLE `organization` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `uid` int(11) DEFAULT NULL,
    `org_name` VARCHAR(100) DEFAULT NULL,
    `website` VARCHAR(200) DEFAULT NULL,
    `contact_person` VARCHAR(100) DEFAULT NULL,
    `contact_email` VARCHAR(100) DEFAULT NULL,
    `department` VARCHAR(100) DEFAULT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `fax` VARCHAR(20) DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_Organization` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE
) ENGINE=ndbcluster;
CREATE TABLE `collection_type` (
  `collection_id` VARCHAR(255) NOT NULL,
  `type` VARCHAR(128) NOT NULL,
  PRIMARY KEY (`collection_id`,`type`),
  FOREIGN KEY (`collection_id`) REFERENCES `samplecollections` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE VIEW `users_groups` AS 
  select `u`.`username` AS `username`,
  `u`.`password` AS `password`,
  `u`.`secret` AS `secret`,
  `u`.`email` AS `email`,
  `g`.`group_name` AS `group_name` 
  from 
    ((`people_group` `ug` join `users` `u` on((`u`.`uid` = `ug`.`uid`))) join `bbc_group` `g` on((`g`.`gid` = `ug`.`gid`)));

### METADATA MANAGER SCHEMA

DELIMITER $$
--
-- Procedures
--
CREATE DEFINER=`kthfshops`@`localhost` PROCEDURE `emptyTables`()
    NO SQL
BEGIN
	
	truncate raw_data;

	delete from fields;
	ALTER TABLE fields AUTO_INCREMENT = 1;

	delete from tables;
	alter table tables AUTO_INCREMENT = 1;

END$$

DELIMITER ;

--
-- Table structure for table `fields`
--

CREATE TABLE IF NOT EXISTS `fields` (
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
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=7 ;


-- --------------------------------------------------------

--
-- Table structure for table `field_predefined_values`
--

CREATE TABLE IF NOT EXISTS `field_predefined_values` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `fieldid` int(11) NOT NULL,
  `valuee` varchar(250) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=10 ;


-- --------------------------------------------------------

--
-- Table structure for table `field_types`
--

CREATE TABLE IF NOT EXISTS `field_types` (
  `id` mediumint(9) NOT NULL AUTO_INCREMENT,
  `description` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=4 ;

--
-- Dumping data for table `field_types`
--

INSERT INTO `field_types` (`id`, `description`) VALUES
(1, 'text'),
(2, 'value list'),
(3, 'yes/no');

-- --------------------------------------------------------

--
-- Table structure for table `raw_data`
--

CREATE TABLE IF NOT EXISTS `raw_data` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `data` longtext,
  `fieldid` int(11) DEFAULT NULL,
  `tupleid` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_raw_data_fieldid` (`fieldid`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=17 ;


-- --------------------------------------------------------

--
-- Table structure for table `tables`
--

CREATE TABLE IF NOT EXISTS `tables` (
  `tableid` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `templateid` int(11) NOT NULL,
  PRIMARY KEY (`tableid`),
  KEY `FK_tables_templateid` (`templateid`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=2 ;


-- --------------------------------------------------------

--
-- Table structure for table `templates`
--

CREATE TABLE IF NOT EXISTS `templates` (
  `templateid` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(250) NOT NULL,
  PRIMARY KEY (`templateid`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=3 ;


-- --------------------------------------------------------

--
-- Table structure for table `tuple_to_file`
--

CREATE TABLE IF NOT EXISTS `tuple_to_file` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `inodeid` int(11) DEFAULT NULL,
  `tupleid` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=28 ;


--
-- Constraints for table `fields`
--
ALTER TABLE `fields`
  ADD CONSTRAINT `FK_fields_tableid` FOREIGN KEY (`tableid`) REFERENCES `tables` (`tableid`);

--
-- Constraints for table `raw_data`
--
ALTER TABLE `raw_data`
  ADD CONSTRAINT `FK_raw_data_fieldid` FOREIGN KEY (`fieldid`) REFERENCES `fields` (`fieldid`);

--
-- Constraints for table `tables`
--
ALTER TABLE `tables`
  ADD CONSTRAINT `FK_tables_templateid` FOREIGN KEY (`templateid`) REFERENCES `templates` (`templateid`);


