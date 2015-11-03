package se.kth.bbc.lims;

import java.io.Serializable;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.security.ua.UserManager;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author stig
 */
@ManagedBean
@SessionScoped
public class ClientSessionState implements Serializable {

  @EJB
  private ProjectFacade projectFacade;

  @EJB
  private UserManager userFacade;

  private Project activeProject;

  private Users user;

  public void setActiveProject(Project project) {
    this.activeProject = project;
  }

  public Project getActiveProject() {
    return activeProject;
  }

  public String getActiveProjectname() {
    if (activeProject != null) {
      return activeProject.getName();
    } else {
      return null;
    }
  }

  public void setActiveProjectByUserAndName(Users user, String projectname) {
    activeProject = projectFacade.findByNameAndOwner(projectname, user);
  }

  private HttpServletRequest getRequest() {
    return (HttpServletRequest) FacesContext.getCurrentInstance().
            getExternalContext().getRequest();
  }

  /**
   * Get the username of the user currently logged in.
   * <p/>
   * @return Email address of the user currently logged in. (Serves as
   * username.)
   */
  public String getLoggedInUsername() {
    return getRequest().getUserPrincipal().getName();
  }

  public Users getLoggedInUser() {
    if (user == null) {
      String email = getRequest().getUserPrincipal().getName();
      user = userFacade.findByEmail(email);
    }
    return user;
  }

}
