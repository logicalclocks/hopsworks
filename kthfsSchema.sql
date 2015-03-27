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
  `uid` INT(10) NOT NULL DEFAULT '1000',
  `username` VARCHAR(10) NOT NULL,
  `password` VARCHAR(128) NOT NULL,
  `email` VARCHAR(45) DEFAULT NULL,
  `fname` VARCHAR(30) DEFAULT NULL,
  `lname` VARCHAR(30) DEFAULT NULL,
  `activated` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `home_org` VARCHAR(100) DEFAULT NULL,
  `title` VARCHAR(10) DEFAULT NULL,
  `orcid` VARCHAR(20) DEFAULT NULL,
  `false_login` INT(11) NOT NULL DEFAULT '-1',
  `isonline` TINYINT(1) NOT NULL DEFAULT '0',
  `secret` VARCHAR(20) DEFAULT NULL,
  `security_question` VARCHAR(20) DEFAULT NULL,
  `security_answer` VARCHAR(128) DEFAULT NULL,
  `yubikey_user` TINYINT(1) NOT NULL DEFAULT '0',
  `password_changed` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
  `notes` VARCHAR(500) DEFAULT NULL,
  `mobile` VARCHAR(20) DEFAULT NULL,
  `status` INT(11) NOT NULL DEFAULT '-1',
  PRIMARY KEY (`uid`),
  UNIQUE (`username`),
  UNIQUE (`email`)
) ENGINE=ndbcluster;

CREATE TABLE `yubikey` (
  `serial` VARCHAR(10) DEFAULT NULL,
  `version` VARCHAR(15) DEFAULT NULL,
  `notes` VARCHAR(100) DEFAULT NULL,
  `counter` INT(11) DEFAULT NULL,
  `low` INT(11) DEFAULT NULL,
  `high` INT(11) DEFAULT NULL,
  `session_use` INT(11) DEFAULT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `aes_secret` VARCHAR(100) DEFAULT NULL,
  `public_id` VARCHAR(40) DEFAULT NULL,
  `accessed` TIMESTAMP NULL DEFAULT NULL,
  `status` INT(11) DEFAULT '-1',
  `yubidnum` INT(11) NOT NULL AUTO_INCREMENT,
  `uid` INT(11) NOT NULL,
  PRIMARY KEY (`yubidnum`),
  UNIQUE (`uid`),
  UNIQUE (`serial`),
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
  `name` VARCHAR(128) NOT NULL,
  `username` VARCHAR(45) NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
  `id` INT(11) NOT NULL,
  `added` date DEFAULT NULL,
  `study_name` VARCHAR(128) DEFAULT NULL,
  `retention_period` date DEFAULT NULL,
  `consent_form` BLOB,
  `status` VARCHAR(30) DEFAULT NULL,
  `name` VARCHAR(80) DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`study_name`) REFERENCES `study` (`name`) ON DELETE CASCADE ON UPDATE NO ACTION
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