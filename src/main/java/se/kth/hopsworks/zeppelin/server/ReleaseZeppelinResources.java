package se.kth.hopsworks.zeppelin.server;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import se.kth.hopsworks.zeppelin.notebook.Notebook;
import se.kth.hopsworks.zeppelin.socket.NotebookServer;

/**
 *
 * @author ermiasg
 */
@Startup
@Singleton
public class ReleaseZeppelinResources {
  private static final Logger logger = Logger.getLogger(ReleaseZeppelinResources.class.
          getName());
  private final ZeppelinSingleton zeppelin = ZeppelinSingleton.SINGLETON;

  public ReleaseZeppelinResources() {
  }
 
  @PreDestroy
  void preDestroy() {
    logger.log(Level.SEVERE, "Closing interpreters");
    zeppelin.getReplFactory().close();
    Notebook notebook = zeppelin.getNotebookServer().notebook();
    notebook.setNotebookRepo(null);
    notebook = null;
    logger.log(Level.SEVERE, "Closed interpreters");

  }
}
