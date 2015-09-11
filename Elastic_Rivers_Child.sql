-- TAKE A BATCH OF RECORDS INTO THE BUFFER TABLE

INSERT INTO hopsworks.meta_inodes_ops_children_deleted (inodeid, parentid, processed) 
(SELECT hops.hdfs_metadata_log.inode_id, hops.hdfs_metadata_log.dataset_id, 0 FROM hops.hdfs_metadata_log LIMIT 100)


-- SELECT ALL CHILDREN THAT HAVE BEEN ADDED (OPERATION 0)

SELECT composite.*, metadata.EXTENDED_METADATA
	FROM (
	SELECT DISTINCT hi.id as _id, child._parent, hi.name, child.logical_time
	FROM hops.hdfs_inodes hi,   

		(SELECT outt.inode_id as child_id, outt.dataset_id as _parent, outt.logical_time
		FROM hops.hdfs_metadata_log outt, 

			(SELECT i.id as parentid 
			FROM hops.hdfs_inodes i, 

				(SELECT inn.id AS rootid 
				FROM hops.hdfs_inodes inn 
				WHERE inn.parent_id = 1
				) AS temp 
			WHERE i.parent_id = temp.rootid
			) AS parent 

		WHERE outt.dataset_id = parent.parentid AND outt.operation = 0
		) as child 

	WHERE hi.id = child.child_id 
	AND hi.id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_datasets_deleted) LIMIT 100
)as composite

LEFT JOIN (
	SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR  '|' ) AS EXTENDED_METADATA
	FROM meta_tuple_to_file mtt, meta_data md
	WHERE mtt.tupleid = md.tupleid
	GROUP BY (mtt.inodeid)
	LIMIT 0 , 30

) as metadata

ON metadata.inodeid = composite._id
ORDER BY composite.logical_time ASC


-- SELECT ALL CHILDREN THAT HAVE BEEN DELETED/RENAMED (OPERATION 1) AND ARE NOT YET PROCESSED

SELECT ops.inode_id as _id, ops.dataset_id as _parent, ops.* 
FROM hops.hdfs_metadata_log ops

WHERE ops.operation = 1
AND ops.inode_id IN (SELECT inode_id FROM hopsworks.meta_inodes_ops_children_deleted WHERE processed = 0)


-- UPDATE AS PROCESSED ALL CHILDREN THAT HAVE BEEN PROCESSED (PROCESSED = 1)

UPDATE hopsworks.meta_inodes_ops_children_deleted m,

(SELECT p.id AS id FROM hops.hdfs_inodes p,

	(SELECT inn.id AS rootid 
	FROM hops.hdfs_inodes inn 
	WHERE inn.parent_id = 1) AS root

WHERE p.parent_id = root.rootid) AS parent

SET m.processed = 1
WHERE m.parentid = parent.id AND m.inodeid IN (SELECT inode_id FROM hops.hdfs_metadata_log)


-- DELETE ALL CHILDREN THAT HAVE BEEN MARKED AS PROCESSED (PROCESSED = 1)

DELETE FROM hopsworks.meta_inodes_ops_children_deleted WHERE processed = 1


-- DELETE ALL PROCESSED CHILDREN FROM THE HDFS_METADATA_LOG TABLE

DELETE FROM hops.hdfs_metadata_log WHERE inode_id NOT IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_children_deleted)



