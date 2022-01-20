/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.user;

import io.hops.hopsworks.common.user.UserAccountHandler;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.serving.utils.KubeServingUtils;
import io.hops.hopsworks.persistence.entity.user.BbcGroup;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.logging.Level.INFO;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KubeUserAccountHandler implements UserAccountHandler {
  
  private static final Logger logger = Logger.getLogger(KubeUserAccountHandler.class.getName());
  
  @EJB
  private KubeClientService kubeClientService;

  @Override
  public void create(Users user) {
    updateUser(user);
  }
  
  @Override
  public void update(Users user) {
    updateUser(user);
  }
  
  @Override
  public void remove(Users user) {
    Map<String, String> userAccountMap = new HashMap<>();
    userAccountMap.put(user.getUsername(), null);
    logger.log(INFO, "Removing user account " + user.getUsername() + ": " + String.join(", ",
      userAccountMap.keySet()));
    patch(userAccountMap);
  }
  
  private void updateUser(Users user) {
    String roles = null;
    if (user.getStatus() == UserAccountStatus.ACTIVATED_ACCOUNT) {
      // only keep activated users in the config map
      roles = user.getBbcGroupCollection().stream().map(BbcGroup::getGroupName).collect(Collectors.joining(","));
    } else {
      // if user is not activated, remove it from the config map
      remove(user);
      return;
    }
    Map<String, String> userAccountMap = new HashMap<>();
    userAccountMap.put(user.getUsername(), roles);
    logger.log(INFO, "Create or update user account " + user.getUsername() + ": " + String.join(", ",
      userAccountMap.keySet()));
    patch(userAccountMap);
  }
  
  private void patch(Map<String, String> userAccountStatus) {
    kubeClientService.patchConfigMap(KubeServingUtils.HOPS_SYSTEM_NAMESPACE,
      KubeServingUtils.HOPS_SYSTEM_USERS, userAccountStatus);
  }
  
  @Override
  public String getClassName() { return KubeUserAccountHandler.class.getName(); }
}
