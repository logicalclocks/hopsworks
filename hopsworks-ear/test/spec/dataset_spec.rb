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
describe 'dataset' do
  after (:all){clean_projects}
  describe "#create" do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "should fail" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/createTopLevelDataSet", {name: "dataset_#{Time.now.to_i}", description: "test dataset", searchable: true, generateReadme: true}
        expect_json(errorMsg: "Client not authorized for this invocation")
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
        expect_json(errorMsg: ->(value){ expect(value).to be_empty})
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
        expect_json(errorMsg: ->(value){ expect(value).to be_empty})
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
        expect_json(errorMsg: "Client not authorized for this invocation")
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
        expect_json(errorMsg: "Client not authorized for this invocation")
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
        expect_json(errorMsg: ->(value){ expect(value).to include("Permission denied:")})       
        expect_status(403)
        reset_session
      end
#      it "should fail to delete dataset belonging to someone else." do
#        with_valid_project
#        dsname = "dataset_#{short_random_id}"
#        create_dataset_by_name(@project, dsname)
#        member = create_user
#        add_member(member[:email], "Data owner")
#        create_session(member[:email],"Pass123")
#        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/#{dsname}"
#        expect_json(errorMsg: ->(value){ expect(value).to include("Permission denied")})
#        expect_status(403)
#      end
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
        expect_json(errorMsg: "Client not authorized for this invocation")
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
        expect_json(errorMsg: "")
        expect_json(successMessage: "Request sent successfully.")
        expect_status(200)
        create_session(@project[:username],"Pass123") # be the user of the project that owns the dataset
        get "#{ENV['HOPSWORKS_API']}/message"
        msg = json_body.detect { |e| e[:content].include? "Dataset name: #{@dataset[:inode_name]}" }
        expect(msg).not_to be_nil
      end
      it "should fail to send request to the same project" do
        post "#{ENV['HOPSWORKS_API']}/request/access", {inodeId: @dataset[:inode_id], projectId: @project[:id]}
        expect_json(errorMsg: "Project already contains dataset.")
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
        expect_json(errorMsg: "Client not authorized for this invocation")
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
        expect_json(errorMsg: "Your role in this project is not authorized to perform this action.")
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
        put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/permissions", {name: dsname, permissions: permissions }
        expect_status(400)
      end
      it "should fail to write on a non editable shared dataset" do
        projectname = "project_#{short_random_id}"
        project = create_project_by_name(projectname)
        dsname = "dataset_#{short_random_id}"
        ds = create_dataset_by_name(@project, dsname)
        share_dataset(@project, dsname, project)
        post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset", {name: "#{dsname + "/testdir"}"}
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
             {name: "#{@project[:projectname] + "::" + dsname + "/testdir"}"}
        expect_status(200)
        # Check if the directory is present
        get_datasets_in(@project, dsname)
        ds = json_body.detect { |d| d[:name] == "testdir"}
        expect(ds).to be_present
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
        expect_json(errorMsg: "Your role in this project is not authorized to perform this action.")
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
        expect_status(403)
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
        get_datasets_in(@project, @dataset[:name])
        createdDir = json_body.detect { |inode| inode[:name] == "afterDir" }
        expect(createdDir).to be_nil
      end
    end
  end
end
