/*
 */
package se.kth.hopsworks.zeppelin.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * Simple servlet to dynamically set the Websocket and
 * the Rest api ports in the JavaScript sent to the client
 * <p/>
 * @author ermiasg
 */
public class AppScriptServlet extends HttpServlet {

  private final static Logger logger = Logger.getLogger(AppScriptServlet.class.
          getName()); 

  private final ZeppelinSingleton zeppelin = ZeppelinSingleton.SINGLETON;
  private final int websocketPort;

  public AppScriptServlet() {
    this.websocketPort = zeppelin.getConf().getWebSocketPort();
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    response.setContentType("text/html;charset=UTF-8");
    try (PrintWriter out = response.getWriter()) {

      ServletContext context = getServletContext();
      // Read the script file chunk by chunk
      InputStream is = context.getResourceAsStream(
              "/zeppelin/scripts/scripts.js");
      StringBuilder script = new StringBuilder();
      byte[] buffer = new byte[1024];
      while (is.available() > 0) {
        int numRead = is.read(buffer);
        if (numRead <= 0) {
          break;
        }
        script.append(new String(buffer, 0, numRead, "UTF-8"));
      }

      // Replace the strings "function getPort(){...}" & "function getRestApiBase(){...}" to return
      // the proper value
      int startIndex = script.indexOf("function getPort()");
      int endIndex = script.indexOf("}", startIndex);

      if (startIndex >= 0 && endIndex >= 0) {
        String replaceString
                = "function getPort(){return '" + this.websocketPort
                + "/hopsworks/websocket'; }";
        script.replace(startIndex, endIndex + 1, replaceString);

        int startIndexRest = script.indexOf("function getRestApiBase()");
        int endIndexRest = script.indexOf("}", startIndexRest);

        // TODO: this should not be hard-coded!
        if (startIndexRest >= 0 && endIndexRest >= 0) {
          String replaceStringRest
                  = "function getRestApiBase(){  var port = Number(location.port);"
                  + "  if (port === 'undefined' || port === 0) {"
                  + "    port = 80;"
                  + "    if (location.protocol === 'https:') {"
                  + "      port = 443; } }"
                  + "  if (port === 3333 || port === 9000) {"
                  + "    port = 8080;" + "  }"
                  + "  return location.protocol+'//'+location.hostname+':'+ port + '/hopsworks/api'; }";
          script.replace(startIndexRest, endIndexRest + 1, replaceStringRest);
        }
      }
      out.println(script.toString());
    }
  }
}
