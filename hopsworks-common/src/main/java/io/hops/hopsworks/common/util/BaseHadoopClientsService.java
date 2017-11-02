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
package io.hops.hopsworks.common.util;

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.exception.CryptoPasswordNotFoundException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.user.CertificateMaterializer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.net.HopsSSLSocketFactory;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.ssl.FileBasedKeyStoresFactory;
import org.apache.hadoop.security.ssl.SSLFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class BaseHadoopClientsService {
  
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private UserFacade userFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private CertsFacade certsFacade;
  @EJB
  protected Settings settings;
  
  private Configuration sslConf;
  private String superKeystorePath;
  private String superKeystorePassword;
  private String superTrustStorePath;
  private String superTrustStorePassword;
  private String superuser;
  
  private final Logger LOG = Logger.getLogger(
      BaseHadoopClientsService.class.getName());
  
  public BaseHadoopClientsService() {
  }
  
  @PostConstruct
  public void init() {
    String confDir = settings.getHadoopConfDir();
    File coreSite = new File(confDir, "core-site.xml");
    if (!coreSite.exists()) {
      handleMissingConf("core-site.xml", confDir);
    }
    
    Configuration conf = new Configuration();
    conf.addResource(new Path(coreSite.getAbsolutePath()));
    
    sslConf = new Configuration(false);
    String hadoopConfDir = settings.getHadoopConfDir();
    File serverSSLConf = new File(hadoopConfDir, conf.get(SSLFactory
        .SSL_SERVER_CONF_KEY, "ssl-server.xml"));
    sslConf.addResource(new Path(serverSSLConf.getAbsolutePath()));
    superKeystorePath = sslConf.get(
        FileBasedKeyStoresFactory.resolvePropertyName(SSLFactory.Mode.SERVER,
            FileBasedKeyStoresFactory.SSL_KEYSTORE_LOCATION_TPL_KEY));
    superKeystorePassword = sslConf.get(
        FileBasedKeyStoresFactory.resolvePropertyName(SSLFactory.Mode.SERVER,
            FileBasedKeyStoresFactory.SSL_KEYSTORE_PASSWORD_TPL_KEY));
    superTrustStorePath = sslConf.get(
        FileBasedKeyStoresFactory.resolvePropertyName(SSLFactory.Mode.SERVER,
            FileBasedKeyStoresFactory.SSL_TRUSTSTORE_LOCATION_TPL_KEY));
    superTrustStorePassword = sslConf.get(
        FileBasedKeyStoresFactory.resolvePropertyName(SSLFactory.Mode.SERVER,
            FileBasedKeyStoresFactory.SSL_TRUSTSTORE_PASSWORD_TPL_KEY));
    try {
      superuser = UserGroupInformation.getLoginUser().getUserName();
    } catch (IOException ex) {
      throw new IllegalStateException("Could not identify login user");
    }
  }
  
  public String getSuperKeystorePath() {
    return superKeystorePath;
  }
  
  public String getSuperKeystorePassword() {
    return superKeystorePassword;
  }
  
  public String getSuperTrustStorePath() {
    return superTrustStorePath;
  }
  
  public String getSuperTrustStorePassword() {
    return superTrustStorePassword;
  }
  
  public String getProjectSpecificUserCertPassword(String username)
    throws CryptoPasswordNotFoundException {
    CertificateMaterializer.CryptoMaterial cryptoMaterial;
    if (username.matches(HopsSSLSocketFactory.USERNAME_PATTERN)) {
      String[] project_username = username.split(HdfsUsersController
          .USER_NAME_DELIMITER);
  
      cryptoMaterial = certificateMaterializer
          .getUserMaterial(project_username[1], project_username[0]);
      return cryptoMaterial.getPassword();
    } else if (!username.equals(superuser)){
      // It's project wide user
      cryptoMaterial = certificateMaterializer.getUserMaterial(username);
      return cryptoMaterial.getPassword();
    } else {
      throw new IllegalArgumentException("User " + username + " is not a " +
          "project specific username nor a project wide");
    }
  }
  
  public void configureTlsForProjectSpecificUser(String username, String
      transientDir, Configuration conf) throws CryptoPasswordNotFoundException {
    String password = getProjectSpecificUserCertPassword(username);
    
    String prefix = Paths.get(transientDir, username).toString();
    String kstorePath = prefix + HopsSSLSocketFactory.KEYSTORE_SUFFIX;
    String tstorePath = prefix + HopsSSLSocketFactory.TRUSTSTORE_SUFFIX;
    HopsSSLSocketFactory.setTlsConfiguration(kstorePath, password,
        tstorePath, password, conf);
  }
  
  public void materializeCertsForNonSuperUser(String username) {
    // Make sure it's a normal, non superuser
    if (username.matches(HopsSSLSocketFactory.USERNAME_PATTERN)) {
      String[] tokens = username.split(HdfsUsersController
          .USER_NAME_DELIMITER, 2);
      if (tokens.length == 2) {
        try {
          certificateMaterializer.materializeCertificates(tokens[1], tokens[0]);
        } catch (IOException ex) {
          throw new RuntimeException("Error while materializing " +
              "user certificates " + ex.getMessage(), ex);
        }
      }
    } else if (!username.equals(superuser)){
      try {
        certificateMaterializer.materializeCertificates(username);
      } catch (IOException ex) {
        throw new RuntimeException("Error while materializing " +
            "user certificates " + ex.getMessage(), ex);
      }
    }
  }
  
  public void removeNonSuperUserCertificate(String username) {
    if (username != null
        && username.matches(HopsSSLSocketFactory.USERNAME_PATTERN)) {
      String[] tokens = username.split(HdfsUsersController
          .USER_NAME_DELIMITER, 2);
      if (tokens.length == 2) {
        certificateMaterializer.removeCertificate(tokens[1], tokens[0]);
      }
    } else if (username != null
        && !username.matches(superuser)) {
      certificateMaterializer.removeCertificate(username);
    }
  }
  
  private void handleMissingConf(String confName, String confDir)
      throws IllegalStateException {
    LOG.log(Level.SEVERE, "Unable to locate {0} in {1}",
        new Object[]{confName, confDir});
    throw new IllegalStateException(
        "Unable to locate " + confName + " in " + confDir);
  }
}
