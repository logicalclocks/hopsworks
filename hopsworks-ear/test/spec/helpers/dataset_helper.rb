module DatasetHelper
  def with_valid_dataset
    @dataset ||= create_dataset
    if @dataset[:projectId] != @project[:id]
      @dataset = create_dataset
    end
  end
  
  def create_dataset
    with_valid_project
    dsname = "dataset_#{short_random_id}"
    post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/createTopLevelDataSet", {name: dsname, description: "test dataset", searchable: true, generateReadme: true}
    expect_json(errorMsg: ->(value){ expect(value).to be_empty})
    expect_json(successMessage: "The Dataset was created successfully.")
    expect_status(200)
    get_dataset_by_name(dsname) 
  end
  
  def create_dataset_by_name(project, dsname)
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/createTopLevelDataSet", {name: dsname, description: "test dataset", searchable: true, generateReadme: true}
    expect_json(errorMsg: ->(value){ expect(value).to be_empty})
    expect_json(successMessage: "The Dataset was created successfully.")
    expect_status(200)
    get_dataset(project, dsname) 
  end
  
  def get_datasets_in(project, dsname)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/getContent/#{dsname}"
    json_body
  end
  
  def get_all_datasets(project)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/getContent"
    json_body
  end
  
  def get_dataset_by_name(name)
    Dataset.where(projectId: "#{@project[:id]}", inode_name: name).first
  end
  def get_dataset(project, name)
    Dataset.where(projectId: "#{project[:id]}", inode_name: name).first
  end
  
  def share_dataset(project1, dsname, project2)
    post "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/dataset/shareDataSet", {name: dsname, projectId: project2[:id]}
  end
  
  def request_dataset_access(project, inode)
    post "#{ENV['HOPSWORKS_API']}/request/access", {inodeId: inode, projectId: project[:id]}
  end
end
