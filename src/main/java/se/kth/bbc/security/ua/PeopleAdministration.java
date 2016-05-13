package se.kth.bbc.security.ua;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.audit.AuditManager;
import se.kth.bbc.security.audit.AuditUtil;
import se.kth.bbc.security.audit.RolesAuditActions;
import se.kth.bbc.security.audit.UserAuditActions;
import se.kth.bbc.security.audit.model.Userlogins;
import se.kth.hopsworks.user.model.BbcGroup;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.BbcGroupFacade;

@ManagedBean
@ViewScoped
public class PeopleAdministration implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private UserManager userManager;

    @EJB
    private AuditManager auditManager;

    @EJB
    private BbcGroupFacade bbcGroupFacade;

    @EJB
    private EmailBean emailBean;

    @ManagedProperty("#{clientSessionState}")
    private ClientSessionState sessionState;

    @Resource
    private UserTransaction userTransaction;

    private Users user;

    private String secAnswer;

    private List<Users> filteredUsers;
    private List<Users> selectedUsers;

    // All verified users
    private List<Users> allUsers;

    // Accounts waiting to be validated by the email owner
    private List<Users> spamUsers;

    // for modifying user roles and status
    private Users editingUser;

    // for mobile users activation
    private List<Users> requests;

    // for user activation
    private List<Users> yRequests;

    // to remove an existing group
    private String sgroup;

    public String getSgroup() {
        return sgroup;
    }

    public void setSgroup(String sgroup) {
        this.sgroup = sgroup;
    }

    // to assign a new status
    private String selectedStatus;

    // to assign a new group
    private String nGroup;

    List<String> status;

    // all groups
    List<String> groups;

    // all existing groups belong tp
    List<String> cGroups;

    // all possible new groups user doesnt belong to
    List<String> nGroups;

    // current status of the editing user
    private String eStatus;

    // list of roles that can be activated for a user
    List<String> actGroups;
    
    public String geteStatus() {
        if (this.editingUser == null) {
            return "";
        }
        
        this.eStatus = PeopleAccountStatus.values()[this.editingUser.getStatus() - 1].name();
        return this.eStatus;
    }

    public void seteStatus(String eStatus) {
        this.eStatus = eStatus;
    }

    public String getnGroup() {
        return nGroup;
    }

    public void setnGroup(String nGroup) {
        this.nGroup = nGroup;
    }

    public Users getEditingUser() {
        return editingUser;
    }

    public void setEditingUser(Users editingUser) {
        this.editingUser = editingUser;
    }

    public List<Users> getyRequests() {
        return yRequests;
    }

    public void setyRequests(List<Users> yRequests) {
        this.yRequests = yRequests;
    }

    public List<String> getUserRole(Users p) {
        List<String> list = userManager.findGroups(p.getUid());
        return list;
    }

    public String getChanged_Status(Users p) {
        return PeopleAccountStatus.values()[userManager.findByEmail(p.getEmail()).getStatus() - 1].name();
    }

    public List<String> getActGroups() {
        return actGroups;
    }

    public void setActGroups(List<String> actGroups) {

        this.actGroups = actGroups;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    /**
     * Filter the current groups
     *
     * @return
     */
    public List<String> getcGroups() {
        List<String> list = userManager.findGroups(editingUser.getUid());
        return list;
    }

    public void setcGroups(List<String> cGroups) {
        this.cGroups = cGroups;
    }

    public List<String> getnGroups() {
        List<String> list = userManager.findGroups(editingUser.getUid());
        List<String> tmp = new ArrayList<>();

        for (BbcGroup b : bbcGroupFacade.findAll()) {
            if (!list.contains(b.getGroupName())) {
                tmp.add(b.getGroupName());
            }
        }
        return tmp;
    }

    public void setnGroups(List<String> nGroups) {
        this.nGroups = nGroups;
    }

    public String getSelectedStatus() {
        return selectedStatus;
    }

    public void setSelectedStatus(String selectedStatus) {
        this.selectedStatus = selectedStatus;
    }

    @PostConstruct
    public void initGroups() {
        groups = new ArrayList<>();
        status = getStatus();                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   
        actGroups = new ArrayList<>();
        // dont include  BBC ADMIN for user management

        for (BbcGroup b : bbcGroupFacade.findAll()) {
            groups.add(b.getGroupName());
        }

        for (BbcGroup b : bbcGroupFacade.findAll()) {
            actGroups.add(b.getGroupName());
        }

    }

    public List<String> getStatus() {

        this.status = new ArrayList<>();

        for (PeopleAccountStatus p : PeopleAccountStatus.values()) {
            status.add(p.name());
        }

        return status;
    }

    public void setStatus(List<String> status) {
        this.status = status;
    }

    public void setFilteredUsers(List<Users> filteredUsers) {
        this.filteredUsers = filteredUsers;
    }

    public List<Users> getFilteredUsers() {
        return filteredUsers;
    }

    /*
   * Find all registered users
     */
    public List<Users> getAllUsers() {
        if (allUsers == null) {
            allUsers = userManager.findAllUsers();
        }
        return allUsers;
    }

    public List<Users> getUsersNameList() {
        return userManager.findAllUsers();
    }

    public List<String> getGroups() {
        return groups;
    }

    public Users getSelectedUser() {
        return user;
    }

    public void setSelectedUser(Users user) {
        this.user = user;
    }

    /**
     * Reject users that are not validated.
     *
     * @param user1
     */
    public void rejectUser(Users user1) {

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.
            getExternalContext().getRequest();

        if (user1 == null) {
            MessagesController.addErrorMessage("Error", "No user found!");
            return;
        }
        try {

            boolean removeByEmail = userManager.deleteUserRequest(user1);

            // update the user request table
            if (removeByEmail) {
                emailBean.sendEmail(user1.getEmail(),
                    UserAccountsEmailMessages.ACCOUNT_REJECT,
                    UserAccountsEmailMessages.accountRejectedMessage());
                MessagesController.addInfoMessage(user1.getEmail() + " was rejected.");
                spamUsers.remove(user1);

            }
        } catch (MessagingException ex) {
            MessagesController.addSecurityErrorMessage("Rejection failed");
            Logger.getLogger(PeopleAdministration.class.getName()).log(Level.SEVERE,
                "Could not reject user.", ex);
            return;
        }

    }

    public void confirmMessage(ActionEvent actionEvent) {

        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
            "Deletion Successful!", null);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public String getLoginName() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.
            getExternalContext().getRequest();

        Principal principal = request.getUserPrincipal();

        try {
            Users p = userManager.findByEmail(principal.getName());
            if (p != null) {
                return p.getFname() + " " + p.getLname();
            } else {
                return principal.getName();
            }
        } catch (Exception ex) {
            ExternalContext extContext = FacesContext.getCurrentInstance().
                getExternalContext();
            System.err.println(extContext.getRequestContextPath());
            extContext.redirect(extContext.getRequestContextPath());
            return null;
        }
    }

    /**
     * Get all open user requests (mobile or simple accounts).
     *
     * @return
     */
    public List<Users> getAllRequests() {
        if (requests == null) {
            requests = userManager.findMobileRequests();
        }
        return requests;
    }

    /**
     * Get all Yubikey requests
     *
     * @return
     */
    public List<Users> getAllYubikeyRequests() {
        if (yRequests == null) {
            yRequests = userManager.findYubikeyRequests();
        }
        return yRequests;
    }

    public List<Users> getSelectedUsers() {
        return selectedUsers;
    }

    public void setSelectedUsers(List<Users> users) {
        this.selectedUsers = users;
    }

    public void activateUser(Users user1) {
        if (sgroup == null || sgroup.isEmpty()) {
            MessagesController.addSecurityErrorMessage("Select a role.");
            return;
        }
        try {

            userTransaction.begin();

            BbcGroup bbcGroup = bbcGroupFacade.findByGroupName(sgroup);

            if (!"#".equals(sgroup) && (!sgroup.isEmpty() || sgroup != null) && bbcGroup != null) {
                userManager.registerGroup(user1, bbcGroup.getGid());
                auditManager.registerRoleChange(sessionState.getLoggedInUser(), RolesAuditActions.ADDROLE.name(),
                    RolesAuditActions.SUCCESS.name(), bbcGroup.getGroupName(),
                    user1);

            } else {
                auditManager.registerAccountChange(sessionState.getLoggedInUser(),
                    PeopleAccountStatus.ACTIVATED_ACCOUNT.name(),
                    RolesAuditActions.FAILED.name(), "Role could not be granted.", user1);
                MessagesController.addSecurityErrorMessage("Role could not be granted.");
                return;
            }

            userManager.updateStatus(user1, PeopleAccountStatus.ACTIVATED_ACCOUNT.getValue());
            userTransaction.commit();

            auditManager.registerAccountChange(sessionState.getLoggedInUser(),
                PeopleAccountStatus.ACTIVATED_ACCOUNT.name(),
                UserAuditActions.SUCCESS.name(), "", user1);
            emailBean.sendEmail(user1.getEmail(),
                UserAccountsEmailMessages.ACCOUNT_CONFIRMATION_SUBJECT,
                UserAccountsEmailMessages.
                accountActivatedMessage(user1.getEmail()));

        } catch (NotSupportedException | SystemException | MessagingException |
            RollbackException | HeuristicMixedException |
            HeuristicRollbackException | SecurityException |
            IllegalStateException e) {
            auditManager.registerAccountChange(sessionState.getLoggedInUser(),
                PeopleAccountStatus.ACTIVATED_ACCOUNT.name(),
                UserAuditActions.FAILED.name(), "", user1);
            return;
        }
        requests.remove(user1);
    }

    public boolean notVerified(Users user) {
        if (user == null || user.getBbcGroupCollection() == null || user.getBbcGroupCollection().isEmpty() == false) {
            return false;
        }
        if (user.getStatus() == PeopleAccountStatus.VERIFIED_ACCOUNT.getValue()) {
            return false;
        }
        return true;
    }
    public void resendAccountVerificationEmail(Users user) throws MessagingException {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.
            getExternalContext().getRequest();

        String activationKey = SecurityUtils.getRandomPassword(64);
        emailBean.sendEmail(user.getEmail(),
            UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
            UserAccountsEmailMessages.buildMobileRequestMessage(
                AuditUtil.getUserURL(request), user.getUsername()
                + activationKey));
        user.setValidationKey(activationKey);
        userManager.persist(user);

    }

    public String modifyUser(Users user1) {
        // Get the latest status
        Users newStatus = userManager.getUserByEmail(user1.getEmail());
        FacesContext.getCurrentInstance().getExternalContext()
            .getSessionMap().put("editinguser", newStatus);

        Userlogins login = auditManager.getLastUserLogin(user1.getUid());
        FacesContext.getCurrentInstance().getExternalContext()
            .getSessionMap().put("editinguser_logins", login);

        return "admin_profile";
    }

    public List<Users> getSpamUsers() {
        return spamUsers = userManager.findSPAMAccounts();
    }

    public void setSpamUsers(List<Users> spamUsers) {
        this.spamUsers = spamUsers;
    }

    public String getSecAnswer() {
        return secAnswer;
    }

    public void setSecAnswer(String secAnswer) {
        this.secAnswer = secAnswer;
    }

    public SecurityQuestion[] getQuestions() {
        return SecurityQuestion.values();
    }

    public String activateYubikeyUser(Users u) {
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put(
            "yUser", u);
        return "activate_yubikey";
    }

    public ClientSessionState getSessionState() {
        return sessionState;
    }

    public void setSessionState(ClientSessionState sessionState) {
        this.sessionState = sessionState;
    }

}
