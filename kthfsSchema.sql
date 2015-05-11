CREATE TABLE `activity` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `activity` VARCHAR(128) NOT NULL,
    `performed_by` VARCHAR(255) NOT NULL,
    `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `flag` VARCHAR(128) DEFAULT NULL,
    `activity_on` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=ndbcluster;

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

CREATE TABLE `inodes` (
    `id` MEDIUMINT(9) NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(128) NOT NULL,
    `pid` MEDIUMINT(9) DEFAULT NULL,
    `modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_dir` TINYINT(1) NOT NULL,
    `size` INT(11) DEFAULT NULL,
    `status` VARCHAR(128) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY (`pid`,`name`),
    KEY (`pid`,`is_dir`),
    KEY (`name`),
    FOREIGN KEY (`pid`) REFERENCES `inodes` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;



CREATE TABLE `users` (
    `uid` int(10) NOT NULL AUTO_INCREMENT,
    `username` varchar(10)  NOT NULL,
    `password` varchar(128)  NOT NULL,
    `email` varchar(45)  DEFAULT NULL,
    `fname` varchar(30)  DEFAULT NULL,
    `lname` varchar(30)  DEFAULT NULL,
    `activated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `home_org` varchar(100)  DEFAULT NULL,
    `title` varchar(10)  DEFAULT NULL,
    `orcid` varchar(20)  DEFAULT NULL,
    `false_login` int(11) NOT NULL DEFAULT '-1',
    `isonline` tinyint(1) NOT NULL DEFAULT '0',
    `secret` varchar(20)  DEFAULT NULL,
    `validation_key` varchar(128)  DEFAULT NULL,
    `security_question` varchar(20)  DEFAULT NULL,
    `security_answer` varchar(128)  DEFAULT NULL,
    `yubikey_user` int(11) NOT NULL DEFAULT '0',
    `password_changed` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `notes` varchar(500)  DEFAULT NULL,
    `mobile` varchar(20)  DEFAULT NULL,
    `status` int(11) NOT NULL DEFAULT '-1',
    PRIMARY KEY (`uid`),
    UNIQUE KEY `username` (`username`),
    UNIQUE KEY `email` (`email`)
) ENGINE=ndbcluster AUTO_INCREMENT=10000;

