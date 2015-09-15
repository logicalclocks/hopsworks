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
		"statement": "INSERT INTO hopsworks.meta_inodes_ops_children_deleted (inodeid, parentid, processed) (SELECT outt.inode_id as inodeid, outt.dataset_id as parentid, 0 as processed FROM hops.hdfs_metadata_log outt, (SELECT i.id as parentid FROM hops.hdfs_inodes i, (SELECT inn.id AS rootid FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE i.parent_id = root.rootid) AS parent WHERE outt.dataset_id = parent.parentid LIMIT 100)"
		},

		{
		"statement": "SELECT composite.*, metadata.EXTENDED_METADATA FROM (SELECT DISTINCT hi.id as _id, child._parent, hi.name, child.logical_time FROM hops.hdfs_inodes hi, (SELECT outt.inode_id as child_id, outt.dataset_id as _parent, outt.logical_time FROM hops.hdfs_metadata_log outt, (SELECT i.id as parentid FROM hops.hdfs_inodes i, (SELECT inn.id AS rootid FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root WHERE i.parent_id = root.rootid) AS parent WHERE outt.dataset_id = parent.parentid AND outt.operation = 0) as child WHERE hi.id = child.child_id AND hi.id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_children_deleted) LIMIT 100)as composite LEFT JOIN (SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR \"|\" ) AS EXTENDED_METADATA FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md WHERE mtt.tupleid = md.tupleid GROUP BY (mtt.inodeid) LIMIT 0 , 30) as metadata ON metadata.inodeid = composite._id ORDER BY composite.logical_time ASC"
		},

		{
		"statement": "SELECT log.inode_id as _id, log.dataset_id as _parent, log.* FROM hops.hdfs_metadata_log log WHERE log.operation = 1 AND log.inode_id IN (SELECT inode_id FROM hopsworks.meta_inodes_ops_children_deleted WHERE processed = 0)"
		},

		{
		"statement" : "UPDATE hops.hdfs_metadata_log set operation = -1 WHERE inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_children_deleted)"
		},

		{
		"statement" : "DELETE FROM hopsworks.meta_inodes_ops_children_deleted"
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
