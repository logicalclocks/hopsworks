describe "Workflow Execution" do
  let(:project_id){ with_valid_project['id']}
  let(:invalid_workflow){with_valid_workflow(project_id)}
  let(:valid_email_execution){with_valid_email_execution(project_id, valid_email_workflow[:id])}
  let(:spark_node_data) do
    {
      mainClass: "org.apache.oozie.example.SparkFileCopy",
      jar: "/examples/apps/spark/lib/oozie-examples.jar",
      arguments: [
        "${nameNode}/examples/input-data/text/data.txt",
        "${nameNode}/examples/output-data/spark"
      ],
      rmDirs: [
        "/examples/output-data/spark"
      ]
    }
  end
  let(:email_node_data) do
      {
        to: "#{random_id}@email.com",
        subject: "test subject",
        body: "Lorem ipsum"
      }
  end

  let(:valid_email_workflow) do
    workflow = with_valid_workflow(project_id)
    types = workflow[:nodes].map{|node| node[:type]}
    return workflow if types == ["root-node", "email-node", "end-node"]

    node = workflow[:nodes].select{|node| node[:type] == "blank-node"}[0]
    edit_node(project_id, node, {data: email_node_data, type: "email-node"})
    reload_workflow(workflow)
  end

  let(:valid_spark_workflow) do
    workflow = with_valid_workflow(project_id)
    types = workflow[:nodes].map{|node| node[:type]}
    return workflow if types == ["root-node", "spark-custom-node", "end-node"]

    node = workflow[:nodes].select{|node| node[:type] == "blank-node"}[0]
    edit_node(project_id, node, {data: spark_node_data, type: "spark-custom-node"})
    reload_workflow(workflow)
  end

  describe "config" do
    it "should have expected spark workflow" do
      workflow = valid_spark_workflow
      types = workflow[:nodes].map{|node| node[:type]}
      expect(types).to contain_exactly("root-node", "spark-custom-node", "end-node")
    end
    it "should have a valid spark node" do
      workflow = valid_spark_workflow
      node = workflow[:nodes].select{|node| node[:type] == "spark-custom-node"}[0]
      expect(node[:data]).to match(spark_node_data)
    end

    it "should have expected email workflow" do
      workflow = valid_email_workflow
      types = workflow[:nodes].map{|node| node[:type]}
      expect(types).to contain_exactly("root-node", "email-node", "end-node")
    end
    it "should have a valid email node" do
      workflow = valid_email_workflow
      node = workflow[:nodes].select{|node| node[:type] == "email-node"}[0]
      expect(node[:data]).to match(email_node_data)
    end
  end

  before(:all){with_valid_project}
  after (:all){clean_projects}
  describe "#show" do
    context 'without authentication' do
      around :example do |example|
        valid_email_workflow
        valid_email_execution
        reset_session
        example.run
      end
      it "should fail" do
        get "/hopsworks/api/project/#{project_id}/workflows/#{valid_email_workflow[:id]}/executions/#{valid_email_execution[:id]}"
        expect_status(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      it "should return the workflow" do
        get "/hopsworks/api/project/#{project_id}/workflows/#{valid_email_workflow[:id]}/executions/#{valid_email_execution[:id]}"
        expect_json(errorMsg: ->(value){ expect(value).to be_nil})
        expect_json_types(id: :int, workflowTimestamp: :int, userId: :int, jobIds: :array, snapshot: :object)
        expect_json_keys('snapshot', [:edges, :nodes])
        expect_status(200)
      end
      it "should fail trying to get unexitising workflow execution" do
        id = Random.new.rand 100000
        get "/hopsworks/api/project/#{project_id}/workflows/#{valid_email_workflow[:id]}/executions/#{id}"
        expect_json(errorMsg: 'Execution not found.')
        expect_status(400)
      end
    end
  end

  describe "#logs" do
    context 'without authentication' do
      around :example do |example|
        valid_email_workflow
        valid_email_execution
        reset_session
        example.run
      end
      it "should fail" do
        get "/hopsworks/api/project/#{project_id}/workflows/#{valid_email_workflow[:id]}/executions/#{valid_email_execution[:id]}/logs"
        expect_status(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      it "should return the workflow" do
        get "/hopsworks/api/project/#{project_id}/workflows/#{valid_email_workflow[:id]}/executions/#{valid_email_execution[:id]}/logs"
        expect_json_types :array_of_objects
        expect_json_types('*', default: :string, error: :string, audit: :string, time: :string)
        expect_status(200)
      end
      it "should fail trying to get unexitising workflow" do
        id = Random.new.rand 100000
        get "/hopsworks/api/project/#{project_id}/workflows/#{valid_email_workflow[:id]}/executions/#{id}/logs"
        expect_json(errorMsg: 'Execution not found.')
        expect_status(400)
      end
    end
  end

  describe "#create" do
    describe "for spark node" do
      context 'without authentication' do
        context "with valid params" do
          it "should fail" do
            valid_spark_workflow
            reset_session
            post "/hopsworks/api/project/#{project_id}/workflows/#{valid_spark_workflow[:id]}/executions"
            expect_status(401)
          end
        end
      end
      context 'with authentication' do
        before :all do
          with_valid_session
        end
        context "with valid params" do
          it "should create a new execution" do
            post "/hopsworks/api/project/#{project_id}/workflows/#{valid_spark_workflow[:id]}/executions"
            expect_json(errorMsg: ->(value){ expect(value).to be_nil})
            expect_json_types(id: :int, workflowId: :int, userId: :int)
            expect_status(200)
          end

          it "should increase job count" do
            post "/hopsworks/api/project/#{project_id}/workflows/#{valid_spark_workflow[:id]}/executions"
            sleep(15.seconds)
            post "/hopsworks/api/project/#{project_id}/workflows/#{valid_spark_workflow[:id]}/executions"
            job_count = json_body[:jobIds].count
            expect_status(200)
            id = json_body[:id]
            sleep(15.seconds)
            get "/hopsworks/api/project/#{project_id}/workflows/#{valid_spark_workflow[:id]}/executions/#{id}"
            expect_json_sizes(jobIds: job_count+1)
            expect_status(200)
          end
        end
      end
    end

    describe "for email node" do
      context 'without authentication' do
        context "with valid params" do
          it "should fail" do
            valid_email_workflow
            reset_session
            post "/hopsworks/api/project/#{project_id}/workflows/#{valid_email_workflow[:id]}/executions"
            expect_status(401)
          end
        end
      end
      context 'with authentication' do
        before :all do
          with_valid_session
        end
        context "with valid params" do
          it "should create a new execution" do
            post "/hopsworks/api/project/#{project_id}/workflows/#{valid_email_workflow[:id]}/executions"
            expect_json(errorMsg: ->(value){ expect(value).to be_nil})
            expect_json_types(id: :int, workflowId: :int, userId: :int)
            expect_status(200)
          end

          it "should increase job count" do
            post "/hopsworks/api/project/#{project_id}/workflows/#{valid_email_workflow[:id]}/executions"
            sleep(15.seconds)
            post "/hopsworks/api/project/#{project_id}/workflows/#{valid_email_workflow[:id]}/executions"
            job_count = json_body[:jobIds].count
            expect_status(200)
            id = json_body[:id]
            sleep(15.seconds)
            get "/hopsworks/api/project/#{project_id}/workflows/#{valid_email_workflow[:id]}/executions/#{id}"
            expect_json_sizes(jobIds: job_count+1)
            expect_status(200)
          end
        end
      end
    end
  end
end
