-- TAKE A BATCH OF RECORDS BLINDLY INTO THE BUFFER TABLE (deprecated. Use the one below instead)

INSERT INTO hopsworks.meta_inodes_ops_children_deleted (inodeid, parentid, processed) 
(SELECT hops.hdfs_metadata_log.inode_id, hops.hdfs_metadata_log.dataset_id, 0 FROM hops.hdfs_metadata_log LIMIT 100);


-- SELECTIVELY DUMP A BATCH OF CHILDREN RECORDS INTO THE BUFFER TABLE - (picks up added and deleted records in one step)

INSERT INTO hopsworks.meta_inodes_ops_children_deleted (inodeid, parentid, processed) (

SELECT DISTINCT ml.inode_id as _id, ml.dataset_id, 0 as processed
FROM hops.hdfs_metadata_log ml,

	(SELECT log.inode_id as inodeid, log.dataset_id as parentid
	FROM hops.hdfs_metadata_log log, 

		(SELECT p.inode_id as id, p.dataset_id as parentt, p.* 
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

	WHERE log.dataset_id = dataset.id
	)as child

WHERE ml.inode_id = child.inodeid LIMIT 100);


-- SELECT ALL CHILDREN (no datasets) THAT HAVE BEEN ADDED (OPERATION 0 - returns dataset id as a parent - indexes subdirs under the dataset)

SELECT composite.*, "child" as type, metadata.EXTENDED_METADATA
FROM (
	SELECT DISTINCT hi.id as _id, op._parent, hi.name, op.operation, op.logical_time
	FROM hops.hdfs_inodes hi,  

		(SELECT log.inode_id as child_id, log.dataset_id as _parent, log.operation, log.logical_time
		FROM hops.hdfs_metadata_log log, 

			(SELECT c.parent_id
			FROM hops.hdfs_inodes c,

				(SELECT d.id 
				FROM hops.hdfs_inodes d, 

					(SELECT p.id 
					FROM hops.hdfs_inodes p, 

						(SELECT r.id 
						FROM hops.hdfs_inodes r 
						WHERE r.parent_id = 1
						) AS root 

					WHERE p.parent_id = root.id
					) AS project 

				WHERE d.parent_id = project.id
				) AS dataset

			WHERE c.parent_id = dataset.id
			) AS child

		WHERE log.dataset_id = child.parent_id AND log.operation = 0
		) as op 

	WHERE hi.id = op.child_id
	AND hi.id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_children_deleted) LIMIT 100
)as composite

LEFT JOIN (
	SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR  '|' ) AS EXTENDED_METADATA
	FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md
	WHERE mtt.tupleid = md.tupleid
	GROUP BY (mtt.inodeid)
	LIMIT 0 , 30

) as metadata

ON metadata.inodeid = composite._id
ORDER BY composite.logical_time ASC;


-- SELECT ALL CHILDREN (no datasets) THAT HAVE BEEN ADDED (OPERATION 0 - returns project id as a parent - indexes subdirs under the project)

SELECT composite.*, "child" as type, metadata.EXTENDED_METADATA
FROM (
	SELECT DISTINCT hi.id as _id, op._parent, hi.name, op.operation, op.logical_time
	FROM hops.hdfs_inodes hi,  

		(SELECT log.inode_id as child_id, child.parent as _parent, log.operation, log.logical_time
		FROM hops.hdfs_metadata_log log, 

			(SELECT c.parent_id, dataset.parent as parent
			FROM hops.hdfs_inodes c,

				(SELECT d.id, project.projectid as parent
				FROM hops.hdfs_inodes d, 

					(SELECT p.id as projectid 
					FROM hops.hdfs_inodes p, 

						(SELECT r.id 
						FROM hops.hdfs_inodes r 
						WHERE r.parent_id = 1
						) AS root 

					WHERE p.parent_id = root.id
					) AS project 

				WHERE d.parent_id = project.projectid
				) AS dataset

			WHERE c.parent_id = dataset.id
			) AS child

		WHERE log.dataset_id = child.parent_id AND log.operation = 0
		) as op 

	WHERE hi.id = op.child_id
	AND hi.id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_children_deleted) LIMIT 100
)as composite

LEFT JOIN (
	SELECT mtt.inodeid, GROUP_CONCAT( md.data SEPARATOR  '|' ) AS EXTENDED_METADATA
	FROM hopsworks.meta_tuple_to_file mtt, hopsworks.meta_data md
	WHERE mtt.tupleid = md.tupleid
	GROUP BY (mtt.inodeid)
	LIMIT 0 , 30

) as metadata

ON metadata.inodeid = composite._id
ORDER BY composite.logical_time ASC;


-- SELECT ALL CHILDREN THAT HAVE BEEN DELETED/RENAMED (OPERATION 1) AND ARE NOT YET PROCESSED (returns dataset id as a parent)
-- THE PARENT IS ASSUMED TO RESIDE IN THE LOGS TABLE. IF THE PARENT IS ALREADY INDEXED (BUT NOT DELETED) THE QUERY WILL NOT FIND THE CORRESPONDING CHILDREN

