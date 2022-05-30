# High Availability

At a high level a Hopsworks cluster can be divided into 4 groups of nodes. Each node group should be deployed according to the requirements (e.g., 3/5/7 nodes for the head node group) to guarantee the availability of the components. 

* **Head nodes**: The head node is responsible for running all the metadata, public API, and user interface services that are required for Hopsworks to provide its functionality. They need to be deployed in an odd number (1, 3, 5) as the head nodes run services like Zookeeper and OpenSearch which enforce consistency through quorum based protocols. The head nodes are also responsible for managing the services running on the remaining group of nodes.
* **Worker nodes**: The worker node is responsible for executing the feature engineering pipeline code as well as storing the data for the offline feature store (HopsFS). In an on-prem deployment, the data is stored and replicated on the workers’ local hard drives. By default the data is replicated across 3 workers. In a cloud deployment, HopsFS’ data is persisted in a cloud object store (Amazon S3, Azure Blob Storage, Google Cloud Blob Storage) and the HopsFS datanodes are responsible for persisting, retrieving and caching of blocks from the object store.
* **RonDB Data nodes**: These nodes are responsible for storing the services’ metadata (Hopsworks, HopsFS, Hive Metastore, Airflow) as well as the data for the online feature store. For high availability, at least two data nodes should be deployed and RonDB is typically  configured with a replication factor of 2, as it uses synchronous replication with 2-phase commit, not a quorum-based replication protocol. More advanced deployment patterns and best practices are covered in the RonDB documentation (https://docs.rondb.ai) .
* **Query brokers**: The query brokers are the entry point for querying the online feature store. They handle authentication, authorization and execution of the requests for online feature data being submitted from the feature store APIs. At least two query brokers should be deployed to achieve high availability. Query brokers are stateless. Additional query brokers should be deployed to handle additional load and clients.

Example deployment:

<figure>
  <a href="../../../assets/images/admin/ha_dr/example_ha_cluster.svg">
    <img width="800px" src="../../../assets/images/admin/ha_dr/example_ha_cluster.svg" alt="Example HA deployment"/>
  </a>
  <figcaption>Example High Available deployment</figcaption>
</figure>

For higher availability, a Hopsworks cluster should be deployed across multiple availability zones, however, a single cluster cannot be deployed across multiple regions. Multiple region deployments are out of the scope of this guide.

A different service placement is also possible, e.g., separating RonDB data nodes between metadata and online feature store or adding more replicas of a metadata service without necessarily adding a whole new head node, however, this is outside the scope of this guide.
