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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.UserCardDTO;

@XmlRootElement
public class DataSetDTO {

  private Long inodeId;
  private String name;
  private String description;
  private boolean isPublic;
  private boolean searchable;
  private boolean generateReadme;
  private DatasetPermissions permissions;
  private int template;
  private Integer projectId;
  private List<Integer> projectIds;
  private String projectName;
  private String templateName;
  private List<UserCardDTO> projectTeam;
  private List<String> sharedWith;
  private DatasetType type;

  public DataSetDTO() {
  }

  public DataSetDTO(String name, String description, boolean searchable,
          int template, boolean generateReadme) {
    this.name = name;
    this.description = description;
    this.searchable = searchable;
    this.template = template;
    this.generateReadme = generateReadme;
  }

  public DataSetDTO(Dataset ds, Project project, List<String> sharedWith) {
    this.inodeId = ds.getInode().getId();
    this.name = ds.getInode().getInodePK().getName();
    this.description = ds.getDescription();
    this.projectName = project.getName();
    this.sharedWith = sharedWith;
    this.projectTeam = new ArrayList<>();
    this.isPublic = ds.isPublicDs();
    this.searchable = ds.isSearchable();
    //this have to be done because project team contains too much info.
    for (ProjectTeam member : project.getProjectTeamCollection()) {
      projectTeam.add(new UserCardDTO(member.getUser().getFname(), member.
              getUser().getLname(), member.getUser().getEmail()));
    }
    this.type = ds.getDsType();
  }

  public DataSetDTO(String name, Long inodeId, Project project) {
    this.inodeId = inodeId;
    this.name = name;
    this.projectName = project.getName();
    this.projectTeam = new ArrayList<>();
    //this have to be done because project team contains too much info.
    for (ProjectTeam member : project.getProjectTeamCollection()) {
      projectTeam.add(new UserCardDTO(member.getUser().getFname(), member.
              getUser().getLname(), member.getUser().getEmail()));
    }
  }
  
  public Long getInodeId() {
    return inodeId;
  }

  public void setInodeId(Long inodeId) {
    this.inodeId = inodeId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isSearchable() {
    return searchable;
  }

  public void setSearchable(boolean searchable) {
    this.searchable = searchable;
  }

  public int getTemplate() {
    return this.template;
  }

  public void setTemplate(int template) {
    this.template = template;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public List<Integer> getProjectIds() {
    return projectIds;
  }

  public void setProjectIds(List<Integer> projectIds) {
    this.projectIds = projectIds;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getTemplateName() {
    return templateName;
  }

  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  public List<UserCardDTO> getMembers() {
    return projectTeam;
  }

  public void setMembers(List<UserCardDTO> members) {
    this.projectTeam = members;
  }

  public List<String> getSharedWith() {
    return sharedWith;
  }

  public void setSharedWith(List<String> sharedWith) {
    this.sharedWith = sharedWith;
  }

  public DatasetPermissions getPermissions() {
    return permissions;
  }

  public void setPermissions(DatasetPermissions permissions) {
    this.permissions = permissions;
  }

  public List<UserCardDTO> getProjectTeam() {
    return projectTeam;
  }

  public void setProjectTeam(List<UserCardDTO> projectTeam) {
    this.projectTeam = projectTeam;
  }

  public boolean isIsPublic() {
    return isPublic;
  }

  public void setIsPublic(boolean isPublic) {
    this.isPublic = isPublic;
  }

  public boolean isGenerateReadme() {
    return generateReadme;
  }

  public void setGenerateReadme(boolean generateReadme) {
    this.generateReadme = generateReadme;
  }

  public void setType(DatasetType type) { this.type = type; }

  public DatasetType getType() { return this.type; }

  @Override
  public String toString() {
    return "DataSetDTO{" + "name=" + name + ", description=" + description
            + ", searchable=" + searchable + ", generateReadme="
            + generateReadme + ", template=" + this.template + '}';
  }

}
