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
import java.util.Map;
import java.util.StringTokenizer;
import javax.annotation.PostConstruct;
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
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.ua.model.Address;
import se.kth.bbc.security.ua.model.User;
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

    private User user;

    // for yubikey administration page
    private User selectedYubikyUser;

    private Address address;

    private String sec_answer;

    
    Map<String, String> questions;
    private List<User> filteredUsers;
    private List<User> selectedUsers;
    private List<User> allUsers;

    // for modifying user roles and status
    private User editingUser;
    
    // for mobile users activation
    private List<User> requests;

    // for user activation
    private List<User> yubikey_requests;

    // to remove an existing group
    private String selected_group;

    // to assign a new stauts
    private String selected_status;

    // to assign a new group
    private String new_group;

    // all groups
    List<String> groups;

    // all existing groups belong tp
     List<String>current_groups;

    // all possible new groups user doesnt belong to
    List<String> new_groups;

    // Yubikey public id. 12 chars: vviehlefjvcb
    private String pubid;

    // Yubikey serial no 
    private String serial;

    // e.g. f1bda8c978766d50c25d48d72ed516e0
    private String secret;

    public String getEdit_status() {
        
        return PeoplAccountStatus.values()[editingUser.getStatus()-1].name();
    }

    public void setEdit_status(String edit_status) {
        this.edit_status = edit_status;
    }
    
    // current status of the editing user
    private String edit_status;
    
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

    public List<User> getYubikey_requests() {
        return yubikey_requests;
    }

    public void setYubikey_requests(List<User> yubikey_requests) {
        this.yubikey_requests = yubikey_requests;
    }

    public List<String> getUserRole(User p) {
        return userManager.findGroups(p.getUid());
    }

    
    public String getChanged_Status(User p) {
        return PeoplAccountStatus.values()[userManager.findByEmail(p.getEmail()).getStatus()-1].name();
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
    public List<String> getCurrent_groups() {

        List<String> list=  userManager.findGroups(editingUser.getUid());
        
        return list;
    
    }

    public void setCurrent_groups( List<String> current_groups) {
        this.current_groups = current_groups;
    }

    
    public List<String> getNew_groups() {
        List<String> list = userManager.findGroups(editingUser.getUid());
        List<String> tmp = new ArrayList<>();
        
        for (BBCGroups b: BBCGroups.values()) {
        
            if (!list.contains(b.name()))
                tmp.add(b.name());
        }
        
        return tmp;
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

    List<String> status;
    
    @PostConstruct
    public void initGroups() {
        
        groups = new ArrayList<>();
        status = new ArrayList<>();
    
        for (int i = 0; i< BBCGroups.values().length; i++) {
             groups.add(BBCGroups.values()[i].name());
        }
        
    
        questions = new SelectSecurityQuestionMenue().getQuestions();
    }

    public List<String> getStatus() {
    
        status = new ArrayList<>();
   
        int st = editingUser.getStatus();
        
        for (PeoplAccountStatus p: PeoplAccountStatus.values()) {
             status.add(p.name());
        }
        
        status.remove(PeoplAccountStatus.values()[st-1].name());
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

    public List<User> getAllUsers() {
        if (allUsers == null) {
            allUsers = userManager.findAllUsers();
        }
        return allUsers;
    }

    public List<User> getUsersNameList() {
        return userManager.findAllUsers();
    }

    public  List<String> getGroups() {
        return groups;
    }

    public User getSelectedUser() {
        return user;
    }

    public void setSelectedUser(User user) {
        this.user = user;
    }

    public void rejectUser(User user1) throws MessagingException {

        if (user1 == null) {
            MessagesController.addErrorMessage("Error", "Null user!");
        }
        try {
            boolean removeByEmail = userManager.removeByEmail(user1.getEmail());
       
            // update the user request table
            if(removeByEmail){
                allUsers.remove(user1);
                if(user1.getYubikeyUser()==1)
                    yubikey_requests.remove(user1);
                else
                    requests.remove(user1);
            }
                else 
                 MessagesController.addErrorMessage("Error", "Could not delete the user!");
            
        } catch (EJBException ejb) {
            MessagesController.addErrorMessage("Error", "Rejection failed");
        }

        emailBean.sendEmail(user1.getEmail(), "BBC Account", accountRejectedMessage());
        MessagesController.addInfoMessage(user1.getEmail() + " was rejected.");
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
        if (yubikey_requests == null) {
            yubikey_requests = userManager.findAllByStatus(PeoplAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue());
        }
        return yubikey_requests;
    }

    public List<User> getSelectedUsers() {
        return selectedUsers;
    }

    public void setSelectedUsers(List<User> users) {
        this.selectedUsers = users;
    }

    /**
     * Activate users
     *
     * @param user1
     */
    public void activateUser(User user1) throws MessagingException {
        userManager.updateGroup(user1.getUid(), BBCGroups.valueOf(selected_group).getValue());
        userManager.updateStatus(user1.getUid(), PeoplAccountStatus.ACCOUNT_ACTIVE.getValue());
        emailBean.sendEmail(user1.getEmail(), "BBC Account", accountActivatedMessage(user1.getEmail()));
        requests.remove(user1);
    }


    /**
     * To reject user requests
     *
     * @param user1
     * @throws javax.mail.MessagingException
     */
    public void blockUser(User user1) throws MessagingException {
        userManager.updateStatus(user1.getUid(), PeoplAccountStatus.ACCOUNT_BLOCKED.getValue());
        emailBean.sendEmail(user1.getEmail(), "Account Blocked", accountBlockedMessage());
        requests.remove(user1);
    }

    public String modifyUser(User user1) {
        this.editingUser = user1;
        this.address = userManager.findAddress(user1.getUid());
        return "admin_profile";
    }

    public String getSec_answer() {
        return sec_answer;
    }

    public User getSelectedYubikyUser() {
        return selectedYubikyUser;
    }

    public void setSelectedYubikyUser(User selectedYubikyUser) {
        this.selectedYubikyUser = selectedYubikyUser;
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

    
    public String activateYubikeyUser(User user1) {
        this.selectedYubikyUser = user1;
        this.address = userManager.findAddress(user1.getUid());
        return "activate_yubikey";
    }
    
    public String activateYubikey() {
        // parse the creds  1486433,vviehlefjvcb,01ec8ce3dea6,f1bda8c978766d50c25d48d72ed516e0,,2014-12-14T23:16:09,

        if (this.selectedYubikyUser.getYubikeyUser() != 1) {

            MessagesController.addInfoMessage(user.getEmail() + " is not a Yubikey user");
            return "";
        }

        Yubikey yubi = userManager.findYubikey(this.selectedYubikyUser.getUid());

        yubi.setStatus(1);
        yubi.setSerial(serial.replaceAll("\\s", ""));
        yubi.setPublicId(pubid.replaceAll("\\s", ""));
        yubi.setAesSecret(secret.replaceAll("\\s", ""));
        yubi.setCreated(new Date());
        yubi.setAccessed(new Date());
        yubi.setCounter(0);
        yubi.setSessionUse(0);
        yubi.setHigh(0);
        yubi.setLow(0);
        
        userManager.updateYubikey(yubi);
        userManager.updateGroup(this.selectedYubikyUser.getUid(), Integer.parseInt(selected_group));
        userManager.updateStatus(this.selectedYubikyUser.getUid(), PeoplAccountStatus.ACCOUNT_ACTIVE.getValue());
        
        yubikey_requests.remove(this.selectedYubikyUser);
       
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

    private String accountActivatedMessage(String username) {
        String l1 = "Greetings!\n\n"
                + "Your account request to access the BiobankCloud is approved.\n\n";
        String l2 = "You can login with the username: " + username + "\n\n\n";
        String l3 = "If you have any questions please contact support@biobankcloud.com";

        return l1 + l2 + l3;
    }

    private String accountBlockedMessage() {
        String l1 = "Greetings!\n\n"
                + "Your account in the Biobankcloud is blocked.\n\n";
        String l2 = "If you have any questions please contact support@biobankcloud.com";
        return l1 + l2;
    }

    private String accountRejectedMessage() {
        String l1 = "Greetings!\n\n"
                + "Your Biobankcloud account request is rejected.\n\n";
        String l2 = "If you have any questions please contact support@biobankcloud.com";
        return l1 + l2;
    }

    /**
     * Update user roles from profile by admin
     */
    public void updateUserByAdmin() {
        try {
            // update status
            if ( !"#".equals(selected_status)) {
                editingUser.setStatus(PeoplAccountStatus.valueOf(selected_status).getValue());
                userManager.updateStatus(editingUser.getUid(), PeoplAccountStatus.valueOf(selected_status).getValue());
                
            }

            // register a new group
            if (!"#".equals(new_group)) {
                userManager.registerGroup(editingUser.getUid(), BBCGroups.valueOf(new_group).getValue());
            }

            // remove a group
            if (!"#".equals(selected_group)) {
                userManager.removeGroup(editingUser.getUid(), BBCGroups.valueOf(selected_group).getValue());
            }
            
            if("#".equals(selected_group)) {
            
                if (("#".equals(selected_status) ) ||
                        "#".equals(new_group)) {
                    MessagesController.addErrorMessage("Error", "No selection made!");
                }
             }   
            
        } catch (EJBException ejb) {
            MessagesController.addErrorMessage("Error: Update failed.");
            return;
        }
        MessagesController.addInfoMessage("Success", "User updated successfully.");
    }

}
