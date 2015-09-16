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
		"statement": "INSERT INTO hopsworks.meta_inodes_ops_children_deleted (inodeid, parentid, processed) (SELECT DISTINCT hi.id as _id, hi.parent_id, 0 as processed FROM hops.hdfs_inodes hi, (SELECT log.inode_id as inodeid, log.dataset_id as parentid, 0 as processed FROM hops.hdfs_metadata_log log, (SELECT i.id as parentid FROM hops.hdfs_inodes i, (SELECT inn.id AS rootid FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE i.parent_id = root.rootid) AS parent WHERE log.dataset_id = parent.parentid LIMIT 100) as child WHERE hi.id = child.inodeid AND hi.parent_id <> child.parentid LIMIT 100);"
		},

		{
		"statement": "INSERT INTO hopsworks.meta_inodes_ops_children_deleted (inodeid, parentid, processed) (SELECT log.inode_id as inodeid, log.dataset_id as parentid, 0 as processed FROM hops.hdfs_metadata_log log, (SELECT i.id as parentid FROM hops.hdfs_inodes i, (SELECT inn.id AS rootid FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE i.parent_id = root.rootid) AS parent WHERE log.operation = 1 AND log.dataset_id = parent.parentid LIMIT 100);"
		},

		{
		"statement": "SELECT composite._id, composite._parent, composite.*, metadata.EXTENDED_METADATA FROM (SELECT DISTINCT hi.id as _id, child._parent, hi.name, child.operation, child.logical_time FROM hops.hdfs_inodes hi, (SELECT outt.inode_id as child_id, outt.dataset_id as _parent, outt.operation, outt.logical_time FROM hops.hdfs_metadata_log outt, (SELECT i.id as parentid FROM hops.hdfs_inodes i, (SELECT inn.id AS rootid FROM hops.hdfs_inodes inn WHERE inn.parent_id=1) AS root WHERE i.parent_id = root.rootid) AS parent WHERE outt.dataset_id = parent.parentid AND outt.operation = 0) as child WHERE hi.id = child.child_id AND hi.parent_id <> child._parent AND hi.id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_children_deleted) LIMIT 100)as composite LEFT JOIN (SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR  \"|\" ) AS EXTENDED_METADATA FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md WHERE mtt.tupleid = md.tupleid GROUP BY (mtt.inodeid) LIMIT 0 , 30) as metadata ON metadata.inodeid = composite._id ORDER BY composite.logical_time ASC;"
		},

		{
		"statement": "SELECT log.inode_id as _id, log.dataset_id as _parent, log.logical_time, log.operation FROM hops.hdfs_metadata_log log,(SELECT c.inodeid FROM hopsworks.meta_inodes_ops_children_deleted c WHERE processed = 0) as buffer WHERE log.operation = 1 AND log.inode_id = buffer.inodeid;"
		},

		{
		"statement" : "UPDATE hops.hdfs_metadata_log set operation = -1 WHERE inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_children_deleted);"
		},

		{
		"statement" : "DELETE FROM hopsworks.meta_inodes_ops_children_deleted;"
		}

        ],
        "index" : "project",
	"type" : "child"
    }
}
'  | java \
              -cp "${lib}/*" \
              -Dlog4j.configurationFile=${bin}/log4j2.xml \
              org.xbib.tools.Runner \
              org.xbib.tools.JDBCImporter
