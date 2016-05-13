package se.kth.bbc.security.ua.authz;

import javax.ejb.EJB;
import se.kth.bbc.security.ua.UserManager;
import se.kth.hopsworks.user.model.BbcGroup;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.BbcGroupFacade;

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
