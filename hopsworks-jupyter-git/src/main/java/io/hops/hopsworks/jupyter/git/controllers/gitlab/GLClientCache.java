/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.jupyter.git.controllers.gitlab;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.gitlab4j.api.GitLabApi;

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
public class GLClientCache {
  private static final Logger LOG = Logger.getLogger(GLClientCache.class.getName());
  private Cache<String, GitLabApi> cache;
  
  @PostConstruct
  public void init() {
    cache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterAccess(30L, TimeUnit.MINUTES)
        .build();
  }
  
  @Lock(LockType.READ)
  public GitLabApi getClient(String hostUrl, String authToken) throws ServiceException {
    try {
      return cache.get(authToken, new Callable<GitLabApi>() {
        @Override
        public GitLabApi call() throws Exception {
          GitLabApi gitLabApi = new GitLabApi(hostUrl, authToken);
          return gitLabApi;
        }
      });
    } catch (ExecutionException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_START_ERROR, Level.SEVERE,
          "Could not create GitLab client", "Could not create GitLab client in cache!", ex);
    }
  }
}
