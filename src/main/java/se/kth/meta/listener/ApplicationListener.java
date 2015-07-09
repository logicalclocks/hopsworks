package se.kth.meta.listener;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import se.kth.meta.db.Dbao;
import se.kth.meta.exception.DatabaseException;
import se.kth.meta.wscomm.Protocol;
import se.kth.meta.wscomm.WebSocketEndpoint;

/**
 *
 * @author Vangelis
 */
@WebListener
public class ApplicationListener implements ServletContextListener,
        ServletRequestListener {

  private static final Logger logger = Logger.getLogger(
          ApplicationListener.class.getName());
  private Dbao db;

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {

    ServletContext context = servletContextEvent.getServletContext();

    final ServerContainer serverContainer = (ServerContainer) context
            .getAttribute("javax.websocket.server.ServerContainer");

    try {
      //initialize the Database Access Object
      this.db = (Dbao) context.getAttribute("db");
      if (this.db == null) {
        //this.db = this.createDb();
        this.db = new Dbao();
        context.setAttribute("db", this.db);
        context.setAttribute("protocol", new Protocol(this.db));
      }

      //initialize the WebSockets Endpoint
      serverContainer.addEndpoint(WebSocketEndpoint.class);

      logger.log(Level.INFO, "HOPSWORKS WS INITIALIZED");
    } catch (DatabaseException e) {
      logger.log(Level.SEVERE, "HOPSWORKS WAS NOT INITIALIZED ", e);
    } catch (DeploymentException ex) {
      Logger.getLogger(ApplicationListener.class.getName()).log(Level.SEVERE,
              null, ex);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    try {
      this.db.shutdown();
    } catch (NullPointerException | DatabaseException e) {
      //in case the database was not initialized
      String message = "METAHOPS WS: Error closing the database " + e.
              getMessage();
      logger.log(Level.SEVERE, message, e);
    }
  }

  //create db object manually
//    private Dbao createDb() throws ApplicationException {
//        Dbao dbao = null;
//
//        try {
//            //local database
//            //dbao = new Dbao("localhost", 3306, "metahops", "metahops", "metahops");
//
//            //sics cloud database
//            //dbao = new Dbao("cloud11.sics.se", 3306, "vangelis", "hop", "hop");
//            //virtual ubuntu database
//            dbao = new Dbao("localhost", 3307, "vmetahops", "vmetahops", "metahopspwd");
//        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | SQLException e) {
//            logger.log(Level.SEVERE, e.getMessage(), e);
//            throw new ApplicationException(e.getMessage());
//        }
//        return dbao;
//    }
  @Override
  public void requestInitialized(ServletRequestEvent event) {
    HttpServletRequest request = (HttpServletRequest) event.getServletRequest();
    HttpSession se = request.getSession();
    //logger.info("METAHOPS WS: REQUEST INITIALIZED");
  }

  @Override
  public void requestDestroyed(ServletRequestEvent sre) {
    //logger.info("METAHOPS WS: REQUEST DESROYED");
  }

//    @Override
//    public void sessionCreated(HttpSessionEvent event) {
//        logger.info("METAHOPS WS: SESSION CREATED");
//    }
//
//    @Override
//    public void sessionDestroyed(HttpSessionEvent event) {
//        logger.log(Level.SEVERE, "METAHOPS WS: SESSION DESTROYED");
//    }
}
