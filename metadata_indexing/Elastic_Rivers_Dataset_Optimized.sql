-- SELECTIVELY DUMP A BATCH OF DATASET RECORDS INTO THE BUFFER TABLE - (picks up added and deleted records in one step)
-- ASSUMES DATASET PARENTS (PROJECTS) ARE RESIDING IN METADATA_LOG TABLE

INSERT INTO hopsworks.meta_inodes_ops_datasets_buffer (inodeid, parentid, operation, logical_time, processed) (

	SELECT log.inode_id, log.dataset_id, log.operation, log.logical_time, 0 as processed 
	FROM hops.hdfs_metadata_log log, 

		(SELECT p.inode_id as id, p.dataset_id, 0 as processed 
		FROM hops.hdfs_metadata_log p, 

			(SELECT inn.id 
			FROM hops.hdfs_inodes inn 
			WHERE inn.parent_id = 1
			) AS root 

		WHERE p.dataset_id = root.id
		) AS project 

	WHERE log.dataset_id = project.id LIMIT 100);


-- SELECTIVELY DUMP A BATCH OF DATASET RECORDS INTO THE BUFFER TABLE - (only picks up added records)
-- THE PARENT IS NOT PRESENT IN METADATA_LOG TABLE (THE CASE WHERE THE DATASET WAS CREATED AFTER THE PARENT HAD BEEN INDEXED)
-- IT MAY LEAD TO DUPLICATE ROWS IN THE BUFFER TABLE IN SOME CASES, BUT IT'S A MANAGEABLE TRADEOFF

INSERT INTO hopsworks.meta_inodes_ops_datasets_buffer (inodeid, parentid, operation, logical_time, processed) (

	SELECT log.inode_id, log.dataset_id, log.operation, log.logical_time, 0 as processed 
	FROM hops.hdfs_metadata_log log, 

		(SELECT p.id, p.parent_id, 0 as processed 
		FROM hops.hdfs_inodes p, 

			(SELECT inn.id 
			FROM hops.hdfs_inodes inn 
			WHERE inn.parent_id = 1
			) AS root 

		WHERE p.parent_id = root.id
		) AS project 

	WHERE log.dataset_id = project.id LIMIT 100);



-- *** SELECT ALL DATASETS THAT HAVE BEEN ADDED (OPERATION 0. Uses the buffer table. Reduces access to metadata_log table and the joins in inodes table) ***

SELECT composite.*, metadata.EXTENDED_METADATA 
FROM (
			
	SELECT DISTINCT dataset._id, dataset.name, dataset.parentid, dt.description, dataset.operation, dataset.logical_time, dt.searchable
	FROM hopsworks.dataset dt,

		(SELECT i.id, i.name, ds.*
		FROM hops.hdfs_inodes i,
	
			(SELECT log.inodeid as _id, log.parentid, log.operation, log.logical_time 
			FROM hopsworks.meta_inodes_ops_datasets_buffer log
			WHERE log.operation = 0
			)as ds
	
		WHERE i.id = ds._id
		)AS dataset

	WHERE dt.inode_pid = dataset.parentid AND dt.inode_name = dataset.name LIMIT 100
	)as composite

LEFT JOIN (

	SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR '|' ) AS EXTENDED_METADATA 
	FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md 
	WHERE mtt.tupleid = md.tupleid GROUP BY (mtt.inodeid) LIMIT 0 , 30

) as metadata 

ON metadata.inodeid = composite._id ORDER BY composite.parentid ASC;


-- SELECT ALL DATASETS THAT HAVE BEEN DELETED/RENAMED (OPERATION 1)
-- THEIR PARENTS RESIDE IN THE BUFFER TABLE

SELECT DISTINCT log.inodeid as _id, log.parentid, log.logical_time, log.operation
FROM  hopsworks.meta_inodes_ops_datasets_buffer log
WHERE log.operation = 1;


-- MARK AS PROCESSED ALL DATASETS THAT HAVE BEEN PROCESSED (PROCESSED = 1)

UPDATE hopsworks.meta_inodes_ops_datasets_buffer m SET m.processed = 1;


-- DELETE ALL PROCESSED DATASETS FROM THE HDFS_METADATA_LOG TABLE

DELETE FROM hops.hdfs_metadata_log WHERE inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_datasets_buffer);


-- DELETE ALL DATASETS THAT HAVE BEEN MARKED AS PROCESSED (PROCESSED = 1)

DELETE FROM hopsworks.meta_inodes_ops_datasets_buffer WHERE processed = 1;






