=begin
This file is part of HopsWorks

Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.

HopsWorks is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

HopsWorks is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
=end
module ProjectHelper
  def with_valid_project
    @project ||= create_project
    get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/getContent"
    if response.code != 200 # project and logged in user not the same 
      @project = create_project
    end
  end

  def create_project
    with_valid_session
    new_project = {projectName: "project_#{short_random_id}", description:"", status: 0, services: ["JOBS","ZEPPELIN","SERVING"], projectTeam:[], retentionPeriod: ""}
    post "#{ENV['HOPSWORKS_API']}/project", new_project
    expect_json(errorMsg: ->(value){ expect(value).to be_empty})
    expect_json(successMessage: regex("Project created successfully.*"))
    expect_status(201)
    get_project_by_name(new_project[:projectName])
  end
  
  def create_project_by_name(projectname)
    with_valid_session
    new_project = {projectName: projectname, description:"", status: 0, services: ["JOBS","ZEPPELIN","SERVING"], projectTeam:[], retentionPeriod: ""}
    post "#{ENV['HOPSWORKS_API']}/project", new_project
    expect_json(errorMsg: ->(value){ expect(value).to be_empty})
    expect_json(successMessage: regex("Project created successfully.*"))
    expect_status(201)
    get_project_by_name(new_project[:projectName])
  end
  
  def delete_project(project)
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/delete"
    expect_json(errorMsg: "")
    expect_json(successMessage: "The project and all related files were removed successfully.")
    expect_status(200)
  end
  
  def add_member(member, role)
    with_valid_project
    post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: @project[:id],teamMember: member},teamRole: role}]}
    expect_json(errorMsg: ->(value){ expect(value).to be_empty})
    expect_json(successMessage: "One member added successfully")
    expect_status(200)
  end
  
  def get_all_projects
    projects = Project.find_by(username: "#{@user.email}")
    projects
  end
  
  def get_project
    @project
  end
 
  def get_project_by_name(name)
    Project.find_by(projectName: "#{name}")
  end
  
  def check_project_limit(limit=0)
    get "#{ENV['HOPSWORKS_API']}/user/profile"
    max_num_projects = json_body[:maxNumProjects]
    num_created_projects = json_body[:numCreatedProjects]
    if (max_num_projects - num_created_projects) <= limit
      reset_session
      with_valid_project
    end
    
  end
  
  def create_max_num_projects
    get "#{ENV['HOPSWORKS_API']}/user/profile"
    max_num_projects = json_body[:maxNumProjects]
    num_created_projects = json_body[:numCreatedProjects]
    while num_created_projects < max_num_projects
      post "#{ENV['HOPSWORKS_API']}/project", {projectName: "project_#{Time.now.to_i}"}
      get "#{ENV['HOPSWORKS_API']}/user/profile"
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
