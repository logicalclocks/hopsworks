/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
