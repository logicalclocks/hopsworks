package se.kth.bbc.security.auth;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import se.kth.bbc.security.ua.authz.PolicyAdministrationPoint;

public class LoginFilter extends PolicyAdministrationPoint implements Filter {

  private String urlList;

  @Override
  public void doFilter(ServletRequest req, ServletResponse res,
          FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;
    String url = request.getServletPath();
    boolean allowedRequest = false;

    if ((url.contains(urlList) && !url.contains("/index.html") && !url.contains("/index.xhtml"))) {
      allowedRequest = true;
    }

    
    
    String username = request.getRemoteUser();

    // If user is logged in redirect to index first page 
    // otherwise continue 
    if (request.getRemoteUser() != null && !allowedRequest) {
      String contextPath = ((HttpServletRequest) request).getContextPath();
      // redirect the admin to the admin pannel
      // otherwise redirect other authorized roles to the index page
      if (isInAdminRole(username)) {
        response.sendRedirect(contextPath
                + "/security/protected/admin/adminIndex.xhtml");
      } else if (isInAuditorRole(username)) {
        response.sendRedirect(contextPath
                + "/security/protected/audit/auditIdex.xhtml");
      } else if (isInDataProviderRole(username) || isInResearcherRole(username)
              || isInGuestRole(username)) {
        response.sendRedirect(contextPath +"/#home");
      }
    } else {
      chain.doFilter(req, res);
    }
  }

  @Override
  public void init(FilterConfig config) throws ServletException {

   urlList= config.getInitParameter("avoid-urls");
    
  }

  @Override
  public void destroy() {
  }
}
