package io.hops.hopsworks.common.yarn;

import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.BaseHadoopClientsService;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.net.HopsSSLSocketFactory;
import org.apache.hadoop.yarn.conf.YarnConfiguration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class YarnClientService {
  private final Logger LOG = Logger.getLogger(
      YarnClientService.class.getName());
  
  @EJB
  private Settings settings;
  @EJB
  private BaseHadoopClientsService bhcs;
  
  private Configuration conf;
  private String hostname;
  private String transientDir;
  private String serviceCertsDir;
  
  public YarnClientService() {
  }
  
  @PostConstruct
  public void init() {
    String confDir = settings.getHadoopConfDir();
    File coreSite = new File(confDir, "core-site.xml");
    if (!coreSite.exists()) {
      handleMissingConf("core-site.xml", confDir);
    }
    
    File yarnSite = new File(confDir, "yarn-site.xml");
    if (!yarnSite.exists()) {
      handleMissingConf("yarn-site.xml", confDir);
    }
    
    conf = new YarnConfiguration();
    Path coreSitePath = new Path(coreSite.getAbsolutePath());
    Path yarnSitePath = new Path(yarnSite.getAbsolutePath());
    conf.addResource(coreSitePath);
    conf.addResource(yarnSitePath);
    
    if (settings.getHopsRpcTls()) {
      bhcs.parseServerSSLConf(conf);
      
      try {
        hostname = InetAddress.getLocalHost().getHostName();
        transientDir = settings.getHopsworksTmpCertDir();
        serviceCertsDir = conf.get(HopsSSLSocketFactory.CryptoKeys
            .SERVICE_CERTS_DIR.getValue(),
            HopsSSLSocketFactory.CryptoKeys.SERVICE_CERTS_DIR.getDefaultValue());
      } catch (UnknownHostException ex) {
        LOG.log(Level.SEVERE, "Could not determine hostname " + ex
            .getMessage(), ex);
        throw new RuntimeException("Could not determine hostname", ex);
      }
    }
  }
  
  @PreDestroy
  public void tearDown() {
    conf.clear();
    conf = null;
  }
  
  public YarnClientWrapper getYarnClient(String username) {
    if (settings.getHopsRpcTls()) {
      try {
        Configuration newConf = new Configuration(conf);
        bhcs.materializeCertsForNonSuperUser(username);
        
        bhcs.configureTlsForProjectSpecificUser(username, transientDir,
            newConf);
  
        return createYarnClient(username, newConf);
      } catch (BaseHadoopClientsService.CryptoPasswordNotFoundException ex) {
        LOG.log(Level.SEVERE, ex.getMessage(), ex);
        String[] project_username = username.split(HdfsUsersController
            .USER_NAME_DELIMITER);
        bhcs.removeNonSuperUserCertificate(project_username[1],
            project_username[0]);
        return null;
      }
    }
    
    return createYarnClient(username, conf);
  }
  
  public YarnClientWrapper getYarnClientSuper() {
    return getYarnClientSuper(null);
  }
  
  public YarnClientWrapper getYarnClientSuper(Configuration conf) {
    if (settings.getHopsRpcTls()) {
      Configuration newConfig;
      if (null != conf) {
        newConfig = new Configuration(conf);
      } else {
        newConfig = new Configuration(this.conf);
      }
      
      String keystorePath = bhcs.getSuperKeystorePath();
      String keystorePass = bhcs.getSuperKeystorePassword();
      String truststorePath = bhcs.getSuperTrustStorePath();
      String truststorePass = bhcs.getSuperTrustStorePassword();
      
      HopsSSLSocketFactory.setTlsConfiguration(keystorePath, keystorePass,
          truststorePath, truststorePass, newConfig);
      
      return createYarnClient(null, newConfig);
    }
    
    return null != conf ? createYarnClient(null, conf) :
        createYarnClient(null, this.conf);
  }
  
  public void closeYarnClient(YarnClientWrapper yarnClientWrapper) {
    if (null != yarnClientWrapper) {
      try {
        yarnClientWrapper.close();
      } finally {
        if (settings.getHopsRpcTls()) {
          String username = yarnClientWrapper.getUsername();
          String projectName = yarnClientWrapper.getProjectName();
          if (null != username && null != projectName) {
            bhcs.removeNonSuperUserCertificate(username, projectName);
          }
        }
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
  
  private YarnClientWrapper createYarnClient(String username,
      Configuration conf) {
    
    if (null != username) {
      String[] tokens = username.split(HdfsUsersController.USER_NAME_DELIMITER,
          2);
  
      if (tokens.length == 2) {
        return new YarnClientWrapper(tokens[0], tokens[1], conf).get();
      }
    }
    
    return new YarnClientWrapper(null, null, conf).get();
  }
}
