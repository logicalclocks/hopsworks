# coding: utf-8
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

describe "On #{ENV['OS']}" do
  before :all do
    @debugOpt = false
  end

  after(:all) {
    clean_all_test_projects(spec: "dataset")
  }
  describe 'dataset' do
    before(:all) {
      setVar("download_allowed", "true")
    }
    describe "#create" do
      context 'without authentication' do
        before :all do
          with_valid_project
          reset_session
        end
        it "should fail" do
          dsname = "dataset_#{short_random_id}"
          query = URI.encode_www_form({description: "test dataset", searchable: true, generate_readme: true})
          create_dir(@project, dsname, query: "&#{query}")
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end

      context 'with authentication' do
        before :all do
          with_valid_project
        end
        it 'should work with valid params' do
          dsname = "dataset_#{short_random_id}"
          query = URI.encode_www_form({description: "test dataset", searchable: true, generate_readme: true})
          create_dir(@project, dsname, query: "&#{query}")
          expect_status_details(201)
          get_dataset_stat(@project, dsname, datasetType: "&type=DATASET")
          expect_status(200)
          ds = json_body
          expect(ds[:description]).to eq ("test dataset")
          expect(ds[:attributes][:owner]).to eq ("#{@user[:fname]} #{@user[:lname]}")
          expect(ds[:permission]).to eq ("READ_ONLY")
          get_datasets_in_path(@project, dsname, query: "&type=DATASET")
          ds = json_body[:items].detect { |d| d[:attributes][:name] == "README.md" }
          expect(ds).to be_present
        end

        it 'should work with valid params and no README.md' do
          dsname = create_random_dataset(@project, true, false)
          get_dataset_stat(@project, dsname, datasetType: "&type=DATASET")
          expect_status(200)
          ds = json_body
          expect(ds[:description]).to eq ("test dataset")
          expect(ds[:attributes][:owner]).to eq ("#{@user[:fname]} #{@user[:lname]}")
          expect(ds[:permission]).to eq ("READ_ONLY")
          get_datasets_in_path(@project, dsname, query: "&type=DATASET")
          expect(json_body[:count]).to be == 0
          expect_status(200)
        end

        it 'should fail to create a dataset with space in the name' do
          query = URI.encode_www_form({description: "test dataset", searchable: true, generate_readme: true})
          create_dir(@project, "test%20dataset", query: "&#{query}")
          expect_json(errorCode: 110028)
          expect_status(400)
        end

        it 'should create a folder with space in the name' do
          dataset = "dataset_#{short_random_id}"
          create_dir(@project, dataset, query: "&type=DATASET")
          expect_status(201)
          dirname = "#{dataset}/test%20dir"
          create_dir(@project, dirname, query: "&type=DATASET")
          expect_status(201)
          get_dataset_stat(@project, dirname, datasetType: "&type=DATASET")
          expect_status(200)
        end

        it 'should fail to create a dataset with Ö in the name' do
          query = URI.encode_www_form({description: "test dataset", searchable: true, generate_readme: true})
          create_dir(@project, "test%C3%96jdataset", query: "&#{query}")
          expect_json(errorCode: 110028)
          expect_status(400)
        end

        it 'should fail to create a dataset with a name that ends with a .' do
          query = URI.encode_www_form({description: "test dataset.", searchable: true, generate_readme: true})
          create_dir(@project, "testdot.", query: "&#{query}")
          expect_json(errorCode: 110028)
          expect_status(400)
        end

        it 'Logs dataset should have HOT storage policy' do
          logs_storage_policy = get_storage_policy("/Projects/#{@project[:projectname]}/Logs")
          expect(logs_storage_policy).to include("HOT")
        end

        it '/Project dir should have DB storage policy' do
          projects_storage_policy = get_storage_policy("/Projects/")
          expect(projects_storage_policy).to include("DB")
        end
      end
    end
    describe "#access" do
      context 'without authentication' do
        before :all do
          with_valid_project
          reset_session
        end
        it "should fail to get dataset list" do
          get_datasets_in_path(@project, '')
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end
      context 'with authentication' do
        before :all do
          with_valid_project
        end
        it "should return dataset list" do
          get_datasets_in_path(@project, '')
          expect(json_body[:count]).to be > 5
          expect_status(200)
        end
        it "should fail to return dataset list from Projects if path contains ../" do
          get_datasets_in_path(@project, 'Logs/../../../Projects', query: "&type=DATASET")
          expect_status(400)
          expect_json(errorCode: 110011)
          reset_session
        end
        it "should fail to check if a dataset is a dir for a project if path contains ../" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email], "Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          get_dataset_stat(project1, "Logs/../../../Projects/#{project[:projectname]}/Logs/README.md", datasetType:
              "&type=DATASET")
          expect_status(400)
          expect_json(errorCode: 110011)
          reset_session
        end

        it "should fail to upload file to a project if path contains ../" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email], "Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          file = URI.encode_www_form({flowChunkNumber: 1, flowChunkSize: 1048576,
                                      flowCurrentChunkSize: 3195, flowTotalSize: 3195,
                                      flowIdentifier: "3195-someFiletxt", flowFilename: "someFile.txt",
                                      flowRelativePath: "someFile.txt", flowTotalChunks: 1})
          get "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/dataset/upload/Logs/../../../Projects/#{project[:projectname]}/Logs/?#{file}", {content_type: "multipart/form-data"}
          expect_status(400)
          expect_json(errorCode: 110011)
          reset_session
        end
        it "should fail to return dataset list from Projects if path contains .." do
          project = get_project
          newUser = create_user
          create_session(newUser[:email], "Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          get_datasets_in_path(project1, "Logs/../../../Projects/#{project[:projectname]}/", query: "&type=DATASET")
          expect_status(400)
          expect_json(errorCode: 110011)
          reset_session
        end
        it "should fail to return dataset list from Projects if path contains ../../Projects/../../Projects" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email], "Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          get_datasets_in_path(project1, "Logs/../../Projects/../../Projects/#{project[:projectname]}/", query: "&type=DATASET")
          expect_status(400)
          expect_json(errorCode: 110011)
          reset_session
        end
        it "should fail to return file from other Projects if path contains .." do
          project = get_project
          newUser = create_user
          create_session(newUser[:email], "Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          get_dataset_blob(project1, "Logs/../../../Projects/#{project[:projectname]}/Logs/README.md",
                           datasetType: "&type=DATASET")
          expect_status(400)
          expect_json(errorCode: 110011)
          reset_session
        end
        it "should fail to check if file exists from another project if path contains ../" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email], "Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          get_dataset_stat(project1, "Logs/../../../Projects/#{project[:projectname]}/Logs/README.md", datasetType:
              "&type=DATASET")
          expect_status(400)
          expect_json(errorCode: 110011)
          reset_session
        end

        it "should fail to download a file from another project if path contains ../" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email], "Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          get_download_token(project1, "Logs/../../../Projects/#{project[:projectname]}/Logs/README.md",
                             datasetType: "&type=DATASET")
          expect_status(400)
          expect_json(errorCode: 110011)
          reset_session
        end
      end
    end
    describe "#upload" do
      context 'without authentication' do
        before :all do
          with_valid_project
          reset_session
        end
        it "should fail to upload" do
          project = get_project
          uploadFile(project, "Logs", "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end
      context 'with authentication but insufficient privilege' do
        before :all do
          with_valid_project
        end
        it "should fail to upload to a dataset with permission owner only if Data scientist" do
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          member = create_user
          add_member_to_project(@project, member[:email], "Data scientist")
          create_session(member[:email], "Pass123")
          uploadFile(@project, dsname, "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_json(errorCode: 200002)
          expect_status(403)
          reset_session
        end
        it "should fail to upload to a shared dataset with permission read only" do
          with_valid_project
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          request_access(@project, ds, project)
          share_dataset(@project, dsname, project[:projectname], permission: "READ_ONLY")
          uploadFile(project, "#{@project[:projectname]}::#{dsname}", "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_json(errorCode: 200002)
          expect_status(403)
        end
      end
      context 'with authentication and sufficient privilege' do
        before :all do
          with_valid_project
        end
        it "should upload file" do
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          uploadFile(@project, dsname, "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_status(204)
        end
        it "should upload to a shared dataset with permission group writable." do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          request_access(@project, ds, project)
          share_dataset(@project, dsname, project[:projectname], permission: "EDITABLE")
          update_dataset_permissions(@project, dsname, "EDITABLE", datasetType: "&type=DATASET")
          uploadFile(project, "#{@project[:projectname]}::#{dsname}", "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_status(204)
        end
        it "should upload to a dataset with permission owner only if Data owner" do
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "EDITABLE")
          member = create_user
          add_member_to_project(@project, member[:email], "Data owner")
          create_session(member[:email], "Pass123")
          uploadFile(@project, dsname, "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_status(204)
          reset_session
        end
      end
    end
    describe "#delete" do
      context 'without authentication' do
        before :all do
          with_valid_project
          reset_session
        end
        it "should fail to delete dataset" do
          project = get_project
          delete_dataset(project, "Logs", datasetType: "?type=DATASET")
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end
      context 'with authentication but insufficient privilege' do
        before :all do
          with_valid_project
        end
        it "should fail to delete dataset with insufficient privilege" do
          project = get_project
          member = create_user
          add_member_to_project(project, member[:email], "Data scientist")
          create_session(member[:email], "Pass123")
          delete_dataset(project, "Logs", datasetType: "?type=DATASET")
          expect_status(403)
          expect_json(errorCode: 110050)
          reset_session
        end

        it "should fail to delete dataset in another project with .. in path" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email], "Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          delete_dataset(project1, "Logs/../../../Projects/#{project[:projectname]}/Logs/README.md", datasetType: "?type=DATASET")
          expect_status(400)
          expect_json(errorCode: 110011)
          expect(test_file("/Projects/#{project[:projectname]}/Logs/README.md")).to eq(true)
          reset_session
        end

        it "should fail to delete corrupted(owned by glassfish and size=0) file in another project with .. in path" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email], "Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          hopsworks_user = getHopsworksUser()
          touchz("/Projects/#{project[:projectname]}/Logs/corrupted.txt", hopsworks_user, hopsworks_user)
          delete_corrupted_dataset(project1, "Logs/../../../Projects/#{project[:projectname]}/Logs/corrupted.txt", datasetType: "&type=DATASET")
          expect_status(400)
          expect_json(errorCode: 110011) #DataSet not found. /Projects/#{project[:projectname]}/Logs
          reset_session
        end
      end
      context 'with authentication and sufficient privilege' do
        before :all do
          with_valid_project
        end
        it "should delete dataset" do
          delete_dataset(@project, "Logs", datasetType: "?type=DATASET")
          expect_status(204)
        end
        it "should create-delete-create dataset" do
          dsname = create_random_dataset(@project, true, false)
          delete_dataset(@project, dsname)
          create_dataset_by_name_checked(@project, dsname)
        end
      end
    end
    describe "#request" do
      context 'without authentication' do
        before :all do
          with_valid_project
          with_valid_dataset
        end
        it "should fail to send request" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          get_dataset_stat(@project, @dataset[:inode_name], datasetType: "&type=DATASET")
          expect_status(200)
          dataset = json_body
          reset_session
          request_dataset_access(project, dataset[:attributes][:id])
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end
      context 'with authentication' do
        before :all do
          with_valid_project
          with_valid_dataset
        end
        it "should send request" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          request_access(@project, @dataset, project)
          expect_status(200)
          create_session(@project[:username], "Pass123") # be the user of the project that owns the dataset
          get "#{ENV['HOPSWORKS_API']}/message"
          msg = json_body.detect { |e| e[:content].include? "Dataset name: #{@dataset[:inode_name]}" }
          expect(msg).not_to be_nil
        end
        it "should fail to send request to the same project" do
          get_dataset_stat(@project, @dataset[:inode_name], datasetType: "&type=DATASET")
          expect_status(200)
          dataset = json_body
          request_dataset_access(@project, dataset[:attributes][:id])
          expect_status(400)
        end
      end
    end
    describe "#share" do
      context 'without authentication' do
        before :all do
          with_valid_project
          reset_session
        end
        it "should fail to share dataset" do
          project = get_project
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          create_session(project[:username], "Pass123")
          create_dataset_by_name_checked(project, dsname, permission: "READ_ONLY")
          reset_session
          share_dataset(project, dsname, project1[:name], permission: "EDITABLE", datasetType: "&type=DATASET")
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end
      context 'with authentication but insufficient privilege' do
        before :all do
          with_valid_project
          reset_session
        end
        it "should fail to share dataset" do
          project = get_project
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          create_session(project[:username], "Pass123")
          create_dataset_by_name_checked(project, dsname, permission: "READ_ONLY")
          member = create_user
          add_member_to_project(project, member[:email], "Data scientist")
          create_session(member[:email], "Pass123")
          share_dataset(project, dsname, project1[:name], permission: "EDITABLE", datasetType: "&type=DATASET")
          expect_status(403)
        end
      end
      context 'with authentication and sufficient privilege' do
        before :each do
          check_project_limit
          with_valid_project
        end

        it "should share dataset" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          share_dataset(@project, dsname, project[:projectname], permission: "EDITABLE", datasetType: "&type=DATASET")
          expect_status(204)
        end
        it "should share a HiveDB" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          share_dataset(@project, "#{@project[:projectname].downcase}.db", project[:projectname],
                        permission: "EDITABLE", datasetType: "&type=HIVEDB")
          get_dataset_stat(project, "#{@project[:projectname]}::#{@project[:projectname].downcase}.db", datasetType:
              "&type=HIVEDB")
          shared_ds = json_body
          expect("#{shared_ds[:name]}").to eq("#{@project[:projectname]}::#{@project[:projectname].downcase}.db")
        end
        it "should appear as pending for datasets not requested" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          share_dataset(@project, dsname, project[:projectname], permission: "EDITABLE", datasetType: "&type=DATASET")
          get_dataset_stat(project, "#{@project[:projectname]}::#{dsname}", datasetType: "&type=DATASET")
          shared_ds = json_body
          expect(shared_ds[:accepted]).to be false
        end
        it "should appear as accepted for requested datasets" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          request_access(@project, ds, project)
          share_dataset(@project, dsname, project[:projectname], permission: "EDITABLE")
          get_dataset_stat(project, "#{@project[:projectname]}::#{dsname}", datasetType: "&type=DATASET")
          shared_ds = json_body
          expect(shared_ds[:accepted]).to be true
        end
        it "should fail to make a shared dataset editable" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          permissions = "EDITABLE"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          share_dataset(@project, dsname, project[:projectname], permission: "EDITABLE")
          update_dataset_permissions(project, "#{@project[:projectname]}::#{dsname}", permissions, datasetType: "&type=DATASET")
          expect_status(400)
        end
        it "should fail to write on a non editable shared dataset" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          share_dataset(@project, dsname, project[:projectname], permission: "READ_ONLY")
          # Accept dataset
          accept_dataset(project, "#{@project[:projectname]}::#{dsname}", datasetType: "&type=DATASET")
          # try to write
          create_dir(project, "#{@project[:projectname]}::#{dsname}/testdir", query: "&type=DATASET")
          expect_status(403)
        end
        it "should write in an editable shared dataset" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          permissions = "EDITABLE"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          # Make the dataset editable
          update_dataset_permissions(@project, dsname, permissions, datasetType: "&type=DATASET")
          # share it
          share_dataset(@project, dsname, project[:projectname], permission: "EDITABLE", datasetType: "&type=DATASET")
          # Accept dataset
          accept_dataset(project, "#{@project[:projectname]}::#{dsname}", datasetType: "&type=DATASET")
          # Create a directory - from the "target" project
          create_dir(project, "#{@project[:projectname]}::#{dsname}/testdir", query: "&type=DATASET")
          expect_status(201)
          # Check if the directory is present
          get_datasets_in_path(@project, dsname, query: "&type=DATASET")
          ds = json_body[:items].detect { |d| d[:attributes][:name] == "testdir" }
          expect(ds).to be_present
        end

        it "should be able to see content shared dataset with a name that already exist in the target project" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          create_dataset_by_name_checked(project, dsname, permission: "READ_ONLY")
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          request_dataset_access(project, ds[:inode_id])
          share_dataset(@project, dsname, project[:projectname], permission: "EDITABLE", datasetType: "&type=DATASET")
          get_datasets_in_path(project, "#{@project[:projectname]}::#{dsname}")
          expect_status(200)
          get_datasets_in_path(project, dsname)
          expect_status(200)
        end

        it "when adding a new member it should add it also to the shared datasets" do
          project = get_project

          # Create a second project and a dataset in it
          projectname = "project_#{short_random_id}"
          second_project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          create_dataset_by_name_checked(second_project, dsname, permission: "READ_ONLY")

          # Share the dataset with the first project
          share_dataset(second_project, dsname, project[:projectname], permission: "EDITABLE",
                        datasetType: "&type=DATASET")
          accept_dataset(project, "#{second_project[:projectname]}::#{dsname}", datasetType: "&type=DATASET")

          # Create a new user and add it only to the first project
          member = create_user
          add_member_to_project(project, member[:email], "Data scientist")
          create_session(member[:email], "Pass123")

          # The new member should be able to fetch the readme in the readme
          readme = get_dataset_blob(project, "Projects/#{second_project[:projectname]}/#{dsname}/README.md", datasetType: "&type=DATASET")
          expect_status(200)
          readme_parsed = JSON.parse(readme)
          expect(readme_parsed['preview']).not_to be_nil
        end
        it "should share training and statistic dataset when sharing requested feature store" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          featurestore = "#{@project[:projectname].downcase}_featurestore.db"
          trainingDataset =  "#{@project[:projectname]}_Training_Datasets"
          statisticsDataset =  "Statistics"
          get_dataset_stat(@project, featurestore,  datasetType: "&type=FEATURESTORE")
          ds = json_body
          request_dataset_access(project, ds[:attributes][:id])
          share_dataset(@project, featurestore, project[:projectname], permission: "EDITABLE", datasetType:
              "&type=FEATURESTORE")
          get_dataset_stat(project, "#{@project[:projectname]}::#{trainingDataset}", datasetType: "&type=DATASET")
          expect_status(200)
          get_dataset_stat(project, "#{@project[:projectname]}::#{statisticsDataset}", datasetType: "&type=DATASET")
          expect_status(200)
        end
        it "should share training and statistics dataset when accepting non requested feature store" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          featurestore = "#{@project[:projectname].downcase}_featurestore.db"
          trainingDataset =  "#{@project[:projectname]}_Training_Datasets"
          statisticsDataset =  "Statistics"
          share_dataset(@project, featurestore, project[:projectname], permission: "EDITABLE", datasetType:
              "&type=FEATURESTORE")
          accept_dataset(project, "#{@project[:projectname]}::#{featurestore}", datasetType: "&type=FEATURESTORE")
          get_dataset_stat(project, "#{@project[:projectname]}::#{trainingDataset}", datasetType: "&type=DATASET")
          expect_status(200)
          get_dataset_stat(project, "#{@project[:projectname]}::#{statisticsDataset}", datasetType: "&type=DATASET")
          expect_status(200)
        end
        it "should not share training and statistics dataset when sharing non requested feature store" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          featurestore = "#{@project[:projectname].downcase}_featurestore.db"
          trainingDataset =  "#{@project[:projectname]}_Training_Datasets"
          statisticsDataset =  "Statistics"
          share_dataset(@project, featurestore, project[:projectname], permission: "EDITABLE", datasetType:
              "&type=FEATURESTORE")
          get_dataset_stat(project, "#{@project[:projectname]}::#{trainingDataset}", datasetType: "&type=DATASET")
          expect_status(400)
          get_dataset_stat(project, "#{@project[:projectname]}::#{statisticsDataset}", datasetType: "&type=DATASET")
          expect_status(400)
        end
        it "should accept pending training and statistics dataset when accepting feature store" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          featurestore = "#{@project[:projectname].downcase}_featurestore.db"
          trainingDataset =  "#{@project[:projectname]}_Training_Datasets"
          statisticsDataset =  "Statistics"
          share_dataset(@project, trainingDataset, project[:projectname], permission: "EDITABLE", datasetType:
              "&type=DATASET")
          share_dataset(@project, statisticsDataset, project[:projectname], permission: "EDITABLE", datasetType:
              "&type=DATASET")
          share_dataset(@project, featurestore, project[:projectname], permission: "EDITABLE", datasetType:
              "&type=FEATURESTORE")
          accept_dataset(project, "#{@project[:projectname]}::#{featurestore}", datasetType: "&type=FEATURESTORE")
          get_dataset_stat(project, "#{@project[:projectname]}::#{trainingDataset}", datasetType: "&type=DATASET")
          expect_status(200)
          get_dataset_stat(project, "#{@project[:projectname]}::#{statisticsDataset}", datasetType: "&type=DATASET")
          expect_status(200)
        end
        it "should ignore shared training and statistics dataset when accepting feature store" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          featurestore = "#{@project[:projectname].downcase}_featurestore.db"
          trainingDataset =  "#{@project[:projectname]}_Training_Datasets"
          statisticsDataset =  "Statistics"
          share_dataset(@project, trainingDataset, project[:projectname], permission: "EDITABLE", datasetType:
              "&type=DATASET")
          accept_dataset(project, "#{@project[:projectname]}::#{trainingDataset}", datasetType: "&type=DATASET")
          get_dataset_stat(project, "#{@project[:projectname]}::#{trainingDataset}", datasetType: "&type=DATASET")
          expect_status(200)
          share_dataset(@project, statisticsDataset, project[:projectname],permission: "EDITABLE", datasetType:
              "&type=DATASET")
          accept_dataset(project, "#{@project[:projectname]}::#{statisticsDataset}", datasetType: "&type=DATASET")
          get_dataset_stat(project, "#{@project[:projectname]}::#{statisticsDataset}", datasetType: "&type=DATASET")
          expect_status(200)
          share_dataset(@project, featurestore, project[:projectname], permission: "EDITABLE", datasetType:
              "&type=FEATURESTORE")
          accept_dataset(project, "#{@project[:projectname]}::#{featurestore}", datasetType: "&type=FEATURESTORE")
          expect_status(204)
          get_dataset_stat(project, "#{@project[:projectname]}::#{trainingDataset}", datasetType: "&type=DATASET")
          expect_status(200)
          get_dataset_stat(project, "#{@project[:projectname]}::#{statisticsDataset}", datasetType: "&type=DATASET")
          expect_status(200)
        end
        it "should unshare training and statistics dataset when unsharing feature store" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          featurestore = "#{@project[:projectname].downcase}_featurestore.db"
          trainingDataset =  "#{@project[:projectname]}_Training_Datasets"
          statisticsDataset =  "Statistics"
          share_dataset(@project, featurestore, project[:projectname], permission: "EDITABLE", datasetType:
              "&type=FEATURESTORE")
          accept_dataset(project, "#{@project[:projectname]}::#{featurestore}", datasetType: "&type=FEATURESTORE")
          get_dataset_stat(project, "#{@project[:projectname]}::#{trainingDataset}", datasetType: "&type=DATASET")
          expect_status(200)
          get_dataset_stat(project, "#{@project[:projectname]}::#{statisticsDataset}", datasetType: "&type=DATASET")
          expect_status(200)
          unshare_from(@project, featurestore, project[:projectname], datasetType: "&type=FEATURESTORE")
          get_dataset_stat(project, "#{@project[:projectname]}::#{trainingDataset}", datasetType: "&type=DATASET")
          expect_status(400)
          expect_json(errorCode: 110011)
          get_dataset_stat(project, "#{@project[:projectname]}::#{statisticsDataset}", datasetType: "&type=DATASET")
          expect_status(400)
          expect_json(errorCode: 110011)
        end
        it "should unshare training and statistics dataset when deleting feature store" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          featurestore = "#{@project[:projectname].downcase}_featurestore.db"
          trainingDataset =  "#{@project[:projectname]}_Training_Datasets"
          statisticsDataset =  "Statistics"
          share_dataset(@project, featurestore, project[:projectname], permission: "EDITABLE", datasetType:
              "&type=FEATURESTORE")
          accept_dataset(project, "#{@project[:projectname]}::#{featurestore}", datasetType: "&type=FEATURESTORE")
          get_dataset_stat(project, "#{@project[:projectname]}::#{trainingDataset}", datasetType: "&type=DATASET")
          expect_status(200)
          get_dataset_stat(project, "#{@project[:projectname]}::#{statisticsDataset}", datasetType: "&type=DATASET")
          expect_status(200)
          delete_dataset(project, "#{@project[:projectname]}::#{featurestore}", datasetType: "?type=FEATURESTORE")
          get_dataset_stat(project, "#{@project[:projectname]}::#{trainingDataset}", datasetType: "&type=DATASET")
          expect_status(400)
          get_dataset_stat(project, "#{@project[:projectname]}::#{statisticsDataset}", datasetType: "&type=DATASET")
          expect_status(400)
        end
      end
      context 'delete' do
        before :each do
          check_project_limit
          with_valid_project
        end
        it "should delete dataset shared with other project" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          request_access(@project, ds, project)
          share_dataset(@project, dsname, project[:projectname], permission: "EDITABLE")
          get_dataset_stat(project, "#{@project[:projectname]}::#{dsname}", datasetType: "&type=DATASET")
          expect_status(200)
          delete_dataset(@project, "#{dsname}", datasetType: "?type=DATASET")
          get_dataset_stat(project, "#{@project[:projectname]}::#{dsname}", datasetType: "&type=DATASET")
          expect_status(400)
        end
        it "should unshare a dataset and not delete the original" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          request_access(@project, ds, project)
          share_dataset(@project, dsname, project[:projectname], permission: "EDITABLE")
          get_dataset_stat(project, "#{@project[:projectname]}::#{dsname}", datasetType: "&type=DATASET")
          delete_dataset(project, "#{@project[:projectname]}::#{dsname}", datasetType: "?type=DATASET")
          get_dataset_stat(project, "#{@project[:projectname]}::#{dsname}", datasetType: "&type=DATASET")
          expect_status(400)
          get_dataset_stat(@project, "#{dsname}", datasetType: "&type=DATASET")
          expect_status(200)
        end
        it "should unshare from one project" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          projectname1 = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname1)
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          request_access(@project, ds, project)
          request_access(@project, ds, project1)
          share_dataset(@project, dsname, project[:projectname], permission: "EDITABLE")
          share_dataset(@project, dsname, project1[:projectname], permission: "EDITABLE")
          get_dataset_stat(project, "#{@project[:projectname]}::#{dsname}", datasetType: "&type=DATASET")
          expect_status(200)
          get_dataset_stat(project1, "#{@project[:projectname]}::#{dsname}", datasetType: "&type=DATASET")
          expect_status(200)
          unshare_dataset(@project, "#{dsname}", datasetType: "&type=DATASET&target_project=#{project[:projectname]}")
          get_dataset_stat(project, "#{@project[:projectname]}::#{dsname}", datasetType: "&type=DATASET")
          expect_status(400)
          get_dataset_stat(project1, "#{@project[:projectname]}::#{dsname}", datasetType: "&type=DATASET")
          expect_status(200)
        end
      end
    end

    describe "operations in shared dataset" do
      before :all do
        @project1 = create_project
        @project2 = create_project
      end

      context "read only" do
        before :all do
          @dsname = "dataset_#{short_random_id}"
          create_dataset_by_name_checked(@project1, @dsname)
          share_dataset_checked(@project1, @dsname, @project2[:projectname], permission: "READ_ONLY", datasetType: "DATASET")
          accept_dataset_checked(@project2, "#{@project1[:projectname]}::#{@dsname}", datasetType: "DATASET")
        end
        it "should fail to upload to a shared dataset with permission read only" do
          uploadFile(@project2, "#{@project1[:projectname]}::#{@dsname}", "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_status_details(403, error_code: 200002)
        end

        it "should fail to delete - absolute path" do
          dirname = "dir_#{short_random_id}"
          create_dir(@project1, "#{@dsname}/#{dirname}", query: "&type=DATASET")
          delete_dir(@project2,"/Projects/#{@project2[:projectname]}/#{@project1[:projectname]}::#{@dsname}/#{dirname}")
          expect_status_details(403, error_code: 110050)
        end
        it "should fail to delete - relative path" do
          dirname = "dir_#{short_random_id}"
          create_dir(@project1, "#{@dsname}/#{dirname}", query: "&type=DATASET")
          delete_dir(@project2,"#{@project1[:projectname]}::#{@dsname}/#{dirname}")
          expect_status_details(403, error_code: 110050)
        end
      end

      context "editable" do
        before :all do
          @dsname = "dataset_#{short_random_id}"
          create_dataset_by_name_checked(@project1, @dsname)
          share_dataset_checked(@project1, @dsname, @project2[:projectname], datasetType: "DATASET")
          accept_dataset_checked(@project2, "#{@project1[:projectname]}::#{@dsname}", datasetType: "DATASET")
          update_dataset_permissions_checked(@project1, @dsname, "EDITABLE")
        end
        it "should upload to a shared dataset with permission group writable." do
          uploadFile(@project2, "#{@project1[:projectname]}::#{@dsname}", "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_status_details(204)
        end

        it "should delete - absolute path" do
          dirname = "dir_#{short_random_id}"
          create_dir(@project1, "#{@dsname}/#{dirname}", query: "&type=DATASET")
          delete_dir(@project2,"/Projects/#{@project2[:projectname]}/#{@project1[:projectname]}::#{@dsname}/#{dirname}")
          expect_status_details(204)
        end
        it "should delete - relative path" do
          dirname = "dir_#{short_random_id}"
          create_dir(@project1, "#{@dsname}/testdir", query: "&type=DATASET")
          delete_dir(@project2,"#{@project1[:projectname]}::#{@dsname}/#{dirname}")
          expect_status_details(204)
        end
      end
    end
    describe "#publish-import" do
      context 'regular dataset' do
        before :all do
          with_valid_project
          with_valid_dataset
          expect(@dataset[:public_ds]).to eq false
          publish_dataset_checked(@project, @dataset[:inode_name], datasetType: "DATASET")
        end

        it 'same user - import dataset in parent project' do
          import_dataset(@project, @dataset[:inode_name], @project, datasetType: "DATASET")
          expect_status_details(400)
        end

        it 'same user - import dataset to a different project' do
          project1 = create_project
          import_dataset_checked(project1, @dataset[:inode_name], @project, datasetType: "DATASET")
        end

        it 'same user - import dataset twice to same project' do
          project1 = create_project
          import_dataset_checked(project1, @dataset[:inode_name], @project, datasetType: "DATASET")
          import_dataset(project1, @dataset[:inode_name], @project, datasetType: "DATASET")
          expect_status_details(400)
        end
      end

      context 'hive dataset' do
        before :all do
          with_valid_project
        end
        it 'same user - publish and import hive featurestore dataset' do
          featurestore_dataset_name = "#{@project[:projectname]}_featurestore.db".downcase
          featurestore_dataset = get_dataset(@project, featurestore_dataset_name)
          expect(featurestore_dataset[:public_ds]).to eq false
          publish_dataset_checked(@project, featurestore_dataset_name, datasetType: "FEATURESTORE")
          project1 = create_project
          import_dataset_checked(project1, featurestore_dataset_name, @project, datasetType: "FEATURESTORE")
        end
        it 'same user - publish and import hive db dataset' do
          hivedb_dataset_name = "#{@project[:projectname]}.db".downcase
          featurestore_dataset = get_dataset(@project, hivedb_dataset_name)
          expect(featurestore_dataset[:public_ds]).to eq false
          publish_dataset_checked(@project, hivedb_dataset_name, datasetType: "HIVEDB")
          project1 = create_project
          import_dataset_checked(project1, hivedb_dataset_name, @project, datasetType: "HIVEDB")
        end
      end

      context 'workflows' do
        it 'share-share-unshare-share' do
          with_valid_project
          with_valid_dataset
          expect(@dataset[:public_ds]).to eq false

          publish_dataset_checked(@project, @dataset[:inode_name], datasetType: "DATASET")
          #publishing a public dataset should succeede
          publish_dataset_checked(@project, @dataset[:inode_name], datasetType: "DATASET")
          unpublish_dataset_checked(@project, @dataset[:inode_name], datasetType: "DATASET")
          publish_dataset_checked(@project, @dataset[:inode_name], datasetType: "DATASET")
        end

        it 'workflow 1' do
          with_valid_project
          with_valid_dataset
          expect(@dataset[:public_ds]).to eq false

          project1 = create_project
          project2 = create_project
          publish_dataset_checked(@project, @dataset[:inode_name], datasetType: "DATASET")
          import_dataset_checked(project1, @dataset[:inode_name], @project, datasetType: "DATASET")
          import_dataset_checked(project2, @dataset[:inode_name], @project, datasetType: "DATASET")
          unpublish_dataset_checked(@project, @dataset[:inode_name], datasetType: "DATASET")
          #unpublishing a private dataset should succeede
          unpublish_dataset_checked(@project, @dataset[:inode_name], datasetType: "DATASET")
          check_shared_dataset(project1, @dataset[:inode_name], @project)
          check_shared_dataset(project2, @dataset[:inode_name], @project)
          unshare_all_dataset_checked(@project, @dataset[:inode_name], datasetType: "DATASET")
          check_not_shared_dataset(project1, @dataset[:inode_name], @project)
          check_not_shared_dataset(project2, @dataset[:inode_name], @project)
          #unsharing a private dataset should succeede
          unshare_all_dataset_checked(@project, @dataset[:inode_name], datasetType: "DATASET")
        end

        it 'workflow 2' do
          with_valid_project
          with_valid_dataset
          project1 = create_project
          project2 = create_project
          project3= create_project
          share_dataset(@project, @dataset[:inode_name], project1[:projectname], permission: "EDITABLE")
          accept_dataset(project1, "#{@project[:projectname]}::#{@dataset[:inode_name]}", datasetType: "&type=DATASET")
          expect_status_details(204)
          create_dir_checked(project1, "#{@project[:projectname]}::#{@dataset[:inode_name]}/test_#{short_random_id}",
                             query: "&type=DATASET")
          publish_dataset_checked(@project, @dataset[:inode_name], datasetType: "DATASET")
          create_dir(project1, "#{@project[:projectname]}::#{@dataset[:inode_name]}/test_#{short_random_id}", query: "&type=DATASET")
          expect_status_details(403)
          import_dataset_checked(project2, @dataset[:inode_name], @project)
          import_dataset_checked(project3, @dataset[:inode_name], @project)
          unpublish_dataset_checked(@project, @dataset[:inode_name], datasetType: "DATASET")
          update_dataset_shared_with_permissions(@project, @dataset[:inode_name], project1, "EDITABLE", datasetType: "&type=DATASET")
          update_dataset_shared_with_permissions(@project, @dataset[:inode_name], project2, "EDITABLE", datasetType: "&type=DATASET")
          create_dir_checked(project1, "#{@project[:projectname]}::#{@dataset[:inode_name]}/test_#{short_random_id}",
                             query: "&type=DATASET")
          create_dir_checked(project2, "#{@project[:projectname]}::#{@dataset[:inode_name]}/test_#{short_random_id}",
                             query: "&type=DATASET")
          unshare_dataset(@project, "#{@dataset[:inode_name]}", datasetType: "&type=DATASET&target_project=#{project1[:projectname]}")
          expect_status_details(204)
          unshare_all_dataset_checked(@project, @dataset[:inode_name])
          get_dataset_stat(project1, "#{@project[:projectname]}::#{@dataset[:inode_name]}", datasetType: "&type=DATASET")
          expect_status_details(400)
          get_dataset_stat(project3, "#{@project[:projectname]}::#{@dataset[:inode_name]}", datasetType: "&type=DATASET")
          expect_status_details(400)
        end
      end

      context 'permissions' do
        before :all do
          with_valid_project
          with_valid_dataset
        end
        it 'check correct editable reset on publishing dataset' do
          ds = get_dataset_stat_checked(@project, @dataset[:inode_name], datasetType: "&type=DATASET")
          expect(ds[:permission]).to eq("READ_ONLY"), "dataset should not be editable, found:#{ds[:permission]}"
          update_dataset_permissions(@project, @dataset[:inode_name], "EDITABLE", datasetType: "&type=DATASET")
          ds = get_dataset_stat_checked(@project, @dataset[:inode_name], datasetType: "&type=DATASET")
          expect(ds[:permission]).to eq("EDITABLE"), "dataset should be editable, found:#{ds[:permission]}"
          publish_dataset_checked(@project, @dataset[:inode_name], datasetType: "DATASET")
          ds = get_dataset_stat_checked(@project, @dataset[:inode_name], datasetType: "&type=DATASET")
          expect(ds[:permission]).to eq("READ_ONLY"), "dataset should not be editable, found:#{ds[:permission]}"
        end
        it 'check published dataset cannot be made editable' do
          publish_dataset_checked(@project, @dataset[:inode_name])
          ds = get_dataset_stat_checked(@project, @dataset[:inode_name], datasetType: "&type=DATASET")
          expect(ds[:permission]).to eq("READ_ONLY"), "dataset should not be editable, found:#{ds[:permission]}"
          update_dataset_permissions(@project, @dataset[:inode_name], "EDITABLE", datasetType: "&type=DATASET")
          expect_status_details(400)
        end
      end
    end
    describe "#permissions" do
      context 'with authentication and insufficient privileges' do
        before :all do
          with_valid_project
        end

        it "should fail to change permission as a data scientist" do
          project = get_project
          dsname = "dataset_#{short_random_id}"
          permissions = "EDITABLE"
          create_dataset_by_name_checked(project, dsname, permission: "READ_ONLY")
          member = create_user
          add_member_to_project(project, member[:email], "Data scientist")
          create_session(member[:email], "Pass123")
          update_dataset_permissions(project, dsname, permissions, datasetType: "&type=DATASET")
          expect_json(errorCode: 110050)
          expect_status(403)
        end
      end

      context 'with authentication and sufficient privileges' do
        before :all do
          with_valid_dataset
        end

        it "should make the dataset editable" do
          update_dataset_permissions(@project, @dataset[:inode_name], "EDITABLE", datasetType: "&type=DATASET")
          expect_status(200)
          # check for correct permissions
          get_dataset_stat(@project, @dataset[:inode_name], datasetType: "&type=DATASET")
          expect_status(200)
          ds = json_body
          expect(ds[:permission]).to eq ("EDITABLE")
        end

        it "should allow data scientist to create a directory in an editable dataset" do
          dirname = @dataset[:inode_name] + "/testDir"
          member = create_user
          add_member_to_project(@project, member[:email], "Data scientist")
          create_session(member[:email], "Pass123")
          create_dir(@project, dirname, query: "&type=DATASET")
          expect_status(201)
          get_datasets_in_path(@project, @dataset[:inode_name], query: "&type=DATASET")
          createdDir = json_body[:items].detect { |inode| inode[:attributes][:name] == "testDir" }
          expect(createdDir[:attributes][:permission]).to eq ("rwxrwx---")
        end

        it "should fail to delete a directory in a read only dataset" do
          create_session(@project[:username], "Pass123")
          update_dataset_permissions(@project, @dataset[:inode_name], "READ_ONLY", datasetType: "&type=DATASET")
          member = create_user
          add_member_to_project(@project, member[:email], "Data scientist")
          create_session(member[:email], "Pass123")
          delete_dataset(@project, "#{@dataset[:inode_name]}/testDir", datasetType: "?type=DATASET")
          expect_json(errorCode: 110050) # Permission denied.
          expect_status(403)
          # Directory should still be there
          get_datasets_in_path(@project, @dataset[:inode_name], query: "&type=DATASET")
          createdDir = json_body[:items].detect { |inode| inode[:attributes][:name] == "testDir" }
          expect(createdDir).to be_present
        end

        it "should make the dataset not editable" do
          create_session(@project[:username], "Pass123") # be the user of the project that owns the dataset
          update_dataset_permissions(@project, @dataset[:inode_name], "READ_ONLY", datasetType: "&type=DATASET")
          expect_status(200)
          # check for permission
          get_dataset_stat(@project, @dataset[:inode_name], datasetType: "&type=DATASET")
          ds = json_body
          expect(ds[:permission]).to eq ("READ_ONLY")
          # check for permission inside the dataset directory
          get_datasets_in_path(@project, @dataset[:inode_name], query: "&type=DATASET")
          createdDir = json_body[:items].detect { |inode| inode[:attributes][:name] == "testDir" }
          expect(createdDir[:attributes][:permission]).to eq ("rwxrwx---")#This is default permission of all datasets
        end

        it "should fail to create a directory as data scientist" do
          dataset = @dataset[:inode_name]
          dirname = dataset + "/afterDir"
          member = create_user
          add_member_to_project(@project, member[:email], "Data scientist")
          create_session(member[:email], "Pass123")
          create_dir(@project, dirname, query: "&type=DATASET")
          expect_status(403)
          get_datasets_in_path(@project, @dataset[:inode_name], query: "&type=DATASET")
          createdDir = json_body[:items].detect { |inode| inode[:attributes][:name] == "afterDir" }
          expect(createdDir).to be_nil
        end
      end

      context 'test if the dataset owner is added to the dataset group' do
        before :all do
          with_valid_project
          with_valid_dataset
        end

        it "should be able to download a file created by another user" do
          # Make the dataset writable by other members of the project
          update_dataset_permissions(@project, @dataset[:inode_name], "EDITABLE", datasetType: "&type=DATASET")
          expect_status(200)

          dirname = @dataset[:inode_name] + "/afterDir"
          project_owner = @user
          member = create_user
          add_member_to_project(@project, member[:email], "Data owner")
          create_session(member[:email], "Pass123")

          # Create a subdirectory
          create_dir(@project, dirname, query: "&type=DATASET")
          expect_status(201)

          # Copy README.md to the subdirectory
          copy_dataset(@project, "#{@dataset[:inode_name]}/README.md", "/Projects/#{@project[:projectname]}/#{dirname}/README.md", datasetType: "&type=DATASET")
          expect_status(204)

          # Log in as project owner, if the project owner is in the dataset group, it should be able to preview
          # the copied README.md file.
          create_session(project_owner[:email], "Pass123")
          # Try to preview the README.md
          get_dataset_blob(@project, "#{dirname}/README.md", datasetType: "&type=DATASET")
          expect_status(200)
        end
      end
    end

    describe "#zip_unzip" do
      context 'with authentication and sufficient privileges' do
        before :all do
          with_valid_project
          with_valid_dataset
        end

        it 'create directory to zip' do
          ds1name = @dataset[:inode_name]

          ds2name = ds1name + "/testDir"
          create_dir(@project, ds2name)
          expect_status(201)

          ds3name = ds2name + "/subDir"
          create_dir(@project, ds3name)
          expect_status(201)

          ds4name = ds1name + "/test%20Dir"
          create_dir(@project, ds4name)
          expect_status(201)

          ds5name = ds4name + "/sub%20Dir"
          create_dir(@project, ds5name)
          expect_status(201)

          get_dataset_stat(@project, ds1name, datasetType: "&type=DATASET")
          ds = json_body
          expect(ds).to be_present

          get_dataset_stat(@project, ds2name, datasetType: "&type=DATASET")
          ds = json_body
          expect(ds).to be_present

          get_dataset_stat(@project, ds3name, datasetType: "&type=DATASET")
          ds = json_body
          expect(ds).to be_present

          get_dataset_stat(@project, ds4name, datasetType: "&type=DATASET")
          ds = json_body
          expect(ds).to be_present

          get_dataset_stat(@project, ds5name, datasetType: "&type=DATASET")
          ds = json_body
          expect(ds).to be_present

        end

        it 'zip directory' do
          zip_dataset(@project, "#{@dataset[:inode_name]}/testDir", datasetType: "&type=DATASET")
          expect_status(204)

          wait_for do
            get_datasets_in_path(@project, @dataset[:inode_name], query: "&type=DATASET")
            ds = json_body[:items].detect { |d| d[:attributes][:name] == "testDir.zip" }
            !ds.nil?
          end
        end

        it 'unzip directory' do
          delete_dataset(@project, "#{@dataset[:inode_name]}/testDir", datasetType: "?type=DATASET")
          expect_status(204)

          unzip_dataset(@project, "#{@dataset[:inode_name]}/testDir.zip", datasetType: "&type=DATASET")
          expect_status(204)

          wait_for do
            get_datasets_in_path(@project, @dataset[:inode_name], query: "&type=DATASET")
            ds = json_body[:items].detect { |d| d[:attributes][:name] == "testDir" }
            !ds.nil?
          end
        end

        it 'zip directory with spaces' do
          zip_dataset(@project, "#{@dataset[:inode_name]}/test%20Dir/sub%20Dir", datasetType: "&type=DATASET")
          expect_status(204)

          wait_for do
            get_datasets_in_path(@project, "#{@dataset[:inode_name]}/test%20Dir", query: "&type=DATASET")
            ds = json_body[:items].detect { |d| d[:attributes][:name] == "sub Dir.zip" }
            !ds.nil?
          end
        end

        it 'unzip directory with spaces' do

          delete_dataset(@project, "#{@dataset[:inode_name]}/test%20Dir/sub%20Dir", datasetType: "?type=DATASET")
          expect_status(204)

          unzip_dataset(@project, "#{@dataset[:inode_name]}/test%20Dir/sub%20Dir.zip", datasetType: "&type=DATASET")
          expect_status(204)

          wait_for do
            get_datasets_in_path(@project, "#{@dataset[:inode_name]}/test%20Dir", query: "&type=DATASET")
            ds = json_body[:items].detect { |d| d[:attributes][:name] == "sub Dir" }
            !ds.nil?
          end
        end

        it "should fail to zip a dataset from other projects if path contains ../" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email], "Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          zip_dataset(project1, "Logs/../../../Projects/#{project[:projectname]}/Logs/README.md", datasetType: "&type=DATASET")
          expect_status(400)
          expect_json(errorCode: 110011) # DataSet not found.
          reset_session
        end
        it "should fail to unzip a dataset from other projects if path contains ../" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email], "Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          unzip_dataset(project1, "Logs/../../../Projects/#{project[:projectname]}/Logs/README.md.zip", datasetType: "&type=DATASET")
          expect_status(400) # bad request
          expect_json(errorCode: 110011) # DataSet not found.
          reset_session
        end
        context 'zip/unzip dir with url encoded chars' do
          before :all do
            with_valid_project
            with_valid_dataset
            hdfs_user="#{@project[:inode_name]}__#{@user[:username]}"
            topDataset = "#{@dataset[:inode_name]}/top%3ADir"
            mkdir("/Projects/#{@project[:inode_name]}/#{topDataset}", hdfs_user, hdfs_user, 755)
            subDataset = "#{topDataset}/sub%20Dir"
            mkdir("/Projects/#{@project[:inode_name]}/#{subDataset}", hdfs_user, hdfs_user, 755)
          end
          it 'zip directory with url encoded char' do
            topDataset = "#{@dataset[:inode_name]}/top%253ADir"
            subDataset = "#{topDataset}/sub%2520Dir"
            get_datasets_in_path(@project, topDataset, query: "&type=DATASET")
            expect_status(200)

            zip_dataset(@project, subDataset, datasetType: "&type=DATASET")
            expect_status(204)

            wait_for do
              get_datasets_in_path(@project, topDataset, query: "&type=DATASET")
              ds = json_body[:items].detect { |d| d[:attributes][:name] == "sub%20Dir.zip" }
              !ds.nil?
            end
          end

          it 'unzip directory with url encoded char' do
            topDataset = "#{@dataset[:inode_name]}/top%253ADir"
            subDataset = "#{topDataset}/sub%2520Dir"
            delete_dataset(@project, subDataset, datasetType: "?type=DATASET")
            expect_status(204)

            unzip_dataset(@project, "#{subDataset}.zip", datasetType: "&type=DATASET")
            expect_status(204)

            wait_for do
              get_datasets_in_path(@project, topDataset, query: "&type=DATASET")
              ds = json_body[:items].detect { |d| d[:attributes][:name] == "sub%20Dir" }
              !ds.nil?
            end
          end
        end
      end
    end

    describe "#Download" do
      context 'without authentication' do
        before :all do
          with_valid_project
          reset_session
        end
        it "should fail to get a download token" do
          get_download_token(@project, "Logs/README.md", datasetType: "?type=DATASET")
          expect_json(errorCode: 200003)
          expect_status(401)
        end
        it "should fail to download file without a token" do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/download/with_token/Logs/README.md?type=DATASET"
          expect_json(errorCode: 200003)
          expect_status(401)
        end
        it "should fail to download file without auth" do
          download_dataset_with_auth(@project, "Logs/README.md", datasetType: "type=DATASET")
          expect_status(401)
        end
        it "should fail to download file with an empty string token" do
          download_dataset_with_token(@project, "Logs/README.md", " ", datasetType: "&type=DATASET")
          expect_json(errorCode: 200003)
          expect_status(401)
        end
        it "should fail to download file with an empty token" do
          download_dataset_with_token(@project, "Logs/README.md", "", datasetType: "&type=DATASET")
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end
      context 'with authentication and sufficient privileges' do
        before :all do
          with_valid_project
        end
        it "should download Logs/README.md" do
          download_dataset_with_auth(@project, "Logs/README.md", datasetType: "type=DATASET")
          expect_status(200)
        end
        it "should download Logs/README.md with token" do
          download_dataset(@project, "Logs/README.md", datasetType: "type=DATASET")
          expect_status(200)
        end
        it "should fail to download with a token issued for a different file path" do
          get_download_token(@project, "Resources/README.md", datasetType: "?type=DATASET")
          expect_status(200)
          token = json_body[:data][:value]
          download_dataset_with_token(@project, "Logs/README.md", token, datasetType: "&type=DATASET")
          expect_status(401)
        end
        it "should fail to download more than one file with a single token" do
          get_download_token(@project, "Resources/README.md", datasetType: "?type=DATASET")
          expect_status(200)
          token = json_body[:data][:value]
          download_dataset_with_token(@project, "Resources/README.md", token, datasetType: "&type=DATASET")
          expect_status(200)
          download_dataset_with_token(@project, "Resources/README.md", token, datasetType: "&type=DATASET")
          expect_status(401)
        end
        it 'should fail to download a file if variable download_allowed is false' do
          user = @user[:email]
          setVar("download_allowed", 'false')
          create_session(user, "Pass123")
          download_dataset_with_auth(@project, "Logs/README.md", datasetType: "type=DATASET")
          expect_status(403)
          #set var back to true
          setVar("download_allowed", "true")
          expect(getVar("download_allowed").value).to eq "true"
        end
        it 'should fail to download a file with token if variable download_allowed is false' do
          user = @user[:email]
          setVar("download_allowed", 'false')
          create_session(user, "Pass123")
          get_download_token(@project, "Logs/README.md", datasetType: "?type=DATASET")
          expect_status(403)
          #set var back to true
          setVar("download_allowed", "true")
          expect(getVar("download_allowed").value).to eq "true"
        end
        it 'should fail to download a file with apikey if variable download_allowed is false' do
          user = @user[:email]
          setVar("download_allowed", 'false')
          create_session(user, "Pass123")
          # use a valid apikey
          key_view = create_api_key('datasetKey', %w(DATASET_VIEW))
          set_api_key_to_header(key_view)
          download_dataset_with_auth(@project, "Logs/README.md", datasetType: "type=DATASET")
          expect_status(403)
          #set var back to true
          setVar("download_allowed", "true")
          expect(getVar("download_allowed").value).to eq "true"
        end
      end
    end
    describe '#checks' do
      before(:all) do
        with_valid_project
      end
      it 'should allow .. if path resolves to a valid file' do
        projectname = "project_#{short_random_id}"
        project = create_project_by_name(projectname)
        dsname = "dataset_#{short_random_id}"
        ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
        request_access(@project, ds, project)
        share_dataset(@project, dsname, project[:projectname], permission: "EDITABLE")
        get_dataset_stat(project, "Logs/../../../Projects/#{@project[:projectname]}/#{dsname}", datasetType: "&type=DATASET")
        shared_ds = json_body
        get_dataset_stat(project, "#{@project[:projectname]}::#{dsname}", datasetType: "&type=DATASET")
        expect(json_body[:attributes]).to eq (shared_ds[:attributes])# :href can be different
      end
      it 'should not allow path with too many ..' do
        dsname = "dataset_#{short_random_id}"
        ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
        get_dataset_stat(@project, "Logs/../../../../Projects/#{@project[:projectname]}/#{dsname}", datasetType: "&type=DATASET")
        expect_status(400) # bad request
        expect_json(errorCode: 110011) # DataSet not found.
      end
      it 'should not let users create dataset with type Hive' do
        dsname = "dataset_#{short_random_id}"
        create_dir(@project, dsname, query: "&type=HIVEDB")
        expect_status(400)
      end
      it 'should not let users create dataset with type FS' do
        dsname = "dataset_#{short_random_id}"
        create_dir(@project, dsname, query: "&type=FEATURESTORE")
        expect_status(400)
      end
      it 'should not let users create dirs within featurestore' do
        featurestore_dataset_name = "#{@project[:projectname]}_featurestore.db".downcase
        create_dir(@project, "#{featurestore_dataset_name}/test_#{short_random_id}", query: "&type=FEATURESTORE")
        expect_status(400)
      end
      it 'should not let users create dirs within hivedb' do
        hivedb_dataset_name = "#{@project[:projectname]}.db".downcase
        create_dir(@project, "#{hivedb_dataset_name}/test_#{short_random_id}", query: "&type=HIVEDB")
        expect_status(400)
      end
      it 'should not allow :: in dataset name' do
        create_dataset_by_name(@project, "test::dataset_#{short_random_id}", permission: "READ_ONLY")
        expect_status_details(400)
      end
      it 'should not allow create fake shared dataset' do
        with_valid_dataset
        project1 = create_project
        share_dataset(@project, @dataset[:inode_name], project1[:projectname], permission: "EDITABLE")
        accept_dataset(project1, "#{@project[:projectname]}::#{@dataset[:inode_name]}", datasetType: "&type=DATASET")
        project2 = create_project
        create_dataset_by_name(project2, "#{@project[:projectname]}::#{@dataset[:inode_name]}", permission: "READ_ONLY")
        expect_status_details(400)
      end
      it 'should not allow :: in file name' do
        with_valid_dataset
        create_dir(@project, "#{@dataset[:inode_name]}/test::dir_#{short_random_id}", query: "&type=DATASET")
        expect_status_details(422)
        featurestore_dataset_name = "#{@project[:projectname]}_featurestore.db".downcase
        create_dir(@project, "#{featurestore_dataset_name}/test::dir_#{short_random_id}", query: "&type=FEATURESTORE")
        expect_status_details(400)
        hivedb_dataset_name = "#{@project[:projectname]}.db".downcase
        create_dir(@project, "#{hivedb_dataset_name}/test::dir_#{short_random_id}", query: "&type=HIVEDB")
        expect_status_details(400)
      end
    end
    describe '#sort' do
      context 'top level dataset' do
        before(:all) do
          with_valid_project
          create_datasets(10)
          create_datasets_for_new_user(5)
          create_shared_datasets(5, true)
          get_datasets_in_path(@project, "")
          @datasets = json_body[:items]
        end
        it 'should return sorted datasets by id (asc)' do
          test_sort_by(@project, @datasets, "", "id", "asc", "id")
        end
        it 'should return sorted datasets by id (desc)' do
          test_sort_by(@project, @datasets, "", "id", "desc", "id")
        end
        it 'should return sorted datasets by name (asc)' do
          test_sort_by_str(@project, @datasets, "", "name", "asc", "name")
        end
        it 'should return sorted datasets by name (desc)' do
          test_sort_by_str(@project, @datasets, "", "name", "desc", "name")
        end
        it 'should return sorted datasets by searchable (asc)' do
          test_sort_by_str(@project, @datasets, "", "searchable", "asc", "searchable")
        end
        it 'should return sorted datasets by searchable (desc)' do
          test_sort_by_str(@project, @datasets, "", "searchable", "desc", "searchable")
        end
        it 'should return sorted datasets by size (asc)' do
          test_sort_by_attr(@project, @datasets, "", "size", "asc", "size")# dataset size is not set
        end
        it 'should return sorted datasets by size (desc)' do
          test_sort_by_attr(@project, @datasets, "", "size", "desc", "size")
        end
        it 'should return sorted datasets by modificationTime (asc)' do
          test_sort_by_date_attr(@project, @datasets, "", "modificationTime", "asc", "modification_time")
        end
        it 'should return sorted datasets by modificationTime (desc)' do
          test_sort_by_date_attr(@project, @datasets, "", "modificationTime", "desc", "modification_time")
        end
        it 'should return sorted datasets by accessTime (asc)' do
          test_sort_by_date_attr(@project, @datasets, "", "accessTime", "asc", "access_time")
        end
        it 'should return sorted datasets by accessTime (desc)' do
          test_sort_by_date_attr(@project, @datasets, "", "accessTime", "desc", "access_time")
        end
        it 'should return sorted datasets by type (asc)' do
          test_sort_by_datasetType(@project, @datasets, "", "datasetType", "asc", "type")
        end
        it 'should return sorted datasets by type (desc)' do
          test_sort_by_datasetType(@project, @datasets, "", "datasetType", "desc", "type")
        end
      end
      context 'dataset content' do
        before(:all) do
          with_valid_dataset
          create_dataset_contents(10)
          create_dataset_contents_for_new_user(5)
          create_files
          get_datasets_in_path(@project, @dataset[:inode_name])
          @dataset_content = json_body[:items]
        end
        it 'should return sorted dataset content by id (asc)' do
          test_sort_by_attr(@project, @dataset_content, @dataset[:inode_name], "id", "asc", "id")
        end
        it 'should return sorted dataset content by id (desc)' do
          test_sort_by_attr(@project, @dataset_content, @dataset[:inode_name],"id", "desc", "id")
        end
        it 'should return sorted dataset content by name (asc)' do
          test_sort_by_str_attr(@project, @dataset_content, @dataset[:inode_name], "name", "asc", "name")
        end
        it 'should return sorted dataset content by name (desc)' do
          test_sort_by_str_attr(@project, @dataset_content, @dataset[:inode_name],"name", "desc", "name")
        end
        it 'should return sorted dataset content by size (asc)' do
          test_sort_by_attr(@project, @dataset_content, @dataset[:inode_name], "size", "asc", "size")
        end
        it 'should return sorted dataset content by size (desc)' do
          test_sort_by_attr(@project, @dataset_content, @dataset[:inode_name], "size", "desc", "size")
        end
        it 'should return sorted dataset content by modificationTime (asc)' do
          test_sort_by_date_attr(@project, @dataset_content, @dataset[:inode_name], "modificationTime", "asc", "modification_time")
        end
        it 'should return sorted dataset content by modificationTime (desc)' do
          test_sort_by_date_attr(@project, @dataset_content, @dataset[:inode_name], "modificationTime", "desc", "modification_time")
        end
        it 'should return sorted dataset content by accessTime (asc)' do
          test_sort_by_date_attr(@project, @dataset_content, @dataset[:inode_name], "accessTime", "asc", "access_time")
        end
        it 'should return sorted dataset content by accessTime (desc)' do
          test_sort_by_date_attr(@project, @dataset_content, @dataset[:inode_name], "accessTime", "desc", "access_time")
        end
      end
    end
    describe '#filter' do
      context 'top level dataset' do
        before(:all) do
          with_valid_dataset #will create a dataset with name starting with dataset
          create_datasets(5, searchable=true) # searchable
          create_datasets(5, searchable=false) # not searchable
          create_shared_datasets(5, true) # accepted
          create_shared_datasets(5, false) # pending
          create_datasets_for_new_user(5)
          get_datasets_in_path(@project, "")
          @datasets = json_body[:items]
        end
        it 'should return only shared datasets' do
          test_filter_by(@project, [false], "", "shared", "shared:true")
        end
        it 'should return only non shared datasets' do
          test_filter_by(@project, [true], "", "shared", "shared:false")
        end
        it 'should return only pending datasets' do
          test_filter_by(@project, [true], "", "accepted", "accepted:false")
        end
        it 'should return only accepted datasets' do
          test_filter_by(@project, [false], "", "accepted", "accepted:true")
        end
        it 'should return only datasets underConstruction' do
          test_filter_by_attr(@project, [true], "", "underConstruction", "under_construction:false")
        end
        it 'should return only searchable datasets' do
          test_filter_by(@project, [false], "", "searchable", "searchable:true")
        end
        it 'should filter by name' do
          test_filter_by_starts_with(@project, @datasets, "", "name", "name", "data")
        end
        it 'should filter by user email' do
          nonSharedDs = @datasets.map { |o| o if o[:shared]==false}.compact
          test_filter_by_eq_attr(@project, nonSharedDs, "", "owner", "#{@user[:fname]} #{@user[:lname]}", "user_email",
                                 @user[:email])
        end
        it 'should filter by user project name' do
          nonSharedDs = @datasets.map { |o| o if o[:shared]==false}.compact
          test_filter_by_eq_attr(@project, nonSharedDs, "", "owner", "#{@user[:fname]} #{@user[:lname]}", "hdfs_user",
                                 "#{@project[:projectname]}__#{@user[:username]}")
        end
        it 'should filter by size == ' do
          test_filter_by_eq_attr(@project, @datasets, "", "size", 0, "size", 0)#folders have no size but filter should work
        end
        it 'should filter by size > ' do
          test_filter_by_gt_attr(@project, @datasets, "", "size", -1, "size_gt", -1)
        end
        it 'should filter by size < ' do
          test_filter_by_lt_attr(@project, @datasets, "", "size", 10, "size_lt", 10)
        end
        it 'should filter by modificationTime = ' do
          s = @datasets.sample
          mt = s[:attributes][:modificationTime]
          if mt.length < 24
            mt.insert(22, "0")
          end
          test_filter_by_eq_attr(@project, @datasets, "", "modificationTime", s[:attributes][:modificationTime], "modification_time", mt)
        end
        it 'should filter by modificationTime <' do
          s = @datasets.sample
          mt = s[:attributes][:modificationTime]
          if mt.length < 24
            mt.insert(22, "0")
          end
          test_filter_by_lt_attr(@project, @datasets, "", "modificationTime", s[:attributes][:modificationTime], "modification_time_lt", mt)
        end
        it 'should filter by modificationTime >' do
          s = @datasets.sample
          mt = s[:attributes][:modificationTime]
          if mt.length < 24
            mt.insert(22, "0")
          end
          test_filter_by_gt_attr(@project, @datasets, "", "modificationTime", s[:attributes][:modificationTime], "modification_time_gt", mt)
        end
        it 'should filter by accessTime =' do
          s = @datasets.sample
          acct = s[:attributes][:accessTime]
          test_filter_by_eq_attr(@project, @datasets, "", "accessTime", acct, "access_time", acct)
        end
        it 'should filter datasets by accessTime <' do
          s = @datasets.sample
          acct = s[:attributes][:accessTime]
          test_filter_by_lt_attr(@project, @datasets, "", "accessTime", acct, "access_time_lt", acct)
        end
        it 'should filter datasets by accessTime >' do
          s = @datasets.sample
          acct = s[:attributes][:accessTime]
          test_filter_by_gt_attr(@project, @datasets, "", "accessTime", acct, "access_time_gt", acct)
        end
      end
      context 'dataset content' do
        before(:all) do
          with_valid_dataset
          create_dataset_contents(10)
          create_dataset_contents_for_new_user(5)
          create_files
          get_datasets_in_path(@project, @dataset[:inode_name])
          @dataset_content = json_body[:items]
        end
        it 'should filter by name' do
          test_filter_by_starts_with_attr(@project, @dataset_content, @dataset[:inode_name], "name", "name", "Sample")#will get the metadat files
        end
        it 'should filter by user email' do
          test_filter_by_eq_attr(@project, @dataset_content, @dataset[:inode_name], "owner", "#{@user[:fname]} #{@user[:lname]}", "user_email",
                                 @user[:email])
        end
        it 'should filter by user project name' do
          test_filter_by_eq_attr(@project, @dataset_content, @dataset[:inode_name], "owner", "#{@user[:fname]} #{@user[:lname]}", "hdfs_user",
                                 "#{@project[:projectname]}__#{@user[:username]}")
        end
        it 'should filter by size == ' do
          s = @dataset_content.map { |o| "#{o[:attributes][:size]}" if o[:attributes][:size]>0}.compact
          size = s.sample
          test_filter_by_eq_attr(@project, @dataset_content, @dataset[:inode_name], "size", size.to_i, "size", size)
        end
        it 'should filter by size > ' do
          s = @dataset_content.map { |o| "#{o[:attributes][:size]}" if o[:attributes][:size]>1}.compact
          size = s.sample
          test_filter_by_gt_attr(@project, @dataset_content, @dataset[:inode_name], "size", size.to_i - 1, "size_gt", size.to_i - 1)
        end
        it 'should filter by size < ' do
          s = @dataset_content.map { |o| "#{o[:attributes][:size]}" if o[:attributes][:size]>0}.compact
          size = s.sample
          test_filter_by_lt_attr(@project, @dataset_content, @dataset[:inode_name], "size", size.to_i + 1, "size_lt", size.to_i + 1)
        end
        it 'should filter by modificationTime = ' do
          s = @dataset_content.sample
          mt = s[:attributes][:modificationTime]
          if mt.length < 24
            mt.insert(22, "0")
          end
          test_filter_by_eq_attr(@project, @dataset_content, @dataset[:inode_name], "modificationTime",
                                 s[:attributes][:modificationTime], "modification_time", mt)
        end
        it 'should filter by modificationTime <' do
          s = @dataset_content.sample
          mt = s[:attributes][:modificationTime]
          if mt.length < 24
            mt.insert(22, "0")
          end
          test_filter_by_lt_attr(@project, @dataset_content, @dataset[:inode_name], "modificationTime",
                                 s[:attributes][:modificationTime], "modification_time_lt", mt)
        end
        it 'should filter by modificationTime >' do
          s = @dataset_content.sample
          mt = s[:attributes][:modificationTime]
          if mt.length < 24
            mt.insert(22, "0")
          end
          test_filter_by_gt_attr(@project, @dataset_content, @dataset[:inode_name], "modificationTime",
                                 s[:attributes][:modificationTime], "modification_time_gt", mt)
        end
        it 'should filter by accessTime =' do
          s = @dataset_content.sample
          acct = s[:attributes][:accessTime]
          test_filter_by_eq_attr(@project, @dataset_content, @dataset[:inode_name], "accessTime", acct, "access_time",
                                 acct)
        end
        it 'should filter datasets by accessTime <' do
          s = @dataset_content.sample
          acct = s[:attributes][:accessTime]
          test_filter_by_lt_attr(@project, @dataset_content, @dataset[:inode_name], "accessTime", acct, "access_time_lt", acct)
        end
        it 'should filter datasets by accessTime >' do
          s = @dataset_content.sample
          acct = s[:attributes][:accessTime]
          test_filter_by_gt_attr(@project, @dataset_content, @dataset[:inode_name], "accessTime", acct, "access_time_gt", acct)
        end
      end
    end
    describe '#pagination' do
      before(:all) do
        with_valid_dataset
        create_datasets(15, searchable=true)
        create_shared_datasets(5, true)
        get_datasets_in_path(@project, "")
        @datasets = json_body[:items]
        create_dataset_contents(15)
        create_files
        get_datasets_in_path(@project, @dataset[:inode_name])
        @dataset_content = json_body[:items]
      end
      context 'top level dataset' do
        it 'should limit results' do
          test_offset_limit(@project, @datasets, "", 0, 15)
        end
        it 'should ignore if limit < 0.' do
          test_offset_limit(@project, @datasets, "", 0, -10)
        end
        it 'should get all results if limit > len' do
          test_offset_limit(@project, @datasets, "", 0, 1000)
        end
        it 'should get all results from the offset' do
          test_offset_limit(@project, @datasets, "", 5, 0)
        end
        it 'should get limit results from the offset' do
          test_offset_limit(@project, @datasets, "", 5, 10)
        end
        it 'should ignore if offset < 0.' do
          test_offset_limit(@project, @datasets, "", -1, 10)
        end
        it 'should get 0 result if offset >= len.' do
          test_offset_limit(@project, @datasets, "", 2500, 10)
        end
      end
      context 'dataset content' do
        it 'should limit results' do
          test_offset_limit_attr(@project, @dataset_content, @dataset[:inode_name], 0, 15)
        end
        it 'should ignore if limit < 0.' do
          test_offset_limit_attr(@project, @dataset_content, @dataset[:inode_name], 0, -10)
        end
        it 'should get all results if limit > len' do
          test_offset_limit_attr(@project, @dataset_content, @dataset[:inode_name], 0, 1000)
        end
        it 'should get all results from the offset' do
          test_offset_limit_attr(@project, @dataset_content, @dataset[:inode_name], 5, 0)
        end
        it 'should get limit results from the offset' do
          test_offset_limit_attr(@project, @dataset_content, @dataset[:inode_name], 5, 10)
        end
        it 'should ignore if offset < 0.' do
          test_offset_limit_attr(@project, @dataset_content, @dataset[:inode_name], -1, 10)
        end
        it 'should get 0 result if offset >= len.' do
          test_offset_limit_attr(@project, @dataset_content, @dataset[:inode_name], 2500, 10)
        end
      end
    end
    describe 'with Api key' do
      before(:all) do
        with_valid_project
        @key_view = create_api_key('datasetKey', %w(DATASET_VIEW))
        @key_create = create_api_key('datasetKey_create', %w(DATASET_VIEW DATASET_CREATE))
        @key_delete = create_api_key('datasetKey_delete', %w(DATASET_VIEW DATASET_DELETE))
        @invalid_key = create_api_key('datasetKey_invalid', %w(JOB INFERENCE))
      end
      context 'with invalid scope' do
        before(:all) do
          set_api_key_to_header(@invalid_key)
        end
        it 'should fail to access datasets' do
          get_datasets_in_path(@project, '')
          expect_json(errorCode: 320004)
          expect_status(403)
        end
        it 'should fail to create a dataset' do
          create_dir(@project, "dataset_#{Time.now.to_i}", query: "&type=DATASET")
          expect_json(errorCode: 320004)
          expect_status(403)
        end
        it 'should fail to delete a dataset' do
          delete_dataset(@project, "Logs", datasetType: "?type=DATASET")
          expect_status(403)
        end
      end
      context 'with valid scope' do
        it 'should get ' do
          set_api_key_to_header(@key_view)
          get_datasets_in_path(@project, '')
          expect_status(200)
        end
        it 'should create' do
          set_api_key_to_header(@key_create)
          create_dataset_by_name_checked(@project, "dataset_#{Time.now.to_i}", permission: "READ_ONLY")
          expect_status(201)
        end
        it 'should move a dataset' do
          set_api_key_to_header(@key_create)
          create_dir(@project, "Resources/test1", query: "&type=DATASET")
          expect_status(201)
          move_dataset(@project, "Resources/test1", "/Projects/#{@project[:projectname]}/Logs/test1", datasetType: "&type=DATASET")
          expect_status(204)
        end
        it 'should copy a dataset' do
          set_api_key_to_header(@key_create)
          create_dir(@project, "Resources/test2", query: "&type=DATASET")
          expect_status(201)
          copy_dataset(@project, "Resources/test2", "/Projects/#{@project[:projectname]}/Logs/test2", datasetType: "&type=DATASET")
          expect_status(204)
        end
        it 'should download Logs/README.md' do
          set_api_key_to_header(@key_view)
          download_dataset_with_auth(@project, "Logs/README.md", datasetType: "type=DATASET")
          expect_status(200)
        end
        it 'should delete' do
          set_api_key_to_header(@key_delete)
          delete_dataset(@project, "Logs", datasetType: "?type=DATASET")
          expect_status(204)
        end
      end
    end
  end
end