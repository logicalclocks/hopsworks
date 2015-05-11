package se.kth.bbc.project;

import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.bbc.security.ua.model.User;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author roshan
 */
@Stateless
public class ProjectFacade extends AbstractFacade<Project> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ProjectFacade() {
    super(Project.class);
  }

  @Override
  public List<Project> findAll() {
    TypedQuery<Project> query = em.createNamedQuery("Project.findAll",
            Project.class);
    return query.getResultList();
  }

  public Project find(Integer id) {
    return em.find(Project.class, id);
  }

  /**
   * Find all the studies for which the given user is owner. This implies that
   * this user created all the returned studies.
   * <p>
   * @param user The user for whom studies are sought.
   * @return List of all the studies that were created by this user.
   */
  public List<Project> findByUser(User user) {
    TypedQuery<Project> query = em.createNamedQuery(
            "Project.findByOwner", Project.class).setParameter(
                    "owner", user);
    return query.getResultList();
  }

  /**
   * Find all the studies for which the user with given email is owner. This
   * implies that this user created all the returned studies.
   * <p>
   * @param email The email of the user for whom studies are sought.
   * @return List of all the studies that were created by this user.
   * @deprecated use findByUser(User user) instead.
   */
  public List<Project> findByUser(String email) {
    TypedQuery<User> query = em.createNamedQuery(
            "User.findByEmail", User.class).setParameter(
                    "email", email);
    User user = query.getSingleResult();
    return findByUser(user);
  }

  /**
   * Get the project with the given name created by the given User.
   * <p>
   * @param projectname The name of the project.
   * @param user The owner of the project.
   * @return The project with given name created by given user, or null if such
   * does not exist.
   */
  public Project findByNameAndOwner(String projectname, User user) {
    TypedQuery<Project> query = em.createNamedQuery("Project.findByOwnerAndName",
            Project.class).setParameter("name", projectname).setParameter("owner",
                    user);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Get the project with the given name created by the User with given email.
   * <p>
   * @param projectname The name of the project.
   * @param email The email of the owner of the project.
   * @return The project with given name created by given user, or null if such
   * does not exist.
   * @deprecated use findByNameAndOwner(String projectname, User user) instead.
   */
  public Project findByNameAndOwnerEmail(String projectname, String email) {
    TypedQuery<User> query = em.createNamedQuery("User.findByEmail",
            User.class).setParameter("email", email);
    User user = query.getSingleResult();
    return findByNameAndOwner(projectname, user);
  }

  /**
   * Count the number of studies for which the given user is owner.
   * <p>
   * @param owner
   * @return
   */
  public int countOwnedStudies(User owner) {
    TypedQuery<Long> query = em.createNamedQuery("Project.countProjectByOwner",
            Long.class);
    query.setParameter("owner", owner);
    return query.getSingleResult().intValue();
  }

  /**
   * Count the number of studies for which the owner has the given email.
   * <p>
   * @param email
   * @return The number of studies.
   * @deprecated Use countOwnedStudies(User owner) instead.
   */
  public int countOwnedStudies(String email) {
    TypedQuery<User> query = em.createNamedQuery("User.findByEmail", User.class);
    query.setParameter("email", email);
    //TODO: may throw an exception
    User user = query.getSingleResult();
    return countOwnedStudies(user);
  }

  /**
   * Find all the studies owned by the given user.
   * <p>
   * @param user
   * @return
   */
  public List<Project> findOwnedStudies(User user) {
    TypedQuery<Project> query = em.createNamedQuery("Project.findByOwner",
            Project.class);
    query.setParameter("owner", user);
    return query.getResultList();
  }

  /**
   * Get the owner of the given project.
   * <p>
   * @param project The project for which to get the current owner.
   * @return The primary key of the owner of the project.
   * @deprecated Use project.getOwner().getEmail(); instead.
   */
  public String findOwner(Project project) {
    return project.getOwner().getEmail();
  }

  /**
   * Find all the studies the given user is a member of.
   * <p>
   * @param user
   * @return
   */
  public List<Project> findAllMemberStudies(User user) {
    TypedQuery<Project> query = em.createNamedQuery(
            "ProjectTeam.findAllMemberStudiesForUser",
            Project.class);
    query.setParameter("user", user);
    return query.getResultList();
  }

  /**
   * Find all studies created (and owned) by this user.
   * <p>
   * @param user
   * @return
   */
  public List<Project> findAllPersonalStudies(User user) {
    TypedQuery<Project> query = em.createNamedQuery("Project.findByOwner",
            Project.class);
    query.setParameter("owner", user);
    return query.getResultList();
  }

  /**
   * Get all the studies this user has joined, but not created.
   * <p>
   * @param user
   * @return
   */
  public List<Project> findAllJoinedStudies(User user) {
    TypedQuery<Project> query = em.createNamedQuery(
            "ProjectTeam.findAllJoinedStudiesForUser",
            Project.class);
    query.setParameter("user", user);
    return query.getResultList();
  }

  public void persistProject(Project project) {
    em.persist(project);
  }

  public void flushEm() {
    em.flush();
  }

  /**
   * Mark the project <i>project</i> as deleted.
   * <p>
   * @param project
   */
  public void removeProject(Project project) {
    project.setDeleted(Boolean.TRUE);
    em.merge(project);
  }

  /**
   * Check if a project with this name already exists.
   * <p>
   * @param name
   * @return
   */
  public boolean projectExists(String name) {
    TypedQuery<Project> query = em.createNamedQuery("Project.findByName",
            Project.class);
    query.setParameter("name", name);
    return !query.getResultList().isEmpty();
  }

  /**
   * Check if a project with this name already exists for a user.
   * <p>
   * @param name
   * @param owner
   * @return
   */
  public boolean projectExistsForOwner(String name, User owner) {
    TypedQuery<Project> query = em.createNamedQuery("Project.findByOwnerAndName",
            Project.class);
    query.setParameter("owner", owner).setParameter("name", name);
    return !query.getResultList().isEmpty();
  }

  /**
   * Merge the new project.
   * <p>
   * @param newProject
   */
  public void mergeProject(Project newProject) {
    em.merge(newProject);
  }

  public void archiveProject(String projectname) {
    Project project = findByName(projectname);
    if (project != null) {
      project.setArchived(true);
    }
    em.merge(project);
  }

  public void unarchiveProject(String projectname) {
    Project project = findByName(projectname);
    if (project != null) {
      project.setArchived(false);
    }
    em.merge(project);
  }

  public boolean updateRetentionPeriod(String name, Date date) {
    Project project = findByName(name);
    if (project != null) {
      project.setRetentionPeriod(date);
      em.merge(project);
      return true;
    }
    return false;
  }

  public Date getRetentionPeriod(String name) {
    Project project = findByName(name);
    if (project != null) {
      return project.getRetentionPeriod();
    }
    return null;
  }

  private Project findByName(String name) {
    TypedQuery<Project> query = em.createNamedQuery("Project.findByName",
            Project.class);
    query.setParameter("name", name);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
