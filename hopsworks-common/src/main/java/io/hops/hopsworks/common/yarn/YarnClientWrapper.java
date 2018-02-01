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
package io.hops.hopsworks.common.yarn;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.client.api.YarnClient;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YarnClientWrapper {
  private final Logger LOG = Logger.getLogger(YarnClientWrapper.class.getName());
  private final String projectName;
  private final String username;
  private final Configuration conf;
  private YarnClient yarnClient;
  
  public YarnClientWrapper(String projectName, String username,
      Configuration conf) {
    this.projectName = projectName;
    this.username = username;
    this.conf = conf;
  }
  
  public YarnClientWrapper get() {
    if (yarnClient == null) {
      yarnClient = YarnClient.createYarnClient();
      yarnClient.init(conf);
      yarnClient.start();
    }
    
    return this;
  }
  
  public YarnClient getYarnClient() {
    if (yarnClient == null) {
      throw new RuntimeException("YarnClient has not been initialized");
    }
    
    return yarnClient;
  }
  
  public String getProjectName() {
    return projectName;
  }
  
  public String getUsername() {
    return username;
  }
  
  public void close() {
    if (null != yarnClient) {
      try {
        yarnClient.close();
      } catch (IOException ex) {
        LOG.log(Level.WARNING, "Error while closing YarnClient", ex);
      }
    }
  }
}
