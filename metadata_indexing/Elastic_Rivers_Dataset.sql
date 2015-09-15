-- TAKE A BATCH OF RECORDS INTO THE BUFFER TABLE

INSERT INTO hopsworks.meta_inodes_ops_datasets_deleted (inodeid, parentid, processed) 
(SELECT hops.hdfs_metadata_log.inode_id, hops.hdfs_metadata_log.dataset_id, 0 FROM hops.hdfs_metadata_log LIMIT 100)

-- SELECTIVELY DUMP TAKE A BATCH OF DATASET RECORDS INTO THE BUFFER TABLE (convenient when it comes to update the buffer table)

INSERT INTO hopsworks.meta_inodes_ops_datasets_deleted (inodeid, parentid, processed) (

	SELECT outt.id as ds_id, outt.parent_id as parentt, 0 as processed FROM hops.hdfs_inodes outt, 
		(SELECT i.id as parentid FROM hops.hdfs_inodes i, 
			(SELECT inn.id AS rootid FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root 

		WHERE i.parent_id = root.rootid) AS parent 

	WHERE outt.parent_id = parent.parentid LIMIT 100)

-- SELECT ALL DATASETS (FIRST LEVEL CHILDREN) THAT HAVE BEEN ADDED (OPERATION 0)

SELECT ds._id, ds.name, ds.inode_id, ds.dataset_id as parentid, ds.description, ds.operation, metadata.EXTENDED_METADATA 
FROM (
	SELECT DISTINCT composite.*, dt.inode_pid, dt.inode_name, dt.description 
	FROM hopsworks.dataset dt,

		(SELECT ops.inode_id as _id, ops.dataset_id as dsid, dataset.name, ops.* 
		FROM hops.hdfs_metadata_log ops,   

			(SELECT outt.id as ds_id, outt.parent_id as parentt, outt.* 
			FROM hops.hdfs_inodes outt, 

				(SELECT i.id as parentid 
				FROM hops.hdfs_inodes i, 

					(SELECT inn.id AS rootid 
					FROM hops.hdfs_inodes inn 
					WHERE inn.parent_id = 1
					) AS root 

				WHERE i.parent_id = root.rootid
				) AS parent 

			WHERE outt.parent_id = parent.parentid
			) as dataset 

		WHERE ops.operation = 0 AND ops.inode_id = dataset.ds_id 
		AND ops.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_datasets_deleted) LIMIT 100
		)as composite 

	WHERE dt.inode_pid = composite.dsid AND dt.inode_name = composite.name AND dt.searchable = 1
) as ds

LEFT JOIN (

	SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR '|' ) AS EXTENDED_METADATA 
	FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md 
	WHERE mtt.tupleid = md.tupleid GROUP BY (mtt.inodeid) LIMIT 0 , 30

) as metadata 

ON metadata.inodeid = ds._id ORDER BY parentid ASC

-- SELECT ALL DATASETS THAT HAVE BEEN DELETED/RENAMED (OPERATION 1) AND ARE NOT YET PROCESSED

SELECT ops.inode_id as _id, ops.dataset_id as _parent, ops.* 
FROM hops.hdfs_metadata_log ops

WHERE ops.operation = 1
AND ops.inode_id IN (SELECT inode_id FROM hopsworks.meta_inodes_ops_datasets_deleted WHERE processed = 0)


-- MARK AS PROCESSED ALL DATASETS THAT HAVE BEEN PROCESSED (PROCESSED = 1)

UPDATE hopsworks.meta_inodes_ops_datasets_deleted m,

(SELECT p.id AS id FROM hops.hdfs_inodes p,

	(SELECT inn.id AS rootid 
	FROM hops.hdfs_inodes inn 
	WHERE inn.parent_id = 1) AS root

WHERE p.parent_id = root.rootid) AS parent

SET m.processed = 1
WHERE m.parentid = parent.id AND m.inodeid IN (SELECT inode_id FROM hops.hdfs_metadata_log)


-- DELETE ALL DATASETS THAT HAVE BEEN MARKED AS PROCESSED (PROCESSED = 1)

DELETE FROM hopsworks.meta_inodes_ops_datasets_deleted WHERE processed = 1


-- DELETE ALL PROCESSED DATASETS FROM THE HDFS_METADATA_LOG TABLE

DELETE FROM hops.hdfs_metadata_log WHERE inode_id NOT IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_datasets_deleted)
