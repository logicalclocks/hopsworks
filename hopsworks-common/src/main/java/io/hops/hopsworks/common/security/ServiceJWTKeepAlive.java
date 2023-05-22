/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Strings;
import io.hops.hopsworks.common.util.DateUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.jwt.exception.JWTException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hadoop.util.BackOff;
import org.apache.hadoop.util.ExponentialBackOff;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@DependsOn("Settings")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ServiceJWTKeepAlive {
  
  private final static Logger LOGGER = Logger.getLogger(ServiceJWTKeepAlive.class.getName());
  private final static List<String> SERVICE_RENEW_JWT_AUDIENCE = new ArrayList<>(1);
  static {
    SERVICE_RENEW_JWT_AUDIENCE.add("services");
  }
  
  @EJB
  private Settings settings;
  @EJB
  private JWTController jwtController;
  @Resource
  private TimerService timerService;

  private String hostname;
  private BackOff backOff;
  
  @PostConstruct
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void init() {
    hostname = "hopsworks";
    backOff = new ExponentialBackOff.Builder()
        .setInitialIntervalMillis(100L)
        .setMaximumIntervalMillis(2000L)
        .setMultiplier(2)
        // We issue five renewal tokens so we have four retries
        .setMaximumRetries(4)
        .build();
    
    // Do not whirl like a sufi
    long interval = Math.min(10000,
        Math.max(500L, settings.getServiceJWTLifetimeMS() / 2));
    timerService.createIntervalTimer(5000L, interval, new TimerConfig("Service JWT renewer", false));
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  @Timeout
  @Lock(LockType.WRITE)
  public void renewServiceToken() {
    try {
      doRenew(false);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Error renewing service JWT", ex);
    }
  }
  
  @Lock(LockType.WRITE)
  public void forceRenewServiceToken() throws JWTException {
    try {
      doRenew(true);
    } catch (InterruptedException ex) {
      LOGGER.log(Level.SEVERE, "Could not renew service JWT", ex);
      throw new JWTException(ex.getMessage(), ex);
    }
  }
  
  private void doRenew(boolean force)
      throws JWTException, InterruptedException {
    String masterToken = settings.getServiceMasterJWT();
    if (Strings.isNullOrEmpty(masterToken)) {
      throw new JWTException("Master token is empty!");
    }
    LocalDateTime now = DateUtils.getNow();
    DecodedJWT masterJWT = jwtController.decodeToken(masterToken);
    if (force || maybeRenewMasterToken(masterJWT, now)) {
      String[] renewalTokens = settings.getServiceRenewJWTs();
      List<String> masterJWTRoles = getJWTRoles(masterJWT);
      String user = masterJWT.getSubject();
  
      backOff.reset();
      int renewIdx = 0;
      while (renewIdx < renewalTokens.length) {
        String oneTimeToken = renewalTokens[renewIdx];
        Date notBefore = DateUtils.localDateTime2Date(now);
        LocalDateTime expiresAt = now.plus(settings.getServiceJWTLifetimeMS(), ChronoUnit.MILLIS);
        try {
          Pair<String, String[]> renewedTokens = jwtController.renewServiceToken(oneTimeToken, masterToken,
              DateUtils.localDateTime2Date(expiresAt), notBefore, settings.getServiceJWTLifetimeMS(), user,
              masterJWTRoles, SERVICE_RENEW_JWT_AUDIENCE, hostname, settings.getJWTIssuer(),
              settings.getJWTSigningKeyName(), force);
          LOGGER.log(Level.FINEST, "New master JWT: " + renewedTokens.getLeft());
          updateTokens(renewedTokens);
          LOGGER.log(Level.FINEST, "Invalidating JWT: " + masterToken);
          jwtController.invalidateServiceToken(masterToken, settings.getJWTSigningKeyName());
          break;
        } catch (JWTException | NoSuchAlgorithmException ex) {
          renewIdx++;
          Long backoffTimeout = backOff.getBackOffInMillis();
          if (backoffTimeout != -1) {
            LOGGER.log(Level.WARNING, "Failed to renew service JWT, retrying in {0} ms. {1}",
              new Object[]{backoffTimeout, ex.getMessage()});
            TimeUnit.MILLISECONDS.sleep(backoffTimeout);
          } else {
            backOff.reset();
            throw new JWTException("Cannot renew service JWT", ex);
          }
        }
      }
      LOGGER.log(Level.FINE, "Successfully renewed service JWT");
    }
  }
  
  private void updateTokens(Pair<String, String[]> tokens) {
    settings.setServiceMasterJWT(tokens.getLeft());
    settings.setServiceRenewJWTs(tokens.getRight());
  }
  
  private List<String> getJWTRoles(DecodedJWT jwt) {
    String[] rolesArray = jwtController.getRolesClaim(jwt);
    return Arrays.asList(rolesArray);
  }
  
  private boolean maybeRenewMasterToken(DecodedJWT jwt, LocalDateTime now) {
    LocalDateTime expiresAt = DateUtils.date2LocalDateTime(jwt.getExpiresAt());
    return expiresAt.isBefore(now) || expiresAt.isEqual(now);
  }
}
