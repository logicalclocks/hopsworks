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

package io.hops.hopsworks.common.dao.jupyter.config;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterProject;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class JupyterFacade extends AbstractFacade<JupyterProject> {

  private static final Logger LOGGER = Logger.getLogger(JupyterFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public JupyterFacade() {super(JupyterProject.class);}

  protected EntityManager getEntityManager() {
    return em;
  }

  public Optional<JupyterProject> findByProjectUser(Project project, Users user) {
    try {
      return Optional.of(em.createNamedQuery("JupyterProject.findByProjectUser", JupyterProject.class)
          .setParameter("project", project)
          .setParameter("user", user)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public void remove(Project project, Users user) {
    try {
      JupyterProject jupyterProject = em.createNamedQuery("JupyterProject.findByProjectUser", JupyterProject.class)
          .setParameter("project", project)
          .setParameter("user", user)
          .getSingleResult();
      em.remove(jupyterProject);
    } catch (NoResultException e) {
    }
  }

  public List<JupyterProject> getAllNotebookServers() {
    List<JupyterProject> res = null;
    TypedQuery<JupyterProject> query = em.createNamedQuery(
        "JupyterProject.findAll", JupyterProject.class);
    try {
      res = query.getResultList();
    } catch (EntityNotFoundException | NoResultException e) {
      LOGGER.log(Level.FINE, null, e);
      return null;
    }
    return res;
  }

  public JupyterProject saveServer(Project project, Users user, String secretConfig, int port,
                                   String token, String cid, Date expires, boolean noLimit) {
    JupyterProject jp = new JupyterProject(project, user, secretConfig, port, token, cid, expires, noLimit);
    persist(jp);
    return jp;
  }

  private void persist(JupyterProject jp) {
    if (jp != null) {
      em.persist(jp);
    }
  }
}
