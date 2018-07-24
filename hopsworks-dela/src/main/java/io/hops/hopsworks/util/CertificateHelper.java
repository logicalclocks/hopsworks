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

package io.hops.hopsworks.util;

import com.google.common.io.ByteStreams;
import io.hops.hopsworks.common.dao.dela.certs.ClusterCertificate;
import io.hops.hopsworks.common.dao.dela.certs.ClusterCertificateFacade;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.LocalhostServices;
import io.hops.hopsworks.common.util.Settings;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.x500.X500Principal;
import org.apache.commons.io.FileUtils;
import org.javatuples.Triplet;

public class CertificateHelper {

  private final static Logger LOG = Logger.getLogger(CertificateHelper.class.getName());

  public static Optional<Triplet<KeyStore, KeyStore, String>> loadKeystoreFromFile(String masterPswd, Settings settings,
    ClusterCertificateFacade certFacade, CertificatesMgmService certificatesMgmService) {
    String certPath = settings.getHopsSiteCert();
    String intermediateCertPath = settings.getHopsSiteIntermediateCert();
    String keystorePath = settings.getHopsSiteKeyStorePath();
    String truststorePath = settings.getHopsSiteTrustStorePath();
    try {
      String certPswd = HopsUtils.randomString(64);
      String encryptedCertPswd = HopsUtils.encrypt(masterPswd, certPswd,
          certificatesMgmService.getMasterEncryptionPassword());
      File certFile = readFile(certPath);
      File intermediateCertFile = readFile(intermediateCertPath);
      String clusterName = getClusterName(certFile);
      settings.setHopsSiteClusterName(clusterName);
      generateKeystore(certFile, intermediateCertFile, certPswd, settings);
      File keystoreFile = readFile(keystorePath);
      File truststoreFile = readFile(truststorePath);
      KeyStore keystore, truststore;
      try (FileInputStream keystoreIS = new FileInputStream(keystoreFile);
        FileInputStream truststoreIS = new FileInputStream(truststoreFile)) {
        keystore = keystore(keystoreIS, certPswd);
        truststore = keystore(truststoreIS, certPswd);
      }
      try (FileInputStream keystoreIS = new FileInputStream(keystoreFile);
        FileInputStream truststoreIS = new FileInputStream(truststoreFile)) {
        certFacade.saveClusterCerts(clusterName, ByteStreams.toByteArray(keystoreIS),
          ByteStreams.toByteArray(truststoreIS), encryptedCertPswd);
      }
      return Optional.of(Triplet.with(keystore, truststore, certPswd));
    } catch (Exception ex) {
      settings.deleteHopsSiteClusterName();
      LOG.log(Level.SEVERE, "keystore ex. {0}", ex.getMessage());
      return Optional.empty();
    } finally {
      FileUtils.deleteQuietly(new File(keystorePath));
      FileUtils.deleteQuietly(new File(truststorePath));
    }
  }

  public static Optional<Triplet<KeyStore, KeyStore, String>> loadKeystoreFromDB(String masterPswd, String clusterName,
    ClusterCertificateFacade certFacade, CertificatesMgmService certificatesMgmService) {
    try {
      Optional<ClusterCertificate> cert = certFacade.getClusterCert(clusterName);
      if (!cert.isPresent()) {
        return Optional.empty();
      }
      String certPswd = HopsUtils.decrypt(masterPswd, cert.get().getCertificatePassword(),
          certificatesMgmService.getMasterEncryptionPassword());
      KeyStore keystore, truststore;
      try (ByteArrayInputStream keystoreIS = new ByteArrayInputStream(cert.get().getClusterKey());
        ByteArrayInputStream truststoreIS = new ByteArrayInputStream(cert.get().getClusterCert())) {
        keystore = keystore(keystoreIS, certPswd);
        truststore = keystore(truststoreIS, certPswd);
      }
      return Optional.of(Triplet.with(keystore, truststore, certPswd));
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, "keystore ex. {0}", ex.getMessage());
      return Optional.empty();
    }
  }

  private static void generateKeystore(File cert, File intermediateCert, String certPswd, Settings settings)
    throws IllegalStateException {
    if (!isCertSigned(cert, intermediateCert)) {
      throw new IllegalStateException("Certificate is not signed");
    }
    try {
      LocalhostServices.generateHopsSiteKeystore(settings, certPswd);
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, "keystore generate ex. {0}", ex.getMessage());
      throw new IllegalStateException("keystore generate ex", ex);
    }
  }

  private static String getClusterName(File certFile) throws IllegalStateException {
    X509Certificate cert = getX509Cert(certFile);
    String o = getCertificatePart(cert, "O");
    String ou = getCertificatePart(cert, "OU");
    String clusterName = o + "_" + ou;
    return clusterName;
  }

  private static X509Certificate getX509Cert(File cert) throws IllegalStateException {
    try (InputStream inStream = new FileInputStream(cert)) {
      CertificateFactory factory = CertificateFactory.getInstance("X.509");
      X509Certificate x509Cert = (X509Certificate) factory.generateCertificate(inStream);
      return x509Cert;
    } catch (CertificateException | IOException ex) {
      LOG.log(Level.SEVERE, "cert ex {0}", ex);
      throw new IllegalStateException("cert ex", ex);
    }
  }

  private static boolean isCertSigned(File certFile, File intermediateCertFile) throws IllegalStateException {
    X509Certificate cert = getX509Cert(certFile);
    X509Certificate caCert = getX509Cert(intermediateCertFile);
    String intermediateSubjectDN = caCert.getSubjectDN().getName();
    String issuerDN = cert.getIssuerDN().getName();
    LOG.log(Level.INFO, "sign check: {0} {1}", new Object[]{issuerDN, intermediateSubjectDN});
    return issuerDN.equals(intermediateSubjectDN);
  }

  private static File readFile(String certPath) throws IllegalStateException {
    File certFile = new File(certPath);
    if (!certFile.exists()) {
      LOG.log(Level.SEVERE, "Could not find file:{0}", certPath);
      throw new IllegalStateException("Could not find file");
    }
    return certFile;
  }

  private static KeyStore keystore(InputStream is, String certPswd) throws IllegalStateException {
    try {
      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      keystore.load(is, certPswd.toCharArray());
      return keystore;
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException ex) {
      LOG.log(Level.SEVERE, "keystore ex. {0}", ex);
      throw new IllegalStateException("keystore ex", ex);
    }
  }

  public static String getCertificatePart(X509Certificate cert, String partName) {
    String tmpName, name = "";
    X500Principal principal = cert.getSubjectX500Principal();
    String part = partName + "=";
    int start = principal.getName().indexOf(part);
    if (start > -1) {
      tmpName = principal.getName().substring(start + part.length());
      int end = tmpName.indexOf(",");
      if (end > 0) {
        name = tmpName.substring(0, end);
      } else {
        name = tmpName;
      }
    }
    return name.toLowerCase();
  }
}
