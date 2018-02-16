/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

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
