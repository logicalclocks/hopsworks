package se.kth.hopsworks.hdfs.fileoperations;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import se.kth.hopsworks.util.Settings;

@Singleton
public class DFSSingleton {

  private static final Logger logger = Logger.getLogger(DFSSingleton.class.
          getName());

  @EJB
  private Settings settings;
  private final Map<String, DistributedFileSystemOps> distributedFileSystems
          = new LinkedHashMap<>();
  private DistributedFileSystemOps dfsOps;

  public DFSSingleton() {
  }

  @PreDestroy
  public void preDestroy() {
    for (DistributedFileSystemOps dfs : distributedFileSystems.values()) {
      dfs.close();
    }

  }

  /**
   * Returns the user specific distributed file system operations
   * <p>
   * @param username
   * @return
   */
  public DistributedFileSystemOps getDfsOps(String username) {
    DistributedFileSystemOps dfs;
    if (username == null || username.isEmpty()) {
      throw new NullPointerException("username not set.");
    }
    synchronized (distributedFileSystems) {
      dfs = distributedFileSystems.get(username);
    }
    if (dfs == null) {
      logger.log(Level.INFO, "No dfs object found for {0} creating new.", username);
      dfs = createDfs(username);
    }
    return dfs;
  }

  /**
   * Removes the DfsOps object with key==username from the hash map
   * @param username 
   */
  public void removeDfsOps (String username) {
    if (username == null || username.isEmpty()) {
      return;
    }
    synchronized (distributedFileSystems) {
      distributedFileSystems.remove(username);
    }
  }
  /**
   * creates a new distributed file system operations with the super user
   * <p>
   * @return DistributedFileSystemOps
   */
  public DistributedFileSystemOps getDfsOps() {
    if (dfsOps == null){
      logger.log(Level.INFO, "No dfs object found creating new.");
      dfsOps = new DistributedFileSystemOps(settings);
    }
    return dfsOps;
  }

  /**
   * Creates and adds a distributed file system operations for the username.
   * this can be called when a user changes project, to reduce the time needed for
   * creating new file system objects when the user performs a file operation.
   * <p>
   * @param username
   */
  public void addDfs(String username) {
    DistributedFileSystemOps dfs = new DistributedFileSystemOps(username,
            settings);
    synchronized (distributedFileSystems) {
      distributedFileSystems.put(username, dfs);
    }
  }

  private DistributedFileSystemOps createDfs(String username) {
    DistributedFileSystemOps dfs = new DistributedFileSystemOps(username,
            settings);
    synchronized (distributedFileSystems) {
      distributedFileSystems.put(username, dfs);
    }
    return dfs;
  }
}
