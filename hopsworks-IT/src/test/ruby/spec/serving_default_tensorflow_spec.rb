=begin
 This file is part of Hopsworks
 Copyright (C) 2022, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end

# serving_default_tensorflow_spec.rb: Tests for serving tensorflow models on default deployments

require 'json'

describe "On #{ENV['OS']}" do
  
  before :all do
    # ensure data science profile is enabled
    @enable_data_science_profile = getVar('enable_data_science_profile')
    setVar('enable_data_science_profile', "true")
  end

  after :all do
    clean_all_test_projects(spec: "serving_default_tensorflow")
    purge_all_tf_serving_instances
    setVar('enable_data_science_profile', @enable_data_science_profile[:value])
  end

  describe "#create" do
    before :all do
      with_valid_project
      copy_mnist_files(@project[:projectname], @user[:username])
    end

    # model path

    it "should fail to create a serving with a non-standard path" do
      rm("/Projects/#{@project[:projectname]}/Models/mnist/1/saved_model.pb")

      put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
          {name: "testModel6",
           modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
            batchingConfiguration: {
              batchingEnabled: false
            },
           modelVersion: 1,
           modelServer: "TENSORFLOW_SERVING",
           modelFramework: "TENSORFLOW",
           servingTool: "DEFAULT",
           requestedInstances: 1
          })
      expect_status_details(400, error_code: 240017)
      expect_json(usrMsg: "Model path does not respect the Tensorflow standard")

      saved_model_path = "#{MNIST_TOUR_DATA_LOCATION}/model/1/saved_model.pb"
      copy(saved_model_path, "/Projects/#{@project[:projectname]}/Models/mnist/1", "#{@user[:username]}", "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")
    end

    # artifact version

    it "should fail to create a serving with a non-zero artifact version" do
      put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
          {name: "testModel5",
          modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
          batchingConfiguration: {
            batchingEnabled: false
          },
          modelVersion: 1,
          artifactVersion: 99,
          modelServer: "TENSORFLOW_SERVING",
          modelFramework: "TENSORFLOW",
          servingTool: "DEFAULT",
          requestedInstances: 1,
          requestedTransformerInstances: 1
          })
      if kubernetes_installed
        expect_status_details(400, error_code: 240018)
        expect_json(usrMsg: "Default deployments with Tensorflow Serving only support MODEL-ONLY artifacts")
      else
        expect_status_details(400, error_code: 240014)
        expect_json(usrMsg: "Artifacts only supported in Kubernetes deployments")
      end
    end

    # predictor

    it "should fail to create a serving with predictor" do
      if kserve_installed
        skip "This test only runs without KServe installed"
      end

      put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
          {name: "testModelwithPredictor",
            modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
            modelVersion: 1,
            batchingConfiguration: {
              batchingEnabled: false
            },
            kafkaTopicDTO: {
                name: "NONE"
            },
            modelServer: "TENSORFLOW_SERVING",
            modelFramework: "TENSORFLOW",
            servingTool: "DEFAULT",
            predictor: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
            requestedInstances: 1,
            requestedTransformerInstances: 1
          })
      expect_status_details(400, error_code: 240020)
    end
    
    # request batching

    it "should create a serving with request batching" do
      put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
          {name: "testRequestBatchingTensorflowDefault1",
           modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
           modelVersion: 1,
           batchingConfiguration: {
             batchingEnabled: true
           },
           modelServer: "TENSORFLOW_SERVING",
           modelFramework: "TENSORFLOW",
           servingTool: "DEFAULT",
           requestedInstances: 1
          })
      expect_status_details(201)
    end

    it 'should fail to create a serving with request batching enabled (with extra values set)' do
      put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
          {name: "testRequestBatchingTensorflowDefault2",
           modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
           modelVersion: 1,
           batchingConfiguration: {
             batchingEnabled: true,
             timeout: 60
           },
           modelServer: "TENSORFLOW_SERVING",
           modelFramework: "TENSORFLOW",
           servingTool: "DEFAULT",
           requestedInstances: 1
          })
      expect_status_details(400, error_code: 240025)
      expect_json(usrMsg: "Fine-grained request batching is only supported in KServe deployments")
    end
  end

  describe "#update", vm: true do
    before :all do
      with_valid_project
      with_tensorflow_serving(@project[:id], @project[:projectname], @user[:username])
      start_serving(@project, @serving)
      wait_for_type(@serving[:name])
    end

    after :all do
      purge_all_tf_serving_instances
      delete_all_servings(@project[:id])
    end

    after :each do
      serving = Serving.find(@serving[:id])
      wait_for_type(serving[:name])
    end

    # request batching

    it "should be able to update the request batching" do
      serving = Serving.find(@serving[:id])
      topic = ProjectTopics.find(@serving[:kafka_topic_id])
      put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
          {id: serving[:id],
           name: serving[:name],
           modelPath: serving[:model_path],
           modelVersion: serving[:model_version],
           batchingConfiguration: {
             batchingEnabled: true
           },
           kafkaTopicDTO: {
               name: topic[:topic_name]
           },
           inferenceLogging: parse_inference_logging(serving[:inference_logging]),
           modelServer: parse_model_server(serving[:model_server]),
           modelFramework: parse_model_framework(serving[:model_framework]),
           servingTool: parse_serving_tool(serving[:serving_tool]),
           requestedInstances: serving[:instances]
          })
      expect_status_details(201)

      serving = Serving.find(@serving[:id])
      expect(JSON.parse(serving[:batching_configuration])['batchingEnabled']).to eql true
    end
  end

  # Tests for #start, #stop and #delete are run in serving_spec.rb, and removed from here to avoid duplication.
  # serving_spec.rb contains common tests. To run these tests, it's necessary to pick one model server/serving tool
  # combination as an example. Since this combination is default deployments of tensorflow models, we don't need to
  # duplicate the same tests here.
end
