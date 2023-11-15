-- ----------------------------------------------------------------------------
-- MySQL Workbench Migration
-- Migrated Schemata: hopsworks
-- Source Schemata: hopsworks
-- Created: Tue Oct 24 13:52:16 2023
-- Workbench Version: 8.0.32
-- ----------------------------------------------------------------------------

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------------------------
-- Table hopsworks.account_audit
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`account_audit`
(
    `log_id`           BIGINT                              NOT NULL AUTO_INCREMENT,
    `initiator`        INT                                 NOT NULL,
    `target`           INT                                 NOT NULL,
    `action`           VARCHAR(45) CHARACTER SET 'latin1'  NULL DEFAULT NULL,
    `action_timestamp` TIMESTAMP                           NULL DEFAULT NULL,
    `message`          VARCHAR(100) CHARACTER SET 'latin1' NULL DEFAULT NULL,
    `outcome`          VARCHAR(45) CHARACTER SET 'latin1'  NULL DEFAULT NULL,
    `ip`               VARCHAR(45) CHARACTER SET 'latin1'  NULL DEFAULT NULL,
    `useragent`        VARCHAR(255) CHARACTER SET 'latin1' NULL DEFAULT NULL,
    PRIMARY KEY (`log_id`),
    INDEX `initiator` (`initiator` ASC) VISIBLE,
    INDEX `target` (`target` ASC) VISIBLE,
    CONSTRAINT `FK_257_274`
        FOREIGN KEY (`initiator`)
            REFERENCES `hopsworks`.`users` (`uid`),
    CONSTRAINT `FK_257_275`
        FOREIGN KEY (`target`)
            REFERENCES `hopsworks`.`users` (`uid`)
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 515
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.activity
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`activity`
(
    `id`         INT                                 NOT NULL AUTO_INCREMENT,
    `activity`   VARCHAR(255) CHARACTER SET 'latin1' NOT NULL,
    `user_id`    INT                                 NOT NULL,
    `created`    TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `flag`       VARCHAR(128) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `project_id` INT                                 NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `project_id` (`project_id` ASC) VISIBLE,
    INDEX `user_id` (`user_id` ASC) VISIBLE,
    CONSTRAINT `FK_257_296`
        FOREIGN KEY (`user_id`)
            REFERENCES `hopsworks`.`users` (`uid`),
    CONSTRAINT `FK_284_295`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1547
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.alert_manager_config
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`alert_manager_config`
(
    `id`      INT        NOT NULL AUTO_INCREMENT,
    `content` MEDIUMBLOB NOT NULL,
    `created` TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.alert_receiver
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`alert_receiver`
(
    `id`     INT          NOT NULL AUTO_INCREMENT,
    `name`   VARCHAR(128) NOT NULL,
    `config` BLOB         NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `name_UNIQUE` (`name` ASC) VISIBLE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.api_key
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`api_key`
(
    `id`       INT          NOT NULL AUTO_INCREMENT,
    `prefix`   VARCHAR(45)  NOT NULL,
    `secret`   VARCHAR(512) NOT NULL,
    `salt`     VARCHAR(256) NOT NULL,
    `created`  TIMESTAMP    NOT NULL,
    `modified` TIMESTAMP    NOT NULL,
    `name`     VARCHAR(45)  NOT NULL,
    `user_id`  INT          NOT NULL,
    `reserved` TINYINT(1)   NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `prefix_UNIQUE` (`prefix` ASC) VISIBLE,
    UNIQUE INDEX `index4` (`user_id` ASC, `name` ASC) VISIBLE,
    INDEX `fk_api_key_1_idx` (`user_id` ASC) VISIBLE,
    CONSTRAINT `fk_api_key_1`
        FOREIGN KEY (`user_id`)
            REFERENCES `hopsworks`.`users` (`uid`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.api_key_scope
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`api_key_scope`
(
    `id`      INT         NOT NULL AUTO_INCREMENT,
    `api_key` INT         NOT NULL,
    `scope`   VARCHAR(45) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `index2` (`api_key` ASC, `scope` ASC) VISIBLE,
    CONSTRAINT `fk_api_key_scope_1`
        FOREIGN KEY (`api_key`)
            REFERENCES `hopsworks`.`api_key` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.bbc_group
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`bbc_group`
(
    `group_name` VARCHAR(20) CHARACTER SET 'latin1'  NOT NULL,
    `group_desc` VARCHAR(200) CHARACTER SET 'latin1' NULL DEFAULT NULL,
    `gid`        INT                                 NOT NULL,
    PRIMARY KEY (`gid`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.cached_feature
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`cached_feature`
(
    `id`                      INT                                NOT NULL AUTO_INCREMENT,
    `cached_feature_group_id` INT                                NULL     DEFAULT NULL,
    `stream_feature_group_id` INT                                NULL     DEFAULT NULL,
    `name`                    VARCHAR(63) CHARACTER SET 'latin1' NOT NULL,
    `description`             VARCHAR(256)                       NOT NULL DEFAULT '',
    PRIMARY KEY (`id`),
    INDEX `cached_feature_group_fk` (`cached_feature_group_id` ASC) VISIBLE,
    INDEX `stream_feature_group_fk` (`stream_feature_group_id` ASC) VISIBLE,
    CONSTRAINT `cached_feature_group_fk2`
        FOREIGN KEY (`cached_feature_group_id`)
            REFERENCES `hopsworks`.`cached_feature_group` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `stream_feature_group_fk2`
        FOREIGN KEY (`stream_feature_group_id`)
            REFERENCES `hopsworks`.`stream_feature_group` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.cached_feature_extra_constraints
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`cached_feature_extra_constraints`
(
    `id`                      INT                                NOT NULL AUTO_INCREMENT,
    `cached_feature_group_id` INT                                NULL     DEFAULT NULL,
    `stream_feature_group_id` INT                                NULL     DEFAULT NULL,
    `name`                    VARCHAR(63) CHARACTER SET 'latin1' NOT NULL,
    `primary_column`          TINYINT(1)                         NOT NULL DEFAULT '0',
    `hudi_precombine_key`     TINYINT(1)                         NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    INDEX `cached_feature_group_fk` (`cached_feature_group_id` ASC) VISIBLE,
    INDEX `stream_feature_group_fk` (`stream_feature_group_id` ASC) VISIBLE,
    CONSTRAINT `cached_feature_group_constraint_fk`
        FOREIGN KEY (`cached_feature_group_id`)
            REFERENCES `hopsworks`.`cached_feature_group` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `stream_feature_group_constraint_fk`
        FOREIGN KEY (`stream_feature_group_id`)
            REFERENCES `hopsworks`.`stream_feature_group` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.cached_feature_group
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`cached_feature_group`
(
    `id`                INT NOT NULL AUTO_INCREMENT,
    `timetravel_format` INT NOT NULL DEFAULT '1',
    PRIMARY KEY (`id`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.cloud_role_mapping
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`cloud_role_mapping`
(
    `id`           INT           NOT NULL AUTO_INCREMENT,
    `project_id`   INT           NOT NULL,
    `project_role` VARCHAR(32)   NOT NULL,
    `cloud_role`   VARCHAR(2048) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `index3` (`project_id` ASC, `cloud_role`(255) ASC) VISIBLE,
    UNIQUE INDEX `index4` (`id` ASC, `project_id` ASC, `project_role` ASC) VISIBLE,
    INDEX `fk_cloud_role_mapping_1_idx` (`project_id` ASC) VISIBLE,
    CONSTRAINT `fk_cloud_role_mapping_1`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.cloud_role_mapping_default
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`cloud_role_mapping_default`
(
    `id`           INT         NOT NULL AUTO_INCREMENT,
    `mapping_id`   INT         NOT NULL,
    `project_id`   INT         NOT NULL,
    `project_role` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `index3` (`project_id` ASC, `project_role` ASC) VISIBLE,
    UNIQUE INDEX `index4` (`mapping_id` ASC, `project_id` ASC, `project_role` ASC) VISIBLE,
    INDEX `fk_cloud_role_mapping_default_1_idx` (`mapping_id` ASC, `project_id` ASC, `project_role` ASC) VISIBLE,
    CONSTRAINT `fk_cloud_role_mapping_default_1`
        FOREIGN KEY (`mapping_id`, `project_id`, `project_role`)
            REFERENCES `hopsworks`.`cloud_role_mapping` (`id`, `project_id`, `project_role`)
            ON DELETE CASCADE
            ON UPDATE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.cluster_cert
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`cluster_cert`
(
    `id`                       INT                                 NOT NULL AUTO_INCREMENT,
    `agent_id`                 INT                                 NOT NULL,
    `common_name`              VARCHAR(64) CHARACTER SET 'latin1'  NOT NULL,
    `organization_name`        VARCHAR(64) CHARACTER SET 'latin1'  NOT NULL,
    `organizational_unit_name` VARCHAR(64) CHARACTER SET 'latin1'  NOT NULL,
    `serial_number`            VARCHAR(45) CHARACTER SET 'latin1'  NULL     DEFAULT NULL,
    `registration_status`      VARCHAR(45) CHARACTER SET 'latin1'  NOT NULL,
    `validation_key`           VARCHAR(128) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `validation_key_date`      TIMESTAMP                           NULL     DEFAULT NULL,
    `registration_date`        TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `organization_name` (`organization_name` ASC, `organizational_unit_name` ASC) VISIBLE,
    UNIQUE INDEX `serial_number` (`serial_number` ASC) VISIBLE,
    INDEX `agent_id` (`agent_id` ASC) VISIBLE,
    CONSTRAINT `FK_257_552`
        FOREIGN KEY (`agent_id`)
            REFERENCES `hopsworks`.`users` (`uid`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 7
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.command_search_fs
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`command_search_fs`
(
    `id`                  BIGINT         NOT NULL AUTO_INCREMENT,
    `inode_id`            BIGINT         NOT NULL,
    `project_id`          INT            NULL DEFAULT NULL,
    `op`                  VARCHAR(20)    NOT NULL,
    `status`              VARCHAR(20)    NOT NULL,
    `feature_group_id`    INT            NULL DEFAULT NULL,
    `feature_view_id`     INT            NULL DEFAULT NULL,
    `training_dataset_id` INT            NULL DEFAULT NULL,
    `error_message`       VARCHAR(10000) NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_command_search_feature_group` (`feature_group_id` ASC) VISIBLE,
    INDEX `fk_command_search_feature_view` (`feature_view_id` ASC) VISIBLE,
    INDEX `fk_command_search_training_dataset` (`training_dataset_id` ASC) VISIBLE,
    CONSTRAINT `fk_command_search_feature_group`
        FOREIGN KEY (`feature_group_id`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE SET NULL,
    CONSTRAINT `fk_command_search_feature_view`
        FOREIGN KEY (`feature_view_id`)
            REFERENCES `hopsworks`.`feature_view` (`id`)
            ON DELETE SET NULL,
    CONSTRAINT `fk_command_search_training_dataset`
        FOREIGN KEY (`training_dataset_id`)
            REFERENCES `hopsworks`.`training_dataset` (`id`)
            ON DELETE SET NULL
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.command_search_fs_history
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`command_search_fs_history`
(
    `h_id`                BIGINT         NOT NULL AUTO_INCREMENT,
    `executed`            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `id`                  BIGINT         NOT NULL,
    `inode_id`            BIGINT         NOT NULL,
    `project_id`          INT            NULL     DEFAULT NULL,
    `op`                  VARCHAR(20)    NOT NULL,
    `status`              VARCHAR(20)    NOT NULL,
    `feature_group_id`    INT            NULL     DEFAULT NULL,
    `feature_view_id`     INT            NULL     DEFAULT NULL,
    `training_dataset_id` INT            NULL     DEFAULT NULL,
    `error_message`       VARCHAR(10000) NULL     DEFAULT NULL,
    PRIMARY KEY (`h_id`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.conda_commands
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`conda_commands`
(
    `id`                   INT                                   NOT NULL AUTO_INCREMENT,
    `project_id`           INT                                   NOT NULL,
    `op`                   VARCHAR(52) CHARACTER SET 'latin1'    NOT NULL,
    `channel_url`          VARCHAR(255) CHARACTER SET 'latin1'   NULL     DEFAULT NULL,
    `arg`                  VARCHAR(11000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `lib`                  VARCHAR(255) CHARACTER SET 'latin1'   NULL     DEFAULT NULL,
    `version`              VARCHAR(52) CHARACTER SET 'latin1'    NULL     DEFAULT NULL,
    `status`               VARCHAR(52) CHARACTER SET 'latin1'    NOT NULL,
    `created`              TIMESTAMP(3)                          NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `install_type`         VARCHAR(52) CHARACTER SET 'latin1'    NULL     DEFAULT NULL,
    `environment_file`     VARCHAR(1000) CHARACTER SET 'latin1'  NULL     DEFAULT NULL,
    `custom_commands_file` VARCHAR(255) CHARACTER SET 'latin1'   NULL     DEFAULT NULL,
    `install_jupyter`      TINYINT(1)                            NOT NULL DEFAULT '0',
    `git_api_key_name`     VARCHAR(125)                          NULL     DEFAULT NULL,
    `git_backend`          VARCHAR(45)                           NULL     DEFAULT NULL,
    `user_id`              INT                                   NOT NULL,
    `error_message`        VARCHAR(10000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `project_id` (`project_id` ASC) VISIBLE,
    INDEX `user_fk_conda` (`user_id` ASC) VISIBLE,
    CONSTRAINT `user_fk_conda`
        FOREIGN KEY (`user_id`)
            REFERENCES `hopsworks`.`users` (`uid`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 32
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.databricks_instance
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`databricks_instance`
(
    `id`          INT          NOT NULL AUTO_INCREMENT,
    `url`         VARCHAR(255) NOT NULL,
    `uid`         INT          NOT NULL,
    `secret_name` VARCHAR(200) NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `db_secret_fk` (`uid` ASC, `secret_name` ASC) VISIBLE,
    CONSTRAINT `databricks_instance_ibfk_1`
        FOREIGN KEY (`uid`, `secret_name`)
            REFERENCES `hopsworks`.`secrets` (`uid`, `secret_name`)
            ON DELETE CASCADE,
    CONSTRAINT `databricks_instance_ibfk_2`
        FOREIGN KEY (`uid`)
            REFERENCES `hopsworks`.`users` (`uid`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.dataset
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`dataset`
(
    `id`               INT                                  NOT NULL AUTO_INCREMENT,
    `inode_name`       VARCHAR(255) CHARACTER SET 'latin1'  NOT NULL,
    `projectId`        INT                                  NOT NULL,
    `description`      VARCHAR(2000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `searchable`       TINYINT(1)                           NOT NULL DEFAULT '0',
    `public_ds`        TINYINT(1)                           NOT NULL DEFAULT '0',
    `public_ds_id`     VARCHAR(1000) CHARACTER SET 'latin1' NULL     DEFAULT '0',
    `dstype`           INT                                  NOT NULL DEFAULT '0',
    `feature_store_id` INT                                  NULL     DEFAULT NULL,
    `permission`       VARCHAR(45)                          NOT NULL DEFAULT 'READ_ONLY',
    PRIMARY KEY (`id`),
    INDEX `projectId_name` (`projectId` ASC, `inode_name` ASC) VISIBLE,
    INDEX `featurestore_fk` (`feature_store_id` ASC) VISIBLE,
    INDEX `dataset_name` (`inode_name` ASC) VISIBLE,
    CONSTRAINT `featurestore_fk`
        FOREIGN KEY (`feature_store_id`)
            REFERENCES `hopsworks`.`feature_store` (`id`)
            ON DELETE SET NULL,
    CONSTRAINT `FK_284_434`
        FOREIGN KEY (`projectId`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 747
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.dataset_request
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`dataset_request`
(
    `id`         INT                                                             NOT NULL AUTO_INCREMENT,
    `dataset`    INT                                                             NOT NULL,
    `projectId`  INT                                                             NOT NULL,
    `user_email` VARCHAR(150) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs' NOT NULL,
    `requested`  TIMESTAMP                                                       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `message`    VARCHAR(3000) CHARACTER SET 'latin1'                            NULL     DEFAULT NULL,
    `message_id` INT                                                             NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `index2` (`dataset` ASC, `projectId` ASC) VISIBLE,
    INDEX `projectId` (`projectId` ASC, `user_email` ASC) VISIBLE,
    INDEX `message_id` (`message_id` ASC) VISIBLE,
    CONSTRAINT `FK_302_451`
        FOREIGN KEY (`projectId`, `user_email`)
            REFERENCES `hopsworks`.`project_team` (`project_id`, `team_member`)
            ON DELETE CASCADE,
    CONSTRAINT `FK_429_449`
        FOREIGN KEY (`dataset`)
            REFERENCES `hopsworks`.`dataset` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `FK_438_452`
        FOREIGN KEY (`message_id`)
            REFERENCES `hopsworks`.`message` (`id`)
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 3
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.dataset_shared_with
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`dataset_shared_with`
(
    `id`          INT         NOT NULL AUTO_INCREMENT,
    `dataset`     INT         NOT NULL,
    `project`     INT         NOT NULL,
    `accepted`    TINYINT(1)  NOT NULL DEFAULT '0',
    `shared_on`   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `permission`  VARCHAR(45) NOT NULL DEFAULT 'READ_ONLY',
    `shared_by`   INT         NULL     DEFAULT NULL,
    `accepted_by` INT         NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `index2` (`dataset` ASC, `project` ASC) VISIBLE,
    INDEX `fk_dataset_shared_with_2_idx` (`project` ASC) VISIBLE,
    INDEX `fk_shared_by` (`shared_by` ASC) VISIBLE,
    INDEX `fk_accepted_by` (`accepted_by` ASC) VISIBLE,
    CONSTRAINT `fk_accepted_by`
        FOREIGN KEY (`accepted_by`)
            REFERENCES `hopsworks`.`users` (`uid`),
    CONSTRAINT `fk_dataset_shared_with_1`
        FOREIGN KEY (`dataset`)
            REFERENCES `hopsworks`.`dataset` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fk_dataset_shared_with_2`
        FOREIGN KEY (`project`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fk_shared_by`
        FOREIGN KEY (`shared_by`)
            REFERENCES `hopsworks`.`users` (`uid`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1;

-- ----------------------------------------------------------------------------
-- Table hopsworks.default_job_configuration
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`default_job_configuration`
(
    `project_id` INT                                   NOT NULL,
    `type`       VARCHAR(128) CHARACTER SET 'latin1'   NOT NULL,
    `config`     VARCHAR(12500) CHARACTER SET 'latin1' NULL DEFAULT NULL,
    PRIMARY KEY (`project_id`, `type`),
    CONSTRAINT `FK_JOBCONFIG_PROJECT`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.environment_history
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`environment_history`
(
    `id`                    INT                                  NOT NULL AUTO_INCREMENT,
    `project`               INT                                  NOT NULL,
    `docker_image`          VARCHAR(255) CHARACTER SET 'latin1'  NOT NULL,
    `downgraded`            VARCHAR(7000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `installed`             VARCHAR(7000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `uninstalled`           VARCHAR(7000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `upgraded`              VARCHAR(7000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `previous_docker_image` VARCHAR(255) CHARACTER SET 'latin1'  NOT NULL,
    `user`                  INT                                  NOT NULL,
    `created`               TIMESTAMP                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `env_project_fk` (`project` ASC) VISIBLE,
    CONSTRAINT `env_project_fk`
        FOREIGN KEY (`project`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.executions
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`executions`
(
    `id`              INT                                                             NOT NULL AUTO_INCREMENT,
    `submission_time` TIMESTAMP                                                       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `user`            VARCHAR(150) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs' NOT NULL,
    `state`           VARCHAR(128) CHARACTER SET 'latin1'                             NOT NULL,
    `execution_start` BIGINT                                                          NULL     DEFAULT NULL,
    `execution_stop`  BIGINT                                                          NULL     DEFAULT NULL,
    `stdout_path`     VARCHAR(255) CHARACTER SET 'latin1'                             NULL     DEFAULT NULL,
    `stderr_path`     VARCHAR(255) CHARACTER SET 'latin1'                             NULL     DEFAULT NULL,
    `hdfs_user`       VARCHAR(255) CHARACTER SET 'latin1'                             NULL     DEFAULT NULL,
    `args`            VARCHAR(10000) CHARACTER SET 'latin1'                           NOT NULL DEFAULT '',
    `app_id`          CHAR(45) CHARACTER SET 'latin1'                                 NULL     DEFAULT NULL,
    `job_id`          INT                                                             NOT NULL,
    `finalStatus`     VARCHAR(128) CHARACTER SET 'latin1'                             NOT NULL DEFAULT 'UNDEFINED',
    `progress`        FLOAT                                                           NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `app_id` (`app_id` ASC) VISIBLE,
    INDEX `job_id` (`job_id` ASC) VISIBLE,
    INDEX `user` (`user` ASC) VISIBLE,
    INDEX `submission_time_idx` (`submission_time` ASC, `job_id` ASC) VISIBLE,
    INDEX `state_idx` (`state` ASC, `job_id` ASC) VISIBLE,
    INDEX `finalStatus_idx` (`finalStatus` ASC, `job_id` ASC) VISIBLE,
    INDEX `progress_idx` (`progress` ASC, `job_id` ASC) VISIBLE,
    CONSTRAINT `FK_262_366`
        FOREIGN KEY (`user`)
            REFERENCES `hopsworks`.`users` (`email`)
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 23
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.expectation
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`expectation`
(
    `id`                   INT           NOT NULL AUTO_INCREMENT,
    `expectation_suite_id` INT           NOT NULL,
    `expectation_type`     VARCHAR(150)  NOT NULL,
    `kwargs`               VARCHAR(5000) NOT NULL,
    `meta`                 VARCHAR(1000) NULL DEFAULT '{}',
    PRIMARY KEY (`id`),
    INDEX `suite_fk` (`expectation_suite_id` ASC) VISIBLE,
    CONSTRAINT `suite_fk`
        FOREIGN KEY (`expectation_suite_id`)
            REFERENCES `hopsworks`.`expectation_suite` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.expectation_suite
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`expectation_suite`
(
    `id`                          INT           NOT NULL AUTO_INCREMENT,
    `feature_group_id`            INT           NOT NULL,
    `name`                        VARCHAR(63)   NOT NULL,
    `meta`                        VARCHAR(1000) NULL DEFAULT '{}',
    `data_asset_type`             VARCHAR(50)   NULL DEFAULT NULL,
    `ge_cloud_id`                 VARCHAR(200)  NULL DEFAULT NULL,
    `run_validation`              TINYINT(1)    NULL DEFAULT '1',
    `validation_ingestion_policy` VARCHAR(25)   NULL DEFAULT NULL,
    `created_at`                  TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `feature_group_suite_fk` (`feature_group_id` ASC) VISIBLE,
    CONSTRAINT `feature_group_suite_fk`
        FOREIGN KEY (`feature_group_id`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_group
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_group`
(
    `id`                         INT           NOT NULL AUTO_INCREMENT,
    `name`                       VARCHAR(63)   NOT NULL,
    `feature_store_id`           INT           NOT NULL,
    `created`                    TIMESTAMP     NULL     DEFAULT CURRENT_TIMESTAMP,
    `creator`                    INT           NOT NULL,
    `version`                    INT           NOT NULL,
    `description`                VARCHAR(1000) NULL     DEFAULT NULL,
    `feature_group_type`         INT           NOT NULL DEFAULT '0',
    `on_demand_feature_group_id` INT           NULL     DEFAULT NULL,
    `cached_feature_group_id`    INT           NULL     DEFAULT NULL,
    `stream_feature_group_id`    INT           NULL     DEFAULT NULL,
    `event_time`                 VARCHAR(63)   NULL     DEFAULT NULL,
    `online_enabled`             TINYINT(1)    NULL     DEFAULT NULL,
    `topic_name`                 VARCHAR(255)  NULL     DEFAULT NULL,
    `deprecated`                 TINYINT(1)    NULL     DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `name_version` (`feature_store_id` ASC, `name` ASC, `version` ASC) VISIBLE,
    INDEX `feature_store_id` (`feature_store_id` ASC) VISIBLE,
    INDEX `creator` (`creator` ASC) VISIBLE,
    INDEX `on_demand_feature_group_fk` (`on_demand_feature_group_id` ASC) VISIBLE,
    INDEX `cached_feature_group_fk` (`cached_feature_group_id` ASC) VISIBLE,
    INDEX `stream_feature_group_fk` (`stream_feature_group_id` ASC) VISIBLE,
    CONSTRAINT `cached_feature_group_fk`
        FOREIGN KEY (`cached_feature_group_id`)
            REFERENCES `hopsworks`.`cached_feature_group` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `FK_1012_790`
        FOREIGN KEY (`creator`)
            REFERENCES `hopsworks`.`users` (`uid`),
    CONSTRAINT `FK_656_740`
        FOREIGN KEY (`feature_store_id`)
            REFERENCES `hopsworks`.`feature_store` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `on_demand_feature_group_fk2`
        FOREIGN KEY (`on_demand_feature_group_id`)
            REFERENCES `hopsworks`.`on_demand_feature_group` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `stream_feature_group_fk`
        FOREIGN KEY (`stream_feature_group_id`)
            REFERENCES `hopsworks`.`stream_feature_group` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 13
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_group_alert
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_group_alert`
(
    `id`               INT                                NOT NULL AUTO_INCREMENT,
    `feature_group_id` INT                                NOT NULL,
    `status`           VARCHAR(45) CHARACTER SET 'latin1' NOT NULL,
    `type`             VARCHAR(45) CHARACTER SET 'latin1' NOT NULL,
    `severity`         VARCHAR(45) CHARACTER SET 'latin1' NOT NULL,
    `receiver`         INT                                NOT NULL,
    `created`          TIMESTAMP                          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `unique_feature_group_alert` (`feature_group_id` ASC, `status` ASC) VISIBLE,
    INDEX `fk_feature_group_alert_2_idx` (`feature_group_id` ASC) VISIBLE,
    INDEX `fk_feature_group_alert_1_idx` (`receiver` ASC) VISIBLE,
    CONSTRAINT `fk_feature_group_alert_1`
        FOREIGN KEY (`receiver`)
            REFERENCES `hopsworks`.`alert_receiver` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fk_feature_group_alert_2`
        FOREIGN KEY (`feature_group_id`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_group_commit
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_group_commit`
(
    `feature_group_id`  INT          NOT NULL,
    `commit_id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `committed_on`      TIMESTAMP(6) NOT NULL,
    `num_rows_updated`  INT          NULL     DEFAULT '0',
    `num_rows_inserted` INT          NULL     DEFAULT '0',
    `num_rows_deleted`  INT          NULL     DEFAULT '0',
    `archived`          TINYINT(1)   NOT NULL DEFAULT '0',
    PRIMARY KEY (`commit_id`, `feature_group_id`),
    INDEX `commit_id_idx` (`commit_id` ASC) VISIBLE,
    INDEX `commit_date_idx` (`committed_on` ASC) VISIBLE,
    CONSTRAINT `feature_group_fk`
        FOREIGN KEY (`feature_group_id`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_group_link
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_group_link`
(
    `id`                           INT          NOT NULL AUTO_INCREMENT,
    `feature_group_id`             INT          NOT NULL,
    `parent_feature_group_id`      INT          NULL DEFAULT NULL,
    `parent_feature_store`         VARCHAR(100) NOT NULL,
    `parent_feature_group_name`    VARCHAR(63)  NOT NULL,
    `parent_feature_group_version` INT          NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `link_unique` (`feature_group_id` ASC, `parent_feature_group_id` ASC) VISIBLE,
    INDEX `feature_group_id_fkc` (`feature_group_id` ASC) VISIBLE,
    INDEX `parent_feature_group_id_fkc` (`parent_feature_group_id` ASC) VISIBLE,
    CONSTRAINT `feature_group_id_fkc`
        FOREIGN KEY (`feature_group_id`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `feature_group_parent_fkc`
        FOREIGN KEY (`parent_feature_group_id`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE SET NULL
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store`
(
    `id`         INT                                 NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(100) CHARACTER SET 'latin1' NOT NULL,
    `project_id` INT                                 NOT NULL,
    `created`    TIMESTAMP                           NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `project_id` (`project_id` ASC) VISIBLE,
    CONSTRAINT `FK_883_662`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 67
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store_activity
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store_activity`
(
    `id`                   INT          NOT NULL AUTO_INCREMENT,
    `event_time`           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `uid`                  INT          NOT NULL,
    `type`                 INT          NOT NULL,
    `meta_type`            INT          NULL     DEFAULT NULL,
    `meta_msg`             VARCHAR(255) NULL     DEFAULT NULL,
    `execution_id`         INT          NULL     DEFAULT NULL,
    `statistics_id`        INT          NULL     DEFAULT NULL,
    `commit_id`            BIGINT       NULL     DEFAULT NULL,
    `feature_group_id`     INT          NULL     DEFAULT NULL,
    `training_dataset_id`  INT          NULL     DEFAULT NULL,
    `feature_view_id`      INT          NULL     DEFAULT NULL,
    `expectation_suite_id` INT          NULL     DEFAULT NULL,
    `validation_report_id` INT          NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `fsa_feature_view_fk` (`feature_view_id` ASC) VISIBLE,
    INDEX `fs_act_td_fk` (`training_dataset_id` ASC) VISIBLE,
    INDEX `fs_act_uid_fk` (`uid` ASC) VISIBLE,
    INDEX `fs_act_exec_fk` (`execution_id` ASC) VISIBLE,
    INDEX `fs_act_stat_fk` (`statistics_id` ASC) VISIBLE,
    INDEX `fs_act_commit_fk` (`feature_group_id` ASC, `commit_id` ASC) VISIBLE,
    INDEX `fs_act_validationreport_fk` (`validation_report_id` ASC) VISIBLE,
    INDEX `fs_act_expectationsuite_fk` (`expectation_suite_id` ASC) VISIBLE,
    CONSTRAINT `fs_act_commit_fk`
        FOREIGN KEY (`feature_group_id`, `commit_id`)
            REFERENCES `hopsworks`.`feature_group_commit` (`feature_group_id`, `commit_id`)
            ON DELETE CASCADE,
    CONSTRAINT `fs_act_exec_fk`
        FOREIGN KEY (`execution_id`)
            REFERENCES `hopsworks`.`executions` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fs_act_expectationsuite_fk`
        FOREIGN KEY (`expectation_suite_id`)
            REFERENCES `hopsworks`.`expectation_suite` (`id`)
            ON DELETE SET NULL,
    CONSTRAINT `fs_act_fg_fk`
        FOREIGN KEY (`feature_group_id`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fs_act_stat_fk`
        FOREIGN KEY (`statistics_id`)
            REFERENCES `hopsworks`.`feature_store_statistic` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fs_act_td_fk`
        FOREIGN KEY (`training_dataset_id`)
            REFERENCES `hopsworks`.`training_dataset` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fs_act_uid_fk`
        FOREIGN KEY (`uid`)
            REFERENCES `hopsworks`.`users` (`uid`)
            ON DELETE CASCADE,
    CONSTRAINT `fs_act_validationreport_fk`
        FOREIGN KEY (`validation_report_id`)
            REFERENCES `hopsworks`.`validation_report` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fsa_feature_view_fk`
        FOREIGN KEY (`feature_view_id`)
            REFERENCES `hopsworks`.`feature_view` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store_adls_connector
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store_adls_connector`
(
    `id`               INT          NOT NULL AUTO_INCREMENT,
    `generation`       INT          NOT NULL,
    `directory_id`     VARCHAR(40)  NOT NULL,
    `application_id`   VARCHAR(40)  NOT NULL,
    `account_name`     VARCHAR(30)  NOT NULL,
    `container_name`   VARCHAR(65)  NULL DEFAULT NULL,
    `cred_secret_uid`  INT          NOT NULL,
    `cred_secret_name` VARCHAR(200) NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `adls_cred_secret_fk` (`cred_secret_uid` ASC, `cred_secret_name` ASC) VISIBLE,
    CONSTRAINT `adls_cred_secret_fk`
        FOREIGN KEY (`cred_secret_uid`, `cred_secret_name`)
            REFERENCES `hopsworks`.`secrets` (`uid`, `secret_name`)
            ON DELETE RESTRICT
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store_bigquery_connector
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store_bigquery_connector`
(
    `id`                      INT                                 NOT NULL AUTO_INCREMENT,
    `parent_project`          VARCHAR(1000)                       NOT NULL,
    `dataset`                 VARCHAR(1000)                       NULL DEFAULT NULL,
    `query_table`             VARCHAR(1000)                       NULL DEFAULT NULL,
    `query_project`           VARCHAR(1000)                       NULL DEFAULT NULL,
    `materialization_dataset` VARCHAR(1000)                       NULL DEFAULT NULL,
    `arguments`               VARCHAR(2000)                       NULL DEFAULT NULL,
    `key_path`                VARCHAR(255) CHARACTER SET 'latin1' NULL DEFAULT NULL,
    PRIMARY KEY (`id`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store_code
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store_code`
(
    `id`                      INT                                 NOT NULL AUTO_INCREMENT,
    `commit_time`             DATETIME(3)                         NOT NULL,
    `name`                    VARCHAR(255) CHARACTER SET 'latin1' NOT NULL,
    `feature_group_id`        INT                                 NULL DEFAULT NULL,
    `feature_group_commit_id` BIGINT                              NULL DEFAULT NULL,
    `training_dataset_id`     INT                                 NULL DEFAULT NULL,
    `application_id`          VARCHAR(50)                         NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `feature_group_id` (`feature_group_id` ASC) VISIBLE,
    INDEX `training_dataset_id` (`training_dataset_id` ASC) VISIBLE,
    INDEX `feature_group_commit_id_fk` (`feature_group_id` ASC, `feature_group_commit_id` ASC) VISIBLE,
    CONSTRAINT `fg_ci_fk_fsc`
        FOREIGN KEY (`feature_group_id`, `feature_group_commit_id`)
            REFERENCES `hopsworks`.`feature_group_commit` (`feature_group_id`, `commit_id`)
            ON DELETE SET NULL,
    CONSTRAINT `fg_fk_fsc`
        FOREIGN KEY (`feature_group_id`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `td_fk_fsc`
        FOREIGN KEY (`training_dataset_id`)
            REFERENCES `hopsworks`.`training_dataset` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store_connector
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store_connector`
(
    `id`               INT           NOT NULL AUTO_INCREMENT,
    `feature_store_id` INT           NOT NULL,
    `name`             VARCHAR(150)  NOT NULL,
    `description`      VARCHAR(1000) NULL DEFAULT NULL,
    `type`             INT           NOT NULL,
    `jdbc_id`          INT           NULL DEFAULT NULL,
    `s3_id`            INT           NULL DEFAULT NULL,
    `hopsfs_id`        INT           NULL DEFAULT NULL,
    `redshift_id`      INT           NULL DEFAULT NULL,
    `adls_id`          INT           NULL DEFAULT NULL,
    `snowflake_id`     INT           NULL DEFAULT NULL,
    `kafka_id`         INT           NULL DEFAULT NULL,
    `gcs_id`           INT           NULL DEFAULT NULL,
    `bigquery_id`      INT           NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `fs_conn_name` (`name` ASC, `feature_store_id` ASC) VISIBLE,
    INDEX `fs_connector_featurestore_fk` (`feature_store_id` ASC) VISIBLE,
    INDEX `fs_connector_jdbc_fk` (`jdbc_id` ASC) VISIBLE,
    INDEX `fs_connector_s3_fk` (`s3_id` ASC) VISIBLE,
    INDEX `fs_connector_hopsfs_fk` (`hopsfs_id` ASC) VISIBLE,
    INDEX `fs_connector_redshift_fk` (`redshift_id` ASC) VISIBLE,
    INDEX `fs_connector_adls_fk` (`adls_id` ASC) VISIBLE,
    INDEX `fs_connector_snowflake_fk` (`snowflake_id` ASC) VISIBLE,
    INDEX `fs_connector_kafka_fk` (`kafka_id` ASC) VISIBLE,
    INDEX `fs_connector_gcs_fk` (`gcs_id` ASC) VISIBLE,
    INDEX `fs_connector_bigquery_fk` (`bigquery_id` ASC) VISIBLE,
    CONSTRAINT `fs_connector_adls_fk`
        FOREIGN KEY (`adls_id`)
            REFERENCES `hopsworks`.`feature_store_adls_connector` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fs_connector_bigquery_fk`
        FOREIGN KEY (`bigquery_id`)
            REFERENCES `hopsworks`.`feature_store_bigquery_connector` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fs_connector_featurestore_fk`
        FOREIGN KEY (`feature_store_id`)
            REFERENCES `hopsworks`.`feature_store` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fs_connector_gcs_fk`
        FOREIGN KEY (`gcs_id`)
            REFERENCES `hopsworks`.`feature_store_gcs_connector` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fs_connector_hopsfs_fk`
        FOREIGN KEY (`hopsfs_id`)
            REFERENCES `hopsworks`.`feature_store_hopsfs_connector` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fs_connector_jdbc_fk`
        FOREIGN KEY (`jdbc_id`)
            REFERENCES `hopsworks`.`feature_store_jdbc_connector` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fs_connector_kafka_fk`
        FOREIGN KEY (`kafka_id`)
            REFERENCES `hopsworks`.`feature_store_kafka_connector` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fs_connector_redshift_fk`
        FOREIGN KEY (`redshift_id`)
            REFERENCES `hopsworks`.`feature_store_redshift_connector` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fs_connector_s3_fk`
        FOREIGN KEY (`s3_id`)
            REFERENCES `hopsworks`.`feature_store_s3_connector` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fs_connector_snowflake_fk`
        FOREIGN KEY (`snowflake_id`)
            REFERENCES `hopsworks`.`feature_store_snowflake_connector` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store_gcs_connector
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store_gcs_connector`
(
    `id`                     INT                                 NOT NULL AUTO_INCREMENT,
    `algorithm`              VARCHAR(10)                         NULL DEFAULT NULL,
    `bucket`                 VARCHAR(1000)                       NOT NULL,
    `encryption_secret_uid`  INT                                 NULL DEFAULT NULL,
    `encryption_secret_name` VARCHAR(200)                        NULL DEFAULT NULL,
    `key_path`               VARCHAR(255) CHARACTER SET 'latin1' NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_fs_storage_connector_gcs_idx` (`encryption_secret_uid` ASC, `encryption_secret_name` ASC) VISIBLE,
    CONSTRAINT `fk_fs_storage_connector_gcs`
        FOREIGN KEY (`encryption_secret_uid`, `encryption_secret_name`)
            REFERENCES `hopsworks`.`secrets` (`uid`, `secret_name`)
            ON DELETE RESTRICT
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store_hopsfs_connector
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store_hopsfs_connector`
(
    `id`             INT NOT NULL AUTO_INCREMENT,
    `hopsfs_dataset` INT NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `hopsfs_connector_dataset_fk` (`hopsfs_dataset` ASC) VISIBLE,
    CONSTRAINT `hopsfs_connector_dataset_fk`
        FOREIGN KEY (`hopsfs_dataset`)
            REFERENCES `hopsworks`.`dataset` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store_jdbc_connector
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store_jdbc_connector`
(
    `id`                INT           NOT NULL AUTO_INCREMENT,
    `connection_string` VARCHAR(5000) NOT NULL,
    `arguments`         VARCHAR(2000) NULL DEFAULT NULL,
    PRIMARY KEY (`id`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store_job
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store_job`
(
    `id`                  INT NOT NULL AUTO_INCREMENT,
    `job_id`              INT NOT NULL,
    `training_dataset_id` INT NULL DEFAULT NULL,
    `feature_group_id`    INT NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `fs_job_job_fk` (`job_id` ASC) VISIBLE,
    INDEX `fs_job_td_fk` (`training_dataset_id` ASC) VISIBLE,
    INDEX `fs_job_fg_fk` (`feature_group_id` ASC) VISIBLE,
    CONSTRAINT `fs_job_fg_fk`
        FOREIGN KEY (`feature_group_id`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fs_job_job_fk`
        FOREIGN KEY (`job_id`)
            REFERENCES `hopsworks`.`jobs` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fs_job_td_fk`
        FOREIGN KEY (`training_dataset_id`)
            REFERENCES `hopsworks`.`training_dataset` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store_kafka_connector
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store_kafka_connector`
(
    `id`                                    INT                                 NOT NULL AUTO_INCREMENT,
    `bootstrap_servers`                     VARCHAR(1000)                       NOT NULL,
    `security_protocol`                     VARCHAR(1000)                       NOT NULL,
    `ssl_secret_uid`                        INT                                 NULL DEFAULT NULL,
    `ssl_secret_name`                       VARCHAR(200)                        NULL DEFAULT NULL,
    `ssl_endpoint_identification_algorithm` VARCHAR(100)                        NULL DEFAULT NULL,
    `options`                               VARCHAR(2000)                       NULL DEFAULT NULL,
    `truststore_path`                       VARCHAR(255) CHARACTER SET 'latin1' NULL DEFAULT NULL,
    `keystore_path`                         VARCHAR(255) CHARACTER SET 'latin1' NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_fs_storage_connector_kafka_idx` (`ssl_secret_uid` ASC, `ssl_secret_name` ASC) VISIBLE,
    CONSTRAINT `fk_fs_storage_connector_kafka`
        FOREIGN KEY (`ssl_secret_uid`, `ssl_secret_name`)
            REFERENCES `hopsworks`.`secrets` (`uid`, `secret_name`)
            ON DELETE RESTRICT
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store_keyword
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store_keyword`
(
    `id`                  INT          NOT NULL AUTO_INCREMENT,
    `name`                VARCHAR(255) NOT NULL,
    `feature_group_id`    INT          NULL DEFAULT NULL,
    `feature_view_id`     INT          NULL DEFAULT NULL,
    `training_dataset_id` INT          NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_fs_keyword_fg` (`feature_group_id` ASC) VISIBLE,
    INDEX `fk_fs_keyword_fv` (`feature_view_id` ASC) VISIBLE,
    INDEX `fk_fs_keyword_td` (`training_dataset_id` ASC) VISIBLE,
    CONSTRAINT `fk_fs_keyword_fg`
        FOREIGN KEY (`feature_group_id`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fk_fs_keyword_fv`
        FOREIGN KEY (`feature_view_id`)
            REFERENCES `hopsworks`.`feature_view` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fk_fs_keyword_td`
        FOREIGN KEY (`training_dataset_id`)
            REFERENCES `hopsworks`.`training_dataset` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store_redshift_connector
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store_redshift_connector`
(
    `id`                       INT           NOT NULL AUTO_INCREMENT,
    `cluster_identifier`       VARCHAR(64)   NOT NULL,
    `database_driver`          VARCHAR(64)   NOT NULL,
    `database_endpoint`        VARCHAR(128)  NULL DEFAULT NULL,
    `database_name`            VARCHAR(64)   NULL DEFAULT NULL,
    `database_port`            INT           NULL DEFAULT NULL,
    `table_name`               VARCHAR(128)  NULL DEFAULT NULL,
    `database_user_name`       VARCHAR(128)  NULL DEFAULT NULL,
    `auto_create`              TINYINT(1)    NULL DEFAULT '0',
    `database_group`           VARCHAR(2048) NULL DEFAULT NULL,
    `iam_role`                 VARCHAR(2048) NULL DEFAULT NULL,
    `arguments`                VARCHAR(2000) NULL DEFAULT NULL,
    `database_pwd_secret_uid`  INT           NULL DEFAULT NULL,
    `database_pwd_secret_name` VARCHAR(200)  NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_feature_store_redshift_connector_2_idx` (`database_pwd_secret_uid` ASC, `database_pwd_secret_name` ASC) VISIBLE,
    CONSTRAINT `fk_feature_store_redshift_connector_2`
        FOREIGN KEY (`database_pwd_secret_uid`, `database_pwd_secret_name`)
            REFERENCES `hopsworks`.`secrets` (`uid`, `secret_name`)
            ON DELETE RESTRICT
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store_s3_connector
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store_s3_connector`
(
    `id`                          INT           NOT NULL AUTO_INCREMENT,
    `bucket`                      VARCHAR(5000) NOT NULL,
    `iam_role`                    VARCHAR(2048) NULL DEFAULT NULL,
    `server_encryption_algorithm` INT           NULL DEFAULT NULL,
    `server_encryption_key`       VARCHAR(1000) NULL DEFAULT NULL,
    `key_secret_uid`              INT           NULL DEFAULT NULL,
    `key_secret_name`             VARCHAR(200)  NULL DEFAULT NULL,
    `arguments`                   VARCHAR(2000) NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_feature_store_s3_connector_1_idx` (`key_secret_uid` ASC, `key_secret_name` ASC) VISIBLE,
    CONSTRAINT `fk_feature_store_s3_connector_1`
        FOREIGN KEY (`key_secret_uid`, `key_secret_name`)
            REFERENCES `hopsworks`.`secrets` (`uid`, `secret_name`)
            ON DELETE RESTRICT
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store_snowflake_connector
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store_snowflake_connector`
(
    `id`                       INT           NOT NULL AUTO_INCREMENT,
    `url`                      VARCHAR(3000) NOT NULL,
    `database_user`            VARCHAR(128)  NOT NULL,
    `database_name`            VARCHAR(64)   NOT NULL,
    `database_schema`          VARCHAR(45)   NOT NULL,
    `table_name`               VARCHAR(128)  NULL DEFAULT NULL,
    `role`                     VARCHAR(65)   NULL DEFAULT NULL,
    `warehouse`                VARCHAR(128)  NULL DEFAULT NULL,
    `arguments`                VARCHAR(8000) NULL DEFAULT NULL,
    `application`              VARCHAR(50)   NULL DEFAULT NULL,
    `database_pwd_secret_uid`  INT           NULL DEFAULT NULL,
    `database_pwd_secret_name` VARCHAR(200)  NULL DEFAULT NULL,
    `oauth_token_secret_uid`   INT           NULL DEFAULT NULL,
    `oauth_token_secret_name`  VARCHAR(200)  NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_feature_store_snowflake_connector_2_idx` (`database_pwd_secret_uid` ASC, `database_pwd_secret_name` ASC) VISIBLE,
    INDEX `fk_feature_store_snowflake_connector_3_idx` (`oauth_token_secret_uid` ASC, `oauth_token_secret_name` ASC) VISIBLE,
    CONSTRAINT `fk_feature_store_snowflake_connector_2`
        FOREIGN KEY (`database_pwd_secret_uid`, `database_pwd_secret_name`)
            REFERENCES `hopsworks`.`secrets` (`uid`, `secret_name`)
            ON DELETE RESTRICT,
    CONSTRAINT `fk_feature_store_snowflake_connector_3`
        FOREIGN KEY (`oauth_token_secret_uid`, `oauth_token_secret_name`)
            REFERENCES `hopsworks`.`secrets` (`uid`, `secret_name`)
            ON DELETE RESTRICT
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store_statistic
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store_statistic`
(
    `id`                      INT           NOT NULL AUTO_INCREMENT,
    `commit_time`             DATETIME(3)   NOT NULL,
    `file_path`               VARCHAR(1000) NOT NULL,
    `feature_group_id`        INT           NULL DEFAULT NULL,
    `feature_group_commit_id` BIGINT        NULL DEFAULT NULL,
    `training_dataset_id`     INT           NULL DEFAULT NULL,
    `for_transformation`      TINYINT(1)    NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    INDEX `feature_group_id` (`feature_group_id` ASC) VISIBLE,
    INDEX `training_dataset_id` (`training_dataset_id` ASC) VISIBLE,
    INDEX `feature_group_commit_id_fk` (`feature_group_id` ASC, `feature_group_commit_id` ASC) VISIBLE,
    CONSTRAINT `fg_fk_fss`
        FOREIGN KEY (`feature_group_id`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `td_fk_fss`
        FOREIGN KEY (`training_dataset_id`)
            REFERENCES `hopsworks`.`training_dataset` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store_tag
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store_tag`
(
    `id`         INT            NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(255)   NOT NULL,
    `tag_schema` VARCHAR(13000) NOT NULL DEFAULT '{"type":"string"}',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `name_UNIQUE` (`name` ASC) VISIBLE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_store_tag_value
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_store_tag_value`
(
    `id`                  INT            NOT NULL AUTO_INCREMENT,
    `schema_id`           INT            NOT NULL,
    `feature_group_id`    INT            NULL DEFAULT NULL,
    `feature_view_id`     INT            NULL DEFAULT NULL,
    `training_dataset_id` INT            NULL DEFAULT NULL,
    `value`               VARCHAR(29000) NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_fs_tag_value_schema` (`schema_id` ASC) VISIBLE,
    INDEX `fk_fs_tag_value_fg` (`feature_group_id` ASC) VISIBLE,
    INDEX `fk_fs_tag_value_fv` (`feature_view_id` ASC) VISIBLE,
    INDEX `fk_fs_tag_value_td` (`training_dataset_id` ASC) VISIBLE,
    CONSTRAINT `fk_fs_tag_value_fg`
        FOREIGN KEY (`feature_group_id`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fk_fs_tag_value_fv`
        FOREIGN KEY (`feature_view_id`)
            REFERENCES `hopsworks`.`feature_view` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fk_fs_tag_value_schema`
        FOREIGN KEY (`schema_id`)
            REFERENCES `hopsworks`.`feature_store_tag` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fk_fs_tag_value_td`
        FOREIGN KEY (`training_dataset_id`)
            REFERENCES `hopsworks`.`training_dataset` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_view
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_view`
(
    `id`               INT                                   NOT NULL AUTO_INCREMENT,
    `name`             VARCHAR(63)                           NOT NULL,
    `feature_store_id` INT                                   NOT NULL,
    `created`          TIMESTAMP                             NULL DEFAULT CURRENT_TIMESTAMP,
    `creator`          INT                                   NOT NULL,
    `version`          INT                                   NOT NULL,
    `description`      VARCHAR(10000) CHARACTER SET 'latin1' NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `name_version` (`feature_store_id` ASC, `name` ASC, `version` ASC) VISIBLE,
    INDEX `feature_store_id` (`feature_store_id` ASC) VISIBLE,
    INDEX `creator` (`creator` ASC) VISIBLE,
    CONSTRAINT `fv_creator_fk`
        FOREIGN KEY (`creator`)
            REFERENCES `hopsworks`.`users` (`uid`),
    CONSTRAINT `fv_feature_store_id_fk`
        FOREIGN KEY (`feature_store_id`)
            REFERENCES `hopsworks`.`feature_store` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 9
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.feature_view_link
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`feature_view_link`
(
    `id`                           INT          NOT NULL AUTO_INCREMENT,
    `feature_view_id`              INT          NOT NULL,
    `parent_feature_group_id`      INT          NULL DEFAULT NULL,
    `parent_feature_store`         VARCHAR(100) NOT NULL,
    `parent_feature_group_name`    VARCHAR(63)  NOT NULL,
    `parent_feature_group_version` INT          NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `link_unique` (`feature_view_id` ASC, `parent_feature_group_id` ASC) VISIBLE,
    INDEX `feature_view_id_fkc` (`feature_view_id` ASC) VISIBLE,
    INDEX `feature_view_parent_id_fkc` (`parent_feature_group_id` ASC) VISIBLE,
    CONSTRAINT `feature_view_id_fkc`
        FOREIGN KEY (`feature_view_id`)
            REFERENCES `hopsworks`.`feature_view` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `feature_view_parent_fkc`
        FOREIGN KEY (`parent_feature_group_id`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE SET NULL
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.files_to_remove
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`files_to_remove`
(
    `execution_id` INT                                 NOT NULL,
    `filepath`     VARCHAR(255) CHARACTER SET 'latin1' NOT NULL,
    PRIMARY KEY (`execution_id`, `filepath`),
    CONSTRAINT `FK_361_376`
        FOREIGN KEY (`execution_id`)
            REFERENCES `hopsworks`.`executions` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.git_commits
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`git_commits`
(
    `id`              INT                                  NOT NULL AUTO_INCREMENT,
    `repository`      INT                                  NOT NULL,
    `branch`          VARCHAR(250) CHARACTER SET 'latin1'  NULL DEFAULT NULL,
    `hash`            VARCHAR(255) CHARACTER SET 'latin1'  NOT NULL,
    `message`         VARCHAR(1000) CHARACTER SET 'latin1' NOT NULL,
    `committer_name`  VARCHAR(1000) CHARACTER SET 'latin1' NOT NULL,
    `committer_email` VARCHAR(1000) CHARACTER SET 'latin1' NOT NULL,
    `date`            TIMESTAMP                            NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `repository_fk` (`repository` ASC) VISIBLE,
    CONSTRAINT `repository_fk`
        FOREIGN KEY (`repository`)
            REFERENCES `hopsworks`.`git_repositories` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 4119
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.git_executions
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`git_executions`
(
    `id`                   INT                                   NOT NULL AUTO_INCREMENT,
    `submission_time`      TIMESTAMP                             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `user`                 INT                                   NOT NULL,
    `repository`           INT                                   NOT NULL,
    `execution_start`      BIGINT                                NULL     DEFAULT NULL,
    `execution_stop`       BIGINT                                NULL     DEFAULT NULL,
    `command_config`       VARCHAR(11000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `state`                VARCHAR(128) CHARACTER SET 'latin1'   NOT NULL,
    `final_result_message` VARCHAR(11000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `config_secret`        VARCHAR(255) CHARACTER SET 'latin1'   NOT NULL,
    `hostname`             VARCHAR(128)                          NOT NULL DEFAULT 'localhost',
    PRIMARY KEY (`id`),
    INDEX `user` (`user` ASC) VISIBLE,
    INDEX `git_exec_repo_fkc` (`repository` ASC) VISIBLE,
    CONSTRAINT `git_exec_repo_fkc`
        FOREIGN KEY (`repository`)
            REFERENCES `hopsworks`.`git_repositories` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `git_exec_usr_fkc`
        FOREIGN KEY (`user`)
            REFERENCES `hopsworks`.`users` (`uid`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 8251
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.git_repositories
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`git_repositories`
(
    `id`             INT                                  NOT NULL AUTO_INCREMENT,
    `path`           VARCHAR(1000) CHARACTER SET 'latin1' NOT NULL,
    `name`           VARCHAR(255) CHARACTER SET 'latin1'  NOT NULL,
    `project`        INT                                  NOT NULL,
    `provider`       VARCHAR(20) CHARACTER SET 'latin1'   NOT NULL,
    `current_branch` VARCHAR(250) CHARACTER SET 'latin1'  NULL DEFAULT NULL,
    `current_commit` VARCHAR(255) CHARACTER SET 'latin1'  NULL DEFAULT NULL,
    `cid`            VARCHAR(255) CHARACTER SET 'latin1'  NULL DEFAULT NULL,
    `creator`        INT                                  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `repository_path_constraint_unique` (`path`(255) ASC) VISIBLE,
    INDEX `project_fk` (`project` ASC) VISIBLE,
    CONSTRAINT `project_fk`
        FOREIGN KEY (`project`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 2061
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.git_repository_remotes
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`git_repository_remotes`
(
    `id`          INT                                  NOT NULL AUTO_INCREMENT,
    `repository`  INT                                  NOT NULL,
    `remote_name` VARCHAR(255) CHARACTER SET 'latin1'  NOT NULL,
    `remote_url`  VARCHAR(1000) CHARACTER SET 'latin1' NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `git_repository_fk` (`repository` ASC) VISIBLE,
    CONSTRAINT `git_repository_fk`
        FOREIGN KEY (`repository`)
            REFERENCES `hopsworks`.`git_repositories` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 6164
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.great_expectation
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`great_expectation`
(
    `id`               INT           NOT NULL AUTO_INCREMENT,
    `kwargs_template`  VARCHAR(1000) NOT NULL,
    `expectation_type` VARCHAR(150)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `unique_great_expectation` (`expectation_type` ASC) VISIBLE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.hdfs_command_execution
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`hdfs_command_execution`
(
    `id`           INT           NOT NULL AUTO_INCREMENT,
    `execution_id` INT           NOT NULL,
    `command`      VARCHAR(45)   NOT NULL,
    `submitted`    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `src_path`     VARCHAR(1000) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uq_execution_id` (`execution_id` ASC) VISIBLE,
    UNIQUE INDEX `uq_src_path` (`src_path`(255) ASC) VISIBLE,
    INDEX `fk_hdfs_file_command_1_idx` (`execution_id` ASC) VISIBLE,
    CONSTRAINT `fk_hdfs_file_command_1`
        FOREIGN KEY (`execution_id`)
            REFERENCES `hopsworks`.`executions` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.host_services
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`host_services`
(
    `id`         BIGINT                             NOT NULL AUTO_INCREMENT,
    `host_id`    INT                                NOT NULL,
    `pid`        INT                                NULL DEFAULT NULL,
    `name`       VARCHAR(48) CHARACTER SET 'latin1' NOT NULL,
    `group_name` VARCHAR(48) CHARACTER SET 'latin1' NOT NULL,
    `status`     INT                                NOT NULL,
    `uptime`     BIGINT                             NULL DEFAULT NULL,
    `startTime`  BIGINT                             NULL DEFAULT NULL,
    `stopTime`   BIGINT                             NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `service_UNIQUE` (`host_id` ASC, `name` ASC) VISIBLE,
    INDEX `host_id` (`host_id` ASC) VISIBLE,
    CONSTRAINT `FK_481_491`
        FOREIGN KEY (`host_id`)
            REFERENCES `hopsworks`.`hosts` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 42
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.hosts
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`hosts`
(
    `id`              INT                                 NOT NULL AUTO_INCREMENT,
    `hostname`        VARCHAR(128) CHARACTER SET 'latin1' NOT NULL,
    `cores`           INT                                 NULL DEFAULT NULL,
    `host_ip`         VARCHAR(128) CHARACTER SET 'latin1' NOT NULL,
    `last_heartbeat`  BIGINT                              NULL DEFAULT NULL,
    `memory_capacity` BIGINT                              NULL DEFAULT NULL,
    `private_ip`      VARCHAR(15) CHARACTER SET 'latin1'  NULL DEFAULT NULL,
    `public_ip`       VARCHAR(15) CHARACTER SET 'latin1'  NULL DEFAULT NULL,
    `agent_password`  VARCHAR(25) CHARACTER SET 'latin1'  NULL DEFAULT NULL,
    `num_gpus`        TINYINT(1)                          NULL DEFAULT '0',
    `registered`      TINYINT(1)                          NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `hostname` (`hostname` ASC) VISIBLE,
    UNIQUE INDEX `host_ip` (`host_ip` ASC) VISIBLE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 6
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.invalid_jwt
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`invalid_jwt`
(
    `jti`               VARCHAR(45) CHARACTER SET 'latin1' NOT NULL,
    `expiration_time`   DATETIME                           NOT NULL,
    `renewable_for_sec` INT                                NOT NULL,
    PRIMARY KEY (`jti`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.job_alert
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`job_alert`
(
    `id`       INT         NOT NULL AUTO_INCREMENT,
    `job_id`   INT         NOT NULL,
    `status`   VARCHAR(45) NOT NULL,
    `type`     VARCHAR(45) NOT NULL,
    `severity` VARCHAR(45) NOT NULL,
    `receiver` INT         NOT NULL,
    `created`  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `unique_job_alert` (`job_id` ASC, `status` ASC) VISIBLE,
    INDEX `fk_job_alert_2_idx` (`job_id` ASC) VISIBLE,
    INDEX `fk_job_alert_1_idx` (`receiver` ASC) VISIBLE,
    CONSTRAINT `fk_job_alert_1`
        FOREIGN KEY (`receiver`)
            REFERENCES `hopsworks`.`alert_receiver` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fk_job_alert_2`
        FOREIGN KEY (`job_id`)
            REFERENCES `hopsworks`.`jobs` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.job_schedule
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`job_schedule`
(
    `id`                       INT          NOT NULL AUTO_INCREMENT,
    `job_id`                   INT          NOT NULL,
    `start_date_time`          TIMESTAMP    NOT NULL,
    `end_date_time`            TIMESTAMP    NULL DEFAULT NULL,
    `enabled`                  TINYINT(1)   NOT NULL,
    `cron_expression`          VARCHAR(500) NOT NULL,
    `next_execution_date_time` TIMESTAMP    NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `job_id` (`job_id` ASC) VISIBLE,
    CONSTRAINT `fk_schedule_job`
        FOREIGN KEY (`job_id`)
            REFERENCES `hopsworks`.`jobs` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.jobs
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`jobs`
(
    `id`            INT                                                             NOT NULL AUTO_INCREMENT,
    `name`          VARCHAR(128) CHARACTER SET 'latin1'                             NULL     DEFAULT NULL,
    `creation_time` TIMESTAMP                                                       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `project_id`    INT                                                             NOT NULL,
    `creator`       VARCHAR(150) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs' NOT NULL,
    `type`          VARCHAR(128) CHARACTER SET 'latin1'                             NOT NULL,
    `json_config`   VARCHAR(12500) CHARACTER SET 'latin1'                           NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `name_project_idx` (`name` ASC, `project_id` ASC) VISIBLE,
    INDEX `project_id` (`project_id` ASC) VISIBLE,
    INDEX `creator` (`creator` ASC) VISIBLE,
    INDEX `creator_project_idx` (`creator` ASC, `project_id` ASC) VISIBLE,
    INDEX `creation_time_project_idx` (`creation_time` ASC, `project_id` ASC) VISIBLE,
    INDEX `type_project_id_idx` (`type` ASC, `project_id` ASC) VISIBLE,
    CONSTRAINT `FK_262_353`
        FOREIGN KEY (`creator`)
            REFERENCES `hopsworks`.`users` (`email`),
    CONSTRAINT `FK_284_352`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 37
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.jupyter_project
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`jupyter_project`
(
    `port`       INT                                 NOT NULL,
    `uid`        INT                                 NOT NULL,
    `created`    TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expires`    TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `no_limit`   TINYINT(1)                          NULL     DEFAULT '0',
    `token`      VARCHAR(255) CHARACTER SET 'latin1' NOT NULL,
    `secret`     VARCHAR(64) CHARACTER SET 'latin1'  NOT NULL,
    `cid`        VARCHAR(255) CHARACTER SET 'latin1' NOT NULL,
    `project_id` INT                                 NOT NULL,
    PRIMARY KEY (`port`),
    UNIQUE INDEX `project_user` (`project_id` ASC, `uid` ASC) VISIBLE,
    INDEX `project_id` (`project_id` ASC) VISIBLE,
    INDEX `jp_uid_fk` (`uid` ASC) VISIBLE,
    CONSTRAINT `FK_284_526`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `jp_uid_fk`
        FOREIGN KEY (`uid`)
            REFERENCES `hopsworks`.`users` (`uid`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.jupyter_settings
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`jupyter_settings`
(
    `project_id`     INT                                                             NOT NULL,
    `team_member`    VARCHAR(150) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs' NOT NULL,
    `secret`         VARCHAR(255) CHARACTER SET 'latin1'                             NOT NULL,
    `advanced`       TINYINT(1)                                                      NULL     DEFAULT '0',
    `shutdown_level` INT                                                             NOT NULL DEFAULT '6',
    `no_limit`       TINYINT(1)                                                      NULL     DEFAULT '0',
    `base_dir`       VARCHAR(255) CHARACTER SET 'latin1'                             NULL     DEFAULT NULL,
    `job_config`     VARCHAR(11000) CHARACTER SET 'latin1'                           NULL     DEFAULT NULL,
    `docker_config`  VARCHAR(1000) CHARACTER SET 'latin1'                            NULL     DEFAULT NULL,
    `python_kernel`  TINYINT(1)                                                      NULL     DEFAULT '1',
    PRIMARY KEY (`project_id`, `team_member`),
    INDEX `team_member` (`team_member` ASC) VISIBLE,
    INDEX `secret_idx` (`secret` ASC) VISIBLE,
    CONSTRAINT `FK_262_309`
        FOREIGN KEY (`team_member`)
            REFERENCES `hopsworks`.`users` (`email`)
            ON DELETE CASCADE,
    CONSTRAINT `FK_284_308`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.jwt_signing_key
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`jwt_signing_key`
(
    `id`         INT                                 NOT NULL AUTO_INCREMENT,
    `secret`     VARCHAR(128) CHARACTER SET 'latin1' NOT NULL,
    `name`       VARCHAR(255) CHARACTER SET 'latin1' NOT NULL,
    `created_on` TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `jwt_signing_key_name_UNIQUE` (`name` ASC) VISIBLE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 33
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.maggy_driver
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`maggy_driver`
(
    `id`      MEDIUMINT                           NOT NULL AUTO_INCREMENT,
    `app_id`  CHAR(45) CHARACTER SET 'latin1'     NOT NULL,
    `host_ip` VARCHAR(128) CHARACTER SET 'latin1' NOT NULL,
    `port`    INT                                 NOT NULL,
    `secret`  VARCHAR(128) CHARACTER SET 'latin1' NOT NULL,
    `created` TIMESTAMP                           NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `app_id` (`app_id` ASC, `port` ASC) VISIBLE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 2
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.materialized_jwt
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`materialized_jwt`
(
    `project_id` INT     NOT NULL,
    `user_id`    INT     NOT NULL,
    `usage`      TINYINT NOT NULL,
    PRIMARY KEY (`project_id`, `user_id`, `usage`),
    INDEX `jwt_material_user` (`user_id` ASC) VISIBLE,
    CONSTRAINT `jwt_material_project`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `jwt_material_user`
        FOREIGN KEY (`user_id`)
            REFERENCES `hopsworks`.`users` (`uid`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.message
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`message`
(
    `id`           INT                                                             NOT NULL AUTO_INCREMENT,
    `user_from`    VARCHAR(150) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs' NULL DEFAULT NULL,
    `user_to`      VARCHAR(150) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs' NOT NULL,
    `date_sent`    DATETIME                                                        NOT NULL,
    `subject`      VARCHAR(128) CHARACTER SET 'latin1'                             NULL DEFAULT NULL,
    `preview`      VARCHAR(128) CHARACTER SET 'latin1'                             NULL DEFAULT NULL,
    `content`      VARCHAR(11000) CHARACTER SET 'latin1'                           NOT NULL,
    `unread`       TINYINT(1)                                                      NOT NULL,
    `deleted`      TINYINT(1)                                                      NOT NULL,
    `path`         VARCHAR(600) CHARACTER SET 'latin1'                             NULL DEFAULT NULL,
    `reply_to_msg` INT                                                             NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `user_from` (`user_from` ASC) VISIBLE,
    INDEX `user_to` (`user_to` ASC) VISIBLE,
    INDEX `reply_to_msg` (`reply_to_msg` ASC) VISIBLE,
    CONSTRAINT `FK_262_441`
        FOREIGN KEY (`user_from`)
            REFERENCES `hopsworks`.`users` (`email`)
            ON DELETE SET NULL,
    CONSTRAINT `FK_262_442`
        FOREIGN KEY (`user_to`)
            REFERENCES `hopsworks`.`users` (`email`)
            ON DELETE CASCADE,
    CONSTRAINT `FK_438_443`
        FOREIGN KEY (`reply_to_msg`)
            REFERENCES `hopsworks`.`message` (`id`)
            ON DELETE SET NULL
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 3
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.message_to_user
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`message_to_user`
(
    `message`    INT                                                             NOT NULL,
    `user_email` VARCHAR(150) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs' NOT NULL,
    PRIMARY KEY (`message`, `user_email`),
    INDEX `user_email` (`user_email` ASC) VISIBLE,
    CONSTRAINT `FK_262_458`
        FOREIGN KEY (`user_email`)
            REFERENCES `hopsworks`.`users` (`email`)
            ON DELETE CASCADE,
    CONSTRAINT `FK_438_457`
        FOREIGN KEY (`message`)
            REFERENCES `hopsworks`.`message` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.oauth_client
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`oauth_client`
(
    `id`                                   INT                                  NOT NULL AUTO_INCREMENT,
    `client_id`                            VARCHAR(256) CHARACTER SET 'latin1'  NOT NULL,
    `client_secret`                        VARCHAR(2048) CHARACTER SET 'latin1' NOT NULL,
    `provider_logo_uri`                    VARCHAR(2048) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `provider_uri`                         VARCHAR(2048) CHARACTER SET 'latin1' NOT NULL,
    `provider_name`                        VARCHAR(256) CHARACTER SET 'latin1'  NOT NULL,
    `provider_display_name`                VARCHAR(45) CHARACTER SET 'latin1'   NOT NULL,
    `authorisation_endpoint`               VARCHAR(1024) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `token_endpoint`                       VARCHAR(1024) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `userinfo_endpoint`                    VARCHAR(1024) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `end_session_endpoint`                 VARCHAR(1024) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `logout_redirect_param`                VARCHAR(45) CHARACTER SET 'latin1'   NULL     DEFAULT NULL,
    `jwks_uri`                             VARCHAR(1024) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `provider_metadata_endpoint_supported` TINYINT(1)                           NOT NULL DEFAULT '0',
    `offline_access`                       TINYINT(1)                           NOT NULL DEFAULT '0',
    `code_challenge`                       TINYINT(1)                           NOT NULL DEFAULT '0',
    `code_challenge_method`                VARCHAR(16) CHARACTER SET 'latin1'   NULL     DEFAULT NULL,
    `verify_email`                         TINYINT(1)                           NOT NULL DEFAULT '0',
    `given_name_claim`                     VARCHAR(256) CHARACTER SET 'latin1'  NOT NULL DEFAULT 'given_name',
    `family_name_claim`                    VARCHAR(256) CHARACTER SET 'latin1'  NOT NULL DEFAULT 'family_name',
    `email_claim`                          VARCHAR(256) CHARACTER SET 'latin1'  NOT NULL DEFAULT 'email',
    `group_claim`                          VARCHAR(256) CHARACTER SET 'latin1'  NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `client_id_UNIQUE` (`client_id`(255) ASC) VISIBLE,
    UNIQUE INDEX `provider_name_UNIQUE` (`provider_name`(255) ASC) VISIBLE,
    KEY `client_id_index` (`client_id`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.oauth_login_state
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`oauth_login_state`
(
    `id`             INT                                  NOT NULL AUTO_INCREMENT,
    `state`          VARCHAR(256) CHARACTER SET 'latin1'  NOT NULL,
    `session_id`     VARCHAR(128) CHARACTER SET 'latin1'  NOT NULL,
    `client_id`      VARCHAR(256) CHARACTER SET 'latin1'  NOT NULL,
    `login_time`     TIMESTAMP                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `id_token`       VARCHAR(8000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `access_token`   VARCHAR(8000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `refresh_token`  VARCHAR(8000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `nonce`          VARCHAR(128) CHARACTER SET 'latin1'  NOT NULL,
    `scopes`         VARCHAR(2048) CHARACTER SET 'latin1' NOT NULL,
    `code_challenge` VARCHAR(128) CHARACTER SET 'latin1'  NULL     DEFAULT NULL,
    `redirect_uri`   VARCHAR(1024) CHARACTER SET 'latin1' NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `login_state_UNIQUE` (`state`(255) ASC) VISIBLE,
    INDEX `oauth_login_state_client` (`client_id`(255) ASC) VISIBLE,
    CONSTRAINT `fk_oauth_login_state_client`
        FOREIGN KEY (`client_id`)
            REFERENCES `hopsworks`.`oauth_client` (`client_id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.oauth_token
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`oauth_token`
(
    `id`            INT                                  NOT NULL AUTO_INCREMENT,
    `user_id`       INT                                  NOT NULL,
    `id_token`      VARCHAR(8000) CHARACTER SET 'latin1' NOT NULL,
    `access_token`  VARCHAR(8000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `refresh_token` VARCHAR(8000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `login_time`    TIMESTAMP                            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `login_state_UNIQUE` (`user_id` ASC) VISIBLE,
    INDEX `fk_oauth_token_user` (`user_id` ASC) VISIBLE,
    CONSTRAINT `fk_oauth_token_user`
        FOREIGN KEY (`user_id`)
            REFERENCES `hopsworks`.`users` (`uid`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.on_demand_feature
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`on_demand_feature`
(
    `id`                         INT                                   NOT NULL AUTO_INCREMENT,
    `on_demand_feature_group_id` INT                                   NULL     DEFAULT NULL,
    `name`                       VARCHAR(1000) CHARACTER SET 'latin1'  NOT NULL,
    `primary_column`             TINYINT(1)                            NOT NULL DEFAULT '0',
    `description`                VARCHAR(10000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `type`                       VARCHAR(1000) CHARACTER SET 'latin1'  NOT NULL,
    `idx`                        INT                                   NOT NULL DEFAULT '0',
    `default_value`              VARCHAR(400)                          NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `on_demand_feature_group_fk` (`on_demand_feature_group_id` ASC) VISIBLE,
    CONSTRAINT `on_demand_feature_group_fk1`
        FOREIGN KEY (`on_demand_feature_group_id`)
            REFERENCES `hopsworks`.`on_demand_feature_group` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.on_demand_feature_group
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`on_demand_feature_group`
(
    `id`           INT            NOT NULL AUTO_INCREMENT,
    `query`        VARCHAR(26000) NULL     DEFAULT NULL,
    `connector_id` INT            NULL     DEFAULT NULL,
    `data_format`  VARCHAR(10)    NULL     DEFAULT NULL,
    `path`         VARCHAR(1000)  NULL     DEFAULT NULL,
    `spine`        TINYINT(1)     NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    INDEX `on_demand_conn_fk` (`connector_id` ASC) VISIBLE,
    CONSTRAINT `on_demand_conn_fk`
        FOREIGN KEY (`connector_id`)
            REFERENCES `hopsworks`.`feature_store_connector` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.on_demand_option
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`on_demand_option`
(
    `id`                         INT                                 NOT NULL AUTO_INCREMENT,
    `on_demand_feature_group_id` INT                                 NULL DEFAULT NULL,
    `name`                       VARCHAR(255) CHARACTER SET 'latin1' NOT NULL,
    `value`                      VARCHAR(255) CHARACTER SET 'latin1' NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `on_demand_option_key` (`on_demand_feature_group_id` ASC) VISIBLE,
    CONSTRAINT `on_demand_option_fk`
        FOREIGN KEY (`on_demand_feature_group_id`)
            REFERENCES `hopsworks`.`on_demand_feature_group` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.ops_log
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`ops_log`
(
    `id`         INT        NOT NULL AUTO_INCREMENT,
    `op_id`      INT        NOT NULL,
    `op_on`      TINYINT(1) NOT NULL,
    `op_type`    TINYINT(1) NOT NULL,
    `project_id` INT        NOT NULL,
    `dataset_id` BIGINT     NOT NULL,
    `inode_id`   BIGINT     NOT NULL,
    PRIMARY KEY (`id`)
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 308
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.pki_certificate
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`pki_certificate`
(
    `ca`            TINYINT          NOT NULL,
    `serial_number` BIGINT           NOT NULL,
    `status`        TINYINT          NOT NULL,
    `subject`       VARCHAR(255)     NOT NULL,
    `certificate`   VARBINARY(10000) NULL DEFAULT NULL,
    `not_before`    DATETIME         NOT NULL,
    `not_after`     DATETIME         NOT NULL,
    PRIMARY KEY (`status`, `subject`),
    INDEX `sn_index` (`serial_number` ASC) VISIBLE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.pki_crl
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`pki_crl`
(
    `type` VARCHAR(20) NOT NULL,
    `crl`  MEDIUMBLOB  NOT NULL,
    PRIMARY KEY (`type`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.pki_key
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`pki_key`
(
    `owner` VARCHAR(100)    NOT NULL,
    `type`  TINYINT         NOT NULL,
    `key`   VARBINARY(8192) NOT NULL,
    PRIMARY KEY (`owner`, `type`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.pki_serial_number
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`pki_serial_number`
(
    `type`   VARCHAR(20) NOT NULL,
    `number` BIGINT      NOT NULL,
    PRIMARY KEY (`type`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.project
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`project`
(
    `id`                   INT                                                             NOT NULL AUTO_INCREMENT,
    `projectname`          VARCHAR(100) CHARACTER SET 'latin1'                             NOT NULL,
    `username`             VARCHAR(150) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs' NOT NULL,
    `created`              TIMESTAMP                                                       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `description`          VARCHAR(2000) CHARACTER SET 'latin1'                            NULL     DEFAULT NULL,
    `payment_type`         VARCHAR(255) CHARACTER SET 'latin1'                             NOT NULL DEFAULT 'PREPAID',
    `last_quota_update`    TIMESTAMP                                                       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `kafka_max_num_topics` INT                                                             NOT NULL DEFAULT '100',
    `docker_image`         VARCHAR(255) CHARACTER SET 'latin1'                             NULL     DEFAULT NULL,
    `topic_name`           VARCHAR(255)                                                    NULL     DEFAULT NULL,
    `python_env_id`        INT                                                             NULL     DEFAULT NULL,
    `creation_status`      TINYINT(1)                                                      NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `projectname` (`projectname` ASC) VISIBLE,
    INDEX `user_idx` (`username` ASC) VISIBLE,
    CONSTRAINT `FK_262_290`
        FOREIGN KEY (`username`)
            REFERENCES `hopsworks`.`users` (`email`)
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 119
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.project_pythondeps
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`project_pythondeps`
(
    `project_id` INT NOT NULL,
    `dep_id`     INT NOT NULL,
    PRIMARY KEY (`project_id`, `dep_id`),
    INDEX `dep_id` (`dep_id` ASC) VISIBLE,
    CONSTRAINT `FK_284_513`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `FK_505_514`
        FOREIGN KEY (`dep_id`)
            REFERENCES `hopsworks`.`python_dep` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.project_service_alert
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`project_service_alert`
(
    `id`         INT                                NOT NULL AUTO_INCREMENT,
    `project_id` INT                                NOT NULL,
    `service`    VARCHAR(32) CHARACTER SET 'latin1' NOT NULL,
    `status`     VARCHAR(45) CHARACTER SET 'latin1' NOT NULL,
    `type`       VARCHAR(45) CHARACTER SET 'latin1' NOT NULL,
    `severity`   VARCHAR(45) CHARACTER SET 'latin1' NOT NULL,
    `receiver`   INT                                NOT NULL,
    `created`    TIMESTAMP                          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `unique_project_service_alert` (`project_id` ASC, `status` ASC) VISIBLE,
    INDEX `fk_project_service_2_idx` (`project_id` ASC) VISIBLE,
    INDEX `fk_project_service_alert_1_idx` (`receiver` ASC) VISIBLE,
    CONSTRAINT `fk_project_service_alert_1`
        FOREIGN KEY (`receiver`)
            REFERENCES `hopsworks`.`alert_receiver` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `fk_project_service_alert_2`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.project_services
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`project_services`
(
    `project_id` INT                                NOT NULL,
    `service`    VARCHAR(32) CHARACTER SET 'latin1' NOT NULL,
    PRIMARY KEY (`project_id`, `service`),
    CONSTRAINT `FK_284_300`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.project_team
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`project_team`
(
    `project_id`  INT                                                             NOT NULL,
    `team_member` VARCHAR(150) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs' NOT NULL,
    `team_role`   VARCHAR(32) CHARACTER SET 'latin1'                              NOT NULL,
    `added`       TIMESTAMP                                                       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`project_id`, `team_member`),
    INDEX `team_member` (`team_member` ASC) VISIBLE,
    CONSTRAINT `FK_262_304`
        FOREIGN KEY (`team_member`)
            REFERENCES `hopsworks`.`users` (`email`)
            ON DELETE CASCADE,
    CONSTRAINT `FK_284_303`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.project_topics
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`project_topics`
(
    `topic_name`     VARCHAR(255) CHARACTER SET 'latin1' NOT NULL,
    `project_id`     INT                                 NOT NULL,
    `id`             INT                                 NOT NULL AUTO_INCREMENT,
    `subject_id`     INT                                 NULL DEFAULT NULL,
    `num_partitions` INT                                 NULL DEFAULT NULL,
    `num_replicas`   INT                                 NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `topic_name_UNIQUE` (`topic_name` ASC) VISIBLE,
    UNIQUE INDEX `topic_project` (`topic_name` ASC, `project_id` ASC) VISIBLE,
    INDEX `project_idx_proj_topics` (`project_id` ASC) VISIBLE,
    INDEX `subject_idx` (`subject_id` ASC) VISIBLE,
    CONSTRAINT `project_idx_proj_topics`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `subject_idx`
        FOREIGN KEY (`subject_id`)
            REFERENCES `hopsworks`.`subjects` (`id`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.python_dep
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`python_dep`
(
    `id`           INT                                 NOT NULL AUTO_INCREMENT,
    `dependency`   VARCHAR(128) CHARACTER SET 'latin1' NOT NULL,
    `version`      VARCHAR(128) CHARACTER SET 'latin1' NOT NULL,
    `repo_url`     VARCHAR(255)                        NOT NULL,
    `preinstalled` TINYINT(1)                          NULL DEFAULT '0',
    `install_type` INT                                 NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `dependency` (`dependency` ASC, `version` ASC, `install_type` ASC, `repo_url` ASC) VISIBLE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 31
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.python_environment
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`python_environment`
(
    `id`                INT                                   NOT NULL AUTO_INCREMENT,
    `project_id`        INT                                   NOT NULL,
    `python_version`    VARCHAR(25) CHARACTER SET 'latin1'    NOT NULL,
    `jupyter_conflicts` TINYINT(1)                            NOT NULL DEFAULT '0',
    `conflicts`         VARCHAR(12000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `project_env` (`project_id` ASC) VISIBLE,
    CONSTRAINT `FK_PYTHONENV_PROJECT`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.remote_group_project_mapping
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`remote_group_project_mapping`
(
    `id`           INT          NOT NULL AUTO_INCREMENT,
    `remote_group` VARCHAR(256) NOT NULL,
    `project`      INT          NOT NULL,
    `project_role` VARCHAR(32)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `index3` (`remote_group`(255) ASC, `project` ASC) VISIBLE,
    INDEX `fk_remote_group_project_mapping_1_idx` (`project` ASC) VISIBLE,
    CONSTRAINT `fk_remote_group_project_mapping_1`
        FOREIGN KEY (`project`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.remote_material_references
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`remote_material_references`
(
    `username`   VARCHAR(128) CHARACTER SET 'latin1' NOT NULL,
    `path`       VARCHAR(255) CHARACTER SET 'latin1' NOT NULL,
    `references` INT                                 NOT NULL DEFAULT '0',
    `lock`       INT                                 NOT NULL DEFAULT '0',
    `lock_id`    VARCHAR(30) CHARACTER SET 'latin1'  NOT NULL DEFAULT '',
    PRIMARY KEY (`username`, `path`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.remote_user
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`remote_user`
(
    `id`       INT                                 NOT NULL AUTO_INCREMENT,
    `type`     VARCHAR(45) CHARACTER SET 'latin1'  NOT NULL,
    `status`   VARCHAR(45) CHARACTER SET 'latin1'  NOT NULL DEFAULT '0',
    `auth_key` VARCHAR(64) CHARACTER SET 'latin1'  NOT NULL,
    `uuid`     VARCHAR(128) CHARACTER SET 'latin1' NOT NULL,
    `uid`      INT                                 NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uuid_UNIQUE` (`uuid` ASC) VISIBLE,
    UNIQUE INDEX `uid_UNIQUE` (`uid` ASC) VISIBLE,
    CONSTRAINT `FK_257_557`
        FOREIGN KEY (`uid`)
            REFERENCES `hopsworks`.`users` (`uid`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.roles_audit
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`roles_audit`
(
    `log_id`           BIGINT                              NOT NULL AUTO_INCREMENT,
    `target`           INT                                 NOT NULL,
    `initiator`        INT                                 NOT NULL,
    `action`           VARCHAR(45) CHARACTER SET 'latin1'  NULL DEFAULT NULL,
    `action_timestamp` TIMESTAMP                           NULL DEFAULT NULL,
    `message`          VARCHAR(45) CHARACTER SET 'latin1'  NULL DEFAULT NULL,
    `ip`               VARCHAR(45) CHARACTER SET 'latin1'  NULL DEFAULT NULL,
    `outcome`          VARCHAR(45) CHARACTER SET 'latin1'  NULL DEFAULT NULL,
    `useragent`        VARCHAR(255) CHARACTER SET 'latin1' NULL DEFAULT NULL,
    PRIMARY KEY (`log_id`),
    INDEX `initiator` (`initiator` ASC) VISIBLE,
    INDEX `target` (`target` ASC) VISIBLE,
    CONSTRAINT `FK_257_280`
        FOREIGN KEY (`initiator`)
            REFERENCES `hopsworks`.`users` (`uid`),
    CONSTRAINT `FK_257_281`
        FOREIGN KEY (`target`)
            REFERENCES `hopsworks`.`users` (`uid`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.rstudio_interpreter
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`rstudio_interpreter`
(
    `port`          INT                                 NOT NULL,
    `name`          VARCHAR(255) CHARACTER SET 'latin1' NOT NULL,
    `created`       TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_accessed` TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`port`, `name`),
    CONSTRAINT `FK_575_582`
        FOREIGN KEY (`port`)
            REFERENCES `hopsworks`.`rstudio_project` (`port`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.rstudio_project
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`rstudio_project`
(
    `port`          INT                                 NOT NULL,
    `created`       TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_accessed` TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `host_ip`       VARCHAR(255) CHARACTER SET 'latin1' NOT NULL,
    `token`         VARCHAR(255) CHARACTER SET 'latin1' NOT NULL,
    `secret`        VARCHAR(64) CHARACTER SET 'latin1'  NOT NULL,
    `pid`           BIGINT                              NOT NULL,
    `project_id`    INT                                 NOT NULL,
    PRIMARY KEY (`port`),
    INDEX `project_id` (`project_id` ASC) VISIBLE,
    CONSTRAINT `FK_284_578`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.rstudio_settings
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`rstudio_settings`
(
    `project_id`                INT                                                             NOT NULL,
    `team_member`               VARCHAR(150) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs' NOT NULL,
    `num_tf_ps`                 INT                                                             NULL     DEFAULT '1',
    `num_tf_gpus`               INT                                                             NULL     DEFAULT '0',
    `num_mpi_np`                INT                                                             NULL     DEFAULT '1',
    `appmaster_cores`           INT                                                             NULL     DEFAULT '1',
    `appmaster_memory`          INT                                                             NULL     DEFAULT '1024',
    `num_executors`             INT                                                             NULL     DEFAULT '1',
    `num_executor_cores`        INT                                                             NULL     DEFAULT '1',
    `executor_memory`           INT                                                             NULL     DEFAULT '1024',
    `dynamic_initial_executors` INT                                                             NULL     DEFAULT '1',
    `dynamic_min_executors`     INT                                                             NULL     DEFAULT '1',
    `dynamic_max_executors`     INT                                                             NULL     DEFAULT '1',
    `secret`                    VARCHAR(255) CHARACTER SET 'latin1'                             NOT NULL,
    `log_level`                 VARCHAR(32) CHARACTER SET 'latin1'                              NULL     DEFAULT 'INFO',
    `mode`                      VARCHAR(32) CHARACTER SET 'latin1'                              NOT NULL,
    `umask`                     VARCHAR(32) CHARACTER SET 'latin1'                              NULL     DEFAULT '022',
    `advanced`                  TINYINT(1)                                                      NULL     DEFAULT '0',
    `archives`                  VARCHAR(1500) CHARACTER SET 'latin1'                            NULL     DEFAULT '',
    `jars`                      VARCHAR(1500) CHARACTER SET 'latin1'                            NULL     DEFAULT '',
    `files`                     VARCHAR(1500) CHARACTER SET 'latin1'                            NULL     DEFAULT '',
    `py_files`                  VARCHAR(1500) CHARACTER SET 'latin1'                            NULL     DEFAULT '',
    `spark_params`              VARCHAR(6500) CHARACTER SET 'latin1'                            NULL     DEFAULT '',
    `shutdown_level`            INT                                                             NOT NULL DEFAULT '6',
    PRIMARY KEY (`project_id`, `team_member`),
    INDEX `team_member` (`team_member` ASC) VISIBLE,
    INDEX `secret_idx` (`secret` ASC) VISIBLE,
    CONSTRAINT `RS_FK_PROJS`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `RS_FK_USERS`
        FOREIGN KEY (`team_member`)
            REFERENCES `hopsworks`.`users` (`email`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.schemas
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`schemas`
(
    `id`         INT                                   NOT NULL AUTO_INCREMENT,
    `schema`     VARCHAR(29000) CHARACTER SET 'latin1' NOT NULL,
    `project_id` INT                                   NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `project_idx_schemas` (`project_id` ASC) VISIBLE,
    CONSTRAINT `project_idx_schemas`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.secrets
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`secrets`
(
    `uid`         INT              NOT NULL,
    `secret_name` VARCHAR(200)     NOT NULL,
    `secret`      VARBINARY(10000) NOT NULL,
    `added_on`    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `visibility`  TINYINT          NOT NULL,
    `pid_scope`   INT              NULL     DEFAULT NULL,
    PRIMARY KEY (`uid`, `secret_name`),
    CONSTRAINT `secrets_ibfk_1`
        FOREIGN KEY (`uid`)
            REFERENCES `hopsworks`.`users` (`uid`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.serving
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`serving`
(
    `id`                     INT                                  NOT NULL AUTO_INCREMENT,
    `local_port`             INT                                  NULL     DEFAULT NULL,
    `cid`                    VARCHAR(255) CHARACTER SET 'latin1'  NULL     DEFAULT NULL,
    `project_id`             INT                                  NOT NULL,
    `created`                TIMESTAMP                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `name`                   VARCHAR(255) CHARACTER SET 'latin1'  NOT NULL,
    `description`            VARCHAR(1000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `model_path`             VARCHAR(255) CHARACTER SET 'latin1'  NOT NULL,
    `artifact_version`       INT                                  NULL     DEFAULT NULL,
    `predictor`              VARCHAR(255) CHARACTER SET 'latin1'  NULL     DEFAULT NULL,
    `transformer`            VARCHAR(255) CHARACTER SET 'latin1'  NULL     DEFAULT NULL,
    `model_name`             VARCHAR(255) CHARACTER SET 'latin1'  NOT NULL,
    `model_version`          INT                                  NOT NULL,
    `model_framework`        INT                                  NOT NULL,
    `local_dir`              VARCHAR(255) CHARACTER SET 'latin1'  NULL     DEFAULT NULL,
    `batching_configuration` VARCHAR(255) CHARACTER SET 'latin1'  NULL     DEFAULT NULL,
    `optimized`              TINYINT                              NOT NULL DEFAULT '0',
    `instances`              INT                                  NOT NULL DEFAULT '0',
    `transformer_instances`  INT                                  NULL     DEFAULT NULL,
    `creator`                INT                                  NULL     DEFAULT NULL,
    `lock_ip`                VARCHAR(15) CHARACTER SET 'latin1'   NULL     DEFAULT NULL,
    `lock_timestamp`         BIGINT                               NULL     DEFAULT NULL,
    `kafka_topic_id`         INT                                  NULL     DEFAULT NULL,
    `inference_logging`      INT                                  NULL     DEFAULT NULL,
    `model_server`           INT                                  NOT NULL DEFAULT '0',
    `serving_tool`           INT                                  NOT NULL DEFAULT '0',
    `deployed`               TIMESTAMP                            NULL     DEFAULT NULL,
    `revision`               VARCHAR(8)                           NULL     DEFAULT NULL,
    `predictor_resources`    VARCHAR(1000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `transformer_resources`  VARCHAR(1000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `Serving_Constraint` (`project_id` ASC, `name` ASC) VISIBLE,
    INDEX `user_fk` (`creator` ASC) VISIBLE,
    INDEX `kafka_fk` (`kafka_topic_id` ASC) VISIBLE,
    INDEX `name_k` (`name` ASC) VISIBLE,
    CONSTRAINT `FK_284_315`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `kafka_fk`
        FOREIGN KEY (`kafka_topic_id`)
            REFERENCES `hopsworks`.`project_topics` (`id`)
            ON DELETE SET NULL,
    CONSTRAINT `user_fk_serving`
        FOREIGN KEY (`creator`)
            REFERENCES `hopsworks`.`users` (`uid`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.serving_key
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`serving_key`
(
    `id`               INT           NOT NULL AUTO_INCREMENT,
    `prefix`           VARCHAR(63)   NULL     DEFAULT '',
    `feature_name`     VARCHAR(1000) NOT NULL,
    `join_on`          VARCHAR(1000) NULL     DEFAULT NULL,
    `join_index`       INT           NOT NULL,
    `feature_group_id` INT           NOT NULL,
    `required`         TINYINT(1)    NOT NULL DEFAULT '0',
    `feature_view_id`  INT           NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `feature_view_id` (`feature_view_id` ASC) VISIBLE,
    INDEX `feature_group_id` (`feature_group_id` ASC) VISIBLE,
    CONSTRAINT `feature_group_serving_key_fk`
        FOREIGN KEY (`feature_group_id`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `feature_view_serving_key_fk`
        FOREIGN KEY (`feature_view_id`)
            REFERENCES `hopsworks`.`feature_view` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.statistic_columns
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`statistic_columns`
(
    `id`                   INT                                 NOT NULL AUTO_INCREMENT,
    `statistics_config_id` INT                                 NULL DEFAULT NULL,
    `name`                 VARCHAR(500) CHARACTER SET 'latin1' NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `statistics_config_id` (`statistics_config_id` ASC) VISIBLE,
    CONSTRAINT `statistics_config_fk`
        FOREIGN KEY (`statistics_config_id`)
            REFERENCES `hopsworks`.`statistics_config` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.statistics_config
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`statistics_config`
(
    `id`                  INT        NOT NULL AUTO_INCREMENT,
    `feature_group_id`    INT        NULL     DEFAULT NULL,
    `training_dataset_id` INT        NULL     DEFAULT NULL,
    `descriptive`         TINYINT(1) NOT NULL DEFAULT '1',
    `correlations`        TINYINT(1) NOT NULL DEFAULT '1',
    `histograms`          TINYINT(1) NOT NULL DEFAULT '1',
    `exact_uniqueness`    TINYINT(1) NOT NULL DEFAULT '1',
    PRIMARY KEY (`id`),
    INDEX `feature_group_id` (`feature_group_id` ASC) VISIBLE,
    INDEX `training_dataset_id` (`training_dataset_id` ASC) VISIBLE,
    CONSTRAINT `fg_statistics_config_fk`
        FOREIGN KEY (`feature_group_id`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `td_statistics_config_fk`
        FOREIGN KEY (`training_dataset_id`)
            REFERENCES `hopsworks`.`training_dataset` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.stream_feature_group
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`stream_feature_group`
(
    `id`                INT NOT NULL AUTO_INCREMENT,
    `timetravel_format` INT NOT NULL DEFAULT '1',
    PRIMARY KEY (`id`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.subjects
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`subjects`
(
    `id`         INT                                 NOT NULL AUTO_INCREMENT,
    `subject`    VARCHAR(255) CHARACTER SET 'latin1' NOT NULL,
    `version`    INT                                 NOT NULL,
    `schema_id`  INT                                 NOT NULL,
    `project_id` INT                                 NOT NULL,
    `created_on` TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `subjects__constraint_key` (`subject` ASC, `version` ASC, `project_id` ASC) VISIBLE,
    INDEX `project_id_idx` (`project_id` ASC) VISIBLE,
    INDEX `created_on_idx` (`created_on` ASC) VISIBLE,
    INDEX `schema_id_idx` (`schema_id` ASC) VISIBLE,
    CONSTRAINT `project_idx_subjects`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `schema_id_idx`
        FOREIGN KEY (`schema_id`)
            REFERENCES `hopsworks`.`schemas` (`id`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.subjects_compatibility
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`subjects_compatibility`
(
    `id`            INT                                                                                                          NOT NULL AUTO_INCREMENT,
    `subject`       VARCHAR(255) CHARACTER SET 'latin1'                                                                          NOT NULL,
    `compatibility` ENUM ('BACKWARD', 'BACKWARD_TRANSITIVE', 'FORWARD', 'FORWARD_TRANSITIVE', 'FULL', 'FULL_TRANSITIVE', 'NONE') NOT NULL DEFAULT 'BACKWARD',
    `project_id`    INT                                                                                                          NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `subjects_compatibility__constraint_key` (`subject` ASC, `project_id` ASC) VISIBLE,
    INDEX `project_idx_sub_comp` (`project_id` ASC) VISIBLE,
    CONSTRAINT `project_idx_sub_comp`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.system_commands
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`system_commands`
(
    `id`        BIGINT                             NOT NULL AUTO_INCREMENT,
    `host_id`   INT                                NOT NULL,
    `op`        VARCHAR(50) CHARACTER SET 'latin1' NOT NULL,
    `status`    VARCHAR(20) CHARACTER SET 'latin1' NOT NULL,
    `priority`  INT                                NOT NULL DEFAULT '0',
    `exec_user` VARCHAR(50) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `host_id` (`host_id` ASC) VISIBLE,
    CONSTRAINT `FK_481_349`
        FOREIGN KEY (`host_id`)
            REFERENCES `hopsworks`.`hosts` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 3816
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.system_commands_args
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`system_commands_args`
(
    `id`         INT                                   NOT NULL AUTO_INCREMENT,
    `command_id` BIGINT                                NOT NULL,
    `arguments`  VARCHAR(13900) CHARACTER SET 'latin1' NULL DEFAULT '',
    PRIMARY KEY (`id`),
    INDEX `command_id_idx` (`command_id` ASC) VISIBLE,
    CONSTRAINT `command_id_fk`
        FOREIGN KEY (`command_id`)
            REFERENCES `hopsworks`.`system_commands` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 3816
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.tensorboard
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`tensorboard`
(
    `project_id`    INT                                   NOT NULL,
    `user_id`       INT                                   NOT NULL,
    `endpoint`      VARCHAR(100) CHARACTER SET 'latin1'   NOT NULL,
    `ml_id`         VARCHAR(100) CHARACTER SET 'latin1'   NOT NULL,
    `cid`           VARCHAR(255) CHARACTER SET 'latin1'   NOT NULL,
    `last_accessed` TIMESTAMP                             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `hdfs_logdir`   VARCHAR(10000) CHARACTER SET 'latin1' NOT NULL,
    `secret`        VARCHAR(255) CHARACTER SET 'latin1'   NOT NULL,
    PRIMARY KEY (`project_id`, `user_id`),
    INDEX `user_id_fk` (`user_id` ASC) VISIBLE,
    CONSTRAINT `project_id_fk`
        FOREIGN KEY (`project_id`)
            REFERENCES `hopsworks`.`project` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `user_id_fk`
        FOREIGN KEY (`user_id`)
            REFERENCES `hopsworks`.`users` (`uid`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.training_dataset
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`training_dataset`
(
    `id`                           INT                                   NOT NULL AUTO_INCREMENT,
    `name`                         VARCHAR(63)                           NOT NULL,
    `feature_store_id`             INT                                   NOT NULL,
    `created`                      TIMESTAMP                             NULL     DEFAULT CURRENT_TIMESTAMP,
    `creator`                      INT                                   NOT NULL,
    `version`                      INT                                   NOT NULL,
    `data_format`                  VARCHAR(128) CHARACTER SET 'latin1'   NOT NULL,
    `description`                  VARCHAR(10000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `hopsfs_training_dataset_id`   INT                                   NULL     DEFAULT NULL,
    `external_training_dataset_id` INT                                   NULL     DEFAULT NULL,
    `training_dataset_type`        INT                                   NOT NULL DEFAULT '0',
    `seed`                         BIGINT                                NULL     DEFAULT NULL,
    `query`                        TINYINT(1)                            NOT NULL DEFAULT '0',
    `coalesce`                     TINYINT(1)                            NOT NULL DEFAULT '0',
    `train_split`                  VARCHAR(63) CHARACTER SET 'latin1'    NULL     DEFAULT NULL,
    `feature_view_id`              INT                                   NULL     DEFAULT NULL,
    `start_time`                   TIMESTAMP                             NULL     DEFAULT NULL,
    `end_time`                     TIMESTAMP                             NULL     DEFAULT NULL,
    `sample_ratio`                 FLOAT                                 NULL     DEFAULT NULL,
    `connector_id`                 INT                                   NULL     DEFAULT NULL,
    `connector_path`               VARCHAR(1000)                         NULL     DEFAULT NULL,
    `tag_path`                     VARCHAR(1000)                         NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `name_version` (`feature_store_id` ASC, `feature_view_id` ASC, `name` ASC, `version` ASC) VISIBLE,
    INDEX `feature_store_id` (`feature_store_id` ASC) VISIBLE,
    INDEX `creator` (`creator` ASC) VISIBLE,
    INDEX `td_feature_view_fk` (`feature_view_id` ASC) VISIBLE,
    INDEX `td_conn_fk` (`connector_id` ASC) VISIBLE,
    CONSTRAINT `FK_1012_877`
        FOREIGN KEY (`creator`)
            REFERENCES `hopsworks`.`users` (`uid`),
    CONSTRAINT `FK_656_817`
        FOREIGN KEY (`feature_store_id`)
            REFERENCES `hopsworks`.`feature_store` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `td_conn_fk`
        FOREIGN KEY (`connector_id`)
            REFERENCES `hopsworks`.`feature_store_connector` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `td_feature_view_fk`
        FOREIGN KEY (`feature_view_id`)
            REFERENCES `hopsworks`.`feature_view` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 9
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.training_dataset_feature
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`training_dataset_feature`
(
    `id`                      INT                                  NOT NULL AUTO_INCREMENT,
    `training_dataset`        INT                                  NULL     DEFAULT NULL,
    `feature_group`           INT                                  NULL     DEFAULT NULL,
    `name`                    VARCHAR(1000) CHARACTER SET 'latin1' NOT NULL,
    `type`                    VARCHAR(1000) CHARACTER SET 'latin1' NULL     DEFAULT NULL,
    `td_join`                 INT                                  NULL     DEFAULT NULL,
    `idx`                     INT                                  NULL     DEFAULT NULL,
    `label`                   TINYINT(1)                           NOT NULL DEFAULT '0',
    `transformation_function` INT                                  NULL     DEFAULT NULL,
    `feature_view_id`         INT                                  NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `td_key` (`training_dataset` ASC) VISIBLE,
    INDEX `fg_key` (`feature_group` ASC) VISIBLE,
    INDEX `tdf_feature_view_fk` (`feature_view_id` ASC) VISIBLE,
    INDEX `join_fk_tdf` (`td_join` ASC) VISIBLE,
    INDEX `tfn_fk_tdf` (`transformation_function` ASC) VISIBLE,
    CONSTRAINT `fg_fk_tdf`
        FOREIGN KEY (`feature_group`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE SET NULL,
    CONSTRAINT `join_fk_tdf`
        FOREIGN KEY (`td_join`)
            REFERENCES `hopsworks`.`training_dataset_join` (`id`)
            ON DELETE SET NULL,
    CONSTRAINT `td_fk_tdf`
        FOREIGN KEY (`training_dataset`)
            REFERENCES `hopsworks`.`training_dataset` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `tdf_feature_view_fk`
        FOREIGN KEY (`feature_view_id`)
            REFERENCES `hopsworks`.`feature_view` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `tfn_fk_tdf`
        FOREIGN KEY (`transformation_function`)
            REFERENCES `hopsworks`.`transformation_function` (`id`)
            ON DELETE SET NULL
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.training_dataset_filter
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`training_dataset_filter`
(
    `id`                  INT         NOT NULL AUTO_INCREMENT,
    `training_dataset_id` INT         NULL DEFAULT NULL,
    `feature_view_id`     INT         NULL DEFAULT NULL,
    `type`                VARCHAR(63) NULL DEFAULT NULL,
    `path`                VARCHAR(63) NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `tdf_training_dataset_fk` (`training_dataset_id` ASC) VISIBLE,
    INDEX `tdfilter_feature_view_fk` (`feature_view_id` ASC) VISIBLE,
    CONSTRAINT `tdf_training_dataset_fk`
        FOREIGN KEY (`training_dataset_id`)
            REFERENCES `hopsworks`.`training_dataset` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `tdfilter_feature_view_fk`
        FOREIGN KEY (`feature_view_id`)
            REFERENCES `hopsworks`.`feature_view` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.training_dataset_filter_condition
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`training_dataset_filter_condition`
(
    `id`                         INT           NOT NULL AUTO_INCREMENT,
    `training_dataset_filter_id` INT           NULL DEFAULT NULL,
    `feature_group_id`           INT           NULL DEFAULT NULL,
    `feature_name`               VARCHAR(63)   NULL DEFAULT NULL,
    `filter_condition`           VARCHAR(128)  NULL DEFAULT NULL,
    `filter_value`               VARCHAR(1024) NULL DEFAULT NULL,
    `filter_value_fg_id`         INT           NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `tdfc_training_dataset_filter_fk` (`training_dataset_filter_id` ASC) VISIBLE,
    INDEX `tdfc_feature_group_fk` (`feature_group_id` ASC) VISIBLE,
    CONSTRAINT `tdfc_feature_group_fk`
        FOREIGN KEY (`feature_group_id`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE SET NULL,
    CONSTRAINT `tdfc_training_dataset_filter_fk`
        FOREIGN KEY (`training_dataset_filter_id`)
            REFERENCES `hopsworks`.`training_dataset_filter` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.training_dataset_join
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`training_dataset_join`
(
    `id`                      INT         NOT NULL AUTO_INCREMENT,
    `training_dataset`        INT         NULL     DEFAULT NULL,
    `feature_group`           INT         NULL     DEFAULT NULL,
    `feature_group_commit_id` BIGINT      NULL     DEFAULT NULL,
    `type`                    TINYINT     NOT NULL DEFAULT '0',
    `idx`                     INT         NOT NULL DEFAULT '0',
    `prefix`                  VARCHAR(63) NULL     DEFAULT NULL,
    `feature_view_id`         INT         NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `fg_key` (`feature_group` ASC) VISIBLE,
    INDEX `tdj_feature_view_fk` (`feature_view_id` ASC) VISIBLE,
    INDEX `td_fk_tdj` (`training_dataset` ASC) VISIBLE,
    CONSTRAINT `fg_left`
        FOREIGN KEY (`feature_group`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `td_fk_tdj`
        FOREIGN KEY (`training_dataset`)
            REFERENCES `hopsworks`.`training_dataset` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `tdj_feature_view_fk`
        FOREIGN KEY (`feature_view_id`)
            REFERENCES `hopsworks`.`feature_view` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.training_dataset_join_condition
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`training_dataset_join_condition`
(
    `id`            INT           NOT NULL AUTO_INCREMENT,
    `td_join`       INT           NOT NULL,
    `left_feature`  VARCHAR(1000) NOT NULL DEFAULT '',
    `right_feature` VARCHAR(1000) NOT NULL DEFAULT '',
    PRIMARY KEY (`id`),
    INDEX `join_key` (`td_join` ASC) VISIBLE,
    CONSTRAINT `join_fk_tdjc`
        FOREIGN KEY (`td_join`)
            REFERENCES `hopsworks`.`training_dataset_join` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.training_dataset_split
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`training_dataset_split`
(
    `id`                  INT                                NOT NULL AUTO_INCREMENT,
    `training_dataset_id` INT                                NOT NULL,
    `name`                VARCHAR(63) CHARACTER SET 'latin1' NOT NULL,
    `percentage`          FLOAT                              NULL DEFAULT NULL,
    `split_type`          VARCHAR(40)                        NULL DEFAULT NULL,
    `start_time`          TIMESTAMP                          NULL DEFAULT NULL,
    `end_Time`            TIMESTAMP                          NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `dataset_id_split_name` (`training_dataset_id` ASC, `name` ASC) VISIBLE,
    INDEX `training_dataset_id` (`training_dataset_id` ASC) VISIBLE,
    CONSTRAINT `training_dataset_fk`
        FOREIGN KEY (`training_dataset_id`)
            REFERENCES `hopsworks`.`training_dataset` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.transformation_function
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`transformation_function`
(
    `id`               INT          NOT NULL AUTO_INCREMENT,
    `name`             VARCHAR(255) NOT NULL,
    `output_type`      VARCHAR(32)  NOT NULL,
    `version`          INT          NOT NULL,
    `feature_store_id` INT          NOT NULL,
    `created`          TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP,
    `creator`          INT          NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `feature_store_fn_fk` (`feature_store_id` ASC) VISIBLE,
    INDEX `creator_fn_fk` (`creator` ASC) VISIBLE,
    CONSTRAINT `creator_fn_fk`
        FOREIGN KEY (`creator`)
            REFERENCES `hopsworks`.`users` (`uid`),
    CONSTRAINT `feature_store_fn_fk`
        FOREIGN KEY (`feature_store_id`)
            REFERENCES `hopsworks`.`feature_store` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.tutorial
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`tutorial`
(
    `id`          INT          NOT NULL AUTO_INCREMENT,
    `idx`         INT          NOT NULL,
    `name`        VARCHAR(100) NOT NULL,
    `github_path` VARCHAR(200) NOT NULL,
    `description` VARCHAR(200) NOT NULL,
    PRIMARY KEY (`id`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.user_certs
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`user_certs`
(
    `projectname`  VARCHAR(100) CHARACTER SET 'latin1'                            NOT NULL,
    `username`     VARCHAR(10) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs' NOT NULL,
    `user_key`     VARBINARY(7000)                                                NULL DEFAULT NULL,
    `user_cert`    VARBINARY(3000)                                                NULL DEFAULT NULL,
    `user_key_pwd` VARCHAR(200) CHARACTER SET 'latin1'                            NULL DEFAULT NULL,
    PRIMARY KEY (`projectname`, `username`),
    INDEX `username` (`username` ASC) VISIBLE,
    CONSTRAINT `FK_260_465`
        FOREIGN KEY (`username`)
            REFERENCES `hopsworks`.`users` (`username`)
            ON DELETE CASCADE,
    CONSTRAINT `FK_287_464`
        FOREIGN KEY (`projectname`)
            REFERENCES `hopsworks`.`project` (`projectname`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.user_group
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`user_group`
(
    `uid` INT NOT NULL,
    `gid` INT NOT NULL,
    PRIMARY KEY (`uid`, `gid`),
    INDEX `gid` (`gid` ASC) VISIBLE,
    CONSTRAINT `FK_255_269`
        FOREIGN KEY (`gid`)
            REFERENCES `hopsworks`.`bbc_group` (`gid`),
    CONSTRAINT `FK_257_268`
        FOREIGN KEY (`uid`)
            REFERENCES `hopsworks`.`users` (`uid`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.userlogins
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`userlogins`
(
    `login_id`   BIGINT                              NOT NULL AUTO_INCREMENT,
    `ip`         VARCHAR(45) CHARACTER SET 'latin1'  NULL DEFAULT NULL,
    `useragent`  VARCHAR(255) CHARACTER SET 'latin1' NULL DEFAULT NULL,
    `action`     VARCHAR(80) CHARACTER SET 'latin1'  NULL DEFAULT NULL,
    `outcome`    VARCHAR(20) CHARACTER SET 'latin1'  NULL DEFAULT NULL,
    `uid`        INT                                 NOT NULL,
    `login_date` TIMESTAMP                           NULL DEFAULT NULL,
    PRIMARY KEY (`login_id`),
    INDEX `login_date` (`login_date` ASC) VISIBLE,
    INDEX `uid` (`uid` ASC) VISIBLE,
    CONSTRAINT `FK_257_345`
        FOREIGN KEY (`uid`)
            REFERENCES `hopsworks`.`users` (`uid`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 316
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.users
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`users`
(
    `uid`                    INT                                                              NOT NULL AUTO_INCREMENT,
    `username`               VARCHAR(10) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs'   NOT NULL,
    `password`               VARCHAR(128) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs'  NOT NULL,
    `email`                  VARCHAR(150) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs'  NULL     DEFAULT NULL,
    `fname`                  VARCHAR(30) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_0900_ai_ci' NULL     DEFAULT NULL,
    `lname`                  VARCHAR(30) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_0900_ai_ci' NULL     DEFAULT NULL,
    `activated`              TIMESTAMP                                                        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `title`                  VARCHAR(10) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs'   NULL     DEFAULT '-',
    `false_login`            INT                                                              NOT NULL DEFAULT '-1',
    `status`                 INT                                                              NOT NULL DEFAULT '-1',
    `isonline`               INT                                                              NOT NULL DEFAULT '-1',
    `secret`                 VARCHAR(20) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs'   NULL     DEFAULT NULL,
    `validation_key`         VARCHAR(128) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs'  NULL     DEFAULT NULL,
    `validation_key_updated` TIMESTAMP                                                        NULL     DEFAULT NULL,
    `validation_key_type`    VARCHAR(20)                                                      NULL     DEFAULT NULL,
    `mode`                   INT                                                              NOT NULL DEFAULT '0',
    `password_changed`       TIMESTAMP                                                        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `notes`                  VARCHAR(500) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs'  NULL     DEFAULT '-',
    `max_num_projects`       INT                                                              NOT NULL,
    `num_active_projects`    INT                                                              NOT NULL DEFAULT '0',
    `two_factor`             TINYINT(1)                                                       NOT NULL DEFAULT '1',
    `tours_state`            TINYINT(1)                                                       NOT NULL DEFAULT '0',
    `salt`                   VARCHAR(128) CHARACTER SET 'latin1' COLLATE 'latin1_general_cs'  NOT NULL DEFAULT '',
    PRIMARY KEY (`uid`),
    UNIQUE INDEX `username` (`username` ASC) VISIBLE,
    UNIQUE INDEX `email` (`email` ASC) VISIBLE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 10178
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.validation_report
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`validation_report`
(
    `id`                    INT                                 NOT NULL AUTO_INCREMENT,
    `feature_group_id`      INT                                 NOT NULL,
    `success`               TINYINT(1)                          NOT NULL,
    `validation_time`       TIMESTAMP                           NULL DEFAULT CURRENT_TIMESTAMP,
    `evaluation_parameters` VARCHAR(1000)                       NULL DEFAULT '{}',
    `meta`                  VARCHAR(2000)                       NULL DEFAULT '{}',
    `statistics`            VARCHAR(1000)                       NOT NULL,
    `file_name`             VARCHAR(255) CHARACTER SET 'latin1' NOT NULL,
    `ingestion_result`      VARCHAR(11)                         NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `feature_group_report_fk` (`feature_group_id` ASC) VISIBLE,
    CONSTRAINT `feature_group_report_fk`
        FOREIGN KEY (`feature_group_id`)
            REFERENCES `hopsworks`.`feature_group` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.validation_result
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`validation_result`
(
    `id`                   INT           NOT NULL AUTO_INCREMENT,
    `validation_report_id` INT           NOT NULL,
    `expectation_id`       INT           NOT NULL,
    `success`              TINYINT(1)    NOT NULL,
    `result`               VARCHAR(1000) NOT NULL,
    `meta`                 VARCHAR(1000) NULL DEFAULT '{}',
    `validation_time`      TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP,
    `ingestion_result`     VARCHAR(11)   NOT NULL,
    `expectation_config`   VARCHAR(2150) NOT NULL,
    `exception_info`       VARCHAR(1000) NULL DEFAULT '{}',
    PRIMARY KEY (`id`),
    INDEX `expectation_id` (`expectation_id` ASC) VISIBLE,
    INDEX `report_fk_validation_result` (`validation_report_id` ASC) VISIBLE,
    CONSTRAINT `report_fk_validation_result`
        FOREIGN KEY (`validation_report_id`)
            REFERENCES `hopsworks`.`validation_report` (`id`)
            ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;

-- ----------------------------------------------------------------------------
-- Table hopsworks.variables
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hopsworks`.`variables`
(
    `id`         VARCHAR(255) CHARACTER SET 'latin1'  NOT NULL,
    `value`      VARCHAR(1024) CHARACTER SET 'latin1' NOT NULL,
    `visibility` TINYINT                              NOT NULL DEFAULT '0',
    `hide`       TINYINT                              NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = latin1
    COLLATE = latin1_general_cs;
SET FOREIGN_KEY_CHECKS = 1;