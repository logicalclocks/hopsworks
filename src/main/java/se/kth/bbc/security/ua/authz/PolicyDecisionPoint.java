package se.kth.bbc.security.ua.authz;

import javax.ejb.EJB;
import se.kth.bbc.security.ua.BBCGroup;
import se.kth.bbc.security.ua.UserManager;
import se.kth.hopsworks.user.model.Users;


public class PolicyDecisionPoint {

  @EJB
  protected UserManager userManager;

  public boolean isInAdminRole(Users user) {
    return userManager.findGroups(user.getUid()).contains(BBCGroup.HOPS_ADMIN.
            name());
  }

  public boolean isInAuditorRole(Users user) {
    return userManager.findGroups(user.getUid()).contains(BBCGroup.AUDITOR.
            name());
  }

  public boolean isInUserRole(Users user) {
    return userManager.findGroups(user.getUid()).contains(
            BBCGroup.HOPS_USER.name());
  }


  public String redirectUser(Users user) {

    if (isInAdminRole(user)) {
      return "adminIndex";
    } else if (isInAuditorRole(user)) {
      return "adminAuditIndex";
    } else if (isInUserRole(user) ) {
      return "home";
    }

    return "home";
  }
}
