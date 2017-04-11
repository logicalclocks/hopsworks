describe "Workflow Job" do
  let(:project_id){ with_valid_project['id']}
  let(:valid_email_workflow){with_valid_email_workflow(project_id)}
  let(:valid_email_execution){with_valid_email_execution(project_id, valid_email_workflow[:id])}
  let(:valid_workflow_job){with_valid_workflow_job(project_id, valid_email_workflow[:id], valid_email_execution[:id])}
  after (:all){clean_projects}
  describe "#index" do

    context 'without authentication' do
      around :example do |example|
        valid_email_workflow
        valid_email_execution
        valid_workflow_job
        reset_session
        example.run
      end
      it "should fail" do
        get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{valid_email_workflow[:id]}/executions/#{valid_email_execution[:id]}/jobs"
        expect_status(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      it "should return all jobs" do
        get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{valid_email_workflow[:id]}/executions/#{valid_email_execution[:id]}/jobs"
        expect(json_body.count).to be > 0
        expect_json_types(:array)
        expect_status(200)
      end
    end
  end

  describe "#show" do
    context 'without authentication' do
      around :example do |example|
        valid_email_workflow
        valid_email_execution
        valid_workflow_job
        reset_session
        example.run
      end
      it "should fail" do
        get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{valid_email_workflow[:id]}/executions/#{valid_email_execution[:id]}/jobs/#{valid_workflow_job[:id]}"
        expect_status(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_session
      end
      it "should return the workflow" do
        get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{valid_email_workflow[:id]}/executions/#{valid_email_execution[:id]}/jobs/#{valid_workflow_job[:id]}"
        expect_json(errorMsg: ->(value){ expect(value).to be_nil})
        expect_json_types(id: :string, actions: :array, path: :string, status: :string)
        expect_status(200)
      end
      it "should fail trying to get unexitising workflow job" do
        id = Random.new.rand 100000
        get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/workflows/#{valid_email_workflow[:id]}/executions/#{valid_email_execution[:id]}/jobs/#{id}"
        expect_json(errorMsg: 'Job not found.')
        expect_status(400)
      end
    end
  end
end
