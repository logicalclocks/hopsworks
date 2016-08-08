module ProjectHelper
  def with_valid_project
    @project ||= create_project
  end

  def create_project
    with_valid_session
    post "/hopsworks/api/project", {projectName: "project_#{short_random_id}", description:"", status: 0, services: ["JOBS","ZEPPELIN"], projectTeam:[], retentionPeriod: ""}
    JSON.parse json_body[:data]
  end

  def clean_projects
    with_valid_session
    get "/hopsworks/api/project/getAll"
    json_body.map{|project| project[:id]}.each{|i| post "/hopsworks/api/project/#{i}/delete" }
  end
end