SELECT c.inode_id as _id, c.dataset_id as _parent, c.logical_time, c.operation
FROM hops.hdfs_metadata_log c,

	(SELECT d.dataset_id, d.inode_id
	FROM hops.hdfs_metadata_log d,

		(SELECT log.dataset_id, log.inode_id
		FROM hops.hdfs_metadata_log log, 

			(SELECT inn.id 
			FROM hops.hdfs_inodes inn 
			WHERE inn.parent_id = 1
			) AS root 

		WHERE log.dataset_id = root.id
		)AS project

	WHERE d.dataset_id = project.inode_id
	) AS dataset

WHERE c.dataset_id = dataset.inode_id AND c.operation = 1 
AND c.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_children_deleted WHERE processed = 0);


-- SELECT ALL CHILDREN THAT HAVE BEEN DELETED/RENAMED (OPERATION 1) AND ARE NOT YET PROCESSED (returns project id as a parent)
-- THE PARENT IS ASSUMED TO RESIDE IN THE LOGS TABLE. IF THE PARENT IS ALREADY INDEXED (BUT NOT DELETED) THE QUERY WILL NOT FIND THE CORRESPONDING CHILDREN.

SELECT c.inode_id as _id, dataset.parent as _parent, c.logical_time, c.operation
FROM hops.hdfs_metadata_log c,

	(SELECT d.dataset_id, d.inode_id, project.projectid as parent
	FROM hops.hdfs_metadata_log d,

		(SELECT log.dataset_id, log.inode_id as projectid
		FROM hops.hdfs_metadata_log log, 

			(SELECT inn.id 
			FROM hops.hdfs_inodes inn 
			WHERE inn.parent_id = 1
			) AS root 

		WHERE log.dataset_id = root.id
		)AS project

	WHERE d.dataset_id = project.projectid
	) AS dataset

WHERE c.dataset_id = dataset.inode_id AND c.operation = 1 
AND c.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_children_deleted WHERE processed = 0);


-- SELECT ALL CHILDREN THAT HAVE BEEN DELETED/RENAMED (OPERATION 1) AND ARE NOT YET PROCESSED (returns dataset id as a parent)
-- THE PARENT IS NOT DELETED AND RESIDES IN THE HDFS_INODES TABLE.

SELECT c.inode_id as _id, c.dataset_id as _parent, c.logical_time, c.operation
FROM hops.hdfs_metadata_log c,

	(SELECT d.parent_id, d.id, project.projectid as parent
	FROM hops.hdfs_inodes d,

		(SELECT p.parent_id, p.id as projectid
		FROM hops.hdfs_inodes p, 

			(SELECT inn.id 
			FROM hops.hdfs_inodes inn 
			WHERE inn.parent_id = 1
			) AS root 

		WHERE p.parent_id = root.id
		)AS project

	WHERE d.parent_id = project.projectid
	) AS dataset

WHERE c.dataset_id = dataset.id AND c.operation = 1 
AND c.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_children_deleted WHERE processed = 0);


-- SELECT ALL CHILDREN THAT HAVE BEEN DELETED/RENAMED (OPERATION 1) AND ARE NOT YET PROCESSED (returns project id as a parent)
-- THE PARENT IS NOT DELETED AND RESIDES IN THE HDFS_INODES TABLE.

SELECT c.inode_id as _id, dataset.parent as _parent, c.logical_time, c.operation
FROM hops.hdfs_metadata_log c,

	(SELECT d.parent_id, d.id, project.projectid as parent
	FROM hops.hdfs_inodes d,

		(SELECT p.parent_id, p.id as projectid
		FROM hops.hdfs_inodes p, 

			(SELECT inn.id 
			FROM hops.hdfs_inodes inn 
			WHERE inn.parent_id = 1
			) AS root 

		WHERE p.parent_id = root.id
		)AS project

	WHERE d.parent_id = project.projectid
	) AS dataset

WHERE c.dataset_id = dataset.id AND c.operation = 1 
AND c.inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_children_deleted WHERE processed = 0);


-- DELETE ALL PROCESSED CHILDREN FROM THE HDFS_METADATA_LOG TABLE

DELETE FROM hops.hdfs_metadata_log WHERE inode_id IN (SELECT inodeid FROM hopsworks.meta_inodes_ops_children_deleted);


-- MARK AS PROCESSED ALL CHILDREN THAT HAVE BEEN PROCESSED (PROCESSED = 1 - deprecated)

UPDATE hopsworks.meta_inodes_ops_children_deleted m 
SET m.processed = 1 AND m.inodeid IN (SELECT inode_id FROM hops.hdfs_metadata_log)


-- DELETE ALL CHILDREN THAT HAVE BEEN MARKED AS PROCESSED (PROCESSED = 1)

DELETE FROM hopsworks.meta_inodes_ops_children_deleted WHERE processed = 1;






