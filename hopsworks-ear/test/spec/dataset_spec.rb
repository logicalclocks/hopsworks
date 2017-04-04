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
        reset_session
      end
      it "should fail to delete dataset with insufficient privilege" do
        project = get_project
        member = create_user
        add_member(member[:email], "Data scientist")
        create_session(member[:email],"Pass123")
        delete "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/Logs"
        expect_json(errorMsg: "Your role in this project is not authorized to perform this action.")
        expect_status(403)
      end     
      it "should fail to delete dataset belonging to someone else." do
        with_valid_project
        dsname = "dataset_#{short_random_id}"
        create_dataset_by_name(@project, dsname)
        member = create_user
        #add_member(member[:email], "Data owner")
        create_session(member[:email],"Pass123")
        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/#{dsname}"
        expect_json(errorMsg: ->(value){ expect(value).to include("Permission denied")})
        expect_status(403)
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
      before :all do
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
    end
  end
end
