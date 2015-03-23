package se.kth.kthfsdashboard.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

/**
 * Controller for the registration view.
 * <p>
 * @author stig
 */
@ManagedBean
@RequestScoped
public class RegistrationController implements Serializable {

  private static final Logger logger = Logger.getLogger(UserController.class.
          getName());

  private static final long serialVersionUID = 1L;
  private static final String USER_REGISTERED = "userRegistered";
  private static final String REGISTERED_SUM = "Registered.";
  private static final String REGISTERED_MESS
          = "Your registration request has been submitted for evaluation.";
  @EJB
  private UserFacade userFacade;
  private Username user;

  public Username getUser() {
    if (user == null) {
      user = new Username();
    }
    return user;
  }

  public void setUser(Username user) {
    this.user = user;
  }

  /**
   * Called upon completing the "Register user" form. Registers a user as having
   * requested access.
   */
  public String registerUser() {
    user.encodePassword();
    user.setRegisteredOn(new Date());
    List<Group> groups = new ArrayList<>();
    groups.add(Group.GUEST);
    user.setGroups(groups);
    user.setStatus(Username.STATUS_REQUEST);
    FacesMessage message;

    try {
      userFacade.persist(user);
      message = new FacesMessage(FacesMessage.SEVERITY_INFO, REGISTERED_SUM,
              REGISTERED_MESS);
    } catch (EJBException ejb) {
      message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
              "Failed registration.",
              "Something went wrong while trying to register your account.");
      logger.log(Level.SEVERE, "Failed insert");
    }
    FacesContext.getCurrentInstance().addMessage(USER_REGISTERED, message);
    FacesContext context = FacesContext.getCurrentInstance();
    context.getExternalContext().getFlash().setKeepMessages(true);
    return "welcome";
  }
}
