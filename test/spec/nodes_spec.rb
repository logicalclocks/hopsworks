describe 'nodes' do
  let(:project_id){ with_valid_project['id']}
  let(:workflow){with_valid_workflow(project_id)}
  let(:node){with_valid_node(project_id, workflow[:id])}
  let(:valid_params){ valid_node_params }
  let(:invalid_params){ {type: nil} }
  before(:all){with_valid_project}

  describe "#index" do
    before(:all) do
      project_id = with_valid_project['id']
      @workflow1 = create_workflow(project_id)
      @node1 = create_node(project_id, @workflow1[:id])
    end
    context 'without authentication' do
      around :example do |example|
        workflow
        reset_session
        example.run
      end
      it "should fail" do
        get "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes"
        expect_status(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      it "should return all nodes" do
        get "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes"
        expect_json_sizes(3)
        expect_json_types(:array)
        expect_status(200)
      end

      it "should not include other project's nodes" do
        get "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes"
        ids = json_body.map{|body| body[:id]}
        expect(ids).to include("root", "end")
        expect(ids).not_to include(@node1[:id])
      end
    end
  end

  describe "#create" do
    context 'without authentication' do
      around :example do |example|
        workflow
        reset_session
        example.run
      end
      context "with valid params" do
        it "should fail" do
          post "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes", valid_params
          expect_status(401)
        end
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      context "with valid params" do
        it "should create a new node" do
          post "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes", valid_params
          expect_json(errorMsg: -> (value){ expect(value).to be_nil})
          expect_json_types(id: :string, workflowId: :int, type: :string, data: :object)
          expect_status(200)
        end

        it "should not be nil externalId" do
          post "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes", valid_params
          expect_status(200)
          expect_json(externalId: -> (value){ expect(value).to_not be_nil})
        end
      end
      context "with invalid params" do
        it "should fail" do
          post "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes", invalid_params
          expect_json(errorMsg: -> (value){ expect(value).not_to be_empty})
          expect_status(400)
        end
      end
    end
  end

  describe "#show" do
    context 'without authentication' do
      around :example do |example|
        node
        reset_session
        example.run
      end
      it "should fail" do
        get "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes/#{node[:id]}"
        expect_status(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      it "should return the node" do
        get "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes/#{node[:id]}"
        expect_json_types(id: :string, workflowId: :int, type: :string, data: :object)
        expect_json(id: node[:id])
        expect_json(type: node[:type])
        expect_status(200)
      end
      it "should fail trying to get unexitising node" do
        id = create_node(project_id, workflow[:id])[:id]
        delete "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes/#{id}"
        get "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes/#{id}"
        expect_json(errorMsg: 'Node not found.')
        expect_status(400)
      end
    end
  end

  describe "#update" do
    let(:valid_params) do
      params = valid_node_params
      params[:data] = {new: "stuff"}
      params.delete :id
      params
    end
    context 'without authentication' do
      around :example do |example|
        node
        reset_session
        example.run
      end
      context "with valid params" do
        it "should fail" do
          put "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes/#{node[:id]}", valid_params
          expect_status(401)
        end
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      context "with valid params" do
        it "should update a node" do
          put "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes/#{node[:id]}", valid_params
          expect_json(errorMsg: -> (value){ expect(value).to be_nil})
          expect_json_types(id: :string, workflowId: :int, type: :string, data: :object)
          expect_json(id: node[:id])
          expect_json(data: valid_params[:data])
          expect_status(200)
        end

        it "should not be nil externalId" do
          params = valid_params
          put "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes/#{node[:id]}", params
          expect_status(200)
          expect_json(externalId: -> (value){ expect(value).to_not be_nil})
        end

        it "should fail trying to update unexitising node" do
          id = create_node(project_id, workflow[:id])[:id]
          delete "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes/#{id}"
          put "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes/#{id}", valid_params
          expect_json(errorMsg: 'Node not found.')
          expect_status(400)
        end
      end

      context "with invalid params" do
        it "should fail" do
          put "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes/#{node[:id]}", invalid_params
          expect_json(errorMsg: -> (value){ expect(value).not_to be_empty})
          expect_status(400)
        end
      end
    end
  end

  describe "#delete" do
    let(:node){create_node(project_id, workflow[:id])}
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
      it "should remove a node" do
        delete "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes/#{node[:id]}"
        expect_json_types(id: :string, workflowId: :int, type: :string, data: :object)
        expect_json(id: node[:id])
        expect_status(200)
      end
      it "should fail trying to removed unexitising node" do
        id = create_node(project_id, workflow[:id])[:id]
        delete "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes/#{id}"
        delete "/hopsworks/api/project/#{project_id}/workflows/#{workflow[:id]}/nodes/#{id}"
        expect_json(errorMsg: 'Node not found.')
        expect_status(400)
      end
    end
  end

end
