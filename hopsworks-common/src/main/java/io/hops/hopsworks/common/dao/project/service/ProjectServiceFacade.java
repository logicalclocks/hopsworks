/*
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
 *
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
