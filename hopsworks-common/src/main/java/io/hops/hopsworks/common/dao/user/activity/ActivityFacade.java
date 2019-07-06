/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.hops.hopsworks.common.dao.user.activity;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class ActivityFacade extends AbstractFacade<Activity> {

  private static final Logger LOGGER = Logger.getLogger(ActivityFacade.class.getName());

  // String constants
  public static final String NEW_PROJECT = " created a new project named ";
  public static final String NEW_DATA = " added a new dataset named ";
  public static final String SHARED_DATA = " shared dataset ";
  public static final String UNSHARED_DATA = " unshared dataset ";
  public static final String NEW_MEMBER = " added a member ";
  public static final String CHANGE_ROLE = " changed the role of ";
  public static final String REMOVED_MEMBER = " removed team member ";
  public static final String RAN_JOB = " ran a job named ";
  public static final String ADDED_SERVICE = " added new service ";
  public static final String PROJECT_DESC_CHANGED = " changed project description ";
  public static final String PROJECT_RETENTION_CHANGED = " changed project retention ";
  public static final String CREATED_JOB = " created a new job named ";
  public static final String DELETED_JOB = " deleted a job named ";
  public static final String SCHEDULED_JOB = " scheduled a job named ";
  public static final String EXECUTED_JOB = " ran a job used as input file ";
  public static final String CREATED_FEATURESTORE = " created a new feature store named ";
  public static final String CREATED_FEATUREGROUP = " created a new feature group named ";
  public static final String CREATED_TRAINING_DATASET = " created a new training dataset named ";
  public static final String DELETED_FEATUREGROUP = " deleted a feature group named ";
  public static final String DELETED_TRAINING_DATASET = " deleted a training dataset named ";
  public static final String CREATED_NEW_VERSION_OF_FEATUREGROUP = " created a new version of a feature group named ";
  public static final String EDITED_FEATUREGROUP = " edited feature group named ";
  public static final String EDITED_TRAINING_DATASET = " edited training dataset named ";
  public static final String ADDED_FEATURESTORE_STORAGE_CONNECTOR = " added a storage connector for the featurestore " +
      "with name: ";
  public static final String REMOVED_FEATURESTORE_STORAGE_CONNECTOR = " added a storage connector for " +
      "the featurestore with name: ";
  // Flag constants
  public static final String FLAG_PROJECT = "PROJECT";
  public static final String FLAG_DATASET = "DATASET";
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ActivityFacade() {
    super(Activity.class);
  }

  public void persistActivity(Activity activity) {
    em.persist(activity);
  }

  public void removeActivity(Activity activity) {
    em.remove(activity);
  }

  public long getTotalCount() {
    TypedQuery<Long> q = em.createNamedQuery("Activity.countAll", Long.class);
    return q.getSingleResult();
  }

  public long getProjectCount(Project project) {
    TypedQuery<Long> q = em.createNamedQuery("Activity.countPerProject", Long.class);
    q.setParameter("project", project);
    return q.getSingleResult();
  }

  public Activity activityByID(int id) {
    TypedQuery<Activity> query = em.createNamedQuery("Activity.findById", Activity.class).setParameter("id", id);
    Activity activity = null;
    try {
      activity = query.getSingleResult();
    } catch (NoResultException e) {
      LOGGER.log(Level.FINE, e.getMessage());
    }
    return activity;
  }

  public Activity lastActivityOnProject(Project project) {
    TypedQuery<Activity> query = em.createNamedQuery("Activity.findByProject", Activity.class);
    query.setParameter("project", project);
    query.setMaxResults(1);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      LOGGER.log(Level.SEVERE, "No activity returned for project " + project
          + ", while its creation should always be there!", e);
      return null;
    }
  }

  public void persistActivity(String activity, Project project, Users user, ActivityFlag flag) {
    Activity a = new Activity();
    a.setActivity(activity);
    a.setProject(project);
    a.setFlag(flag);
    a.setUser(user);
    a.setTimestamp(new Date());
    em.persist(a);
  }

  public void persistActivity(String activity, Project project, String email, ActivityFlag flag) {
    TypedQuery<Users> userQuery = em.createNamedQuery("Users.findByEmail", Users.class);
    userQuery.setParameter("email", email);
    Users user;
    try {
      user = userQuery.getSingleResult();
    } catch (NoResultException e) {
      throw new IllegalArgumentException("No user found with email " + email
          + " when trying to persist activity for that user.", e);
    }
    persistActivity(activity, project, user, flag);
  }

  /**
   * Gets all activity information.
   * <p/>
   * @return
   */
  public List<Activity> getAllActivities() {
    TypedQuery<Activity> q = em.createNamedQuery("Activity.findAll", Activity.class);
    return q.getResultList();
  }

  /**
   * Get all the activities performed on project <i>project</i>.
   * <p/>
   * @param project
   * @return
   */
  public List<Activity> getAllActivityByProject(Project project) {
    TypedQuery<Activity> q = em.createNamedQuery("Activity.findByProject", Activity.class);
    q.setParameter("project", project);
    return q.getResultList();
  }

  public Activity getActivityByIdAndProject(Project project, Integer id) {
    TypedQuery<Activity> q = em.createNamedQuery("Activity.findByIdAndProject", Activity.class);
    q.setParameter("id", id);
    q.setParameter("project", project);
    Activity activity = null;
    try {
      activity = q.getSingleResult();
    } catch (NoResultException e) {
      LOGGER.log(Level.FINE, e.getMessage());
    }
    return activity;
  }

  /**
   * Get all the activities performed on by user <i>user</i>.
   * <p/>
   * @param user
   * @return
   */
  public List<Activity> getAllActivityByUser(Users user) {
    TypedQuery<Activity> q = em.createNamedQuery("Activity.findByUser", Activity.class);
    q.setParameter("user", user);
    return q.getResultList();
  }

  public Activity getActivityByIdAndUser(Users user, Integer id) {
    TypedQuery<Activity> q = em.createNamedQuery("Activity.findByIdAndUser", Activity.class);
    q.setParameter("id", id);
    q.setParameter("user", user);
    Activity activity = null;
    try {
      activity = q.getSingleResult();
    } catch (NoResultException e) {
      LOGGER.log(Level.FINE, e.getMessage());
    }
    return activity;
  }

  /**
   * Get all the activities performed on by user <i>user</i>.but paginated.Items
   * from
   * <i>offset</i> till
   * <i>offset+limit</i> are returned.
   * <p/>
   * @param offset
   * @param limit
   * @param user
   * @return
   */
  public List<Activity> getPaginatedActivityByUser(Integer offset, Integer limit, Users user) {
    TypedQuery<Activity> q = em.createNamedQuery("Activity.findByUser", Activity.class);
    q.setParameter("user", user);
    setOffsetAndLim(offset, limit, q);
    return q.getResultList();
  }

  /**
   * Returns all activity, but paginated. Items from <i>offset</i> till
   * <i>offset+limit</i> are returned.
   * <p/>
   * @param offset
   * @param limit
   * @return
   */
  public List<Activity> getPaginatedActivity(Integer offset, Integer limit) {
    TypedQuery<Activity> q = em.createNamedQuery("Activity.findAll", Activity.class);
    setOffsetAndLim(offset, limit, q);
    return q.getResultList();
  }

  /**
   * Returns all activities on project <i>projectName</i>, but paginated. Items
   * from
   * <i>offset</i> till
   * <i>offset+limit</i> are returned.
   * <p/>
   * @param offset
   * @param limit
   * @param project
   * @return
   */
  public List<Activity> getPaginatedActivityForProject(Integer offset, Integer limit, Project project) {
    TypedQuery<Activity> q = em.createNamedQuery("Activity.findByProject", Activity.class);
    q.setParameter("project", project);
    setOffsetAndLim(offset, limit, q);
    return q.getResultList();
  }

  public List<Activity> findAllTeamActivity(String flag) {
    Query query = em.createNamedQuery("Activity.findByFlag", Activity.class).setParameter("flag", flag);
    return query.getResultList();
  }

  public CollectionInfo findAllByProject(Integer offset, Integer limit, Set<? extends AbstractFacade.FilterBy> filter,
    Set<? extends AbstractFacade.SortBy> sort, Project project) {
    String queryStr = buildQuery("SELECT u FROM Activity u ", filter, sort, "u.project = :project ");
    String queryCountStr = buildQuery("SELECT COUNT(u.id) FROM Activity u ", filter, sort, "u.project = :project ");
    Query query = em.createQuery(queryStr, Activity.class).setParameter("project", project);
    Query queryCount = em.createQuery(queryCountStr, Activity.class).setParameter("project", project);
    return findAll(offset, limit, filter, query, queryCount);
  }
  
  public CollectionInfo findAllByUser(Integer offset, Integer limit,
    Set<? extends AbstractFacade.FilterBy> filter,
    Set<? extends AbstractFacade.SortBy> sort, Users user) {
    String queryStr = buildQuery("SELECT u FROM Activity u ", filter, sort, "u.user = :user ");
    String queryCountStr = buildQuery("SELECT COUNT(u.id) FROM Activity u ", filter, sort, "u.user = :user ");
    Query query = em.createQuery(queryStr, Activity.class).setParameter("user", user);
    Query queryCount = em.createQuery(queryCountStr, Activity.class).setParameter("user", user);
    return findAll(offset, limit, filter, query, queryCount);
  }
  
  private CollectionInfo findAll(Integer offset, Integer limit,
    Set<? extends AbstractFacade.FilterBy> filter, Query query, Query queryCount) {
    setFilter(filter, query);
    setFilter(filter, queryCount);
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }

  private void setFilter(Set<? extends AbstractFacade.FilterBy> filter, Query q) {
    if (filter == null || filter.isEmpty()) {
      return;
    }
    for (FilterBy aFilter : filter) {
      setFilterQuery(aFilter, q);
    }
  }

  private void setFilterQuery(AbstractFacade.FilterBy filterBy, Query q) {
    List<ActivityFlag> activityFlags = getEnumValues(filterBy, ActivityFlag.class);
    q.setParameter(filterBy.getField(), activityFlags);
  }

  public enum Sorts {
    ID("ID", "u.id ", "ASC"),
    FLAG("FLAG", "u.flag ", "ASC"),
    DATE_CREATED("DATE_CREATED", "u.timestamp ", "ASC");

    private final String value;
    private final String sql;
    private final String defaultParam;

    private Sorts(String value, String sql, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.defaultParam = defaultParam;
    }

    public String getValue() {
      return value;
    }

    public String getSql() {
      return sql;
    }

    public String getDefaultParam() {
      return defaultParam;
    }
    
    public String getJoin(){
      return null;
    }

    @Override
    public String toString() {
      return value;
    }

  }

  public enum Filters {
    FLAG("FLAG", "u.flag IN :flag ", "flag", "PROJECT"),
    FLAG_NEQ("FLAG_NEQ", "u.flag NOT IN :flag_neq ", "flag_neq", "PROJECT");

    private final String value;
    private final String sql;
    private final String field;
    private final String defaultParam;

    private Filters(String value, String sql, String field, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.field = field;
      this.defaultParam = defaultParam;
    }

    public String getDefaultParam() {
      return defaultParam;
    }

    public String getValue() {
      return value;
    }

    public String getSql() {
      return sql;
    }

    public String getField() {
      return field;
    }

    @Override
    public String toString() {
      return value;
    }

  }
}
