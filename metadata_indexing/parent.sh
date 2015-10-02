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
		"statement": "INSERT INTO hopsworks.meta_inodes_ops_parents_buffer (inodeid, parentid, operation, logical_time, processed)(SELECT log.inode_id, log.dataset_id, log.operation, log.logical_time, 0 as processed FROM hops.hdfs_metadata_log log, (SELECT inn.id AS rootid FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE log.dataset_id = root.rootid LIMIT 100);"
		},

		{
		"statement": "SELECT composite.*, 1 as searchable, metadata.EXTENDED_METADATA FROM (SELECT i.id as _id, i.name, project.inodeid, project.parentid, project.operation, project.logical_time FROM hops.hdfs_inodes i, (SELECT log.inodeid, log.parentid, log.operation,log.logical_time FROM hopsworks.meta_inodes_ops_parents_buffer log WHERE log.operation = 0) AS project WHERE i.id = project.inodeid LIMIT 100) as composite LEFT JOIN(SELECT mtt.inodeid, GROUP_CONCAT(md.data SEPARATOR \"|\") AS EXTENDED_METADATA FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md WHERE mtt.tupleid = md.tupleid GROUP BY (mtt.inodeid) LIMIT 0, 30) as metadata ON metadata.inodeid = composite._id ORDER BY composite.logical_time ASC;"
		},

		{
		"statement": "SELECT DISTINCT log.inodeid as _id, log.parentid, log.operation, log.logical_time FROM hopsworks.meta_inodes_ops_parents_buffer log WHERE log.operation = 1;"
		},

		{
		"statement": "UPDATE hopsworks.meta_inodes_ops_parents_buffer m SET m.processed = 1;"
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
