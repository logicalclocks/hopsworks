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
    @dataset ||= create_dataset_checked
    if @dataset[:projectId] != @project[:id]
      @dataset = create_dataset_checked
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

  def uploadFile(project, dsname, filePath)
    file_size = File.size(filePath)
    file_name = File.basename(filePath)
    file = URI.encode_www_form({flowChunkNumber: 1, flowChunkSize: 1048576,
                                flowCurrentChunkSize: file_size, flowTotalSize: file_size,
                                flowIdentifier: "#{file_size}-#{file_name}", flowFilename: "#{file_name}",
                                flowRelativePath: "#{file_name}", flowTotalChunks: 1})
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/upload/#{dsname}?#{file}", {content_type: "multipart/form-data"}
  end
  
  def create_dataset_checked
    with_valid_project
    dsname = "dataset_#{short_random_id}"
    create_dataset_by_name_checked(@project, dsname)
    expect_status_details(201)
    get_dataset_by_name(dsname)
  end

  def create_dataset_by_name_checked(project, dsname, permission: "READ_ONLY")
    create_dataset_by_name(project, dsname, permission: permission)
    expect_status_details(201)
    get_dataset(project, dsname) 
  end

  def create_dataset_by_name(project, dsname, permission: "READ_ONLY")
    query = URI.encode_www_form({description: "test dataset", searchable: true, generate_readme: true, permission: permission})
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{dsname}?action=create&#{query}"
  end
  
  def get_all_datasets(project)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset?action=listing&expand=inodes"
    json_body
  end

  def get_dataset_by_name(name)
    Dataset.where(projectId: "#{@project[:id]}", inode_name: name).first # not a primary key lookup
  end
  def get_dataset(project, name)
    pp "dataset get - project_id:#{project[:id]} name:#{name}" if defined?(@debugOpt) && @debugOpt == true
    Dataset.where(projectId: "#{project[:id]}", inode_name: name).first
  end
  
  def request_dataset_access(project, inode)
    post "#{ENV['HOPSWORKS_API']}/request/access", {inodeId: inode, projectId: project[:id]}
  end

  def request_access(owningProject, dataset, requestingProject)
    request_access_by_name(owningProject, dataset[:inode_name], requestingProject)
  end

  def request_access_by_name(owningProject, datasetName, requestingProject)
    get_dataset_stat(owningProject, datasetName, datasetType: "&type=DATASET")
    expect_status(200)
    ds = json_body
    request_dataset_access(requestingProject, ds[:attributes][:id])
  end

  def request_access_by_dataset(dataset, requesting_project)
    dataset_inode = get_dataset_inode(dataset)
    query = "#{ENV['HOPSWORKS_API']}/request/access"
    payload = {inodeId: dataset_inode[:id], projectId: requesting_project[:id]}
    pp "#{query}, #{payload}" if defined?(@debugOpt) && @debugOpt == true
    post "#{query}", payload
    expect_status_details(200)
  end

  def find_inode_in_dataset(inode_list, inode_substring_name)
    inode_list.each do |inode|
      if inode["name"].include?(inode_substring_name)
        return true
      end
    end
    return false
  end

  def create_datasets(num, searchable=true)
    create_dirs(@project, "", num, searchable)
  end

  def create_datasets_for_new_user(num)
    project_owner = @user
    newUserParams = {}
    newUserParams[:first_name] = "firstName" # should be different from name
    newUserParams[:last_name] = "lastName" # should be different from last
    newUser = create_user(newUserParams)
    add_member_to_project(@project, newUser[:email], "Data owner")
    create_session(newUser[:email], "Pass123")
    create_dirs(@project, "", num, true)
    create_session(project_owner[:email], "Pass123")
    @user = project_owner
  end

  def create_shared_datasets(num, accepted)
    projectname = "project_#{short_random_id}"
    project = create_project_by_name_existing_user(projectname)
    x = 0
    while x < num do
      name = (0...7).map { ('a'..'z').to_a[rand(26)] }.join
      dsname = "#{name}_#{short_random_id}"
      query = URI.encode_www_form({description: "test dataset", searchable: false, generate_readme: false, permission: "READ_ONLY"})
      create_dir(project, dsname, query: "&type=DATASET&#{query}")
      if response.code == 201
        x += 1
        if accepted
          request_access_by_name(project, dsname, @project)
        end
        share_dataset(project, dsname, @project[:projectname], permission: "EDITABLE", datasetType: "")
      end
    end

  end

  def create_dataset_contents(num)
    create_dirs(@project, @dataset[:inode_name], num, false)
  end

  def create_dataset_contents_for_new_user(num)
    project_owner = @user
    newUserParams = {}
    newUserParams[:first_name] = "firstName" # should be different from name
    newUserParams[:last_name] = "lastName" # should be different from last
    newUser = create_user(newUserParams)
    add_member_to_project(@project, newUser[:email], "Data owner")
    update_dataset_permissions(@project, @dataset[:inode_name], "EDITABLE", datasetType: "&type=DATASET")
    expect_status(200)
    create_session(newUser[:email], "Pass123")
    create_dirs(@project, @dataset[:inode_name], num, false)
    create_session(project_owner[:email], "Pass123")
    @user = project_owner
  end

  def create_files
    chmod_local_dir("#{ENV['PROJECT_DIR']}".gsub("/hopsworks", ""), 701, false)
    chmod_local_dir("#{ENV['PROJECT_DIR']}/tools", 777)
    copy_from_local("#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json",
                    "/Projects/#{@project[:projectname]}/#{@dataset[:inode_name]}/Sample.json", @user[:username],
                    "#{@project[:projectname]}__#{@dataset[:inode_name]}", 750, "#{@project[:projectname]}")
  end

  def create_dirs(project, path, num, searchable)
    x = 0
    while x < num do
      name = (0...7).map { ('a'..'z').to_a[rand(26)] }.join
      dsname = "#{path}/#{name}_#{short_random_id}"
      query = URI.encode_www_form({description: "test dataset", searchable: searchable, generate_readme: false, permission: "READ_ONLY"})
      create_dir(project, dsname, query: "&type=DATASET&#{query}")
      if response.code == 201
        x += 1
      end
    end
  end

  def create_dir(project, path, query: "")
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=create#{query}"
  end

  def create_dir_checked(project, path, query: "")
    create_dir(project, path, query: query)
    expect_status_details(201)
  end

  def delete_dir(project, path, dataset_type: "DATASET")
    pp "delete #{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?type=#{dataset_type}" if defined?(@debugOpt) && @debugOpt
    delete "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?type=#{dataset_type}"
  end

  def delete_dir_checked(project, path, dataset_type: "DATASET")
    delete_dir(project, path, dataset_type)
    expect_status_details(204)
  end

  def create_random_dataset(project, searchable, generate_readme)
    dsname = "dataset_#{short_random_id}"
    query = URI.encode_www_form({description: "test dataset", searchable: searchable, generate_readme: generate_readme, permission: "READ_ONLY"})
    create_dir(project, dsname, query: "&#{query}")
    expect_status(201)
    return dsname
  end

  def copy_dataset(project, path, destination_path, datasetType: "")
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=copy&destination_path=#{destination_path}#{datasetType}"
  end

  def move_dataset(project, path, destination_path, datasetType: "")
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=move&destination_path=#{destination_path}#{datasetType}"
  end

  def share_dataset(project, path, target_project, permission: "EDITABLE", datasetType: "")
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=share&target_project=#{target_project}&permission=#{permission}#{datasetType}"
  end

  def share_dataset_checked(project, path, target_project, permission: "EDITABLE", datasetType: "DATASET")
    query = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=share&target_project=#{target_project}&permission=#{permission}&type=#{datasetType}"
    pp "#{query}" if defined?(@debugOpt) && @debugOpt == true
    post "#{query}"
    expect_status_details(204)
  end

  def get_dataset_inode(dataset)
    inode = INode.where(partition_id: dataset[:partition_id], parent_id: dataset[:inode_pid], name: dataset[:inode_name])
    expect(inode.length).to eq(1), "inode not found for dataset: #{dataset[:inode_name]}"
    inode.first
  end

  def publish_dataset(project, dataset_name, datasetType: "DATASET")
    query = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{dataset_name}?type=#{datasetType}&action=publish"
    pp "#{query}" if defined?(@debugOpt) && @debugOpt == true
    post "#{query}"
  end

  def publish_dataset_checked(project, dataset_name, datasetType: "DATASET")
    publish_dataset(project, dataset_name, datasetType: datasetType)
    expect_status_details(204)
    dataset = get_dataset(project, dataset_name)
    expect(dataset).not_to be_nil, "main dataset is nil"
    expect(dataset[:public_ds]).to be(true), "main dataset - attribute public - not set to true"
  end

  def unpublish_dataset(project, dataset_name, datasetType: "")
    query = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{dataset_name}?type=#{datasetType}&action=unpublish"
    pp "#{query}" if defined?(@debugOpt) && @debugOpt == true
    post "#{query}"
  end

  def unpublish_dataset_checked(project, dataset_name, datasetType: "")
    unpublish_dataset(project, dataset_name, datasetType: datasetType)
    expect_status_details(204)
    dataset = get_dataset(project, dataset_name)
    expect(dataset).not_to be_nil, "main dataset is nil"
    expect(dataset[:public_ds]).to be(false), "main dataset - attribute public - not reset to false"
  end

  def import_dataset(target_project, dataset_name, dataset_project, datasetType: "")
    query = "#{ENV['HOPSWORKS_API']}/project/#{target_project[:id]}/dataset/#{dataset_name}"\
      "?type=#{datasetType}&action=import&target_project=#{dataset_project[:projectname]}"
    pp "#{query}" if defined?(@debugOpt) && @debugOpt == true
    post "#{query}"
  end

  def import_dataset_checked(target_project, dataset_name, dataset_project, datasetType: "")
    import_dataset(target_project, dataset_name, dataset_project, datasetType: datasetType)
    expect_status_details(204)
    check_shared_dataset(target_project, dataset_name, dataset_project)
  end

  def unshare_all_dataset(project, dataset_name, datasetType: "DATASET")
    query = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{dataset_name}?type=#{datasetType}&action=unshare_all"
    pp "#{query}" if defined?(@debugOpt) && @debugOpt == true
    post "#{query}"
  end

  def unshare_all_dataset_checked(project, dataset_name, datasetType: "DATASET")
    unshare_all_dataset(project, dataset_name, datasetType: datasetType)
    expect_status_details(204)
  end

  def check_shared_dataset(target_project, dataset_name, dataset_project)
    get_all_datasets(target_project)
    dataset_full_name = "#{dataset_project[:projectname]}::#{dataset_name}"
    dataset = json_body[:items].select do | d | d[:name] == "#{dataset_full_name}" end
    expect(dataset.length).to eq(1), "dataset:#{dataset_name} not available in project:#{target_project} body:#{JSON.pretty_generate(json_body)}"
  end

  def check_not_shared_dataset(target_project, dataset_name, dataset_project)
    get_all_datasets(target_project)
    dataset_full_name = "#{dataset_project[:projectname]}::#{dataset_name}"
    dataset = json_body[:items].select do | d | d[:name] == "#{dataset_full_name}" end
    expect(dataset.length).to eq(0), "dataset:#{dataset_name} should not be available in project:#{target_project} body:#{JSON.pretty_generate(json_body)}"
  end

  def accept_dataset(project, path, datasetType: "")
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=accept#{datasetType}"
  end

  def accept_dataset_checked(project, path, datasetType: "DATASET")
    query = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=accept&type=#{datasetType}"
    pp "#{query}" if defined?(@debugOpt) && @debugOpt == true
    post "#{query}"
    expect_status_details(204)
  end

  def reject_dataset_checked(project, path, datasetType: "DATASET")
    query = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=reject&type=#{datasetType}"
    pp "#{query}" if defined?(@debugOpt) && @debugOpt == true
    post "#{query}"
    expect_status_details(204)
  end

  def reject_dataset(project, path, datasetType: "")
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=reject#{datasetType}"
  end

  def zip_dataset(project, path, datasetType: "")
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=zip#{datasetType}"
  end

  def unzip_dataset(project, path, datasetType: "")
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=unzip#{datasetType}"
  end

  def delete_dataset(project, path, datasetType: "")
    delete "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}#{datasetType}"
  end

  def delete_corrupted_dataset(project, path, datasetType: "")
    delete "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=corrupted#{datasetType}"
  end

  def unshare_dataset(project, path, datasetType: "")
    delete "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=unshare#{datasetType}"
  end

  def unshare_from(project, path, target, datasetType: "")
    delete "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=unshare&target_project=#{target}#{datasetType}"
  end

  def get_download_token(project, path, datasetType: "")
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/download/token/#{path}#{datasetType}"
  end

  def download_dataset_with_token(project, path, token, datasetType: "")
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/download/with_token/#{path}?token=#{token
    }#{datasetType}"
  end

  def download_dataset_with_auth(project, path, datasetType: "")
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/download/with_auth/#{path}?#{datasetType}"
  end

  def download_dataset(project, path, datasetType: "")
    get_download_token(project, path, datasetType: "?#{datasetType}")
    expect_status(200)
    token = json_body[:data][:value]
    download_dataset_with_token(project, path, token, datasetType: "&#{datasetType}")
  end

  def get_datasets_in_path(project, path, query: "")
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=listing&expand=inodes#{query}"
  end

  def get_dataset_stat(project, path, datasetType: "", expand_inodes: true)
    if expand_inodes
      pp "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=stat&expand=inodes#{datasetType}" if defined?(@debugOpt) && @debugOpt
      get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=stat&expand=inodes#{datasetType}"
    else
      pp "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=stat#{datasetType}" if defined?(@debugOpt) && @debugOpt
      get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=stat#{datasetType}"
    end
  end

  def get_dataset_stat_checked(project, path, datasetType: "", expand_inodes: true)
    get_dataset_stat(project, path, datasetType: datasetType, expand_inodes: expand_inodes)
    expect_status_details(200)
    json_body
  end

  def get_dataset_blob(project, path, datasetType: "")
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=blob&expand=inodes&mode=head#{datasetType}"
  end

  def update_dataset_permissions(project, path, permissions, datasetType: "")
    put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=permission&permissions=#{permissions}#{datasetType}"
  end

  def update_dataset_permissions_checked(project, path, permissions, datasetType: "DATASET")
    update_dataset_permissions(project, path, permissions, datasetType: "&type=#{datasetType}")
    expect_status_details(200)
  end

  def update_dataset_shared_with_permissions(project, path, target_project, permissions, datasetType: "")
    put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=share_permission&permissions=#{permissions}&target_project=#{target_project[:projectname]}#{datasetType}"
  end

  def update_dataset_description(project, path, description, datasetType: "")
    put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=description&description=#{description}#{datasetType}"
  end

  def get_list_from_json(json_body, field)
    json_body[:items].map { |o| o[:"#{field}"] }
  end

  def get_list_from_json_attr(json_body, field)
    json_body[:items].map { |o| o[:attributes][:"#{field}"] }
  end

  def test_sort_by_str(project, datasets, path, sort_by, order, sort_by_query)
    get_datasets_in_path(project, path, query: "&sort_by=#{sort_by_query}:#{order}")
    expect_status(200)
    sortedRes = json_body[:items].map { |o| "#{o[:"#{sort_by}"]}" }
    if order == 'asc'
      sorted = sortedRes.sort_by(&:downcase)
    else
      sorted = sortedRes.sort_by(&:downcase).reverse
    end
    expect(sortedRes).to eq(sorted)
  end

  def test_sort_by_str_attr(project, datasets, path, sort_by, order, sort_by_query)
    get_datasets_in_path(project, path, query: "&sort_by=#{sort_by_query}:#{order}")
    expect_status(200)
    sortedRes = json_body[:items].map { |o| "#{o[:attributes][:"#{sort_by}"]}" }
    if order == 'asc'
      sorted = sortedRes.sort_by(&:downcase)
    else
      sorted = sortedRes.sort_by(&:downcase).reverse
    end
    expect(sortedRes).to eq(sorted)
  end

  def test_sort_by(project, datasets, path, sort_by, order, sort_by_query)
    get_datasets_in_path(project, path, query: "&sort_by=#{sort_by_query}:#{order}")
    expect_status(200)
    sortedRes = get_list_from_json(json_body, sort_by)
    if order == 'asc'
      sorted = sortedRes.sort
    else
      sorted = sortedRes.sort.reverse
    end
    expect(sortedRes).to eq(sorted)
  end

  def test_sort_by_attr(project, datasets, path, sort_by, order, sort_by_query)
    get_datasets_in_path(project, path, query: "&sort_by=#{sort_by_query}:#{order}")
    expect_status(200)
    sortedRes = get_list_from_json_attr(json_body, sort_by)
    if order == 'asc'
      sorted = sortedRes.sort
    else
      sorted = sortedRes.sort.reverse
    end
    expect(sortedRes).to eq(sorted)
  end

  def test_sort_by_date_attr(project, datasets, path, sort_by, order, sort_by_query)
    get_datasets_in_path(project, path, query: "&sort_by=#{sort_by_query}:#{order}")
    expect_status(200)
    sortedRes = get_list_from_json_attr(json_body, sort_by)
    if order == 'asc'
      sorted = sortedRes.sort
    else
      sorted = sortedRes.sort.reverse
    end
    time_expect_to_be_eq(sortedRes, sorted)
  end

  def test_sort_by_datasetType(project, datasets, path, sort_by, order, sort_by_query)
    datasetTypes = {}
    datasetTypes[:DATASET] = 0
    datasetTypes[:HIVEDB] = 1
    datasetTypes[:FEATURESTORE] = 2
    ds = datasets.map { |o| o[:"#{sort_by}"]}
    if order == 'asc'
      sorted = ds.sort do |a, b|
        datasetTypes[:"#{a}"] <=> datasetTypes[:"#{b}"]
      end
    else
      sorted = ds.sort do |a, b|
        datasetTypes[:"#{b}"] <=> datasetTypes[:"#{a}"]
      end
    end
    get_datasets_in_path(project, path, query: "&sort_by=#{sort_by_query}:#{order}")
    expect_status(200)
    sortedRes = get_list_from_json(json_body, sort_by)
    expect(sortedRes).to eq(sorted)
  end

  def test_filter_by(project, excluded, path, filter_by, filter_by_query)
    get_datasets_in_path(project, path, query: "&filter_by=#{filter_by_query}")
    expect_status(200)
    filteredRes = get_list_from_json(json_body, filter_by)
    expect(filteredRes & excluded).to be_empty
  end

  def test_filter_by_attr(project, excluded, path, filter_by, filter_by_query)
    get_datasets_in_path(project, path, query: "&filter_by=#{filter_by_query}")
    expect_status(200)
    filteredRes = get_list_from_json_attr(json_body, filter_by)
    expect(filteredRes & excluded).to be_empty
  end

  def test_filter_by_starts_with(project, datasets, path, filter_by, filter_by_query, filter_val)
    ds = datasets.map { |o| o[:id] if o[:"#{filter_by}"].start_with?("#{filter_val}")}.compact
    sorted = ds.sort
    get_datasets_in_path(project, path, query: "&sort_by=id:asc&filter_by=#{filter_by_query}:#{filter_val}")
    expect_status(200)
    filteredRes = get_list_from_json(json_body, "id")
    expect(filteredRes).to eq(sorted)
  end

  def test_filter_by_starts_with_attr(project, datasets, path, filter_by, filter_by_query, filter_val)
    if path == ""
      ds = datasets.map { |o| o[:id] if o[:attributes][:"#{filter_by}"].start_with?("#{filter_val}")}.compact
    else
      ds = datasets.map { |o| o[:attributes][:id] if o[:attributes][:"#{filter_by}"].start_with?("#{filter_val}")}.compact
    end
    sorted = ds.sort
    get_datasets_in_path(project, path, query: "&sort_by=id:asc&filter_by=#{filter_by_query}:#{filter_val}")
    expect_status(200)
    if path == ""
      filteredRes = get_list_from_json(json_body, "id")
    else
      filteredRes = get_list_from_json_attr(json_body, "id")
    end
    expect(filteredRes).to eq(sorted)
  end

  def test_filter_by_eq(project, datasets, path, filter_by, filter_by_query, filter_val)
    ds = datasets.map { |o| o[:id] if o[:"#{filter_by}"]==filter_val}.compact
    sorted = ds.sort
    get_datasets_in_path(project, path, query: "&sort_by=id:asc&filter_by=#{filter_by_query}:#{filter_val}")
    expect_status(200)
    filteredRes = get_list_from_json(json_body, "id")
    expect(filteredRes).to eq(sorted)
  end

  def test_filter_by_eq_attr(project, datasets, path, filter_by, filter_by_val, filter_by_query, filter_by_query_val)
    if path == ""
      ds = datasets.map { |o| o[:id] if o[:attributes][:"#{filter_by}"]==filter_by_val}.compact
    else
      ds = datasets.map { |o| o[:attributes][:id] if o[:attributes][:"#{filter_by}"]==filter_by_val}.compact
    end
    sorted = ds.sort
    get_datasets_in_path(project, path, query: "&sort_by=id:asc&filter_by=#{filter_by_query}:#{filter_by_query_val}")
    expect_status(200)
    if path == ""
      filteredRes = get_list_from_json(json_body, "id")
    else
      filteredRes = get_list_from_json_attr(json_body, "id")
    end
    expect(filteredRes).to eq(sorted)
  end

  def test_filter_by_lt(project, datasets, path, filter_by, filter_by_query, filter_val)
    ds = datasets.map { |o| o[:id] if o[:"#{filter_by}"]<filter_val}.compact
    sorted = ds.sort
    get_datasets_in_path(project, path, query: "&sort_by=id:asc&filter_by=#{filter_by_query}:#{filter_val}")
    expect_status(200)
    filteredRes = []
    if json_body[:items]
      filteredRes = get_list_from_json(json_body, "id")
    end
    expect(filteredRes).to eq(sorted)
  end

  def test_filter_by_lt_attr(project, datasets, path, filter_by, filter_by_val, filter_by_query, filter_by_query_val)
    if path == ""
      ds = datasets.map { |o| o[:id] if o[:attributes][:"#{filter_by}"]<filter_by_val}.compact
    else
      ds = datasets.map { |o| o[:attributes][:id] if o[:attributes][:"#{filter_by}"]<filter_by_val}.compact
    end
    sorted = ds.sort
    get_datasets_in_path(project, path, query: "&sort_by=id:asc&filter_by=#{filter_by_query}:#{filter_by_query_val}")
    expect_status(200)
    filteredRes = []
    if json_body[:items]
      if path == ""
        filteredRes = get_list_from_json(json_body, "id")
      else
        filteredRes = get_list_from_json_attr(json_body, "id")
      end
    end
    expect(filteredRes).to eq(sorted)
  end

  def test_filter_by_gt(project, datasets, path, filter_by, filter_by_query, filter_val)
    ds = datasets.map { |o| o[:id] if o[:"#{filter_by}"]>filter_val}.compact
    sorted = ds.sort
    get_datasets_in_path(project, path, query: "&sort_by=id:asc&filter_by=#{filter_by_query}:#{filter_val}")
    expect_status(200)
    filteredRes = []
    if json_body[:items]
      filteredRes = get_list_from_json(json_body, "id")
    end
    expect(filteredRes).to eq(sorted)
  end

  def test_filter_by_gt_attr(project, datasets, path, filter_by, filter_by_val, filter_by_query, filter_by_query_val)
    if path == ""
      ds = datasets.map { |o| o[:id] if o[:attributes][:"#{filter_by}"]>filter_by_val}.compact
    else
      ds = datasets.map { |o| o[:attributes][:id] if o[:attributes][:"#{filter_by}"]>filter_by_val}.compact
    end
    sorted = ds.sort
    get_datasets_in_path(project, path, query: "&sort_by=id:asc&filter_by=#{filter_by_query}:#{filter_by_query_val}")
    expect_status(200)
    filteredRes = []
    if json_body[:items]
      if path == ""
        filteredRes = get_list_from_json(json_body, "id")
      else
        filteredRes = get_list_from_json_attr(json_body, "id")
      end
    end
    expect(filteredRes).to eq(sorted)
  end

  def check_offset_limit(offset, limit, len)
    if offset < 0
      offset = 0
    end
    if limit <= 0 or limit > len
      limit = len
    end
    if offset > len
      offset = len
    end
    return offset, limit
  end

  def test_offset_limit(project, datasets, path, offset, limit)
    ds = datasets.map { |o| o[:id]}
    sorted = ds.sort
    get_datasets_in_path(project, path, query: "&sort_by=id:asc&limit=#{limit}&offset=#{offset}")
    expect_status(200)
    filteredRes = get_list_from_json(json_body, "id")
    offset, limit = check_offset_limit(offset, limit, sorted.length)
    expect(filteredRes).to eq(sorted.drop(offset).take(limit))
  end

  def test_offset_limit_attr(project, datasets, path, offset, limit)
    if path == ""
      ds = datasets.map { |o| o[:id]}
    else
      ds = datasets.map { |o| o[:attributes][:id]}
    end
    sorted = ds.sort
    get_datasets_in_path(project, path, query: "&sort_by=id:asc&limit=#{limit}&offset=#{offset}")
    expect_status(200)
    if path == ""
      filteredRes = get_list_from_json(json_body, "id")
    else
      filteredRes = get_list_from_json_attr(json_body, "id")
    end
    offset, limit = check_offset_limit(offset, limit, sorted.length)
    expect(filteredRes).to eq(sorted.drop(offset).take(limit))
  end

  def data_owner_create_dir(project, dataset, data_owner)
    create_session(data_owner[:email], "Pass123")
    create_dir(project, "#{dataset}/test_#{short_random_id}", query: "&type=DATASET")
  end

  def data_scientist_create_dir(project, dataset, data_scientist)
    create_session(data_scientist[:email], "Pass123")
    create_dir(project, "#{dataset}/test_#{short_random_id}", query: "&type=DATASET")
  end

  def test_read_only_dataset(project, dataset, data_owner, data_scientist)
    data_owner_create_dir(project, dataset, data_owner)
    expect_status_details(403)

    data_scientist_create_dir(project, dataset, data_scientist)
    expect_status_details(403)
  end

  def test_editable_dataset(project, dataset, data_owner, data_scientist)
    data_owner_create_dir(project, dataset, data_owner)
    expect_status_details(201)

    data_scientist_create_dir(project, dataset, data_scientist)
    expect_status_details(201)
  end

  def test_owners_only_dataset(project, dataset, data_owner, data_scientist)
    data_owner_create_dir(project, dataset, data_owner)
    expect_status_details(201)

    data_scientist_create_dir(project, dataset, data_scientist)
    expect_status_details(403)
  end

  def get_group_members(dataset_group, dataset_read_group)
    hdfs_group = HdfsGroups.find_by(name: dataset_group)
    hdfs_read_group = HdfsGroups.find_by(name: dataset_read_group)
    usernames_in_write_group = hdfs_group.hdfs_users.map { |user| "#{user[:name]}" }
    usernames_in_read_group = hdfs_read_group.hdfs_users.map { |user| "#{user[:name]}" }
    return {write_group: usernames_in_write_group, read_group: usernames_in_read_group}
  end

  def test_members_read_only_dataset(all_users, dataset_group, dataset_read_group)
    members = get_group_members(dataset_group, dataset_read_group)
    usernames_in_write_group = members[:write_group]
    usernames_in_read_group = members[:read_group]
    expect(usernames_in_read_group & all_users).to match_array(all_users)
    expect(usernames_in_write_group & all_users).to be_empty
  end

  def test_members_editable_dataset(all_users, dataset_group, dataset_read_group)
    members = get_group_members(dataset_group, dataset_read_group)
    usernames_in_write_group = members[:write_group]
    usernames_in_read_group = members[:read_group]
    expect(usernames_in_write_group & all_users).to match_array(all_users)
    expect(usernames_in_read_group & all_users).to be_empty
  end

  def test_members_owners_only_dataset(data_owners, data_scientists, dataset_group, dataset_read_group)
    members = get_group_members(dataset_group, dataset_read_group)
    usernames_in_write_group = members[:write_group]
    usernames_in_read_group = members[:read_group]
    expect(usernames_in_write_group & data_owners).to match_array(data_owners)
    expect(usernames_in_write_group & data_scientists).to be_empty
    expect(usernames_in_read_group & data_scientists).to match_array(data_scientists)
    expect(usernames_in_read_group & data_owners).to be_empty
  end

  def test_members_not_in_read_only_dataset(all_users, dataset_group, dataset_read_group)
    members = get_group_members(dataset_group, dataset_read_group)
    usernames_in_write_group = members[:write_group]
    usernames_in_read_group = members[:read_group]
    expect(usernames_in_read_group & all_users).to be_empty
    expect(usernames_in_write_group & all_users).to be_empty
  end

  def test_members_not_in_editable_dataset(all_users, dataset_group, dataset_read_group)
    members = get_group_members(dataset_group, dataset_read_group)
    usernames_in_write_group = members[:write_group]
    usernames_in_read_group = members[:read_group]
    expect(usernames_in_write_group & all_users).to be_empty
    expect(usernames_in_read_group & all_users).to be_empty
  end

  def test_members_not_in_owners_only_dataset(data_owners, data_scientists, dataset_group, dataset_read_group)
    members = get_group_members(dataset_group, dataset_read_group)
    usernames_in_write_group = members[:write_group]
    usernames_in_read_group = members[:read_group]
    expect(usernames_in_write_group & data_owners).to be_empty
    expect(usernames_in_write_group & data_scientists).to be_empty
    expect(usernames_in_read_group & data_scientists).to be_empty
    expect(usernames_in_read_group & data_owners).to be_empty
  end

  def do_permission_cleanup(project)
    with_admin_session
    post "#{ENV['HOPSWORKS_API']}/admin/projects/#{project[:id]}/fix-permission"
    expect_status_details(204)
    create_session(project[:username], "Pass123")
  end

  def test_dataset_permission(project, datasetName, permission)
    get_dataset_stat(project, datasetName, datasetType: '&type=DATASET')
    expect_status_details(200)
    ds = json_body
    expect(ds[:attributes][:permission]).to eq (permission)
  end
end
