package se.kth.kthfsdashboard.user;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.servlet.http.HttpServletRequest;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import se.kth.bbc.activity.ActivityMB;
import se.kth.bbc.study.StudyMB;


/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@SessionScoped
public class UserController implements Serializable {
    private static final Logger logger = Logger.getLogger(UserController.class.getName());
    
    private static final long serialVersionUID = 1L;
    @EJB
    private UserFacade userFacade;
    private Username user;
    
    List<Group> g = new ArrayList<>();
    private List<Username> filteredUsers;
    private List<Username> usernames;
    private List<Username> selectedUsername;
    
    private String email;
    private String name;
    private String username;
    
    public UserController() {
        
    }
    
    public Username getUser() {
        if (user == null) {
            user = new Username();
        }
        return user;
    }

    public void setUser(Username user) {
        this.user = user;
    }

    public void setFilteredUsers(List<Username> filteredUsers){
        this.filteredUsers = filteredUsers;
    }
    
     public List<Username> getFilteredUsers(){
        return filteredUsers;
     }
    
    public List<Username> getAllUsers() {
        return userFacade.findAll();
    }
    
    public List<Username> getUsersNameList() {
        return userFacade.findAllUsers();
    }
    
    public List<Username> getAllUsersName() {
        return userFacade.findAllByName();
    }
    
   
    
//    public AutocompleteMB getAutocompleteMB(){
//        return this.aCompleteMB;
//    }
    
//    public void setAutocomplete(AutocompleteMB autoComplete) {
//        this.autoComplete = autoComplete;
//     }
//    
//    //Autocomplete
//    public List<Username> completeUsername(String name) {
//
//        usernames = autoComplete.getUsersname();
//        List<Username> suggestions = new ArrayList<Username>();
//        for(Username names : usernames) {
//            if(names.getName().toLowerCase().startsWith(name))
//                suggestions.add(names);
//        }
//            return suggestions;
//    }
//    
//    public List<Username> getSelectedUsersname() {
//        return selectedUsername;
//    }
// 
//    public void setSelectedUsersname(List<Username> selectedUsername) {
//        this.selectedUsername = selectedUsername;
//    }
       
    
    public Group[] getGroups() {
        return Group.values();
    }
    
    public Username getSelectedUser(){
        return user;
    }
    
    public void setSelectedUser(Username user){
        this.user = user;
    }
    
    
    public String getEmail(){
        return email;
    }
    
    public void setEmail(String email){
        this.email = email;
    }
    
    public String getName(){
        return name;
    }
    
    public void setName(String name){
        this.name = name;
    }
    
    public String getUsername(){
        return username;
    }
    
    public void setUsername(String username){
        this.username = username;
    }
    
    
    
//    public void removeSessionAttributeAfterRender(PhaseEvent event) {
//        if (event.getPhaseId() == PhaseId.RENDER_RESPONSE) {
//            //FacesContext.getExternalContext().getSessionMap().get("studyManagedBean");
//            //Object sessionObj = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().
//           
//        }
//    }
    
    
    
    public String fetchUser(){
    
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String,String> params = fc.getExternalContext().getRequestParameterMap();
        this.email =  params.get("email"); 
        this.name =  params.get("name"); 
        this.username = params.get("username");
        
        return "studyMember";
    
    }

    public void addUser() {
        
        user.encodePassword();
        user.setRegisteredOn(new Date());
        //g.add(Group.BBC_ADMIN);
        user.setGroups(g);
        try {
            userFacade.persist(user);
        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Operation failed!");
            return;
        }
        addMessage("Successfully added: " + user.getName());
        
    }

    public void deleteUser() {
        try {
            userFacade.removeByEmail(user.getEmail());
        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Delete Operation failed.");
            //return;
        }

           addMessage("Delete Operation Completed.");
        
    }

    public void updateUser() {
        try {
            userFacade.update(user);
        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Update action failed.");
            return;
        }
        addMessage("Update Completed.");
       
    }

//    public void create() {
//        Username u = new Username();
//        u.setEmail("roshan@kth.se");
//        u.setMobileNum("022");
//        u.setName("Roshan Sedar");
//        u.setPassword("roshan");
//        u.setRegisteredOn(new Date());
//        u.setSalt("011".getBytes());
//        u.setUsername("admin");
//        List<Group> g = new ArrayList<Group>();
//        g.add(Group.USER);
//        u.setGroups(g);
//        userFacade.persist(u);
//    }
    
    public void addMessage(String summary) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, summary);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void addErrorMessageToUserAction(String message) {
        FacesMessage errorMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
        FacesContext.getCurrentInstance().addMessage(null, errorMessage);
    }

    public void changePassword() {
        addMessage("Change Password not implemented!");
    }

    public void logout() {
        addMessage("Logout not implemented!");
    }

    private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }
    
    
    public String getUserLogin(){
          return getRequest().getUserPrincipal().getName();
    }
    
    
   public String userManagement(){
       
            addMessage("Switched to the LIMS User Management Service!");
            return "userMgmt";
    }
   
//    public void onTabChange(TabChangeEvent event) {
//        FacesMessage msg = new FacesMessage("Tab Changed", "Active Tab: " + event.getTab().getTitle());
//        FacesContext.getCurrentInstance().addMessage(null, msg);
//    }
   
   
   public void confirmMessage(ActionEvent actionEvent){
   
       FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Deletion Successful!",  null);
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
}
