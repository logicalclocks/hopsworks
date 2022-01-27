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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@Entity
@Table(name = "git_repository_remotes", catalog = "hopsworks")
@NamedQueries({
    @NamedQuery(name = "GitRepositoryRemote.findAllInRepository",
        query
            = "SELECT r FROM GitRepositoryRemote r WHERE r.repository = :repository"),
    @NamedQuery(name = "GitRepositoryRemote.findById",
        query
            = "SELECT r FROM GitRepositoryRemote r WHERE r.id = :id"),
    @NamedQuery(name = "GitRepositoryRemote.findByNameAndRepository",
        query
            = "SELECT r FROM GitRepositoryRemote r WHERE r.remoteName = :name AND r.repository = :repository"),
    @NamedQuery(name = "GitRepositoryRemote.deleteAllInRepository",
        query
            = "DELETE FROM GitRepositoryRemote r WHERE r.repository = :repository"),
    @NamedQuery(name = "GitRepositoryRemote.delete",
        query
            = "DELETE FROM GitRepositoryRemote r WHERE r.id = :id AND r.repository = :repository")})
@XmlRootElement
public class GitRepositoryRemote implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @JoinColumn(name = "repository",
      referencedColumnName = "id")
  @ManyToOne(optional = false)
  @NotNull
  private GitRepository repository;

  @Basic(optional = false)
  @NotNull
  @Size(max = 250)
  @Column(name = "remote_name")
  private String remoteName;

  @Basic(optional = false)
  @NotNull
  @Size(max = 1000)
  @Column(name = "remote_url")
  private String url;

  public GitRepositoryRemote() {}

  public GitRepositoryRemote(GitRepository repository, String remoteName, String url) {
    this.repository = repository;
    this.remoteName = remoteName;
    this.url = url;
  }

  public Integer getId() { return id; }

  public void setId(Integer id) { this.id = id; }

  public GitRepository getRepository() {
    return repository;
  }

  public void setRepository(GitRepository repository) { this.repository = repository; }

  public String getRemoteName() { return remoteName; }

  public void setRemoteName(String remoteName) { this.remoteName = remoteName; }

  public String getUrl() { return url; }

  public void setUrl(String url) { this.url = url; }
}