CREATE TABLE `yubikey` (
    `serial` varchar(10)  DEFAULT NULL,
    `version` varchar(15)  DEFAULT NULL,
    `notes` varchar(100)  DEFAULT NULL,
    `counter` int(11) DEFAULT NULL,
    `low` int(11) DEFAULT NULL,
    `high` int(11) DEFAULT NULL,
    `session_use` int(11) DEFAULT NULL,
    `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `aes_secret` varchar(100)  DEFAULT NULL,
    `public_id` varchar(40)  DEFAULT NULL,
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
    )ENGINE=ndbcluster;


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
    `name` varchar(128) NOT NULL,
    `username` varchar(45)NOT NULL,
    `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `retention_period` date DEFAULT NULL,
    `ethical_status` varchar(30) DEFAULT NULL,
    PRIMARY KEY (`name`),
    FOREIGN KEY (`username`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `study_meta` (
    `id` VARCHAR(128) NOT NULL,
    `studyname` VARCHAR(128) NOT NULL,
    `description` VARCHAR(2000) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`studyname`),
    FOREIGN KEY (`studyname`) REFERENCES `study` (`name`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `study_services` (
    `study` VARCHAR(128) NOT NULL,
    `service` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`study`,`service`),
    FOREIGN KEY (`study`) REFERENCES `study` (`name`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `study_team` (
    `name` VARCHAR(128) NOT NULL,
    `team_member` VARCHAR(45) NOT NULL,
    `team_role` VARCHAR(32) NOT NULL,
    `added` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`name`,`team_member`),
    FOREIGN KEY (`name`) REFERENCES `study` (`name`) ON DELETE CASCADE ON UPDATE NO ACTION,
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
    `studyname` VARCHAR(128) DEFAULT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`studyname`) REFERENCES `study` (`name`) ON DELETE NO ACTION ON UPDATE NO ACTION
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
    `study` VARCHAR(128) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`acronym`),
    FOREIGN KEY (`study`) REFERENCES `study` (`name`) ON DELETE NO ACTION ON UPDATE NO ACTION,
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
    `study_id` VARCHAR(128) DEFAULT NULL,
    `design` VARCHAR(128) DEFAULT NULL,
     FOREIGN KEY (`study_id`) REFERENCES `study_meta` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `study_inclusion_criteria` (
    `study_id` VARCHAR(128) NOT NULL,
    `criterium` VARCHAR(128) NOT NULL,
    PRIMARY KEY (`study_id`,`criterium`),
    FOREIGN KEY (`study_id`) REFERENCES `study_meta` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
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
    `study` VARCHAR(128) NOT NULL,
    `user` VARCHAR(45) NOT NULL,
    `state` VARCHAR(128) NOT NULL,
    `execution_duration` BIGINT(20) DEFAULT NULL,
    `args` VARCHAR(255) DEFAULT NULL,
    `stdout_path` VARCHAR(255) DEFAULT NULL,
    `stderr_path` VARCHAR(255) DEFAULT NULL,
    `type` VARCHAR(128) NOT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`study`) REFERENCES `study` (`name`) ON DELETE CASCADE ON UPDATE NO ACTION,
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
    `study_name` varchar(128) DEFAULT NULL,
    `status` varchar(30) DEFAULT NULL,
    `name` varchar(80) DEFAULT NULL,
    `type` varchar(30) DEFAULT NULL,
    `consent_form` longblob DEFAULT NULL,
     PRIMARY KEY (`id`),
    FOREIGN KEY (`study_name`) REFERENCES `study` (`name`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

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
    CONSTRAINT `fk_Organization` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE
) ENGINE=ndbcluster;

CREATE TABLE `collection_type` (
    `collection_id` VARCHAR(255) NOT NULL,
    `type` VARCHAR(128) NOT NULL,
    PRIMARY KEY (`collection_id`,`type`),
    FOREIGN KEY (`collection_id`) REFERENCES `samplecollections` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE VIEW `activity_details` AS
    select `activity`.`id` AS `id`,
    `activity`.`performed_by` AS `performed_by_email`,
    concat(`users`.`fname`,' ',`users`.`lname`) AS `performed_by_name`,
    `activity`.`activity` AS `description`,
    `activity`.`activity_on` AS `studyname`,
    `activity`.`created` AS `created`
    from
    (`activity` join `users` on((`activity`.`performed_by` = `users`.`email`)));

    CREATE VIEW `study_details` AS
    select `study`.`name` AS `studyname`,
    `study`.`username` AS `email`,
    concat(`users`.`fname`,' ',`users`.`lname`) AS `creator`
    from
    (`study` join `users` on((`study`.`username` = `users`.`email`)))
    where `study`.`name` in
    (select `study_team`.`name` from `study_team`);

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
  PRIMARY KEY (`fieldid`),
  KEY `FK_fields_tableid` (`tableid`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=0 ;

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
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=0 ;

--
-- Table structure for table `tables`
--

CREATE TABLE IF NOT EXISTS `tables` (
  `tableid` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `templateid` int(11) NOT NULL,
  PRIMARY KEY (`tableid`),
  KEY `FK_tables_templateid` (`templateid`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=0 ;

--
-- Table structure for table `templates`
--

CREATE TABLE IF NOT EXISTS `templates` (
  `templateid` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(250) NOT NULL,
  PRIMARY KEY (`templateid`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=0 ;

--
-- Table structure for table `tuple_to_file`
--

CREATE TABLE IF NOT EXISTS `tuple_to_file` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `inodeid` int(11) DEFAULT NULL,
  `tupleid` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=0 ;

--
-- Table structure for table `field_predefined_values`
--

CREATE TABLE IF NOT EXISTS `field_predefined_values` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `fieldid` int(11) NOT NULL,
  `valuee` varchar(250) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=10 ;


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

--
-- Table structure for table `inodes_ops_deleted`
--

CREATE TABLE IF NOT EXISTS `inodes_ops_deleted` (
  `inodeid` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Constraints for dumped tables
--

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


