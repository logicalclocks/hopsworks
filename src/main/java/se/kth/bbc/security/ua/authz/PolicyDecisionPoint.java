package se.kth.bbc.security.ua.authz;

import javax.ejb.EJB;
import se.kth.bbc.security.ua.BBCGroup;
import se.kth.bbc.security.ua.UserManager;
import se.kth.hopsworks.user.model.Users;


public class PolicyDecisionPoint {

  @EJB
  protected UserManager userManager;

  public boolean isInAdminRole(Users user) {
    return userManager.findGroups(user.getUid()).contains(BBCGroup.SYS_ADMIN.
            name());
  }

  public boolean isInDataProviderRole(Users user) {
    return userManager.findGroups(user.getUid()).contains(BBCGroup.BBC_ADMIN.
            name());
  }

  public boolean isInAuditorRole(Users user) {
    return userManager.findGroups(user.getUid()).contains(BBCGroup.AUDITOR.
            name());
  }

  public boolean isInResearcherRole(Users user) {
    return userManager.findGroups(user.getUid()).contains(
            BBCGroup.BBC_RESEARCHER.name());
  }

  public boolean isInGuestRole(Users user) {
    return userManager.findGroups(user.getUid()).contains(BBCGroup.BBC_GUEST.
            name());
  }

  public String redirectUser(Users user) {

    if (isInAdminRole(user)) {
      return "adminIndex";
    } else if (isInAuditorRole(user)) {
      return "adminAuditIndex";
    } else if (isInDataProviderRole(user) || isInResearcherRole(user) ) {
      return "home";
    }

    return "home";
  }
}
