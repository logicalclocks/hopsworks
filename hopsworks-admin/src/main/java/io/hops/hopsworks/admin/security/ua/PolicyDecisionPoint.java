package io.hops.hopsworks.admin.security.ua;

import javax.ejb.EJB;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.user.UsersController;

public class PolicyDecisionPoint {

  @EJB
  private UsersController userController;


  public boolean isInAdminRole(Users user) {
    return userController.isUserInRole(user, "HOPS_ADMIN");
  }

  public boolean isInAuditorRole(Users user) {
    return userController.isUserInRole(user, "AUDITOR");
  }

  public boolean isInUserRole(Users user) {
    return userController.isUserInRole(user, "HOPS_USER");
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
