-- TAKE A BATCH OF RECORDS BLINDLY INTO THE BUFFER TABLE

INSERT INTO hopsworks.meta_inodes_ops_children_deleted (inodeid, parentid, processed) 
(SELECT hops.hdfs_metadata_log.inode_id, hops.hdfs_metadata_log.dataset_id, 0 FROM hops.hdfs_metadata_log LIMIT 100)

-- SELECTIVELY DUMP A BATCH OF CHILDREN RECORDS INTO THE BUFFER TABLE (convenient when it comes to update the buffer table)

INSERT INTO hopsworks.meta_inodes_ops_children_deleted (inodeid, parentid, processed) (

	SELECT outt.inode_id as inodeid, outt.dataset_id as parentid, 0 as processed FROM hops.hdfs_metadata_log outt, 
		(SELECT i.id as parentid FROM hops.hdfs_inodes i, 
			(SELECT inn.id AS rootid FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root 
		WHERE i.parent_id = root.rootid) AS parent 

	WHERE outt.dataset_id = parent.parentid LIMIT 100)

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
				) AS root 
			WHERE i.parent_id = root.rootid
			) AS parent 

		WHERE outt.dataset_id = parent.parentid AND outt.operation = 0
		) as child 

	WHERE hi.id = child.child_id 
	AND hi.id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_datasets_deleted) LIMIT 100
)as composite

LEFT JOIN (
	SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR  '|' ) AS EXTENDED_METADATA
	FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md
	WHERE mtt.tupleid = md.tupleid
	GROUP BY (mtt.inodeid)
	LIMIT 0 , 30

) as metadata

ON metadata.inodeid = composite._id
ORDER BY composite.logical_time ASC


-- SELECT ALL CHILDREN THAT HAVE BEEN DELETED/RENAMED (OPERATION 1) AND ARE NOT YET PROCESSED

SELECT log.inode_id as _id, log.dataset_id as _parent, log.* 
FROM hops.hdfs_metadata_log log

WHERE log.operation = 1
AND log.inode_id IN (SELECT inode_id FROM hopsworks.meta_inodes_ops_children_deleted WHERE processed = 0)


-- MARK AS PROCESSED ALL CHILDREN THAT HAVE BEEN PROCESSED (PROCESSED = 1 - jdbc driver throws no database selected error - taken care of the first statement)

UPDATE hopsworks.meta_inodes_ops_children_deleted m, 

	(SELECT p.id 
	FROM hops.hdfs_inodes p, 

		(SELECT inn.id 
		FROM hops.hdfs_inodes inn 
		WHERE inn.parent_id = 1) AS root 

	WHERE p.parent_id = root.id) AS parent 

SET m.processed = 1 WHERE m.parentid = parent.id 
AND m.inodeid IN (SELECT inode_id FROM hops.hdfs_metadata_log)

-- MARK AS PROCESSED ALL CHILDREN THAT HAVE BEEN PROCESSED (PROCESSED = 1)

UPDATE hopsworks.meta_inodes_ops_children_deleted m 
SET m.processed = 1 AND m.inodeid IN (SELECT inode_id FROM hops.hdfs_metadata_log)

-- DELETE ALL CHILDREN THAT HAVE BEEN MARKED AS PROCESSED (PROCESSED = 1)

DELETE FROM hopsworks.meta_inodes_ops_children_deleted WHERE processed = 1


-- DELETE ALL PROCESSED CHILDREN FROM THE HDFS_METADATA_LOG TABLE

DELETE FROM hops.hdfs_metadata_log WHERE inode_id NOT IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_children_deleted)



