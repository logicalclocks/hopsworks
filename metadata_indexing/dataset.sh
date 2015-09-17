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
		"statement": "INSERT INTO hopsworks.meta_inodes_ops_datasets_deleted (inodeid, parentid, processed) (SELECT log.inode_id, log.dataset_id, 0 as processed FROM hops.hdfs_metadata_log log, (SELECT p.inode_id as id, p.dataset_id, 0 as processed FROM hops.hdfs_metadata_log p, (SELECT inn.id FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE p.dataset_id = root.id) AS parent WHERE log.dataset_id = parent.id LIMIT 100);"
		},

		{
		"statement": "SELECT composite.*, metadata.EXTENDED_METADATA FROM (SELECT DISTINCT dsop.*, dt.description FROM hopsworks.dataset dt, (SELECT log.inode_id as _id, log.dataset_id as parentid, dataset.name, log.* FROM hops.hdfs_metadata_log log, (SELECT p.id as parentid, p.parent_id as parentt, p.* FROM hops.hdfs_inodes p, (SELECT i.id FROM hops.hdfs_inodes i, (SELECT inn.id FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE i.parent_id = root.id) AS project WHERE p.parent_id = project.id) AS dataset	WHERE log.inode_id = dataset.id) AS dsop WHERE dt.inode_pid = dsop.parentid AND dt.inode_name = dsop.name AND dsop._id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_datasets_deleted) LIMIT 100) AS composite LEFT JOIN (SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR \"|\" ) AS EXTENDED_METADATA FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md WHERE mtt.tupleid = md.tupleid GROUP BY (mtt.inodeid) LIMIT 0 , 30) as metadata ON metadata.inodeid = composite._id ORDER BY composite.parentid ASC;"
		},

		{
		"statement": "SELECT log.inode_id as _id, log.dataset_id as parentid, log.logical_time, log.operation FROM hops.hdfs_metadata_log log, (SELECT log.dataset_id, log.inode_id FROM hops.hdfs_metadata_log log, (SELECT inn.id FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE log.dataset_id = root.id) AS project WHERE log.dataset_id = project.inode_id AND log.operation = 1 AND log.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_datasets_deleted WHERE processed = 0);"
		},

		{
		"statement": "SELECT log.inode_id as _id, log.dataset_id as parentid, log.logical_time, log.operation FROM hops.hdfs_metadata_log log, (SELECT p.parent_id, p.id FROM hops.hdfs_inodes p, (SELECT inn.id FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE p.parent_id = root.id) AS project WHERE log.dataset_id = project.id AND log.operation = 1 AND log.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_datasets_deleted WHERE processed = 0);"
		}

        ],
        "index" : "datasets",
	"type" : "dataset"
    }
}
'  | java \
              -cp "${lib}/*" \
              -Dlog4j.configurationFile=${bin}/log4j2.xml \
              org.xbib.tools.Runner \
              org.xbib.tools.JDBCImporter
