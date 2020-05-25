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
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.AnacondaRepo;
import io.hops.hopsworks.persistence.entity.python.CondaInstallType;
import io.hops.hopsworks.persistence.entity.python.CondaStatus;
import io.hops.hopsworks.persistence.entity.python.MachineType;
import io.hops.hopsworks.persistence.entity.python.PythonDep;
import io.hops.hopsworks.restutils.RESTCodes;
import java.util.Collection;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

@Stateless
public class LibraryFacade extends AbstractFacade<PythonDep> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public LibraryFacade() {
    super(PythonDep.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public AnacondaRepo getRepo(String channelUrl, boolean create) throws ServiceException {
    TypedQuery<AnacondaRepo> query = em.createNamedQuery("AnacondaRepo.findByUrl", AnacondaRepo.class);
    query.setParameter("url", channelUrl);
    AnacondaRepo repo = null;
    try {
      repo = query.getSingleResult();
    } catch (NoResultException ex) {
      if (create) {
        repo = new AnacondaRepo();
        repo.setUrl(channelUrl);
        em.persist(repo);
        em.flush();
      }

    }
    if (repo == null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_REPO_ERROR, Level.SEVERE);
    }
    return repo;
  }

  public PythonDep getOrCreateDep(PythonDep dep) {
    return getOrCreateDep(dep.getRepoUrl(), dep.getMachineType(), dep.getInstallType(), dep.getDependency(), dep.
        getVersion(), true, dep.isPreinstalled(), dep.getBaseEnv());
  }
  
  public PythonDep getOrCreateDep(AnacondaRepo repo, MachineType machineType,
                                  CondaInstallType installType,
                                  String dependency, String version, boolean persist, boolean preinstalled) {
    return getOrCreateDep(repo, machineType, installType, dependency, version, persist, preinstalled, null);
  }
  
  public PythonDep getOrCreateDep(AnacondaRepo repo, MachineType machineType,
                                  CondaInstallType installType,
                                  String dependency, String version, boolean persist, boolean preinstalled,
                                  String baseEnv) {
    TypedQuery<PythonDep> deps = em.createNamedQuery("PythonDep.findUniqueDependency", PythonDep.class);
    deps.setParameter("dependency", dependency);
    deps.setParameter("version", version);
    deps.setParameter("installType", installType);
    deps.setParameter("repoUrl", repo);
    deps.setParameter("machineType", machineType);
    PythonDep dep = null;
    try {
      dep = deps.getSingleResult();
    } catch (NoResultException ex) {
      dep = new PythonDep();
      dep.setRepoUrl(repo);
      dep.setDependency(dependency);
      dep.setVersion(version);
      dep.setPreinstalled(preinstalled);
      dep.setInstallType(installType);
      dep.setMachineType(machineType);
      dep.setBaseEnv(baseEnv);
      if (persist) {
        em.persist(dep);
        em.flush();
      }
    }
    return dep;
  }
  
  public Collection<PythonDep> getBaseEnvDeps(String baseEnvName){
    TypedQuery<PythonDep> deps = em.createNamedQuery("PythonDep.findBaseEnv", PythonDep.class);
    deps.setParameter("baseEnv", baseEnvName);
    return deps.getResultList();
  }
  
  public PythonDep findByDependencyAndProject(String dependency, Project project) {
    try {
      return em.createNamedQuery("PythonDep.findByDependencyAndProject", PythonDep.class)
        .setParameter("dependency", dependency).setParameter("project", project).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public CollectionInfo findInstalledPythonDepsByProject(Integer offset, Integer limit,
      Set<? extends AbstractFacade.FilterBy> filter, Set<? extends AbstractFacade.SortBy> sort, Project project) {
    String queryStr = buildQuery("SELECT p FROM PythonDep p ", filter, sort, ":project MEMBER OF p.projectCollection ");
    String queryCountStr = buildQuery("SELECT COUNT(p.id) FROM PythonDep p ", filter, sort,
        ":project MEMBER OF p.projectCollection ");
    Query query = em.createQuery(queryStr, PythonDep.class).setParameter("project", project);
    Query queryCount = em.createQuery(queryCountStr, PythonDep.class).setParameter("project", project);
    return findAll(offset, limit, filter, query, queryCount);
  }

  private CollectionInfo findAll(Integer offset, Integer limit, Set<? extends AbstractFacade.FilterBy> filter,
      Query query, Query queryCount) {
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
    switch (Filters.valueOf(filterBy.getValue())) {
      case PREINSTALLED:
        setPreinstall(filterBy, q);
        break;
      case STATUS:
      case STATUS_NEQ:
        setStatus(filterBy, q);
        break;
      case MACHINE_TYPE:
      case MACHINE_TYPE_NEQ:
        setMachineType(filterBy, q);
        break;
      default:
        break;
    }
  }
  
  private void setPreinstall(AbstractFacade.FilterBy filterBy, Query q) {
    String field = filterBy.getField();
    boolean val = getBooleanValue(filterBy.getParam());
    q.setParameter(field, val);
  }
  
  private void setStatus(AbstractFacade.FilterBy filterBy, Query q) {
    List<CondaStatus> status = getEnumValues(filterBy, CondaStatus.class);
    q.setParameter(filterBy.getField(), status);
  }

  private void setMachineType(AbstractFacade.FilterBy filterBy, Query q) {
    List<MachineType> machineTypes = getEnumValues(filterBy, MachineType.class);
    q.setParameter(filterBy.getField(), machineTypes);
  }
  
  public enum Sorts {
    ID("ID", "p.id ", "ASC"),
    DEPENDENCY("DEPENDENCY", "p.dependency ", "ASC"),
    STATUS("STATUS", "p.status ", "ASC");

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
    PREINSTALLED("PREINSTALLED", "p.preinstalled = :preinstalled ", "preinstalled", "1"),
    STATUS("STATUS", "p.status IN :status ", "status", "NEW"),
    STATUS_NEQ("STATUS_NEQ", "p.status NOT IN :status_neq ", "status_neq", "NEW"),
    MACHINE_TYPE("MACHINE_TYPE", "p.machineType IN :machineType ", "machineType", "ALL"),
    MACHINE_TYPE_NEQ("MACHINE_TYPE_NEQ", "p.machineType NOT IN :machineType_neq ", "machineType_neq", "CPU");

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
