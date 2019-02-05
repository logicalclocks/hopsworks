/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
