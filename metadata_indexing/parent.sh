#!/bin/sh

bin=$JDBC_IMPORTER_HOME/bin
lib=$JDBC_IMPORTER_HOME/lib
echo '
{
    "type" : "jdbc",
    "jdbc" : {
        "url" : "jdbc:mysql://193.10.66.125:3306/?useUnicode=true&characterEncoding=UTF-8",
	"schedule" : "0/5 0-59 0-23 ? * *",
	"elasticsearch.cluster" : "hopsworks",
        "user" : "kthfs",
        "password" : "kthfs",
        "locale" : "en_US",
        "sql" : [
		{
		"statement": "INSERT INTO hopsworks.meta_inodes_ops_parents_deleted (inodeid, parentid, processed) (SELECT log.inode_id, log.dataset_id, 0 as processed FROM hops.hdfs_metadata_log log, (SELECT inn.id AS rootid FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE log.dataset_id = root.rootid LIMIT 100);"
		},

		{
		"statement": "SELECT composite.*, metadata.EXTENDED_METADATA FROM (SELECT log.inode_id as _id, project.name, project.meta_enabled, log.* FROM hops.hdfs_metadata_log log, (SELECT i.id, i.name, i.meta_enabled FROM hops.hdfs_inodes i, (SELECT inn.id FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE i.parent_id = root.id) as project WHERE log.operation = 0 AND log.inode_id = project.id AND log.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_parents_deleted) LIMIT 100)as composite LEFT JOIN(SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR \"|\" ) AS EXTENDED_METADATA FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md WHERE mtt.tupleid = md.tupleid GROUP BY (mtt.inodeid) LIMIT 0, 30) as metadata ON metadata.inodeid = composite._id ORDER BY composite.logical_time ASC;"
		},

		{
		"statement": "SELECT log.inode_id as _id, log.* FROM hops.hdfs_metadata_log log, (SELECT inn.id FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE log.operation = 1 AND log.dataset_id = root.id AND log.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_parents_deleted);"
		}

        ],
        "index" : "project",
	"type" : "parent"
    }
}
'  | java \
              -cp "${lib}/*" \
              -Dlog4j.configurationFile=${bin}/log4j2.xml \
              org.xbib.tools.Runner \
              org.xbib.tools.JDBCImporter
