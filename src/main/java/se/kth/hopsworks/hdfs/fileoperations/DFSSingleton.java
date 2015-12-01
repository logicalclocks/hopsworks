package se.kth.hopsworks.hdfs.fileoperations;

import java.util.LinkedHashMap;
import java.util.Map;
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
  public DistributedFileSystemOps getDfs(String username) {
    DistributedFileSystemOps dfs;
    synchronized (distributedFileSystems) {
      dfs = distributedFileSystems.get(username);
    }
    if (dfs == null) {
      dfs = createDfs(username);
    }
    return dfs;
  }

  /**
   * creates a new distributed file system operations with the super user
   * <p>
   * @return DistributedFileSystemOps
   */
  public DistributedFileSystemOps getDfs() {
    if (dfsOps == null){
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
