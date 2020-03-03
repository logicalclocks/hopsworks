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
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.project.Project;
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

  public enum CondaOp {
    CLONE,
    CREATE,
    BACKUP,
    REMOVE,
    LIST,
    INSTALL,
    UNINSTALL,
    UPGRADE,
    CLEAN,
    YML,
    EXPORT;

    public boolean isEnvOp() {
      return CondaOp.isEnvOp(this);
    }
  
    public static boolean isEnvOp(CondaOp arg) {
      if (arg.compareTo(CondaOp.CLONE) == 0 || arg.compareTo(CondaOp.CREATE) == 0 || arg.compareTo(CondaOp.YML) == 0 ||
        arg.compareTo(CondaOp.REMOVE) == 0 || arg.compareTo(CondaOp.BACKUP) == 0 || arg.compareTo(CondaOp.CLEAN) == 0
        || arg.compareTo(CondaOp.EXPORT) == 0) {
        return true;
      }
      return false;
    }
  }

  public enum CondaInstallType {
    ENVIRONMENT,
    CONDA,
    PIP
  }

  public enum CondaStatus {
    NEW,
    SUCCESS,
    ONGOING,
    FAILED
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
      if (cc.getOp().equals(CondaOp.CREATE) || cc.getOp().equals(CondaOp.YML) || cc.getOp().equals(CondaOp.EXPORT)) {
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
  
  public List<CondaCommands> findUnfinishedByHost(Hosts host) {
    TypedQuery<CondaCommands> query = em.createNamedQuery("CondaCommands.findNotFinishedByHost",
      CondaCommands.class);
    query.setParameter("host", host);
    return query.getResultList();
  }
  
  public List<CondaCommands> findByHost(Hosts host) {
    TypedQuery<CondaCommands> query = em.createNamedQuery("CondaCommands.findByHost", CondaCommands.class);
    query.setParameter("host", host);
    return query.getResultList();
  }
  
  public List<CondaCommands> findByStatus(CondaCommandFacade.CondaStatus status) {
    TypedQuery<CondaCommands> query = em.createNamedQuery("CondaCommands.findByStatus", CondaCommands.class);
    query.setParameter("status", status);
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
        .setParameter("installType", CondaCommandFacade.CondaInstallType.ENVIRONMENT).setParameter("project", project);
    Query queryCount = em.createQuery(queryCountStr, CondaCommands.class)
        .setParameter("installType", CondaCommandFacade.CondaInstallType.ENVIRONMENT).setParameter("project", project);
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
        .setParameter("installType", CondaCommandFacade.CondaInstallType.ENVIRONMENT).setParameter("project", project);
    Query queryCount = em.createQuery(queryCountStr, CondaCommands.class).setParameter("lib", libName)
        .setParameter("installType", CondaCommandFacade.CondaInstallType.ENVIRONMENT).setParameter("project", project);
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
      case MACHINE_TYPE:
      case MACHINE_TYPE_NEQ:
        setMachineType(filterBy, q);
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
    List<CondaCommandFacade.CondaOp> ops = getEnumValues(filterBy, CondaCommandFacade.CondaOp.class);
    q.setParameter(filterBy.getField(), ops);
  }

  private void setStatus(AbstractFacade.FilterBy filterBy, Query q) {
    List<CondaCommandFacade.CondaStatus> status = getEnumValues(filterBy, CondaCommandFacade.CondaStatus.class);
    q.setParameter(filterBy.getField(), status);
  }

  private void setMachineType(AbstractFacade.FilterBy filterBy, Query q) {
    List<LibraryFacade.MachineType> machineTypes = getEnumValues(filterBy, LibraryFacade.MachineType.class);
    q.setParameter(filterBy.getField(), machineTypes);
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
    MACHINE_TYPE("MACHINE_TYPE", "c.machineType IN :machineType ", "machineType", "ALL"),
    MACHINE_TYPE_NEQ("MACHINE_TYPE_NEQ", "c.machineType NOT IN :machineType_neq ", "machineType_neq", "CPU"),
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
