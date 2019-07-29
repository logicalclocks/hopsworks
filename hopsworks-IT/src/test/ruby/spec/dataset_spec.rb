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
  describe 'dataset' do
    after(:all){clean_projects}
    describe "#create" do
      context 'without authentication' do
        before :all do
          with_valid_project
          reset_session
        end
        it "should fail" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/createTopLevelDataSet", {name: "dataset_#{Time.now.to_i}", description: "test dataset", searchable: true, generateReadme: true}
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
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/createTopLevelDataSet", {name: dsname, description: "test dataset", searchable: true, generateReadme: true}
          expect_json(successMessage: "The Dataset was created successfully.")
          expect_status(200)
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/getContent"
          ds = json_body.detect { |d| d[:name] == dsname }
          expect(ds[:description]).to eq ("test dataset")
          expect(ds[:owningProjectName]).to eq ("#{@project[:projectname]}")
          expect(ds[:owner]).to eq ("#{@user[:fname]} #{@user[:lname]}")
          expect(ds[:permission]).to eq ("rwxr-x---")
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/getContent/#{dsname}"
          ds = json_body.detect { |d| d[:name] == "README.md" }
          expect(ds).to be_present
        end

        it 'should work with valid params and no README.md' do
          dsname = "dataset_#{short_random_id}"
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/createTopLevelDataSet", {name: dsname, description: "test dataset", searchable: true, generateReadme: false}
          expect_json(successMessage: "The Dataset was created successfully.")
          expect_status(200)
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/getContent"
          ds = json_body.detect { |d| d[:name] == dsname }
          expect(ds[:description]).to eq ("test dataset")
          expect(ds[:owningProjectName]).to eq ("#{@project[:projectname]}")
          expect(ds[:owner]).to eq ("#{@user[:fname]} #{@user[:lname]}")
          expect(ds[:permission]).to eq ("rwxr-x---")
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/getContent/#{dsname}"
          ds = json_body.detect { |d| d[:name] == "README.md" }
          expect(ds).to be_nil
        end

        it 'should fail to create a dataset with space in the name' do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/createTopLevelDataSet", {name: "test dataset", description: "test dataset", searchable: true, generateReadme: true}
          expect_json(errorCode: 110028)
          expect_status(400)
        end

        it 'should fail to create a dataset with Ö in the name' do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/createTopLevelDataSet", {name: "testÖjdataset", description: "test dataset", searchable: true, generateReadme: true}
          expect_json(errorCode: 110028)
          expect_status(400)
        end

        it 'should fail to create a dataset with Ö in the name' do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/createTopLevelDataSet", {name: "testÖjdataset", description: "test dataset", searchable: true, generateReadme: true}
          expect_json(errorCode: 110028)
          expect_status(400)
        end

        it 'should fail to create a dataset with a name that ends with a .' do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/createTopLevelDataSet", {name: "testdot.", description: "test dataset", searchable: true, generateReadme: true}
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
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/getContent"
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end
      context 'with authentication' do
        before :all do
          with_valid_project
        end
        it "should return dataset list" do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/getContent"
          expect_json_types :array
          expect_status(200)
        end
        it "should fail to return dataset list from Projects if path contains ../" do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/getContent/Logs/../../../Projects"
          expect_status(400)
          expect_json(errorCode: 110018)
          reset_session
        end
        it "should fail to check if a dataset is a dir for a project if path contains ../" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email],"Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          get "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/dataset/isDir/Logs/../../../Projects/#{project[:projectname]}/Logs/README.md"
          expect_status(400)
          expect_json(errorCode: 110018)
          reset_session
        end
        it "should fail to count file blocks to a project if path contains ../" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email],"Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          get "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/dataset/countFileBlocks/Logs/../../../Projects/#{project[:projectname]}/Logs/README.md"
          expect_status(200)# Always returns 200 but if file does not exist blocks = -1
          expect(response).to include("-1")
          reset_session
        end
        it "should fail to upload file to a project if path contains ../" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email],"Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          file = URI.encode_www_form({templateId: -1, flowChunkNumber: 1, flowChunkSize: 1048576,
                                      flowCurrentChunkSize: 3195, flowTotalSize: 3195,
                                      flowIdentifier: "3195-someFiletxt", flowFilename: "someFile.txt",
                                      flowRelativePath: "someFile.txt", flowTotalChunks: 1})
          get "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/dataset/upload/Logs/../../../Projects/#{project[:projectname]}/Logs/?#{file}", {content_type: "multipart/form-data"}
          expect_status(204)# will upload to project1/dataset/getContent/Logs/Projects/project/Logs/
          expect(response).to be_empty
          get "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/dataset/getContent/Logs/Projects/#{project[:projectname]}/Logs/"
          ds = json_body.detect { |d| d[:name] == "someFile.txt" }
          expect(ds).to be_present
          reset_session
        end
        it "should fail to return dataset list from Projects if path contains .." do
          project = get_project
          newUser = create_user
          create_session(newUser[:email],"Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          get "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/dataset/getContent/Logs/../../../Projects/#{project[:projectname]}/"
          expect_status(400)
          expect_json(errorCode: 110018)
          reset_session
        end
        it "should fail to return dataset list from Projects if path contains ../../Projects/../../Projects" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email],"Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          get "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/dataset/getContent/Logs/../../Projects/../../Projects/#{project[:projectname]}/"
          expect_status(400)
          expect_json(errorCode: 110018)
          reset_session
        end
        it "should fail to return file from other Projects if path contains .." do
          project = get_project
          newUser = create_user
          create_session(newUser[:email],"Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          get "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/dataset/getFile/Logs/../../../Projects/#{project[:projectname]}/Logs/README.md"
          expect_status(400)
          expect_json(errorCode: 110018)
          reset_session
        end
        it "should fail to check if file exists from another project if path contains ../" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email],"Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          get "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/dataset/fileExists/Logs/../../../Projects/#{project[:projectname]}/Logs/README.md"
          expect_status(400)
          expect_json(errorCode: 110018)
          reset_session
        end
        it "should fail to preview a file from another project if path contains ../" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email],"Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          get "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/dataset/filePreview/Logs/../../../Projects/#{project[:projectname]}/Logs/README.md"
          expect_status(400)
          expect_json(errorCode: 110018)
          reset_session
        end
        it "should fail to check for download a file from another project if path contains ../" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email],"Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          get "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/dataset/checkFileForDownload/Logs/../../../Projects/#{project[:projectname]}/Logs/README.md"
          expect_status(404)
          expect_json(errorCode: 110008)
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
          delete "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/Logs"
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
          add_member(member[:email], "Data scientist")
          create_session(member[:email],"Pass123")
          delete "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/Logs"
          expect_status(500)
          reset_session
        end

        it "should fail to delete dataset in another project with .. in path" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email],"Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          delete "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/dataset/file/Logs/../../../Projects/#{project[:projectname]}/Logs/README.md"
          expect_status(200)#returns 200 if file not found
          expect(test_file("/Projects/#{project[:projectname]}/Logs/README.md")).to eq(true)
          reset_session
        end

        it "should fail to delete corrupted(owned by glassfish and size=0) file in another project with .. in path" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email],"Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          hopsworks_user = getHopsworksUser()
          touchz("/Projects/#{project[:projectname]}/Logs/corrupted.txt", hopsworks_user, hopsworks_user)
          delete "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/dataset/corrupted/Logs/../../../Projects/#{project[:projectname]}/Logs/corrupted.txt"
          expect_status(500)# throws INODE_DELETION_ERROR with 500 status
          expect_json(errorCode: 110007)
          reset_session
        end
      end
      context 'with authentication and sufficient privilege' do
        before :all do
          with_valid_project
        end
        it "should delete dataset" do
          delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/Logs"
          expect_json(successMessage: "DataSet removed from hdfs.")
          expect_status(200)
        end
      end
    end
    describe "#request" do
      context 'without authentication' do
        before :all do
          with_valid_project
          with_valid_dataset
          reset_session
        end
        it "should fail to send request" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          reset_session
          post "#{ENV['HOPSWORKS_API']}/request/access", {inodeId: @dataset[:inode_id], projectId: project[:id]}
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end
      context 'with authentication' do
        before :all do
          with_valid_project
          with_valid_dataset
          reset_session
        end
        it "should send request" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          post "#{ENV['HOPSWORKS_API']}/request/access", {inodeId: @dataset[:inode_id], projectId: project[:id]}
          expect_json(successMessage: "Request sent successfully.")
          expect_status(200)
          create_session(@project[:username],"Pass123") # be the user of the project that owns the dataset
          get "#{ENV['HOPSWORKS_API']}/message"
          msg = json_body.detect { |e| e[:content].include? "Dataset name: #{@dataset[:inode_name]}" }
          expect(msg).not_to be_nil
        end
        it "should fail to send request to the same project" do
          post "#{ENV['HOPSWORKS_API']}/request/access", {inodeId: @dataset[:inode_id], projectId: @project[:id]}
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
          create_session(project[:username],"Pass123")
          create_dataset_by_name(project, dsname)
          reset_session
          post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/shareDataSet", {name: dsname, projectId: project1[:id]}
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
          create_session(project[:username],"Pass123")
          create_dataset_by_name(project, dsname)
          member = create_user
          add_member(member[:email], "Data scientist")
          create_session(member[:email],"Pass123")
          post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/shareDataSet", {name: dsname, projectId: project1[:id]}
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
          create_dataset_by_name(@project, dsname)
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/shareDataSet", {name: dsname, projectId: project[:id]}
          expect_json(successMessage: "The Dataset was successfully shared.")
          expect_status(200)
        end
        it "should share a HiveDB" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          share_dataset(@project, "#{@project[:projectname].downcase}.db", project, type="HIVEDB")
          datasets = get_all_datasets(project)
          shared_ds = datasets.detect { |e| e[:name] == "#{@project[:projectname].downcase}::#{@project[:projectname].downcase}.db" }
          expect(shared_ds).not_to be_nil
        end
        it "should appear as pending for datasets not requested" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          create_dataset_by_name(@project, dsname)
          share_dataset(@project, dsname, project)
          datasets = get_all_datasets(project)
          shared_ds = datasets.detect { |e| e[:name] == "#{@project[:projectname]}::#{dsname}" }
          expect(shared_ds[:status]).to be false
        end
        it "should appear as accepted for requested datasets" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name(@project, dsname)
          request_dataset_access(project, ds[:inode_id])
          share_dataset(@project, dsname, project)
          datasets = get_all_datasets(project)
          shared_ds = datasets.detect { |e| e[:name] == "#{@project[:projectname]}::#{dsname}" }
          expect(shared_ds[:status]).to be true
        end
        it "should fail to make a shared dataset editable with sticky bit" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          permissions = "GROUP_WRITABLE_SB"
          ds = create_dataset_by_name(@project, dsname)
          share_dataset(@project, dsname, project)
          put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/permissions",
              {name: "#{@project[:projectname]}::#{dsname}", permissions: permissions}
          expect_status(400)
        end
        it "should fail to write on a non editable shared dataset" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name(@project, dsname)
          share_dataset(@project, dsname, project)
          # Accept dataset
          get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/accept/#{ds[:inode_id]}"
          # try to write
          post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset",
               {name: "#{@project[:projectname]}::#{dsname}/testdir"}
          expect_status(403)
        end
        it "should write in an editable shared dataset" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          permissions = "GROUP_WRITABLE_SB"
          ds = create_dataset_by_name(@project, dsname)
          # Make the dataset editable
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/permissions", {name: dsname, permissions: permissions}
          share_dataset(@project, dsname, project)
          # Accept dataset
          get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/accept/#{ds[:inode_id]}"
          # Create a directory - from the "target" project
          post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset",
               {name: "#{"#{@project[:projectname]}::#{dsname}/testdir"}"}
          expect_status(200)
          # Check if the directory is present
          get_datasets_in(@project, dsname)
          ds = json_body.detect { |d| d[:name] == "testdir"}
          expect(ds).to be_present
        end
        it "should be able to see content shared dataset with a name that already exist in the target project" do
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          create_dataset_by_name(project, dsname)
          ds = create_dataset_by_name(@project, dsname)
          request_dataset_access(project, ds[:inode_id])
          share_dataset(@project, dsname, project)
          get_datasets_in(project, "#{@project[:projectname]}::#{dsname}")
          expect_status(200)
          get_datasets_in(project, dsname)
          expect_status(200)
        end
      end
    end

    describe "#permissions" do
      context 'with authentication and insufficient privileges' do
        before :all do
          with_valid_project
        end

        it "should fail" do
          project = get_project
          dsname = "dataset_#{short_random_id}"
          permissions = "GROUP_WRITABLE_SB"
          create_dataset_by_name(project, dsname)
          member = create_user
          add_member(member[:email], "Data scientist")
          create_session(member[:email],"Pass123")
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/permissions", {name: dsname, permissions: permissions, projectId: project[:id]}
          expect_json(errorCode: 150068)
          expect_status(403)
        end
      end

      context 'with authentication and sufficient privileges' do
        before :all do
          with_valid_project
          with_valid_dataset
        end

        it "should make the dataset editable with sticky bit" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/permissions", {name: @dataset[:inode_name], permissions: "GROUP_WRITABLE_SB"}
          expect_status(200)
          # check for correct permissions
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/getContent"
          ds = json_body.detect { |d| d[:name] == @dataset[:inode_name]}
          expect(ds[:permission]).to eq ("rwxrwx--T")
        end

        it "should allow data scientist to create a directory" do
          dirname = @dataset[:inode_name] + "/testDir"
          member = create_user
          add_member(member[:email], "Data scientist")
          create_session(member[:email],"Pass123")
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset", {name: dirname}
          expect_status(200)
          get_datasets_in(@project, @dataset[:inode_name])
          createdDir = json_body.detect { |inode| inode[:name] == "testDir" }
          expect(createdDir[:permission]).to eq ("rwxrwx--T")
        end

        it "should fail to delete a directory of another user" do
          create_session(@project[:username], "Pass123")
          member = create_user
          add_member(member[:email], "Data scientist")
          create_session(member[:email],"Pass123")
          delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/file/#{@dataset[:inode_name] + "/testDir"}"
          expect_status(500)
          # Directory should still be there
          get_datasets_in(@project, @dataset[:inode_name])
          createdDir = json_body.detect { |inode| inode[:name] == "testDir" }
          expect(createdDir).to be_present
        end

        it "should make the dataset not editable" do
          create_session(@project[:username],"Pass123") # be the user of the project that owns the dataset
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/permissions", {name: @dataset[:inode_name], permissions: "OWNER_ONLY"}
          expect_status(200)
          # check for permission
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/getContent"
          ds = json_body.detect { |d| d[:name] == @dataset[:inode_name]}
          expect(ds[:permission]).to eq ("rwxr-x---")
          # check for permission inside the dataset directory
          get_datasets_in(@project, @dataset[:inode_name])
          createdDir = json_body.detect { |inode| inode[:name] == "testDir" }
          expect(createdDir[:permission]).to eq ("rwxr-x---")
        end

        it "should fail to create a directory as data scientist" do
          dataset = @dataset[:inode_name]
          dirname = dataset + "/afterDir"
          member = create_user
          add_member(member[:email], "Data scientist")
          create_session(member[:email],"Pass123")
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset", {name: dirname}
          expect_status(403)
          get_datasets_in(@project, @dataset[:inode_name])
          createdDir = json_body.detect { |inode| inode[:name] == "afterDir" }
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
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/permissions", {name: @dataset[:inode_name], permissions: "GROUP_WRITABLE_SB"}
          expect_status(200)

          dirname = @dataset[:inode_name] + "/afterDir"
          project_owner = @user
          member = create_user
          add_member(member[:email], "Data owner")
          create_session(member[:email],"Pass123")

          # Create a subdirectory
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset", {name: dirname}
          expect_status(200)

          # Get README.md inodeId
          get_datasets_in(@project, @dataset[:inode_name])
          readme = json_body.detect { |inode| inode[:name] == "README.md" }

          # Copy README.md to the subdirectory
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/copy", {destPath: "/Projects/#{@project[:projectname]}/#{dirname}/README.md", inodeId: readme[:id]}
          expect_status(200)

          # Log in as project owner, if the project owner is in the dataset group, it should be able to preview
          # the copied README.md file.
          create_session(project_owner[:email], "Pass123")
          # Try to preview the README.md
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/filePreview/#{dirname}/README.md?mode=head"
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
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset", {name: ds2name, description: "test dataset", searchable: false, generateReadme: false}
          expect_status(200)

          ds3name = ds2name + "/subDir"
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset", {name: ds3name, description: "test dataset", searchable: false, generateReadme: false}
          expect_status(200)

          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/getContent/#{ds1name}"
          ds = json_body.detect { |d| d[:name] == "testDir" }
          expect(ds).to be_present

          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/getContent/#{ds2name}"
          ds = json_body.detect { |d| d[:name] == "subDir" }
          expect(ds).to be_present
        end

        it 'zip directory' do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/zip/#{@dataset[:inode_name]}/testDir"
          expect_status(200)

          wait_for do
            get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/getContent/#{@dataset[:inode_name]}"
            ds = json_body.detect { |d| d[:name] == "testDir.zip" }
            !ds.nil?
          end
        end

        it 'unzip directory' do
          delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/file/#{@dataset[:inode_name] + "/testDir"}"
          expect_status(200)

          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/unzip/#{@dataset[:inode_name]}/testDir.zip"
          expect_status(200)

          wait_for do
            get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/getContent/#{@dataset[:inode_name]}"
            ds = json_body.detect { |d| d[:name] == "testDir" }
            !ds.nil?
          end
        end

        it "should fail to zip a dataset from other projects if path contains ../" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email],"Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          get "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/dataset/zip/Logs/../../../Projects/#{project[:projectname]}/Logs/README.md"
          expect_status(404)
          expect_json(errorCode: 110008)
          reset_session
        end
        it "should fail to unzip a dataset from other projects if path contains ../" do
          project = get_project
          newUser = create_user
          create_session(newUser[:email],"Pass123")
          projectname = "project_#{short_random_id}"
          project1 = create_project_by_name(projectname)
          get "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/dataset/unzip/Logs/../../../Projects/#{project[:projectname]}/Logs/README.md.zip"
          expect_status(404)
          expect_json(errorCode: 110008)
          reset_session
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
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/checkFileForDownload/Logs/README.md"
          expect_json(errorCode: 200003)
          expect_status(401)
        end
        it "should fail to download file without a token" do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/fileDownload/Logs/README.md"
          expect_json(errorCode: 200003)
          expect_status(401)
        end
        it "should fail to download file with an empty string token" do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/fileDownload/Logs/README.md?token= "
          expect_json(errorCode: 200003)
          expect_status(401)
        end
        it "should fail to download file with an empty token" do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/fileDownload/Logs/README.md?token="
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end
      context 'with authentication and sufficient privileges' do
        before :all do
          with_valid_project
        end
        it "should download Logs/README.md" do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/checkFileForDownload/Logs/README.md"
          expect_status(200)
          token = json_body[:data][:value]
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/fileDownload/Logs/README.md?token=" + token 
          expect_status(200)
        end
        it "should fail to download with a token issued for a different file path" do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/checkFileForDownload/Resources/README.md"
          expect_status(200)
          token = json_body[:data][:value]
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/fileDownload/Logs/README.md?token=" + token 
          expect_status(401)
        end
        it "should fail to download more than one file with a single token" do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/checkFileForDownload/Resources/README.md"
          expect_status(200)
          token = json_body[:data][:value]
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/fileDownload/Resources/README.md?token=" + token 
          expect_status(200)
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/fileDownload/Resources/README.md?token=" + token 
          expect_status(401)
        end
        it 'should fail to download a file if variable download_allowed is false' do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/checkFileForDownload/Logs/README.md"
          expect_status(200)
          token = json_body[:data][:value]
          setVar("download_allowed", 'false')
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/checkFileForDownload/Logs/README.md"
          expect_status(403)
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/fileDownload/Logs/README.md?token=" + token
          expect_status(403)
          #set var back to true
          setVar("download_allowed", "true")
          expect(getVar("download_allowed").value). to eq "true"
        end
      end 
    end
  end
end
