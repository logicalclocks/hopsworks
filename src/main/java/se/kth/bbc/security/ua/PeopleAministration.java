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
import java.util.logging.Level;
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

    // Yubikey public id. eg. f1bda8c978766d50c25d48d72ed516e0
    private String credentials;
    private String serial;

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
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
            
    private int tabIndex;

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
    
        
    public void rejectYubikeyUser(People user) {
        try {
            userManager.removeByEmail(user.getEmail()); 
        } catch (EJBException ejb) {
            MessagesController.addErrorMessage("Error", "Rejection failed");
        }
        MessagesController.addInfoMessage(user.getFname() + " was rejected.");
        yubikey_requests.remove(user);
    }
    
    

    public void updateUser() {
        try {
            userManager.updatePeople(user);
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
            requests = userManager.findAllByStatus(AccountStatusIF.MOBILE_ACCOUNT_INACTIVE);
        }
        return requests;
    }

    /**
     * Get all Yubikey requests
     * @return 
     */
    public List<People> getAllYubikeyRequests() {
        if (yubikey_requests == null) {
           yubikey_requests = userManager.findAllByStatus(AccountStatusIF.YUBIKEY_ACCOUNT_INACTIVE);
        }
        return  yubikey_requests;
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

    public String activateYubikeyUser(People user) {
        userManager.updateGroup(user.getUid(), Integer.parseInt(selected_group));
        userManager.updateStatus(user.getUid(), AccountStatusIF.ACCOUNT_ACTIVE);
        selectedYubikyUser = user;
        address = userManager.findAddress(user.getUid());
        yubikey_requests.remove(user);
        return "activate_yubikey";
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
    
    public String activateYubikey(){
        // parse the creds  1486433,vviehlefjvcb,01ec8ce3dea6,f1bda8c978766d50c25d48d72ed516e0,,2014-12-14T23:16:09,
        
        if (selectedYubikyUser.getYubikeyUser()!=1){
    
            MessagesController.addInfoMessage(user.getEmail() + " is not a Yubikey user");
            return "";
        }
            
        Yubikey yubi = userManager.findYubikey(selectedYubikyUser.getUid());
    
        try {
        
        List list = parseCredentials(credentials);
        yubi.setStatus(1);
       
        yubi.setSerial((String) list.get(0));
        yubi.setPublicId((String) list.get(1));
        yubi.setAesSecret((String) list.get(3));
            yubi.setCreated(getTimeStamp((String) list.get(4)));
            yubi.setAccessed(getTimeStamp((String) list.get(4)));
        yubi.setCounter(0);
        yubi.setSessionUse(0);
        yubi.setHigh(0);
        yubi.setLow(0);
        userManager.updateYubikey(yubi);
        } catch (ParseException ex) {
            Logger.getLogger(PeopleAministration.class.getName()).log(Level.SEVERE, null, ex);
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
    
        List <String>list = new ArrayList<>();
   
        StringTokenizer st = new StringTokenizer(creds, ",");
         while (st.hasMoreTokens()) {
             list.add(st.nextToken());
         } 
     
     return list;
    
    }
    
    public Date getTimeStamp (String time) throws ParseException{
    SimpleDateFormat sdf =
      new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss");
      Date tmp = new Date();
      tmp = sdf.parse(time);
      return tmp;
  }
    
}
