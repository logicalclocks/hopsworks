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

import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.io.FileUtils;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static io.hops.hopsworks.common.security.PKI.CAType.ROOT;

@Stateless
public class PKI {

  @EJB
  private Settings settings;

  private Map<CAType, String> caPubCertCache = new HashMap<>();

  final static Logger logger = Logger.getLogger(PKI.class.getName());

  private final static long TEN_YEARS = 3650;

  public String getCertFileName(CertificateType certType, Map<String, String> subject) {
    switch (certType) {
      case APP:
        return subject.get("CN") + "__" + subject.get("O") + "__" + subject.get("OU");
      case HOST:
        return subject.get("CN") + "__" + subject.get("OU");
      default:
        return subject.get("CN");
    }
  }

  public long getValidityPeriod(CertificateType certType) {
    switch (certType) {
      case APP:
        return getAppCertificateValidityPeriod();
      case HOST:
        return getServiceCertificateValidityPeriod();
      case DELA: case KUBE:
        return TEN_YEARS;
      default:
        throw new IllegalArgumentException("Certificate type not recognized");
    }
  }

  private long getServiceCertificateValidityPeriod() {
    if (!settings.isServiceKeyRotationEnabled()) {
      return TEN_YEARS;
    }

    // Add 4 days just to be sure.
    return getCertificateValidityInDays(settings.getServiceKeyRotationInterval())+ 4;
  }

  private long getAppCertificateValidityPeriod() {
    return getCertificateValidityInDays(settings.getApplicationCertificateValidityPeriod());
  }

  private long getCertificateValidityInDays(String rawConfigurationProperty) {
    Long timeValue = settings.getConfTimeValue(rawConfigurationProperty);
    TimeUnit unitValue = settings.getConfTimeTimeUnit(rawConfigurationProperty);
    return TimeUnit.DAYS.convert(timeValue, unitValue);
  }

  public HashMap<String, String> getKeyValuesFromSubject(String subject) {
    if (subject == null || subject.isEmpty()) {
      return null;
    }
    String[] parts = subject.split("/");
    String[] keyVal;
    HashMap<String, String> keyValStore = new HashMap<>();
    for (String part : parts) {
      keyVal = part.split("=");
      if (keyVal.length < 2) {
        continue;
      }
      keyValStore.put(keyVal[0], keyVal[1]);
    }
    return keyValStore;
  }


  /**
   * This function provides a mapping between certificate types and the corresponding CA
   * @param certType
   * @return
   */
  public CAType getResponsibileCA(CertificateType certType) {
    switch (certType) {
      case HOST: case DELA: case APP: case PROJECT_USER:
        return CAType.INTERMEDIATE;
      case KUBE:
        return CAType.KUBECA;
      default:
        throw new IllegalArgumentException("Certificate type not recognized");
    }
  }

  public enum CAType {
    ROOT,
    INTERMEDIATE,
    KUBECA;
  }

  public String getCAParentPath(CAType caType) {
    switch (caType) {
      case ROOT:
        return settings.getCaDir();
      case INTERMEDIATE:
        return settings.getIntermediateCaDir();
      case KUBECA:
        return settings.getKubeCAPath();
      default:
        throw new IllegalArgumentException("CA type not recognized");
    }
  }

  public String getCAKeyPassword(CAType caType) {
    switch (caType) {
      case ROOT: case INTERMEDIATE:
        return settings.getHopsworksMasterPasswordSsl();
      case KUBECA:
        return settings.getKubeCAPassword();
      default:
        throw new IllegalArgumentException("CA type not recognized");
    }
  }

  public Path getCAConfPath(CAType caType) {
    switch (caType) {
      case ROOT:
        return Paths.get(settings.getCaDir(), "openssl-ca.cnf");
      case INTERMEDIATE:
        return Paths.get(settings.getIntermediateCaDir(), "openssl-intermediate.cnf");
      case KUBECA:
        return Paths.get(settings.getKubeCAPath(), "kube-ca.cnf");
      default:
        throw new IllegalArgumentException("CA type not recognized");
    }
  }

  public Path getCACertsDir(CAType caType) {
    return Paths.get(getCAParentPath(caType), "certs");
  }

  public Path getCAKeysDir(CAType caType) {
    return Paths.get(getCAParentPath(caType), "private");
  }

  public Path getCACRLPath(CAType caType){
    switch (caType) {
      case ROOT:
        return Paths.get(settings.getCaDir(), "crl", "ca.crl.pem");
      case INTERMEDIATE:
        return Paths.get(settings.getIntermediateCaDir(), "crl", "intermediate.crl.pem");
      case KUBECA:
        return Paths.get(settings.getKubeCAPath(), "crl", "kube-ca.crl.pem");
      default:
        throw new IllegalArgumentException("CA type not recognized");
    }
  }

  public String getEffectiveExtensions(CAType caType) {
    switch (caType) {
      case ROOT:
        return "v3_intermediate_ca";
      case INTERMEDIATE:
        return "usr_cert";
      case KUBECA:
        return "v3_ext";
      default:
        throw new IllegalArgumentException("CA type not recognized");
    }
  }

  public Path getCertPath(CAType caType, String certFileName) {
    return Paths.get(getCACertsDir(caType).toString(), certFileName + ".cert.pem");
  }

  public Path getKeyPath(CAType caType, String keyFileName) {
    return Paths.get(getCAKeysDir(caType).toString(), keyFileName + ".cert.pem");
  }

  public Path getCACertPath(CAType caType) {
    switch (caType) {
      case ROOT:
        return getCertPath(caType, "ca");
      case INTERMEDIATE:
        return getCertPath(caType, "intermediate");
      case KUBECA:
        return getCertPath(caType, "kube-ca");
      default:
        throw new IllegalArgumentException("CA type not recognized");
    }
  }

  /**
   * This method differs from the next one as this returns the file on the local fs
   * fo the chain of trust. File that contains the certificates of all the CAs in the chain
   * based on the intermediate CA
   * @param caType
   * @return
   */
  public Path getChainOfTrustFilePath(CAType caType) {
    switch (caType) {
      case ROOT:
        return getCertPath(caType,"ca");
      case INTERMEDIATE:
        return getCertPath(caType, "ca-chain");
      case KUBECA:
        return getCertPath(caType, "ca-chain");
      default:
        throw new IllegalArgumentException("CA type not recognized");
    }
  }

  /**
   *  This is an attempt to cache the certificate of the CAs.
   *  Certificates for CAs will stay in memory until the ejb is evicted from the pool
   *
   *  Rotation of intermediate CA private key will require a restart of Hopsworks
   */
  public Pair<String, String> getChainOfTrust(CAType caType) throws IOException {
    String intermediateCaCert = null;
    if (caType != ROOT) {
      intermediateCaCert = getCert(caType);
    }

    String rootCaCert = getCert(ROOT);

    return new Pair<>(rootCaCert, intermediateCaCert);
  }


  private String getCert(CAType caType) throws IOException {
    String caPubCert = caPubCertCache.get(caType);
    if (caPubCert == null) {
      synchronized (caPubCertCache) {
        if (caPubCertCache.get(caType) == null) {
          File caPubCertFile = getCACertPath(caType).toFile();
          caPubCert = FileUtils.readFileToString(caPubCertFile);
          caPubCertCache.put(caType, caPubCert);
        }
      }
    }

    return caPubCert;
  }
}
