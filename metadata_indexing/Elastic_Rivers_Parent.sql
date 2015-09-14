-- TAKE A BATCH OF RECORDS INTO THE BUFFER TABLE

INSERT INTO hopsworks.meta_inodes_ops_parents_deleted (inodeid, parentid, processed) 
(SELECT hops.hdfs_metadata_log.inode_id, hops.hdfs_metadata_log.dataset_id, 0 FROM hops.hdfs_metadata_log LIMIT 100)

-- SELECTIVELY DUMP A BATCH OF PARENT RECORDS INTO THE BUFFER TABLE (convenient when it comes to update the buffer table)

INSERT INTO hopsworks.meta_inodes_ops_parents_deleted (inodeid, parentid, processed)

	(SELECT i.id, i.parent_id, 0 as processed FROM hops.hdfs_inodes i, 
		(SELECT inn.id AS rootid FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root 
	WHERE i.parent_id = root.rootid LIMIT 100)

-- SELECT ALL PARENTS THAT HAVE BEEN ADDED (OPERATION 0)

SELECT composite.*, metadata.EXTENDED_METADATA

FROM (
	SELECT log.inode_id as _id, parent.name, parent.meta_enabled, log.* 
	FROM hops.hdfs_metadata_log log, 

		(SELECT i.id, i.name, i.meta_enabled 
		FROM hops.hdfs_inodes i, 

			(SELECT inn.id AS rootid 
			FROM hops.hdfs_inodes inn 
			WHERE inn.parent_id = 1
			) AS root 

		WHERE i.parent_id = root.rootid
		) as parent 

	WHERE log.operation = 0 AND log.inode_id = parent.id 
	AND log.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_parents_deleted) LIMIT 100

)as composite

LEFT JOIN(
	SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR  '|' ) AS EXTENDED_METADATA
	FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md
	WHERE mtt.tupleid = md.tupleid
	GROUP BY (mtt.inodeid)
	LIMIT 0 , 30

) as metadata

ON metadata.inodeid = composite._id
ORDER BY composite.logical_time ASC


-- SELECT ALL PARENTS THAT HAVE BEEN DELETED/RENAMED (OPERATION 1)


SELECT log.inode_id as _id, log.* 

FROM hops.hdfs_metadata_log log, 
	(SELECT inn.id AS rootid 
	FROM hops.hdfs_inodes inn 
	WHERE inn.parent_id = 1) AS temp 

WHERE log.operation = 1 AND log.dataset_id = temp.rootid 
AND log.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_parents_deleted)


-- MARK AS PROCESSED ALL PARENTS THAT HAVE BEEN PROCESSED (PROCESSED = 1)

UPDATE hopsworks.meta_inodes_ops_parents_deleted m,

	(SELECT inn.id 
	FROM hops.hdfs_inodes inn 
	WHERE inn.parent_id = 1) AS root

SET m.processed = 1
WHERE m.parentid = root.id AND m.inodeid IN (SELECT inode_id FROM hops.hdfs_metadata_log)


-- DELETE ALL PARENTS THAT HAVE BEEN MARKED AS PROCESSED (PROCESSED = 1)

DELETE FROM hopsworks.meta_inodes_ops_parents_deleted WHERE processed = 1


-- DELETE ALL PROCESSED PARENTS FROM THE HDFS_METADATA_LOG TABLE

DELETE FROM hops.hdfs_metadata_log WHERE inode_id NOT IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_parents_deleted)
