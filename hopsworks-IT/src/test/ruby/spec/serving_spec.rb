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

# serving_spec.rb: Common tests for serving models regardless the model server and serving tool.

require 'json'

describe "On #{ENV['OS']}" do
  after (:all) do
    clean_all_test_projects(spec: "serving")
    purge_all_tf_serving_instances
  end

  describe "#create" do

    context 'without authentication' do
      before :all do
        with_valid_project
        copy_mnist_files(@project[:projectname], @user[:username])
        reset_session
      end

      it "should fail to create a serving" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testModel",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             modelVersion: 1,
             batchingEnabled: false,
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_status_details(401, error_code: 200003)
      end
    end

    context 'with authentication', vm: true do
      before :all do
        with_valid_project
        copy_mnist_files(@project[:projectname], @user[:username])
      end

      # serving name

      it "should fail to create a serving without a name" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             batchingEnabled: false,
             modelVersion: 1,
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_json(usrMsg: "Serving name not provided")
      end

      it "should fail to create a serving with space in the name" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "test Model1",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             modelVersion: 1,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: "CREATE",
                 numOfPartitions: 1,
                 numOfReplicas: 1
             },
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_json(usrMsg: "Serving name cannot contain spaces")
        expect_status(422)
      end

      it "should fail to create a serving with an existing name" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testModelExistingName",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             modelVersion: 1,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: "CREATE",
                 numOfPartitions: 1,
                 numOfReplicas: 1
             },
             inferenceLogging: "ALL",
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_status_details(201)

        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
           {name: "testModelExistingName",
            modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
            modelVersion: 1,
            batchingEnabled: false,
            kafkaTopicDTO: {
                name: "CREATE",
                numOfPartitions: 1,
                numOfReplicas: 1
            },
            inferenceLogging: "ALL",
            modelServer: "TENSORFLOW_SERVING",
            servingTool: "DEFAULT",
            requestedInstances: 1
            }
        expect_status_details(400, error_code: 240011)
        expect_json(errorMsg: "An entry with the same name already exists in this project")
      end

      # model path

      it "should fail to create a serving without a path" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testModel3",
             batchingEnabled: false,
             modelVersion: 1,
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_json(usrMsg: "Model path not provided")
      end

      it "should fail to create a serving with a non-existing path" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testModelNonExistingPath",
             modelPath: "/Projects/#{@project[:projectname]}/DOESNTEXISTS",
             batchingEnabled: false,
             modelVersion: 1,
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_status_details(400, error_code: 240006)
      end

      it "should set the model name from model path" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testmodel28",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             modelVersion: 1,
             batchingEnabled: false,
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_status_details(201)

        serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
        serving = JSON.parse(serving_list).select { |serving| serving['name'] == "testmodel28"}[0]
        expect(serving['modelName']).to eq "mnist"
      end

      # model version

      it "should fail to create a serving without a model version" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testModel4",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             batchingEnabled: false,
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_json(usrMsg: "Model version not provided")
      end

      it "should fail to create a serving with a non-existing model version" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testModelNoMV",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             modelVersion: 99,
             batchingEnabled: false,
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_status_details(400, error_code: 240006)
      end

      # model server

      it "should fail to create a serving without model server" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testmodel5",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             modelVersion: 1,
             batchingEnabled: false,
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_json(usrMsg: "Model server not provided or unsupported")
        expect_status(422)
      end

      it "should fail to create a serving with an invalid model server" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testmodel5",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             modelVersion: 1,
             batchingEnabled: false,
             modelServer: "INVALID",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_json(usrMsg: "Model server not provided or unsupported")
        expect_status(422)
      end

      # serving tool

      it "should fail to create a serving without serving tool" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testmodel6",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             modelVersion: 1,
             batchingEnabled: false,
             modelServer: "TENSORFLOW_SERVING",
             requestedInstances: 1
            }
        expect_json(usrMsg: "Serving tool not provided or invalid")
        expect_status(422)
      end

      
      it "should fail to create a serving with an invalid serving tool" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testmodel6",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             modelVersion: 1,
             batchingEnabled: false,
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "INVALID",
             requestedInstances: 1
            }
        expect_json(usrMsg: "Serving tool not provided or invalid")
        expect_status(422)
      end

      # kafka topic

      it "should fail to create a serving with bad kafka configuration" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testModelBadKafka",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             modelVersion: 1,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: "CREATE",
                 numOfPartitions: -10,
                 numOfReplicas: 5
             },
             inferenceLogging: "ALL",
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_json(errorMsg: "Maximum topic replication factor exceeded")
        expect_status(400)
      end

      it "should create a serving without kafka topic" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testModelNoKafka",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             modelVersion: 1,
             batchingEnabled: false,
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_status_details(201)

        serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
        kafka_topic = JSON.parse(serving_list).select { |serving| serving['name'] == "testModelNoKafka"}[0]['kafkaTopicDTO']
        expect(kafka_topic).to be nil
      end

      it "should create the serving with a new kafka topic" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testModelNewKafka",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             modelVersion: 1,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: "CREATE",
                 numOfPartitions: 1,
                 numOfReplicas: 1
             },
             inferenceLogging: "ALL",
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_status_details(201)

        # Kafka authorizer needs some time to take up the new permissions.
        sleep(5)

        # Check that the topic has been created correctly
        serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
        kafka_topic_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/kafka/topics"

        kafka_topic_name = JSON.parse(serving_list).select { |serving| serving['name'] == "testModelNewKafka"}[0]['kafkaTopicDTO']['name']
        kafka_topic = JSON.parse(kafka_topic_list)['items'].select { |topic| topic['name'] == kafka_topic_name}
        expect(kafka_topic.size).to eq 1
        expect(kafka_topic[0]['schemaName']).to eql INFERENCE_SCHEMA_NAME
      end

      it "should fail to create a serving with an existing kafka topic using other than inferenceschema schema" do
        # Create Kafka Schema
        json_result, schema_name = add_schema(@project[:id])

        # Create kafka topic
        json, topic_name = add_topic(@project[:id], schema_name, 1)

        # Create serving
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testModelExistingKafkaNoSchema",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             modelVersion: 1,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: topic_name
             },
             inferenceLogging: "ALL",
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_status_details(400, error_code: 240023)
        expect_json(usrMsg: "Inference logging requires a Kafka topic with schema 'inferenceschema'")
      end

      it "should fail to create a serving with a non-existing kafka topic" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testModelNonExistingKafka",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             modelVersion: 1,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: "NON-EXISTING"
             },
             inferenceLogging: "ALL",
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_status_details(400, error_code: 240022)
      end

      # inference logging mode

      it "should fail to create a serving with kafka topic but without inference logging defined" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testmodelWithoutInferenceLogging",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: "CREATE"
             },
             modelVersion: 1,
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_status_details(422)
        expect_json(usrMsg: "A valid inference logger mode must be provided with a Kafka topic")
      end

      it "should fail to create a serving with inference logging mode but without kafka topic" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testmodelWithInferenceLoggingButNotTopic",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             batchingEnabled: false,
             modelVersion: 1,
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             inferenceLogging: "ALL",
             requestedInstances: 1
            }
        expect_status_details(422)
        expect_json(usrMsg: "Inference logger mode cannot be provided without a Kafka topic")
      end

      it "should fail to create a serving with invalid inference logging mode" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testmodelInvalidInferenceLogging",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             batchingEnabled: false,
             kafkaTopicDTO: {
              name: "CREATE"
             },
             inferenceLogging: "INVALID",
             modelVersion: 1,
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_status_details(422)
        expect_json(usrMsg: "A valid inference logger mode must be provided with a Kafka topic")
      end

      # resources config

      it "should fail to create a serving without predictor requested instances" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testmodel7",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
               modelVersion: 1,
               batchingEnabled: false,
               modelServer: "TENSORFLOW_SERVING",
               servingTool: "DEFAULT"
              }
        expect_json(usrMsg: "Number of instances not provided")
      end

      # artifact

      it "should fail to create a serving with artifact when kubernetes is not installed" do
        if kubernetes_installed
          skip "This test only runs without Kubernetes installed"
        end

        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "mnist",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             modelVersion: 1,
             artifactVersion: 1,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: "NONE"
             },
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_status_details(400, error_code: 240014)
        expect_json(errorMsg: "Kubernetes is not installed", usrMsg: "Artifacts only supported in Kubernetes deployments")
      end

      it "should create the serving and zipped model artifact when kubernetes is installed" do
        if !kubernetes_installed
          skip "This test only runs with Kubernetes installed"
        end

        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {name: "testmodelzipped",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist",
             modelVersion: 1,
             batchingEnabled: false,
             modelServer: "TENSORFLOW_SERVING",
             servingTool: "DEFAULT",
             requestedInstances: 1
            }
        expect_status_details(201)
      end
    end
  end

  describe "#update", vm: true do

    context 'without authentication' do
      before :all do
        with_valid_project
        with_tensorflow_serving(@project[:id], @project[:projectname], @user[:username])
        reset_session
      end

      it "should fail to update a serving" do
        serving = Serving.find(@serving[:id])
        topic = ProjectTopics.find(@serving[:kafka_topic_id])

        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
           {id: serving[:id],
            name: serving[:name],
            modelPath: serving[:model_path],
            modelVersion: 2,
            batchingEnabled: serving[:enable_batching],
            kafkaTopicDTO: {
                name: topic[:topic_name]
            },
            inferenceLogging: parse_inference_logging(serving[:inference_logging]),
            modelServer: parse_model_server(serving[:model_server]),
            servingTool: parse_serving_tool(serving[:serving_tool]),
            requestedInstances: serving[:instances]
            }
        expect_status_details(401, error_code: 200003)
      end
    end

    context 'with authentication', vm: true do
      before :all do
        with_valid_project
        with_tensorflow_serving(@project[:id], @project[:projectname], @user[:username])

        mkdir("/Projects/#{@project[:projectname]}/Models/newMnist/", @user[:username],
              "#{@project[:projectname]}__Models", 750)

        copy("/Projects/#{@project[:projectname]}/Models/mnist/*",
            "/Projects/#{@project[:projectname]}/Models/newMnist/",
            @user[:username], "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")

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

      # serving name

      it "should be able to update the name" do
        serving = Serving.find(@serving[:id])
        topic = ProjectTopics.find(@serving[:kafka_topic_id])
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: serving[:id],
            name: "testModelChanged",
            modelPath: serving[:model_path],
            modelVersion: serving[:model_version],
            artifactVersion: serving[:artifact_version],
            batchingEnabled: serving[:enable_batching],
            kafkaTopicDTO: {
                name: topic[:topic_name]
            },
            inferenceLogging: parse_inference_logging(serving[:inference_logging]),
            modelServer: parse_model_server(serving[:model_server]),
            servingTool: parse_serving_tool(serving[:serving_tool]),
            requestedInstances: serving[:instances]
            }
        expect_status_details(201)

        serving = Serving.find(@serving[:id])
        expect(serving[:name]).to eql "testModelChanged"
      end

      # model path

      it "should be able to update the model path" do
        serving = Serving.find(@serving[:id])
        topic = ProjectTopics.find(@serving[:kafka_topic_id])

        expect(serving[:model_name]).to eq "mnist"

        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: serving[:id],
            name: serving[:name],
            modelPath: "/Projects/#{@project[:projectname]}/Models/newMnist",
            modelVersion: serving[:model_version],
            batchingEnabled: serving[:enable_batching],
            kafkaTopicDTO: {
                name: topic[:topic_name]
            },
            inferenceLogging: parse_inference_logging(serving[:inference_logging]),
            modelServer: parse_model_server(serving[:model_server]),
            servingTool: parse_serving_tool(serving[:serving_tool]),
            requestedInstances: serving[:instances]
            }
        expect_status_details(201)

        serving = Serving.find(@serving[:id])
        expect(serving[:model_name]).to eq "newMnist"
        expect(serving[:model_path]).to eql "/Projects/#{@project[:projectname]}/Models/newMnist"
      end

      # model version

      it "should be able to update the model version" do
        serving = Serving.find(@serving[:id])
        topic = ProjectTopics.find(@serving[:kafka_topic_id])
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: serving[:id],
            name: serving[:name],
            modelPath: serving[:model_path],
            modelVersion: 2,
            batchingEnabled: serving[:enable_batching],
            kafkaTopicDTO: {
                name: topic[:topic_name]
            },
            inferenceLogging: parse_inference_logging(serving[:inference_logging]),
            modelServer: parse_model_server(serving[:model_server]),
            servingTool: parse_serving_tool(serving[:serving_tool]),
            requestedInstances: serving[:instances]
            }
        expect_status_details(201)

        serving = Serving.find(@serving[:id])
        expect(serving[:model_version]).to eql 2
      end

      # artifact version

      it "should create a new zipped model artifact when updating the model version" do
        if !kubernetes_installed
          skip "This test only runs with Kubernetes installed"
        end

        serving = Serving.find(@serving[:id])
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: serving[:id],
            name: serving[:name],
            modelPath: serving[:model_path],
            modelVersion: 2,
            batchingEnabled: serving[:enable_batching],
            modelServer: parse_model_server(serving[:model_server]),
            servingTool: parse_serving_tool(serving[:serving_tool]),
            requestedInstances: serving[:instances]
            }
        expect_status_details(201)

        serving = Serving.find(@serving[:id])
        expect(serving[:model_version]).to eql 2

        wait_result = wait_for_me_time(30, 3) do
          get_datasets_in_path(@project, "Models/#{serving[:model_name]}/#{serving[:model_version]}/Artifacts/#{serving[:artifact_version]}", query: "&type=DATASET")
          ds = json_body[:items].detect { |d| d[:attributes][:name] == "#{serving[:model_name]}_#{serving[:model_version]}_#{serving[:artifact_version]}.zip" }
          { "success" => ds.present?, "ds" => ds }
        end
        expect(wait_result['ds']).to be_present
      end

      it "should create a zipped model artifact when updating the model path" do
        if !kubernetes_installed
          skip "This test only runs with Kubernetes installed"
        end

        serving = Serving.find(@serving[:id])

        rmdir("/Projects/#{@project[:projectname]}/Models/newMnist/#{serving[:model_version]}/Artifacts")

        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: serving[:id],
            name: serving[:name],
            modelPath: "/Projects/#{@project[:projectname]}/Models/newMnist",
            modelVersion: serving[:model_version],
            batchingEnabled: serving[:enable_batching],
            modelServer: parse_model_server(serving[:model_server]),
            servingTool: parse_serving_tool(serving[:serving_tool]),
            requestedInstances: serving[:instances]
            }
        expect_status_details(201)

        serving = Serving.find(@serving[:id])
        expect(serving[:model_path]).to eql "/Projects/#{@project[:projectname]}/Models/newMnist"

        wait_result = wait_for_me_time(30, 3) do
          get_datasets_in_path(@project, "Models/newMnist/#{serving[:model_version]}/Artifacts/#{serving[:artifact_version]}", query: "&type=DATASET")
          ds = json_body[:items].detect { |d| d[:attributes][:name] == "newMnist_#{serving[:model_version]}_#{serving[:artifact_version]}.zip" }
          { "success" => ds.present?, "ds" => ds }
        end
        expect(wait_result['ds']).to be_present
      end

      # model server

      it "should not be able to update the model server" do
        serving = Serving.find(@serving[:id])
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: serving[:id],
            name: serving[:name],
            modelPath: serving[:model_path],
            modelVersion: serving[:model_version],
            artifactVersion: serving[:artifact_version],
            batchingEnabled: serving[:enable_batching],
            modelServer: "PYTHON",
            servingTool: parse_serving_tool(serving[:serving_tool]),
            requestedInstances: serving[:instances]
            }
        expect_status_details(400, error_code: 240013)
      end

      # kafka topic

      it "should be able to change the kafka topic it's writing to"  do
        json_result, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, INFERENCE_SCHEMA_VERSION)
        serving = Serving.find(@serving[:id])
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: serving[:id],
            name: serving[:name],
            modelPath: serving[:model_path],
            modelVersion: serving[:model_version],
            artifactVersion: serving[:artifact_version],
            batchingEnabled: serving[:enable_batching],
            kafkaTopicDTO: {
                name: topic_name
            },
            inferenceLogging: "ALL",
            modelServer: parse_model_server(serving[:model_server]),
            servingTool: parse_serving_tool(serving[:serving_tool]),
            requestedInstances: serving[:instances]
            }
        expect_status_details(201)

        serving = Serving.find(@serving[:id])
        new_topic = ProjectTopics.find_by(topic_name: topic_name, project_id: @project[:id])
        expect(serving[:kafka_topic_id]).to be new_topic[:id]
      end

      it "should be able to stop writing to a kafka topic" do
        serving = Serving.find(@serving[:id])
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: serving[:id],
            name: serving[:name],
            modelPath: serving[:model_path],
            modelVersion: serving[:model_version],
            artifactVersion: serving[:artifact_version],
            batchingEnabled: serving[:enable_batching],
            kafkaTopicDTO: {
                name: "NONE"
            },
            modelServer: parse_model_server(serving[:model_server]),
            servingTool: parse_serving_tool(serving[:serving_tool]),
            requestedInstances: serving[:instances]
            }
        expect_status_details(201)

        serving = Serving.find(@serving[:id])
        expect(serving[:kafka_topic_id]).to be nil
        expect(serving[:inference_logging]).to be nil
      end

      it "should be able to create a new kafka topic to write to" do
        serving = Serving.find(@serving[:id])
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
           {id: serving[:id],
            name: serving[:name],
            modelPath: serving[:model_path],
            modelVersion: serving[:model_version],
            artifactVersion: serving[:artifact_version],
            batchingEnabled: serving[:enable_batching],
            kafkaTopicDTO: {
                name: "CREATE"
            },
            inferenceLogging: "ALL",
            modelServer: parse_model_server(serving[:model_server]),
            servingTool: parse_serving_tool(serving[:serving_tool]),
            requestedInstances: serving[:instances]
            }
        expect_status_details(201)

        serving = Serving.find(@serving[:id])
        expect(serving[:kafka_topic_id]).not_to be_nil
        topic = ProjectTopics.find(@serving[:kafka_topic_id])
        expect(topic).not_to be_nil
      end
    end
  end

  describe "#start", vm: true do
    
    context 'without authentication' do
      before :all do
        with_valid_project
        with_tensorflow_serving(@project[:id], @project[:projectname], @user[:username])
        reset_session
      end

      it "should fail to start a serving instance" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
        expect_status_details(401, error_code: 200003)
      end
    end

    context 'with authentication', vm: true do
      before :all do
        with_valid_project
        with_tensorflow_serving(@project[:id], @project[:projectname], @user[:username])
      end

      after :all do
        purge_all_tf_serving_instances
        delete_all_servings(@project[:id])
      end

      it "should be able to start a serving instance" do
        start_serving(@project, @serving)
        wait_for_type(@serving[:name])

        # Check that the logs are written in the elastic index.
        wait_result = wait_for_me_time(30, 4) do
          result = elastic_rest do
            response = elastic_get "#{@project[:projectname].downcase}_serving*/_search?q=serving_name:#{@serving[:name]}"
            index = response.body
            parsed_index = JSON.parse(index)
            hits = parsed_index['hits']['total']['value']
            { "success": hits > 0, "hits": hits}
          end
          result
        end
        expect(wait_result[:hits]).to be > 0
      end

      it "should fail to start a running instance" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
        expect_status_details(400, error_code: 240003)
      end
    end
  end

  describe "#stop", vm: true do
        
    context 'without authentication' do
      before :all do
        with_valid_project
        @backup_user = @user
        with_tensorflow_serving(@project[:id], @project[:projectname], @user[:username])
        start_serving(@project, @serving)
        sleep(5) # Wait a bit for tfserving server to be in a running state
        reset_session
      end

      after :all do
        create_session(@backup_user[:email], "Pass123")
        purge_all_tf_serving_instances
        delete_all_servings(@project[:id])
      end

      it "should fail to stop a serving instance" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
        expect_status_details(401, error_code: 200003)
      end
    end

    context 'with authentication', vm: true do
      before :all do
        with_valid_project
        with_tensorflow_serving(@project[:id], @project[:projectname], @user[:username])
      end

      before :each do
        start_serving(@project, @serving)
        sleep(5) # Wait a bit for tfserving server to be in a running state
      end

      after :all do
        purge_all_tf_serving_instances
        delete_all_servings(@project[:id])
      end 

      it "should be able to kill a running serving instance" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
        expect_status_details(200)

        sleep(5)

        check_process_running("tensorflow_model_server")
      end

      it "should fail to kill a non running instance" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
        expect_status_details(200)

        sleep(5)

        check_process_running("tensorflow_model_server")

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
        expect_status_details(400, error_code: 240003)
      end

      it "should mark the serving as not running if the process dies" do
        if kubernetes_installed
          skip "This test does not run on Kubernetes"
        end
        # Simulate the process dying by its own
        system "pgrep -f tensorflow_model_server | xargs kill -9"

        # Check that the serving is reported as dead
        wait_result = wait_for_me_time(30, 4) do
          serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
          serving = JSON.parse(serving_list).select { |serving| serving["name"] == @serving[:name]}[0]
          { 'success': serving['status'].eql?("Stopped"), 'status': serving['status'] }
        end
        expect(wait_result[:status]).to eql "Stopped"
      end
    end
  end

  describe "#delete", vm: true do

    context 'without authentication' do
      before :all do
        # Make sure no tensorflow serving instance is running"
        system "pgrep -f tensorflow_model_server | xargs kill -9"
        with_valid_project
        @backup_user = @user
        with_tensorflow_serving(@project[:id], @project[:projectname], @user[:username])
        start_serving(@project, @serving)
        sleep(5) # Wait a bit for tfserving server to be in a running state
        reset_session
      end

      after :all do
        create_session(@backup_user[:email], "Pass123")
        purge_all_tf_serving_instances
        delete_all_servings(@project[:id])
      end
  
      it "should fail to delete a serving instance" do
        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}"
        expect_status_details(401, error_code: 200003)
      end
    end

    context 'with authentication', vm: true do
      before :all do
        # Make sure no tensorflow serving instance is running"
        system "pgrep -f tensorflow_model_server | xargs kill -9"
        with_valid_project
        copy_mnist_files(@project[:projectname], @user[:username])
      end

      before :each do
        @serving = create_tensorflow_serving(@project[:id], @project[:projectname])
      end

      it "should delete a serving instance" do
        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}"
        expect_status_details(200)
      end

      it "should delete a running serving instance" do
        start_serving(@project, @serving)
        wait_for_type(@serving[:name])

        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}"
        expect_status(200)

        sleep(5)

        check_process_running("tensorflow_model_server")
      end
    end
  end

  describe "#filter" do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end

      it "should fail to get servings" do
        get_servings(@project, nil)
        expect_status(401)
      end
    end

    context 'with authentication', vm: true do
      before :all do
        with_valid_project
        copy_mnist_files(@project[:projectname], @user[:username])
        create_tensorflow_serving(@project[:id], @project[:projectname])
        create_tensorflow_serving(@project[:id], @project[:projectname])
        @tf_serving = create_tensorflow_serving(@project[:id], @project[:projectname])
      end

      it "should return all servings" do
        get_servings(@project, nil)
        expect_status(200)
        json_body.each {|model| expect(model[:status]).to eq "Stopped"}
        expect(json_body.length).to eq 3
      end

      describe "#status" do
        it "should return all stopped servings" do
          get_servings(@project, "?status=Stopped")
          expect_status(200)
          json_body.each {|model| expect(model[:status]).to eq "Stopped"}
          expect(json_body.length).to eq 3
        end

        it "should return no running servings" do
          get_servings(@project, "?status=Running")
          expect_status(200)
          expect(json_body.length).to eq 0
        end

        it "should return single running serving" do
          start_serving(@project, @tf_serving)
          wait_for_serving_status(@tf_serving[:name], "Running", timeout: 90, delay: 10)

          get_servings(@project, "?status=Running")
          expect_status(200)
          json_body.each {|model| expect(model[:status]).to eq "Running"}
          expect(json_body.length).to eq 1
        end
      end

      describe "#model" do
        it "should return no servings for non-existent model" do
          get_servings(@project, "?model=cifar")
          expect_status(200)
          expect(json_body.length).to eq 0
        end

        it "should return all servings for mnist model" do
          get_servings(@project, "?model=mnist")
          expect_status(200)
          json_body.each {|model| expect(model[:modelName]).to eq "mnist"}
          expect(json_body.length).to eq 3
        end
      end
    end
  end
end
