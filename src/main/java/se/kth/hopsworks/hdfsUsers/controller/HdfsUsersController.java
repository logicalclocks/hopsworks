package se.kth.hopsworks.hdfsUsers.controller;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.hdfsUsers.HdfsUsersFacade;
import se.kth.hopsworks.user.model.Users;
import io.hops.security.UsersGroups;
import se.kth.hopsworks.dataset.Dataset;

@Stateless
public class HdfsUsersController {

  @EJB
  public HdfsUsersFacade hdfsUsersFacade;

  /**
   * Creates a new group in HDFS with the name <code>projectName</code> if it
   * does not exist, then a new user is created in HDFS with the name
   * <code>projectName</code>__<code>username</code> ,also if it does not exist, 
   * and gets added to the group <code>projectName</code>. This will add a
   * data_owner with full privileges to the project.
   * <p>
   * @param user
   * @param project
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void addProjectMember(Users user, Project project) {

  }

  /**
   * Create a new group in HDFS with the name project.name__datasetName if it
   * does not exist, then adds all members of the project to this group. This is
   * done when a new dataset is created in a project.
   * <p>
   * @param project
   * @param datasetName
   * @param stickyBit
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void addDatasetUsersGroups(Project project, String datasetName,
          boolean stickyBit) {

  }

  /**
   * Removes the user project__username. This should cascade to the groups the
   * user is a member of. This can remove a data_owner or a data_scientist.
   * <p>
   * @param user
   * @param projectName
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void removeProjectMember(Users user, String projectName) {

  }

  /**
   * Removes the user project__username from the group projectName.
   * This means the user is no longer a data_owner in this project.
   * (will be a data_scientist)
   * <p>
   * @param user
   * @param project
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void modifyProjectMembership(Users user, Project project) {

  }

  /**
   * Adds all members of project to the dataset's group.
   * <p>
   * @param project
   * @param dataset
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void shareDataset(Project project, Dataset dataset) {

  }
  
}
