describe 'tfserving' do
  after (:all){clean_projects}
  describe "#create" do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "not authenticated" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/tfserving/", {hdfsModelPath: "hdfs:///Projects/#{@project[:projectname]}/Models/mnist/1/model.pb", enableBatching: true}
        expect_json(errorMsg: "Client not authorized for this invocation")
        expect_status(401)
      end
    end

    context 'with authentication' do
      before :all do
        with_valid_project
      end
      it 'models dataset should exist' do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/createTopLevelDataSet", {name: "Models", description: "test dataset", searchable: true, generateReadme: true}
        # expect_json(errorMsg: "Invalid folder name for DataSet: A directory with the same name already exists. If you want to replace it delete it first then try recreating.")
        expect_status(400)
      end
      it 'non-existing .pb file should fail creating serving' do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/tfserving/", {hdfsModelPath: "hdfs:///Projects/#{@project[:projectname]}/Models/mnist/1/model.pb", enableBatching: true}
        expect_status(404)
      end
      it 'serving lifecycle' do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/", {name: "Models/mnist", description: "", searchable: true, template: 0}
        expect_status(200)
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/", {name: "Models/mnist/1", description: "", searchable: true, template: 0}
        expect_status(200)
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/", {name: "Models/mnist/1/mnist.pb", description: "", searchable: true, template: 0}
        expect_status(200)

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/tfserving/", {hdfsModelPath: "hdfs:///Projects/#{@project[:projectname]}/Models/mnist/1/mnist.pb", enableBatching: true}
        expect_status(201)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/tfserving/"
        expect_status(200)

        serving = json_body.detect { |serving| serving[:modelName] == "mnist" }
        serving_id = serving[:id]

        # Start the serving
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/tfserving/start/#{serving_id}"
        expect_status(200)

        # Get the logs
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/tfserving/logs/#{serving_id}"
        expect_status(200)

        # Stop the serving
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/tfserving/stop/#{serving_id}"
        expect_status(200)

        # Delete serving
        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/tfserving/#{serving_id}"
        expect_status(200)
      end
    end
  end
end
