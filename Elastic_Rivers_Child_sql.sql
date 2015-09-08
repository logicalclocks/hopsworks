-- TAKE A BATCH OF RECORDS INTO THE BUFFER TABLE

INSERT INTO hopsworks.meta_inodes_ops_children_deleted (inodeid, parentid, processed) 
(SELECT hops.hdfs_metadata_log.inode_id, hops.hdfs_metadata_log.dataset_id, 0 FROM hops.hdfs_metadata_log LIMIT 100)


-- SELECT ALL CHILDREN THAT HAVE BEEN ADDED (OPERATION 0)

SELECT composite.*, metadata.EXTENDED_METADATA

FROM (
	SELECT DISTINCT ops.inode_id as _id, ops.dataset_id as _parent, inodeinn.name as inode_name, ops.*
	FROM hops.hdfs_metadata_log ops,

		(SELECT outt.id as nodeinn_id, outt.parent_id as _parent, outt.* 
		FROM hops.hdfs_inodes outt,

			(SELECT i.id as parentid
			FROM hops.hdfs_inodes i,

				(SELECT inn.id AS rootid
				FROM hops.hdfs_inodes inn
				WHERE inn.parent_id = 1) AS temp

			WHERE i.parent_id = temp.rootid) AS parent

		WHERE outt.parent_id = parent.parentid) as inodeinn

	WHERE ops.operation = 0 AND ops.inode_id = inodeinn.nodeinn_id 
	AND ops.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_children_deleted) LIMIT 100

)as composite

LEFT JOIN (
	SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR  '_' ) AS EXTENDED_METADATA
	FROM meta_tuple_to_file mtt, meta_data md
	WHERE mtt.tupleid = md.tupleid
	GROUP BY (mtt.inodeid)
	LIMIT 0 , 30

) as metadata

ON metadata.inodeid = composite._id


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



