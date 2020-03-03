/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.hdfs.inode;

import io.hops.common.Pair;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.NavigationPath;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class InodeController {
  
  private static final Logger LOGGER = Logger.getLogger(InodeController.class.getName());
  @EJB
  private InodeFacade inodeFacade;
  /**
   * Get all the children of <i>parent</i>. Alias of findByParent().
   * <p/>
   * @param parent
   * @param children
   * @return
   */
  public void getAllChildren(Inode parent, List<Inode> children) {
    List<Inode> curr = inodeFacade.findByParent(parent);
    children.addAll(curr);
    
    for (Inode inode : curr) {
      if (inode.isDir()) {
        getAllChildren(inode, children);
      }
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
  public Inode getInodeAtPath(String path) {
    return getInode(path);
  }
  
  public Inode getInodeAtPath(Inode inode, int depth, String path) {
    return getInode(inode, depth, path.split(File.separator));
  }
  
  /**
   * Get the Inode representing the project root directory of the project with
   * given name.
   * <p/>
   * @param name
   * @return The sought for Inode, or null if this Inode does not exist.
   */
  public Inode getProjectRoot(String name) {
    return getInode(Utils.getProjectPath(name));
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
      Inode parent = inodeFacade.findParent(i);
      if (parent == null) {
        throw new IllegalStateException("Transversing the path from folder did not encounter project root folder.");
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
    Inode parent = inodeFacade.findParent(i);
    if (!parent.getInodePK().getName().equals(Settings.DIR_ROOT)) {
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
      p.add(new NavigationPath(i.getInodePK().getName(), i.getInodePK().getName() + "/"));
      return p;
    } else {
      List<NavigationPath> p = getConstituentsPath(inodeFacade.findParent(i));
      NavigationPath a;
      if (i.isDir()) {
        a = new NavigationPath(i.getInodePK().getName(), p.get(p.size() - 1).getPath() + i.getInodePK().getName()
          + "/");
      } else {
        a = new NavigationPath(i.getInodePK().getName(), p.get(p.size() - 1).getPath() + i.getInodePK().getName());
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
  public String getPath(Inode i) {
    if(i == null) {
      throw new IllegalArgumentException("Inode was not provided.");
    }
    List<String> pathComponents = new ArrayList<>();
    Inode parent = i;
    while (parent.getId() != 1) {
      pathComponents.add(parent.getInodePK().getName());
      parent = inodeFacade.findParent(parent);
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
   * Get all the children of <i>parent</i>. Alias of findByParent().
   * <p/>
   * @param parent
   * @return
   */
  public List<Inode> getChildren(Inode parent) {
    return inodeFacade.findByParent(parent);
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
      project = inodeFacade.findParent(project);
      if (project == null) {
        throw new IllegalStateException("Transversing the path from folder did not encounter project root folder.");
      }
    } while (!isProjectRoot(project));
    return new Pair<>(project, dataset);
  }
  
  /**
   *
   * @param inode
   * @return
   */
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
    return getInode(inodeFacade.getRootNode(p[0]), 1, Arrays.copyOfRange(p, 1, p.length));
  }
  
  private Inode getInode(Inode inode, int depth, String[] p) {
    //Get the right root node
    Inode curr = inode;
    if (curr == null) {
      return null;
    }
    //Move down the path
    for (int i = 0; i < p.length; i++) {
      long partitionId = HopsUtils.calculatePartitionId(curr.getId(), p[i], i + depth + 1);
      Inode next = inodeFacade.findByInodePK(curr, p[i], partitionId);
      if (next == null) {
        return null;
      } else {
        curr = next;
      }
    }
    return curr;
  }
  
}
