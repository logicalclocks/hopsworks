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
package io.hops.hopsworks.ca.controllers;

import io.hops.hopsworks.ca.controllers.CAConf.CAConfKeys;
import org.apache.commons.io.FileUtils;
import org.javatuples.Pair;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
public class PKI {

  @EJB
  private CAConf CAConf;

  private Map<CAType, String> caPubCertCache = new HashMap<>();
  private SimpleDateFormat dateFormat = null;

  private final static long TEN_YEARS = 3650;
  private static final Map<String, TimeUnit> TIME_SUFFIXES;

  private static final Pattern SUBJECT_PATTERN = Pattern.compile("(([\\w\\.]+\\s?)=(\\s?[\\w\\.\\-@~\\+\\?%:]+))");
  private static final String SUBJECT = "subject=";

  private static final String CERTIFICATE_TYPE_NOT_RECOGNIZED_ERR = "Certificate type not recognized";
  private static final String CA_TYPE_NOT_RECOGNIZED_ERR = "CA type not recognized";

  static {
    TIME_SUFFIXES = new HashMap<>(5);
    TIME_SUFFIXES.put("ms", TimeUnit.MILLISECONDS);
    TIME_SUFFIXES.put("s", TimeUnit.SECONDS);
    TIME_SUFFIXES.put("m", TimeUnit.MINUTES);
    TIME_SUFFIXES.put("h", TimeUnit.HOURS);
    TIME_SUFFIXES.put("d", TimeUnit.DAYS);
  }
  private static final Pattern TIME_CONF_PATTERN = Pattern.compile("([0-9]+)([a-z]+)?");


