package io.hops.hopsworks.api.tensorflow;

import io.hops.hopsworks.api.kibana.ProxyServlet;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstate;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.tensorflow.TensorflowFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.project.ProjectDTO;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.apache.http.client.utils.URIUtils;

public class TensorboardProxyServlet extends ProxyServlet {

  @EJB
  private YarnApplicationstateFacade yarnApplicationstateFacade;
  @EJB
  private UserManager userManager;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private ProjectController projectController;
  @EJB
  private TensorflowFacade tensorflowFacade;

  private final static Logger LOGGER = Logger.getLogger(TensorboardProxyServlet.class.
      getName());

  @Override
  protected void service(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    String email = servletRequest.getUserPrincipal().getName();
    Pattern pattern = Pattern.compile("(application_.*?_\\d*)");
    Matcher matcher = pattern.matcher(servletRequest.getRequestURI());
    if (matcher.find()) {
      String appId = matcher.group(1);
      YarnApplicationstate appState = yarnApplicationstateFacade.findByAppId(
          appId);
      if (appState == null) {
        servletResponse.sendError(Response.Status.FORBIDDEN.getStatusCode(),
            "You don't have the access right for this application");
        return;
      }
      String projectName = hdfsUsersBean.getProjectName(appState.getAppuser());
      ProjectDTO project;
      try {
        project = projectController.getProjectByName(projectName);
      } catch (AppException ex) {
        throw new ServletException(ex);
      }

      Users user = userManager.getUserByEmail(email);

      boolean inTeam = false;
      for (ProjectTeam pt : project.getProjectTeam()) {
        if (pt.getUser().equals(user)) {
          inTeam = true;
          break;
        }
      }
      if (!inTeam) {
        servletResponse.sendError(Response.Status.FORBIDDEN.getStatusCode(),
            "You don't have the access right for this application");
        return;
      }
      if (appState.getAppsmstate() != null && appState.getAppsmstate().equals("FINISHED")) {
        sendErrorResponse(servletResponse, "This tensorboard has finished running");
        return;
      }
      //get tensorboard address from hdfs file
      String uri = tensorflowFacade.getTensorboardURI(appId, projectName);
      if (uri == null || uri.equals("null")) {
        sendErrorResponse(servletResponse, "This tensorboard is not running right now");
        return;
      }
      targetUri = uri;
      try {
        targetUriObj = new URI(targetUri);
      } catch (Exception e) {
        throw new ServletException("Trying to process targetUri init parameter: "
            + e, e);
      }
      targetHost = URIUtils.extractHost(targetUriObj);
      try {
        super.service(servletRequest, servletResponse);
      } catch (IOException ex) {
        sendErrorResponse(servletResponse, "This tensorboard is not running right now");
        return;
      }
    } else {
      servletResponse.sendError(Response.Status.FORBIDDEN.getStatusCode(),
          "You don't have the access right for this application");
      return;
    }

  }

  private void sendErrorResponse(ServletResponse servletResponse, String message) throws IOException {
    servletResponse.setContentType("text/html");
    PrintWriter out = servletResponse.getWriter();
    out.println("<html>");
    out.println("<head>");
    out.println("<title></title>");
    out.println("</head>");
    out.println("<body>");
    out.println(message);
    out.println("</body>");
    out.println("</html>");
  }

  /**
   * Reads the request URI from {@code servletRequest} and rewrites it,
   * considering targetUri.
   * It's used to make the new request.
   */
  protected String rewriteUrlFromRequest(HttpServletRequest servletRequest) {
    StringBuilder uri = new StringBuilder(500);
    uri.append(getTargetUri(servletRequest));
    // Handle the path given to the servlet
    if (servletRequest.getPathInfo() != null) {//ex: /my/path.html
      String pathInfo = servletRequest.getPathInfo();
      pathInfo = pathInfo.substring(1);
      if (pathInfo.contains("/")) {
        pathInfo = pathInfo.substring(pathInfo.indexOf("/"));
      } else {
        pathInfo = "";
      }
      uri.append(encodeUriQuery(pathInfo));
    }
    // Handle the query string & fragment
    //ex:(following '?'): name=value&foo=bar#fragment
    String queryString = servletRequest.getQueryString();
    String fragment = null;
    //split off fragment from queryString, updating queryString if found
    if (queryString != null) {
      int fragIdx = queryString.indexOf('#');
      if (fragIdx >= 0) {
        fragment = queryString.substring(fragIdx + 2); // '#!', not '#'
//        fragment = queryString.substring(fragIdx + 1);
        queryString = queryString.substring(0, fragIdx);
      }
    }

    queryString = rewriteQueryStringFromRequest(servletRequest, queryString);
    if (queryString != null && queryString.length() > 0) {
      uri.append('?');
      uri.append(encodeUriQuery(queryString));
    }

    if (doSendUrlFragment && fragment != null) {
      uri.append('#');
      uri.append(encodeUriQuery(fragment));
    }
    return uri.toString();
  }
}
