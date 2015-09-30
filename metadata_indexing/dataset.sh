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
		"statement": "INSERT INTO hopsworks.meta_inodes_ops_datasets_buffer (inodeid, parentid, operation, logical_time, processed) (SELECT log.inode_id, log.dataset_id, log.operation, log.logical_time, 0 as processed FROM hops.hdfs_metadata_log log, (SELECT p.inode_id as id, p.dataset_id, 0 as processed FROM hops.hdfs_metadata_log p, (SELECT inn.id FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE p.dataset_id = root.id) AS project WHERE log.dataset_id = project.id LIMIT 100);"
		},

		{
		"statement": "INSERT INTO hopsworks.meta_inodes_ops_datasets_buffer (inodeid, parentid, operation, logical_time, processed) (SELECT log.inode_id, log.dataset_id, log.operation, log.logical_time, 0 as processed FROM hops.hdfs_metadata_log log, (SELECT p.id, p.parent_id, 0 as processed FROM hops.hdfs_inodes p, (SELECT inn.id FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE p.parent_id = root.id) AS project WHERE log.dataset_id = project.id LIMIT 100);"
		},

		{
		"statement": "SELECT composite.*, metadata.EXTENDED_METADATA FROM (SELECT DISTINCT dataset._id, dataset.name, dataset.parentid, dt.description, dataset.operation, dataset.logical_time, dt.searchable FROM hopsworks.dataset dt, (SELECT i.id, i.name, ds.* FROM hops.hdfs_inodes i,(SELECT log.inodeid as _id, log.parentid, log.operation, log.logical_time FROM hopsworks.meta_inodes_ops_datasets_buffer log WHERE log.operation = 0) as ds WHERE i.id = ds._id) AS dataset WHERE dt.inode_pid = dataset.parentid AND dt.inode_name = dataset.name LIMIT 100) as composite LEFT JOIN (SELECT mtt.inodeid, GROUP_CONCAT(md.data SEPARATOR \"|\") AS EXTENDED_METADATA FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md WHERE mtt.tupleid = md.tupleid GROUP BY (mtt.inodeid) LIMIT 0 , 30) as metadata ON metadata.inodeid = composite._id ORDER BY composite.parentid ASC;"
		},

		{
		"statement": "SELECT DISTINCT log.inodeid as _id, log.parentid, log.logical_time, log.operation FROM hopsworks.meta_inodes_ops_datasets_buffer log WHERE log.operation = 1;"
		},

		{
		"statement": "UPDATE hopsworks.meta_inodes_ops_datasets_buffer m SET m.processed = 1;"
		}

        ],
        "index" : "dataset",
	"type" : "parent"
    }
}
'  | java \
              -cp "${lib}/*" \
              -Dlog4j.configurationFile=${bin}/log4j2.xml \
              org.xbib.tools.Runner \
              org.xbib.tools.JDBCImporter
