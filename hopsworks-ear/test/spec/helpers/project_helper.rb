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
    new_project = {projectName: "project_#{short_random_id}", description:"", status: 0, services: ["JOBS","ZEPPELIN"], projectTeam:[], retentionPeriod: ""}
    post "#{ENV['HOPSWORKS_API']}/project", new_project
    expect_json(errorMsg: ->(value){ expect(value).to be_empty})
    expect_json(successMessage: "Project created successfully.")
    expect_status(201)
    get_project_by_name(new_project[:projectName])
  end
  
  def create_project_by_name(projectname)
    with_valid_session
    new_project = {projectName: projectname, description:"", status: 0, services: ["JOBS","ZEPPELIN"], projectTeam:[], retentionPeriod: ""}
    post "#{ENV['HOPSWORKS_API']}/project", new_project
    expect_json(errorMsg: ->(value){ expect(value).to be_empty})
    expect_json(successMessage: "Project created successfully.")
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
  
  def check_project_limit
    get "#{ENV['HOPSWORKS_API']}/user/profile"
    max_num_projects = json_body[:maxNumProjects]
    get "#{ENV['HOPSWORKS_API']}/project"
    if json_body.length >= max_num_projects
      delete_project(json_body[0][:project])
    end
  end
  
  def create_max_num_projects
    get "#{ENV['HOPSWORKS_API']}/user/profile"
    max_num_projects = json_body[:maxNumProjects]
    get "#{ENV['HOPSWORKS_API']}/project"
    while json_body.length < max_num_projects
      post "#{ENV['HOPSWORKS_API']}/project", {projectName: "project_#{Time.now.to_i}"}
      get "#{ENV['HOPSWORKS_API']}/project"
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
