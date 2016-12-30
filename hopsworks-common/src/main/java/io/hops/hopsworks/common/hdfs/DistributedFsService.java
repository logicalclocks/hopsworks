package io.hops.hopsworks.common.hdfs;

import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptorsFacade;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsGroups;
import io.hops.hopsworks.common.util.Settings;

@Stateless
public class DistributedFsService {

  private static final Logger logger = Logger.getLogger(
          DistributedFsService.class.
          getName());

  @EJB
  private Settings settings;
  @EJB
  private InodeFacade inodes;
  @EJB
  private UserGroupInformationService ugiService;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsLeDescriptorsFacade hdfsLeDescriptorsFacade;
  
  private Configuration conf;
  private String hadoopConfDir;

  public DistributedFsService() {
  }

  @PostConstruct
  public void init() {
    System.setProperty("hadoop.home.dir", settings.getHadoopDir());
    hadoopConfDir = settings.getHadoopConfDir();
    //Get the configuration file at found path
    File hadoopConfFile = new File(hadoopConfDir, "core-site.xml");
    if (!hadoopConfFile.exists()) {
      logger.log(Level.SEVERE, "Unable to locate configuration file in {0}",
              hadoopConfFile);
      throw new IllegalStateException("No hadoop conf file: core-site.xml");
    }

    File yarnConfFile = new File(hadoopConfDir, "yarn-site.xml");
    if (!yarnConfFile.exists()) {
      logger.log(Level.SEVERE, "Unable to locate configuration file in {0}",
              yarnConfFile);
      throw new IllegalStateException("No yarn conf file: yarn-site.xml");
    }

    File hdfsConfFile = new File(hadoopConfDir, "hdfs-site.xml");
    if (!hdfsConfFile.exists()) {
      logger.log(Level.SEVERE, "Unable to locate configuration file in {0}",
              hdfsConfFile);
      throw new IllegalStateException("No hdfs conf file: hdfs-site.xml");
    }

    //Set the Configuration object for the hdfs client
    Path yarnPath = new Path(yarnConfFile.getAbsolutePath());
    Path hdfsPath = new Path(hdfsConfFile.getAbsolutePath());
    Path hadoopPath = new Path(hadoopConfFile.getAbsolutePath());
    conf = new Configuration();
    conf.addResource(hadoopPath);
    conf.addResource(yarnPath);
    conf.addResource(hdfsPath);
    conf.set("fs.permissions.umask-mode", "000");
    conf.setStrings("dfs.namenode.rpc-address", hdfsLeDescriptorsFacade.getSingleEndpoint());
//    conf.setStrings("dfs.namenodes.rpc.addresses", hdfsLeDescriptorsFacade.getActiveNN().getHostname());
//    conf.setStrings("fs.defaultFS", "hdfs://"+hdfsLeDescriptorsFacade.getActiveNN().getHostname());
  }

  @PreDestroy
  public void preDestroy() {
    conf.clear();
    conf = null;
  }

  /**
   * creates a new distributed file system operations with the super user
   * <p>
   * @return DistributedFileSystemOps
   */
  public DistributedFileSystemOps getDfsOps() {
    return new DistributedFileSystemOps(UserGroupInformation.createRemoteUser(
            settings.getHdfsSuperUser()), conf);
  }

  /**
   * Returns the user specific distributed file system operations
   * <p>
   * @param username
   * @return
   */
  public DistributedFileSystemOps getDfsOps(String username) {
    if (username == null || username.isEmpty()) {
      throw new NullPointerException("username not set.");
    }
    UserGroupInformation ugi;
    try {
      ugi = UserGroupInformation.createProxyUser(username, UserGroupInformation.
              getLoginUser());
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      return null;
    }
    return new DistributedFileSystemOps(ugi, conf);
  }
  public DistributedFileSystemOps getDfsOpsForTesting(String username) {
    if (username == null || username.isEmpty()) {
      throw new NullPointerException("username not set.");
    }
    //Get hdfs groups
        Collection<HdfsGroups> groups = hdfsUsersFacade.findByName(username).getHdfsGroupsCollection();
        String[] userGroups = new String[groups.size()];
        Iterator<HdfsGroups> iter = groups.iterator();
        int i=0;
        while(iter.hasNext()){
          userGroups[i] = iter.next().getName();
          i++;
        }
    UserGroupInformation ugi;
    try {
      ugi = UserGroupInformation.createProxyUserForTesting(username, UserGroupInformation.
              getLoginUser(), userGroups);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      return null;
    }
    return new DistributedFileSystemOps(ugi, conf);
  }

  /**
   * Removes the user group info and closes any file system created for this
   * user.
   * <p>
   * @param username
   */
  public void removeDfsOps(String username) {
    if (username == null || username.isEmpty()) {
      return;
    }
    UserGroupInformation ugi = ugiService.remove(username);
    if (ugi == null) {
      return;
    }
    try {
      FileSystem.closeAllForUGI(ugi);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Could not close file system for user " + ugi.
              getUserName(), ex);
    }
  }

  /**
   * Check if the inode at the given path is a directory.
   * <p/>
   * @param path
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public boolean isDir(String path) {
    Inode i = inodes.getInodeAtPath(path);
    if (i != null) {
      return i.isDir();
    }
    return false;
  }

  /**
   * Get the inode for a given path.
   * <p/>
   * @param path
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public Inode getInode(String path) {
    Inode i = inodes.getInodeAtPath(path);
    return i;
  }

  /**
   * Get a list of the names of the child files (so no directories) of the given
   * path.
   * <p/>
   * @param path
   * @return A list of filenames, empty if the given path does not have
   * children.
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public List<String> getChildNames(String path) {
    Inode inode = inodes.getInodeAtPath(path);
    if (inode.isDir()) {
      List<Inode> inodekids = inodes.getChildren(inode);
      ArrayList<String> retList = new ArrayList<>(inodekids.size());
      for (Inode i : inodekids) {
        if (!i.isDir()) {
          retList.add(i.getInodePK().getName());
        }
      }
      return retList;
    } else {
      return Collections.EMPTY_LIST;
    }
  }

  /**
   * Returns a list of inodes if the path is a directory empty list otherwise.
   * @param path
   * @return 
   */
  public List<Inode> getChildInodes(String path) {
    Inode inode = inodes.getInodeAtPath(path);
    if (inode.isDir()) {
      return inodes.getChildren(inode);
    } else {
      return Collections.EMPTY_LIST;
    }
  }
}
