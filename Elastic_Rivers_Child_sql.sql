-- TAKE A BATCH OF RECORDS INTO THE BUFFER TABLE -- DB NAME MUST CHANGE TO POINT TO HOPS (for hdfs_metadata_log and hdfs_inodes)

INSERT INTO hopsworks.meta_inodes_ops_children_deleted (inodeid, parentid, processed) 
(SELECT hopsworks.hdfs_metadata_log.inode_id, hopsworks.hdfs_metadata_log.dataset_id, 0 FROM hopsworks.hdfs_metadata_log LIMIT 100)


-- SELECT ALL CHILDREN THAT HAVE BEEN ADDED (OPERATION 0) -- DB NAME MUST CHANGE TO POINT TO HOPS (for hdfs_metadata_log and hdfs_inodes)

SELECT DISTINCT ops.inode_id as _id, ops.dataset_id as _parent, inodeinn.name, ops.* 
FROM hopsworks.hdfs_metadata_log ops,

	(SELECT outt.id as nodeinn_id, outt.parent_id as _parent, outt.* 
	FROM hopsworks.hdfs_inodes outt,

		(SELECT i.id as parentid
		FROM hopsworks.hdfs_inodes i,

			(SELECT inn.id AS rootid
			FROM hopsworks.hdfs_inodes inn
			WHERE inn.parent_id = 1) AS temp

		WHERE i.parent_id = temp.rootid) AS parent

	WHERE outt.parent_id = parent.parentid) as inodeinn

WHERE ops.operation = 0 AND ops.inode_id = inodeinn.nodeinn_id 
AND ops.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_children_deleted) LIMIT 100


-- SELECT ALL CHILDREN THAT HAVE BEEN DELETED/RENAMED (OPERATION 1) AND ARE NOT YET PROCESSED -- DB NAME MUST CHANGE TO POINT TO HOPS (for hdfs_metadata_log and hdfs_inodes)

SELECT ops.inode_id as _id, ops.dataset_id as _parent, ops.* 
FROM hopsworks.hdfs_metadata_log ops

WHERE ops.operation = 1
AND ops.inode_id IN (SELECT inode_id FROM hopsworks.meta_inodes_ops_children_deleted WHERE processed = 0)


-- UPDATE AS PROCESSED ALL CHILDREN THAT HAVE BEEN PROCESSED (PROCESSED = 1) -- DB NAME MUST CHANGE TO POINT TO HOPS (for hdfs_metadata_log and hdfs_inodes)

UPDATE hopsworks.meta_inodes_ops_children_deleted m,

(SELECT p.id AS id FROM hopsworks.hdfs_inodes p,

	(SELECT inn.id AS rootid 
	FROM hopsworks.hdfs_inodes inn 
	WHERE inn.parent_id = 1) AS root

WHERE p.parent_id = root.rootid) AS parent

SET m.processed = 1
WHERE m.parentid = parent.id AND m.inodeid IN (SELECT inode_id FROM hopsworks.hdfs_metadata_log)


-- DELETE ALL CHILDREN THAT HAVE BEEN MARKED AS PROCESSED (PROCESSED = 1)

DELETE FROM hopsworks.meta_inodes_ops_children_deleted WHERE processed = 1


-- DELETE ALL PROCESSED CHILDREN FROM THE HDFS_METADATA_LOG TABLE -- DB NAME MUST CHANGE TO POINT TO HOPS (for hdfs_metadata_log and hdfs_inodes)

DELETE FROM hopsworks.hdfs_metadata_log WHERE inode_id NOT IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_children_deleted)



