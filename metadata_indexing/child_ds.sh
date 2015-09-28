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
		"statement": "INSERT INTO hopsworks.meta_inodes_ops_children_ds_buffer (inodeid, parentid, operation, logical_time, processed) (SELECT DISTINCT log.inode_id, log.dataset_id, log.operation, log.logical_time, 0 as processed FROM hops.hdfs_metadata_log log, (SELECT p.inode_id as id, p.dataset_id as parentt, p.* FROM hops.hdfs_metadata_log p, (SELECT i.inode_id as id FROM hops.hdfs_metadata_log i, (SELECT inn.id FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE i.dataset_id = root.id) AS project WHERE p.dataset_id = project.id) as dataset WHERE log.dataset_id = dataset.id LIMIT 100);"
		},

		{
		"statement": "INSERT INTO hopsworks.meta_inodes_ops_children_ds_buffer (inodeid, parentid, operation, logical_time, processed) (SELECT DISTINCT log.inode_id, log.dataset_id, log.operation, log.logical_time, 0 as processed FROM hops.hdfs_metadata_log log, (SELECT p.id as datasetid, p.parent_id as parentt, p.* FROM hops.hdfs_inodes p, (SELECT i.id as projectid FROM hops.hdfs_inodes i, (SELECT inn.id as rootid FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE i.parent_id = root.rootid) AS project WHERE p.parent_id = project.projectid) as dataset WHERE log.dataset_id = dataset.datasetid LIMIT 100);"
		},

		{
		"statement": "SELECT composite.*, metadata.EXTENDED_METADATA FROM (SELECT DISTINCT hi.id as _id, op._parent, hi.name, mib.description, op.operation, op.logical_time, mib.searchable FROM hops.hdfs_inodes hi, hopsworks.meta_inode_basic_metadata mib, (SELECT log.inodeid as child_id, log.parentid as _parent, log.operation, log.logical_time FROM hopsworks.meta_inodes_ops_children_ds_buffer log WHERE log.operation = 0) as op WHERE hi.id = op.child_id AND hi.parent_id = mib.inode_pid AND hi.name = mib.inode_name LIMIT 100)as composite LEFT JOIN (SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR \"|\" ) AS EXTENDED_METADATA FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md WHERE mtt.tupleid = md.tupleid GROUP BY (mtt.inodeid) LIMIT 0 , 30) as metadata ON metadata.inodeid = composite._id ORDER BY composite.logical_time ASC;"
		},

		{
		"statement" : "SELECT DISTINCT c.inodeid as _id, c.parentid as _parent, c.logical_time, c.operation FROM hopsworks.meta_inodes_ops_children_ds_buffer c WHERE c.operation = 1 LIMIT 100;"
		},

		{
		"statement" : "DELETE FROM hopsworks.meta_inodes_ops_children_ds_buffer;"
		},

		{
		"statement" : "DELETE FROM hopsworks.meta_inodes_ops_datasets_buffer WHERE processed = 1;"
		}

        ],
        "index" : "dataset",
	"type" : "child"
    }
}
'  | java \
              -cp "${lib}/*" \
              -Dlog4j.configurationFile=${bin}/log4j2.xml \
              org.xbib.tools.Runner \
              org.xbib.tools.JDBCImporter
