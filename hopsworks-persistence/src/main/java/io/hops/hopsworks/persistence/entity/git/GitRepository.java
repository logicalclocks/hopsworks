/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.git;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.hops.hopsworks.persistence.entity.git.config.GitProvider;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
@Entity
@Table(name = "git_repositories", catalog = "hopsworks")
@NamedQueries({
    @NamedQuery(name = "GitRepository.findAllInProject",
        query
            = "SELECT r FROM GitRepository r WHERE r.project = :project"),
    @NamedQuery(name = "GitRepository.findById",
        query
            = "SELECT r FROM GitRepository r WHERE r.id = :id"),
    @NamedQuery(name = "GitRepository.findByInode",
        query
            = "SELECT r FROM GitRepository r WHERE r.inode = :inode"),
    @NamedQuery(name = "GitRepository.findAllWithRunningOperation",
        query
            = "SELECT r FROM GitRepository r WHERE r.cid IS NOT NULL"),
    @NamedQuery(name = "GitRepository.findByIdAndProject",
        query
            = "SELECT r FROM GitRepository r WHERE r.id = :id AND r.project = :project")})
public class GitRepository implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @JoinColumns({
      @JoinColumn(name = "inode_pid",
          referencedColumnName = "parent_id")
      ,
      @JoinColumn(name = "inode_name",
          referencedColumnName = "name")
      ,
      @JoinColumn(name = "partition_id",
          referencedColumnName = "partition_id")})
  @ManyToOne(optional = false)
  private Inode inode;

  @Basic(optional = false)
  @Column(name = "inode_name",
      updatable = false,
      insertable = false)
  private String name;

  @JoinColumn(name = "project",
      referencedColumnName = "id")
  @ManyToOne(optional = false)
  @NotNull
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private Project project;

  @Basic(optional = false)
  @NotNull
  @Column(name = "provider")
  @Enumerated(EnumType.STRING)
  private GitProvider gitProvider;

  @Size(max = 255)
  @Column(name = "current_branch")
  private String currentBranch;

  @Size(max = 255)
  @Column(name = "current_commit")
  private String currentCommit;

  @Column(name = "cid")
  private String cid;

  @JoinColumn(name = "creator",
      referencedColumnName = "uid")
  @ManyToOne(optional = false)
  @NotNull
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private Users creator;

  public GitRepository() {}

  public GitRepository(Inode inode, Project project, GitProvider gitProvider, Users creator) {
    this.inode = inode;
    this.project = project;
    this.gitProvider = gitProvider;
    this.creator = creator;
    this.name = inode.getInodePK().getName();
  }

  public Integer getId() { return id; }

  public void setId(Integer id) { this.id = id; }

  public Inode getInode() { return inode; }

  public Project getProject() { return project; }

  public void setProject(Project project) { this.project = project; }

  public void setInode(Inode inode) { this.inode = inode; }

  public GitProvider getGitProvider() { return gitProvider; }

  public void setGitProvider(GitProvider gitProvider) { this.gitProvider = gitProvider; }

  public String getCid() { return cid; }

  public void setCid(String pid){ this.cid = pid; }

  public String getCurrentBranch() { return currentBranch; }

  public void setCurrentBranch(String currentBranch) { this.currentBranch = currentBranch; }

  public String getCurrentCommit() { return currentCommit; }

  public void setCurrentCommit(String currentHead) { this.currentCommit = currentHead; }

  public Users getCreator() { return creator; }

  public void setCreator(Users creator) { this.creator = creator; }

  public String getName() { return name; }

  public void setName(String name) { this.name = name; }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // won't work in the case the id fields are not set
    if (!(object instanceof GitOpExecution)) {
      return false;
    }
    GitRepository other = (GitRepository) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
        equals(other.id))) {
      return false;
    }
    return true;
  }
}
