
----------   ACCUMULATE ALL THE METADATA FOR AN INODE INTO ONE FIELD --------------
SELECT mtof.inodeid, GROUP_CONCAT( mrd.data SEPARATOR  '|' ) AS DATA
FROM meta_tuple_to_file mtof, meta_raw_data mrd
WHERE mrd.tupleid = mtof.tupleid
AND mtof.inodeid = 136
GROUP BY (mtof.inodeid)
LIMIT 0 , 30
-----------------------------------------------------------------------------------
"schedule": "0 0-59 0-23 ? * *",

--------------------------------- DUMP CSV INTO TABLE INODES ------------

load data local infile '/home/vangelis/inodes_100000.csv' into table inodes fields terminated by ',' (name,pid,root,modified,isDir,size,status,searchable);


---- TRANSFER RECORDS FROM ONE TABLE TO ANOTHER -----------------------------------

INSERT INTO meta_inodes_ops (inodeid, inode_pid, inode_root, modified, operationn, processed) 
(SELECT inodes.id, inodes.pid, inodes.root, inodes.modified, 1, 0 FROM inodes WHERE status = '' LIMIT 75000)

-----------------------------------------------------------------------------------

-------------- RIVERS TO DEFINE PARENT - CHILD RELATIONSHIPS --------------------------------

curl -XPUT 'localhost:9200/_river/parent/_meta' -d '{
    "type" : "jdbc",
    "jdbc" : {
        "strategy": "simple",
	"driver" : "com.mysql.jdbc.Driver",
        "url" : "jdbc:mysql://localhost:3306/kthfs",
        "user" : "kthfs",
        "password" : "kthfs",
    	"interval": "10",
        "sql" : [
{
"statement": "INSERT INTO meta_inodes_ops_parents_deleted (inodeid) (SELECT meta_inodes_ops.inodeid FROM meta_inodes_ops LIMIT 100)"
},

{
"statement": "SELECT ops.inodeid as _id, inodeinn.name, inodeinn.searchable, ops.* FROM meta_inodes_ops ops, (SELECT i.id, i.name, i.searchable FROM inodes i, (SELECT inn.id AS rootid FROM inodes inn WHERE inn.pid IS null) AS temp WHERE i.pid = temp.rootid) as inodeinn WHERE ops.operationn = 1 AND ops.inodeid = inodeinn.id AND ops.inodeid IN (SELECT inodeid FROM meta_inodes_ops_parents_deleted) LIMIT 100"
},

{
"statement": "SELECT ops.inodeid as _id, ops.* FROM meta_inodes_ops ops, (SELECT inn.id AS rootid FROM inodes inn WHERE inn.pid IS null) AS temp WHERE ops.operationn = 0 AND ops.inode_pid = temp.rootid AND ops.inodeid IN (SELECT inodeid FROM meta_inodes_ops_parents_deleted)"
},

{
"statement": "UPDATE meta_inodes_ops o, (SELECT inn.id AS rootid FROM inodes inn WHERE inn.pid IS null) AS temp SET o.processed = 1 WHERE o.inodeid IN (SELECT inodeid FROM meta_inodes_ops_parents_deleted) AND o.inode_pid = temp.rootid"
},

{
"statement" : "DELETE FROM meta_inodes_ops_parents_deleted"
}

],
        "index" : "project",
        "type" : "parent"
    }
}'

#################### CHILD MAPPING ########################

curl -XPOST 'localhost:9200/project/child/_mapping' -d '{
  "child":{
    "_parent": {"type": "parent"}
  }
}'

###########################################################

curl -XPUT 'localhost:9200/_river/child/_meta' -d '{
    "type" : "jdbc",
    "jdbc" : {
        "strategy": "simple",
	"driver" : "com.mysql.jdbc.Driver",
        "url" : "jdbc:mysql://localhost:3306/kthfs",
        "user" : "kthfs",
        "password" : "kthfs",
    	"interval": "10",
        "sql" : [
{
"statement" : "DELETE FROM meta_inodes_ops WHERE processed = 1"
},

{
"statement" : "insert into meta_river_benchmark (description, timestamp) values(\"started\", ?)",
"parameter" : ["$last.sql.start"]
},

{
"statement": "INSERT INTO meta_inodes_ops_children_deleted (inodeid) (SELECT meta_inodes_ops.inodeid FROM meta_inodes_ops LIMIT 100)"
},

{
"statement": "SELECT DISTINCT ops.inodeid as _id, ops.inode_root as _parent, inodeinn.name, inodeinn.searchable, ops.* FROM meta_inodes_ops ops, (SELECT outt.id as nodeinn_id, outt.root as _parent, outt.* FROM inodes outt, (SELECT i.id as parentid FROM inodes i, (SELECT inn.id AS rootid FROM inodes inn WHERE inn.pid IS null) AS temp WHERE i.pid = temp.rootid) AS parent WHERE outt.root = parent.parentid) as inodeinn WHERE ops.operationn = 1 AND ops.inodeid = inodeinn.nodeinn_id AND ops.inodeid IN (SELECT inodeid FROM meta_inodes_ops_children_deleted) LIMIT 100"
},

{
"statement": "SELECT ops.inodeid as _id, ops.inode_root as _parent, ops.* FROM meta_inodes_ops ops WHERE ops.operationn = 0 AND ops.processed = 0 AND ops.inodeid IN (SELECT inodeid FROM meta_inodes_ops_children_deleted)"
},

{
"statement" : "insert into meta_river_benchmark (description, timestamp) values(\"ended\", ?)",
"parameter" : ["$last.sql.end"]
},

{
"statement" : "UPDATE meta_inodes_ops o SET o.processed = 1 WHERE o.inodeid IN (SELECT inodeid FROM meta_inodes_ops_children_deleted)"
},

{
"statement" : "DELETE FROM meta_inodes_ops WHERE processed = 1"
},

{
"statement" : "DELETE FROM meta_inodes_ops_children_deleted"
}
],
        "index" : "project",
        "type" : "child"
    }
}'


---------------------------------------------------------------------------------------------


