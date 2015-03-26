/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
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
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.ua.model.Address;
import se.kth.bbc.security.ua.model.User;
import se.kth.bbc.security.ua.model.Userlogins;
import se.kth.bbc.security.ua.model.Yubikey;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@SessionScoped
public class PeopleAministration implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private UserManager userManager;

    @EJB
    private EmailBean emailBean;

    @Resource
    private UserTransaction userTransaction;

    private User user;

    // for yubikey administration page
    private User selectedYubikyUser;

    private Address address;

    private String secAnswer;

    List<String> questions;
    private List<User> filteredUsers;
    private List<User> selectedUsers;
    private List<User> allUsers;

    // for modifying user roles and status
    private User editingUser;

    // for mobile users activation
    private List<User> requests;

    // for user activation
    private List<User> yRequests;

    // to remove an existing group
    private String sgroup;

    public String getSgroup() {
        return sgroup;
    }

    public void setSgroup(String sgroup) {
        this.sgroup = sgroup;
    }

    // to assign a new stauts
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

    // Yubikey public id. 12 chars: vviehlefjvcb
    private String pubid;

    // Yubikey serial no 
    private String serial;

    // e.g. f1bda8c978766d50c25d48d72ed516e0
    private String secret;

    // current status of the editing user
    private String eStatus;

    public String geteStatus() {
        this.eStatus = PeoplAccountStatus.values()[this.editingUser.getStatus() - 1].name();
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

    public User getEditingUser() {
        return editingUser;
    }

    public void setEditingUser(User editingUser) {
        this.editingUser = editingUser;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getPubid() {
        return pubid;
    }

    public void setPubid(String pubid) {
        this.pubid = pubid;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public List<User> getyRequests() {
        return yRequests;
    }

    public void setyRequests(List<User> yRequests) {
        this.yRequests = yRequests;
    }

    public List<String> getUserRole(User p) {
        List<String> list = userManager.findGroups(p.getUid());
        return list;
    }

    public String getChanged_Status(User p) {
        return PeoplAccountStatus.values()[userManager.findByEmail(p.getEmail()).getStatus() - 1].name();
    }

    /*
     public String getUserStatus(People p) {
     return Integer.toString(userManager.findByEmail(p.getEmail()).getStatus());
     }
     */
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
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

        for (BBCGroups b : BBCGroups.values()) {

            if (!list.contains(b.name())) {
                tmp.add(b.name());
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
        status = new ArrayList<>();
        questions = new ArrayList<>();
        for (BBCGroups value : BBCGroups.values()) {
            groups.add(value.name());
        }

        for (SecurityQuestions value : SecurityQuestions.values()) {
            questions.add(value.getValue());
        }

    }

    public List<String> getStatus() {

        status = new ArrayList<>();

        int st = editingUser.getStatus();

        for (PeoplAccountStatus p : PeoplAccountStatus.values()) {
            status.add(p.name());
        }

        // remove the inactive users
        status.remove(PeoplAccountStatus.MOBILE_ACCOUNT_INACTIVE.name());
        status.remove(PeoplAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.name());
        return status;
    }

    public void setStatus(List<String> status) {
        this.status = status;
    }

    public void setFilteredUsers(List<User> filteredUsers) {
        this.filteredUsers = filteredUsers;
    }

    public List<User> getFilteredUsers() {
        return filteredUsers;
    }

    /* Find all registered users*/
    public List<User> getAllUsers() {
        if (allUsers == null) {
            allUsers = userManager.findAllUsers();
        }
        return allUsers;
    }

    public List<User> getUsersNameList() {
        return userManager.findAllUsers();
    }

    public List<String> getGroups() {
        return groups;
    }

    public User getSelectedUser() {
        return user;
    }

    public void setSelectedUser(User user) {
        this.user = user;
    }

    /**
     * Reject users that are not validated.
     * @param user1 
     */
    public void rejectUser(User user1) {

        if (user1 == null) {
            MessagesController.addErrorMessage("Error", "Null user!");
        }
        try {
            boolean removeByEmail = userManager.removeByEmail(user1.getEmail());

            // update the user request table
            if (removeByEmail) {
                allUsers.remove(user1);
                if (user1.getYubikeyUser() == 1) {
                    yRequests.remove(user1);
                } else {
                    requests.remove(user1);
                }
            } else {
                MessagesController.addSecurityErrorMessage("Could not delete the user!");
            }
            emailBean.sendEmail(user1.getEmail(), UserAccountsEmailMessages.ACCOUNT_REJECT, UserAccountsEmailMessages.accountRejectedMessage());
            MessagesController.addInfoMessage(user1.getEmail() + " was rejected.");

        } catch (EJBException ejb) {
            MessagesController.addSecurityErrorMessage("Rejection failed");
        } catch (MessagingException ex) {
            Logger.getLogger(PeopleAministration.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void confirmMessage(ActionEvent actionEvent) {

        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Deletion Successful!", null);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public String getLoginName() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        Principal principal = request.getUserPrincipal();

        try {
            User p = userManager.findByEmail(principal.getName());
            if (p != null) {
                return p.getFname() + " " + p.getLname();
            } else {
                return principal.getName();
            }
        } catch (Exception ex) {
            ExternalContext extContext = FacesContext.getCurrentInstance().getExternalContext();
            System.err.println(extContext.getRequestContextPath());
            extContext.redirect(extContext.getRequestContextPath());
            return null;
        }
    }

    /**
     * Get all open user requests.
     *
     * @return
     */
    public List<User> getAllRequests() {
        if (requests == null) {
            requests = userManager.findAllByStatus(PeoplAccountStatus.MOBILE_ACCOUNT_INACTIVE.getValue());
        }
        return requests;
    }

    /**
     * Get all Yubikey requests
     *
     * @return
     */
    public List<User> getAllYubikeyRequests() {
        if (yRequests == null) {
            yRequests = userManager.findAllByStatus(PeoplAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue());
        }
        return yRequests;
    }

    public List<User> getSelectedUsers() {
        return selectedUsers;
    }

    public void setSelectedUsers(List<User> users) {
        this.selectedUsers = users;
    }

    public void activateUser(User user1) {
        if (sgroup == null || sgroup.isEmpty()) {
            MessagesController.addSecurityErrorMessage("Select a role.");
            return;
        }
        try {

            userTransaction.begin();

            if (!"#".equals(sgroup) && (!sgroup.equals(BBCGroups.BBC_GUEST.name()))) {
                userManager.registerGroup(user1, BBCGroups.valueOf(sgroup).getValue());
            }
 
            userManager.updateStatus(user1, PeoplAccountStatus.ACCOUNT_ACTIVE.getValue());
            userTransaction.commit();

            emailBean.sendEmail(user1.getEmail(), UserAccountsEmailMessages.ACCOUNT_CONFIRMATION_SUBJECT, UserAccountsEmailMessages.accountActivatedMessage(user1.getEmail()));

        } catch (NotSupportedException | SystemException | MessagingException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException e) {
            return;
        }
        requests.remove(user1);
        allUsers.add(user1);
    }

    public void blockUser(User user1) {
        try {
            userTransaction.begin();
            userManager.updateStatus(user1, PeoplAccountStatus.ACCOUNT_BLOCKED.getValue());
            userTransaction.commit();

            emailBean.sendEmail(user1.getEmail(), UserAccountsEmailMessages.ACCOUNT_BLOCKED__SUBJECT, UserAccountsEmailMessages.accountBlockedMessage());
        } catch (NotSupportedException | SystemException | MessagingException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            Logger.getLogger(PeopleAministration.class.getName()).log(Level.SEVERE, null, ex);
        }
        requests.remove(user1);
    }

    public String modifyUser(User user1) {
        // Get the latest status
        User newStatus = userManager.getUser(user1.getEmail());
        FacesContext.getCurrentInstance().getExternalContext()
                .getSessionMap().put("editinguser", newStatus);
      
        Userlogins login = userManager.getLastUserLoing(user1.getUid());
        
        FacesContext.getCurrentInstance().getExternalContext()
                .getSessionMap().put("editinguser_logins", login);
      
        return "admin_profile";
    }

    public String getSecAnswer() {
        return secAnswer;
    }

    public User getSelectedYubikyUser() {
        return selectedYubikyUser;
    }

    public void setSelectedYubikyUser(User selectedYubikyUser) {
        this.selectedYubikyUser = selectedYubikyUser;
    }

    public void setSecAnswer(String secAnswer) {
        this.secAnswer = secAnswer;
    }

    public List<String> getQuestions() {
        return questions;
    }

    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }

    public String activateYubikeyUser(User user1) {
        this.selectedYubikyUser = user1;
        this.address = this.selectedYubikyUser.getAddress();
        return "activate_yubikey";
    }

    public String activateYubikey() {
        try {
            // parse the creds  1486433,vviehlefjvcb,01ec8ce3dea6,f1bda8c978766d50c25d48d72ed516e0,,2014-12-14T23:16:09,

            if (this.selectedYubikyUser.getYubikeyUser() != 1) {
                MessagesController.addSecurityErrorMessage(user.getEmail() + " is not a Yubikey user");
                return "";
            }

            Yubikey yubi = this.selectedYubikyUser.getYubikey();

            yubi.setStatus(PeoplAccountStatus.ACCOUNT_ACTIVE.getValue());

            // Trim the input
            yubi.setSerial(serial.replaceAll("\\s", ""));
            yubi.setPublicId(pubid.replaceAll("\\s", ""));
            yubi.setAesSecret(secret.replaceAll("\\s", ""));

            // Update the info
            yubi.setCreated(new Date());
            yubi.setAccessed(new Date());
            yubi.setCounter(0);
            yubi.setSessionUse(0);
            yubi.setHigh(0);
            yubi.setLow(0);

            userTransaction.begin();

            userManager.updateYubikey(yubi);

            if (!"#".equals(sgroup) && (!sgroup.equals(BBCGroups.BBC_GUEST.name()))) {
                userManager.registerGroup(this.selectedYubikyUser, BBCGroups.valueOf(sgroup).getValue());
            }

            userManager.updateStatus(this.selectedYubikyUser, PeoplAccountStatus.ACCOUNT_ACTIVE.getValue());
            userTransaction.commit();

            // Update the user management GUI
            yRequests.remove(this.selectedYubikyUser);
            allUsers.add(this.selectedYubikyUser);

        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            MessagesController.addSecurityErrorMessage("Technical Error!");
            return ("");
        }

        return "print_address";
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public List<String> parseCredentials(String creds) {

        List<String> list = new ArrayList<>();

        StringTokenizer st = new StringTokenizer(creds, ",");
        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }

        return list;

    }

    public Date getTimeStamp(String time) throws ParseException {
        SimpleDateFormat sdf
                = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date tmp = new Date();
        tmp = sdf.parse(time);
        return tmp;
    }

    /**
     * Update user roles from profile by admin.
     */
    public void updateUserByAdmin() {
        try {
            // update status
            if (!"#".equals(selectedStatus)) {
                editingUser.setStatus(PeoplAccountStatus.valueOf(selectedStatus).getValue());
                userManager.updateStatus(editingUser, PeoplAccountStatus.valueOf(selectedStatus).getValue());
                MessagesController.addInfoMessage("Success", "Status updated successfully.");

            }

            // register a new group
            if (!"#".equals(nGroup)) {
                userManager.registerGroup(editingUser, BBCGroups.valueOf(nGroup).getValue());
                MessagesController.addInfoMessage("Success", "Role updated successfully.");

            }

            // remove a group
            if (!"#".equals(sgroup)) {
                if (sgroup.equals(BBCGroups.BBC_GUEST.name())) {
                    MessagesController.addSecurityErrorMessage(BBCGroups.BBC_GUEST.name() + " can not be removed.");
                } else {
                    userManager.removeGroup(editingUser, BBCGroups.valueOf(sgroup).getValue());
                    MessagesController.addInfoMessage("Success", "User updated successfully.");
                }
            }

            if ("#".equals(sgroup)) {

                if (("#".equals(selectedStatus))
                        || "#".equals(nGroup)) {
                    MessagesController.addSecurityErrorMessage("No selection made!");
                }
            }

        } catch (EJBException ejb) {
            MessagesController.addSecurityErrorMessage("Update failed.");
        }
    }
}
