
CREATE TABLE `bbc_group` (
  `group_name` VARCHAR(20) NOT NULL,
  `group_desc` VARCHAR(200) DEFAULT NULL,
  `gid` INT(11) NOT NULL,
  PRIMARY KEY (`gid`)
) ENGINE=ndbcluster;

CREATE TABLE `users` (
  `uid` INT(16) NOT NULL DEFAULT '1000',
  `username` VARCHAR(16) NOT NULL,
  `password` VARCHAR(128) NOT NULL,
  `email` VARCHAR(45) DEFAULT NULL,
  `fname` VARCHAR(30) DEFAULT NULL,
  `lname` VARCHAR(30) DEFAULT NULL,
  `activated` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `title` VARCHAR(10)  DEFAULT NULL,
  `orcid` VARCHAR(20)  DEFAULT NULL,
  `false_login` INT(11) NOT NULL DEFAULT '-1',
  `isonline` tinyINT(1) NOT NULL DEFAULT '0',
  `secret` VARCHAR(20)  DEFAULT NULL,
  `validation_key` VARCHAR(128)  DEFAULT NULL,
  `security_question` VARCHAR(20)  DEFAULT NULL,
  `security_answer` VARCHAR(128)  DEFAULT NULL,
  `yubikey_user` INT(11) NOT NULL DEFAULT '0',
  `password_changed` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
  `notes` VARCHAR(500)  DEFAULT NULL,
  `mobile` VARCHAR(20)  DEFAULT NULL,
  `status` INT(11) NOT NULL DEFAULT '-1',
  PRIMARY KEY (`uid`),
  UNIQUE (`username`),
  UNIQUE (`email`)
) ENGINE=ndbcluster AUTO_INCREMENT=10000;

