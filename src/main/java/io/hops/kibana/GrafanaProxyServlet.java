package io.hops.kibana;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import se.kth.bbc.jobs.jobhistory.YarnApplicationstate;
import se.kth.bbc.jobs.jobhistory.YarnApplicationstateFacade;
import se.kth.bbc.security.ua.UserManager;
import se.kth.hopsworks.hdfsUsers.controller.HdfsUsersController;
import se.kth.hopsworks.rest.AppException;

public class GrafanaProxyServlet extends ProxyServlet{
  
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
    if (servletRequest.getRequestURI().contains("query")){
      String email = servletRequest.getUserPrincipal().getName();
      Pattern pattern = Pattern.compile("(application_.*?_.*?)\\.");
      Matcher matcher = pattern.matcher(servletRequest.getQueryString());
      if(matcher.find()){
        String appId = matcher.group(1);
        YarnApplicationstate appState = yarnApplicationstateFacade.findByAppId(appId);
        if(appState==null){
          servletResponse.sendError(Response.Status.BAD_REQUEST.getStatusCode(), "You don't have the access right for this application");
          return;
        }
        String user = hdfsUsersBean.getUserName(appState.getAppuser());
        
        String userEmail= userManager.getUserByUsername(user).getEmail();
        if(!userEmail.equals(email)){
          servletResponse.sendError(Response.Status.BAD_REQUEST.getStatusCode(), "You don't have the access right for this application");
          return;
        }
      }else{
        servletResponse.sendError(Response.Status.BAD_REQUEST.getStatusCode(), "You don't have the access right for this application");
        return;
      }
    }
    super.service(servletRequest, servletResponse);
    
  }
}
