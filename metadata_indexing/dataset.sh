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
		"statement": "INSERT INTO hopsworks.meta_inodes_ops_datasets_deleted (inodeid, parentid, processed) (SELECT outt.id as ds_id, outt.parent_id as parentt, 0 as processed FROM hops.hdfs_inodes outt, (SELECT i.id as parentid FROM hops.hdfs_inodes i, (SELECT inn.id AS rootid FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE i.parent_id = root.rootid) AS parent WHERE outt.parent_id = parent.parentid LIMIT 100);"
		},

		{
		"statement": "INSERT INTO hopsworks.meta_inodes_ops_datasets_deleted (inodeid, parentid, processed) (SELECT log.inode_id as ds_id, log.dataset_id as parent, 0 as processed FROM hops.hdfs_metadata_log log, (SELECT i.id FROM hops.hdfs_inodes i, (SELECT inn.id AS rootid FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE i.parent_id = root.rootid) AS parent WHERE log.operation = 1 AND log.dataset_id = parent.id LIMIT 100);"
		},

		{
		"statement": "SELECT ds._id, ds.name, ds.inode_id, ds.dataset_id as parentid, ds.description, ds.operation, metadata.EXTENDED_METADATA FROM (SELECT DISTINCT composite.*, dt.inode_pid, dt.inode_name, dt.description FROM hopsworks.dataset dt, (SELECT ops.inode_id as _id, ops.dataset_id as dsid, dataset.name, ops.* FROM hops.hdfs_metadata_log ops, (SELECT outt.id as ds_id, outt.parent_id as parentt, outt.* FROM hops.hdfs_inodes outt, (SELECT i.id as parentid FROM hops.hdfs_inodes i, (SELECT inn.id AS rootid FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS temp WHERE i.parent_id = temp.rootid) AS parent WHERE outt.parent_id = parent.parentid) as dataset WHERE ops.operation = 0 AND ops.inode_id = dataset.ds_id AND ops.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_datasets_deleted) LIMIT 100)as composite WHERE dt.inode_pid = composite.dsid AND dt.inode_name = composite.name AND dt.searchable=1) as ds LEFT JOIN (SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR \"|\" ) AS EXTENDED_METADATA FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md WHERE mtt.tupleid = md.tupleid GROUP BY (mtt.inodeid) LIMIT 0 , 30) as metadata ON metadata.inodeid = ds._id ORDER BY parentid ASC;"
		},

		{
		"statement": "SELECT ops.inode_id as _id, ops.dataset_id as parentid, ops.* FROM hops.hdfs_metadata_log ops WHERE ops.operation = 1 AND ops.inode_id IN (SELECT inode_id FROM hopsworks.meta_inodes_ops_datasets_deleted WHERE processed = 0);"
		},

		{
		"statement": "UPDATE hops.hdfs_metadata_log set operation = -1 WHERE inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_datasets_deleted);"
		},

		{
		"statement": "DELETE FROM hopsworks.meta_inodes_ops_datasets_deleted;"
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
