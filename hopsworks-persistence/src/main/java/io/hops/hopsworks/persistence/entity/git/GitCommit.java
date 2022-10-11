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

import com.fasterxml.jackson.annotation.JsonFormat;

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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "git_commits", catalog = "hopsworks")
@NamedQueries({
    @NamedQuery(name = "GitCommit.findAllForBranchAndRepository",
        query
            = "SELECT gc FROM GitCommit gc WHERE gc.branch = :branch AND gc.repository = :repository ORDER BY gc.id"),
    @NamedQuery(name = "GitCommit.findById",
        query
            = "SELECT gc FROM GitCommit gc WHERE gc.id = :id"),
    @NamedQuery(name = "GitCommit.findByCommitHashAndRepository",
        query
            = "SELECT gc FROM GitCommit gc WHERE gc.repository = :repository AND gc.hash = :hash"),
    @NamedQuery(name = "GitCommit.deleteAllForBranchAndRepository",
        query
            = "DELETE FROM GitCommit gc WHERE gc.branch = :branch AND gc.repository = :repository"),
    @NamedQuery(name = "GitCommit.findBranchesForRepository",
        query
            = "SELECT DISTINCT gc.branch FROM GitCommit gc WHERE gc.repository = :repository ORDER BY gc.date DESC")})
public class GitCommit implements Serializable {
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
  @Size(max = 255)
  @Column(name = "branch")
  private String branch;

  @Basic(optional = false)
  @NotNull
  @Size(max = 1000)
  @Column(name = "committer_name")
  private String name;

  @Basic(optional = false)
  @NotNull
  @Size(max = 1000)
  @Column(name = "committer_email")
  private String email;

  @Basic(optional = false)
  @NotNull
  @Size(max = 255)
  @Column(name = "hash")
  private String hash;

  @Basic(optional = false)
  @NotNull
  @Size(max = 1000)
  @Column(name = "message")
  private String message;

  @Column(name = "date")
  @Temporal(TemporalType.TIMESTAMP)
  // Date format from hops-git is RFC3339
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
  private Date date;

  public GitCommit() {}

  public GitCommit(GitRepository repository, String branch, String name, String email, String hash, String message,
                   Date date) {
    this.repository = repository;
    this.branch = branch;
    this.name = name;
    this.email = email;
    this.hash = hash;
    this.message = message;
    this.date = date;
  }

  public Integer getId() {  return id; }

  public void setId(Integer id) { this.id = id; }

  public GitRepository getRepository() { return repository; }

  public void setRepository(GitRepository repository) { this.repository = repository; }

  public String getBranch() { return branch; }

  public void setBranch(String branch) { this.branch = branch; }

  public String getName() { return name; }

  public void setName(String name) { this.name = name; }

  public String getEmail() { return email; }

  public void setEmail(String email) { this.email = email; }

  public String getHash() { return hash; }

  public void setHash(String hash) { this.hash = hash; }

  public String getMessage() { return message; }

  public void setMessage(String message) { this.message = message; }

  public Date getDate() { return date; }

  public void setDate(Date date) { this.date = date; }
}
