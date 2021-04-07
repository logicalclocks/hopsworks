/*
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
 */
package io.hops.hopsworks.common.dao.python;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.CondaInstallType;
import io.hops.hopsworks.persistence.entity.python.CondaOp;
import io.hops.hopsworks.persistence.entity.python.CondaStatus;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.Query;

@Stateless
public class CondaCommandFacade extends AbstractFacade<CondaCommands> {

  private static final Logger LOGGER = Logger.getLogger(CondaCommandFacade.class.getName());
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public CondaCommandFacade() {
    super(CondaCommands.class);
  }

  public int deleteAllCommandsByStatus(CondaStatus status) {
    TypedQuery<CondaCommands> query = em.createNamedQuery("CondaCommands.deleteAllFailedCommands",
        CondaCommands.class);
    query.setParameter("status", status);
    return query.executeUpdate();
  }

  public List<CondaCommands> getCommandsForProject(Project proj) {
    TypedQuery<CondaCommands> query = em.createNamedQuery("CondaCommands.findByProj", CondaCommands.class);
    query.setParameter("projectId", proj);
    return query.getResultList();
  }
  
  public List<CondaCommands> getFailedCommandsForProject(Project proj) {
    TypedQuery<CondaCommands> query =
      em.createNamedQuery("CondaCommands.findByProjectAndStatus", CondaCommands.class);
    query.setParameter("projectId", proj);
    query.setParameter("status", CondaStatus.FAILED);
    return query.getResultList();
  }
  
  public List<CondaCommands> getFailedEnvCommandsForProject(Project proj) {
    TypedQuery<CondaCommands> query =
      em.createNamedQuery("CondaCommands.findByProjectAndTypeAndStatus", CondaCommands.class);
    query.setParameter("projectId", proj);
    query.setParameter("installType", CondaInstallType.ENVIRONMENT);
    query.setParameter("status", CondaStatus.FAILED);
    return query.getResultList();
  }
  
  public List<CondaCommands> getFailedCommandsForProjectAndLib(Project proj, String lib) {
    TypedQuery<CondaCommands> query =
      em.createNamedQuery("CondaCommands.findByProjectAndLibAndStatus", CondaCommands.class);
    query.setParameter("projectId", proj);
    query.setParameter("lib", lib);
    query.setParameter("status", CondaStatus.FAILED);
    return query.getResultList();
  }
  
  public void deleteCommandsForLibrary(Project proj, String dependency) {
    List<CondaCommands> commands = getCommandsForProject(proj);
    for (CondaCommands cc : commands) {
      // delete the conda library command if it has the same name as the input library name
      if (cc.getLib().compareToIgnoreCase(dependency) == 0) {
        em.remove(cc);
      }
    }
  }

  public void deleteCommandsForEnvironment(Project proj) {
    List<CondaCommands> commands = getCommandsForProject(proj);
    for (CondaCommands cc : commands) {
      // delete the conda library command if it has the same name as the input library name
      if (cc.getOp().equals(CondaOp.CREATE) || cc.getOp().equals(CondaOp.EXPORT)) {
        em.remove(cc);
      }
    }
  }
  
  public void removeCondaCommand(int commandId) {
    CondaCommands cc = findCondaCommand(commandId);
    if (cc != null) {
      em.remove(cc);
      em.flush();
    } else {
      LOGGER.log(Level.FINE, "Could not remove CondaCommand with id: {0}", commandId);
    }
  }
  
  public List<CondaCommands> findByStatus(CondaStatus status) {
    TypedQuery<CondaCommands> query = em.createNamedQuery("CondaCommands.findByStatus", CondaCommands.class);
    query.setParameter("status", status);
    return query.getResultList();
  }
  
  public List<CondaCommands> findByStatusAndCondaOp(CondaStatus status, CondaOp op) {
    TypedQuery<CondaCommands> query = em.createNamedQuery("CondaCommands.findByStatusAndCondaOp", CondaCommands.class);
    query.setParameter("status", status);
    query.setParameter("op", op);
    return query.getResultList();
  }
  
  public List<CondaCommands> findByStatusAndCondaOpAndProject(List<CondaStatus> statuses, CondaOp op, Project project) {
    TypedQuery<CondaCommands> query =
      em.createNamedQuery("CondaCommands.findByStatusListAndCondaOpAndProject", CondaCommands.class);
    query.setParameter("statuses", statuses);
    query.setParameter("op", op);
    query.setParameter("project", project);
    return query.getResultList();
  }

  public CondaCommands findCondaCommand(int commandId) {
    return em.find(CondaCommands.class, commandId);
  }
  
