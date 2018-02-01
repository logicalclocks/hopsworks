/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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
