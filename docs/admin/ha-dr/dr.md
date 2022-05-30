# Disaster Recovery

## Backup
The state of the Hopsworks cluster is divided into data and metadata and distributed across the different node groups. This section of the guide allows you to take a consistent backup between data in the offline and online feature store as well as the metadata.

The following services contain critical state that should be backed up:

* **RonDB**: as mentioned above, the RonDB is used by Hopsworks to store the cluster metadata as well as the data for the online feature store.
* **HopsFS**: HopsFS stores the data for the batch feature store as well as checkpoints and logs for feature engineering applications.

Backing up service/application metrics and services/applications logs are out of the scope of this guide. By default metrics and logs are rotated after 7 days. Application logs are available on HopsFS when the application has finished and, as such, are backed up with the rest of HopsFS’ data.

Apache Kafka and OpenSearch are additional services maintaining state. The OpenSearch metadata can be reconstructed from the metadata stored on RonDB.

Apache Kafka is used in Hopsworks to store the in-flight data that is on its way to the online feature store. In the event of a total loss of the cluster, running jobs with inflight data will have to be replayed.

### Configuration Backup

Hopsworks adopts an Infrastructure-as-code philosophy, as such all the configuration files for the different Hopsworks services are generated during the deployment phase. Cluster-specific customizations should be centralized in the cluster definition used to deploy the cluster. As such the cluster definition should be backed up (e.g., by committing it to a git repository) to be able to recreate the same cluster in case it needs to be recreated.

### RonDB Backup

The RonDB backup is divided into two parts: user and privileges backup and data backup.

To take the backup of users and privileges you can run the following command from any of the nodes in the head node group. This command generates a SQL file containing all the user definitions for both the metadata services (Hopsworks, HopsFS, Metastore) as well as the user and permission grants for the online feature store. This command needs to be run as user ‘mysql’ or with sudo privileges.

```sh
/srv/hops/mysql/bin/mysqlpump -S /srv/hops/mysql-cluster/mysql.sock --exclude-databases=% --exclude-users=root,mysql.sys,mysql.session,mysql.infoschema --users > users.sql
```

The second step is to trigger the backup of the data. This can be achieved by running the following command as user ‘mysql’ on one of the nodes of the head node group. 

```sh
/srv/hops/mysql-cluster/ndb/scripts/mgm-client.sh -e "START BACKUP [replace_backup_id] SNAPSHOTEND WAIT COMPLETED"
```

The backup ID is an integer greater or equal than 1. The script uses the following: `$(date +'%y%m%d%H%M')` instead of an integer as backup id to make it easier to identify backups over time.

The command instructs each RonDB datanode to backup the data it is responsible for. The backup will be located locally on each datanode under the following path: 

```sh
/srv/hops/mysql-cluster/ndb/backups/BACKUP - the directory name will be BACKUP-[backup_id] 
```

