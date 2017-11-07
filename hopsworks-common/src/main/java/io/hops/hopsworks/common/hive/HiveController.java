package io.hops.hopsworks.common.hive;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetType;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.BaseHadoopClientsService;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless(name = "HiveController")
public class HiveController {

  @EJB
  private Settings settings;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private BaseHadoopClientsService bhcs;


  private final static String driver = "org.apache.hive.jdbc.HiveDriver";
  private final static Logger logger = Logger.getLogger(HiveController.class.getName());

  private Connection conn;
  private String jdbcString = null;

  @PostConstruct
  public void init() {
    try {
      // Load Hive JDBC Driver
      Class.forName(driver);

      // Create connection url
      String hiveEndpoint = settings.getHiveServerHostName(false);
      jdbcString = "jdbc:hive2://" + hiveEndpoint + "/default;" +
          "auth=noSasl;ssl=true;twoWay=true;" +
          "sslTrustStore=" + bhcs.getSuperTrustStorePath() + ";" +
          "trustStorePassword=" + bhcs.getSuperTrustStorePassword() + ";" +
          "sslKeyStore=" + bhcs.getSuperKeystorePath() + ";" +
          "keyStorePassword=" + bhcs.getSuperKeystorePassword();

      // Create connection
      initConnection();
    } catch (ClassNotFoundException | SQLException e) {
      logger.log(Level.SEVERE, "Error opening Hive JDBC connection: " +
        e);
    }
  }

  private void initConnection() throws SQLException{
    conn = DriverManager.getConnection(jdbcString);
  }

  @PreDestroy
  public void close() {
    try {
      if (conn != null && !conn.isClosed()){
        conn.close();
      }
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Error closing Hive JDBC connection: " +
        e);
    }
  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void createDatabase(Project project, Users user, DistributedFileSystemOps dfso)
      throws SQLException, IOException {
    if (conn == null || conn.isClosed()) {
      initConnection();
    }

    Statement stmt = null;
    try {
      // Create database
      stmt = conn.createStatement();
      // Project name cannot include any spacial character or space.
      stmt.executeUpdate("create database " + project.getName());
    } finally {
      if (stmt != null) {
        stmt.close();
      }
    }

    // Hive database names are case insensitive and lower case
    Path dbPath = getDbPath(project.getName());
    Inode dbInode = inodeFacade.getInodeAtPath(dbPath.toString());

    // Persist Hive db as dataset in the HopsWorks database
    Dataset dbDataset = new Dataset(dbInode, project);
    dbDataset.setType(DatasetType.HIVEDB);
    // As we are running Zeppelin as projectGenericUser, we have to make
    // the directory editable by default
    dbDataset.setEditable(true);
    dbDataset.setDescription(buildDescription(project.getName()));
    datasetFacade.persistDataset(dbDataset);

    try {
      // Assign database directory to the user and project group
      hdfsUsersBean.addDatasetUsersGroups(user, project, dbDataset, dfso);

      // Make the dataset editable by default
      FsPermission fsPermission = new FsPermission(FsAction.ALL, FsAction.ALL,
          FsAction.NONE, true);
      dfso.setPermission(dbPath, fsPermission);

      // Set the default quota
      dfso.setHdfsSpaceQuotaInMBs(dbPath, settings.getHiveDbDefaultQuota());
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Cannot assign Hive database directory " + dbPath.toString() +
          " to correct user/group. Trace: " + e);

      // Remove the database directory and cleanup the metadata
      try {
        dfso.rm(dbPath, true);
      } catch (IOException rmEx) {
        // Nothing we can really do here
        logger.log(Level.SEVERE, "Cannot delete Hive database directory: " + dbPath.toString() +
          " Trace: " + rmEx);
      }

      throw new IOException(e);
    }
  }

  public void dropDatabase(Project project, DistributedFileSystemOps dfso)
      throws IOException {
    // To avoid case sensitive bugs, check if the project has a Hive database
    Dataset ds = datasetFacade.findByNameAndProjectId(project, project.getName().toLowerCase() + ".db");
    if (ds == null || ds.getType() != DatasetType.HIVEDB)  {
      return;
    }

    // Delete HopsFs db directory -- will automatically clean up all the related Hive's metadata
    dfso.rm(getDbPath(project.getName()), true);

    // Delete all the scratchdirs
    for (HdfsUsers u : hdfsUsersBean.getAllProjectHdfsUsers(project.getName())) {
      dfso.rm(new Path(settings.getHiveScratchdir(), u.getName()), true);
    }
  }

  public Path getDbPath(String projectName) {
    return new Path(settings.getHiveWarehouse(), projectName.toLowerCase() + ".db");
  }

  private String buildDescription(String projectName) {
    return "Use the following configuration settings to connect to Hive from external clients:<br>" +
        "Url: jdbc:hive2://" + settings.getHiveServerHostName(true) + "/" + projectName + "<br>" +
        "Authentication: noSasl<br>" +
        "SSL: enabled - TrustStore and its password<br>" +
        "Username: your HopsWorks email address<br>" +
        "Password: your HopsWorks password";
  }


}
