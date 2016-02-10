package se.kth.hopsworks.zeppelin.server;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.interpreter.InterpreterFactory;
import org.apache.zeppelin.interpreter.InterpreterOption;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.apache.zeppelin.scheduler.SchedulerFactory;
import org.apache.zeppelin.search.LuceneSearch;
import org.apache.zeppelin.search.SearchService;
import se.kth.hopsworks.zeppelin.socket.NotebookServer;

public enum ZeppelinSingleton {

  SINGLETON;
  private final Logger logger = Logger.getLogger(ZeppelinSingleton.class.
          getName());
  private static final String ZEPPELIN_DIR = "/srv/zeppelin";
  private static final String ZEPPELIN_SITE_XML = "/conf/zeppelin-site.xml";
  private ZeppelinConfiguration conf;
  private SchedulerFactory schedulerFactory;
  
  private NotebookServer notebookServer;
  private InterpreterFactory replFactory;
  private NotebookRepo notebookRepo;
  private SearchService notebookIndex;

  ZeppelinSingleton() {
    try {
      this.conf = loadConfig();
      this.schedulerFactory = new SchedulerFactory();
      this.notebookServer = setupNotebookServer();
      this.replFactory = new InterpreterFactory(conf,
              new InterpreterOption(true), notebookServer);
      this.notebookIndex = new LuceneSearch();
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

  public SearchService getNotebookIndex() {
    return notebookIndex;
  }

  private ZeppelinConfiguration loadConfig() {
    URL url = null;
    File zeppelinConfig = new File(ZEPPELIN_DIR + ZEPPELIN_SITE_XML);
      try {
        url = zeppelinConfig.toURI().toURL();
        logger.log(Level.INFO, "Load configuration from {0}", url);
        conf = new ZeppelinConfiguration(url);
      } catch (ConfigurationException e) {
        logger.log(Level.INFO, "Failed to load configuration from " + url
                + " proceeding with a default", e);
        conf = new ZeppelinConfiguration();
      } catch (MalformedURLException ex) {
        logger.log(Level.INFO, "Malformed URL failed to load configuration from " + url
                + " proceeding with a default", ex);
        conf = new ZeppelinConfiguration();
      }
    return conf;
  }

}
