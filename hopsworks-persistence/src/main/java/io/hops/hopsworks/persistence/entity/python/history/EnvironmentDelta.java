/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.python.history;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.json.JSONArray;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Convert;
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
@Table(name = "environment_history",
    catalog = "hopsworks",
    schema = "")
@NamedQueries({
    @NamedQuery(name = "EnvironmentDelta.findAllForProject",
        query
            = "SELECT e FROM EnvironmentDelta e WHERE e.project = :project ORDER BY e.id DESC"),
    @NamedQuery(name = "EnvironmentDelta.findById",
        query
            = "SELECT e FROM EnvironmentDelta e WHERE e.id = :id AND e.project = :project "),
    @NamedQuery(name = "EnvironmentDelta.deleteAllForProject",
        query
            = "DELETE FROM EnvironmentDelta e WHERE e.project = :project")})
public class EnvironmentDelta implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @JoinColumn(name = "project",
      referencedColumnName = "id")
  @ManyToOne(optional = false)
  @NotNull
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private Project project;

  @Size(max = 255)
  @Column(name = "docker_image")
  @Basic(optional = false)
  private String dockerImage;

  @Size(max = 255)
  @Column(name = "previous_docker_image")
  @NotNull
  @Basic(optional = false)
  private String previousDockerImage;

  @Column(name = "installed")
  @Convert(converter = JSONArrayStringConverter.class)
  @SuppressFBWarnings(justification="Converter", value="SE_BAD_FIELD")
  private JSONArray installed;

  @Column(name = "uninstalled")
  @Convert(converter = JSONArrayStringConverter.class)
  @SuppressFBWarnings(justification="Converter", value="SE_BAD_FIELD")
  private JSONArray uninstalled;

  @Column(name = "upgraded")
  @Convert(converter = JSONArrayStringConverter.class)
  @SuppressFBWarnings(justification="Converter", value="SE_BAD_FIELD")
  private JSONArray upgraded;

  @Column(name = "downgraded")
  @Convert(converter = JSONArrayStringConverter.class)
  @SuppressFBWarnings(justification="Converter", value="SE_BAD_FIELD")
  private JSONArray downgraded;

  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @JoinColumn(name = "user", referencedColumnName = "uid")
  @ManyToOne(optional = false)
  private Users user;

  public EnvironmentDelta() {}

  public EnvironmentDelta(Project project, Users user, String dockerImage, Date created) {
    this.dockerImage = dockerImage;
    this.created = created;
    this.project = project;
    this.user = user;
  }

  public Integer getId() { return id; }

  public void setId(Integer id) { this.id = id; }

  public String getDockerImage() { return dockerImage; }

  public void setDockerImage(String dockerImage) { this.dockerImage = dockerImage; }

  public JSONArray getUpgraded() { return upgraded; }

  public void setUpgraded(JSONArray upgraded) { this.upgraded = upgraded; }

  public JSONArray getDowngraded() { return downgraded; }

  public void setDowngraded(JSONArray downgraded) { this.downgraded = downgraded; }

  public JSONArray getInstalled() { return installed; }

  public void setInstalled(JSONArray installed) { this.installed = installed; }

  public JSONArray getUninstalled() { return uninstalled; }

  public void setUninstalled(JSONArray removed) { this.uninstalled = removed; }

  public Project getProject() { return project; }

  public void setProject(Project project) { this.project = project; }

  public String getPreviousDockerImage() { return previousDockerImage; }

  public void setPreviousDockerImage(String previous_docker_image) {
    this.previousDockerImage = previous_docker_image;
  }

  public Date getCreated() { return created; }

  public void setCreated(Date created) { this.created = created; }

  public Users getUser() { return user; }

  public void setUser(Users userId) { this.user = userId; }
}
