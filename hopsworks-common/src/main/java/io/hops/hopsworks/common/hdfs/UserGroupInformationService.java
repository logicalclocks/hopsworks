package io.hops.hopsworks.common.hdfs;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.security.UserGroupInformation;

@Singleton
public class UserGroupInformationService {

  private static final Logger logger = Logger.getLogger(
          UserGroupInformationService.class.
          getName());
  private ConcurrentMap<String, UserGroupInformation> proxyCache
          = new ConcurrentHashMap<>();
  private ConcurrentMap<String, UserGroupInformation> remoteCache
          = new ConcurrentHashMap<>();

  public UserGroupInformationService() {
  }

  @PreDestroy
  public void preDestroy() {
    for (UserGroupInformation ugi : proxyCache.values()) {
      try {
        FileSystem.closeAllForUGI(ugi);
      } catch (IOException ioe) {
        logger.log(Level.INFO,
                "Exception occurred while closing filesystems for " + ugi.
                getUserName(), ioe);
      }
    }
    proxyCache.clear();
  }

  /**
   * Creates and saves a proxy user.
   *
   * @param user
   * @return
   * @throws IOException
   */
  public UserGroupInformation getProxyUser(String user) throws IOException {
    proxyCache.putIfAbsent(user, UserGroupInformation.createProxyUser(user,
            UserGroupInformation.getLoginUser()));
    return proxyCache.get(user);
  }

  /**
   * Creates and saves a remote user.
   *
   * @param user
   * @return
   * @throws IOException
   */
  public UserGroupInformation getRemoteUser(String user) throws IOException {
    remoteCache.putIfAbsent(user, UserGroupInformation.createProxyUser(user,
            UserGroupInformation.getLoginUser()));
    return remoteCache.get(user);
  }

  /**
   * removes a user from cache
   *
   * @param username
   * @return
   */
  public UserGroupInformation remove(String username) {
    return proxyCache.remove(username);
  }

}
