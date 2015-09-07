/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.batch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.batch.api.chunk.AbstractItemReader;
import javax.ejb.EJB;
import javax.inject.Named;
import org.apache.log4j.Logger;
import se.kth.bbc.project.ProjectTeam;
import se.kth.bbc.project.ProjectTeamFacade;
import se.kth.bbc.security.ua.UserManager;
import se.kth.bbc.security.ua.model.User;
import se.kth.hopsworks.util.LocalhostServices;

/**
 * Batch job artifact - read the usernames
 *
 */
@Named
public class AuthorizedKeystemReader extends AbstractItemReader {

  private static final Logger logger = Logger.getLogger(AuthorizedKeystemReader.class);

  @EJB
  private UserManager userBean;
  @EJB
  private ProjectTeamFacade projectTeamFacade;

//  @Inject
//  private JobContext jobContext;

  private List<User> users;
  private Iterator<User> usersIter;

  public AuthorizedKeystemReader() {
  }

  @Override
  public void open(Serializable checkpoint) throws Exception {
    users = userBean.findAllUsers();
    usersIter = users.iterator();
  }

  @Override
  public List<String> readItem() throws Exception {
    List<String> projectUsers = new ArrayList<>();
    if (usersIter.hasNext()) {
      User u = usersIter.next();
      List<ProjectTeam> teams = projectTeamFacade.findByMember(u);
      for (ProjectTeam pt : teams) {
        projectUsers.add(LocalhostServices.getUsernameInProject(u.getUsername(), pt.getProject().getName()));
      }
    }
    return projectUsers;
  }
}
