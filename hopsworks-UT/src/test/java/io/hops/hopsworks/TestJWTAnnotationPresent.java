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

import io.hops.hopsworks.api.filter.JWTNotRequired;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestJWTAnnotationPresent {
  Set<Class<?>> apiClasses = new HashSet<>();
  Set<Method> jwtAnnotated = new HashSet<>();
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
    
    reflections = new Reflections("io.hops.hopsworks", new MethodAnnotationsScanner());
    for (Method method : reflections.getMethodsAnnotatedWith(JWTRequired.class)) {
      Class<?> c = method.getDeclaringClass();
      if (!c.getName().startsWith("io.hops.hopsworks.api.filter")) {
        jwtAnnotated.add(method);
      }
    }
    
    for (Method method : reflections.getMethodsAnnotatedWith(JWTRequired.class)) {
      Class<?> c = method.getDeclaringClass();
      if (!c.getName().startsWith("io.hops.hopsworks.api.filter")) {
        jwtAnnotated.add(method);
      }
    }
  }
  
  @Test
  public void testIfClassesAreFound() {
    assertTrue("Api classes should not be empty.", apiClasses.size() > 0);
    assertTrue("JWTRequired annotated classes should not be empty.", jwtAnnotated.size() > 0);
  }
  
  @Test
  public void testIfAllApiClassesRequireJWT() {
    Set<Class<?>> notAnnotatedClasses = new HashSet<>();
    for (Class<?> c : apiClasses) {
      if (c.getAnnotation(JWTRequired.class) == null && c.getAnnotation(RolesAllowed.class) == null) {
        notAnnotatedClasses.add(c);
      }
    }
    StringBuilder classNames = new StringBuilder();
    boolean annotated;
    for (Class<?> c : notAnnotatedClasses) {
      annotated = true;
      StringBuilder methodNames = new StringBuilder();
      for (Method m : c.getDeclaredMethods()) {
        if (m.getAnnotation(JWTRequired.class) == null && Response.class.isAssignableFrom(m.getReturnType()) && Modifier
          .isPublic(m.getModifiers()) && m.getAnnotation(JWTNotRequired.class) == null) {
          // if a public method in the class is not annotated and returns Response
          methodNames.append(m.getName());
          methodNames.append(", ");
          annotated = false;
        }
      }
      if (!annotated) {
        classNames.append(c.getCanonicalName());
        classNames.append(": Method/s: {");
        classNames.append(methodNames.toString());
        classNames.append("}");
        classNames.append(", ");
      }
    }
    assertFalse("Following classes contain public methods not annotated with JWTRequired: " + classNames,
      classNames.length() > 0);
  }
}
