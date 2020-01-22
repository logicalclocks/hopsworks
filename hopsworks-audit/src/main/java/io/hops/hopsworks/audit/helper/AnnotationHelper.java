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

import io.hops.hopsworks.audit.auditor.annotation.AuditTarget;
import io.hops.hopsworks.audit.logger.annotation.Caller;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * EJB Restrictions on Using the Reflection API
 * The enterprise bean must not attempt to use the Reflection API to access information that the security rules of
 * the Java programming language make unavailable.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AnnotationHelper {
  
  @EJB
  private UserFacade userFacade;
  
  public <T extends Annotation> T getAnnotation(Class<?> resourceClass, Method method, Class<T> type) {
    return method.isAnnotationPresent(type)? method.getAnnotation(type) :
      resourceClass.getAnnotation(type);
  }
  
  public int getAnnotatedParamIndex(Parameter[] methodParameters, Class<? extends Annotation> type) {
    int len = methodParameters != null? methodParameters.length : 0;
    for (int i=0; i < len; i++) {
      if (methodParameters[i].isAnnotationPresent(type)) {
        return i;
      }
    }
    return -1;
  }
  
  public Object getAnnotatedParam(Method method, Object[] parameters, Class<? extends Annotation> type) {
    Parameter[] methodParameters = method.getParameters();
    int index = getAnnotatedParamIndex(methodParameters, type);
    if (index < 0) {
      return null;
    }
    return parameters[index];
  }
  
  public List<Object> getParamsExceptAnnotated(Method method, Object[] parameters, Class<? extends Annotation> type) {
    Parameter[] methodParameters = method.getParameters();
    List<Object> params = new ArrayList<>();
    int len = parameters != null? parameters.length : 0;
    for (int i=0; i < len; i++) {
      if (methodParameters[i].isAnnotationPresent(type)) {
        continue;
      }
      params.add(parameters[i]);
    }
    return params;
  }
  
  public Users getAuditTarget(Method method, Object[] parameters) {
    Parameter[] methodParameters = method.getParameters();
    int len = parameters != null? parameters.length : 0;
    for (int i=0; i < len; i++) {
      if (methodParameters[i].isAnnotationPresent(AuditTarget.class)) {
        UserIdentifier identifier = methodParameters[i].getAnnotation(AuditTarget.class).value();
        return getUser(identifier, parameters[i]);
      }
    }
    return null;
  }
  
  public Users getCaller(Method method, Object[] parameters) {
    Parameter[] methodParameters = method.getParameters();
    if (parameters != null && parameters.length > 0) {
      for (int i = 0; i < parameters.length; i++) {
        if (methodParameters[i].isAnnotationPresent(Caller.class)) {
          return getCaller(methodParameters[i], parameters[i]);
        } else if (parameters[i] instanceof HttpServletRequest &&
          ((HttpServletRequest) parameters[i]).getRemoteUser() != null) {
          return userFacade.findByEmail(((HttpServletRequest) parameters[i]).getRemoteUser());
        } else if (parameters[i] instanceof SecurityContext &&
          ((SecurityContext) parameters[i]).getUserPrincipal() != null) {
          return userFacade.findByUsername(((SecurityContext) parameters[i]).getUserPrincipal().getName());
        }
      }
    }
    return null;
  }
  
  public String getCallerName(Method method, Object[] parameters) {
    Parameter[] methodParameters = method.getParameters();
    if (parameters != null && parameters.length > 0) {
      for (int i = 0; i < parameters.length; i++) {
        if (methodParameters[i].isAnnotationPresent(Caller.class)) {
          return getCallerStr(methodParameters[i], parameters[i]);
        } else if (parameters[i] instanceof HttpServletRequest &&
          ((HttpServletRequest) parameters[i]).getRemoteUser() != null) {
          return ((HttpServletRequest) parameters[i]).getRemoteUser();
        } else if (parameters[i] instanceof SecurityContext &&
          ((SecurityContext) parameters[i]).getUserPrincipal() != null) {
          return ((SecurityContext) parameters[i]).getUserPrincipal().getName();
        }
      }
    }
    return "";
  }
  
  public Users getCaller(Parameter methodParameter, Object parameter) {
    UserIdentifier identifier = methodParameter.getAnnotation(Caller.class).value();
    return getUser(identifier, parameter);
  }
  
  public String getCallerStr(Parameter methodParameter,Object parameter) {
    UserIdentifier identifier = methodParameter.getAnnotation(Caller.class).value();
    return getUserStr(identifier, parameter);
  }
  
  private Users getUser(UserIdentifier identifier, Object userIdentifier) {
    Users user = null;
    switch (identifier) {
      case ID:
        user = userFacade.find((Integer) userIdentifier);
        break;
      case KEY:
        user = userFacade.findByUsername(getUserFromKey((String) userIdentifier));
        break;
      case EMAIL:
        user = userFacade.findByEmail((String) userIdentifier);
        break;
      case USERNAME:
        user = userFacade.findByUsername((String) userIdentifier);
        break;
      case USERS:
        user = (Users) userIdentifier;
        break;
      case USER_DTO:
        user = userFacade.findByEmail(((UserDTO) userIdentifier).getEmail());
        break;
      case REQ:
        user = getUserFromReq(userIdentifier);
        break;
    }
    return user;
  }
  
  private String getUserStr(UserIdentifier identifier, Object userIdentifier) {
    switch (identifier) {
      case ID:
      case EMAIL:
      case USERNAME:
        return (String) userIdentifier;
      case USER_DTO:
        return ((UserDTO) userIdentifier).getEmail();
      case USERS:
        return ((Users) userIdentifier).getEmail();
      case KEY:
        return getUserFromKey((String) userIdentifier);
    }
    return null;
  }
  
  private String getUserFromKey(String key) {
    if (key == null) {
      throw new IllegalArgumentException("Validation key not supplied.");
    }
    if (key.length() <= Settings.USERNAME_LENGTH) {
      throw new IllegalArgumentException("Unrecognized validation key.");
    }
    String userName = key.substring(0, Settings.USERNAME_LENGTH);
    return userName;
  }
  
  public Users getUserFromReq(Object userIdentifier) {
    if (userIdentifier instanceof HttpServletRequest) {
      return userFacade.findByEmail(((HttpServletRequest) userIdentifier).getRemoteUser());
    } else if (userIdentifier instanceof SecurityContext) {
      SecurityContext sc = (SecurityContext) userIdentifier;
      return sc.getUserPrincipal() != null ? userFacade.findByUsername(sc.getUserPrincipal().getName()) : null;
    }
    return null;
  }
  
  public Users getUserFromReq(HttpServletRequest req) {
    if (req != null) {
      return userFacade.findByEmail(req.getRemoteUser());
    }
    return null;
  }
  
}
