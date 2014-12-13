/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.ua.model.People;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@SessionScoped
public class PeopleAministration implements Serializable {

    private static final Logger logger = Logger.getLogger(PeopleAministration.class.getName());

    private static final long serialVersionUID = 1L;
    @EJB
    private UserManager userManager;
    private People user;

    private String sec_answer;        

    public String getSec_answer() {
        return sec_answer;
    }

    public void setSec_answer(String sec_answer) {
        this.sec_answer = sec_answer;
    }

    public Map<String, String> getQuestions() {
        return questions;
    }

    public void setQuestions(Map<String, String> questions) {
        this.questions = questions;
    }
    Map<String, Integer> groups;
    Map<String, String> questions;
    private List<People> filteredUsers;
    private List<People> selectedUsers;
    private List<People> allUsers;
    private int tabIndex;

    private List<People> requests;
    private String selected_group;
    private String selected_status;
    
    public String getUserRole(People user) {
        return userManager.getPeopleGroupName(user.getUid());
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
  
    @PostConstruct
    public void initGroups() {
        groups = new BBCGroups().getGroups();
        questions = new SelectSecurityQuestionMenue().getQuestions();
    }

    public void setFilteredUsers(List<People> filteredUsers) {
        this.filteredUsers = filteredUsers;
    }

    public List<People> getFilteredUsers() {
        return filteredUsers;
    }

    public List<People> getAllUsers() {
        if(allUsers == null) {
            allUsers= userManager.findAllUsers();
        }
        return allUsers;
    }

    public List<People> getUsersNameList() {
        return userManager.findAllUsers();
    }

    public List<People> getAllUsersName() {
        return userManager.findAllByName();
    }

    public Map<String, Integer> getGroups() {
        return groups;
    }

    public People getSelectedUser() {
        return user;
    }

    public void setSelectedUser(People user) {
        this.user = user;
    }
  
    public void deleteUser(People user) {
        try {
            userManager.removeByEmail(user.getEmail()); 
        } catch (EJBException ejb) {
            MessagesController.addErrorMessage("Error", "Delete operation failed");
        }
        MessagesController.addInfoMessage(user.getFname() + " successfully removed.");
    }
    
    public void rejectUser(People user) {
        try {
            userManager.removeByEmail(user.getEmail()); 
        } catch (EJBException ejb) {
            MessagesController.addErrorMessage("Error", "Rejection failed");
        }
        MessagesController.addInfoMessage(user.getFname() + " was rejected.");
        requests.remove(user);
    }

    public void updateUser() {
        try {
            userManager.update(user);
        } catch (EJBException ejb) {
            MessagesController.addErrorMessage("Error", "Update action failed.");
            return;
        }
        MessagesController.addInfoMessage("Update Completed.");

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
            return principal.getName();
        } catch (Exception ex) {
//            throw new RuntimeException("Not logged in");
            ExternalContext extContext = FacesContext.getCurrentInstance().getExternalContext();
            System.err.println(extContext.getRequestContextPath());
            extContext.redirect(extContext.getRequestContextPath());
            return null;
        }
    }

    /**
     * Get all open user requests.
     * @return 
     */
    public List<People> getAllRequests() {
        if (requests == null) {
            requests = userManager.findAllByStatus(AccountStatusIF.ACCOUNT_INACTIVE);
        }
        return requests;
    }

    public List<People> getSelectedUsers() {
        return selectedUsers;
    }

    public void setSelectedUsers(List<People> users) {
        this.selectedUsers = users;
    }

    
    /**
     * Activate users
     * @param user 
     */
    public void activateUser(People user) {
        userManager.updateGroup(user.getUid(), Integer.parseInt(selected_group));
        userManager.updateStatus(user.getUid(), AccountStatusIF.ACCOUNT_ACTIVE);
        requests.remove(user);
    }
    
    /**
     * To reject user requests
     * @param user 
     */
    public void blockUser(People user) {
        userManager.updateStatus(user.getUid(), AccountStatusIF.ACCOUNT_BLOCKED);
        requests.remove(user);
    }
    public void modifyUser(People user) {
        userManager.updateGroup(user.getUid(), Integer.parseInt(selected_group));
        userManager.updateStatus(user.getUid(), Integer.parseInt(selected_status));
    }

    
    public void setTabIndex(int index) {
        this.tabIndex = index;
    }

    public int getTabIndex() {
        int oldindex = tabIndex;
        tabIndex = 0;
        return oldindex;
    }

    public String openRequests() {
        this.tabIndex = 1;
        return "userMgmt";
    }

}
