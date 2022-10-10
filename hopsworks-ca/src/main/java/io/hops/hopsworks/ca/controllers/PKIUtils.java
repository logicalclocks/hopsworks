/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
 */
package io.hops.hopsworks.ca.controllers;

import com.google.common.base.Strings;
import io.hops.hopsworks.ca.configuration.CAConf.CAConfKeys;
import io.hops.hopsworks.ca.persistence.PKICertificateFacade;
import io.hops.hopsworks.persistence.entity.pki.CAType;
import io.hops.hopsworks.persistence.entity.pki.PKICertificate;
import io.hops.hopsworks.restutils.RESTCodes;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStrictStyle;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.io.pem.PemObjectGenerator;
import org.bouncycastle.util.io.pem.PemWriter;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InvalidNameException;
import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Extension;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
public class PKIUtils {

  @EJB
  private io.hops.hopsworks.ca.configuration.CAConf CAConf;
  @EJB
  private PKICertificateFacade pkiCertificateFacade;

  protected final static long TEN_YEARS = 3650;
  private static final Map<String, ChronoUnit> TIME_SUFFIXES;

  private static final String CERTIFICATE_TYPE_NOT_RECOGNIZED_ERR = "Certificate type not recognized";
  private static final Pattern HOST_CERTIFICATE_SUBJECT = Pattern.compile("^(?<cn>.+)__(?<l>.+)__(?<ou>\\d+)$");
  private static final Pattern APP_CERTIFICATE_SUBJECT = Pattern.compile("^(?<cn>.+)__(?<o>app.+)__(?<ou>\\d+)$");

  static {
    TIME_SUFFIXES = new HashMap<>(5);
    TIME_SUFFIXES.put("s", ChronoUnit.SECONDS);
    TIME_SUFFIXES.put("m", ChronoUnit.MINUTES);
    TIME_SUFFIXES.put("h", ChronoUnit.HOURS);
    TIME_SUFFIXES.put("d", ChronoUnit.DAYS);
  }
  private static final Pattern TIME_CONF_PATTERN = Pattern.compile("([0-9]+)([a-z]+)?");


  public X500Name parseCertificateSubjectName(String subject, CertificateType certificateType)
      throws InvalidNameException {
    switch (certificateType) {
      case APP:
        return parseApplicationCertificateSubjectName(subject);
      case HOST:
        return parseHostCertificateSubjectName(subject);
      default:
        return parseGenericCertificateSubjectName(subject);
    }
  }

  public X500Name parseHostCertificateSubjectName(String subject) throws InvalidNameException {
    Matcher match = HOST_CERTIFICATE_SUBJECT.matcher(subject);
    if (match.matches()) {
      X500NameBuilder name = new X500NameBuilder(BCStrictStyle.INSTANCE);
      name.addRDN(BCStyle.CN, match.group("cn"));
      name.addRDN(BCStyle.L, match.group("l"));
      name.addRDN(BCStyle.OU, match.group("ou"));
      return name.build();
    }
    throw new InvalidNameException("Cannot parse Host certificate subject: " + subject);
  }

  public X500Name parseGenericCertificateSubjectName(String subject) throws InvalidNameException {
    if (Strings.isNullOrEmpty(subject)) {
      throw new InvalidNameException("Certificate subject cannot be null or empty");
    }
    X500NameBuilder name = new X500NameBuilder(BCStrictStyle.INSTANCE);
    name.addRDN(BCStyle.CN, subject);
    return name.build();
  }

  public X500Name parseApplicationCertificateSubjectName(String subject) throws InvalidNameException {
    Matcher match = APP_CERTIFICATE_SUBJECT.matcher(subject);
    if (match.matches()) {
      X500NameBuilder name = new X500NameBuilder(BCStrictStyle.INSTANCE);
      name.addRDN(BCStyle.CN, match.group("cn"));
      name.addRDN(BCStyle.O, match.group("o"));
      name.addRDN(BCStyle.OU, match.group("ou"));
      return name.build();
    }
    throw new InvalidNameException("Cannot parse Application certificate subject: " + subject);
  }

