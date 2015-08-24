package se.kth.hopsworks.zeppelin.server;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.interpreter.InterpreterFactory;
import org.apache.zeppelin.interpreter.InterpreterOption;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.apache.zeppelin.scheduler.SchedulerFactory;
import se.kth.hopsworks.zeppelin.socket.NotebookServer;

/**
 *
 * @author ermiasg
 */
public enum ZeppelinSingleton {

  SINGLETON;
  private final Logger logger = Logger.getLogger(ZeppelinSingleton.class.
          getName());
  private static final String ZEPPELIN_SITE_XML
          = "/zeppelinConf/zeppelin-site.xml";
  private ZeppelinConfiguration conf;
  private SchedulerFactory schedulerFactory;

  private NotebookServer notebookServer;

  private InterpreterFactory replFactory;
  private NotebookRepo notebookRepo;

  ZeppelinSingleton() {
    try {
      this.conf = loadConfig();
      this.schedulerFactory = new SchedulerFactory();
      this.notebookServer = setupNotebookServer();
      this.replFactory = new InterpreterFactory(conf, new InterpreterOption(true), notebookServer);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error in initializing singleton class.", e);
    }
  }

  public ZeppelinConfiguration getConf() {
    return this.conf;
  }

  private NotebookServer setupNotebookServer() throws Exception {
    NotebookServer server = new NotebookServer();
    return server;
  }

  public NotebookRepo getNotebookRepo() {
    return notebookRepo;
  }

  public SchedulerFactory getSchedulerFactory() {
    return this.schedulerFactory;
  }

  public NotebookServer getNotebookServer() {
    return this.notebookServer;
  }

  public InterpreterFactory getReplFactory() {
    return this.replFactory;
  }

  private ZeppelinConfiguration loadConfig() {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    URL url;

    url = ZeppelinSingleton.class.getResource(ZEPPELIN_SITE_XML);
    if (url == null) {
      ClassLoader cl = ZeppelinSingleton.class.getClassLoader();
      if (cl != null) {
        url = cl.getResource(ZEPPELIN_SITE_XML);
      }
    }
    if (url == null) {
      url = classLoader.getResource(ZEPPELIN_SITE_XML);
    }

    if (url == null) {
      logger.log(Level.WARNING,
              "Failed to load configuration from {0}, proceeding with a default",
              ZEPPELIN_SITE_XML);
      conf = new ZeppelinConfiguration();
    } else {
      try {
        logger.log(Level.INFO, "Load configuration from {0}", url);
        conf = new ZeppelinConfiguration(url);
      } catch (ConfigurationException e) {
        logger.log(Level.INFO, "Failed to load configuration from " + url
                + " proceeding with a default", e);
        conf = new ZeppelinConfiguration();
      }
    }

    return conf;
  }

}
