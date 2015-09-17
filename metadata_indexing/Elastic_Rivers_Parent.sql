-- TAKE A BATCH OF RECORDS INTO THE BUFFER TABLE (deprecated. Use the one below instead)

INSERT INTO hopsworks.meta_inodes_ops_parents_deleted (inodeid, parentid, processed) 
(SELECT hops.hdfs_metadata_log.inode_id, hops.hdfs_metadata_log.dataset_id, 0 FROM hops.hdfs_metadata_log LIMIT 100);

-- SELECTIVELY DUMP A BATCH OF PARENT RECORDS INTO THE BUFFER TABLE (picks up added and deleted records in one step)

INSERT INTO hopsworks.meta_inodes_ops_parents_deleted (inodeid, parentid, processed)

(SELECT log.inode_id, log.dataset_id, 0 as processed FROM hops.hdfs_metadata_log log, 
	(SELECT inn.id AS rootid FROM hops.hdfs_inodes inn WHERE inn.parent_id = 1) AS root 
WHERE log.dataset_id = root.rootid LIMIT 100);

-- SELECT ALL PARENTS THAT HAVE BEEN ADDED (OPERATION 0)

SELECT composite.*, "parent" as type, metadata.EXTENDED_METADATA

FROM (
	SELECT log.inode_id as _id, project.name, project.meta_enabled, log.* 
	FROM hops.hdfs_metadata_log log, 

		(SELECT i.id, i.name, i.meta_enabled 
		FROM hops.hdfs_inodes i, 

			(SELECT inn.id 
			FROM hops.hdfs_inodes inn 
			WHERE inn.parent_id = 1
			) AS root 

		WHERE i.parent_id = root.id
		) as project 

	WHERE log.operation = 0 AND log.inode_id = project.id 
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
ORDER BY composite.logical_time ASC;


-- SELECT ALL PARENTS THAT HAVE BEEN DELETED/RENAMED (OPERATION 1)


SELECT log.inode_id as _id, log.* 

FROM hops.hdfs_metadata_log log, 
	(SELECT inn.id 
	FROM hops.hdfs_inodes inn 
	WHERE inn.parent_id = 1
	) AS root 

WHERE log.operation = 1 AND log.dataset_id = root.id 
AND log.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_parents_deleted);


-- MARK AS PROCESSED ALL PARENTS THAT HAVE BEEN PROCESSED (PROCESSED = 1)

UPDATE hopsworks.meta_inodes_ops_parents_deleted m,

	(SELECT inn.id 
	FROM hops.hdfs_inodes inn 
	WHERE inn.parent_id = 1) AS root

SET m.processed = 1
WHERE m.parentid = root.id AND m.inodeid IN (SELECT inode_id FROM hops.hdfs_metadata_log);

-- DELETE ALL PROCESSED PARENTS FROM THE HDFS_METADATA_LOG TABLE

DELETE FROM hops.hdfs_metadata_log WHERE inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_parents_deleted);

-- DELETE ALL PARENTS THAT HAVE BEEN MARKED AS PROCESSED (PROCESSED = 1) (processed field can be phased out)

DELETE FROM hopsworks.meta_inodes_ops_parents_deleted WHERE processed = 1;



