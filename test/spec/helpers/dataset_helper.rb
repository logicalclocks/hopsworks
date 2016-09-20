module DatasetHelper
  def with_valid_dataset
    @dataset ||= create_dataset
  end
  
  def create_dataset
    with_valid_project
    dsname = "dataset_#{short_random_id}"
    post "/hopsworks/api/project/#{@project[:id]}/dataset/createTopLevelDataSet", {name: dsname, description: "test dataset", searchable: true, generateReadme: true}
    get_dataset_by_name(dsname) 
  end
  
  def create_dataset_by_name(project, dsname)
    post "/hopsworks/api/project/#{project[:id]}/dataset/createTopLevelDataSet", {name: dsname, description: "test dataset", searchable: true, generateReadme: true}
    get_dataset(project, dsname) 
  end
  
  def get_datasets_in(project, dsname)
    get "/hopsworks/api/project/#{project[:id]}/dataset/#{dsname}"
    json_body
  end
  
  def get_all_datasets(project)
    get "/hopsworks/api/project/#{project[:id]}/dataset"
    json_body
  end
  
  def get_dataset_by_name(name)
    Dataset.where(projectId: "#{@project[:id]}", inode_name: name).first
  end
  def get_dataset(project, name)
    Dataset.where(projectId: "#{project[:id]}", inode_name: name).first
  end
  
  def share_dataset(project1, dsname, project2)
    post "/hopsworks/api/project/#{project1[:id]}/dataset/shareDataSet", {name: dsname, projectId: project2[:id]}
  end
  
  def request_dataset_access(project, inode)
    post "/hopsworks/api/request/access", {inodeId: inode, projectId: project[:id]}
  end
end
