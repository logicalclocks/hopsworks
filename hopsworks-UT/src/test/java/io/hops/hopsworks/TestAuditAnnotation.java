/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks;

import io.hops.hopsworks.audit.auditor.AuditType;
import io.hops.hopsworks.audit.auditor.annotation.AuditTarget;
import io.hops.hopsworks.audit.auditor.annotation.Audited;
import io.hops.hopsworks.audit.auditor.annotation.AuditedList;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Caller;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.audit.logger.annotation.Secret;
import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.SecurityContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestAuditAnnotation {
  Set<Class<?>> apiClasses = new HashSet<>();
  Set<Class<?>> loggedAnnotated = new HashSet<>();
  Set<Method> auditedAnnotated = new HashSet<>();
  
  @Before
  public void beforeTest() {
    Reflections reflections = new Reflections("io.hops.hopsworks", new SubTypesScanner(false),
      new TypeAnnotationsScanner());
    for (Class<?> c : reflections.getTypesAnnotatedWith(Path.class, true)) {
      if (c.getName().startsWith("io.hops.hopsworks.api")) {
        apiClasses.add(c);
      }
    }
  
    for (Class<?> c : reflections.getTypesAnnotatedWith(RequestScoped.class, true)) {
      if (c.getName().startsWith("io.hops.hopsworks.api")) {
        apiClasses.add(c);
      }
    }
    
    for (Class<?> c : reflections.getTypesAnnotatedWith(Logged.class, true)) {
      if (!c.getName().startsWith("io.hops.hopsworks.audit")) {
        loggedAnnotated.add(c);
      }
    }
  
    reflections = new Reflections("io.hops.hopsworks", new MethodAnnotationsScanner());
    for (Method method : reflections.getMethodsAnnotatedWith(Logged.class)) {
      Class<?> c = method.getDeclaringClass();
      if (!c.getName().startsWith("io.hops.hopsworks.audit")) {
        loggedAnnotated.add(c);
      }
    }
    
    for (Method method : reflections.getMethodsAnnotatedWith(AuditedList.class)) {
      Class<?> c = method.getDeclaringClass();
      if (!c.getName().startsWith("io.hops.hopsworks.audit")) {
        auditedAnnotated.add(method);
      }
    }
    for (Method method : reflections.getMethodsAnnotatedWith(Audited.class)) {
      Class<?> c = method.getDeclaringClass();
      if (!c.getName().startsWith("io.hops.hopsworks.audit")) {
        auditedAnnotated.add(method);
      }
    }
  }
  
  @Test
  public void testIfClassesAreFound() {
    assertTrue("Api classes should not be empty.", apiClasses.size() > 0);
    assertTrue("Logged annotated classes should not be empty.", loggedAnnotated.size() > 0);
    assertTrue("Audited annotated classes should not be empty.", auditedAnnotated.size() > 0);
  }
  
  @Test
  public void testIfAllApiClassesAreLogged() {
    Set<Class<?>> notAnnotatedClasses = new HashSet<>();
    for (Class<?> c : apiClasses) {
      if (c.getAnnotation(Logged.class) == null) {
        notAnnotatedClasses.add(c);
      }
    }
    StringBuilder classNames = new StringBuilder();
    boolean annotated;
    for (Class<?> c : notAnnotatedClasses) {
      annotated = true;
      for (Method m : c.getDeclaredMethods()) {
        if (m.getAnnotation(Logged.class) == null) { //if all methods in the class are not annotated
          annotated = false;
        }
      }
      if (!annotated) {
        classNames.append(c.getCanonicalName());
        classNames.append(", ");
      }
    }
    assertFalse("Following classes are not annotated with Logged: " + classNames, classNames.length() > 0);
  }
  
  @Test
  public void testCanLog() {
    Set<Class<?>> loggedAnnotatedClasses = new HashSet<>(apiClasses);
    loggedAnnotatedClasses.addAll(loggedAnnotated);
    for (Class<?> c : loggedAnnotatedClasses) {
      if (c.getAnnotation(Logged.class) != null && !LogLevel.OFF.equals(c.getAnnotation(Logged.class).logLevel())) {
        for (Method m : c.getDeclaredMethods()) {
          if(!Modifier.isPublic(m.getModifiers())) {
            continue;
          }
          Logged methodAnnotation = m.getAnnotation(Logged.class);
          //method is not annotated or log level is not off the check if required params are present
          if (methodAnnotation == null || !LogLevel.OFF.equals(methodAnnotation.logLevel())) {
            assertMethodCanBeLogged(c, m);
          }
        }
      } else if (c.getAnnotation(Logged.class) == null) {
        for (Method m : c.getDeclaredMethods()) {
          if (!Modifier.isPublic(m.getModifiers())) {
            continue;
          }
          Logged methodAnnotation = m.getAnnotation(Logged.class);
          //method is not annotated or log level is not off the check if required params are present
          if (methodAnnotation != null && !LogLevel.OFF.equals(methodAnnotation.logLevel())) {
            assertMethodCanBeLogged(c, m);
          }
        }
      }
    }
  }
  
  private void assertMethodCanBeLogged(Class<?> c, Method m) {
    assertFalse("No parameter found for method " + m.getName() + " in class " + c.getCanonicalName() +
      ". Method should be annotated with @Logged(logLevel = LogLevel.OFF) or contain parameter that can be " +
      "used to identify the caller of the method.", m.getParameterCount() == 0);
    boolean canIdentifyCaller = false;
    for (int i = 0; i < m.getParameterCount(); i++) {
      if (m.getParameterTypes()[i].isAnnotationPresent(Caller.class) ||
        m.getParameterTypes()[i].isAssignableFrom(SecurityContext.class) ||
        m.getParameterTypes()[i].isAssignableFrom(HttpServletRequest.class)) {
        canIdentifyCaller = true;
        break;
      }
    }
    assertTrue("No parameter was found that can be used to identify the caller of method " + m.getName() +
      " in class " + c.getCanonicalName(), canIdentifyCaller);
  }
  
  @Test
  public void testNoSensitiveParamIsLogged() {
    Set<Class<?>> loggedAnnotatedClasses = new HashSet<>(apiClasses);
    loggedAnnotatedClasses.addAll(loggedAnnotated);
    for (Class<?> c : loggedAnnotatedClasses) {
      if (c.getAnnotation(Logged.class) != null && !LogLevel.OFF.equals(c.getAnnotation(Logged.class).logLevel())) {
        for (Method m : c.getDeclaredMethods()) {
          if(!Modifier.isPublic(m.getModifiers())) {
            continue;
          }
          Logged methodAnnotation = m.getAnnotation(Logged.class);
          //method is not annotated or log level is not off the check if required params are present
          if (methodAnnotation == null || !LogLevel.OFF.equals(methodAnnotation.logLevel())) {
            Annotation[][] annotations = m.getParameterAnnotations();
            boolean isSensitiveParam;
            for (int i= 0; i < annotations.length; i++) {
              isSensitiveParam = false;
              for (Annotation annotation : annotations[i]) {
                if (isSensitiveParam(annotation)) {
                  isSensitiveParam = true;
                  break;
                }
              }
              if (isSensitiveParam) {
                assertTrue("Sensitive parameter not annotated with  @Secret in class " + c.getCanonicalName() +
                  " method " + m.getName(), annotations[i].length > 1);
                boolean isAnnotatedWithSecret = false;
                for (Annotation annotation : annotations[i]) {
                  if (annotation instanceof Secret) {
                    isAnnotatedWithSecret = true;
                    break;
                  }
                }
                assertTrue("Sensitive parameter not annotated with  @Secret in class " + c.getCanonicalName() +
                  " method " + m.getName(), isAnnotatedWithSecret);
              }
            }
          }
        }
      }
    }
  }
  
  //Add test for logging off if response is not of type Response
  
  //Add test for audit
  
  @Test
  public void testCanAudit() {
    for (Method method : auditedAnnotated) {
      //if AuditType == ACCOUNT_AUDIT || ROLE_AUDIT check AuditTarget and caller
      //if AuditType == USER_LOGIN check caller
      boolean canIdentifyCaller = false;
      boolean canIdentifyTarget = method.getAnnotation(Audited.class) != null &&
        AuditType.USER_LOGIN.equals(method.getAnnotation(Audited.class).type());
      Annotation[][] annotations = method.getParameterAnnotations();
      for (int i = 0; i < method.getParameterCount(); i++) {
        for (int j=0; j < annotations[i].length; j++) {
          if (annotations[i][j] instanceof Caller) {
            canIdentifyCaller = true;
          }
          if (annotations[i][j] instanceof AuditTarget) {
            canIdentifyTarget = true;
          }
        }
        //if not annotated just check param type
        if (!canIdentifyCaller && (method.getParameterTypes()[i].isAssignableFrom(SecurityContext.class) ||
          method.getParameterTypes()[i].isAssignableFrom(HttpServletRequest.class))) {
          canIdentifyCaller = true;
        }
        if (canIdentifyCaller && canIdentifyTarget) {
          break;
        }
      }
      assertTrue("Audited method " + method.getName() + " in class " +  method.getDeclaringClass().getName() +
        " has no parameter that can be used to identify the initiator." , canIdentifyCaller);
      assertTrue("Audited method " + method.getName() + " in class " +  method.getDeclaringClass().getName() +
        " has no parameter that can be used to identify the target.", canIdentifyTarget);
      
    }
  }
  
  private boolean isSensitiveParam(Annotation annotation) {
    String formParamValue = annotation instanceof FormParam ? ((FormParam) annotation).value() : null;
    String queryParamValue = annotation instanceof QueryParam ? ((QueryParam) annotation).value() : null;
    
    return (formParamValue != null &&
      (formParamValue.toLowerCase().contains("password") || formParamValue.toLowerCase().contains("pwd") ||
        formParamValue.toLowerCase().contains("security"))) ||
      (queryParamValue != null &&
        (queryParamValue.toLowerCase().contains("password") || queryParamValue.toLowerCase().contains("pwd") ||
          queryParamValue.toLowerCase().contains("security")));
  }
  
}
