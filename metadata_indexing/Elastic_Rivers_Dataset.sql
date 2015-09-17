-- TAKE A BATCH OF RECORDS INTO THE BUFFER TABLE (deprecated. Use the one below instead)

INSERT INTO hopsworks.meta_inodes_ops_datasets_deleted (inodeid, parentid, processed) 
(SELECT hops.hdfs_metadata_log.inode_id, hops.hdfs_metadata_log.dataset_id, 0 FROM hops.hdfs_metadata_log LIMIT 100);

-- SELECTIVELY DUMP A BATCH OF DATASET RECORDS INTO THE BUFFER TABLE - (picks up added and deleted records in one step)

INSERT INTO hopsworks.meta_inodes_ops_datasets_deleted (inodeid, parentid, processed) (

	SELECT log.inode_id, log.dataset_id, 0 as processed 
	FROM hops.hdfs_metadata_log log, 

		(SELECT p.inode_id as id, p.dataset_id, 0 as processed 
		FROM hops.hdfs_metadata_log p, 

			(SELECT inn.id 
			FROM hops.hdfs_inodes inn 
			WHERE inn.parent_id = 1
			) AS root 

		WHERE p.dataset_id = root.id
		) AS parent 

	WHERE log.dataset_id = parent.id LIMIT 100);


-- SELECT ALL DATASETS (FIRST LEVEL CHILDREN) THAT HAVE BEEN ADDED (OPERATION 0)

SELECT composite.*, "dataset" as type, metadata.EXTENDED_METADATA 
FROM (
			
	SELECT DISTINCT dsop.*, dt.description, dt.searchable
	FROM hopsworks.dataset dt,

		(SELECT log.inode_id as _id, log.dataset_id as parentid, dataset.name, log.* 
		FROM hops.hdfs_metadata_log log,

			(SELECT p.id as parentid, p.parent_id as parentt, p.* 
			FROM hops.hdfs_inodes p, 

				(SELECT i.id
				FROM hops.hdfs_inodes i, 

					(SELECT inn.id
					FROM hops.hdfs_inodes inn 
					WHERE inn.parent_id = 1
					) AS root 

				WHERE i.parent_id = root.id
				) AS project 

			WHERE p.parent_id = project.id
			)as dataset
	
		WHERE log.inode_id = dataset.id
		)as dsop

	WHERE dt.inode_pid = dsop.parentid AND dt.inode_name = dsop.name 
	AND dsop._id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_datasets_deleted) LIMIT 100
	)as composite

LEFT JOIN (

	SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR '|' ) AS EXTENDED_METADATA 
	FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md 
	WHERE mtt.tupleid = md.tupleid GROUP BY (mtt.inodeid) LIMIT 0 , 30

) as metadata 

ON metadata.inodeid = composite._id ORDER BY composite.parentid ASC;

-- SELECT ALL DATASETS THAT HAVE BEEN DELETED/RENAMED (OPERATION 1) AND ARE NOT YET PROCESSED
-- THEIR PARENT IS ASSUMED TO RESIDE IN THE HDFS_METADATA_LOG TABLE

SELECT log.inode_id as _id, log.dataset_id as parentid, log.logical_time, log.operation
FROM hops.hdfs_metadata_log log,

	(SELECT log.dataset_id, log.inode_id
	FROM hops.hdfs_metadata_log log, 

		(SELECT inn.id 
		FROM hops.hdfs_inodes inn 
		WHERE inn.parent_id = 1
		) AS root 

	WHERE log.dataset_id = root.id
	)AS project

WHERE log.dataset_id = project.inode_id AND log.operation = 1 
AND log.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_datasets_deleted WHERE processed = 0);


-- SELECT ALL DATASETS THAT HAVE BEEN DELETED/RENAMED (OPERATION 1) AND ARE NOT YET PROCESSED
-- THEIR PARENT IS ASSUMED TO RESIDE IN THE HDFS_INODES TABLE

SELECT log.inode_id as _id, log.dataset_id as parentid, log.logical_time, log.operation
FROM hops.hdfs_metadata_log log,

	(SELECT p.parent_id, p.id
	FROM hops.hdfs_inodes p, 

		(SELECT inn.id 
		FROM hops.hdfs_inodes inn 
		WHERE inn.parent_id = 1
		) AS root 

	WHERE p.parent_id = root.id
	)AS project

WHERE log.dataset_id = project.id AND log.operation = 1 
AND log.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_datasets_deleted WHERE processed = 0);


-- DELETE ALL PROCESSED DATASETS FROM THE HDFS_METADATA_LOG TABLE

DELETE FROM hops.hdfs_metadata_log WHERE inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_datasets_deleted);


-- MARK AS PROCESSED ALL DATASETS THAT HAVE BEEN PROCESSED (PROCESSED = 1) (deprecated)

UPDATE hopsworks.meta_inodes_ops_datasets_deleted m,

(SELECT p.id AS id FROM hops.hdfs_inodes p,

	(SELECT inn.id AS rootid 
	FROM hops.hdfs_inodes inn 
	WHERE inn.parent_id = 1) AS root

WHERE p.parent_id = root.rootid) AS parent

SET m.processed = 1
WHERE m.parentid = parent.id AND m.inodeid IN (SELECT inode_id FROM hops.hdfs_metadata_log);


-- DELETE ALL DATASETS THAT HAVE BEEN MARKED AS PROCESSED (PROCESSED = 1)

DELETE FROM hopsworks.meta_inodes_ops_datasets_deleted WHERE processed = 1;






