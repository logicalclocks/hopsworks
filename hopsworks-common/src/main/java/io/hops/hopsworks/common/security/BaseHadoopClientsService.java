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
package io.hops.hopsworks.common.security;

import io.hops.hopsworks.exceptions.CryptoPasswordNotFoundException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.security.HopsUtil;
import io.hops.security.SuperuserKeystoresLoader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.net.HopsSSLSocketFactory;
import org.apache.hadoop.security.ssl.X509SecurityMaterial;

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
  protected Settings settings;

  private X509SecurityMaterial securityMaterial;
  private String materialPassword;

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

    try {
      SuperuserKeystoresLoader loader = new SuperuserKeystoresLoader(conf);
      securityMaterial = loader.loadSuperUserMaterial();
      materialPassword = HopsUtil.readCryptoMaterialPassword(securityMaterial.getPasswdLocation().toFile());
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, "Could not load super user x.509 material", ex);
      throw new RuntimeException(ex);
    }
  }
  
  public String getSuperKeystorePath() {
    return securityMaterial.getKeyStoreLocation().toString();
  }
  
  public String getSuperKeystorePassword() {
    return materialPassword;
  }
  
  public String getSuperTrustStorePath() {
    return securityMaterial.getTrustStoreLocation().toString();
  }
  
  public String getSuperTrustStorePassword() {
    return materialPassword;
  }

  public String getProjectSpecificUserCertPassword(String username) throws CryptoPasswordNotFoundException {
    if (username != null) {
      String[] project_username = username.split(HdfsUsersController
          .USER_NAME_DELIMITER, 2);
      CertificateMaterializer.CryptoMaterial cryptoMaterial = certificateMaterializer
          .getUserMaterial(project_username[1], project_username[0]);

      return new String(cryptoMaterial.getPassword());
    }

    throw new RuntimeException("Username cannot be null!");
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
    if (username != null) {
      String[] tokens = username.split(HdfsUsersController.USER_NAME_DELIMITER, 2);
      if (tokens.length == 2) {
        try {
          certificateMaterializer.materializeCertificatesLocal(tokens[1], tokens[0]);
        } catch (IOException ex) {
          throw new RuntimeException("Error while materializing " +
              "user certificates " + ex.getMessage(), ex);
        }
      }
    }
  }
  
  public void removeNonSuperUserCertificate(String username) {
    if (username != null) {
      String[] tokens = username.split(HdfsUsersController.USER_NAME_DELIMITER, 2);
      if (tokens.length == 2) {
        certificateMaterializer.removeCertificatesLocal(tokens[1], tokens[0]);
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
}
