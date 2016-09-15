module ProjectHelper
  def with_valid_project
    @project ||= create_project
  end

  def create_project
    with_valid_session
    new_project = {projectName: "project_#{short_random_id}", description:"", status: 0, services: ["JOBS","ZEPPELIN"], projectTeam:[], retentionPeriod: ""}
    post "/hopsworks/api/project", new_project
    get_project_by_name(new_project[:projectName])
  end
  
  def add_member(member, role)
    with_valid_session
    with_valid_project
    post "/hopsworks/api/project/#{@project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: @project[:id],teamMember: member},teamRole: role}]}
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
   get "/hopsworks/api/project/getAll"
    json_body.map{|project| project[:id]}.each{|i| post "/hopsworks/api/project/#{i}/delete" }
  end
end
