/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import javax.ejb.EJBException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.ua.model.User;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@ViewScoped
public class AdminProfileAministration implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private UserManager userManager;

    @EJB
    private EmailBean emailBean;

    @Resource
    private UserTransaction userTransaction;

    private User user;
    // for modifying user roles and status
    private User editingUser;

    // for mobile users activation
    private List<User> requests;

    // to remove an existing group
    private String selected_group;

    // to assign a new stauts
    private String selected_status;

    // to assign a new group
    private String new_group;

    // all groups
    List<String> groups;

    // all existing groups belong tp
    List<String> current_groups;

    // all possible new groups user doesnt belong to
    List<String> new_groups;

    // current status of the editing user
    private String edit_status;

    List<String> status;

    
    public void setEdit_status(String edit_status) {
        this.edit_status = edit_status;
    }

    public String getNew_group() {
        return new_group;
    }

    public void setNew_group(String new_group) {
        this.new_group = new_group;
    }

    public User getEditingUser() {
        return editingUser;
    }

    public void setEditingUser(User editingUser) {
        this.editingUser = editingUser;
    }

    public List<String> getUserRole(User p) {
        List<String> list = userManager.findGroups(p.getUid());
        return list;
    }

    public String getChanged_Status(User p) {
        return PeoplAccountStatus.values()[userManager.findByEmail(p.getEmail()).getStatus() - 1].name();
    }

    
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setNew_groups(List<String> new_groups) {
        this.new_groups = new_groups;
    }

    public String getSelected_status() {
        return selected_status;
    }

    public void setSelected_status(String selected_status) {
        this.selected_status = selected_status;
    }

    public String getSelected_group() {
        return selected_group;
    }

    public void setSelected_group(String selected_group) {
        this.selected_group = selected_group;
    }

    /**
     * Filter the current groups of the user.
     *
     * @return
     */
    public List<String> getCurrent_groups() {
        List<String> list = userManager.findGroups(editingUser.getUid());
        return list;
    }

    public void setCurrent_groups(List<String> current_groups) {
        this.current_groups = current_groups;
    }

    public List<String> getNew_groups() {
        List<String> list = userManager.findGroups(editingUser.getUid());
        List<String> tmp = new ArrayList<>();

        for (BBCGroups b : BBCGroups.values()) {

            if (!list.contains(b.name())) {
                tmp.add(b.name());
            }
        }
        return tmp;
    }

    public String getEdit_status() {

        int status = userManager.getUser(this.editingUser.getEmail()).getStatus();
        this.edit_status = PeoplAccountStatus.values()[status - 1].name();
        return this.edit_status;
    }

    
    @PostConstruct
    public void init() {

        groups = new ArrayList<>();
        status = new ArrayList<>();

        for (BBCGroups value : BBCGroups.values()) {
            groups.add(value.name());
        }

        editingUser = (User) FacesContext.getCurrentInstance().getExternalContext()
                .getSessionMap().get("editinguser");
    }

    public List<String> getStatus() {

        status = new ArrayList<>();

        for (PeoplAccountStatus p : PeoplAccountStatus.values()) {
            status.add(p.name());
        }

        // Remove the inactive users
        status.remove(PeoplAccountStatus.MOBILE_ACCOUNT_INACTIVE.name());
        status.remove(PeoplAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.name());

        return status;
    }

    public void setStatus(List<String> status) {
        this.status = status;
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

    public String getLoginName() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        Principal principal = request.getUserPrincipal();

        try {
            User p = userManager.findByEmail(principal.getName());
            if (p != null) {
                return p.getName();
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
     * Reject user requests.
     *
     * @param user1
     */
    public void blockUser(User user1) {
        try {
            userTransaction.begin();

            userManager.updateStatus(user1.getUid(), PeoplAccountStatus.ACCOUNT_BLOCKED.getValue());
            userTransaction.commit();

            emailBean.sendEmail(user1.getEmail(), UserAccountsEmailMessages.ACCOUNT_BLOCKED__SUBJECT, UserAccountsEmailMessages.accountBlockedMessage());

        } catch (NotSupportedException | SystemException | MessagingException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            Logger.getLogger(AdminProfileAministration.class.getName()).log(Level.SEVERE, null, ex);
        }
        requests.remove(user1);
    }

    /**
     * Update user roles from profile by admin.
     */
    public void updateUserByAdmin() {
        try {
            // Update status
            if (!"#".equals(selected_status)) {
                editingUser.setStatus(PeoplAccountStatus.valueOf(selected_status).getValue());
                userManager.updateStatus(editingUser.getUid(), PeoplAccountStatus.valueOf(selected_status).getValue());
                MessagesController.addInfoMessage("Success", "Status updated successfully.");

            }

            // Register a new group
            if (!"#".equals(new_group)) {
                userManager.registerGroup(editingUser.getUid(), BBCGroups.valueOf(new_group).getValue());
                MessagesController.addInfoMessage("Success", "Role updated successfully.");

            }

            // Remove a group
            if (!"#".equals(selected_group)) {
                if (selected_group.equals(BBCGroups.BBC_GUEST.name())) {
                    MessagesController.addMessageToGrowl(BBCGroups.BBC_GUEST.name() + " can not be removed.");
                } else {
                    userManager.removeGroup(editingUser.getUid(), BBCGroups.valueOf(selected_group).getValue());
                    MessagesController.addMessageToGrowl("User updated successfully.");
                }
            }

            if ("#".equals(selected_group)) {

                if (("#".equals(selected_status))
                        || "#".equals(new_group)) {
                    MessagesController.addErrorMessage("Error", "No selection made!");
                }
            }

        } catch (EJBException ejb) {
            MessagesController.addErrorMessage("Error: Update failed.");
        }
    }
}
