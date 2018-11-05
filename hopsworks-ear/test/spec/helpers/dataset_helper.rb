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
module DatasetHelper
  def with_valid_dataset
    @dataset ||= create_dataset
    if @dataset[:projectId] != @project[:id]
      @dataset = create_dataset
    end
  end

  def wait_for
    timeout = 30
    start = Time.now
    x = yield
    until x
      if Time.now - start > timeout
        raise "Timed out waiting for Dataset action to finish. Timeout #{timeout} sec"
      end
      sleep(1)
      x = yield
    end
  end
  
  def create_dataset
    with_valid_project
    dsname = "dataset_#{short_random_id}"
    post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/createTopLevelDataSet", {name: dsname, description: "test dataset", searchable: true, generateReadme: true}
    expect_json(successMessage: "The Dataset was created successfully.")
    expect_status(200)
    get_dataset_by_name(dsname) 
  end
  
  def create_dataset_by_name(project, dsname)
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/createTopLevelDataSet", {name: dsname, description: "test dataset", searchable: true, generateReadme: true}
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
