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
package io.hops.hopsworks.audit.logger;

import io.hops.hopsworks.audit.helper.AnnotationHelper;
import io.hops.hopsworks.audit.helper.AuditHelper;
import io.hops.hopsworks.audit.helper.LoggerFactory;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.audit.logger.annotation.Secret;
import io.hops.hopsworks.restutils.RESTException;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Logged
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class LogInterceptor implements Serializable {
  
  @Inject
  private AuditHelper auditHelper;
  @Inject
  private AnnotationHelper annotationHelper;
  @Inject
  private LoggerFactory loggerFactory;
  
  @AroundInvoke
  public Object collectBasicLoggingInformation(InvocationContext context) throws Exception {
    Logger logger = loggerFactory.getLogger();
    Method method = context.getMethod();
    Class<?> resourceClass = method.getDeclaringClass();
    String email = annotationHelper.getCallerName(method, context.getParameters());
    List<Object> params = annotationHelper.getParamsExceptAnnotated(method, context.getParameters(), Secret.class);
    Logged logged = annotationHelper.getAnnotation(resourceClass, method, Logged.class);
    Level level = logged.logLevel().getLevel();
    if (LogLevel.OFF.equals(logged.logLevel())) {
      return context.proceed();
    }
    Object ret;
    try {
      ret = context.proceed();
    } catch (Exception e) {
      logger.log(level, "Class Called: {0}; Method Called: {1}; Parameters: {2}; Caller: {3}; Error: {4}",
        new Object[]{resourceClass.getName(), method.getName(), params, email, getExceptionMessage(e)});
      throw e;
    }
    if (method.getReturnType().equals(Void.TYPE)) {
      logger.log(level, "Class Called: {0}; Method Called: {1}; Parameters: {2}; Caller: {3}; Outcome: {4}",
        new Object[]{resourceClass.getName(), method.getName(), params, email, "void"});
    } else if (ret == null) {
      logger.log(level, "Class Called: {0}; Method Called: {1}; Parameters: {2}; Caller: {3}; Outcome: {4}",
        new Object[]{resourceClass.getName(), method.getName(), params, email, null});
    } else {
      Response res = auditHelper.getResponse(ret);
      if (res != null) {
        logger.log(level, "Class Called: {0}; Method Called: {1}; Parameters: {2}; Caller: {3}; Response: {4}",
          new Object[]{resourceClass.getName(), method.getName(), params, email, res.getStatus()});
      } else {
        logger.log(level, "Class Called: {0}; Method Called: {1}; Parameters: {2}; Caller: {3}; Outcome: {4}",
          new Object[]{resourceClass.getName(), method.getName(), params, email, ret.getClass().getName()});
      }
    }
    return ret;
  }
  
  private String getExceptionMessage(Exception e) {
    if (e instanceof RESTException) {
      RESTException restException = (RESTException) e;
      return restException.getDevMsg() != null && !restException.getDevMsg().isEmpty()? restException.getDevMsg() :
        restException.getErrorCode().getMessage();// user msg is not set in the exception
    }
    return e.getMessage();
  }

}