  public CollectionInfo findAllEnvCmdByProject(Integer offset, Integer limit,
      Set<? extends AbstractFacade.FilterBy> filter, Set<? extends AbstractFacade.SortBy> sort, Project project) {
    String queryStr = buildQuery("SELECT c FROM CondaCommands c ", filter, sort,
        "c.installType = :installType AND c.projectId = :project ");
    String queryCountStr = buildQuery("SELECT COUNT(c.id) FROM CondaCommands c ", filter, sort,
        "c.installType = :installType AND c.projectId = :project ");
    Query query = em.createQuery(queryStr, CondaCommands.class)
        .setParameter("installType", CondaInstallType.ENVIRONMENT).setParameter("project", project);
    Query queryCount = em.createQuery(queryCountStr, CondaCommands.class)
        .setParameter("installType", CondaInstallType.ENVIRONMENT).setParameter("project", project);
    return findAll(offset, limit, filter, query, queryCount);
  }

  public CollectionInfo findAllLibCmdByProject(Integer offset, Integer limit,
      Set<? extends AbstractFacade.FilterBy> filter, Set<? extends AbstractFacade.SortBy> sort, Project project, 
      String libName) {
    String queryStr = buildQuery("SELECT c FROM CondaCommands c ", filter, sort,
        "c.lib = :lib AND c.installType <> :installType AND c.projectId = :project ");
    String queryCountStr = buildQuery("SELECT COUNT(c.id) FROM CondaCommands c ", filter, sort,
        "c.lib = :lib AND c.installType <> :installType AND c.projectId = :project ");
    Query query = em.createQuery(queryStr, CondaCommands.class).setParameter("lib", libName)
        .setParameter("installType", CondaInstallType.ENVIRONMENT).setParameter("project", project);
    Query queryCount = em.createQuery(queryCountStr, CondaCommands.class).setParameter("lib", libName)
        .setParameter("installType", CondaInstallType.ENVIRONMENT).setParameter("project", project);
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
    for (AbstractFacade.FilterBy aFilter : filter) {
      setFilterQuery(aFilter, q);
    }
  }

  private void setFilterQuery(AbstractFacade.FilterBy filterBy, Query q) {
    switch (Filters.valueOf(filterBy.getValue())) {
      case OP:
      case OP_NEQ:
        setCondaOp(filterBy, q);
        break;
      case STATUS:
      case STATUS_NEQ:
        setStatus(filterBy, q);
        break;
      case HOST_IN:
      case HOST_NIN:
        setHosts(filterBy, q);
        break;
      case HOST_LT:
      case HOST_GT:
        setHostsLTandGT(filterBy, q);
        break;
      default:
        break;
    }
  }
  
  private void setCondaOp(AbstractFacade.FilterBy filterBy, Query q) {
    List<CondaOp> ops = getEnumValues(filterBy, CondaOp.class);
    q.setParameter(filterBy.getField(), ops);
  }

  private void setStatus(AbstractFacade.FilterBy filterBy, Query q) {
    List<CondaStatus> status = getEnumValues(filterBy, CondaStatus.class);
    q.setParameter(filterBy.getField(), status);
  }

  private void setHosts(AbstractFacade.FilterBy filterBy, Query q) {//set name
    List<Integer> hosts = getIntValues(filterBy);
    q.setParameter(filterBy.getField(), hosts);
  }
  
  private void setHostsLTandGT(AbstractFacade.FilterBy filterBy, Query q) {
    Integer val = getIntValue(filterBy);
    q.setParameter(filterBy.getField(), val);
  }

  public enum Sorts {
    ID("ID", "c.id ", "ASC"),
    HOST("HOST", "c.hostId ", "ASC"),
    STATUS("STATUS", "c.status ", "ASC"),
    DATE_CREATED("DATE_CREATED", "c.created ", "ASC");

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

    public String getJoin() {
      return null;
    }

    @Override
    public String toString() {
      return value;
    }

  }

  public enum Filters {
    OP("OP", "c.op IN :op ", "op", "CREATE"),
    OP_NEQ("OP_NEQ", "c.op NOT IN :op_neq ", "op_neq", "CREATE"),
    STATUS("STATUS", "c.status IN :status ", "status", "NEW"),
    STATUS_NEQ("STATUS_NEQ", "c.status NOT IN :status_neq ", "status_neq", "NEW"),
    HOST_IN("HOST_IN", "c.hostId IN :hostId_in ", "hostId_in", "1"),
    HOST_NIN("HOST_NIN", "c.hostId NOT IN :hostId_nin ", "hostId_nin", "1"),
    HOST_LT("HOST_LT", "c.hostId < :hostId_lt ", "hostId_lt", "2"),
    HOST_GT("HOST_GT", "c.hostId > :hostId_gt ", "hostId_gt", "2");

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
