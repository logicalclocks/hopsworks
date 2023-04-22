/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.jobs;

import io.hops.hopsworks.common.jobs.ExecutionJWT;
import io.hops.hopsworks.common.jwt.JWTTokenWriter;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.DateUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.jwt.Constants;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.jwt.SignatureAlgorithm;
import io.hops.hopsworks.jwt.exception.JWTException;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class JobsJWTManager {
  
  private static final Logger LOG = Logger.getLogger(JobsJWTManager.class.getName());
  
  @EJB
  private Settings settings;
  @EJB
  private UsersController usersController;
  @EJB
  private JWTController jwtController;
  @Inject
  private JWTTokenWriter jwtTokenWriter;
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void materializeJWT(Users user, Project project, Execution execution)
    throws JobException {
    
    LocalDateTime expirationDate = LocalDateTime.now().plus(settings.getJWTLifetimeMs(), ChronoUnit.MILLIS);
    ExecutionJWT executionJWT = new ExecutionJWT(project, user, execution, expirationDate);
    
    try {
      String[] roles = usersController.getUserRoles(user).toArray(new String[1]);
      
      Map<String, Object> claims = new HashMap<>(3);
      claims.put(Constants.RENEWABLE, false);
      claims.put(Constants.EXPIRY_LEEWAY, settings.getJWTExpLeewaySec());
      claims.put(Constants.ROLES, roles);
  
      executionJWT.token = jwtController.createToken(settings.getJWTSigningKeyName(), false, settings.getJWTIssuer(),
        new String[]{"api"}, DateUtils.localDateTime2Date(expirationDate),
        DateUtils.localDateTime2Date(DateUtils.getNow()),
        user.getUsername(), claims, SignatureAlgorithm.valueOf(settings.getJWTSignatureAlg()));
      
      jwtTokenWriter.writeToken(executionJWT);
    } catch (GeneralSecurityException | JWTException ex) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_START_FAILED, Level.SEVERE,
        "Could not generate JWT", ex.getMessage(), ex);
    }
  }
  
  @Schedule(minute = "*/1", hour = "*", info = "Jobs JWT renew Manager")
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void monitorJWT() {
    // Get all JWTs for jobs, check creation date in Kubernetes and renew if necessary
    Map<String, String> labels = new HashMap<>(2);
    labels.put("type", "jwt");
    labels.put("deployment", "execution");
    try {
      TreeSet<ExecutionJWT> jwts = new TreeSet<>((t0, t1) -> {
        if (t0.equals(t1)) {
          return 0;
        } else {
          if (t0.expiration.isBefore(t1.expiration)) {
            return -1;
          } else if (t0.expiration.isAfter(t1.expiration)) {
            return 1;
          }
          return 0;
        }
      });
      jwts.addAll(jwtTokenWriter.getJWTs(labels));
      LOG.log(Level.FINEST, "jwts:" + jwts);
      Iterator<ExecutionJWT> iter = jwts.iterator();
      LocalDateTime now = DateUtils.getNow();
      while (iter.hasNext()) {
        ExecutionJWT element = iter.next();
        // Elements are sorted by their expiration date.
        // If element N does not need to be renewed neither does N+1
        if (element.maybeRenew(now)) {
          LOG.log(Level.FINE, "renew:" + element);
          LocalDateTime newExpirationDate = now.plus(settings.getJWTLifetimeMs(), ChronoUnit.MILLIS);
          String newToken = jwtController.renewToken(element.token, DateUtils.localDateTime2Date(newExpirationDate),
            DateUtils.localDateTime2Date(now), true, new HashMap<>(3));
          
          ExecutionJWT renewedJWT = new ExecutionJWT(element.project, element.user, element.execution,
            newExpirationDate);
          renewedJWT.token = newToken;
          jwtTokenWriter.writeToken(renewedJWT);
        } else {
          break;
        }
      }
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, "Exception while fetching Job JWTs. Keep going on...", ex);
    }
    
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void cleanJWT(ExecutionJWT executionJWT) {
    jwtTokenWriter.deleteToken(executionJWT);
  }
}
