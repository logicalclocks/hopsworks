package se.kth.kthfsdashboard.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.openssl.PEMReader;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class PKIUtils {

    // TODO this should not be static
    final static String KEYSTORE_FILENAME = "keystore.jks";
    final static String ALIAS = "s1as";
    final static String PASSWORD = "changeit";
    final static String GLASSFISH_CONFIG_DIR = "/usr/local/glassfish-3.1.2.2/glassfish/domains/domain1/config/";
    
    final static Logger logger = Logger.getLogger(PKIUtils.class.getName());
    
    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    public static String signWithServerCertificate(String csr) throws CryptoException, IOException,
            KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException,
            InterruptedException, NoSuchProviderException, InvalidKeyException, SignatureException {

        verifyCSR(csr);
        File serverKeyPairFile = exportServerKeyPair(KEYSTORE_FILENAME, ALIAS, PASSWORD);
        String generatedCert = sign(serverKeyPairFile, csr);
        return generatedCert;
    }

    private static void verifyCSR(String csr) throws CryptoException, NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException {
        PEMReader reader = new PEMReader(new StringReader(csr));
        Object pemObject;
        try {
            pemObject = reader.readObject();
        } catch (IOException e) {
            logger.log(Level.INFO, "Could not read CSR from string: {0}", e);
            throw new CryptoException("Could not read CSR from string: " + e.getMessage());
        }
        if (pemObject instanceof PKCS10CertificationRequest) {
//            TODO: This part fails:
//            WebappClassLoader unable to load resource [org.bouncycastle.jce.provider.JDKKeyFactory$RSA], 
//            because it has not yet been started, or was already stopped
            
            PKCS10CertificationRequest certificationRequest = (PKCS10CertificationRequest) pemObject;
            if (!certificationRequest.verify()) {
                logger.info("CSR signature is not correct.");
                throw new CryptoException("CSR signature is not correct.");
            }
            return;
        }
        throw new CryptoException("Not an instance of PKCS10CertificationRequest.");
    }

    private static File exportServerKeyPair(String keyStoreFileName, String alias, String password) throws KeyStoreException,
            IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {

        File keyPairFile = new File(alias + ".pem");
        if (!keyPairFile.exists()) {
            logger.log(Level.INFO, "Key/Cert file ({0}) does not exist. Exporting Key/Cert from keystore...", 
                    keyPairFile.getAbsolutePath());
            KeyStore keyStore = KeyStore.getInstance("jks");
            keyStore.load(new FileInputStream(keyStoreFileName), password.toCharArray());
            Certificate cert = keyStore.getCertificate(alias);
            Key key = keyStore.getKey(alias, password.toCharArray());
            String certString = new String(Base64.encodeBase64(cert.getEncoded(), true));
            String keyString = new String(Base64.encodeBase64(key.getEncoded(), true));
            String content;
            content = "-----BEGIN CERTIFICATE-----\n";
            content += certString;
            content += "-----END CERTIFICATE-----\n";
            content += "-----BEGIN PRIVATE KEY-----\n";
            content += keyString;
            content += "-----END PRIVATE KEY-----\n";
            keyPairFile.createNewFile();
            FileUtils.writeStringToFile(keyPairFile, content);
        }
        return keyPairFile;
    }

    private static String sign(File CAKeyPairFile, String csr) throws IOException, InterruptedException {

        File csrFile = File.createTempFile(System.getProperty("java.io.tmpdir"), ".csr");
        File generatedCertFile = File.createTempFile(System.getProperty("java.io.tmpdir"), ".cert");
        FileUtils.writeStringToFile(csrFile, csr);
        logger.log(Level.INFO, "Signing CSR with {0}", CAKeyPairFile.getCanonicalPath());
        List<String> cmds = new ArrayList<String>();
        cmds.add("openssl");
        cmds.add("x509");
        cmds.add("-req");
        cmds.add("-CA");
        cmds.add(CAKeyPairFile.getAbsolutePath());
        cmds.add("-CAkey");
        cmds.add(CAKeyPairFile.getAbsolutePath());
        cmds.add("-in");
        cmds.add(csrFile.getAbsolutePath());
        cmds.add("-out");
        cmds.add(generatedCertFile.getAbsolutePath());
        cmds.add("-days");
        cmds.add("3650");
        cmds.add("-CAcreateserial");
        Process process = new ProcessBuilder(cmds).directory(new File(GLASSFISH_CONFIG_DIR)).
                redirectErrorStream(true).start();
        
        BufferedReader br = new BufferedReader(new InputStreamReader(
                process.getInputStream(), Charset.forName("UTF8")));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            logger.info(line);
        }
        process.waitFor();
        if (process.exitValue() != 0) {
            throw new RuntimeException("Failed to sign the certificate. Error code: " + process.exitValue());
        }
        logger.info("Signned certificate.");
        return FileUtils.readFileToString(generatedCertFile);
    }
}
