describe 'projects' do
  describe "#create" do
    context 'without authentication' do
      before :all do
        reset_session
      end
      it "should fail" do
        post "/hopsworks/api/project", {projectName: "project_#{Time.now.to_i}", description:"", status: 0, services: ["JOBS","ZEPPELIN"], projectTeam:[], retentionPeriod: ""}
        expect_json(errorMsg: "Client not authorized for this invocation")
        expect_status(401)
      end
    end

    context 'with authentication' do
      before :all do
        with_valid_session
      end
      it 'should work with valid params' do
        post "/hopsworks/api/project", {projectName: "project_#{Time.now.to_i}", description:"", status: 0, services: ["JOBS","ZEPPELIN"], projectTeam:[], retentionPeriod: ""}
        expect_json(errorMsg: ->(value){ expect(value).to be_empty})
        expect_json(successMessage: "Project created successfully.")
        expect_status(201)
      end
      
      it 'should fail with invalid params' do
        post "/hopsworks/api/project", {projectName: "project_#{Time.now.to_i}"}
        expect_status(500)
      end
    end
  end
  describe "#access" do
    context 'without authentication' do
      before :all do
        reset_session
      end
      it "should fail to get project list" do
        get "/hopsworks/api/project/getAll"
        expect_json(errorMsg: "Client not authorized for this invocation")
        expect_status(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      it "should return project list" do
        get "/hopsworks/api/project/getAll"
        #expect_json()
        expect_status(200)
      end
    end
  end
end
