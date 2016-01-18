
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
 */
public class AppScriptServlet extends HttpServlet {

  private final static Logger logger = Logger.getLogger(AppScriptServlet.class.
          getName());
  public AppScriptServlet() {
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

      // Replace the string "function getRestApiBase(){...}" to return
      // the proper value 
      int startIndexRest = script.indexOf("this.getRestApiBase=function(){");
      int endIndexRest = script.indexOf("};", startIndexRest);

      if (startIndexRest >= 0 && endIndexRest >= 0) {
        String replaceStringRest
                = "this.getRestApiBase=function(){return location.protocol + '//'"
                + " + location.hostname + ':' + this.getPort() "
                + " + '/hopsworks/api'; }";
        script.replace(startIndexRest, endIndexRest + 1, replaceStringRest);
      }
      out.println(script.toString());
    }
  }
}
