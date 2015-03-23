package se.kth.kthfsdashboard.user;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;
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
import org.apache.commons.lang.StringUtils;
import se.kth.bbc.lims.MessagesController;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@SessionScoped
public class UserController implements Serializable {

  private static final Logger logger = Logger.getLogger(UserController.class.
          getName());

  private static final long serialVersionUID = 1L;
  @EJB
  private UserFacade userFacade;
  private Username user;

  List<Group> groups;
  private List<Username> filteredUsers;
  private List<Username> selectedUsers;

  private String email;
  private String name;
  private String username;
  private Group role;

  private int tabIndex;

  private List<Username> requests;

  public UserController() {
  }

  @PostConstruct
  public void initGroups() {
    groups = new ArrayList<>(Arrays.asList(Group.values())); // to support the remove operation
    groups.remove(Group.GUEST);
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

  public void setFilteredUsers(List<Username> filteredUsers) {
    this.filteredUsers = filteredUsers;
  }

  public List<Username> getFilteredUsers() {
    return filteredUsers;
  }

  public List<Username> getAllUsers() {
    return userFacade.findAllByStatus(Username.STATUS_ALLOW);
  }

  public List<Username> getUsersNameList() {
    return userFacade.findAllUsers();
  }

  public List<Username> getAllUsersName() {
    return userFacade.findAllByName();
  }

  public List<Group> getGroups() {
    return groups;
  }

  public Username getSelectedUser() {
    return user;
  }

  public void setSelectedUser(Username user) {
    this.user = user;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Group getRole() {
    return role;
  }

  public void setRole(Group role) {
    this.role = role;
  }

  public String fetchUser() {

    FacesContext fc = FacesContext.getCurrentInstance();
    Map<String, String> params = fc.getExternalContext().
            getRequestParameterMap();
    this.email = params.get("email");
    this.name = params.get("name");
    this.username = params.get("username");

    return "studyMember";

  }

  public void deleteUser(Username user) {
    try {
      userFacade.removeByEmail(user.getEmail()); //userFacade.remove(user) doesn't seem to work
    } catch (EJBException ejb) {
      MessagesController.addErrorMessage("Error", "Delete operation failed");
    }
    MessagesController.addInfoMessage(user.getName() + " successfully removed.");
  }

  public void rejectUser(Username user) {
    try {
      userFacade.removeByEmail(user.getEmail()); //userFacade.remove(user) doesn't seem to work
    } catch (EJBException ejb) {
      MessagesController.addErrorMessage("Error", "Rejection failed");
    }
    MessagesController.addInfoMessage(user.getName() + " was rejected.");
    requests.remove(user);
  }

  public void deleteUser() {
    try {
      userFacade.removeByEmail(user.getEmail());
    } catch (EJBException ejb) {
      MessagesController.addErrorMessage("Error", "Delete operation failed.");
    }
    MessagesController.addInfoMessage(user.getName() + "successfully removed.");
  }

  public void updateUser() {
    try {
      userFacade.update(user);
    } catch (EJBException ejb) {
      MessagesController.addErrorMessage("Error", "Update action failed.");
      return;
    }
    MessagesController.addInfoMessage("Update Completed.");

  }

  private HttpServletRequest getRequest() {
    return (HttpServletRequest) FacesContext.getCurrentInstance().
            getExternalContext().getRequest();
  }

  public String getUserLogin() {
    return getRequest().getUserPrincipal().getName();
  }

  public String userManagement() {

    MessagesController.addInfoMessage(
            "Switched to the LIMS User Management Service!");
    return "userMgmt";
  }

//    public void onTabChange(TabChangeEvent event) {
//        FacesMessage msg = new FacesMessage("Tab Changed", "Active Tab: " + event.getTab().getTitle());
//        FacesContext.getCurrentInstance().addMessage(null, msg);
//    }
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
      return principal.getName();
    } catch (Exception ex) {
//            throw new RuntimeException("Not logged in");
      ExternalContext extContext = FacesContext.getCurrentInstance().
              getExternalContext();
      System.err.println(extContext.getRequestContextPath());
      extContext.redirect(extContext.getRequestContextPath());
      return null;
    }
  }

  /**
   * Get all open user requests.
   */
  public List<Username> getAllRequests() {
    if (requests == null) {
      requests = userFacade.findAllByStatus(Username.STATUS_REQUEST);
    }
    return requests;
  }

  public List<Username> getSelectedUsers() {
    return selectedUsers;
  }

  public void setSelectedUsers(List<Username> users) {
    this.selectedUsers = users;
  }

  /**
   * Accept the requests of the selected users.
   */
  public void allowSelectedUsers() {
    int num = selectedUsers.size();
    ArrayList<String> failedNames = new ArrayList<>();
    for (Username s : selectedUsers) {
      try {
        s.setStatus(Username.STATUS_ALLOW);
        userFacade.update(s);
        requests.remove(s);
      } catch (EJBException ejb) {
        failedNames.add(s.getEmail());
      }
    }
    if (!failedNames.isEmpty()) {
      if (failedNames.size() == num) {
        MessagesController.addErrorMessage("Operation failed.",
                "Requests were not processed.");
      } else {
        MessagesController.addErrorMessage("Operation partially failed.",
                "Requests for " + StringUtils.join(failedNames.iterator(), ", ")
                + " were not processed.");
      }
    } else {
      MessagesController.addInfoMessage("Operation successful");
    }
  }

  /**
   * Deny requests for the selected users.
   */
  public void denySelectedUsers() {
    int num = selectedUsers.size();
    ArrayList<String> failedNames = new ArrayList<>();
    for (Username s : selectedUsers) {
      try {
        userFacade.removeByEmail(s.getEmail());
        requests.remove(s);
      } catch (EJBException ejb) {
        failedNames.add(s.getEmail());
      }
    }
    if (!failedNames.isEmpty()) {
      if (failedNames.size() == num) {
        MessagesController.addErrorMessage("Operation failed.",
                "Requests were not processed.");
      } else {
        MessagesController.addErrorMessage("Operation partially failed.",
                "Requests for " + StringUtils.join(failedNames.iterator(), ", ")
                + " were not processed.");
      }
    } else {
      MessagesController.addInfoMessage("Operation successful");
    }
  }

  public String getGroup(Username user) {
    return user.getGroups().get(0).getGroup();
  }

  public void allowUser(Username user) {
    user.setStatus(Username.STATUS_ALLOW);
    System.out.println("User group:" + user.getExtraGroup());
    userFacade.update(user);
    requests.remove(user);
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
