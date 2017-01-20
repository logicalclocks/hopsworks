package io.hops.hopsworks.admin.security.ua;

import javax.ejb.EJB;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;

public class PolicyDecisionPoint {

  @EJB
  protected UserManager userManager;

  @EJB
  private BbcGroupFacade bbcGroupFacade;

  public boolean isInAdminRole(Users user) {
    return userManager.findGroups(user.getUid()).contains("HOPS_ADMIN");
  }

  public boolean isInAuditorRole(Users user) {
    return userManager.findGroups(user.getUid()).contains("AUDITOR");
  }

  public boolean isInUserRole(Users user) {
    return userManager.findGroups(user.getUid()).contains("HOPS_USER");
  }

  public String redirectUser(Users user) {

    if (isInAdminRole(user)) {
      return "adminIndex";
    } else if (isInAuditorRole(user)) {
      return "adminAuditIndex";
    } else if (isInUserRole(user)) {
      return "home";
    }

    return "home";
  }
}
