
-- SELECTIVELY DUMP A BATCH OF CHILDREN RECORDS INTO THE BUFFER TABLE - (picks up added and deleted records in one step)
-- PARENT IS THE PROJECT AND IS ASSUMED TO RESIDE IN METADATA_LOGS. IF THE PARENT PROJECT IS NOT PRESENT IN THE LOGS TABLE
-- (WAS INDEXED PREVIOUSLY AND REMOVED) THIS QUERY WILL FAIL TO RETRIEVE ANY CHILD FOLDERS. THE ONE BELOW TAKES CARE OF THIS CASE

INSERT INTO hopsworks.meta_inodes_ops_children_pr_buffer (inodeid, parentid, operation, logical_time, processed) (

SELECT DISTINCT log.inode_id, dataset.parent, log.operation, log.logical_time, 0 as processed
FROM hops.hdfs_metadata_log log, 

	(SELECT p.inode_id as id, project.id as parent, p.* 
	FROM hops.hdfs_metadata_log p, 

		(SELECT i.inode_id as id
		FROM hops.hdfs_metadata_log i, 

			(SELECT inn.id
			FROM hops.hdfs_inodes inn 
			WHERE inn.parent_id = 1
			) AS root 

		WHERE i.dataset_id = root.id
		) AS project 

	WHERE p.dataset_id = project.id
	)as dataset

WHERE log.dataset_id = dataset.id LIMIT 100);


-- SELECTIVELY DUMP A BATCH OF CHILDREN RECORDS INTO THE BUFFER TABLE
-- THE PARENT PROJECT IS NOT PRESENT IN METADATA_LOG TABLE (THE CASE WHERE THE INODE WAS CREATED AFTER THE PROJECT HAD BEEN INDEXED)
-- IT MAY LEAD TO DUPLICATE ROWS IN THE BUFFER TABLE IN SOME CASES, BUT IT'S A MANAGEABLE TRADEOFF

INSERT INTO hopsworks.meta_inodes_ops_children_pr_buffer (inodeid, parentid, operation, logical_time, processed) (

SELECT DISTINCT log.inode_id, dataset.parent, log.operation, log.logical_time, 0 as processed
FROM hops.hdfs_metadata_log log, 

	(SELECT p.id as datasetid, project.projectid as parent, p.* 
	FROM hops.hdfs_inodes p, 

		(SELECT i.id as projectid
		FROM hops.hdfs_inodes i, 

			(SELECT inn.id as rootid
			FROM hops.hdfs_inodes inn 
			WHERE inn.parent_id = 1
			) AS root 

		WHERE i.parent_id = root.rootid
		) AS project 

	WHERE p.parent_id = project.projectid
	)as dataset

WHERE log.dataset_id = dataset.datasetid LIMIT 100);


-- SELECT ALL CHILDREN (no datasets) THAT HAVE BEEN ADDED (OPERATION 0 - PROJECT IS THE PARENT - indexes subdirs under the dataset)

SELECT composite.*, metadata.EXTENDED_METADATA
FROM (
	SELECT DISTINCT hi.id as _id, op._parent, hi.name, mib.description, op.operation, op.logical_time, mib.searchable
	FROM hops.hdfs_inodes hi, hopsworks.meta_inode_basic_metadata mib,

		(SELECT log.inodeid as child_id, log.parentid as _parent, log.operation, log.logical_time
		FROM hopsworks.meta_inodes_ops_children_pr_buffer log
		WHERE log.operation = 0
		) as op 

	WHERE hi.id = op.child_id AND hi.parent_id = mib.inode_pid AND hi.name = mib.inode_name LIMIT 100
)as composite

LEFT JOIN (
	SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR '|' ) AS EXTENDED_METADATA
	FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md
	WHERE mtt.tupleid = md.tupleid
	GROUP BY (mtt.inodeid)
	LIMIT 0 , 30

) as metadata

ON metadata.inodeid = composite._id
ORDER BY composite.logical_time ASC;



-- SELECT ALL CHILDREN THAT HAVE BEEN DELETED/RENAMED (OPERATION 1) AND ARE NOT YET PROCESSED (DATASET IS THE PARENT)
-- THE PARENT IS ASSUMED TO RESIDE IN THE LOGS TABLE. IF THE PARENT IS ALREADY INDEXED (BUT NOT DELETED) THE QUERY WILL NOT FIND THE CORRESPONDING CHILDREN

SELECT DISTINCT c.inodeid as _id, c.parentid as _parent, c.logical_time, c.operation
FROM hopsworks.meta_inodes_ops_children_pr_buffer c
WHERE c.operation = 1 LIMIT 100;


-- MARK AS PROCESSED ALL CHILDREN THAT HAVE BEEN PROCESSED (PROCESSED = 1)

UPDATE hopsworks.meta_inodes_ops_children_pr_buffer b SET b.processed = 1;


-- DELETE ALL PROCESSED CHILDREN FROM THE HDFS_METADATA_LOG TABLE

DELETE FROM hops.hdfs_metadata_log WHERE inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_children_pr_buffer WHERE processed = 1);


-- DELETE ALL CHILDREN THAT HAVE BEEN MARKED AS PROCESSED (PROCESSED = 1)

DELETE FROM hopsworks.meta_inodes_ops_children_pr_buffer WHERE processed = 1;

