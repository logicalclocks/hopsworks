package io.hops.hopsworks.api.kibana;

import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstate;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

public class GrafanaProxyServlet extends ProxyServlet {

  @EJB
  private YarnApplicationstateFacade yarnApplicationstateFacade;
  @EJB
  private UserManager userManager;
  @EJB
  private HdfsUsersController hdfsUsersBean;

  @Override
  protected void service(HttpServletRequest servletRequest,
          HttpServletResponse servletResponse)
          throws ServletException, IOException {
    if (servletRequest.getRequestURI().contains("query")) {
      String email = servletRequest.getUserPrincipal().getName();
      Pattern pattern = Pattern.compile("(application_.*?_.*?)\\.");
      Matcher matcher = pattern.matcher(servletRequest.getQueryString());
      if (matcher.find()) {
        String appId = matcher.group(1);
        YarnApplicationstate appState = yarnApplicationstateFacade.findByAppId(
                appId);
        if (appState == null) {
          servletResponse.sendError(Response.Status.BAD_REQUEST.getStatusCode(),
                  "You don't have the access right for this application");
          return;
        }
        String user = hdfsUsersBean.getUserName(appState.getAppuser());

        String userEmail = userManager.getUserByUsername(user).getEmail();
        if (!userEmail.equals(email)) {
          servletResponse.sendError(Response.Status.BAD_REQUEST.getStatusCode(),
                  "You don't have the access right for this application");
          return;
        }
      } else {
        servletResponse.sendError(Response.Status.BAD_REQUEST.getStatusCode(),
                "You don't have the access right for this application");
        return;
      }
    }
    super.service(servletRequest, servletResponse);

  }
}
