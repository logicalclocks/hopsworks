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

import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class JupyterFacade {

  private static final Logger logger = Logger.getLogger(JupyterFacade.class.
      getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  private Settings settings;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;

  protected EntityManager getEntityManager() {
    return em;
  }

  public boolean removeNotebookServer(String hdfsUsername, int port) {

    if (hdfsUsername == null || hdfsUsername.isEmpty()) {
      return false;
    }

    JupyterProject jp = findByUserAndPort(hdfsUsername, port);
    if (jp == null) {
      return false;
    }
    try {
      em.remove(jp);
      em.flush();
    } catch (Exception ex) {
      logger.warning("Problem removing jupyter notebook entry from hopsworks DB");
      logger.warning(ex.getMessage());
      return false;
    }
    return true;
  }

  public JupyterProject findByUserAndPort(String hdfsUser, int port) {
    HdfsUsers res = null;
    TypedQuery<HdfsUsers> query = em.createNamedQuery(
        "HdfsUsers.findByName", HdfsUsers.class);
    query.setParameter("name", hdfsUser);
    try {
      res = query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
    TypedQuery<JupyterProject> jupyterProjectQuery = em.createNamedQuery(
        "JupyterProject.findByHdfsUserIdAndPort", JupyterProject.class);
    jupyterProjectQuery.setParameter("hdfsUserId", res.getId());
    jupyterProjectQuery.setParameter("port", port);
    try {
      return jupyterProjectQuery.getSingleResult();
    } catch (NoResultException e) {
      Logger.getLogger(JupyterFacade.class.getName()).log(Level.FINE, null,
          e);
    }
    return null;
  }

  public JupyterProject findByUser(String hdfsUser) {
    HdfsUsers res = null;
    TypedQuery<HdfsUsers> hdfsUsersQuery = em.createNamedQuery(
      "HdfsUsers.findByName", HdfsUsers.class);
    hdfsUsersQuery.setParameter("name", hdfsUser);
    try {
      res = hdfsUsersQuery.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
    TypedQuery<JupyterProject> jupyterProjectQuery = em.createNamedQuery(
      "JupyterProject.findByHdfsUserId", JupyterProject.class);
    jupyterProjectQuery.setParameter("hdfsUserId", res.getId());
    try {
      return jupyterProjectQuery.getSingleResult();
    } catch (NoResultException e) {
      Logger.getLogger(JupyterFacade.class.getName()).log(Level.FINE, null,
        e);
    }
    return null;
  }

  public List<JupyterProject> getAllNotebookServers() {
    List<JupyterProject> res = null;
    TypedQuery<JupyterProject> query = em.createNamedQuery(
        "JupyterProject.findAll", JupyterProject.class);
    try {
      res = query.getResultList();
    } catch (EntityNotFoundException | NoResultException e) {
      Logger.getLogger(JupyterFacade.class.getName()).log(Level.FINE, null,
          e);
      return null;
    }
    return res;
  }

  public void stopServers(Project project) {

    // delete JupyterProject entity bean
  }

  public JupyterProject saveServer(String host,
      Project project, String secretConfig, int port,
      int hdfsUserId, String token, long pid) {
    JupyterProject jp = null;
    String ip;
    ip = host + ":" + settings.getHopsworksPort();
    jp = new JupyterProject(project, secretConfig, port, hdfsUserId, ip, token,
        pid);

    persist(jp);
    return jp;
  }

  private void persist(JupyterProject jp) {
    if (jp != null) {
      em.persist(jp);
    }
  }

  public void update(JupyterProject jp) {
    if (jp != null) {
      em.merge(jp);
    }
  }

  private void remove(JupyterProject jp) {
    if (jp != null) {
      em.remove(jp);
    }
  }

  public void removeProject(Project project) {
    // Find any active jupyter servers

    Collection<JupyterProject> instances = project.getJupyterProjectCollection();
    if (instances != null) {
      for (JupyterProject jp : instances) {
        HdfsUsers hdfsUser = hdfsUsersFacade.find(jp.getHdfsUserId());
        if (hdfsUser != null) {
          String user = hdfsUser.getUsername();
        }
        remove(jp);
      }
    }
    // Kill any processes

  }

  public String getProjectPath(JupyterProject jp, String projectName,
      String hdfsUser) {
    return settings.getJupyterDir() + File.separator
        + Settings.DIR_ROOT + File.separator + projectName
        + File.separator + hdfsUser + File.separator + jp.getSecret();
  }
}
