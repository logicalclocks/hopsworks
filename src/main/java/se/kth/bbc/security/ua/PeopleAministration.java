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
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.ua.model.Address;
import se.kth.bbc.security.ua.model.People;
import se.kth.bbc.security.ua.model.Yubikey;
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

    @EJB
    private EmailBean emailBean;

    private People user;

    private String sec_answer;

    Map<String, Integer> groups;
    Map<String, String> questions;
    private List<People> filteredUsers;
    private List<People> selectedUsers;
    private List<People> allUsers;

    private Address address;

    // Yubikey public id. 12 chars: vviehlefjvcb
    private String pubid;

    
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    // Yubikey serial no 
    private String serial;

    //f1bda8c978766d50c25d48d72ed516e0
    private String secret;

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

    // for mobile users activation
    private List<People> requests;

    // for user activation
    private List<People> yubikey_requests;

    // for yubikey administration page
    private People selectedYubikyUser;

    public List<People> getYubikey_requests() {
        return yubikey_requests;
    }

    public void setYubikey_requests(List<People> yubikey_requests) {
        this.yubikey_requests = yubikey_requests;
    }

    private String selected_group;
    private String selected_status;
    
    public List <String> getUserRole(People p) {
        return userManager.findGroups(p.getUid());
    }

    public String getUserStatus(People p) {
        return Integer.toString(userManager.findByEmail(p.getEmail()).getStatus());
    }
    
    public People getUser() {
        return user;
    }

    public void setUser(People user) {
        this.user = user;
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
        if (allUsers == null) {
            allUsers = userManager.findAllUsers();
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

    public void rejectUser(People user1) throws MessagingException {

        if (user1 == null) {
            MessagesController.addErrorMessage("Error", "Null user!");
        }
        try {
            userManager.removeByEmail(user1.getEmail());
            allUsers.remove(user1);
        } catch (EJBException ejb) {
            MessagesController.addErrorMessage("Error", "Rejection failed");
        }

        emailBean.sendEmail(user1.getEmail(), "Account rejected", accountRejectedMessage());
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
            People p = userManager.findByEmail(principal.getName());
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
    public List<People> getAllRequests() {
        if (requests == null) {
            requests = userManager.findAllByStatus(AccountStatusIF.MOBILE_ACCOUNT_INACTIVE);
        }
        return requests;
    }

    /**
     * Get all Yubikey requests
     *
     * @return
     */
    public List<People> getAllYubikeyRequests() {
        if (yubikey_requests == null) {
            yubikey_requests = userManager.findAllByStatus(AccountStatusIF.YUBIKEY_ACCOUNT_INACTIVE);
        }
        return yubikey_requests;
    }

    public List<People> getSelectedUsers() {
        return selectedUsers;
    }

    public void setSelectedUsers(List<People> users) {
        this.selectedUsers = users;
    }

    /**
     * Activate users
     *
     * @param user1
     */
    public void activateUser(People user1) throws MessagingException {
        userManager.updateGroup(user1.getUid(), Integer.parseInt(selected_group));
        userManager.updateStatus(user1.getUid(), AccountStatusIF.ACCOUNT_ACTIVE);
        emailBean.sendEmail(user1.getEmail(), "BBC account", accountActivatedMessage(user1.getEmail()));
        requests.remove(user1);
    }

    public String activateYubikeyUser(People user1) {
        userManager.updateGroup(user1.getUid(), Integer.parseInt(selected_group));
        userManager.updateStatus(user1.getUid(), AccountStatusIF.ACCOUNT_ACTIVE);
        selectedYubikyUser = user1;
        address = userManager.findAddress(user1.getUid());

        yubikey_requests.remove(user1);
        return "activate_yubikey";
    }

    /**
     * To reject user requests
     *
     * @param user1
     */
    public void blockUser(People user1) throws MessagingException {
        userManager.updateStatus(user1.getUid(), AccountStatusIF.ACCOUNT_BLOCKED);
        emailBean.sendEmail(user1.getEmail(), "Account Blocked", accountBlockedMessage());
        requests.remove(user1);
    }

    public void modifyUser(People user1) {
        userManager.updateStatus(user1.getUid(), Integer.parseInt(selected_status));
        userManager.updateGroup(user1.getUid(), Integer.parseInt(selected_group));
        MessagesController.addInfoMessage("Success", "User modified");

    }

    public String getSec_answer() {
        return sec_answer;
    }

    public People getSelectedYubikyUser() {
        return selectedYubikyUser;
    }

    public void setSelectedYubikyUser(People selectedYubikyUser) {
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


    public String activateYubikey() {
        // parse the creds  1486433,vviehlefjvcb,01ec8ce3dea6,f1bda8c978766d50c25d48d72ed516e0,,2014-12-14T23:16:09,

        if (selectedYubikyUser.getYubikeyUser() != 1) {

            MessagesController.addInfoMessage(user.getEmail() + " is not a Yubikey user");
            return "";
        }

        Yubikey yubi = userManager.findYubikey(selectedYubikyUser.getUid());

        yubi.setStatus(1);
        yubi.setSerial(serial);
        yubi.setPublicId(pubid);
        yubi.setAesSecret(secret);
        yubi.setCreated(new Date());
        yubi.setAccessed(new Date());
        yubi.setCounter(0);
        yubi.setSessionUse(0);
        yubi.setHigh(0);
        yubi.setLow(0);
        userManager.updateYubikey(yubi);
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
                  + "Your request for access the BiobankCloud is approved.\n\n";
        String l2 = "You can login with the username: " + username + "\n\n\n";
        String l3 = "If you have any questions please contact support@biobankcloud.com";

        return l1 + l2 + l3;
    }

    private String accountBlockedMessage() {
        String l1 = "Greetings!\n\n"
                +   "Your account in the Biobankcloud is blocked.\n\n";
        String l2 = "If you have any questions please contact support@biobankcloud.com";
        return l1 + l2;
    }

    private String accountRejectedMessage() {
        String l1 = "Greetings!\n\n"
                +   "Your Biobankcloud account request is rejected.\n\n";
        String l2 = "If you have any questions please contact support@biobankcloud.com";
        return l1 + l2;
    }
    

}
