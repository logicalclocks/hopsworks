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

import com.google.common.annotations.VisibleForTesting;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.lock.FencedLock;
import io.hops.hadoop.shaded.com.google.gson.Gson;
import io.hops.hopsworks.ca.configuration.CAConf;
import io.hops.hopsworks.ca.configuration.CAConfiguration;
import io.hops.hopsworks.ca.configuration.CAsConfiguration;
import io.hops.hopsworks.ca.configuration.KubeCAConfiguration;
import io.hops.hopsworks.ca.configuration.SubjectAlternativeName;
import io.hops.hopsworks.ca.configuration.UsernamesConfiguration;
import io.hops.hopsworks.ca.persistence.CRLFacade;
import io.hops.hopsworks.ca.persistence.KeyFacade;
import io.hops.hopsworks.ca.persistence.PKICertificateFacade;
import io.hops.hopsworks.ca.persistence.SerialNumberFacade;
import io.hops.hopsworks.persistence.entity.pki.CAType;
import io.hops.hopsworks.persistence.entity.pki.KeyIdentifier;
import io.hops.hopsworks.persistence.entity.pki.PKICertificate;
import io.hops.hopsworks.persistence.entity.pki.PKICertificateId;
import io.hops.hopsworks.persistence.entity.pki.PKICrl;
import io.hops.hopsworks.persistence.entity.pki.PKIKey;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.Utilities;
import io.hops.hopsworks.servicediscovery.tags.MysqlTags;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStrictStyle;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.naming.InvalidNameException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@DependsOn("CAConf")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PKI {
  private static final Logger LOGGER = Logger.getLogger(PKI.class.getName());
  private final RSAKeyGenParameterSpec keyGenSpecs = new RSAKeyGenParameterSpec(4096, RSAKeyGenParameterSpec.F4);
  private final Map<CAType, KeyPair> caKeys = new HashMap<>(3);
  private final Map<CAType, X509Certificate> caCertificates = new HashMap<>(3);

  public static final String SIGNATURE_ALGORITHM = "SHA256WithRSAEncryption";
  private static final CRLReason REVOCATION_REASON = CRLReason.lookup(CRLReason.privilegeWithdrawn);

  private final AtomicBoolean CA_INITIALIZED = new AtomicBoolean(false);
  
  private static final GeneralName[] EMTPY_GENERAL_NAMES = new GeneralName[0];

  @EJB
  private SerialNumberFacade serialNumberFacade;
  @EJB
  private KeyFacade keyFacade;
  @EJB
  private PKICertificateFacade pkiCertificateFacade;
  @EJB
  private PKIUtils pkiUtils;
  @EJB
  private CRLFacade crlFacade;
  @EJB
  private CAConf caConf;
  @EJB
  private UsernamesConfiguration usernamesConfiguration;
  @Inject
  private HazelcastInstance hazelcastInstance;

  private KeyPairGenerator keyPairGenerator;
  private KeyFactory keyFactory;
  private JcaX509CertificateConverter converter;
  private JcaX509CRLConverter crlConverter;
  private JcaPEMKeyConverter pemKeyConverter;
  private CAsConfiguration conf;
  private static final Map<CAType, X500Name> CA_SUBJECT_NAME = new HashMap<>(3);
  private static final String CA_INIT_LOCK = "caInitLock";

  static {
    X500NameBuilder rootNameBuilder = new X500NameBuilder(BCStrictStyle.INSTANCE);
    rootNameBuilder.addRDN(BCStyle.C, "SE");
    rootNameBuilder.addRDN(BCStyle.O, "Hopsworks");
    rootNameBuilder.addRDN(BCStyle.OU, "core");
    rootNameBuilder.addRDN(BCStyle.CN, "HopsRootCA");
    X500Name rootCAName = rootNameBuilder.build();
    CA_SUBJECT_NAME.put(CAType.ROOT, rootCAName);

    X500NameBuilder intermediateNameBuilder = new X500NameBuilder(BCStrictStyle.INSTANCE);
    intermediateNameBuilder.addRDN(BCStyle.C, "SE");
    intermediateNameBuilder.addRDN(BCStyle.O, "Hopsworks");
    intermediateNameBuilder.addRDN(BCStyle.OU, "core");
    intermediateNameBuilder.addRDN(BCStyle.CN, "HopsIntermediateCA");
    X500Name intermediateCAName = intermediateNameBuilder.build();
    CA_SUBJECT_NAME.put(CAType.INTERMEDIATE, intermediateCAName);

    X500NameBuilder kubeNameBuilder = new X500NameBuilder(BCStrictStyle.INSTANCE);
    kubeNameBuilder.addRDN(BCStyle.C, "SE");
    kubeNameBuilder.addRDN(BCStyle.O, "Hopsworks");
    kubeNameBuilder.addRDN(BCStyle.OU, "core");
    kubeNameBuilder.addRDN(BCStyle.CN, "KubeHopsIntermediateCA");
    X500Name kubeName = kubeNameBuilder.build();
    CA_SUBJECT_NAME.put(CAType.KUBECA, kubeName);
  }

  @PostConstruct
  public void init() {
    try {
      Security.addProvider(new BouncyCastleProvider());
      Provider[] providers = Security.getProviders();

      keyPairGenerator = KeyPairGenerator
          .getInstance("RSA", new BouncyCastleProvider());
      keyPairGenerator.initialize(keyGenSpecs);

      keyFactory = KeyFactory.getInstance("RSA");
      converter = new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider());
      crlConverter = new JcaX509CRLConverter().setProvider(new BouncyCastleProvider());
      pemKeyConverter = new JcaPEMKeyConverter().setProvider(new BouncyCastleProvider());
      configure();
    } catch (GeneralSecurityException ex) {
      throw new RuntimeException("Failed to initialize PKI", ex);
    }
  }
  
  private FencedLock getLock() {
    if (hazelcastInstance != null && hazelcastInstance.getCluster().getMembers().size() > 1) {
      return hazelcastInstance.getCPSubsystem().getLock(CA_INIT_LOCK);
    } else {
      return null;
    }
  }

  public void configure() {
    conf = loadConfiguration();
    overrideCAX500Names(conf);
  }

  protected static final CAsConfiguration EMPTY_CONFIGURATION = new CAsConfiguration(null, null, null);

  protected CAsConfiguration loadConfiguration() {
    String rawConf = caConf.getString(CAConf.CAConfKeys.CA_CONFIGURATION);
    if (rawConf.isEmpty()) {
      return EMPTY_CONFIGURATION;
    }
    Gson gson = new Gson();
    return gson.fromJson(rawConf, CAsConfiguration.class);
  };

  protected void overrideCAX500Names(CAsConfiguration conf) {
    conf.getRootCA().flatMap(CAConfiguration::getX509Name).ifPresent((v) -> CA_SUBJECT_NAME.put(CAType.ROOT,
        new X500Name(BCStyle.INSTANCE, v)));
    conf.getIntermediateCA().flatMap(CAConfiguration::getX509Name)
        .ifPresent((v) -> CA_SUBJECT_NAME.put(CAType.INTERMEDIATE, new X500Name(BCStyle.INSTANCE, v)));
    conf.getKubernetesCA().flatMap(CAConfiguration::getX509Name).ifPresent((v) -> CA_SUBJECT_NAME.put(CAType.KUBECA,
        new X500Name(BCStyle.INSTANCE, v)));
  }

  protected void maybeInitializeCA() throws GeneralSecurityException, IOException, OperatorCreationException {
    if (!CA_INITIALIZED.getAndSet(true)) {
      FencedLock lock = getLock();
      if(lock == null || lock.tryLock(3, TimeUnit.MINUTES)) {
        try {
          LOGGER.log(Level.INFO, "Initializing CAs");
          initializeCertificateAuthorities();
        } catch (Exception ex) {
          CA_INITIALIZED.set(false);
          LOGGER.log(Level.SEVERE, "Error initializing CAs", ex);
          throw ex;
        } finally {
          if (lock != null ) {
            lock.unlock();
          }
        }
      } else {
        CA_INITIALIZED.set(false);
        LOGGER.log(Level.WARNING, "Timed out waiting for lock to initializing CAs");
      }
    } else {
      LOGGER.log(Level.FINE, "CAs already initialized");
    }
  }

  public Pair<String, String> getChainOfTrust(CAType type) throws CAInitializationException, IOException,
      GeneralSecurityException {
    try {
      maybeInitializeCA();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Failed to initialize CA", ex);
      throw new CAInitializationException(ex);
    }
    String intermediateCert = null;
    if (type != CAType.ROOT) {
      intermediateCert = pkiUtils.convertToPEM(caCertificates.get(type));
    }
    String rootCert = pkiUtils.convertToPEM(caCertificates.get(CAType.ROOT));
    return Pair.of(rootCert, intermediateCert);
  }

  public String getCertificateRevocationListPEM(CAType type) throws CAInitializationException,
      GeneralSecurityException, IOException {
    try {
      maybeInitializeCA();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Failed to initialize CA", ex);
      throw new CAInitializationException(ex);
    }
    X509CRL crl = loadCRL(type);
    return pkiUtils.convertToPEM(crl);
  }

  protected void initializeCertificateAuthorities() throws GeneralSecurityException, IOException,
      OperatorCreationException {
    for (CAType ca : CAType.values()) {
      if (ca.equals(CAType.KUBECA)) {
        if (!caConf.getBoolean(CAConf.CAConfKeys.KUBERNETES)
            || !caConf.getString(CAConf.CAConfKeys.KUBERNETES_TYPE).equals("local")) {
          continue;
        }
      }
      initializeCertificateAuthority(ca);
    }
  }

  private void initializeCertificateAuthority(CAType caType) throws IOException, GeneralSecurityException,
      OperatorCreationException {
    caInitializeSerialNumber(caType);

    caInitializeKeys(caType);

    caInitializeCertificate(caType);

    caInitializeCRL(caType);
  }

  protected void caInitializeSerialNumber(CAType type) throws IOException {
    if (!serialNumberFacade.isInitialized(type)) {
      if (loadFromFile()) {
        LOGGER.log(Level.INFO, "Loading serial number from file for: " + type);
        try {
          Path path = getPathToSerialNumber(type);
          migrateSerialNumber(type, path);
          return;
        } catch (FileNotFoundException ex) {
          throw new IOException("Bootstrapping serial number of " + type + " but openssl file could not be found", ex);
        }
      }
      serialNumberFacade.initialize(type);
    }
  }

  @VisibleForTesting
  protected Path getPathToSerialNumber(CAType type) throws FileNotFoundException {
    Path path;
    switch (type) {
      case ROOT:
        path = Paths.get(caConf.getString(CAConf.CAConfKeys.CERTS_DIR), "serial");
        break;
      case INTERMEDIATE:
        path = Paths.get(caConf.getString(CAConf.CAConfKeys.CERTS_DIR), "intermediate/serial");
        break;
      case KUBECA:
        path = Paths.get(caConf.getString(CAConf.CAConfKeys.CERTS_DIR), "kube/serial");
        break;
      default:
        throw new IllegalArgumentException("Unknown CA type: " + type);
    }
    if (!path.toFile().exists()) {
      throw new FileNotFoundException("File " + path.toFile() + " does not exist");
    }
    return path;
  }

  @VisibleForTesting
  protected void migrateSerialNumber(CAType type, Path path) throws IOException {
    Long sn = getSerialNumber(path);
    serialNumberFacade.initializeWithNumber(type, sn);
    LOGGER.log(Level.INFO, "Migrated Serial Number for " + type + " with next number " + sn);
  }

  private Long getSerialNumber(Path path) throws IOException {
    String hex = FileUtils.readFileToString(path.toFile(), Charset.defaultCharset());
    return Long.parseUnsignedLong(hex.trim(), 16);
  }

  @VisibleForTesting
  protected boolean loadFromFile() {
    return Paths.get(caConf.getString(CAConf.CAConfKeys.CERTS_DIR), "private/ca.key.pem").toFile().exists();
  }

  protected void caInitializeKeys(CAType type) throws InvalidKeySpecException, IOException {
    Pair<Boolean, KeyPair> kp = loadOrGenerateKeypair(type.name());
    if (!kp.getLeft()) {
      LOGGER.log(Level.INFO, "Saving key pair for " + type.name());
      PKIKey privateKey = new PKIKey(new KeyIdentifier(type.name(), PKIKey.Type.PRIVATE),
          kp.getRight().getPrivate().getEncoded());
      PKIKey publicKey = new PKIKey(new KeyIdentifier(type.name(), PKIKey.Type.PUBLIC),
          kp.getRight().getPublic().getEncoded());
      saveKeys(privateKey, publicKey);
    }
    caKeys.put(type, kp.getRight());
  }

  protected void caInitializeCertificate(CAType type) throws IOException, GeneralSecurityException,
      OperatorCreationException {
    Pair<Boolean, X509Certificate> certificatePair = loadOrGenerateCACertificate(type);
    X509Certificate certificate = certificatePair.getRight();
    if (!certificatePair.getLeft()) {
      LOGGER.log(Level.INFO, "Saving certificate for " + type);
      saveNewCertificate(CAType.ROOT, certificate);
    }
    caCertificates.put(type, certificate);
  }

  protected void caInitializeCRL(CAType type) throws GeneralSecurityException, CertIOException, IOException {
    LOGGER.log(Level.INFO, "Initializing Certificate Revocation List for " + type);
    if (crlFacade.exist(type)) {
      LOGGER.log(Level.INFO, "Skip CRL initialization for " + type + " as it already exists");
      return;
    }

    if (loadFromFile() && type.equals(CAType.INTERMEDIATE)) {
      LOGGER.log(Level.INFO, "Loading CRL of " + type + " from file");
      X509CRL crl = loadCRL(Paths.get(caConf.getString(CAConf.CAConfKeys.CERTS_DIR), "intermediate/crl/intermediate" +
          ".crl.pem"));
      if (crl != null) {
        initCRL(type, crl);
        return;
      } else {
        LOGGER.log(Level.WARNING, "CRL loaded from file for " + type + " is empty, continue generating new one");
      }
    }
    KeyPair keyPair = getCAKeyPair(type);
    X509Certificate certificate = getCACertificate(type);
    Instant now = Instant.now();
    LOGGER.log(Level.INFO, "Generating initial CRL for " + type);
    X509v2CRLBuilder builder = new JcaX509v2CRLBuilder(certificate, Date.from(now));
    Instant nextUpdate = now.plus(1, ChronoUnit.DAYS);
    builder.setNextUpdate(Date.from(nextUpdate));

    JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
    builder.addExtension(Extension.authorityKeyIdentifier, false,
        extUtils.createAuthorityKeyIdentifier(certificate));
    try {
      ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
          .setProvider(new BouncyCastleProvider())
          .build(keyPair.getPrivate());
      X509CRLHolder holder = builder.build(signer);
      X509CRL crl = crlConverter.getCRL(holder);
      initCRL(type, crl);
      LOGGER.log(Level.INFO, "Finished initializing CRL for " + type);
    } catch (OperatorCreationException ex) {
      throw new GeneralSecurityException(ex);
    }
  }

  protected void initCRL(CAType type, X509CRL crl) throws CRLException {
    PKICrl pkiCrl = new PKICrl(type, crl.getEncoded());
    crlFacade.init(pkiCrl);
  }

  protected void updateCRL(CAType type, X509CRL crl) throws CRLException {
    PKICrl pkiCrl = new PKICrl(type, crl.getEncoded());
    crlFacade.update(pkiCrl);
  }

  protected void saveNewCertificate(CAType caType, X509Certificate certificate) throws CertificateEncodingException {
    Long serialNumber = certificate.getSerialNumber().longValue();
    String subject = certificate.getSubjectDN().toString();
    byte[] certEncoded = certificate.getEncoded();
    PKICertificateId id = new PKICertificateId(PKICertificate.Status.VALID, subject);
    PKICertificate certificateToSave = new PKICertificate(id, caType, serialNumber, certEncoded,
        certificate.getNotBefore(), certificate.getNotAfter());
    pkiCertificateFacade.saveCertificate(certificateToSave);
  }

  protected Pair<Boolean, KeyPair> loadOrGenerateKeypair(String owner) throws InvalidKeySpecException, IOException {
    LOGGER.log(Level.INFO, "Loading key pair for " + owner);
    Optional<KeyPair> kp = loadKeyPair(owner);
    if (kp.isPresent()) {
      LOGGER.log(Level.INFO, "Loaded key pair for " + owner);
      return Pair.of(true, kp.get());
    }
    if (loadFromFile()) {
      LOGGER.log(Level.INFO, "Key pair for " + owner + " does NOT exist but will load from file");
      try {
        return Pair.of(false, loadKeyPairFromFile(owner));
      } catch (FileNotFoundException ex) {
        throw new InvalidKeySpecException("Bootstrapping private key of " + owner + " but openssl file could not be" +
            " found", ex);
      }
    }
    LOGGER.log(Level.INFO, "Key pair for " + owner + " does NOT exist, generating new");
    return Pair.of(false, generateKeyPair());
  }

  protected KeyPair loadKeyPairFromFile(String owner) throws IOException {
    Path keyPath = getPathToPrivateKey(owner);
    return loadKeyPair(keyPath, getPrivateFileKeyPassword(owner));
  }

  private String getPrivateFileKeyPassword(String owner) {
    if (owner.toUpperCase().equals(CAType.KUBECA.toString())) {
      return caConf.getString(CAConf.CAConfKeys.KUBE_CA_PASSWORD);
    }
    return caConf.getString(CAConf.CAConfKeys.HOPSWORKS_SSL_MASTER_PASSWORD);
  }

  private Path getPathToPrivateKey(String owner) throws FileNotFoundException {
    Path path;
    switch (CAType.valueOf(owner)) {
      case ROOT:
        path = Paths.get(caConf.getString(CAConf.CAConfKeys.CERTS_DIR), "private/ca.key.pem");
        break;
      case INTERMEDIATE:
        path = Paths.get(caConf.getString(CAConf.CAConfKeys.CERTS_DIR), "intermediate/private/intermediate.key.pem");
        break;
      case KUBECA:
        path = Paths.get(caConf.getString(CAConf.CAConfKeys.CERTS_DIR), "kube/private/kube-ca.key.pem");
        break;
      default:
        throw new IllegalArgumentException("Unknown private key owner: " + owner);
    }
    if (!path.toFile().exists()) {
      throw new FileNotFoundException("File " + path.toFile() + " does not exist");
    }
    return path;
  }

  private KeyPair loadKeyPair(Path path, String password) throws IOException  {
    try (PEMParser pemParser = new PEMParser(new FileReader(path.toFile()))) {
      Object object = pemParser.readObject();
      KeyPair kp;
      if (object instanceof PEMEncryptedKeyPair) {
        PEMEncryptedKeyPair ekp = (PEMEncryptedKeyPair) object;
        PEMDecryptorProvider decryptorProvider = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
        kp = pemKeyConverter.getKeyPair(ekp.decryptKeyPair(decryptorProvider));
      } else {
        PEMKeyPair ukp = (PEMKeyPair) object;
        kp = pemKeyConverter.getKeyPair(ukp);
      }
      return kp;
    }
  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  protected KeyPair generateKeyPair() {
    return keyPairGenerator.generateKeyPair();
  }

  protected Optional<KeyPair> loadKeyPair(String owner) throws InvalidKeySpecException {
    byte[] privateKey = keyFacade.getEncodedKey(owner, PKIKey.Type.PRIVATE);
    if (privateKey != null) {
      byte[] publicKey = keyFacade.getEncodedKey(owner, PKIKey.Type.PUBLIC);
      return Optional.of(restoreKeypair(privateKey, publicKey));
    }
    return Optional.empty();
  }

  protected void saveKeys(PKIKey privateKey, PKIKey publicKey) {
    keyFacade.saveKey(privateKey);
    keyFacade.saveKey(publicKey);
    LOGGER.log(Level.INFO, "Saved keys");
  }

  private KeyPair restoreKeypair(byte[] privateKey, byte[] publicKey) throws InvalidKeySpecException {
    PrivateKey prK = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKey));
    PublicKey puK = keyFactory.generatePublic(new X509EncodedKeySpec(publicKey));
    return new KeyPair(puK, prK);
  }

  protected Pair<Boolean, X509Certificate> loadOrGenerateCACertificate(CAType type) throws IOException,
      CertificateException, NoSuchAlgorithmException, CertificateException, KeyException, OperatorCreationException {
    LOGGER.log(Level.INFO, "Loading certificate for " + type);

    X500Name name = CA_SUBJECT_NAME.get(type);
    Optional<X509Certificate> cert = loadCertificate(name.toString());
    if (cert.isPresent()) {
      LOGGER.log(Level.INFO, "Loaded ROOT CA certificate");
      return Pair.of(true, cert.get());
    }

    if (loadFromFile()) {
      LOGGER.log(Level.INFO, "Loading certificate of " + type + " CA from file");
      try {
        return Pair.of(false, loadCACertificate(type));
      } catch (FileNotFoundException ex) {
        throw new CertificateException("Bootstrapping certificate of " + type + " CA but openssl file could not be " +
            "found", ex);
      }
    }

    switch (type) {
      case ROOT:
        LOGGER.log(Level.INFO, "Root CA certificate does not exist, generating...");
        return Pair.of(false, generateRootCACertificate());
      case INTERMEDIATE:
        LOGGER.log(Level.INFO, "Intermediate CA certificate does not exist, generating...");
        return Pair.of(false, generateCertificate(prepareIntermediateCAGenerationParams()));
      case KUBECA:
        LOGGER.log(Level.INFO, "Kubernetes CA certificate does not exist, generating...");
        return Pair.of(false, generateCertificate(prepareKubernetesCAGenerationParams()));
      default:
        throw new RuntimeException("Unknown CA type " + type);
    }
  }

  @VisibleForTesting
  protected X509Certificate loadCACertificate(CAType type) throws IOException, CertificateException {
    Path certPath = getCACertificatePath(type);
    return loadCertificate(certPath);
  }

  private Path getCACertificatePath(CAType type) throws FileNotFoundException {
    Path path;
    switch (type) {
      case ROOT:
        path = Paths.get(caConf.getString(CAConf.CAConfKeys.CERTS_DIR), "certs/ca.cert.pem");
        break;
      case INTERMEDIATE:
        path = Paths.get(caConf.getString(CAConf.CAConfKeys.CERTS_DIR), "intermediate/certs/intermediate.cert.pem");
        break;
      case KUBECA:
        path = Paths.get(caConf.getString(CAConf.CAConfKeys.CERTS_DIR), "kube/hopsworks/kube-ca.cert.pem");
        break;
      default:
        throw new IllegalArgumentException("Unknown CA type: " + type);
    }
    if (!path.toFile().exists()) {
      throw new FileNotFoundException("File " + path.toFile() + " does not exist");
    }
    return path;
  }

  private X509Certificate loadCertificate(Path path) throws IOException, CertificateException {
    try (PEMParser pemParser = new PEMParser(new FileReader(path.toFile()))) {
      Object object = pemParser.readObject();
      if (object instanceof X509CertificateHolder) {
        return converter.getCertificate((X509CertificateHolder) object);
      }
      return null;
    }
  }

  protected Optional<X509Certificate> loadCertificate(String subject) throws IOException, CertificateException {
    LOGGER.log(Level.INFO, "Loading certificate with subject " + subject);
    Optional<PKICertificate> pkiCert = pkiCertificateFacade.findBySubjectAndStatus(subject,
        PKICertificate.Status.VALID);
    if (!pkiCert.isPresent()) {
      LOGGER.log(Level.INFO, "There is no certificate with subject: " + subject);
      return Optional.empty();
    }
    LOGGER.log(Level.INFO, "Found encoded certificate and decoding it to " + X509Certificate.class.getName());
    byte[] encoded = pkiCert.get().getCertificate();
    X509Certificate certificate = converter.getCertificate(new X509CertificateHolder(encoded));
    return Optional.of(certificate);
  }

  private final Duration ROOT_CA_DEFAULT_VALIDITY_PERIOD = Duration.of(PKIUtils.TEN_YEARS, ChronoUnit.DAYS);
  private final Duration INTERMEDIATE_CA_DEFAULT_VALIDITY_PERIOD = Duration.of(PKIUtils.TEN_YEARS, ChronoUnit.DAYS);
  private final Duration KUBERNETES_CA_DEFAULT_VALIDITY_PERIOD = Duration.of(PKIUtils.TEN_YEARS, ChronoUnit.DAYS);
  private Duration getCAValidityPeriod(Optional<? extends CAConfiguration> conf, Duration defaultDuration) {
    if (conf.isPresent() && conf.get().getValidityDuration().isPresent()) {
      return pkiUtils.parseDuration(conf.get().getValidityDuration().get());
    }
    return defaultDuration;
  }

  protected X509Certificate generateRootCACertificate()
      throws KeyException, NoSuchAlgorithmException, CertificateException, CertIOException, OperatorCreationException {
    KeyPair keyPair = caKeys.get(CAType.ROOT);
    if (keyPair == null) {
      String msg = "Could not find Root CA key pair in cache. Have you initialized?";
      LOGGER.log(Level.SEVERE, msg);
      throw new KeyException(msg);
    }

    Long sn = serialNumberFacade.nextSerialNumber(CAType.ROOT);
    Duration validityPeriod =  getCAValidityPeriod(conf.getRootCA(), ROOT_CA_DEFAULT_VALIDITY_PERIOD);
    Instant notBefore = Instant.now().minus(3, ChronoUnit.MINUTES);
    Instant notAfter = notBefore.plus(validityPeriod);
    X500Name name = CA_SUBJECT_NAME.get(CAType.ROOT);
    X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
        name,
        BigInteger.valueOf(sn),
        Date.from(notBefore),
        Date.from(notAfter),
        name,
        keyPair.getPublic());
    JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
    SubjectKeyIdentifier subjectKeyIdentifier = extUtils.createSubjectKeyIdentifier(keyPair.getPublic());
    AuthorityKeyIdentifier authorityKeyIdentifier = extUtils.createAuthorityKeyIdentifier(keyPair.getPublic());
    builder
        .addExtension(Extension.basicConstraints, true, new BasicConstraints(10))
        .addExtension(Extension.keyUsage, true,
            new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign | KeyUsage.digitalSignature))
        .addExtension(Extension.subjectKeyIdentifier, false, subjectKeyIdentifier)
        .addExtension(Extension.authorityKeyIdentifier, false, authorityKeyIdentifier);
    ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
        .setProvider(new BouncyCastleProvider())
        .build(keyPair.getPrivate());
    X509CertificateHolder holder = builder.build(signer);
    X509Certificate certificate = converter.getCertificate(holder);
    LOGGER.log(Level.INFO, "Generated ROOT CA certificate");
    return certificate;
  }

  protected CertificateGenerationParameters prepareIntermediateCAGenerationParams()
      throws KeyException, CertificateException {
    KeyPair rootCAKeypair = getCAKeyPair(CAType.ROOT);
    X509Certificate rootCACert = getCACertificate(CAType.ROOT);
    final CertificateSigner signer = new CertificateSigner(rootCAKeypair, rootCACert);
    KeyPair intermediateKeypair = getCAKeyPair(CAType.INTERMEDIATE);
    Long serialNumber = serialNumberFacade.nextSerialNumber(CAType.ROOT);
    X500Name name = CA_SUBJECT_NAME.get(CAType.INTERMEDIATE);

    Duration validityDuration = getCAValidityPeriod(conf.getIntermediateCA(), INTERMEDIATE_CA_DEFAULT_VALIDITY_PERIOD);
    Instant notBefore = Instant.now().minus(3, ChronoUnit.MINUTES);
    Instant notAfter = notBefore.plus(validityDuration);

    final CertificateValidityPeriod validityPeriod = new CertificateValidityPeriod(notBefore, notAfter);

    return new CertificateGenerationParameters(
        signer,
        intermediateKeypair,
        serialNumber,
        name,
        validityPeriod,
        INTERMEDIATE_EXTENSIONS);
  }

  protected CertificateGenerationParameters prepareKubernetesCAGenerationParams()
      throws KeyException, CertificateException {
    KeyPair rootCAKeypair = getCAKeyPair(CAType.ROOT);
    X509Certificate rootCACert = getCACertificate(CAType.ROOT);
    final CertificateSigner signer = new CertificateSigner(rootCAKeypair, rootCACert);
    KeyPair intermediateKeypair = getCAKeyPair(CAType.KUBECA);
    Long serialNumber = serialNumberFacade.nextSerialNumber(CAType.ROOT);
    X500Name name = CA_SUBJECT_NAME.get(CAType.KUBECA);

    Duration validityDuration = getCAValidityPeriod(conf.getKubernetesCA(), KUBERNETES_CA_DEFAULT_VALIDITY_PERIOD);
    Instant notBefore = Instant.now().minus(3, ChronoUnit.MINUTES);
    Instant notAfter = notBefore.plus(validityDuration);

    final CertificateValidityPeriod validityPeriod = new CertificateValidityPeriod(notBefore, notAfter);

    return new CertificateGenerationParameters(
        signer,
        intermediateKeypair,
        serialNumber,
        name,
        validityPeriod,
        INTERMEDIATE_EXTENSIONS);
  }

  protected X509Certificate generateCertificate(CertificateGenerationParameters params)
      throws NoSuchAlgorithmException, CertificateException, CertIOException, OperatorCreationException {

    CertificateSigner signer = params.signer;
    Long sn = params.serialNumber;

    X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
        signer.certificate,
        BigInteger.valueOf(sn),
        Date.from(params.validityPeriod.notBefore),
        Date.from(params.validityPeriod.notAfter),
        params.x500Name,
        params.ownerKeypair.getPublic());
    JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
    SubjectKeyIdentifier subjectKeyIdentifier = extUtils.createSubjectKeyIdentifier(params.ownerKeypair.getPublic());
    AuthorityKeyIdentifier authorityKeyIdentifier = extUtils.createAuthorityKeyIdentifier(signer.keyPair.getPublic());
    builder
        .addExtension(Extension.subjectKeyIdentifier, false, subjectKeyIdentifier)
        .addExtension(Extension.authorityKeyIdentifier, false, authorityKeyIdentifier);
    try {
      params.populateExtensions.apply(builder);
    } catch (RuntimeException ex) {
      throw new CertIOException("Failed to add certificate extensions", ex.getCause());
    }

    ContentSigner contentSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
        .setProvider(new BouncyCastleProvider())
        .build(signer.keyPair.getPrivate());
    X509CertificateHolder holder = builder.build(contentSigner);
    X509Certificate certificate = converter.getCertificate(holder);
    LOGGER.log(Level.INFO, "Generated certificate");
    return certificate;
  }

  public X509Certificate signCertificateSigningRequest(String csrStr, CertificateType certificateType)
      throws CAInitializationException, IOException, CertificateEncodingException, CACertificateNotFoundException,
      CertificateAlreadyExistsException, KeyException, NoSuchAlgorithmException, CertIOException,
      OperatorCreationException, CertificateException, InvalidKeyException, SignatureException,
      CertificationRequestValidationException {
    try {
      maybeInitializeCA();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Failed to initialize CA", ex);
      throw new CAInitializationException(ex);
    }
    CAType caType = pkiUtils.getResponsibleCA(certificateType);
    Function<ExtensionsBuilderParameter, Void>[] certificateExtensionsBuilder = getExtensionsBuilders(caType,
        certificateType);
    X509Certificate certificate = signCertificateSigningRequest(csrStr, certificateType, caType,
        certificateExtensionsBuilder);
    LOGGER.log(Level.FINE, "Signed certificate and going to Save");
    saveNewCertificate(caType, certificate);
    LOGGER.log(Level.FINE, "Saved certificate");
    LOGGER.log(Level.INFO, "Generated and saved certificate with name " + certificate.getSubjectDN().toString());
    return certificate;
  }

  protected final static Function<ExtensionsBuilderParameter, Void> EMPTY_CERTIFICATE_EXTENSIONS_BUILDER = (b) -> null;
  protected final Function<ExtensionsBuilderParameter, Void> KUBE_CERTIFICATE_EXTENSIONS_BUILDER = (b) -> {
    if (conf.getKubernetesCA().isPresent()) {
      KubeCAConfiguration kubeCAConf = (KubeCAConfiguration) conf.getKubernetesCA().get();
      if (kubeCAConf.getSubjectAlternativeName().isPresent()) {
        SubjectAlternativeName san = kubeCAConf.getSubjectAlternativeName().get();
        List<GeneralName> generalNames = null;

        if (san.getDns().isPresent()) {
          List<String> dns = san.getDns().get();
          if (!dns.isEmpty()) {
            generalNames = new ArrayList<>();
            for (String s : dns) {
              generalNames.add(new GeneralName(GeneralName.dNSName, s));
            }
          }
        }

        if (san.getIp().isPresent()) {
          List<String> ips = san.getIp().get();
          if (!ips.isEmpty()) {
            if (generalNames == null) {
              generalNames = new ArrayList<>();
            }
            for (String s : ips) {
              generalNames.add(new GeneralName(GeneralName.iPAddress, s));
            }
          }
        }

        if (generalNames != null) {
          try {
            GeneralName[] gn = generalNames.toArray(new GeneralName[0]);
            appendSubjectAlternativeNames(b.certificateBuilder, gn);
          } catch (CertIOException ex) {
            throw new RuntimeException(ex);
          }
        }
      }
    }
    return null;
  };

  protected final Function<ExtensionsBuilderParameter, Void> SAN_CERTIFICATE_EXTENSIONS_BUILDER = (b) -> {
    if (b.certificateType.equals(CertificateType.HOST)) {
      Optional<String> cn = parseX509CommonName(b.certificationRequest);
      if (!cn.isPresent()) {
        throw new RuntimeException("x509 subject " + b.certificationRequest.getSubject().toString()
            + " does not have CN field");
      }

      GeneralName hostname = new GeneralName(GeneralName.dNSName, cn.get());
      GeneralName[] names = new GeneralName[1];
      names[0] = hostname;
      try {
        appendSubjectAlternativeNames(b.certificateBuilder, names);

        Optional<String> l = parseX509Locality(b.certificationRequest);
        if (l.isPresent()) {
          GeneralName[] sanForUsername = getSanForUsername(l.get());
          appendSubjectAlternativeNames(b.certificateBuilder, sanForUsername);

          final Set<String> extraSanSet = new HashSet<>();
          conf.getIntermediateCA().ifPresent(c -> {
            if (c.getExtraUsernameSAN() != null) {
              SubjectAlternativeName extraSan = c.getExtraUsernameSAN().get(l.get());
              if (extraSan != null) {
                extraSan.getDns().ifPresent(extraSanSet::addAll);
              }
            }
          });
          if (!extraSanSet.isEmpty()) {
            appendSubjectAlternativeNames(b.certificateBuilder, convertToGeneralNames(extraSanSet, false));
          }
        }
      } catch (CertIOException ex) {
        throw new RuntimeException(ex);
      }
    }
    return null;
  };

  GeneralName[] getSanForUsername(String username) {
    String normalizedUsername = usernamesConfiguration.getNormalizedUsername(username);
    if (normalizedUsername == null) {
      return EMTPY_GENERAL_NAMES;
    }
    switch (normalizedUsername) {
      case "glassfish":
      case "glassfishinternal":
        return convertToGeneralNames(HopsworksService.GLASSFISH.domains(), true);
      case "hdfs":
        return convertToGeneralNames(mergeSets(
            HopsworksService.NAMENODE.domains(),
            HopsworksService.SPARK_HISTORY_SERVER.domains()), true);
      case "hive":
        return convertToGeneralNames(HopsworksService.HIVE.domains(), true);
      case "livy":
        return convertToGeneralNames(HopsworksService.LIVY.domains(), true);
      case "flink":
        return convertToGeneralNames(HopsworksService.FLINK.domains(), true);
      case "consul":
        return convertToGeneralNames(HopsworksService.CONSUL.domains(), true);
      case "hopsmon":
        return convertToGeneralNames(HopsworksService.PROMETHEUS.domains(), true);
      case "zookeeper":
        return convertToGeneralNames(HopsworksService.ZOOKEEPER.domains(), true);
      case "rmyarn":
        return convertToGeneralNames(HopsworksService.RESOURCE_MANAGER.domains(), true);
      case "onlinefs":
        Set<String> onlinefsDomain = new HashSet<>();
        onlinefsDomain.add(HopsworksService.MYSQL.getNameWithTag(MysqlTags.onlinefs));
        return convertToGeneralNames(onlinefsDomain, true);
      case "elastic":
        return convertToGeneralNames(HopsworksService.LOGSTASH.domains(), true);
      case "flyingduck":
        return convertToGeneralNames(HopsworksService.FLYING_DUCK.domains(), true);
      case "kagent":
        return convertToGeneralNames(HopsworksService.DOCKER_REGISTRY.domains(), true);
      case "mysql":
        return convertToGeneralNames(mergeSets(
            HopsworksService.MYSQL.domains(),
            HopsworksService.RDRS.domains()), true);
      default:
        return EMTPY_GENERAL_NAMES;
    }
  }

  Set<String> mergeSets(Set<String>... sets) {
    Set<String> merged = new HashSet<>();
    for (Set<String> set : sets) {
      merged.addAll(set);
    }
    return merged;
  }

  GeneralName[] convertToGeneralNames(Set<String> domains, boolean isServiceDiscoveryDomain) {
    GeneralName[] names = new GeneralName[domains.size()];
    Iterator<String> i = domains.iterator();
    int idx = 0;
    while (i.hasNext()) {
      String domain = i.next();
      names[idx] = new GeneralName(GeneralName.dNSName,
          isServiceDiscoveryDomain ? Utilities.constructServiceFQDN(domain,
          caConf.getString(CAConf.CAConfKeys.SERVICE_DISCOVERY_DOMAIN)) : domain);
      idx++;
    }
    return names;
  }

  @VisibleForTesting
  Optional<String> parseX509CommonName(PKCS10CertificationRequest csr) {
    return parseX509Rdn(csr, BCStyle.CN);
  }

  @VisibleForTesting
  Optional<String> parseX509Locality(PKCS10CertificationRequest csr) {
    return parseX509Rdn(csr, BCStyle.L);
  }

  private Optional<String> parseX509Rdn(PKCS10CertificationRequest csr, ASN1ObjectIdentifier identifier) {
    RDN[] rdns = csr.getSubject().getRDNs(identifier);
    if (rdns.length == 0) {
      return Optional.empty();
    }
    return Optional.of(IETFUtils.valueToString(rdns[0].getFirst().getValue()));
  }

  void appendSubjectAlternativeNames(X509v3CertificateBuilder certificateBuilder, GeneralName[] namesToAdd)
      throws CertIOException {
    if (!certificateBuilder.hasExtension(Extension.subjectAlternativeName)) {
      certificateBuilder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(namesToAdd));
    } else {
      Extension existingExtension = certificateBuilder.getExtension(Extension.subjectAlternativeName);
      Extensions existingExtensions = new Extensions(existingExtension);
      GeneralNames existingGeneralNames = GeneralNames.fromExtensions(existingExtensions,
          Extension.subjectAlternativeName);
      GeneralName[] existingNames = existingGeneralNames.getNames();
      Set<GeneralName> uniqueGeneralNames = new HashSet<>(Arrays.asList(existingNames));
      uniqueGeneralNames.addAll(Arrays.asList(namesToAdd));
      GeneralName[] finalNames = new GeneralName[uniqueGeneralNames.size()];
      uniqueGeneralNames.toArray(finalNames);
      certificateBuilder.replaceExtension(Extension.subjectAlternativeName, false, new GeneralNames(finalNames));
    }
  }

  static class ExtensionsBuilderParameter {
    private final X509v3CertificateBuilder certificateBuilder;
    private final PKCS10CertificationRequest certificationRequest;
    private final CertificateType certificateType;

    private ExtensionsBuilderParameter(X509v3CertificateBuilder certificateBuilder,
        PKCS10CertificationRequest certificationRequest, CertificateType certificateType) {
      this.certificateBuilder = certificateBuilder;
      this.certificationRequest = certificationRequest;
      this.certificateType = certificateType;
    }

    static ExtensionsBuilderParameter of(X509v3CertificateBuilder certificateBuilder,
        PKCS10CertificationRequest certificationRequest, CertificateType certificateType) {
      return new ExtensionsBuilderParameter(certificateBuilder, certificationRequest, certificateType);
    }

    @VisibleForTesting
    static ExtensionsBuilderParameter of(X509v3CertificateBuilder certificateBuilder) {
      return new ExtensionsBuilderParameter(certificateBuilder, null, CertificateType.APP);
    }
  }

  private final Function<ExtensionsBuilderParameter, Void>[] HOST_CERTIFICATES_EXTENSION_BUILDERS =
      new Function[]{SAN_CERTIFICATE_EXTENSIONS_BUILDER};

  private final Function<ExtensionsBuilderParameter, Void>[] KUBERNETES_CERTIFICATES_EXTENSION_BUILDERS =
      new Function[]{
          KUBE_CERTIFICATE_EXTENSIONS_BUILDER,
          SAN_CERTIFICATE_EXTENSIONS_BUILDER,
      };

  private final Function<ExtensionsBuilderParameter, Void>[] EMTPY_CERTIFICATES_EXTENSION_BUILDERS =
      new Function[0];

  protected Function<ExtensionsBuilderParameter, Void>[] getExtensionsBuilders(CAType caType,
      CertificateType certificateType) {
    if (certificateType.equals(CertificateType.HOST)) {
      return HOST_CERTIFICATES_EXTENSION_BUILDERS;
    }
    if (caType.equals(CAType.KUBECA)) {
      return KUBERNETES_CERTIFICATES_EXTENSION_BUILDERS;
    }
    return EMTPY_CERTIFICATES_EXTENSION_BUILDERS;
  }

  protected X509Certificate signCertificateSigningRequest(String csrStr, CertificateType certificateType, CAType caType,
      Function<ExtensionsBuilderParameter, Void>[] extensionsBuilders)
      throws IOException, CertificateEncodingException, CACertificateNotFoundException,
      CertificateAlreadyExistsException, KeyException, NoSuchAlgorithmException, CertIOException,
      OperatorCreationException, CertificateException, InvalidKeyException, SignatureException,
      CertificationRequestValidationException {
    LOGGER.log(Level.FINE, "Signing CSR for type " + certificateType);
    PKCS10CertificationRequest csr = parseCertificateRequest(csrStr);
    validateCertificateSigningRequest(csr, caType);
    KeyPair signerKeyPair = getCAKeyPair(caType);
    X509Certificate signerCertificate = getCACertificate(certificateType);
    X500Name signerName = new JcaX509CertificateHolder(signerCertificate).getSubject();
    Optional<PKICertificate> exists = pkiCertificateFacade.findBySubjectAndStatus(csr.getSubject().toString(),
        PKICertificate.Status.VALID);
    if (exists.isPresent()) {
      // Antonios: This is not the most elegant way to identified if we're running on Managed Cloud aka hopsworks.ai
      // but it is one way...
      if (certificateType.equals(CertificateType.HOST)
          && !caConf.getString(CAConf.CAConfKeys.CLOUD_EVENTS_ENDPOINT).isEmpty()) {
        try {
          // On Managed Cloud we need to be able to re-issue a certificate with the same x509 Subject
          // when starting Workers after a Custer Stop when Glassfish has been unresponsive
          // https://github.com/logicalclocks/hopsworks-cloud/issues/3102
          revokeCertificate(csr.getSubject(), certificateType);
        } catch (Exception ex) {
          String msg = "Certificate with Subject " + csr.getSubject() + " already exists. Because running on Managed " +
              "Cloud we tried to revoke the previous certificate but we failed";
          LOGGER.log(Level.SEVERE, msg, ex);
          throw new CertificateAlreadyExistsException(msg, ex);
        }
      } else {
        throw new CertificateAlreadyExistsException("Certificate with Subject name " + csr.getSubject() + " already " +
            "exists");
      }
    }
    LOGGER.log(Level.FINE, "CSR subject: " + csr.getSubject().toString());
    Long serialNumber = serialNumberFacade.nextSerialNumber(caType);
    Instant notBefore = Instant.now().minus(3, ChronoUnit.MINUTES);
    Instant notAfter = getCertificateNotAfter(certificateType, notBefore);
    JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

    X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
        signerName,
        BigInteger.valueOf(serialNumber),
        Date.from(notBefore),
        Date.from(notAfter),
        csr.getSubject(),
        csr.getSubjectPublicKeyInfo());
    builder
        .addExtension(Extension.basicConstraints, true, new BasicConstraints(false))
        .addExtension(Extension.keyUsage, true,
            new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment))
        .addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(signerCertificate))
        .addExtension(Extension.subjectKeyIdentifier, false,
            extUtils.createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo()));
    try {
      for (Function<ExtensionsBuilderParameter, Void> f : extensionsBuilders) {
        f.apply(ExtensionsBuilderParameter.of(builder, csr, certificateType));
      }
    } catch (Exception ex) {
      throw new CertIOException("Failed to add extension to certificate", ex);
    }

    LOGGER.log(Level.FINE, "Built Certificate builder");
    ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
        .setProvider(new BouncyCastleProvider())
        .build(signerKeyPair.getPrivate());
    X509CertificateHolder holder = builder.build(signer);
    LOGGER.log(Level.FINE, "Signed certificate");
    X509Certificate signedCertificate = converter.getCertificate(holder);
    LOGGER.log(Level.FINE, "Converted to X509Certificate");
    signedCertificate.verify(signerKeyPair.getPublic(), new BouncyCastleProvider());
    LOGGER.log(Level.FINE, "Verified certificate");

    return signedCertificate;
  }

  private Instant getCertificateNotAfter(CertificateType certificateType, Instant notBefore) {
    TemporalAmount validity = pkiUtils.getValidityPeriod(certificateType);
    return notBefore.plus(validity);
  }

  private KeyPair getCAKeyPair(CAType type) throws KeyException {
    KeyPair keyPair = caKeys.get(type);
    if (keyPair == null) {
      throw new KeyException("Could not load Key pair from cache for " + type);
    }
    return keyPair;
  }

  private X509Certificate getCACertificate(CertificateType certificateType) throws CACertificateNotFoundException {
    CAType caType = null;
    switch (certificateType) {
      case APP:
      case PROJECT:
      case HOST:
        caType = CAType.INTERMEDIATE;
        break;
      case KUBE:
        caType = CAType.KUBECA;
        break;
      default:
        throw new CACertificateNotFoundException("Could not find suitable CA for " + certificateType);
    }
    return getCACertificate(caType);
  }

  private X509Certificate getCACertificate(CAType type) throws CACertificateNotFoundException {
    X509Certificate cert = caCertificates.get(type);
    if (cert == null) {
      throw new CACertificateNotFoundException("Failed to load " + type + " X509 certificate");
    }
    return cert;
  }

  protected void validateCertificateSigningRequest(PKCS10CertificationRequest csr, CAType caType)
      throws CertificationRequestValidationException {

    X500Name requestedName = csr.getSubject();
    LOGGER.log(Level.FINE, "Validating CSR name against CA names");
    for (X500Name n : CA_SUBJECT_NAME.values()) {
      if (n.equals(requestedName)) {
        throw new CertificationRequestValidationException("Requested Name " + requestedName + " collides with " +
            "Certificate Authority name");
      }
    }
  }

  private PKCS10CertificationRequest parseCertificateRequest(String csr)
      throws IOException, CertificateEncodingException {
    PEMParser pemParser = new PEMParser(new StringReader(csr));
    Object csrObject = pemParser.readObject();
    if (csrObject instanceof PKCS10CertificationRequest) {
      return (PKCS10CertificationRequest) csrObject;
    }
    throw new CertificateEncodingException("Failed to parse CSR to " + PKCS10CertificationRequest.class.getName());
  }

  public void revokeCertificate(String identifier, CertificateType certificateType)
      throws CAInitializationException, InvalidNameException, CertificateException, KeyException, CRLException {
    X500Name certificateName = pkiUtils.parseCertificateSubjectName(identifier, certificateType);
    revokeCertificate(certificateName, certificateType);
  }

  public void revokeCertificate(X500Name certificateName, CertificateType certificateType)
      throws CAInitializationException, CertificateException, KeyException, CRLException {
    try {
      maybeInitializeCA();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Failed to initialize CA", ex);
      throw new CAInitializationException(ex);
    }
    LOGGER.log(Level.FINE, "Revoking certificate with Subject " + certificateName);
    Optional<PKICertificate> maybeCert = pkiCertificateFacade.findById(
        new PKICertificateId(PKICertificate.Status.VALID, certificateName.toString()));

    if (!maybeCert.isPresent()) {
      throw new CertificateNotFoundException("Could not find certificate with Name " + certificateName.toString()
          + " to revoke");
    }
    PKICertificate pkiCert = maybeCert.get();
    LOGGER.log(Level.FINE, "Deleted certificate " + certificateName + " from database");
    byte[] encoded = pkiCert.getCertificate();
    X509Certificate certificate;
    try {
      certificate = converter.getCertificate(parseToX509CertificateHolder(encoded));
    } catch (IOException ex) {
      throw new CertificateException("Failed to decode certificate from CA database", ex);
    }

    if (!shouldCertificateTypeSkipCRL(certificateType)) {
      CAType caType = pkiUtils.getResponsibleCA(certificateType);
      X509CRL newCRL = addRevocationToCRL(caType, certificate);
      updateCRL(caType, newCRL);
      LOGGER.log(Level.FINE, "Updated CRL");
    } else {
      if (certificate != null) {
        // Check is here only to ease testing, certificate should never be null at this point
        LOGGER.log(Level.FINE, "Certificate " + certificate.getSubjectDN().toString() + " of type "
            + certificateType + " is not added to CRL");
      }
    }

    updateRevokedCertificate(pkiCert);
    if (certificate!= null) {
      // Check is here only to ease testing, certificate should never be null at this point
      LOGGER.log(Level.INFO, "Revoked certificate with X.509 name " + certificate.getSubjectDN().toString());
    }
  }

  protected boolean shouldCertificateTypeSkipCRL(CertificateType certificateType) {
    return certificateType.equals(CertificateType.APP);
  }

  protected X509CertificateHolder parseToX509CertificateHolder(byte[] encoded) throws IOException {
    return new X509CertificateHolder(encoded);
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  protected void updateRevokedCertificate(PKICertificate certificate) {
    Optional<PKICertificate> maybeRevoked = pkiCertificateFacade.findById(certificate.getCertificateId());
    if (!maybeRevoked.isPresent()) {
      LOGGER.log(Level.WARNING, "Tried to update revoked certificate " + certificate.getCertificateId() + " but " +
          "certificate does not exist in database. Skip updating");
      return;
    }
    PKICertificate revoked = maybeRevoked.get();
    revoked.getCertificateId().setStatus(PKICertificate.Status.REVOKED);
    revoked.setCertificate(null);
    pkiCertificateFacade.updateCertificate(revoked);
    pkiCertificateFacade.deleteCertificate(certificate);
  }

  protected X509CRL loadCRL(CAType type) throws CRLException, IOException {
    Optional<PKICrl> maybeCrl = crlFacade.getCRL(type);
    if (!maybeCrl.isPresent()) {
      throw new CRLException("CRL for " + type + " is not present");
    }
    return crlConverter.getCRL(new X509CRLHolder(maybeCrl.get().getCrl()));
  }

  private X509CRL loadCRL(Path path) throws IOException, CRLException {
    try (PEMParser pemParser = new PEMParser(new FileReader(path.toFile()))) {
      Object object = pemParser.readObject();
      if (object instanceof X509CRLHolder) {
        return crlConverter.getCRL((X509CRLHolder) object);
      }
      return null;
    }
  }

  protected X509CRL addRevocationToCRL(CAType caType, X509Certificate certificate)
      throws CRLException, KeyException {
    try {
      X509CRL crl = loadCRL(caType);
      KeyPair keyPair = getCAKeyPair(caType);
      X509v2CRLBuilder builder = new JcaX509v2CRLBuilder(crl);
      builder.setNextUpdate(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
      ExtensionsGenerator extGen = new ExtensionsGenerator();

      extGen.addExtension(Extension.reasonCode, false, REVOCATION_REASON);
      builder.addCRLEntry(certificate.getSerialNumber(), new Date(), extGen.generate());

      ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
          .setProvider(new BouncyCastleProvider()).build(keyPair.getPrivate());
      return crlConverter.getCRL(builder.build(signer));
    } catch (OperatorCreationException | IOException ex) {
      throw new CRLException(ex);
    }
  }

  public X509Certificate loadCertificate(String name, PKICertificate.Status status)
      throws CertificateNotFoundException, CertificateException {
    Optional<PKICertificate> maybeCertificate = pkiCertificateFacade.findBySubjectAndStatus(name, status);
    if (!maybeCertificate.isPresent()) {
      throw new CertificateNotFoundException("Certificate with subject " + name + " and Status " + status + " does " +
          "not exist");
    }
    PKICertificate pkiCertificate = maybeCertificate.get();
    byte[] encoded = pkiCertificate.getCertificate();
    try {
      return converter.getCertificate(new X509CertificateHolder(encoded));
    } catch (IOException ex) {
      throw new CertificateException("Failed to decode certificate from CA database", ex);
    }
  }

  private static final Function<X509v3CertificateBuilder, Void> INTERMEDIATE_EXTENSIONS = (builder) -> {
    try {
      builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
      builder.addExtension(Extension.keyUsage, true,
          new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign | KeyUsage.digitalSignature));
      return null;
    } catch (CertIOException ex) {
      throw new RuntimeException(ex);
    }
  };

  public static class CertificateValidityPeriod {
    private final Instant notBefore;
    private final Instant notAfter;

    public CertificateValidityPeriod(Instant notBefore, Instant notAfter) {
      this.notBefore = notBefore;
      this.notAfter = notAfter;
    }

    public Instant getNotBefore() {
      return notBefore;
    }

    public Instant getNotAfter() {
      return notAfter;
    }
  }

  public static class CertificateSigner {
    private final KeyPair keyPair;
    private final X509Certificate certificate;

    public CertificateSigner(KeyPair keyPair, X509Certificate certificate) {
      this.keyPair = keyPair;
      this.certificate = certificate;
    }

    public KeyPair getKeyPair() {
      return keyPair;
    }

    public X509Certificate getCertificate() {
      return certificate;
    }
  }

  public static class CertificateGenerationParameters {
    private final CertificateSigner signer;
    private final KeyPair ownerKeypair;
    private final Long serialNumber;
    private final X500Name x500Name;
    private final CertificateValidityPeriod validityPeriod;
    private final Function<X509v3CertificateBuilder, Void> populateExtensions;

    public CertificateGenerationParameters(CertificateSigner signer, KeyPair ownerKeypair, Long serialNumber,
        X500Name x500Name, CertificateValidityPeriod validityPeriod,
        Function<X509v3CertificateBuilder, Void> populateExtensions) {
      this.signer = signer;
      this.ownerKeypair = ownerKeypair;
      this.serialNumber = serialNumber;
      this.x500Name = x500Name;
      this.validityPeriod = validityPeriod;
      this.populateExtensions = populateExtensions;
    }
  }

  @VisibleForTesting
  protected void setSerialNumberFacade(SerialNumberFacade serialNumberFacade) {
    this.serialNumberFacade = serialNumberFacade;
  }

  @VisibleForTesting
  protected void setCaConf(CAConf caConf) {
    this.caConf = caConf;
  }

  @VisibleForTesting
  protected void setKeyFacade(KeyFacade keyFacade) {
    this.keyFacade = keyFacade;
  }

  @VisibleForTesting
  protected void setPkiCertificateFacade(PKICertificateFacade pkiCertificateFacade) {
    this.pkiCertificateFacade = pkiCertificateFacade;
  }

  @VisibleForTesting
  protected Map<CAType, X500Name> getCaSubjectNames() {
    return CA_SUBJECT_NAME;
  }

  @VisibleForTesting
  protected Map<CAType, KeyPair> getCaKeys() {
    return caKeys;
  }

  @VisibleForTesting
  protected Map<CAType, X509Certificate> getCaCertificates() {
    return caCertificates;
  }

  @VisibleForTesting
  protected void setCRLFacade(CRLFacade crlFacade) {
    this.crlFacade = crlFacade;
  }

  @VisibleForTesting
  protected void setPKIUtils(PKIUtils pkiUtils) {
    this.pkiUtils = pkiUtils;
  }

  @VisibleForTesting
  protected void setConverter(JcaX509CertificateConverter converter) {
    this.converter = converter;
  }

  @VisibleForTesting
  protected void setUsernamesConfiguration(UsernamesConfiguration usernamesConfiguration) {
    this.usernamesConfiguration = usernamesConfiguration;
  }
}
