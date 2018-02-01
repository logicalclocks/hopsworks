/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.project.service;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class ProjectServiceFacade extends AbstractFacade<ProjectServices> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ProjectServiceFacade() {
    super(ProjectServices.class);
  }

  public List<ProjectServiceEnum> findEnabledServicesForProject(Project project) {
    //TODO: why does this return String?
    Query q = em.createNamedQuery("ProjectServices.findServicesByProject",
            ProjectServiceEnum.class);
    q.setParameter("project", project);
    return q.getResultList();
  }

  //TODO: write tests for this
  public void persistServicesForProject(Project project,
          ProjectServiceEnum[] services) {
    //TODO: use copy instead
    List<ProjectServices> newSrvs = new ArrayList<>(services.length);
    List<ProjectServices> toPersist = new ArrayList<>(services.length);
    for (ProjectServiceEnum sse : services) {
      ProjectServices c = new ProjectServices(project, sse);
      newSrvs.add(c);
      toPersist.add(c);
    }
    List<ProjectServices> current = getAllProjectServicesForProject(project);

    toPersist.removeAll(current);
    current.removeAll(newSrvs);

    for (ProjectServices se : toPersist) {
      em.persist(se);
    }
    for (ProjectServices se : current) {
      em.remove(se);
    }
  }

  public void addServiceForProject(Project project, ProjectServiceEnum service) {
    if (!findEnabledServicesForProject(project).contains(service)) {
      ProjectServices ss = new ProjectServices(project, service);
      em.persist(ss);
    }
  }

  public void removeServiceForProject(Project project,
          ProjectServiceEnum service) {
    ProjectServices c = em.find(ProjectServices.class, new ProjectServicePK(
            project.getId(),
            service));
    if (c != null) {
      em.remove(c);
    }
  }

  public List<ProjectServices> getAllProjectServicesForProject(Project project) {
    Query q = em.createNamedQuery("ProjectServices.findByProject",
            ProjectServices.class);
    q.setParameter("project", project);
    return q.getResultList();
  }

  public boolean isServiceEnabledForProject(Project project, ProjectServiceEnum service){
    Query q = em.createNamedQuery("ProjectServices.isServiceEnabledForProject",
        ProjectServices.class);
    q.setParameter("project", project);
    q.setParameter("service", service);
    return !(q.getResultList().isEmpty());
  }
}
