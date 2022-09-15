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

# serving_default_spec.rb: Tests for serving models on default deployments regardless the model server

require 'json'

describe "On #{ENV['OS']}" do
  after (:all) do
    clean_all_test_projects(spec: "serving_default")
    purge_all_tf_serving_instances
  end

  describe "#create" do
    before :all do
      with_valid_project
      copy_mnist_files(@project[:projectname], @user[:username])
    end

    # kafka topic

    it "should create a serving with an existing kafka topic with inferenceschema version 1" do
      # Create kafka topic
      json, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, 1)

      # Create serving
      name = "testModel2"
      put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
          {name: name,
           modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
           modelVersion: 1,
           batchingConfiguration: {
              batchingEnabled: false
           },
           kafkaTopicDTO: {
               name: topic_name
           },
           inferenceLogging: "ALL",
           modelServer: "TENSORFLOW_SERVING",
           modelFramework: "TENSORFLOW",
           servingTool: "DEFAULT",
           requestedInstances: 1
           }
      expect_status_details(201)

      # Kafka authorizer needs some time to take up the new permissions.
      sleep(5)

      # Check that the serving is actually using that topic
      serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
      kafka_topic_name = JSON.parse(serving_list).select { |serving| serving['name'] == name }[0]['kafkaTopicDTO']['name']
      expect(kafka_topic_name).to eql topic_name
    end

    it "should create a serving with an existing kafka topic with inferenceschema version 2" do
      # Create kafka topic
      json, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, 2)

      # Create serving
      name = "testModel3"
      put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
          {name: name,
           modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
           modelVersion: 1,
           batchingConfiguration: {
              batchingEnabled: false
           },
           kafkaTopicDTO: {
               name: topic_name
           },
           inferenceLogging: "ALL",
           modelServer: "TENSORFLOW_SERVING",
           modelFramework: "TENSORFLOW",
           servingTool: "DEFAULT",
           requestedInstances: 1
          }
      expect_status_details(201)

      # Kafka authorizer needs some time to take up the new permissions.
      sleep(5)

      # Check that the serving is actually using that topic
      serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
      kafka_topic_name = JSON.parse(serving_list).select { |serving| serving['name'] == name }[0]['kafkaTopicDTO']['name']
      expect(kafka_topic_name).to eql topic_name
    end

    it "should create a serving with an existing kafka topic with inferenceschema version 3" do
      # Create kafka topic
      json, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, 3)

      # Create serving
      name = "testModel4"
      put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
          {name: name,
           modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
           modelVersion: 1,
           batchingConfiguration: {
              batchingEnabled: false
           },
           kafkaTopicDTO: {
               name: topic_name
           },
           inferenceLogging: "ALL",
           modelServer: "TENSORFLOW_SERVING",
           modelFramework: "TENSORFLOW",
           servingTool: "DEFAULT",
           requestedInstances: 1
          }
      expect_status_details(201)

      # Kafka authorizer needs some time to take up the new permissions.
      sleep(5)

      # Check that the serving is actually using that topic
      serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
      kafka_topic_name = JSON.parse(serving_list).select { |serving| serving['name'] == name }[0]['kafkaTopicDTO']['name']
      expect(kafka_topic_name).to eql topic_name
    end

    it "should fail to create a serving with an existing kafka topic with inferenceschema version 4" do
      # Create kafka topic
      json, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, 4)

      # Create serving
      put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
          {name: "testModel5",
           modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
           modelVersion: 1,
           batchingConfiguration: {
              batchingEnabled: false
           },
           kafkaTopicDTO: {
               name: topic_name
           },
           inferenceLogging: "ALL",
           modelServer: "TENSORFLOW_SERVING",
           modelFramework: "TENSORFLOW",
           servingTool: "DEFAULT",
           requestedInstances: 1
          }
      expect_status_details(400, error_code: 240023)
      expect_json(usrMsg: "Inference logging in default deployments requires schema version 3 or lower")
    end

    it "should fail to create a serving with fine-grained inference logging" do
      put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
          {name: "testmodelFineGrainedInferenceLogging",
           modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
           batchingConfiguration: {
              batchingEnabled: false
           },
           kafkaTopicDTO: {
            name: "CREATE"
           },
           inferenceLogging: "PREDICTIONS",
           modelVersion: 1,
           modelServer: "TENSORFLOW_SERVING",
           modelFramework: "TENSORFLOW",
           servingTool: "DEFAULT",
           requestedInstances: 1
          }
      expect_status_details(400, error_code: 240024)
      expect_json(usrMsg: "Fine-grained inference logging only supported in KServe deployments")
    end

    # transformer

    it "should fail to create a serving with transformer" do
      if kserve_installed
        skip "This test only runs without KServe installed"
      end

      put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
          {name: "testModelwithTransformer",
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
            transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
            requestedInstances: 1,
            requestedTransformerInstances: 1
          }
      expect_status_details(400, error_code: 240021)
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

    # kafka topic

    it "should fail to update the inference logging mode" do
      serving = Serving.find(@serving[:id])
      topic = ProjectTopics.find(@serving[:kafka_topic_id])
      put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
          {id: serving[:id],
           name: serving[:name],
           modelPath: serving[:model_path],
           modelVersion: serving[:model_version],
           batchingConfiguration: serving[:batching_configuration],
           kafkaTopicDTO: {
               name: topic[:topic_name]
           },
           inferenceLogging: "PREDICTIONS",
           modelServer: parse_model_server(serving[:model_server]),
           modelFramework: parse_model_framework(serving[:model_framework]),
           servingTool: parse_serving_tool(serving[:serving_tool]),
           requestedInstances: serving[:instances]
          }
      expect_status_details(400, error_code: 240024)
      expect_json(usrMsg: "Fine-grained inference logging only supported in KServe deployments")
    end
  end
end
