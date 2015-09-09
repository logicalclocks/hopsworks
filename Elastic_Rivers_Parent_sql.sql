-- TAKE A BATCH OF RECORDS INTO THE BUFFER TABLE

INSERT INTO hopsworks.meta_inodes_ops_parents_deleted (inodeid, parentid, processed) 
(SELECT hops.hdfs_metadata_log.inode_id, hops.hdfs_metadata_log.dataset_id, 0 FROM hops.hdfs_metadata_log LIMIT 100)


-- SELECT ALL PARENTS THAT HAVE BEEN ADDED (OPERATION 0)

SELECT composite.*, metadata.EXTENDED_METADATA

FROM (
	SELECT ops.inode_id as _id, inodeinn.name, inodeinn.meta_enabled, ops.* 

	FROM hops.hdfs_metadata_log ops, 
		(SELECT i.id, i.name, i.meta_enabled 
		FROM hops.hdfs_inodes i, 
			(SELECT inn.id AS rootid 
			FROM hops.hdfs_inodes inn 
			WHERE inn.parent_id = 1) AS temp 

		WHERE i.parent_id = temp.rootid) as inodeinn 

	WHERE ops.operation = 0 AND ops.inode_id = inodeinn.id 
	AND ops.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_parents_deleted) LIMIT 100

)as composite

LEFT JOIN(
	SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR  '_' ) AS EXTENDED_METADATA
	FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md
	WHERE mtt.tupleid = md.tupleid
	GROUP BY (mtt.inodeid)
	LIMIT 0 , 30

) as metadata

ON metadata.inodeid = composite._id
ORDER BY composite.logical_time ASC


-- SELECT ALL PARENTS THAT HAVE BEEN DELETED/RENAMED (OPERATION 1)


SELECT ops.inode_id as _id, ops.* 

FROM hops.hdfs_metadata_log ops, 
	(SELECT inn.id AS rootid 
	FROM hops.hdfs_inodes inn 
	WHERE inn.parent_id = 1) AS temp 

WHERE ops.operation = 1 AND ops.dataset_id = temp.rootid 
AND ops.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_parents_deleted)


-- UPDATE AS PROCESSED ALL PARENTS THAT HAVE BEEN PROCESSED (PROCESSED = 1)

UPDATE hopsworks.meta_inodes_ops_parents_deleted m,

	(SELECT inn.id AS rootid 
	FROM hops.hdfs_inodes inn 
	WHERE inn.parent_id = 1) AS temp

SET m.processed = 1
WHERE m.parentid = temp.rootid AND m.inodeid IN (SELECT inode_id FROM hops.hdfs_metadata_log)


-- DELETE ALL PARENTS THAT HAVE BEEN MARKED AS PROCESSED (PROCESSED = 1)

DELETE FROM hopsworks.meta_inodes_ops_parents_deleted WHERE processed = 1


-- DELETE ALL PROCESSED PARENTS FROM THE HDFS_METADATA_LOG TABLE

DELETE FROM hops.hdfs_metadata_log WHERE inode_id NOT IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_parents_deleted)