  public TemporalAmount getValidityPeriod(CertificateType type) {
    switch (type) {
      case APP:
        return getAppCertificateValidityPeriod();
      case HOST:
        return getServiceCertificateValidityPeriod();
      case KUBE:
      case PROJECT:
        return Duration.ofSeconds(TimeUnit.SECONDS.convert(TEN_YEARS, TimeUnit.DAYS));
      default:
        throw new IllegalArgumentException(CERTIFICATE_TYPE_NOT_RECOGNIZED_ERR);
    }
  }

  private TemporalAmount getServiceCertificateValidityPeriod() {
    long validity = -1;
    if (!CAConf.getBoolean(CAConfKeys.SERVICE_KEY_ROTATION_ENABLED)){
      validity = TimeUnit.SECONDS.convert(TEN_YEARS, TimeUnit.DAYS);
    } else {
      // Add 4 days just to be sure.
      validity = getCertificateValidityInS(CAConf.getString(CAConfKeys.SERVICE_KEY_ROTATION_INTERVAL) +
          TimeUnit.SECONDS.convert(4, TimeUnit.DAYS));
    }

    return Duration.ofSeconds(validity);
  }

  private TemporalAmount getAppCertificateValidityPeriod() {
    long s = getCertificateValidityInS(CAConf.getString(CAConfKeys.APPLICATION_CERTIFICATE_VALIDITY_PERIOD));
    return Duration.ofSeconds(s);
  }

  protected long getCertificateValidityInS(String rawConfigurationProperty) {
    Long timeValue = getConfTimeValue(rawConfigurationProperty);
    ChronoUnit unitValue = getConfTimeTimeUnit(rawConfigurationProperty);
    return Duration.of(timeValue, unitValue).getSeconds();
  }

  /**
   * This function provides a mapping between certificate types and the corresponding CA
   * @param certType
   * @return
   */
  public CAType getResponsibleCA(CertificateType certType) {
    switch (certType) {
      case HOST: case APP: case PROJECT:
        return CAType.INTERMEDIATE;
      case KUBE:
        return CAType.KUBECA;
      default:
        throw new IllegalArgumentException(CERTIFICATE_TYPE_NOT_RECOGNIZED_ERR);
    }
  }

  public List<String> findAllHostCertificateSubjectsForHost(String hostname) {
    return pkiCertificateFacade.findAllSubjectsWithStatusAndPartialSubject(String.format("CN=%s", hostname),
        PKICertificate.Status.VALID);
  }

  public List<String> findAllValidSubjectsWithPartialMatch(String partialSubject) {
    return pkiCertificateFacade.findAllSubjectsWithStatusAndPartialSubject(partialSubject, PKICertificate.Status.VALID);
  }

  public String convertToPEM(X509Extension certificate) throws IOException {
    try (StringWriter sw = new StringWriter()) {
      PemWriter pw = new JcaPEMWriter(sw);
      PemObjectGenerator pog = new JcaMiscPEMGenerator(certificate);
      pw.writeObject(pog.generate());
      pw.flush();
      sw.flush();
      pw.close();
      return sw.toString();
    }
  }

  public CAException csrSigningExceptionConvertToCAException(Throwable e, CertificateType certType) {
    if (e instanceof CAInitializationException) {
      return new CAException(RESTCodes.CAErrorCode.CA_INITIALIZATION_ERROR, Level.SEVERE, certType,
          "Failed to initialize CA", "Failed to initialize CA", e.getCause());
    }
    if (e instanceof CertificateEncodingException) {
      return new CAException(RESTCodes.CAErrorCode.BADSIGNREQUEST, Level.FINE, certType,
          "Empty or malformed CSR", "Could not parse CSR to PKCS10CertificationRequest", e);
    }
    if (e instanceof CertificationRequestValidationException) {
      return new CAException(RESTCodes.CAErrorCode.BADSIGNREQUEST, Level.FINE, certType,
          e.getMessage(), e.getMessage(), e);
    }
    if (e instanceof KeyException || e instanceof SignatureException || e instanceof CACertificateNotFoundException) {
      return new CAException(RESTCodes.CAErrorCode.CSR_GENERIC_ERROR, Level.SEVERE, certType,
          "Generic PKI error", "Error while signing CSR. Check logs", e);
    }
    if (e instanceof CertificateAlreadyExistsException) {
      return new CAException(RESTCodes.CAErrorCode.CERTEXISTS, Level.FINE, certType,
          "Certificate with the same X.509 Subject name already exists", "Certificat with same Subject", e);
    }
    if (e instanceof NoSuchAlgorithmException || e instanceof CertIOException
        || e instanceof OperatorCreationException || e instanceof CertificateException) {
      return new CAException(RESTCodes.CAErrorCode.CSR_SIGNING_ERROR, Level.SEVERE, certType,
          "Failed to sign CSR", "Failed to sign CSR. Check logs", e);
    }
    return new CAException(RESTCodes.CAErrorCode.CSR_GENERIC_ERROR, Level.SEVERE, certType,
        "Unknown error while signing CSR", "Unknown error while signing CSR. Check logs", e);
  }

