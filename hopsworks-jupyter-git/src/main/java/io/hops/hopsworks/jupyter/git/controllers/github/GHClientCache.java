/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.jupyter.git.controllers.github;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretPlaintext;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.eclipse.egit.github.core.client.GitHubClient;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class GHClientCache {
  private static final Logger LOG = Logger.getLogger(GHClientCache.class.getName());
  private Cache<String, GitHubClient> cache;
  
  @PostConstruct
  public void init() {
    cache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterAccess(30L, TimeUnit.MINUTES)
        .build();
  }
  
  @Lock(LockType.READ)
  public GitHubClient getClient(String hostUrl, SecretPlaintext authToken) throws ServiceException {
    try {
      String authTokenPlainText = authToken == null ? null : authToken.getPlaintext();
      return cache.get(authTokenPlainText == null ? hostUrl : authTokenPlainText, new Callable<GitHubClient>() {
        @Override
        public GitHubClient call() throws Exception {
          GitHubClient client = new GitHubClient(hostUrl);
          client.setOAuth2Token(authTokenPlainText);
          return client;
        }
      });
    } catch (ExecutionException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_START_ERROR, Level.SEVERE,
          "Could not create GitHub client", "Could not create GitHub client in cache!", ex);
    }
  }
}
