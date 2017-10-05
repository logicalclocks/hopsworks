package io.hops.hopsworks.util;

import io.hops.hopsworks.common.util.Settings;
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
import org.javatuples.Triplet;

public class CertificateHelper {

  private final static Logger LOG = Logger.getLogger(CertificateHelper.class.getName());

  public static Optional<Triplet<KeyStore, KeyStore, String>> initKeystore(Settings settings) {
    String keystoreFile = settings.getHopsSiteCaDir() + Settings.HOPS_SITE_KEY_STORE;
    String truststoreFile = settings.getHopsSiteCaDir() + Settings.HOPS_SITE_TRUST_STORE;
    if (!(new File(keystoreFile).exists()) || !(new File(truststoreFile).exists())) {
      LOG.log(Level.SEVERE, "Could not find keystore {0}, or trust store {1}", new Object[]{keystoreFile,
        truststoreFile});
      return Optional.empty();
    }
    try {
      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      KeyStore truststore = KeyStore.getInstance(KeyStore.getDefaultType());
      String keystorePassword = "adminpw"; // get from settings or rest api
      try (FileInputStream keyStoreIS = new FileInputStream(keystoreFile);
        FileInputStream trustStoreIS = new FileInputStream(truststoreFile)) {
        keystore.load(keyStoreIS, keystorePassword.toCharArray());
        truststore.load(trustStoreIS, keystorePassword.toCharArray());
        if (isCertSigned(settings)) {
          return Optional.of(Triplet.with(keystore, truststore, keystorePassword));
        } else {
          return Optional.empty();
        }
      } catch (IOException | NoSuchAlgorithmException | CertificateException ex) {
        LOG.log(Level.SEVERE, "Could not load keystore. {0}", ex);
        return Optional.empty();
      }
    } catch (KeyStoreException ex) {
      LOG.log(Level.SEVERE, "Keystore type not supported. {0}", ex);
      return Optional.empty();
    }
  }

  private static boolean isCertSigned(Settings settings) {
    String certFile = settings.getHopsSiteCaDir() + Settings.HOPS_SITE_CERTFILE;
    String caCertFile = settings.getHopsSiteCaDir() + Settings.HOPS_SITE_CA_CERTFILE;
    if (!(new File(certFile)).exists() || !(new File(caCertFile)).exists()) {
      LOG.log(Level.SEVERE, "Could not find certs.");
      return false;
    }
    String hopsRootCA;
    try (InputStream inStream = new FileInputStream(certFile);
      InputStream inStreamCA = new FileInputStream(caCertFile)) {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
      X509Certificate caCert = (X509Certificate) cf.generateCertificate(inStreamCA);
      hopsRootCA = caCert.getIssuerDN().getName();
      String issuerdn = cert.getIssuerDN().getName();
      if (issuerdn.equals(hopsRootCA)) {
        return true;
      }
    } catch (CertificateException | IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
      return false;
    }
    return false;
  }
}
