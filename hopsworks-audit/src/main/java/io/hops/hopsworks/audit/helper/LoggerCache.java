/*
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.audit.helper;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.concurrent.TimeUnit;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class LoggerCache {
  private LoadingCache<String, CallerIdentifier> callerCache;
  private LoadingCache<Integer, CallerIdentifier> callerCacheById;
  private LoadingCache<Integer, String> projectCache;
  
  @EJB
  private UserFacade userFacade;
  @EJB
  private ProjectFacade projectFacade;
  
  @PostConstruct
  public void init() {
    callerCache = Caffeine.newBuilder()
      .expireAfterWrite(15, TimeUnit.MINUTES)
      .maximumSize(50)
      .build(this::getCallerByUsername);
    callerCacheById = Caffeine.newBuilder()
      .expireAfterWrite(15, TimeUnit.MINUTES)
      .maximumSize(50)
      .build(this::getCallerByUserId);
    projectCache = Caffeine.newBuilder()
      .expireAfterWrite(15, TimeUnit.MINUTES)
      .maximumSize(50)
      .build(this::getProjectName);
  }
  
  private CallerIdentifier getCallerByUsername(String username) {
    Users user = userFacade.findByUsername(username);
    if (user != null) {
      return new CallerIdentifier(user);
    }
    return null;
  }
  
  private CallerIdentifier getCallerByUserId(Integer userId) {
    Users user = userFacade.find(userId);
    if (user != null) {
      return new CallerIdentifier(user);
    }
    return null;
  }
  
  private String getProjectName(Integer projectId) {
    Project project = projectFacade.find(projectId);
    return project != null ? project.getName() : null;
  }
  
  public CallerIdentifier getCaller(String username) {
    CallerIdentifier callerIdentifier = callerCache.get(username);
    return callerIdentifier != null ? callerIdentifier : new CallerIdentifier(username);
  }
  
  public CallerIdentifier getCaller(Integer userId) {
    CallerIdentifier callerIdentifier = callerCacheById.get(userId);
    return callerIdentifier != null ? callerIdentifier : new CallerIdentifier(userId);
  }
  
  public String getProject(Integer projectId) {
    String projectName = projectCache.get(projectId);
    return projectName != null ? projectName : "";
  }
  
  public String getProject(String projectId) {
    try {
      return getProject(Integer.valueOf(projectId));
    } catch (NumberFormatException ignored) {
    }
    return "";
  }
  
}
