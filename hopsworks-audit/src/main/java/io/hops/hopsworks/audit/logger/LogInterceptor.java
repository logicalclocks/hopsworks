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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.audit.helper.AnnotationHelper;
import io.hops.hopsworks.audit.helper.AuditHelper;
import io.hops.hopsworks.audit.helper.CallerIdentifier;
import io.hops.hopsworks.audit.helper.LogMessage;
import io.hops.hopsworks.audit.helper.LoggerFactory;
import io.hops.hopsworks.audit.helper.VariablesHelper;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

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
  @Inject
  private VariablesHelper variablesHelper;
  private final ObjectMapper mapper = new ObjectMapper();
  
  @AroundInvoke
  public Object collectBasicLoggingInformation(InvocationContext context) throws Exception {
    Method method = context.getMethod();
    Class<?> resourceClass = method.getDeclaringClass();

    Logged logged = annotationHelper.getAnnotation(resourceClass, method, Logged.class);
    Level level = logged.logLevel().getLevel();
    if (LogLevel.OFF.equals(logged.logLevel())) {
      return context.proceed();
    }
  
    CallerIdentifier caller = annotationHelper.getCallerIdentifier(method, context.getParameters());
    Map<String, String> clientInfo = annotationHelper.getClientInfo(context.getParameters());
    List<Object> params = annotationHelper.getParamsExceptAnnotated(method, context.getParameters(), Secret.class);
    String projectName = annotationHelper.getProjectName(method, context.getParameters());

    Object ret;
    try {
      ret = context.proceed();
    } catch (Exception e) {
      log(resourceClass.getName(), method.getName(), caller, projectName, clientInfo, params, getExceptionMessage(e),
        level);
      throw e;
    }
    if (method.getReturnType().equals(Void.TYPE)) {
      log(resourceClass.getName(), method.getName(), caller, projectName, clientInfo, params, "void", level);
    } else {
      Response res = auditHelper.getResponse(ret);
      if (res != null) {
        log(resourceClass.getName(), method.getName(), caller, projectName, clientInfo, params,
          String.valueOf(res.getStatus()), level);
      } else {
        log(resourceClass.getName(), method.getName(), caller, projectName, clientInfo, params,
          ret.getClass().getName(), level);
      }
    }
    return ret;
  }

  private void log(String className, String methodName, CallerIdentifier caller, String projectName,
                   Map<String, String> callInfo, List<Object> parameters, String outcome, Level level) {
    String format = variablesHelper.getAuditLogFileType();
    if (format.equals(SimpleFormatter.class.getName())) {
      MapMessage mapMessage = new MapMessage()
        .with("class", className)
        .with("method", methodName)
        .with("parameters", parameters.toString())
        .with("outcome", outcome != null ? outcome : "")
        .with("email", caller.getEmail() != null ? caller.getEmail() : "")
        .with("username", caller.getUsername() != null ? caller.getUsername() : "")
        .with("user-id", caller.getUserId() != null ? caller.getUserId() : "")
        .with("project", projectName != null ? projectName : "")
        .with("path-info", callInfo.get(AnnotationHelper.PATH_INFO))
        .with("client-ip", callInfo.get(AnnotationHelper.CLIENT_IP))
        .with("user-agent", callInfo.get(AnnotationHelper.USER_AGENT));
  
      loggerFactory.getLogger().log(level, mapMessage.toString());
    } else {
      String dateFormat = variablesHelper.getAuditLogDateFormat();
      LogMessage logMessage = new LogMessage();
      logMessage.setClassName(className);
      logMessage.setMethodName(methodName);
      logMessage.setParameters(parameters.toString());
      logMessage.setCaller(caller);
      logMessage.setProjectName(projectName);
      logMessage.setOutcome(outcome != null ? outcome : "");
      logMessage.setDateTime(new SimpleDateFormat(dateFormat).format(new Date()));
      logMessage.setUserAgent(callInfo.get(AnnotationHelper.USER_AGENT));
      logMessage.setClientIp(callInfo.get(AnnotationHelper.CLIENT_IP));
      logMessage.setPathInfo(callInfo.get(AnnotationHelper.PATH_INFO));
      
      loggerFactory.getLogger().log(level, toJson(logMessage));
    }
  }
  
  private String toJson(LogMessage logMessage) {
    String jsonInString = null;
    try {
      jsonInString = mapper.writeValueAsString(logMessage);
    } catch (JsonProcessingException ex) {
      Logger.getLogger(LogInterceptor.class.getName()).log(Level.SEVERE, null, ex);
    }
    return jsonInString;
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
