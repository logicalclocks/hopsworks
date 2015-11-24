/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua.authz;

import javax.ejb.EJB;
import se.kth.bbc.security.ua.BBCGroup;
import se.kth.bbc.security.ua.UserManager;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
public class PolicyAdministrationPoint {

  @EJB
  private UserManager userPolicMgr;

  public boolean isInAdminRole(Users user) {
    return userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.SYS_ADMIN.
            name());
  }

  public boolean isInDataProviderRole(Users user) {
    return userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.BBC_ADMIN.
            name());
  }

  public boolean isInAuditorRole(Users user) {
    return userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.AUDITOR.
            name());
  }

  public boolean isInResearcherRole(Users user) {
    return userPolicMgr.findGroups(user.getUid()).contains(
            BBCGroup.BBC_RESEARCHER.name());
  }

  public boolean isInGuestRole(Users user) {
    return userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.BBC_GUEST.
            name());
  }

  public boolean isInAdminRole(String username) {
    Users user = userPolicMgr.getUserByUsername(username);
    return userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.SYS_ADMIN.
            name());
  }

  public boolean isInResearcherRole(String username) {
    Users user = userPolicMgr.getUserByUsername(username);
    return userPolicMgr.findGroups(user.getUid()).contains(
            BBCGroup.BBC_RESEARCHER.name());
  }

  public boolean isInDataProviderRole(String username) {
    Users user = userPolicMgr.getUserByUsername(username);
    return userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.BBC_ADMIN.
            name());
  }

  public boolean isInAuditorRole(String username) {
    Users user = userPolicMgr.getUserByUsername(username);
    return userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.AUDITOR.
            name());
  }

  public boolean isInGuestRole(String username) {
    Users user = userPolicMgr.getUserByUsername(username);
    return userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.BBC_GUEST.
            name());
  }

  public String redirectUser(Users user) {

    if (isInAdminRole(user)) {
      return "adminIndex";
    } else if (isInAuditorRole(user)) {
      return "auditIndex";
    } else if (isInDataProviderRole(user)) {
      return "indexPage";
    } else if (isInResearcherRole(user)) {
      return "indexPage";
    }

    return "indexPage";
  }
}
