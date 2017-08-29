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
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.user.CertificateMaterializer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.net.HopsSSLSocketFactory;
import org.apache.hadoop.security.ssl.FileBasedKeyStoresFactory;
import org.apache.hadoop.security.ssl.SSLFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

@Stateless
public class BaseHadoopClientsService {
  
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private UserFacade userFacade;
  @EJB
  private CertsFacade certsFacade;
  @EJB
  protected Settings settings;
  
  private Configuration sslConf;
  private String superKeystorePath;
  private String superKeystorePassword;
  private String superTrustStorePath;
  private String superTrustStorePassword;
  
  private volatile boolean isSSLConfParsed = false;
  
  public BaseHadoopClientsService() {
  }
  
  public synchronized void parseServerSSLConf(Configuration conf) {
    if (!isSSLConfParsed) {
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
      
      isSSLConfParsed = true;
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
    throws Exception {
    String[] project_username = username.split(HdfsUsersController
        .USER_NAME_DELIMITER);
    Users user = userFacade.findByUsername(project_username[1]);
    UserCerts userCert = certsFacade.findUserCert(project_username[0],
        project_username[1]);
    String encryptedPass = userCert.getUserKeyPwd();
    return HopsUtils.decrypt(user.getPassword(), settings
        .getHopsworksMasterPasswordSsl(), encryptedPass);
  }
  
  public void configureTlsForProjectSpecificUser(String username, String
      transientDir, Configuration conf) throws CryptoPasswordNotFoundException {
    String password;
    try {
      password = getProjectSpecificUserCertPassword(username);
    } catch (Exception ex) {
      throw new CryptoPasswordNotFoundException(
          "Could not find crypto material in database for user " + username,
          ex);
    }
    String prefix = Paths.get(transientDir, username).toString();
    String kstorePath = prefix + HopsSSLSocketFactory.KEYSTORE_SUFFIX;
    String tstorePath = prefix + HopsSSLSocketFactory.TRUSTSTORE_SUFFIX;
    HopsSSLSocketFactory.setTlsConfiguration(kstorePath, password,
        tstorePath, password, conf);
  }
  
  public void materializeCertsForNonSuperUser(String username) {
    // Make sure it's a normal, non superuser
    if (username.matches(HopsSSLSocketFactory.USERNAME_PATTERN)) {
      String[] tokens = username.split(HdfsUsersController.USER_NAME_DELIMITER);
      if (tokens.length == 2) {
        try {
          certificateMaterializer.materializeCertificates(tokens[1], tokens[0]);
        } catch (IOException ex) {
          throw new RuntimeException("Error while materializing " +
              "user certificates " + ex.getMessage(), ex);
        }
      }
    }
  }
  
  public void removeNonSuperUserCertificate(String username, String
      projectName) {
    if (null != username && !username.equals(settings.getHdfsSuperUser())
        && null != projectName) {
      certificateMaterializer.removeCertificate(username, projectName);
    }
  }
  
  public class CryptoPasswordNotFoundException extends Exception {
    public CryptoPasswordNotFoundException(String message) {
      super(message);
    }
  
    public CryptoPasswordNotFoundException(Throwable cause) {
      super(cause);
    }
    
    public CryptoPasswordNotFoundException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