  public CAException certificateRevocationExceptionConvertToCAException(Throwable e, CertificateType certType) {
    if (e instanceof CAInitializationException) {
      return new CAException(RESTCodes.CAErrorCode.CA_INITIALIZATION_ERROR, Level.SEVERE, certType,
          "Failed to initialize CA", "Failed to initialize CA", e.getCause());
    }
    if (e instanceof InvalidNameException) {
      return new CAException(RESTCodes.CAErrorCode.BAD_SUBJECT_NAME, Level.FINE, CertificateType.APP, "Bad " +
          "certificate identifier to revoke", e.getMessage(), e);
    }
    if (e instanceof CertificateNotFoundException) {
      return new CAException(RESTCodes.CAErrorCode.CERTNOTFOUND, Level.FINE, CertificateType.APP,
          e.getMessage(), e.getMessage(), e);
    }
    if (e instanceof CertificateException) {
      return new CAException(RESTCodes.CAErrorCode.CERTIFICATE_DECODING_ERROR, Level.FINE, CertificateType.APP,
          e.getMessage(), e.getMessage(), e);
    }
    if (e instanceof CRLException || e instanceof KeyException) {
      return new CAException(RESTCodes.CAErrorCode.CERTIFICATE_REVOCATION_FAILURE, Level.SEVERE, CertificateType.APP,
          "Failed to revoke certificate", e.getMessage(), e);
    }
    return new CAException(RESTCodes.CAErrorCode.CERTIFICATE_REVOCATION_FAILURE, Level.SEVERE, certType,
        "Unknown error while revoking certificate", e.getMessage(), e);
  }

  public CAException certificateLoadingExceptionConvertToCAException(Throwable e) {
    if (e instanceof CertificateNotFoundException) {
      return new CAException(RESTCodes.CAErrorCode.CERTNOTFOUND, Level.FINE, null, e.getMessage(), e.getMessage(), e);
    }
    if (e instanceof CertificateException) {
      return new CAException(RESTCodes.CAErrorCode.CERTIFICATE_DECODING_ERROR, Level.SEVERE, null,
          "Could not decode certificate", e.getMessage(), e);
    }
    return new CAException(RESTCodes.CAErrorCode.PKI_GENERIC_ERROR, Level.SEVERE, null, "Generic PKI error",
        e.getMessage(), e);
  }

  public Duration parseDuration(String duration) {
    return Duration.of(getConfTimeValue(duration), getConfTimeTimeUnit(duration));
  }

  private Long getConfTimeValue(String configurationTime) {
    Matcher matcher = TIME_CONF_PATTERN.matcher(configurationTime.toLowerCase());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid time in configuration: " + configurationTime);
    }
    return Long.parseLong(matcher.group(1));
  }

  private ChronoUnit getConfTimeTimeUnit(String configurationTime) {
    Matcher matcher = TIME_CONF_PATTERN.matcher(configurationTime.toLowerCase());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid time in configuration: " + configurationTime);
    }
    String timeUnitStr = matcher.group(2);
    if (null != timeUnitStr && !TIME_SUFFIXES.containsKey(timeUnitStr.toLowerCase())) {
      throw new IllegalArgumentException("Invalid time suffix in configuration: " + configurationTime);
    }
    return timeUnitStr == null ? ChronoUnit.MINUTES : TIME_SUFFIXES.get(timeUnitStr.toLowerCase());
  }
}
