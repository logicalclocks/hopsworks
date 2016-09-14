describe 'workflows' do
  let(:project_id){ with_valid_project['id']}
  let(:workflow){with_valid_workflow(project_id)}
  let(:valid_params){ {name: "workflow_#{short_random_id}"} }
  let(:invalid_params){ {name: nil} }
  before(:all){with_valid_project}
  after (:all){clean_projects}
  describe "#index" do
    before :all do
      project_id = with_valid_project['id']
      project2 = create_project
      @workflow1 = create_workflow(project_id)
      @workflow2 = create_workflow(project2['id'])
      @workflow3 = create_workflow(project_id)
    end
    context 'without authentication' do
      before :all do
        reset_session
      end
      it "should fail" do
        get "/hopsworks/api/project/#{project_id}/workflows"
        expect_status(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      it "should return all workflows" do
        get "/hopsworks/api/project/#{project_id}/workflows"
        expect_json_sizes(2)
        expect_json_types(:array)
        expect_status(200)
      end

      it "should not include other project's workflow" do
        get "/hopsworks/api/project/#{project_id}/workflows"
        ids = json_body.map{|body| body[:id]}
        expect(ids).to contain_exactly(@workflow1[:id], @workflow3[:id])
        expect(ids).not_to include(@workflow2[:id])
      end
    end
  end

  describe "#create" do
    context 'without authentication' do
      before :all do
        reset_session
      end
      context "with valid params" do
        it "should fail" do
          post "/hopsworks/api/project/#{project_id}/workflows", valid_params
          expect_status(401)
        end
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      context "with valid params" do
        it "should create a new workflow" do
          post "/hopsworks/api/project/#{project_id}/workflows", valid_params
          expect_json(errorMsg: -> (value){ expect(value).to be_nil})
          expect_json_types(id: :int, projectId: :int, nodes: :array_of_objects, edges: :array_of_objects)
          expect_status(200)
        end
      end
      context "with invalid params" do
        it "should fail" do
          post "/hopsworks/api/project/#{project_id}/workflows", invalid_params
          expect_json(errorMsg: -> (value){ expect(value).not_to be_empty})
          expect_status(400)
        end
      end
    end
  end

  describe "#show" do
    context 'without authentication' do
      around :example do |example|
        workflow
        reset_session
        example.run
      end
      it "should fail" do
        get "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}"
        expect_status(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      it "should return the workflow" do
        get "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}"
        expect_json(errorMsg: -> (value){ expect(value).to be_nil})
        expect_json_types(id: :int, projectId: :int, nodes: :array_of_objects, edges: :array_of_objects)
        expect_json(id: workflow[:id])
        expect_json(name: workflow[:name])
        expect_status(200)
      end
      it "should fail trying to get unexitising workflow" do
        id = create_workflow(project_id)[:id]
        delete "/hopsworks/api/project/#{project_id}/workflows/#{id}"
        get "/hopsworks/api/project/#{project_id}/workflows/#{id}"
        expect_json(errorMsg: 'Workflow not found.')
        expect_status(400)
      end
    end
  end

  describe "#update" do
    context 'without authentication' do
      around :example do |example|
        workflow
        reset_session
        example.run
      end
      context "with valid params" do
        it "should fail" do
          put "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}", valid_params
          expect_status(401)
        end
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      context "with valid params" do
        it "should update a workflow" do
          put "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}", valid_params
          expect_json(errorMsg: -> (value){ expect(value).to be_nil})
          expect_json_types(id: :int, projectId: :int, nodes: :array_of_objects, edges: :array_of_objects)
          expect_json(id: workflow[:id])
          expect_json(name: valid_params[:name])
          expect_status(200)
        end
        it "should fail trying to update unexitising workflow" do
          id = create_workflow(project_id)[:id]
          delete "/hopsworks/api/project/#{project_id}/workflows/#{id}"
          put "/hopsworks/api/project/#{project_id}/workflows/#{id}", valid_params
          expect_json(errorMsg: 'Workflow not found.')
          expect_status(400)
        end
      end

      context "with invalid params" do
        it "should fail" do
          put "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}", invalid_params
          expect_json(errorMsg: -> (value){ expect(value).not_to be_empty})
          expect_status(400)
        end
      end
    end
  end

  describe "#delete" do
    let(:workflow){create_workflow(project_id)}
    context 'without authentication' do
      around :example do |example|
        workflow
        reset_session
        example.run
      end
      it "should fail" do
        delete "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}"
        expect_status(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      it "should remove a workflow" do
        delete "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}"
        expect_json_types(id: :int, projectId: :int, nodes: :array_of_objects, edges: :array_of_objects)
        expect_json(id: workflow[:id])
        expect_status(200)
      end
      it "should fail trying to removed unexitising workflow" do
        id = workflow[:id]
        delete "/hopsworks/api/project/#{project_id}/workflows/#{id}"
        delete "/hopsworks/api/project/#{project_id}/workflows/#{id}"
        expect_json(errorMsg: 'Workflow not found.')
        expect_status(400)
      end
    end
  end

end
