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
package io.hops.hopsworks.audit.auditor;

import io.hops.hopsworks.audit.auditor.annotation.Audited;
import io.hops.hopsworks.audit.auditor.annotation.AuditedList;
import io.hops.hopsworks.audit.helper.AnnotationHelper;
import io.hops.hopsworks.audit.helper.AuditActionStatus;
import io.hops.hopsworks.audit.helper.AuditHelper;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

@AuditedList(@Audited)
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuditedListInterceptor implements Serializable {
  private final static Logger LOGGER = Logger.getLogger(AuditedListInterceptor.class.getName());
  
  @Inject
  private transient AuditHelper auditHelper;
  @Inject
  private transient AnnotationHelper annotationHelper;
  
  @AroundInvoke
  public Object collectBasicAuditInformation(InvocationContext context) throws Exception {
    Method method = context.getMethod();
    Object[] parameters = context.getParameters();
    HttpServletRequest req = auditHelper.getHttpServletRequest(parameters);
    if (req == null) {
      LOGGER.log(Level.SEVERE, "Audited method ({0}) needs HttpServletRequest parameter!", method.getName());
      return context.proceed();
    }
    String remoteHost = auditHelper.extractRemoteHostIp(req);
    String userAgent = auditHelper.extractUserAgent(req);
    Users initiator = annotationHelper.getCaller(method, parameters);
    Users target = annotationHelper.getAuditTarget(method, parameters);
    Class<?> resourceClass = method.getDeclaringClass();
    AuditedList auditedList = annotationHelper.getAnnotation(resourceClass, method, AuditedList.class);
    Object ret;
    try {
      ret = context.proceed();
    } catch (Exception e) {
      auditHelper
        .saveAudit(auditedList.value(), method, parameters, initiator, target, remoteHost, userAgent,
          AuditActionStatus.FAILED);
      throw e;
    }
    auditHelper
      .saveAudit(auditedList.value(), method, parameters, initiator, target, remoteHost, userAgent,
        AuditActionStatus.SUCCESS);
    return ret;
  }
  
}