CREATE TABLE `yubikey` (
  `serial` VARCHAR(10)  DEFAULT NULL,
  `version` VARCHAR(15)  DEFAULT NULL,
  `notes` VARCHAR(100)  DEFAULT NULL,
  `counter` INT(11) DEFAULT NULL,
  `low` INT(11) DEFAULT NULL,
  `high` INT(11) DEFAULT NULL,
  `session_use` INT(11) DEFAULT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `aes_secret` VARCHAR(100)  DEFAULT NULL,
  `public_id` VARCHAR(40)  DEFAULT NULL,
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

CREATE TABLE `project` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `inode_pid` INT(11) NOT NULL,
  `inode_name` VARCHAR(255) NOT NULL,
  `projectname` VARCHAR(128) NOT NULL,
  `username` VARCHAR(45) NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `retention_period` DATE DEFAULT NULL,
  `ethical_status` VARCHAR(30) DEFAULT NULL,
  `archived` TINYINT(1) DEFAULT '0',
  `deleted` TINYINT(1) DEFAULT '0',
  `description` VARCHAR(3000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY(`projectname`),
  UNIQUE KEY(`inode_pid`, `inode_name`),
  FOREIGN KEY (`username`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY (`inode_pid`,`inode_name`) REFERENCES `hops`.`hdfs_inodes`(`parent_id`,`name`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster CHARSET=latin1;

CREATE TABLE `activity` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `activity` VARCHAR(128) NOT NULL,
  `user_id` INT(10) NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `flag` VARCHAR(128) DEFAULT NULL,
  `project_id` INT(11) NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  FOREIGN KEY (`user_id`) REFERENCES `users` (`uid`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `project_services` (
  `project_id` INT(11) NOT NULL,
  `service` VARCHAR(32) NOT NULL,
  PRIMARY KEY (`project_id`,`service`),
  FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `project_team` (
  `project_id` INT(11) NOT NULL,
  `team_member` VARCHAR(45) NOT NULL,
  `team_role` VARCHAR(32) NOT NULL,
  `added` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`project_id`,`team_member`),
  FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
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

CREATE TABLE `jobs` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) DEFAULT NULL,
  `creation_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `project_id` INT(11) NOT NULL,
  `creator` VARCHAR(45) NOT NULL,
  `type` VARCHAR(128) NOT NULL,
  `json_config` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY (`creator`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `executions` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `submission_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `user` VARCHAR(45) NOT NULL,
  `state` VARCHAR(128) NOT NULL,
  `execution_duration` BIGINT(20) DEFAULT NULL,
  `stdout_path` VARCHAR(255) DEFAULT NULL,
  `stderr_path` VARCHAR(255) DEFAULT NULL,
  `app_id` CHAR(30) DEFAULT NULL,
  `job_id` INT(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE (`app_id`),
  FOREIGN KEY (`job_id`) REFERENCES `jobs` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY (`user`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `job_output_files` (
  `execution_id` INT(11) NOT NULL,
  `path` VARCHAR(255) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`execution_id`,`name`),
  FOREIGN KEY (`execution_id`) REFERENCES `executions` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `job_input_files` (
  `execution_id` INT(11) NOT NULL,
  `path` VARCHAR(255) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`execution_id`,`name`),
  FOREIGN KEY (`execution_id`) REFERENCES `executions` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `consent` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `date` DATE DEFAULT NULL,
  `project_id` INT(11) NOT NULL,
  `status` VARCHAR(30) DEFAULT NULL,
  `name` VARCHAR(80) DEFAULT NULL,
  `type` VARCHAR(30) DEFAULT NULL,
  `consent_form` longblob DEFAULT NULL,
   PRIMARY KEY (`id`),
  FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `organization` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `uid` INT(11) DEFAULT NULL,
    `org_name` VARCHAR(100) DEFAULT NULL,
    `website` VARCHAR(200) DEFAULT NULL,
    `contact_person` VARCHAR(100) DEFAULT NULL,
    `contact_email` VARCHAR(100) DEFAULT NULL,
    `department` VARCHAR(100) DEFAULT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `fax` VARCHAR(20) DEFAULT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE
) ENGINE=ndbcluster;

-- Metadata --------------
-- ------------------------

CREATE TABLE `meta_templates` (
  `templateid` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(250) NOT NULL,
  PRIMARY KEY (`templateid`)
) ENGINE=ndbcluster;

CREATE TABLE `meta_field_types` (
  `id` MEDIUMINT(9) NOT NULL AUTO_INCREMENT,
  `description` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=ndbcluster;

CREATE TABLE `meta_tables` (
  `tableid` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) DEFAULT NULL,
  `templateid` INT(11) NOT NULL,
  PRIMARY KEY (`tableid`),
  FOREIGN KEY (`templateid`) REFERENCES `meta_templates` (`templateid`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `meta_fields` (
  `fieldid` INT(11) NOT NULL AUTO_INCREMENT,
  `maxsize` INT(11) DEFAULT NULL,
  `name` VARCHAR(255) DEFAULT NULL,
  `required` SMALLINT(6) DEFAULT NULL,
  `searchable` SMALLINT(6) DEFAULT NULL,
  `tableid` INT(11) DEFAULT NULL,
  `type` VARCHAR(255) DEFAULT NULL,
  `description` VARCHAR(250) NOT NULL,
  `fieldtypeid` MEDIUMINT(11) NOT NULL,
  `position` MEDIUMINT(11) DEFAULT '0',
  PRIMARY KEY (`fieldid`),
  FOREIGN KEY (`tableid`) REFERENCES `meta_tables` (`tableid`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY (`fieldtypeid`) REFERENCES `meta_field_types` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `meta_field_predefined_values` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `fieldid` INT(11) NOT NULL,
  `valuee` VARCHAR(250) NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`fieldid`) REFERENCES `meta_fields` (`fieldid`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `meta_tuple_to_file` (
  `tupleid` INT(11) NOT NULL AUTO_INCREMENT,
  `inode_pid` INT(11) NOT NULL,
  `inode_name` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`tupleid`),
  UNIQUE KEY(`inode_pid`, `inode_name`),
  FOREIGN KEY (`inode_pid`, `inode_name`) REFERENCES `hops`.`hdfs_inodes` (`parent_id`, `name`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `meta_raw_data` (
  `fieldid` INT(11) NOT NULL,
  `tupleid` INT(11) NOT NULL,
  PRIMARY KEY (`fieldid`, `tupleid`),
  FOREIGN KEY (`fieldid`) REFERENCES `meta_fields` (`fieldid`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY (`tupleid`) REFERENCES `meta_tuple_to_file` (`tupleid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `meta_data` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `data` LONGTEXT NOT NULL,
  `fieldid` INT(11) NOT NULL,
  `tupleid` INT(11) NOT NULL,
  PRIMARY KEY (`id`, `fieldid`, `tupleid`),
  FOREIGN KEY (`fieldid`, `tupleid`) REFERENCES `meta_raw_data` (`fieldid`, `tupleid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;

CREATE TABLE `meta_template_to_inode` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `template_id` INT(11) NOT NULL,
  `inode_pid` INT(11) NOT NULL,
  `inode_name` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY(`inode_pid`, `inode_name`),
  FOREIGN KEY (`template_id`) REFERENCES `meta_templates` (`templateid`) ON DELETE CASCADE ON UPDATE NO ACTION,
  FOREIGN KEY (`inode_pid`,`inode_name`) REFERENCES `hops`.`hdfs_inodes`(`parent_id`,`name`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster CHARSET=latin1;

CREATE TABLE `meta_inode_basic_metadata` (
  `inode_pid` INT(11) NOT NULL,
  `inode_name` VARCHAR(255) NOT NULL,
  `description` VARCHAR(3000) DEFAULT NULL,
  `searchable` TINYINT(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`inode_pid`, `inode_name`),
  FOREIGN KEY (`inode_pid`, `inode_name`) REFERENCES `hops`.`hdfs_inodes`(`parent_id`, `name`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster CHARSET=latin1;
 
-- elastic jdbc-importer buffer tables -------

CREATE TABLE `meta_inodes_ops_parents_buffer` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `inodeid` INT(11) NOT NULL,
  `parentid` INT(11) NOT NULL,
  `operation` SMALLINT(11) NOT NULL,
  `logical_time` INT(11) NOT NULL,
  `processed` TINYINT(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`,`inodeid`,`parentid`)
) ENGINE=ndbcluster AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `meta_inodes_ops_datasets_buffer` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `inodeid` INT(11) NOT NULL,
  `parentid` INT(11) NOT NULL,
  `operation` SMALLINT(11) NOT NULL,
  `logical_time` INT(11) NOT NULL,
  `processed` TINYINT(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`,`inodeid`,`parentid`)
) ENGINE=ndbcluster AUTO_INCREMENT=85 DEFAULT CHARSET=utf8;

CREATE TABLE `meta_inodes_ops_children_pr_buffer` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `inodeid` INT(11) NOT NULL,
  `parentid` INT(11) NOT NULL,
  `operation` SMALLINT(11) NOT NULL,
  `logical_time` INT(11) NOT NULL,
  `processed` TINYINT(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`,`inodeid`,`parentid`)
) ENGINE=ndbcluster AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `meta_inodes_ops_children_ds_buffer` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `inodeid` INT(11) NOT NULL,
  `parentid` INT(11) NOT NULL,
  `operation` SMALLINT(11) NOT NULL,
  `logical_time` INT(11) NOT NULL,
  `processed` TINYINT(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`,`inodeid`,`parentid`)
) ENGINE=ndbcluster AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- Dataset table-------------------------------------------------
CREATE TABLE `dataset` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `inode_pid` INT(11) NOT NULL,
  `inode_name` VARCHAR(255) NOT NULL,
  `projectId` INT(11) NOT NULL,
  `description` VARCHAR(3000) DEFAULT NULL,
  `editable` TINYINT(1) NOT NULL DEFAULT '1',
  `status` TINYINT(1) NOT NULL DEFAULT '1',
  `searchable` TINYINT(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_dataset` (`inode_pid`,`projectId`,`inode_name`),
  KEY `fk_dataset_2_idx` (`projectId`),
  KEY `fk_dataset_1_idx` (`inode_pid`,`inode_name`),
  CONSTRAINT `fk_dataset_2` FOREIGN KEY (`projectId`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `fk_dataset_1` FOREIGN KEY (`inode_pid`,`inode_name`) REFERENCES `hops`.`hdfs_inodes` (`parent_id`,`name`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1;

CREATE TABLE `dataset_request` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `dataset` INT(11) NOT NULL,
  `projectId` INT(11) NOT NULL,
  `user_email` VARCHAR(45) NOT NULL,
  `requested` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `message` VARCHAR(3000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index2` (`dataset`,`projectId`),
  KEY `fk_dataset_request_2_idx` (`projectId`,`user_email`),
  CONSTRAINT `fk_dataset_request_2` FOREIGN KEY (`projectId`,`user_email`) REFERENCES `project_team` (`project_id`,`team_member`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `fk_dataset_request_1` FOREIGN KEY (`dataset`) REFERENCES `dataset` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;


-- Glassfish timers
-- ----------------------

CREATE TABLE `EJB__TIMER__TBL` (
  `CREATIONTIMERAW` bigint(20) NOT NULL,
  `BLOB` blob,
  `TIMERID` varchar(255) NOT NULL,
  `CONTAINERID` bigint(20) NOT NULL,
  `OWNERID` varchar(255) DEFAULT NULL,
  `STATE` int(11) NOT NULL,
  `PKHASHCODE` int(11) NOT NULL,
  `INTERVALDURATION` bigint(20) NOT NULL,
  `INITIALEXPIRATIONRAW` bigint(20) NOT NULL,
  `LASTEXPIRATIONRAW` bigint(20) NOT NULL,
  `SCHEDULE` varchar(255) DEFAULT NULL,
  `APPLICATIONID` bigint(20) NOT NULL,
  PRIMARY KEY (`TIMERID`)
) ENGINE=ndbcluster;

-- VIEWS --------------
-- ---------------------

CREATE VIEW `users_groups` AS 
  select `u`.`username` AS `username`,
  `u`.`password` AS `password`,
  `u`.`secret` AS `secret`,
  `u`.`email` AS `email`,
  `g`.`group_name` AS `group_name` 
  from 
    ((`people_group` `ug` join `users` `u` on((`u`.`uid` = `ug`.`uid`))) join `bbc_group` `g` on((`g`.`gid` = `ug`.`gid`)));




-- SSH Access -----------
-- -----------------------

CREATE TABLE `ssh_keys` (
  `uid` INT(11) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `public_key` VARCHAR(2000) NOT NULL,
  PRIMARY KEY (`uid`, `name`),
  KEY `name_idx` (`name`),
  KEY `uid_idx` (`uid`),
  CONSTRAINT `FK_248_381` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster;


CREATE VIEW `hops_users` AS select concat(`pt`.`team_member`,'__',`p`.`projectname`) AS `project_user` from ((`project` `p` join `project_team` `pt`) join `ssh_keys` `sk`) where `pt`.`team_member` in (select `u`.`email` from (`users` `u` join `ssh_keys` `s`) where (`u`.`uid` = `s`.`uid`)); 

CREATE TABLE authorized_sshkeys (project VARCHAR(64) NOT NULL, user VARCHAR(48) NOT NULL, sshkey_name VARCHAR(64) NOT NULL, PRIMARY KEY (project, user, sshkey_name), KEY idx_user(user), KEY idx_project(project)) engine=ndbcluster;



