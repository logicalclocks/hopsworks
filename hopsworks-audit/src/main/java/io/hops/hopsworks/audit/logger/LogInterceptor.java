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
import org.apache.logging.log4j.message.MapMessage;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

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
    Method method = context.getMethod();
    Class<?> resourceClass = method.getDeclaringClass();

    Logged logged = annotationHelper.getAnnotation(resourceClass, method, Logged.class);
    Level level = logged.logLevel().getLevel();
    if (LogLevel.OFF.equals(logged.logLevel())) {
      return context.proceed();
    }

    String email = annotationHelper.getCallerName(method, context.getParameters());
    Map<String, String> clientInfo = annotationHelper.getClientInfo(context.getParameters());
    List<Object> params = annotationHelper.getParamsExceptAnnotated(method, context.getParameters(), Secret.class);

    Object ret;
    try {
      ret = context.proceed();
    } catch (Exception e) {
      log(resourceClass.getName(), method.getName(), email, clientInfo, params, getExceptionMessage(e), level);
      throw e;
    }
    if (method.getReturnType().equals(Void.TYPE)) {
      log(resourceClass.getName(), method.getName(), email, clientInfo, params, "void", level);
    } else {
      Response res = auditHelper.getResponse(ret);
      if (res != null) {
        log(resourceClass.getName(), method.getName(), email, clientInfo,
            params, String.valueOf(res.getStatus()), level);
      } else {
        log(resourceClass.getName(), method.getName(), email, clientInfo, params, ret.getClass().getName(), level);
      }
    }
    return ret;
  }

  private void log(String className, String methodName, String email,
                   Map<String, String> callInfo, List<Object> parameters, String outcome, Level level) {
    MapMessage mapMessage = new MapMessage()
        .with("class", className)
        .with("method", methodName)
        .with("parameters", parameters.toString())
        .with("outcome", outcome)
        .with("caller", email)
        .with("client-ip", callInfo.get(AnnotationHelper.CLIENT_IP))
        .with("user-agent", callInfo.get(AnnotationHelper.USER_AGENT));

    loggerFactory.getLogger().log(level, mapMessage.toString());
  }

  private String getExceptionMessage(Exception e) {
    if (e instanceof RESTException) {
      RESTException restException = (RESTException) e;
      return restException.getDevMsg() != null && !restException.getDevMsg().isEmpty() ? restException.getDevMsg() :
          restException.getErrorCode().getMessage();// user msg is not set in the exception
    }
    return e.getMessage();
  }
}
