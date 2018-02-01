/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.admin.security.ua;

import io.hops.hopsworks.common.dao.user.UserFacade;
import java.io.IOException;
import javax.ejb.EJB;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import io.hops.hopsworks.common.dao.user.Users;

public class LoginFilter extends PolicyDecisionPoint implements Filter {

  @EJB
  private UserFacade userFacade;

  private String urlList;

  @Override
  public void doFilter(ServletRequest req, ServletResponse res,
          FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;
    String url = request.getServletPath();
    boolean allowedRequest = false;

    if ((url.contains(urlList) && !url.contains("/index.html") && !url.contains(
            "/index.xhtml"))) {
      allowedRequest = true;
    }

    String username = request.getRemoteUser();

    Users user = null;

    if (username != null) {
      user = userFacade.findByEmail(username);
    }

    // If user is logged in redirect to index first page 
    // otherwise continue 
    if (request.getRemoteUser() != null && !allowedRequest) {
      String contextPath = ((HttpServletRequest) request).getContextPath();
      // redirect the admin to the admin pannel
      // otherwise redirect other authorized roles to the index page
      if (isInAdminRole(user)) {
        response.sendRedirect(contextPath
                + "/security/protected/admin/adminIndex.xhtml");
      } else if (isInAuditorRole(user)) {
        response.sendRedirect(contextPath
                + "/security/protected/audit/adminAuditIndex.xhtml");
      } else if (isInUserRole(user)) {
        response.sendRedirect("/hopsworks/#!home");
      }
    } else {
      chain.doFilter(req, res);
    }
  }

  @Override
  public void init(FilterConfig config) throws ServletException {

    urlList = config.getInitParameter("avoid-urls");

  }

  @Override
  public void destroy() {
  }
}
