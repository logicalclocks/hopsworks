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
    query = URI.encode_www_form({description: "test dataset", searchable: true, generate_readme: true})
    post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/#{dsname}?action=create&#{query}"
    expect_status(201)
    get_dataset_by_name(dsname) 
  end
  
  def create_dataset_by_name(project, dsname)
    query = URI.encode_www_form({description: "test dataset", searchable: true, generate_readme: true})
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{dsname}?action=create&#{query}"
    expect_status(201)
    get_dataset(project, dsname) 
  end
  
  def get_all_datasets(project)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset?action=listing&expand=inodes"
    json_body
  end
  
  def get_dataset_by_name(name)
    Dataset.where(projectId: "#{@project[:id]}", inode_name: name).first # not a primary key lookup
  end
  def get_dataset(project, name)
    Dataset.where(projectId: "#{project[:id]}", inode_name: name).first
  end
  
  def request_dataset_access(project, inode)
    post "#{ENV['HOPSWORKS_API']}/request/access", {inodeId: inode, projectId: project[:id]}
  end

  def request_access(owningProject, dataset, requestingProject)
    request_access_by_name(owningProject, dataset[:inode_name], requestingProject)
  end

  def request_access_by_name(owningProject, datasetName, requestingProject)
    get_dataset_stat(owningProject, datasetName, "&type=DATASET")
    expect_status(200)
    ds = json_body
    request_dataset_access(requestingProject, ds[:attributes][:id])
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
      query = URI.encode_www_form({description: "test dataset", searchable: false, generate_readme: false})
      create_dir(project, dsname, "&type=DATASET&#{query}")
      if response.code == 201
        x += 1
        if accepted
          request_access_by_name(project, dsname, @project)
        end
        share_dataset(project, dsname, @project[:projectname], "")
      end
    end

  end

  def create_dataset_contents(num)
    create_dirs(@project, @dataset[:inode_name], num, false )
  end

  def create_dataset_contents_for_new_user(num)
    project_owner = @user
    newUserParams = {}
    newUserParams[:first_name] = "firstName" # should be different from name
    newUserParams[:last_name] = "lastName" # should be different from last
    newUser = create_user(newUserParams)
    add_member_to_project(@project, newUser[:email], "Data owner")
    update_dataset_permissions(@project, @dataset[:inode_name], "GROUP_WRITABLE_SB", "&type=DATASET")
    expect_status(200)
    create_session(newUser[:email], "Pass123")
    create_dirs(@project, @dataset[:inode_name], num, false)
    create_session(project_owner[:email], "Pass123")
    @user = project_owner
  end

  def create_files
    chmod_local_dir("#{ENV['PROJECT_DIR']}".gsub("/hopsworks", ""), 701, false)
    chmod_local_dir("#{ENV['PROJECT_DIR']}/tools", 777)
    copy_from_local("#{ENV['PROJECT_DIR']}/tools/metadata_designer/Sample.json",
                    "/Projects/#{@project[:projectname]}/#{@dataset[:inode_name]}/Sample.json", @user[:username],
                    "#{@project[:projectname]}__#{@dataset[:inode_name]}", 750, "#{@project[:projectname]}")
    copy_from_local("#{ENV['PROJECT_DIR']}/tools/metadata_designer/Sample.json",
                    "/Projects/#{@project[:projectname]}/#{@dataset[:inode_name]}/SampleCollection.json", @user[:username],
                    "#{@project[:projectname]}__#{@dataset[:inode_name]}", 750, "#{@project[:projectname]}")
    copy_from_local("#{ENV['PROJECT_DIR']}/tools/metadata_designer/Sample.json",
                    "/Projects/#{@project[:projectname]}/#{@dataset[:inode_name]}/SampleCollection_Ext.json", @user[:username],
                    "#{@project[:projectname]}__#{@dataset[:inode_name]}", 750, "#{@project[:projectname]}")
    copy_from_local("#{ENV['PROJECT_DIR']}/tools/metadata_designer/Sample.json",
                    "/Projects/#{@project[:projectname]}/#{@dataset[:inode_name]}/Study.json", @user[:username],
                    "#{@project[:projectname]}__#{@dataset[:inode_name]}", 750, "#{@project[:projectname]}")
  end

  def create_dirs(project, path, num, searchable)
    x = 0
    while x < num do
      name = (0...7).map { ('a'..'z').to_a[rand(26)] }.join
      dsname = "#{path}/#{name}_#{short_random_id}"
      query = URI.encode_www_form({description: "test dataset", searchable: searchable, generate_readme: false})
      create_dir(project, dsname, "&type=DATASET&#{query}")
      if response.code == 201
        x += 1
      end
    end
  end

  def create_dir(project, path, query)
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=create#{query}"
  end

  def create_random_dataset(project, searchable, generate_readme)
    dsname = "dataset_#{short_random_id}"
    query = URI.encode_www_form({description: "test dataset", searchable: searchable, generate_readme: generate_readme})
    create_dir(project, dsname, "&#{query}")
    expect_status(201)
    return dsname
  end

  def copy_dataset(project, path, destination_path, datasetType)
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=copy&destination_path=#{destination_path}#{datasetType}"
  end

  def move_dataset(project, path, destination_path, datasetType)
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=move&destination_path=#{destination_path}#{datasetType}"
  end

  def share_dataset(project, path, target_project, datasetType)
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=share&target_project=#{target_project}#{datasetType}"
  end

  def accept_dataset(project, path, datasetType)
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=accept#{datasetType}"
  end

  def reject_dataset(project, path, datasetType)
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=reject#{datasetType}"
  end

  def zip_dataset(project, path, datasetType)
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=zip#{datasetType}"
  end

  def unzip_dataset(project, path, datasetType)
    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=unzip#{datasetType}"
  end

  def delete_dataset(project, path, datasetType)
    delete "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}#{datasetType}"
  end

  def delete_corrupted_dataset(project, path, datasetType)
    delete "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=corrupted#{datasetType}"
  end

  def unshare_dataset(project, path, datasetType)
    delete "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=unshare#{datasetType}"
  end

  def get_download_token(project, path, datasetType)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/download/token/#{path}#{datasetType}"
  end

  def download_dataset_with_token(project, path, token, datasetType)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/download/#{path}?token=#{token}#{datasetType}"
  end

  def download_dataset(project, path, datasetType)
    get_download_token(project, path, "?#{datasetType}")
    expect_status(200)
    token = json_body[:data][:value]
    download_dataset_with_token(project, path, token, "&#{datasetType}")
  end

  def get_datasets_in_path(project, path, query)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=listing&expand=inodes#{query}"
  end

  def get_dataset_stat(project, path, datasetType)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=stat&expand=inodes#{datasetType}"
  end

  def get_dataset_blob(project, path, datasetType)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=blob&expand=inodes&mode=head#{datasetType}"
  end

  def update_dataset_permissions(project, path, permissions, datasetType)
    put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=permission&permissions=#{permissions}#{datasetType}"
  end

  def update_dataset_description(project, path, description, datasetType)
    put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/#{path}?action=description&description=#{description}#{datasetType}"
  end

  def attach_template(project, templateId, inodePath)
    template = {}
    template[:templateId] = templateId
    template[:inodePath] = inodePath
    post "#{ENV['HOPSWORKS_API']}/metadata/#{project[:id]}/attachTemplate/", template
  end

  def fetch_template(project, inodeid)
    get "#{ENV['HOPSWORKS_API']}/metadata/#{project[:id]}/fetchtemplatesforinode/#{inodeid}"
  end

  def detach_template(project, inodeid, templateid)
    get "#{ENV['HOPSWORKS_API']}/metadata/#{project[:id]}/detachtemplate/#{inodeid}/#{templateid}"
  end

  def test_sort_by_str(project, datasets, path, sort_by, order, sort_by_query)
    ds = datasets.map { |o| "#{o[:"#{sort_by}"]}" }
    if order == 'asc'
      sorted = ds.sort_by(&:downcase)
    else
      sorted = ds.sort_by(&:downcase).reverse
    end
    get_datasets_in_path(project, path, "&sort_by=#{sort_by_query}:#{order}")
    expect_status(200)
    sortedRes = json_body[:items].map { |o| "#{o[:"#{sort_by}"]}" }
    expect(sortedRes).to eq(sorted)
  end

  def test_sort_by_str_attr(project, datasets, path, sort_by, order, sort_by_query)
    ds = datasets.map { |o| "#{o[:attributes][:"#{sort_by}"]}" }
    if order == 'asc'
      sorted = ds.sort_by(&:downcase)
    else
      sorted = ds.sort_by(&:downcase).reverse
    end
    get_datasets_in_path(project, path, "&sort_by=#{sort_by_query}:#{order}")
    expect_status(200)
    sortedRes = json_body[:items].map { |o| "#{o[:attributes][:"#{sort_by}"]}" }
    expect(sortedRes).to eq(sorted)
  end

  def test_sort_by(project, datasets, path, sort_by, order, sort_by_query)
    ds = datasets.map { |o| "#{o[:"#{sort_by}"]}" }
    if order == 'asc'
      sorted = ds.sort
    else
      sorted = ds.sort.reverse
    end
    get_datasets_in_path(project, path, "&sort_by=#{sort_by_query}:#{order}")
    expect_status(200)
    sortedRes = json_body[:items].map { |o| "#{o[:"#{sort_by}"]}" }
    expect(sortedRes).to eq(sorted)
  end

  def test_sort_by_attr(project, datasets, path, sort_by, order, sort_by_query)
    ds = datasets.map { |o| "#{o[:attributes][:"#{sort_by}"]}" }
    if order == 'asc'
      sorted = ds.sort
    else
      sorted = ds.sort.reverse
    end
    get_datasets_in_path(project, path, "&sort_by=#{sort_by_query}:#{order}")
    expect_status(200)
    sortedRes = json_body[:items].map { |o| "#{o[:attributes][:"#{sort_by}"]}" }
    expect(sortedRes).to eq(sorted)
  end

  def test_sort_by_datasetType(project, datasets, path, sort_by, order, sort_by_query)
    datasetTypes = {}
    datasetTypes[:DATASET] = 0
    datasetTypes[:HIVEDB] = 1
    datasetTypes[:FEATURESTORE] = 2
    ds = datasets.map { |o| "#{o[:"#{sort_by}"]}"}
    if order == 'asc'
      sorted = ds.sort do |a, b|
        datasetTypes[:"#{a}"] <=> datasetTypes[:"#{b}"]
      end
    else
      sorted = ds.sort do |a, b|
        datasetTypes[:"#{b}"] <=> datasetTypes[:"#{a}"]
      end
    end
    get_datasets_in_path(project, path, "&sort_by=#{sort_by_query}:#{order}")
    expect_status(200)
    sortedRes = json_body[:items].map { |o| "#{o[:"#{sort_by}"]}"}
    expect(sortedRes).to eq(sorted)
  end

  def test_filter_by(project, excluded, path, filter_by, filter_by_query)
    get_datasets_in_path(project, path, "&filter_by=#{filter_by_query}")
    expect_status(200)
    filteredRes = json_body[:items].map { |o| "#{o[:"#{filter_by}"]}" }
    expect(filteredRes & excluded).to be_empty
  end

  def test_filter_by_attr(project, excluded, path, filter_by, filter_by_query)
    get_datasets_in_path(project, path, "&filter_by=#{filter_by_query}")
    expect_status(200)
    filteredRes = json_body[:items].map { |o| "#{o[:attributes][:"#{filter_by}"]}" }
    expect(filteredRes & excluded).to be_empty
  end

  def test_filter_by_starts_with(project, datasets, path, filter_by, filter_by_query, filter_val)
    ds = datasets.map { |o| "#{o[:id]}" if o[:"#{filter_by}"].start_with?("#{filter_val}")}.compact
    sorted = ds.sort
    get_datasets_in_path(project, path, "&sort_by=id:asc&filter_by=#{filter_by_query}:#{filter_val}")
    expect_status(200)
    filteredRes = json_body[:items].map { |o| "#{o[:id]}" }
    expect(filteredRes).to eq(sorted)
  end

  def test_filter_by_starts_with_attr(project, datasets, path, filter_by, filter_by_query, filter_val)
    if path == ""
      ds = datasets.map { |o| "#{o[:id]}" if o[:attributes][:"#{filter_by}"].start_with?("#{filter_val}")}.compact
    else
      ds = datasets.map { |o| "#{o[:attributes][:id]}" if o[:attributes][:"#{filter_by}"].start_with?("#{filter_val}")}.compact
    end
    sorted = ds.sort
    get_datasets_in_path(project, path, "&sort_by=id:asc&filter_by=#{filter_by_query}:#{filter_val}")
    expect_status(200)
    if path == ""
      filteredRes = json_body[:items].map { |o| "#{o[:id]}" }
    else
      filteredRes = json_body[:items].map { |o| "#{o[:attributes][:id]}" }
    end
    expect(filteredRes).to eq(sorted)
  end

  def test_filter_by_eq(project, datasets, path, filter_by, filter_by_query, filter_val)
    ds = datasets.map { |o| "#{o[:id]}" if o[:"#{filter_by}"]==filter_val}.compact
    sorted = ds.sort
    get_datasets_in_path(project, path, "&sort_by=id:asc&filter_by=#{filter_by_query}:#{filter_val}")
    expect_status(200)
    filteredRes = json_body[:items].map { |o| "#{o[:id]}" }
    expect(filteredRes).to eq(sorted)
  end

  def test_filter_by_eq_attr(project, datasets, path, filter_by, filter_by_val, filter_by_query, filter_by_query_val)
    if path == ""
      ds = datasets.map { |o| "#{o[:id]}" if o[:attributes][:"#{filter_by}"]==filter_by_val}.compact
    else
      ds = datasets.map { |o| "#{o[:attributes][:id]}" if o[:attributes][:"#{filter_by}"]==filter_by_val}.compact
    end
    sorted = ds.sort
    get_datasets_in_path(project, path, "&sort_by=id:asc&filter_by=#{filter_by_query}:#{filter_by_query_val}")
    expect_status(200)
    if path == ""
      filteredRes = json_body[:items].map { |o| "#{o[:id]}" }
    else
      filteredRes = json_body[:items].map { |o| "#{o[:attributes][:id]}" }
    end
    expect(filteredRes).to eq(sorted)
  end

  def test_filter_by_lt(project, datasets, path, filter_by, filter_by_query, filter_val)
    ds = datasets.map { |o| "#{o[:id]}" if o[:"#{filter_by}"]<filter_val}.compact
    sorted = ds.sort
    get_datasets_in_path(project, path, "&sort_by=id:asc&filter_by=#{filter_by_query}:#{filter_val}")
    expect_status(200)
    filteredRes = []
    if json_body[:items]
      filteredRes = json_body[:items].map { |o| "#{o[:id]}" }
    end
    expect(filteredRes).to eq(sorted)
  end

  def test_filter_by_lt_attr(project, datasets, path, filter_by, filter_by_val, filter_by_query, filter_by_query_val)
    if path == ""
      ds = datasets.map { |o| "#{o[:id]}" if o[:attributes][:"#{filter_by}"]<filter_by_val}.compact
    else
      ds = datasets.map { |o| "#{o[:attributes][:id]}" if o[:attributes][:"#{filter_by}"]<filter_by_val}.compact
    end
    sorted = ds.sort
    get_datasets_in_path(project, path, "&sort_by=id:asc&filter_by=#{filter_by_query}:#{filter_by_query_val}")
    expect_status(200)
    filteredRes = []
    if json_body[:items]
      if path == ""
        filteredRes = json_body[:items].map { |o| "#{o[:id]}" }
      else
        filteredRes = json_body[:items].map { |o| "#{o[:attributes][:id]}" }
      end
    end
    expect(filteredRes).to eq(sorted)
  end

  def test_filter_by_gt(project, datasets, path, filter_by, filter_by_query, filter_val)
    ds = datasets.map { |o| "#{o[:id]}" if o[:"#{filter_by}"]>filter_val}.compact
    sorted = ds.sort
    get_datasets_in_path(project, path, "&sort_by=id:asc&filter_by=#{filter_by_query}:#{filter_val}")
    expect_status(200)
    filteredRes = []
    if json_body[:items]
      filteredRes = json_body[:items].map { |o| "#{o[:id]}" }
    end
    expect(filteredRes).to eq(sorted)
  end

  def test_filter_by_gt_attr(project, datasets, path, filter_by, filter_by_val, filter_by_query, filter_by_query_val)
    if path == ""
      ds = datasets.map { |o| "#{o[:id]}" if o[:attributes][:"#{filter_by}"]>filter_by_val}.compact
    else
      ds = datasets.map { |o| "#{o[:attributes][:id]}" if o[:attributes][:"#{filter_by}"]>filter_by_val}.compact
    end
    sorted = ds.sort
    get_datasets_in_path(project, path, "&sort_by=id:asc&filter_by=#{filter_by_query}:#{filter_by_query_val}")
    expect_status(200)
    filteredRes = []
    if json_body[:items]
      if path == ""
        filteredRes = json_body[:items].map { |o| "#{o[:id]}" }
      else
        filteredRes = json_body[:items].map { |o| "#{o[:attributes][:id]}" }
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
    ds = datasets.map { |o| "#{o[:id]}"}
    sorted = ds.sort
    get_datasets_in_path(project, path, "&sort_by=id:asc&limit=#{limit}&offset=#{offset}")
    expect_status(200)
    filteredRes = json_body[:items].map { |o| "#{o[:id]}" }
    offset, limit = check_offset_limit(offset, limit, sorted.length)
    expect(filteredRes).to eq(sorted.drop(offset).take(limit))
  end

  def test_offset_limit_attr(project, datasets, path, offset, limit)
    if path == ""
      ds = datasets.map { |o| "#{o[:id]}"}
    else
      ds = datasets.map { |o| "#{o[:attributes][:id]}"}
    end
    sorted = ds.sort
    get_datasets_in_path(project, path, "&sort_by=id:asc&limit=#{limit}&offset=#{offset}")
    expect_status(200)
    if path == ""
      filteredRes = json_body[:items].map { |o| "#{o[:id]}" }
    else
      filteredRes = json_body[:items].map { |o| "#{o[:attributes][:id]}" }
    end
    offset, limit = check_offset_limit(offset, limit, sorted.length)
    expect(filteredRes).to eq(sorted.drop(offset).take(limit))
  end

end
