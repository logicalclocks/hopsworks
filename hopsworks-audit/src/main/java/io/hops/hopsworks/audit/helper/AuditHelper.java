/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.audit.helper;

import io.hops.hopsworks.audit.auditor.AuditType;
import io.hops.hopsworks.audit.auditor.annotation.Audited;
import io.hops.hopsworks.audit.exception.AuditException;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AuditHelper {
  private final static Logger LOGGER = Logger.getLogger(AuditHelper.class.getName());
  
  @EJB
  private AccountAuditFacade accountAuditFacade;
  @EJB
  private AnnotationHelper annotationHelper;
  
  public void saveAudit(Audited audited, Method method, Object[] parameters, AuditActionStatus result,
    HttpServletRequest req) {
    AuditType type = audited.type();
    AuditAction action = audited.action();
    String message = audited.message();
  
    if (canNotAudit(action, result)) {
      //No user to audit
      LOGGER.log(Level.FINE, "Register failed. Can not audit.");
      return;
    }
    
    String remoteHost = extractRemoteHostIp(req);
    String userAgent = extractUserAgent(req);
    Users initiator = annotationHelper.getCaller(method, parameters);
    Users target = annotationHelper.getAuditTarget(method, parameters);
    
    save(initiator, target, result, type, action, message, method, remoteHost, userAgent, req);
  }
  
  public void saveAudit(Audited[] auditors, Method method, Object[] parameters, AuditActionStatus result,
    HttpServletRequest req) {
    String remoteHost = extractRemoteHostIp(req);
    String userAgent = extractUserAgent(req);
    
    for (Audited audited : auditors) {
      AuditType type = audited.type();
      AuditAction action = audited.action();
      String message = audited.message();
  
      if (canNotAudit(action, result)) {
        //No user to audit
        LOGGER.log(Level.FINE, "Register failed. Can not audit.");
        continue;
      }
      Users initiator = annotationHelper.getCaller(method, parameters);
      Users target = annotationHelper.getAuditTarget(method, parameters);
      save(initiator, target, result, type, action, message, method, remoteHost, userAgent, req);
    }
  }
  
  private void save(Users initiator, Users target, AuditActionStatus result, AuditType type, AuditAction action,
    String message, Method method, String remoteHost, String userAgent, HttpServletRequest req) {
    try {
      doSanityCheck(initiator, req);//get initiator from target for login and register
      switch (type) {
        case ACCOUNT_AUDIT:
          if (target == null) {
            throw new AuditException("Operation target user not found.");
          }
          accountAuditFacade.registerAccountChange(initiator, action.toString(), result.toString(), message, target,
            remoteHost, userAgent);
          break;
        case ROLE_AUDIT:
          if (target == null) {
            throw new AuditException("Operation target user not found.");
          }
          accountAuditFacade.registerRoleChange(initiator, action.toString(), result.toString(), message, target,
            remoteHost, userAgent);
          break;
        case USER_LOGIN:
          accountAuditFacade.registerLoginInfo(initiator, action.toString(), result.toString(), remoteHost, userAgent);
          break;
      }
    } catch (Exception e) {
      LOGGER
        .log(Level.SEVERE, "Failed to do {0} on method {1}. {2}", new Object[]{type, method.getName(), e.getMessage()});
    }
  }
  
  private boolean canNotAudit(AuditAction action, AuditActionStatus result) {
    return (AuditAction.LOGIN.equals(action) ||
      AuditAction.RECOVERY.equals(action) ||
      AuditAction.LOST_DEVICE.equals(action) ||
      AuditAction.REGISTRATION.equals(action)) &&
      AuditActionStatus.FAILED.equals(result);
  }
  
  public HttpServletRequest getHttpServletRequest(Object[] parameters) {
    if (parameters != null && parameters.length > 0) {
      for (Object param : parameters) {
        if (param instanceof HttpServletRequest) {
          return (HttpServletRequest) param;
        }
      }
    }
    return null;
  }
  
  private void doSanityCheck(Users initiator, HttpServletRequest req) throws AuditException {
    String msg = null;
    if (initiator == null) {
      msg = "Operation initiator not found.";
    } else if (req == null) {
      msg = "Operation http request not set.";
    }
    if (msg != null) {
      throw new AuditException(msg);
    }
  }
  
  private String extractUserAgent(HttpServletRequest httpServletRequest) {
    String userAgent = httpServletRequest.getHeader("User-Agent");
    if (userAgent == null || userAgent.isEmpty()) {
      return "Unknown User-Agent";
    }
    return StringUtils.left(userAgent, 255);
  }
  
  private String extractRemoteHostIp(HttpServletRequest httpServletRequest) {
    String ipAddress = httpServletRequest.getHeader("X-Forwarded-For");
    if (ipAddress == null) {
      ipAddress = httpServletRequest.getRemoteAddr();
    }
    // if reverse proxy is used for request, X-FORWARD-FOR header value will contain
    // (client ip, load balancer server, reverse proxy server)
    return ipAddress.contains(",") ? ipAddress.split(",")[0] : ipAddress;
  }
  
  public Response getResponse(Object ret) {
    if (ret instanceof Response) {
      return (Response) ret;
    }
    return null;
  }
}
