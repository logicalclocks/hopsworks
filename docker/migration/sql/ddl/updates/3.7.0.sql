-- FSTORE-1047
CREATE TABLE IF NOT EXISTS `embedding`
(
    `id`                   int(11)      NOT NULL AUTO_INCREMENT,
    `feature_group_id`     int(11)      NOT NULL,
    `col_prefix`           varchar(255) NULL,
    `vector_db_index_name` varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `feature_group_id` (`feature_group_id`),
    CONSTRAINT `feature_group_embedding_fk` FOREIGN KEY (`feature_group_id`) REFERENCES `feature_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE = ndbcluster
  DEFAULT CHARSET = latin1
  COLLATE = latin1_general_cs;

CREATE TABLE IF NOT EXISTS `embedding_feature`
(
    `id`                       int(11)      NOT NULL AUTO_INCREMENT,
    `embedding_id`             int(11)      NOT NULL,
    `name`                     varchar(255) NOT NULL,
    `dimension`                int          NOT NULL,
    `similarity_function_type` varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `embedding_id` (`embedding_id`),
    CONSTRAINT `embedding_feature_fk` FOREIGN KEY (`embedding_id`) REFERENCES `embedding` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE = ndbcluster
  DEFAULT CHARSET = latin1
  COLLATE = latin1_general_cs;