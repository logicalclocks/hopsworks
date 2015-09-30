-- TAKE A BATCH OF RECORDS INTO THE BUFFER TABLE -- DB NAME MUST CHANGE TO POINT TO HOPS (for hdfs_metadata_log and hdfs_inodes)

INSERT INTO hopsworks.meta_inodes_ops_parents_deleted (inodeid, parentid, processed) 
(SELECT hopsworks.hdfs_metadata_log.inode_id, hopsworks.hdfs_metadata_log.dataset_id, 0 FROM hopsworks.hdfs_metadata_log LIMIT 100)


-- SELECT ALL PARENTS THAT HAVE BEEN ADDED (OPERATION 0) -- DB NAME MUST CHANGE TO POINT TO HOPS (for hdfs_metadata_log and hdfs_inodes)

SELECT composite.*, metadata.EXTENDED_METADATA

FROM (
	SELECT ops.inode_id as _id, inodeinn.name, inodeinn.meta_enabled, ops.* 

	FROM hopsworks.hdfs_metadata_log ops, 
		(SELECT i.id, i.name, i.meta_enabled 
		FROM hopsworks.hdfs_inodes i, 
			(SELECT inn.id AS rootid 
			FROM hops.hdfs_inodes inn 
			WHERE inn.parent_id = 1) AS temp 

		WHERE i.parent_id = temp.rootid) as inodeinn 

	WHERE ops.operation = 0 AND ops.inode_id = inodeinn.id 
	AND ops.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_parents_deleted) LIMIT 100

)as composite

LEFT JOIN(
	SELECT mtt.inodeid, GROUP_CONCAT( mrd.data SEPARATOR  '|' ) AS EXTENDED_METADATA
	FROM meta_tuple_to_file mtt, meta_raw_data mrd
	WHERE mrd.tupleid = mtt.tupleid
	GROUP BY (mtt.inodeid)
	LIMIT 0 , 30

) as metadata

ON metadata.inodeid = composite._id


-- SELECT ALL PARENTS THAT HAVE BEEN DELETED/RENAMED (OPERATION 1) -- DB NAME MUST CHANGE TO POINT TO HOPS (for hdfs_metadata_log and hdfs_inodes)


SELECT ops.inode_id as _id, ops.* 

FROM hopsworks.hdfs_metadata_log ops, 
	(SELECT inn.id AS rootid 
	FROM hopsworks.hdfs_inodes inn 
	WHERE inn.parent_id = 1) AS temp 

WHERE ops.operation = 1 AND ops.dataset_id = temp.rootid 
AND ops.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_parents_deleted)


-- UPDATE AS PROCESSED ALL PARENTS THAT HAVE BEEN PROCESSED (PROCESSED = 1) -- DB NAME MUST CHANGE TO POINT TO HOPS (for hdfs_metadata_log and hdfs_inodes)

UPDATE hopsworks.meta_inodes_ops_parents_deleted m,

	(SELECT inn.id AS rootid 
	FROM hopsworks.hdfs_inodes inn 
	WHERE inn.parent_id = 1) AS temp

SET m.processed = 1
WHERE m.parentid = temp.rootid AND m.inodeid IN (SELECT inode_id FROM hopsworks.hdfs_metadata_log)


-- DELETE ALL PARENTS THAT HAVE BEEN MARKED AS PROCESSED (PROCESSED = 1)

DELETE FROM hopsworks.meta_inodes_ops_parents_deleted WHERE processed = 1


-- DELETE ALL PROCESSED PARENTS FROM THE HDFS_METADATA_LOG TABLE -- DB NAME MUST CHANGE TO POINT TO HOPS (for hdfs_metadata_log and hdfs_inodes)

DELETE FROM hopsworks.hdfs_metadata_log WHERE inode_id NOT IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_parents_deleted)
