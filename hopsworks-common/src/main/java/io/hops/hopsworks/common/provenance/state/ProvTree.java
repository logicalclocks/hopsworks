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
package io.hops.hopsworks.common.provenance.state;

import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.exceptions.ProvenanceException;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

public class ProvTree {
  public interface State {
    Long getInodeId();
    String getInodeName();
    Long getProjectInodeId();
    boolean isProject();
    Long getParentInodeId();
  }
  
  public interface Builder<S extends State> {
    void setInodeId(Long inodeId);
    Long getInodeId();
    void setName(String name);
    String getName();
    void setFileState(S fileState);
    S getFileState();
    void addChild(Builder<S> child) throws ProvenanceException;
  }
  
  public static <S extends State> void merge(
    Builder<S> to, Builder<S> from) {
    if(to.getInodeId() == null) {
      to.setInodeId(from.getInodeId());
    }
    if(to.getName() == null) {
      to.setName(from.getName());
    }
    if(to.getFileState() == null) {
      to.setFileState(from.getFileState());
    }
  }
  
  public static class AuxStruct<S extends State> {
    Supplier<Builder<S>> instanceBuilder;
    
    ArrayList<Long> findInInodes = new ArrayList<>();
    ArrayList<Long> pendingInInodes = new ArrayList<>();
    ArrayList<Long> notFound = new ArrayList<>();
    TreeMap<Long, Builder<S>> allNodes = new TreeMap<>();
    TreeMap<Long, Builder<S>> incompleteNodes = new TreeMap<>();
    TreeMap<Long, Builder<S>> projectNodes = new TreeMap<>();
    
    public AuxStruct(Supplier<Builder<S>> instanceBuilder) {
      this.instanceBuilder = instanceBuilder;
    }
    
    public Pair<Map<Long, Builder<S>>,
      Map<Long, Builder<S>>> getMinTree() {
      return Pair.with(incompleteNodes, new HashMap<>());
    }
    
    public Pair<Map<Long, Builder<S>>,
      Map<Long, Builder<S>>> getFullTree() {
      return Pair.with(projectNodes, incompleteNodes);
    }
    
    public void processBasicFileState(List<S> fileStates) throws ProvenanceException {
      for (S fileState : fileStates) {
        if (fileState.isProject()) {
          Builder<S> projectNode = getOrBuildProjectNode(fileState);
          projectNode.setFileState(fileState);
        } else {
          Builder<S> parentNode = getOrBuildParentNode(fileState);
          Builder<S> node = getOrBuildNode(fileState);
          parentNode.addChild(node);
        }
      }
    }
    
    private Builder<S> getOrBuildProjectNode(S fileState) {
      Builder<S> projectNode = projectNodes.get(fileState.getProjectInodeId());
      if (projectNode == null) {
        projectNode = instanceBuilder.get();
        projectNode.setInodeId(fileState.getProjectInodeId());
        allNodes.put(projectNode.getInodeId(), projectNode);
        projectNodes.put(projectNode.getInodeId(), projectNode);
        findInInodes.add(projectNode.getInodeId());
      }
      return projectNode;
    }
    
    private Builder<S> getOrBuildParentNode(S fileState) {
      getOrBuildProjectNode(fileState);
      Builder<S> parentNode = getOrBuildParentNode(fileState.getParentInodeId());
      return parentNode;
    }
    
    private Builder<S> getOrBuildParentNode(Long parentInodeId) {
      Builder<S> parentNode = allNodes.get(parentInodeId);
      if (parentNode == null) {
        parentNode = instanceBuilder.get();
        parentNode.setInodeId(parentInodeId);
        allNodes.put(parentNode.getInodeId(), parentNode);
        if(!projectNodes.containsKey(parentNode.getInodeId())) {
          findInInodes.add(parentNode.getInodeId());
          incompleteNodes.put(parentNode.getInodeId(), parentNode);
        }
      }
      return parentNode;
    }
    
    private Builder<S> getOrBuildNode(S fileState) {
      incompleteNodes.remove(fileState.getInodeId());
      findInInodes.remove(fileState.getInodeId());
      Builder<S> node = allNodes.get(fileState.getInodeId());
      if (node == null) {
        node = instanceBuilder.get();
        node.setInodeId(fileState.getInodeId());
        allNodes.put(node.getInodeId(), node);
      }
      node.setName(fileState.getInodeName());
      node.setFileState(fileState);
      return node;
    }
    
    public boolean findInInodes() {
      return !findInInodes.isEmpty();
    }
    
    public boolean complete() {
      return findInInodes.isEmpty();
    }
    
    public List<Long> nextFindInInodes() {
      int batchSize = Math.min(100, findInInodes.size());
      List<Long> batch = new ArrayList<>(findInInodes.subList(0, batchSize));
      findInInodes.removeAll(batch);
      pendingInInodes.addAll(batch);
      return batch;
    }
    
    public void processInodeBatch(List<Long> inodeBatch, List<Inode> inodes) throws ProvenanceException {
      pendingInInodes.removeAll(inodeBatch);
      Set<Long> inodesNotFound = new HashSet<>(inodeBatch);
      for(Inode inode : inodes) {
        inodesNotFound.remove(inode.getId());
        Builder<S> node = incompleteNodes.remove(inode.getId());
        if(node != null) {
          node.setName(inode.getInodePK().getName());
          Builder<S> parentNode = getOrBuildParentNode(inode.getInodePK().getParentId());
          parentNode.addChild(node);
        } else {
          node = projectNodes.get(inode.getId());
          if(node != null) {
            node.setName(inode.getInodePK().getName());
          }
        }
      }
      notFound.addAll(inodesNotFound);
    }
  }
}
