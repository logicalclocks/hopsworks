module ProjectHelper
  def with_valid_project
    @project ||= create_project
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
  end
  
  def add_member(member, role)
    with_valid_session
    with_valid_project
    post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: @project[:id],teamMember: member},teamRole: role}]}
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
  
  def clean_projects
   with_valid_session
   get "#{ENV['HOPSWORKS_API']}/project/getAll"
   if !json_body.empty?
    json_body.map{|project| project[:id]}.each{|i| post "#{ENV['HOPSWORKS_API']}/project/#{i}/delete" }
   end
  end
end
