-- SELECTIVELY DUMP A BATCH OF PARENT RECORDS INTO THE BUFFER TABLE (picks up added and deleted records in one step)

INSERT INTO hopsworks.meta_inodes_ops_parents_buffer (inodeid, parentid, operation, logical_time, processed) (

	SELECT log.inode_id, log.dataset_id, log.operation, log.logical_time, 0 as processed 
	FROM hops.hdfs_metadata_log log, 

		(SELECT inn.id AS rootid 
		FROM hops.hdfs_inodes inn 
		WHERE inn.parent_id = 1
		) AS root 

	WHERE log.dataset_id = root.rootid LIMIT 100);


-- *** SELECT ALL PARENTS THAT HAVE BEEN ADDED (OPERATION 0. Uses the buffer table. Reduces access to metadata_log table and the joins in inodes table) ***

SELECT composite.*, metadata.EXTENDED_METADATA
FROM 
	(SELECT DISTINCT i.id as _id, i.name, project.inodeid, project.parentid, project.operation, project.logical_time 
	FROM hops.hdfs_inodes i, 

		(SELECT log.inodeid, log.parentid, log.operation, log.logical_time
		FROM hopsworks.meta_inodes_ops_parents_buffer log
		WHERE log.operation = 0
		)AS project

	WHERE i.id = project.inodeid LIMIT 100

)as composite

LEFT JOIN(
	SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR '|' ) AS EXTENDED_METADATA
	FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md
	WHERE mtt.tupleid = md.tupleid
	GROUP BY (mtt.inodeid)
	LIMIT 0 , 30

) as metadata

ON metadata.inodeid = composite._id
ORDER BY composite.logical_time ASC;


-- SELECT ALL PARENTS THAT HAVE BEEN DELETED/RENAMED (OPERATION 1)

SELECT DISTINCT log.inodeid as _id, log.parentid, log.operation, log.logical_time 
FROM hopsworks.meta_inodes_ops_parents_buffer log
WHERE log.operation = 1;


-- MARK AS PROCESSED ALL PARENTS THAT HAVE BEEN PROCESSED (PROCESSED = 1)

UPDATE hopsworks.meta_inodes_ops_parents_buffer m SET m.processed = 1;


-- DELETE ALL PROCESSED PARENTS FROM THE HDFS_METADATA_LOG TABLE

DELETE FROM hops.hdfs_metadata_log WHERE inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_parents_buffer AND processed = 1 );


-- DELETE ALL PARENTS THAT HAVE BEEN MARKED AS PROCESSED (PROCESSED = 1)

DELETE FROM hopsworks.meta_inodes_ops_parents_buffer WHERE processed = 1;
