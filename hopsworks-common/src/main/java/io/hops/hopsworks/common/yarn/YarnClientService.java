/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

package io.hops.hopsworks.common.yarn;

import io.hops.hopsworks.common.exception.CryptoPasswordNotFoundException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.BaseHadoopClientsService;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.net.HopsSSLSocketFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class YarnClientService {
  private final Logger LOG = Logger.getLogger(
      YarnClientService.class.getName());
  
  @EJB
  private Settings settings;
  @EJB
  private BaseHadoopClientsService bhcs;
  
  private Configuration conf;
  private String hostname;
  private String transientDir;
  private String serviceCertsDir;
  
  public YarnClientService() {
  }
  
  @PostConstruct
  public void init() {
    String confDir = settings.getHadoopConfDir();
    File coreSite = new File(confDir, "core-site.xml");
    if (!coreSite.exists()) {
      handleMissingConf("core-site.xml", confDir);
    }
    
    File yarnSite = new File(confDir, "yarn-site.xml");
    if (!yarnSite.exists()) {
      handleMissingConf("yarn-site.xml", confDir);
    }
    
    conf = new Configuration();
    Path coreSitePath = new Path(coreSite.getAbsolutePath());
    Path yarnSitePath = new Path(yarnSite.getAbsolutePath());
    conf.addResource(coreSitePath);
    conf.addResource(yarnSitePath);
    
    if (settings.getHopsRpcTls()) {
      try {
        hostname = InetAddress.getLocalHost().getHostName();
        transientDir = settings.getHopsworksTmpCertDir();
        serviceCertsDir = conf.get(HopsSSLSocketFactory.CryptoKeys
            .SERVICE_CERTS_DIR.getValue(),
            HopsSSLSocketFactory.CryptoKeys.SERVICE_CERTS_DIR.getDefaultValue());
      } catch (UnknownHostException ex) {
        LOG.log(Level.SEVERE, "Could not determine hostname " + ex
            .getMessage(), ex);
        throw new RuntimeException("Could not determine hostname", ex);
      }
    }
  }
  
  @PreDestroy
  public void tearDown() {
    conf.clear();
    conf = null;
  }
  
  public YarnClientWrapper getYarnClient(String username) {
    if (settings.getHopsRpcTls()) {
      try {
        Configuration newConf = new Configuration(conf);
        bhcs.materializeCertsForNonSuperUser(username);
        
        bhcs.configureTlsForProjectSpecificUser(username, transientDir,
            newConf);
  
        return createYarnClient(username, newConf);
      } catch (CryptoPasswordNotFoundException ex) {
        LOG.log(Level.SEVERE, ex.getMessage(), ex);
        bhcs.removeNonSuperUserCertificate(username);
        return null;
      }
    }
    
    return createYarnClient(username, conf);
  }
  
  public YarnClientWrapper getYarnClientSuper() {
    return getYarnClientSuper(null);
  }
  
  public YarnClientWrapper getYarnClientSuper(Configuration conf) {
    if (settings.getHopsRpcTls()) {
      Configuration newConfig;
      if (null != conf) {
        newConfig = new Configuration(conf);
      } else {
        newConfig = new Configuration(this.conf);
      }
      
      String keystorePath = bhcs.getSuperKeystorePath();
      String keystorePass = bhcs.getSuperKeystorePassword();
      String truststorePath = bhcs.getSuperTrustStorePath();
      String truststorePass = bhcs.getSuperTrustStorePassword();
      
      HopsSSLSocketFactory.setTlsConfiguration(keystorePath, keystorePass,
          truststorePath, truststorePass, newConfig);
      
      return createYarnClient(null, newConfig);
    }
    
    return null != conf ? createYarnClient(null, conf) :
        createYarnClient(null, this.conf);
  }
  
  public void closeYarnClient(YarnClientWrapper yarnClientWrapper) {
    if (null != yarnClientWrapper) {
      try {
        yarnClientWrapper.close();
      } finally {
        if (settings.getHopsRpcTls()) {
          String username = yarnClientWrapper.getUsername();
          String projectName = yarnClientWrapper.getProjectName();
          if (null != username && null != projectName) {
            String effectiveUsername = projectName + HdfsUsersController
                .USER_NAME_DELIMITER + username;
            bhcs.removeNonSuperUserCertificate(effectiveUsername);
          }
        }
      }
    }
  }
  
  private void handleMissingConf(String confName, String confDir)
      throws IllegalStateException {
    LOG.log(Level.SEVERE, "Unable to locate {0} in {1}",
        new Object[]{confName, confDir});
    throw new IllegalStateException(
        "Unable to locate " + confName + " in " + confDir);
  }
  
  private YarnClientWrapper createYarnClient(String username,
      Configuration conf) {
    
    if (null != username) {
      String[] tokens = username.split(HdfsUsersController.USER_NAME_DELIMITER,
          2);
  
      if (tokens.length == 2) {
        return new YarnClientWrapper(tokens[0], tokens[1], conf).get();
      }
    }
    
    return new YarnClientWrapper(null, null, conf).get();
  }
}
