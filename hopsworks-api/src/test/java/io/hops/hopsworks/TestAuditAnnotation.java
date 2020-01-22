package io.hops.hopsworks;

import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Caller;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.audit.logger.annotation.Secret;
import io.hops.hopsworks.rest.application.config.ApplicationConfig;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class TestAuditAnnotation {
  Set<Class<?>> classes = new HashSet<>();
  @Before
  public void getRestApiEndPointClasses() {
    ApplicationConfig applicationConfig = new ApplicationConfig();
    for (Class<?> c : applicationConfig.getClasses()) {
      classes.add(c);
    }
    //exclude from test
    classes.remove(io.swagger.jaxrs.listing.ApiListingResource.class);
    classes.remove(io.swagger.jaxrs.listing.SwaggerSerializers.class);
    classes.remove(org.glassfish.jersey.media.multipart.MultiPartFeature.class);
    classes.remove(io.hops.hopsworks.api.exception.mapper.RESTApiThrowableMapper.class);
  
    classes.remove(io.hops.hopsworks.api.filter.JWTAutoRenewFilter.class);
    classes.remove(io.hops.hopsworks.api.filter.apiKey.ApiKeyFilter.class);
    classes.remove(io.hops.hopsworks.api.filter.AuthFilter.class);
    classes.remove(io.hops.hopsworks.api.filter.ProjectAuthFilter.class);
    //classes.remove(io.hops.hopsworks.api.filter.AuthFilter.class);
  }
  
  @Test
  public void testAnnotatedClasses() {
    Set<Class<?>> notAnnotatedClasses = new HashSet<>();
    for (Class<?> c : classes) {
      if (c.getAnnotation(Logged.class) == null) {
        notAnnotatedClasses.add(c);
      }
    }
    StringBuilder classNames = new StringBuilder();
    boolean annotated;
    for (Class<?> c : notAnnotatedClasses) {
      annotated = false;
      for (Method m : c.getDeclaredMethods()) {
        if (m.getAnnotation(Logged.class) != null) { //if a method in the class is annotated
          annotated = true;
        }
      }
      if (!annotated) {
        classNames.append(c.getCanonicalName());
        classNames.append(", \n");
      }
    }
    assertFalse("Following classes are not annotated with Logged: \n" + classNames, classNames.length() > 0);
  }
  
  @Test
  public void testAnnotatedClassMethods() {
    for (Class<?> c : classes) {
      if (c.getAnnotation(Logged.class) != null && !LogLevel.OFF.equals(c.getAnnotation(Logged.class).logLevel())) {
        for (Method m : c.getDeclaredMethods()) {
          if(!Modifier.isPublic(m.getModifiers())) {
            continue;
          }
          Logged methodAnnotation = m.getAnnotation(Logged.class);
          //method is not annotated or log level is not off the check if required params are present
          if (methodAnnotation == null || !LogLevel.OFF.equals(methodAnnotation.logLevel())) {
            Annotation[][] annotations = m.getParameterAnnotations();
            assertFalse("No parameter found for method " + m.getName() + " in class " + c.getCanonicalName() +
                ". Method should be annotated with @Logged(logLevel = LogLevel.OFF) or contain parameter that can be " +
                "used to identify the caller of the method.", m.getParameterCount() == 0);
            boolean canIdentifyCaller = false;
            for (int i= 0; i < annotations.length; i++) {
              for (Annotation annotation : annotations[i]) {
                if (annotation instanceof Caller || m.getParameterTypes()[i].isAssignableFrom(SecurityContext.class) ||
                  m.getParameterTypes()[i].isAssignableFrom(HttpServletRequest.class)) {
                  canIdentifyCaller = true;
                  break;
                }
              }
              if (canIdentifyCaller) {
                break;
              }
            }
            assertTrue("No parameter was found that can be used to identify the caller of method " + m.getName() +
                " in class " + c.getCanonicalName(), canIdentifyCaller);
          }
        }
      }
    }
  }
  
  @Test
  public void testNoSensitiveIsLogged() {
    for (Class<?> c : classes) {
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