  @PostConstruct
  public void init() {
    dateFormat = new SimpleDateFormat("yyMMddHHmmss");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  public String getCertFileName(CertificateType certType, Map<String, String> subject) {
    switch (certType) {
      case APP:
        return subject.get("CN") + "__" + subject.get("O") + "__" + subject.get("OU");
      case HOST:
        return subject.get("CN") + "__" + subject.get("L") + "__" + subject.get("OU");
      default:
        return subject.get("CN");
    }
  }

  public String getValidityPeriod(CertificateType certType) {
    switch (certType) {
      case APP:
        return getAppCertificateValidityPeriod();
      case HOST:
        return getServiceCertificateValidityPeriod();
      case DELA: case KUBE: case PROJECT:
        return getExpirationDateASN1(TimeUnit.MILLISECONDS.convert(TEN_YEARS, TimeUnit.DAYS));
      default:
        throw new IllegalArgumentException(CERTIFICATE_TYPE_NOT_RECOGNIZED_ERR);
    }
  }

  private String getServiceCertificateValidityPeriod() {
    long validityMs = -1;
    if (!CAConf.getBoolean(CAConfKeys.SERVICE_KEY_ROTATION_ENABLED)){
      validityMs = TimeUnit.MILLISECONDS.convert(TEN_YEARS, TimeUnit.DAYS);
    } else {
      // Add 4 days just to be sure.
      validityMs = getCertificateValidityInMS(CAConf.getString(CAConfKeys.SERVICE_KEY_ROTATION_INTERVAL) +
        TimeUnit.MILLISECONDS.convert(4, TimeUnit.DAYS));
    }

    return getExpirationDateASN1(validityMs);
  }

  private String getAppCertificateValidityPeriod() {
    return getExpirationDateASN1(
        getCertificateValidityInMS(CAConf.getString(CAConfKeys.APPLICATION_CERTIFICATE_VALIDITY_PERIOD)));
  }

  private long getCertificateValidityInMS(String rawConfigurationProperty) {
    Long timeValue = getConfTimeValue(rawConfigurationProperty);
    TimeUnit unitValue = getConfTimeTimeUnit(rawConfigurationProperty);
    return TimeUnit.MILLISECONDS.convert(timeValue, unitValue);
  }

  /**
   * Format date ASN1 UTCTime
   */
  private String getExpirationDateASN1(long validityMS) {
    return dateFormat.format(new Date(System.currentTimeMillis() + validityMS)) + 'Z';
  }

  public Map<String, String> getKeyValuesFromSubject(String subject) {
    if (subject == null || subject.isEmpty()) {
      return null;
    }

    // Remove front subject= before using the regex
    subject = subject.replaceFirst(SUBJECT, "");
    Matcher matcher = SUBJECT_PATTERN.matcher(subject);

    HashMap<String, String> keyValStore = new HashMap<>();
    while (matcher.find()) {
      keyValStore.put(matcher.group(2).trim(), matcher.group(3).trim());
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
      case HOST: case DELA: case APP: case PROJECT:
        return CAType.INTERMEDIATE;
      case KUBE:
        return CAType.KUBECA;
      default:
        throw new IllegalArgumentException(CERTIFICATE_TYPE_NOT_RECOGNIZED_ERR);
    }
  }

  public enum CAType {
    ROOT,
    INTERMEDIATE,
    KUBECA
  }

  public String getCAParentPath(CAType caType) {
    switch (caType) {
      case ROOT:
        return CAConf.getString(CAConfKeys.CERTS_DIR);
      case INTERMEDIATE:
        return CAConf.getString(CAConfKeys.CERTS_DIR) + "/intermediate";
      case KUBECA:
        return CAConf.getString(CAConfKeys.CERTS_DIR) + "/kube";
      default:
        throw new IllegalArgumentException(CA_TYPE_NOT_RECOGNIZED_ERR);
    }
  }

  public String getCAKeyPassword(CAType caType) {
    switch (caType) {
      case ROOT: case INTERMEDIATE:
        return CAConf.getString(CAConfKeys.HOPSWORKS_SSL_MASTER_PASSWORD);
      case KUBECA:
        return CAConf.getString(CAConfKeys.KUBE_CA_PASSWORD);
      default:
        throw new IllegalArgumentException(CA_TYPE_NOT_RECOGNIZED_ERR);
    }
  }

  public Path getCAConfPath(CAType caType) {
    switch (caType) {
      case ROOT:
        return Paths.get(getCAParentPath(CAType.ROOT), "openssl-ca.cnf");
      case INTERMEDIATE:
        return Paths.get(getCAParentPath(CAType.INTERMEDIATE), "openssl-intermediate.cnf");
      case KUBECA:
        return Paths.get(getCAParentPath(CAType.KUBECA), "kube-ca.cnf");
      default:
        throw new IllegalArgumentException(CA_TYPE_NOT_RECOGNIZED_ERR);
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
        return Paths.get(getCAParentPath(CAType.ROOT), "crl", "ca.crl.pem");
      case INTERMEDIATE:
        return Paths.get(getCAParentPath(CAType.INTERMEDIATE), "crl", "intermediate.crl.pem");
      case KUBECA:
        return Paths.get(getCAParentPath(CAType.KUBECA), "crl", "kube-ca.crl.pem");
      default:
        throw new IllegalArgumentException(CA_TYPE_NOT_RECOGNIZED_ERR);
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
        throw new IllegalArgumentException(CA_TYPE_NOT_RECOGNIZED_ERR);
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
        throw new IllegalArgumentException(CA_TYPE_NOT_RECOGNIZED_ERR);
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
      case INTERMEDIATE: case KUBECA:
        return getCertPath(caType, "ca-chain");
      default:
        throw new IllegalArgumentException(CA_TYPE_NOT_RECOGNIZED_ERR);
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
    if (caType != CAType.ROOT) {
      intermediateCaCert = getCert(caType);
    }

    String rootCaCert = getCert(CAType.ROOT);

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

  private Long getConfTimeValue(String configurationTime) {
    Matcher matcher = TIME_CONF_PATTERN.matcher(configurationTime.toLowerCase());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid time in configuration: " + configurationTime);
    }
    return Long.parseLong(matcher.group(1));
  }

  private TimeUnit getConfTimeTimeUnit(String configurationTime) {
    Matcher matcher = TIME_CONF_PATTERN.matcher(configurationTime.toLowerCase());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid time in configuration: " + configurationTime);
    }
    String timeUnitStr = matcher.group(2);
    if (null != timeUnitStr && !TIME_SUFFIXES.containsKey(timeUnitStr.toLowerCase())) {
      throw new IllegalArgumentException("Invalid time suffix in configuration: " + configurationTime);
    }
    return timeUnitStr == null ? TimeUnit.MINUTES : TIME_SUFFIXES.get(timeUnitStr.toLowerCase());
  }
}
