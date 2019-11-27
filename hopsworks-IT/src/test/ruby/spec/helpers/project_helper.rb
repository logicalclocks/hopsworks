=begin
 Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 are released under the following license:

 This file is part of Hopsworks
 Copyright (C) 2018, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.

 Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 are released under the following license:

 Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved

 Permission is hereby granted, free of charge, to any person obtaining a copy of this
 software and associated documentation files (the "Software"), to deal in the Software
 without restriction, including without limitation the rights to use, copy, modify, merge,
 publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or
 substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
=end
module ProjectHelper
  def with_valid_project
    @project ||= create_project
    get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/?action=listing"
    if response.code != 200 # project and logged in user not the same
      @project = create_project
    end
  end

  def with_valid_tour_project(type)
    @project ||= create_project_tour(type)
    get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/?action=listing"
    if response.code != 200 # project and logged in user not the same
      @project = create_project_tour(type)
    end
  end

  def create_project
    with_valid_session
    new_project = {projectName: "ProJect_#{short_random_id}", description:"", status: 0, services: ["JOBS","JUPYTER","HIVE","KAFKA","SERVING", "FEATURESTORE"],
                   projectTeam:[], retentionPeriod: ""}
    post "#{ENV['HOPSWORKS_API']}/project", new_project
    expect_status(201)
    expect_json(successMessage: regex("Project created successfully.*"))
    get_project_by_name(new_project[:projectName])
  end

  def create_project_by_name(projectname)
    with_valid_session
    create_project_by_name_existing_user(projectname)
  end

  def create_project_by_name_existing_user(projectname)
    new_project = {projectName: projectname, description:"", status: 0, services: ["JOBS","JUPYTER", "HIVE", "KAFKA","SERVING", "FEATURESTORE"],
                   projectTeam:[], retentionPeriod: ""}
    post "#{ENV['HOPSWORKS_API']}/project", new_project
    expect_status(201)
    expect_json(successMessage: regex("Project created successfully.*"))
    get_project_by_name(new_project[:projectName])
  end

  def create_project_tour(tourtype)
    with_valid_session
    post "#{ENV['HOPSWORKS_API']}/project/starterProject/#{tourtype}"
    expect_status(201)
    expect_json(description: regex("A demo project*"))
    get_project_by_name(json_body[:name])
  end

  def delete_project(project)
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/delete"
    expect_status(200)
    expect_json(successMessage: "The project and all related files were removed successfully.")
  end

  def add_member(member, role)
    with_valid_project
    add_member_to_project(@project, member, role)
  end

  def add_member_to_project(project, member, role)
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: project[:id],teamMember: member},teamRole: role}]}
    expect_status(200)
    expect_json(successMessage: "One member added successfully")
  end

  def get_all_projects
    projects = Project.find_by(username: "#{@user.email}")
    projects
  end

  def get_project
    @project
  end

  def update_project
    @project = get_project_by_name(name)
  end

  def get_project_by_name(name)
    Project.find_by(projectName: "#{name}")
  end

  def check_project_limit(limit=0)
    with_valid_session
    get "#{ENV['HOPSWORKS_API']}/users/profile"
    max_num_projects = json_body[:maxNumProjects]
    num_created_projects = json_body[:numCreatedProjects]
    if (max_num_projects - num_created_projects) <= limit
      reset_session
      with_valid_project
    end

  end

  def create_max_num_projects
    get "#{ENV['HOPSWORKS_API']}/users/profile"
    max_num_projects = json_body[:maxNumProjects]
    num_created_projects = json_body[:numCreatedProjects]
    while num_created_projects < max_num_projects
      post "#{ENV['HOPSWORKS_API']}/project", {projectName: "project_#{Time.now.to_i}"}
      get "#{ENV['HOPSWORKS_API']}/users/profile"
      max_num_projects = json_body[:maxNumProjects]
      num_created_projects = json_body[:numCreatedProjects]
    end
  end

  def clean_projects
    with_valid_session
    get "#{ENV['HOPSWORKS_API']}/project/getAll"
    if !json_body.empty?
      json_body.map{|project| project[:id]}.each{|i| post "#{ENV['HOPSWORKS_API']}/project/#{i}/delete" }
    end
  end
end
