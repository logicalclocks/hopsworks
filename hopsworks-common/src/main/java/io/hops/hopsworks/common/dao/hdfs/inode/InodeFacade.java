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

package io.hops.hopsworks.common.dao.hdfs.inode;

import io.hops.common.Pair;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class InodeFacade extends AbstractFacade<Inode> {

  private static final Logger logger = Logger.getLogger(InodeFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public InodeFacade() {
    super(Inode.class);
  }

  /**
   * Find all the Inodes that have <i>parent</i> as parent.
   * <p/>
   * @param parent
   * @return
   */
  public List<Inode> findByParent(Inode parent) {
    TypedQuery<Inode> query = em.createNamedQuery("Inode.findByParentId",
            Inode.class);
    query.setParameter("parentId", parent.getId());
    return query.getResultList();
  }

  /**
   * Find all the Inodes that have <i>userId</i> as userId.
   * <p/>
   * @return
   */
  public List<Inode> findByHdfsUser(HdfsUsers hdfsUser) {
    TypedQuery<Inode> query = em.createNamedQuery("Inode.findByHdfsUser",
            Inode.class);
    query.setParameter("hdfsUser", hdfsUser);
    return query.getResultList();
  }

  /**
   * Get all the children of <i>parent</i>. Alias of findByParent().
   * <p/>
   * @param parent
   * @return
   */
  public List<Inode> getChildren(Inode parent) {
    return findByParent(parent);
  }

  /**
   * Get all the children of <i>parent</i>. Alias of findByParent().
   * <p/>
   * @param parent
   * @param children
   * @return
   */
  public void getAllChildren(Inode parent, List<Inode> children) {
    List<Inode> curr = findByParent(parent);
    children.addAll(curr);

    for (Inode inode : curr) {
      if (inode.isDir()) {
        getAllChildren(inode, children);
      }
    }
  }

  /**
   * Return the size of an inode
   *
   * @param inode
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public long getSize(Inode inode) {
    if (!inode.isDir()) {
      return inode.getSize();
    }
    long size = 0;
    List<Inode> children = getChildren(inode);
    for (Inode i : children) {
      if (!i.isDir()) {
        size += i.getSize();
      } else {
        size += getSize(i);
      }
    }
    return size;
  }

  /**
   * Get a list of the names of the child files (so no directories) of the given
   * path.
   * <p/>
   * @param path
   * @return A list of filenames, empty if the given path does not have
   * children.
   */
  public List<String> getChildNames(String path) {
    Inode inode = getInodeAtPath(path);
    if (inode.isDir()) {
      List<Inode> inodekids = getChildren(inode);
      ArrayList<String> retList = new ArrayList<>(inodekids.size());
      for (Inode i : inodekids) {
        if (!i.isDir()) {
          retList.add(i.getInodePK().getName());
        }
      }
      return retList;
    } else {
      return Collections.EMPTY_LIST;
    }
  }

  /**
   * Find the parent of the given Inode. If the Inode has no parent, null is
   * returned.
   * <p/>
   * @param i
   * @return The parent, or null if no parent.
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Inode findParent(Inode i) {
    if(i == null){
      throw new IllegalArgumentException("Inode must be provided.");
    }
    int id = i.getInodePK().getParentId();
    TypedQuery<Inode> q = em.createNamedQuery("Inode.findById", Inode.class);
    q.setParameter("id", id);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * find inode by id
   * <p/>
   * @param id
   * @return
   */
  public Inode findById(Integer id) {
    TypedQuery<Inode> q = em.createNamedQuery("Inode.findById", Inode.class);
    q.setParameter("id", id);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   *
   * @param path
   * @return null if no such Inode found
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  private Inode getInode(String path) {
    // Get the path components
    String[] p;
    if (path.charAt(0) == '/') {
      p = path.substring(1).split("/");
    } else {
      p = path.split("/");
    }

    if (p.length < 1) {
      return null;
    }

    //Get the right root node
    Inode curr = getRootNode(p[0]);
    if (curr == null) {
      logger.log(Level.WARNING, "Could not resolve root inode at path: {0}",
              path);
      return null;
    }
    //Move down the path
    for (int i = 1; i < p.length; i++) {
      int partitionId = HopsUtils.
              calculatePartitionId(curr.getId(), p[i], i + 1);
      Inode next = findByInodePK(curr, p[i], partitionId);
      if (next == null) {
        logger.log(Level.WARNING,
                "Could not resolve inode at path: {0} and path-component " + i,
                path);
        return null;
      } else {
        curr = next;
      }
    }
    return curr;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private Inode getRootNode(String name) {
    int partitionId = HopsUtils.calculatePartitionId(HopsUtils.ROOT_INODE_ID, name, HopsUtils.ROOT_DIR_DEPTH + 1);
    TypedQuery<Inode> query = em.createNamedQuery("Inode.findRootByName",
            Inode.class);
    query.setParameter("name", name);
    query.setParameter("parentId", HopsUtils.ROOT_INODE_ID);
    query.setParameter("partitionId", partitionId);
    try {
      //Sure to give a single result because all children of same parent "null" 
      //so name is unique
      return query.getSingleResult();
    } catch (NoResultException e) {
      logger.log(Level.WARNING,
              "Could not resolve root inode with name: {0} and partition_id"
              + partitionId, name);
      return null;
    }
  }

  /**
   * Check whether the given path exists.
   * <p/>
   * @param path The path to search for.
   * @return True if the path exist (i.e. there is an Inode on this path), false
   * otherwise.
   */
  public boolean existsPath(String path) {
    return getInode(path) != null;
  }

  /**
   * Get the Inode at the specified path.
   * <p/>
   * @param path
   * @return Null if path does not exist.
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public Inode getInodeAtPath(String path) {
    return getInode(path);
  }

  /**
   * Get the Inode representing the project root directory of the project with
   * given name.
   * <p/>
   * @param name
   * @return The sought for Inode, or null if this Inode does not exist.
   */
  public Inode getProjectRoot(String name) {
    return getInode("/" + Settings.DIR_ROOT + "/" + name);
  }

  /**
   * Find an Inode by its parent Inode and its name (i.e. its primary key).
   * <p/>
   * @param parent
   * @param name
   * @param partitionId
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Inode findByInodePK(Inode parent, String name, int partitionId) {

    TypedQuery<Inode> q = em.createNamedQuery("Inode.findByPrimaryKey",
            Inode.class);
    q.setParameter("inodePk", new InodePK(parent.getId(), name, partitionId));
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Get the project base directory of which the given Inode is a descendant.
   * <p/>
   * @param i
   * @return The Inode representing the project root directory.
   * @throws IllegalStateException when the given Inode is not under a project
   * root directory.
   */
  public Inode getProjectRootForInode(Inode i) throws IllegalStateException {
    if (isProjectRoot(i)) {
      return i;
    } else {
      Inode parent = findParent(i);
      if (parent == null) {
        throw new IllegalStateException(
                "Transversing the path from folder did not encounter project root folder.");
      }
      return getProjectRootForInode(parent);
    }
  }

  /**
   * Find out if an Inode is a project root directory.
   * <p/>
   * @param i
   * @return
   */
  public boolean isProjectRoot(Inode i) {
    Inode parent = findParent(i);
    if (!parent.getInodePK().getName().equals(
            Settings.DIR_ROOT)) {
      return false;
    } else {
      //A node is the project root if its parent has the name $DIR_ROOT and its 
      //grandparent is the root node
      return parent.getInodePK().getParentId() == 1;
    }
  }

  /**
   * Get the name of the project of which this Inode is a descendant.
   * <p/>
   * @param i
   * @return
   * @throws IllegalStateException When the given Inode is not a descendant of
   * any project.
   */
  public String getProjectNameForInode(Inode i) throws IllegalStateException {
    Inode projectRoot = getProjectRootForInode(i);
    return projectRoot.getInodePK().getName();
  }

  /**
   * Get a list of NavigationPath objects representing the project-relative path
   * to the given Inode. The first element in the list is the project root
   * directory.
   * <p/>
   * @param i
   * @return
   */
  public List<NavigationPath> getConstituentsPath(Inode i) {
    if (isProjectRoot(i)) {
      List<NavigationPath> p = new ArrayList<>();
      p.add(new NavigationPath(i.getInodePK().getName(), i.getInodePK().
              getName() + "/"));
      return p;
    } else {
      List<NavigationPath> p = getConstituentsPath(findParent(i));
      NavigationPath a;
      if (i.isDir()) {
        a = new NavigationPath(i.getInodePK().getName(), p.get(p.size() - 1).
                getPath() + i.getInodePK().getName() + "/");
      } else {
        a = new NavigationPath(i.getInodePK().getName(), p.get(p.size() - 1).
                getPath() + i.getInodePK().getName());
      }
      p.add(a);
      return p;
    }
  }

  /**
   * Get the path to the given Inode.
   * <p/>
   * @param i
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public String getPath(Inode i) {
    if(i == null) {
      throw new IllegalArgumentException("Inode was not provided.");
    }
    List<String> pathComponents = new ArrayList<>();
    Inode parent = i;
    while (parent.getId() != 1) {
      pathComponents.add(parent.getInodePK().getName());
      parent = findParent(parent);
    }
    StringBuilder path = new StringBuilder();
    for (int j = pathComponents.size() - 1; j >= 0; j--) {
      path.append("/").append(pathComponents.get(j));
    }
    return path.toString();
  }

  /**
   * Get the inodes in the directory pointed to by the given absolute HDFS path.
   * <p/>
   * @param path
   * @return
   * @throws IllegalArgumentException If the path does not point to a directory.
   * @throws java.io.FileNotFoundException If the path does not exist.
   */
  public List<Inode> getChildren(String path) throws
          FileNotFoundException {
    Inode parent = getInode(path);
    if (parent == null) {
      throw new FileNotFoundException("Path not found : " + path);
    } else if (!parent.isDir()) {
      throw new FileNotFoundException("Path is not a directory.");
    }
    return getChildren(parent);
  }

  /**
   * Get the project and dataset base directory of which the given Inode is a
   * descendant.
   * <p/>
   * @param i
   * @return The Inodes representing the project and dataset root directories.
   * [ProjectInode, DatasetInode]
   * @throws IllegalStateException when the given Inode is not under a project
   * root directory.
   */
  public Pair<Inode, Inode> getProjectAndDatasetRootForInode(Inode i) throws
          IllegalStateException {
    Inode project = i;
    Inode dataset = i;
    do {
      dataset = project;
      project = findParent(project);
      if (project == null) {
        throw new IllegalStateException(
                "Transversing the path from folder did not encounter project root folder.");
      }
    } while (!isProjectRoot(project));
    return new Pair<>(project, dataset);
  }

  /**
   * Find all the Inodes that have <i>userId</i> as userId and correspond to an
   * history file.
   * <p/>
   * @return
   */
  public List<Inode> findHistoryFileByHdfsUser(HdfsUsers hdfsUser) {
    TypedQuery<Inode> query = em.createNamedQuery(
            "Inode.findHistoryFileByHdfsUser",
            Inode.class);
    query.setParameter("hdfsUser", hdfsUser);
    return query.getResultList();
  }
}