A more comprehensive backup script is available [here](https://github.com/logicalclocks/ndb-chef/blob/master/templates/default/native_ndb_backup.sh.erb) - The script includes the steps above as well as collecting all the partial RonDB backups on a single node. The script is a good starting point and can be adapted to ship the database backup outside the cluster.

### HopsFS Backup

HopsFS is a distributed file system based on Apache HDFS. HopsFS stores its metadata in RonDB, as such metadata backup has already been discussed in the section above. The data is stored in the form of blocks on the different data nodes. 
For availability reasons, the blocks are replicated across three different data nodes.

Within a node, the blocks are stored by default under the following directory, under the ownership of the ‘hdfs’ user: 

```sh
/srv/hopsworks-data/hops/hopsdata/hdfs/dn/
```

To safely backup all the data, a copy of all the datanodes should be taken. As the data is replicated across the different nodes, excluding a set of nodes might result in data loss.

Additionally, as HopsFS blocks are files on the file system and the filesystem can be quite large, the backup is not transactional. Consistency is dictated by the metadata. Blocks being added during the copying process will not be visible when restoring as they are not part of the metadata backup taken prior to cloning the HopsFS blocks.

When the HopsFS data blocks are stored in a cloud block storage, for example, Amazon S3, then it is sufficient to only backup the metadata. The blob cloud storage service will ensure durability of the data blocks.

## Restore

As with the backup phase, the restore operation is broken down in different steps.

### Cluster deployment

The first step to redeploy the cluster is to redeploy the binaries and configuration. You should reuse the same cluster definition used to deploy the first (original) cluster. This will re-create the same cluster with the same configuration.

### RonDB restore

The deployment step above created a functioning empty cluster. To restore the cluster, the first step is to restore the metadata and online feature store data stored on RonDB. 
To restore the state of RonDB, we first need to restore its schemas and tables, then its data, rebuild the indices, and finally restore the users and grants.

#### Restore RonDB schemas and tables

This command should be executed on one of the nodes in the head node group and is going to recreate the schemas, tables, and internal RonDB metadata. In the command below, you should replace the node_id with the id of the node you are running the command on, backup_id with the id of the backup you want to restore. Finally, you should replace the mgm_node_ip with the address of the node where the RonDB management service is running.

```sh
/srv/hops/mysql/bin/ndb_restore -n [node_id] -b [backup_id] -m --disable-indexes --ndb-connectstring=[mgm_node_ip]:1186 --backup_path=/srv/hops/mysql-cluster/ndb/backups/BACKUP/BACKUP-[backup_id]
```

#### Restore RonDB data

This command should be executed on all the RonDB datanodes. Each command should be customized with the node id of the node you are trying to restore (i.e., replace the node_id). As for the command above you should replace the backup_id and mgm_node_ip.

```sh
/srv/hops/mysql/bin/ndb_restore -n [node_id] -b [backup_id] -r --ndb-connectstring=[mgm_node_ip]:1186 --backup_path=/srv/hops/mysql-cluster/ndb/backups/BACKUP/BACKUP-[backup_id]
```

#### Rebuild the indices

In the first command we disable the indices for recovery. This last command will take care of enabling them again. This command needs to run only once on one of the nodes of the head node group. As for the commands above, you should replace node_id, backup_id and mgm_node_id.

```sh
/srv/hops/mysql/bin/ndb_restore -n [node_id] -b [backup_id] --rebuild-indexes --ndb-connectstring=[mgm_node_ip]:1186 --backup_path=/srv/hops/mysql-cluster/ndb/backups/BACKUP/BACKUP-[backup_ip]
```

#### Restore Users and Grants

In the backup phase, we took the backup of the user and grants separately. The last step of the RonDB restore process is to re-create all the users and grants both for Hopsworks services as well as for the online feature store users. This can be achieved by running the following command on one node of the head node group:

```sh
/srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh source users.sql
```

### HopsFS restore

With the metadata restored, you can now proceed to restore the file system blocks on HopsFS and restart the file system. When starting the datanode, it will advertise it’s ID/ClusterID and Storage ID based on the VERSION file that can be found in this directory:
 
```sh
/srv/hopsworks-data/hops/hopsdata/hdfs/dn/current
```

It’s important that all the datanodes are restored and they report their block to the namenodes processes running on the head nodes. By default the namenodes in HopsFS will exit “SAFE MODE” (i.e., the mode that allows only read operations) only when the datanodes have reported 99.9% of the blocks the namenodes have in the metadata.  As such, the namenodes will not resume operations until all the file blocks have been restored.

### OpenSearch state rebuild

The OpenSearch state can be rebuilt using the Hopsworks metadata stored on RonDB. The rebuild process is done by using the re-indexing mechanism provided by ePipe.
The re-indexing can be triggered by running the following command on the head node where ePipe is running:

```sh
/srv/hops/epipe/bin/reindex-epipe.sh
```

The script is deployed and configured during the platform deployment.

### Kafka topics rebuild

The backup and restore plan doesn’t cover the data in transit in Kafka, for which the jobs producing it will have to be replayed. However, the RonDB backup contains the information necessary to recreate the topics of all the feature groups. 
You can run the following command, as super user, to recreate all the topics with the correct partitioning and replication factors:

```sh
/srv/hops/kafka/bin/kafka-restore.sh
```

The script is deployed and configured during the platform deployment.