describe 'edges' do
  let(:project_id){ with_valid_project['id']}
  let(:workflow){with_valid_workflow(project_id)}
  let(:edge){with_valid_edge(project_id, workflow[:id])}
  let(:valid_params){ valid_edge_params(project_id, workflow[:id]) }
  let(:invalid_params){ {targetId: "non-existing"} }
  before(:all){with_valid_project}
  after (:all){clean_projects}
  describe "#index" do
    before(:all) do
      project_id = with_valid_project['id']
      @workflow1 = create_workflow(project_id)
      @edge1 = create_edge(project_id, @workflow1[:id])
    end
    context 'without authentication' do
      around :example do |example|
        workflow
        reset_session
        example.run
      end
      it "should fail" do
        get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges"
        expect_json(errorMsg: "Client not authorized for this invocation")
        expect_status(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      it "should return all edges" do
        get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges"
        expect_json_sizes(2)
        expect_json_types(:array)
        expect_status(200)
      end

      it "should not include other project's edges" do
        get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges"
        node_ids = json_body.map{|body| [body[:sourceId], body[:targetId]]}.flatten
        ids = json_body.map{|body| body[:id]}
        expect(node_ids).to include("root", "end")
        expect(ids).not_to include(@edge1[:id])
      end
    end
  end

  describe "#create" do
    context 'without authentication' do
      around :example do |example|
        workflow
        valid_params
        reset_session
        example.run
      end
      context "with valid params" do
        it "should fail" do
          post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges", valid_params
          expect_json(errorMsg: "Client not authorized for this invocation")
          expect_status(401)
        end
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      context "with valid params" do
        context "with existing source and target" do
          it "should create a new edge" do
            post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges", valid_params
            expect_json(errorMsg: ->(value){ expect(value).to be_nil})
            expect_json_types(id: :string, workflowId: :int)
            expect_status(200)
          end
        end
        context "with existing source only" do
          it "should fail" do
            params = valid_params
            params[:sourceId] = random_id
            post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges", params
            expect_json(errorMsg: ->(value){ expect(value).not_to be_empty})
            expect_status(400)
          end
        end
        context "with existing target only" do
          it "should fail" do
            params = valid_params
            params[:targetId] = random_id
            post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges", params
            expect_json(errorMsg: ->(value){ expect(value).not_to be_empty})
            expect_status(400)
          end
        end
      end
      context "with invalid params" do
        it "should fail" do
          post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges", invalid_params
          expect_json(errorMsg: ->(value){ expect(value).not_to be_empty})
          expect_status(400)
        end
      end
    end
  end

  describe "#show" do
    context 'without authentication' do
      around :example do |example|
        edge
        reset_session
        example.run
      end
      it "should fail" do
        get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges/#{edge[:id]}"
        expect_json(errorMsg: "Client not authorized for this invocation")
        expect_status(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      it "should return the edge" do
        get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges/#{edge[:id]}"
        expect_json_types(id: :string, workflowId: :int)
        expect_json(id: edge[:id])
        expect_json(type: edge[:type])
        expect_status(200)
      end
      it "should fail trying to get unexitising edge" do
        id = create_edge(project_id, workflow[:id])[:id]
        delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges/#{id}"
        get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges/#{id}"
        expect_json(errorMsg: 'Edge not found.')
        expect_status(400)
      end
    end
  end

  describe "#update" do
    let(:valid_params) do
      {type: "new-type"}
    end
    context 'without authentication' do
      around :example do |example|
        edge
        valid_params
        reset_session
        example.run
      end
      context "with valid params" do
        it "should fail" do
          put "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges/#{edge[:id]}", valid_params
          expect_json(errorMsg: "Client not authorized for this invocation")
          expect_status(401)
        end
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      context "with valid params" do
        it "should update a edge" do
          put "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges/#{edge[:id]}", valid_params
          expect_json(errorMsg: ->(value){ expect(value).to be_nil})
          expect_json_types(id: :string, workflowId: :int)
          expect_json(id: edge[:id])
          expect_json(type: valid_params[:type])
          expect_status(200)
        end
        it "should fail trying to update unexitising edge" do
          id = create_edge(project_id, workflow[:id])[:id]
          delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges/#{id}"
          put "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges/#{id}", valid_params
          expect_json(errorMsg: 'Edge not found.')
          expect_status(400)
        end
      end

      context "with invalid params" do
        it "should fail" do
          put "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges/#{edge[:id]}", invalid_params
          expect_json(errorMsg: ->(value){ expect(value).not_to be_empty})
          expect_status(400)
        end
      end
    end
  end

  describe "#delete" do
    let(:edge){create_edge(project_id, workflow[:id])}
    context 'without authentication' do
      around :example do |example|
        workflow
        reset_session
        example.run
      end
      it "should fail" do
        delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}"
        expect_json(errorMsg: "Client not authorized for this invocation")
        expect_status(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      it "should remove a edge" do
        delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges/#{edge[:id]}"
        expect_json_types(id: :string, workflowId: :int)
        expect_json(id: edge[:id])
        expect_status(200)
      end
      it "should fail trying to removed unexitising edge" do
        id = create_edge(project_id, workflow[:id])[:id]
        delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges/#{id}"
        delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{workflow[:id]}/edges/#{id}"
        expect_json(errorMsg: 'Edge not found.')
        expect_status(400)
      end
    end
  end

end
