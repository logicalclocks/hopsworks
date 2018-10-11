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

package io.hops.hopsworks.common.dao.dataset;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.message.Message;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class DatasetRequestFacade extends AbstractFacade<DatasetRequest> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public DatasetRequestFacade() {
    super(DatasetRequest.class);
  }

  @Override
  public List<DatasetRequest> findAll() {
    TypedQuery<DatasetRequest> query = em.createNamedQuery(
            "DatasetRequest.findAll",
            DatasetRequest.class);
    return query.getResultList();
  }

  /**
   * Finds a dataset by id
   * <p/>
   * @param id
   * @return
   */
  public DatasetRequest find(Integer id) {
    return em.find(DatasetRequest.class, id);
  }

  /**
   * Finds all requests made on a dataset
   * <p/>
   * @param ds
   * @return
   */
  public List<DatasetRequest> findByDataset(Dataset ds) {
    TypedQuery<DatasetRequest> query = em.createNamedQuery(
            "DatasetRequest.findByDataset",
            DatasetRequest.class).setParameter(
                    "dataset", ds);
    return query.getResultList();
  }

  /**
   * Find by project member and dataset
   * <p/>
   * @param projectTeam
   * @param ds
   * @return
   */
  public DatasetRequest findByProjectTeamAndDataset(ProjectTeam projectTeam,
          Dataset ds) {
    try {
      return em.createNamedQuery("DatasetRequest.findByProjectTeamAndDataset",
              DatasetRequest.class)
              .setParameter("projectTeam", projectTeam).setParameter(
              "dataset", ds).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Find by project and dataset
   * <p/>
   * @param project
   * @param ds
   * @return
   */
  public DatasetRequest findByProjectAndDataset(Project project, Dataset ds) {
    try {
      return em.createNamedQuery("DatasetRequest.findByProjectAndDataset",
              DatasetRequest.class)
              .setParameter("project", project).setParameter(
              "dataset", ds).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Finds all Requests by a project member.
   * <p/>
   * @param projectTeam
   * @return
   */
  public List<DatasetRequest> findByProjectTeam(ProjectTeam projectTeam) {
    TypedQuery<DatasetRequest> query = em.createNamedQuery(
            "DatasetRequest.findByProjectTeam",
            DatasetRequest.class).setParameter(
                    "projectTeam", projectTeam);
    return query.getResultList();
  }

  /**
   * Finds all Requests by a project member.
   * <p/>
   * @param message
   * @return
   */
  public DatasetRequest findByMessageId(Message message) {
    TypedQuery<DatasetRequest> query = em.createNamedQuery(
            "DatasetRequest.findByMessageId",
            DatasetRequest.class).setParameter(
                    "message_id", message.getId());
    List<DatasetRequest> results = query.getResultList();
    if (results != null && results.size() == 1) {
      return results.get(0);
    } else {
      return null;
    }
  }

  public void persistDataset(DatasetRequest datasetRequest) {
    em.persist(datasetRequest);
  }

  public void flushEm() {
    em.flush();
  }

  public void merge(DatasetRequest dataset) {
    em.merge(dataset);
  }

}
