package se.kth.bbc.security.ua;

import java.io.Serializable;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * <p/>
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@SessionScoped
public class BbcViewController implements Serializable {

  @EJB
  private UserManager um;
  private Users current_user;

  public BbcViewController() {

  }

  public String loadLims() {
    if (renderLims()) {
      return "lims";
    } else {
      return "";
    }
  }

  public String loadWorkflows() {
    if (renderWorfkflows()) {
      return "workflows";
    } else {
      return "";
    }
  }

  public String loadClusters() {
    if (renderClusters()) {
      return "hadoop";
    } else {
      return "";
    }
  }

  public boolean renderLims() {
    return um.findGroups(current_user.getUid()).contains("BBC_ADMIN") || um.
            findGroups(current_user.getUid()).contains("SYS_ADMIN");
  }

  public boolean renderWorfkflows() {
    return um.findGroups(current_user.getUid()).contains("BBC_ADMIN")
            || um.findGroups(current_user.getUid()).contains("BBC_RESEARCHER")
            || um.findGroups(current_user.getUid()).contains("SYS_ADMIN");
  }

  public boolean renderClusters() {
    return um.findGroups(current_user.getUid()).contains("SYS_ADMIN");
  }

  public Users getCurrent_user() {
    return current_user;
  }

  public void setCurrent_user(Users current_user) {
    this.current_user = current_user;
  }

}
