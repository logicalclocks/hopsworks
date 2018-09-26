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

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.project.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class DatasetFacade extends AbstractFacade<Dataset> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public DatasetFacade() {
    super(Dataset.class);
  }

  @Override
  public List<Dataset> findAll() {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findAll",
      Dataset.class);
    return query.getResultList();
  }

  /**
   * Finds a dataset by id
   * <p/>
   * @param id
   * @return
   */
  public Dataset find(Integer id) {
    return em.find(Dataset.class, id);
  }

  /**
   * Finds all instances of a dataset. i.e if a dataset is shared it is going
   * to be present in the parent project and in the project it is shared with.
   * <p/>
   * @param inode
   * @return
   */
  public List<Dataset> findByInode(Inode inode) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findByInode",
      Dataset.class).setParameter(
        "inode", inode);
    return query.getResultList();
  }

  public List<Dataset> findByInodeId(int inodeId) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findByInodeId",
      Dataset.class).setParameter(
        "inodeId", inodeId);
    return query.getResultList();
  }

  public Optional<Dataset> findByPublicDsIdProject(String publicDsId, Project project) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findByPublicDsIdProject", Dataset.class)
      .setParameter("publicDsId", publicDsId)
      .setParameter("project", project);
    try {
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public Optional<Dataset> findByPublicDsId(String publicDsId) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findByPublicDsId", Dataset.class)
      .setParameter("publicDsId", publicDsId);
    try {
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public Dataset findByNameAndProjectId(Project project, String name) {
    TypedQuery<Dataset> query = em.createNamedQuery(
      "Dataset.findByNameAndProjectId",
      Dataset.class);
    query.setParameter("name", name).setParameter("project", project);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public List<Project> findProjectSharedWith(Project project, Inode inode) {
    List<Dataset> datasets = findByInode(inode);
    if (datasets == null){
      return null;
    }

    List<Project> projects = new ArrayList<>();
    for (Dataset ds : datasets) {
      if (!ds.getProject().equals(project)) {
        projects.add(ds.getProject());
      }
    }
    return projects;
  }

  /**
   * Find by project and dataset name
   * <p/>
   * @param project
   * @param inode
   * @return
   */
  public Dataset findByProjectAndInode(Project project, Inode inode) {
    if(project == null || inode == null){
      throw new IllegalArgumentException("Project and/or inode were not provided.");
    }
    try {
      return em.createNamedQuery("Dataset.findByProjectAndInode", Dataset.class)
        .setParameter("projectId", project).setParameter(
          "inode", inode).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Finds all data sets in a project.
   * <p/>
   * @param project
   * @return
   */
  public List<Dataset> findByProject(Project project) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findByProject",
      Dataset.class).setParameter(
        "projectId", project);
    return query.getResultList();
  }

  public List<DataSetDTO> findPublicDatasets() {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findAllPublic",
      Dataset.class);
    List<Dataset> datasets = query.getResultList();

    List<DataSetDTO> ds = new ArrayList<>();
    for (Dataset d : datasets) {
      DataSetDTO dto = new DataSetDTO();
      dto.setDescription(d.getDescription());
      dto.setName(d.getInode().getInodePK().getName());
      dto.setInodeId(d.getInode().getId());
      ds.add(dto);
    }
    return ds;
  }
  
  public List<Dataset> findAllPublicDatasets() {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findAllPublic", Dataset.class);
    return query.getResultList();   
  }
  
  public List<Dataset> findAllDatasetsByState(int state, boolean shared) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findAllByState", Dataset.class)
      .setParameter("state", state)
      .setParameter("shared", shared);
    return query.getResultList();   
  }

  /**
   * Finds all data sets shared with a project.
   * <p/>
   * @param project
   * @return
   */
  public List<Dataset> findSharedWithProject(Project project) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findSharedWithProject", Dataset.class).setParameter(
      "projectId", project);
    return query.getResultList();
  }

  public void persistDataset(Dataset dataset) {
    em.persist(dataset);
  }

  public void flushEm() {
    em.flush();
  }

  public void merge(Dataset dataset) {
    em.merge(dataset);
    em.flush();
  }

  public void removeDataset(Dataset dataset) {
    Dataset ds = em.find(Dataset.class, dataset.getId());
    if (ds != null) {
      em.remove(ds);
    }
  }
}
