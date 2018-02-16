=begin
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
