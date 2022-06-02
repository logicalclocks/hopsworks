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

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.hops.hopsworks.audit.auditor.annotation.AuditTarget;
import io.hops.hopsworks.audit.logger.annotation.Caller;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.util.HttpUtil;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.Constants;
import io.hops.hopsworks.persistence.entity.user.Users;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.hops.hopsworks.jwt.Constants.BEARER;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

/**
 * EJB Restrictions on Using the Reflection API
 * The enterprise bean must not attempt to use the Reflection API to access information that the security rules of
 * the Java programming language make unavailable.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AnnotationHelper {
  
  public static final String USER_AGENT = "user-agent";
  public static final String CLIENT_IP = "client-ip";
  public static final String PATH_INFO = "pathInfo";
  
  @EJB
  private UserFacade userFacade;
  
  public <T extends Annotation> T getAnnotation(Class<?> resourceClass, Method method, Class<T> type) {
    return method.isAnnotationPresent(type) ? method.getAnnotation(type) :
      resourceClass.getAnnotation(type);
  }
  
  public int getAnnotatedParamIndex(Parameter[] methodParameters, Class<? extends Annotation> type) {
    int len = methodParameters != null ? methodParameters.length : 0;
    for (int i = 0; i < len; i++) {
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
    int len = parameters != null ? parameters.length : 0;
    for (int i = 0; i < len; i++) {
      if (methodParameters[i].isAnnotationPresent(type)) {
        continue;
      }
      params.add(parameters[i]);
    }
    return params;
  }
  
  public Users getAuditTarget(Method method, Object[] parameters) {
    Parameter[] methodParameters = method.getParameters();
    int len = parameters != null ? parameters.length : 0;
    for (int i = 0; i < len; i++) {
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
        } else if (parameters[i] instanceof HttpServletRequest) {
          if (((HttpServletRequest) parameters[i]).getRemoteUser() != null) {
            return userFacade.findByEmail(((HttpServletRequest) parameters[i]).getRemoteUser());
          }
          String subject = getSubjectFromJWT((HttpServletRequest) parameters[i]);
          if (subject != null && !subject.isEmpty()) {
            return userFacade.findByUsername(subject);
          }
        } else if (parameters[i] instanceof SecurityContext &&
          ((SecurityContext) parameters[i]).getUserPrincipal() != null) {
          return userFacade.findByUsername(((SecurityContext) parameters[i]).getUserPrincipal().getName());
        }
      }
    }
    return null;
  }
  
  public CallerIdentifier getCallerIdentifier(Method method, Object[] parameters) {
    Parameter[] methodParameters = method.getParameters();
    CallerIdentifier caller = new CallerIdentifier();
    if (parameters != null) {
      for (int i = 0; i < parameters.length; i++) {
        if (methodParameters[i].isAnnotationPresent(Caller.class)) {
          return getCallerIdentifier(methodParameters[i], parameters[i]);
        } else if (parameters[i] instanceof SecurityContext &&
          ((SecurityContext) parameters[i]).getUserPrincipal() != null) {
          caller.setUsername(((SecurityContext) parameters[i]).getUserPrincipal().getName());
          return caller;
        } else if (parameters[i] instanceof HttpServletRequest) {
          if (((HttpServletRequest) parameters[i]).getRemoteUser() != null) {
            caller.setEmail(((HttpServletRequest) parameters[i]).getRemoteUser());
            return caller;
          }
          String subject = getSubjectFromJWT((HttpServletRequest) parameters[i]);
          if (subject != null && !subject.isEmpty()) {
            caller.setUsername(subject);
            return caller;
          }
        }
      }
    }
    return caller;
  }
  
  public String getSubjectFromJWT(HttpServletRequest req) {
    String authorizationHeader = req.getHeader(AUTHORIZATION);
    String token = null;
    if (authorizationHeader != null && authorizationHeader.startsWith(BEARER)) {
      token = authorizationHeader.substring(Constants.BEARER.length()).trim();
    }
    if (token != null && !token.isEmpty()) {
      DecodedJWT djwt = JWT.decode(token);
      return djwt.getSubject();
    }
    return null;
  }
  
  public Map<String, String> getClientInfo(Object[] parameters) {
    Map<String, String> callInfo = new HashMap<>();
    for (Object parameter : parameters) {
      if (parameter instanceof HttpServletRequest) {
        // Extract user agent
        callInfo.put(USER_AGENT, HttpUtil.extractUserAgent((HttpServletRequest) parameter));
        // Extract client IP
        callInfo.put(CLIENT_IP, HttpUtil.extractRemoteHostIp((HttpServletRequest) parameter));
        // Extract requested path
        String pathInfo = ((HttpServletRequest) parameter).getPathInfo();
        callInfo.put(PATH_INFO, pathInfo != null ? pathInfo : "");
      }
    }
    
    return callInfo;
  }
  
  private Users getCaller(Parameter methodParameter, Object parameter) {
    UserIdentifier identifier = methodParameter.getAnnotation(Caller.class).value();
    return getUser(identifier, parameter);
  }
  
  private CallerIdentifier getCallerIdentifier(Parameter methodParameter, Object parameter) {
    UserIdentifier identifier = methodParameter.getAnnotation(Caller.class).value();
    return getCaller(identifier, parameter);
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
  
  private CallerIdentifier getCaller(UserIdentifier identifier, Object userIdentifier) {
    CallerIdentifier caller = new CallerIdentifier();
    switch (identifier) {
      case ID:
        caller.setUserId((Integer) userIdentifier);
        break;
      case EMAIL:
        caller.setEmail((String) userIdentifier);
        break;
      case USERNAME:
        caller.setUsername((String) userIdentifier);
        break;
      case USER_DTO:
        caller.setEmail(((UserDTO) userIdentifier).getEmail());
        break;
      case USERS:
        caller.setEmail(((Users) userIdentifier).getEmail());
        break;
      case KEY:
        caller.setUsername(getUserFromKey((String) userIdentifier));
        break;
      default:
        return null;
    }
    return caller;
  }
  
  private String getUserFromKey(String key) {
    if (key == null) {
      throw new IllegalArgumentException("Validation key not supplied.");
    }
    if (key.length() <= Settings.USERNAME_LENGTH) {
      throw new IllegalArgumentException("Unrecognized validation key.");
    }
    return key.substring(0, Settings.USERNAME_LENGTH);
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
}
