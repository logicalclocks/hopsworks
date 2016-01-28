package se.kth.hopsworks.hdfs.fileoperations;

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
  private ConcurrentMap<String, UserGroupInformation> cache
          = new ConcurrentHashMap<>();

  public UserGroupInformationService() {
  }

  
  @PreDestroy
  public void preDestroy() {
    for (UserGroupInformation ugi : cache.values()) {
      try {
        FileSystem.closeAllForUGI(ugi);
      } catch (IOException ioe) {
        logger.log(Level.INFO,
                "Exception occurred while closing filesystems for " + ugi.
                getUserName(), ioe);
      }
    }
    cache.clear();
  }

  /**
   * Creates and saves a proxy user.
   * @param user
   * @return
   * @throws IOException 
   */
  public UserGroupInformation getProxyUser(String user) throws IOException {
    cache.putIfAbsent(user, UserGroupInformation.createProxyUser(user,
            UserGroupInformation.getLoginUser()));
    return cache.get(user);
  }

  /**
   * removes a user from cache
   * @param username
   * @return 
   */
  public UserGroupInformation remove(String username) {
    return cache.remove(username);
  }
  
}
