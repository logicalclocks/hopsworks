=begin
 This file is part of Hopsworks
 Copyright (C) 2020, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end

require 'json'

describe "On #{ENV['OS']}" do
  before :all do
    if !kfserving_installed
      skip "This test only run with KFServing installed"
    end
  end

  after (:all) do
    clean_all_test_projects(spec: "kfserving")
  end

  describe 'kfserving' do

    describe 'tensorflow' do

      describe "#create" do
        context 'without authentication' do
          before :all do
            with_valid_project
            copy_mnist_files(@project[:id], @project[:projectname], @user[:username])
            reset_session
          end

          it "should fail to create the serving" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingEnabled: false,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(401, error_code: 200003)
          end
        end

        context 'with authentication', vm: true do
          before :all do
            with_valid_project
            copy_mnist_files(@project[:id], @project[:projectname], @user[:username])
          end

          it "should create the serving and zipped model artifact" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodelzipped",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingEnabled: false,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(201)
          end

          it "should create the serving without Kafka topic" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel2",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingEnabled: false,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(201)

            serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
            kafka_topic = JSON.parse(serving_list).select { |serving| serving['name'] ==
            "testmodel2"}[0]['kafkaTopicDTO']
            expect(kafka_topic).to be nil
          end

          # TODO: Batching not supported yet
          #it "should create the serving with batching" do
          #  put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
          #      {name: "testmodelbatching",
          #       modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
          #       modelVersion: 1,
          #       batchingEnabled: true,
          #       modelServer: "TENSORFLOW_SERVING",
          #       servingTool: "KFSERVING",
          #       requestedInstances: 1
          #      }
          #  expect_status_details(201)
          #end

          it "should create the serving with a new Kafka topic" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel3",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingEnabled: false,
                 kafkaTopicDTO: {
                     name: "CREATE",
                     numOfPartitions: 1,
                     numOfReplicas: 1
                 },
                 inferenceLogging: "ALL",
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(201)

            # Kafka authorizer needs some time to take up the new permissions.
            sleep(5)

            # Check that the topic has been created correctly
            serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
            kafka_topic_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/kafka/topics"

            kafka_topic_name = JSON.parse(serving_list).select { |serving| serving['name'] ==
            "testmodel3"}[0]['kafkaTopicDTO']['name']
            kafka_topic = JSON.parse(kafka_topic_list)['items'].select { |topic| topic['name'] == kafka_topic_name}
            expect(kafka_topic.size).to eq 1
            expect(kafka_topic[0]['schemaName']).to eql INFERENCE_SCHEMA_NAME
          end

          it "should create the serving with an existing Kafka topic with Inference Schema version 1" do
            # Create kafka topic
            json, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, 1)

            # Create serving
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel4",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingEnabled: false,
                 kafkaTopicDTO: {
                     name: topic_name
                 },
                 inferenceLogging: "ALL",
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(201)

            # Kafka authorizer needs some time to take up the new permissions.
            sleep(5)

            # Check that the serving is actually using that topic
            serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
            kafka_topic_name = JSON.parse(serving_list).select { |serving| serving['name'] ==
            "testmodel4"}[0]['kafkaTopicDTO']['name']
            expect(kafka_topic_name).to eql topic_name
          end

          it "should create the serving with an existing Kafka topic with Inference Schema version 2" do
            # Create kafka topic
            json, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, 2)

            # Create serving
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel5",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingEnabled: false,
                 kafkaTopicDTO: {
                     name: topic_name
                 },
                 inferenceLogging: "ALL",
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(201)

            # Kafka authorizer needs some time to take up the new permissions.
            sleep(5)

            # Check that the serving is actually using that topic
            serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
            kafka_topic_name = JSON.parse(serving_list).select { |serving| serving['name'] ==
                "testmodel5"}[0]['kafkaTopicDTO']['name']
            expect(kafka_topic_name).to eql topic_name
          end

          it "should fail to create the serving with an existing Kafka topic without the Inference Schema" do
            # Create Kafka Schema
            json_result, schema_name = add_schema(@project[:id])

            # Create kafka topic
            json, topic_name = add_topic(@project[:id], schema_name, 1)

            # Create serving
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel6",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingEnabled: false,
                 kafkaTopicDTO: {
                     name: topic_name
                 },
                 inferenceLogging: "ALL",
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(400)
          end

          it "should create the serving with default predictorResourceConfig if not set" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "res1model",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingEnabled: false,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(201)

            serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
            resource_config = JSON.parse(serving_list).select { |serving| serving['name'] ==
            "res1model"}[0]['predictorResourceConfig']
            expect(resource_config['memory']).to be 1024
            expect(resource_config['cores']).to be 1
            expect(resource_config['gpus']).to be 0
          end

          it "should create the serving with overridden predictorResourceConfig" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "res2model",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingEnabled: false,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1,
                 predictorResourceConfig: {
                   memory: 3000,
                   cores: 2,
                   gpus: 1
                }
                }
            expect_status_details(201)

            serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
            resource_config = JSON.parse(serving_list).select { |serving| serving['name'] ==
            "res2model"}[0]['predictorResourceConfig']
            expect(resource_config['memory']).to be 3000
            expect(resource_config['cores']).to be 2
            expect(resource_config['gpus']).to be 1
          end

          it "should fail to create a serving with the same name" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel7",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingEnabled: false,
                 kafkaTopicDTO: {
                     name: "NONE"
                 },
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(201)

            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel7",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingEnabled: false,
                 kafkaTopicDTO: {
                     name: "NONE"
                 },
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(400, error_code: 240011)
            expect_json(errorMsg: "An entry with the same name already exists in this project")
          end

          it "should fail to create a serving with a non-existent path" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel8",
                 modelPath: "/Projects/#{@project[:projectname]}/DOESNTEXISTS",
                 batchingEnabled: false,
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(422)
            expect_json(usrMsg: "The model path provided does not exists")
          end

          it "should fail to create a serving with an invalid name" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "invalidName",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingEnabled: false,
                 kafkaTopicDTO: {
                     name: "NONE"
                 },
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(422)
            expect_json(usrMsg: "Serving name must consist of lower case alphanumeric characters, '-' or '.', and start and end with an alphanumeric character")
          end

          it "should fail to create a serving with request batching" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel9",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingEnabled: true,
                 kafkaTopicDTO: {
                     name: "NONE"
                 },
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(422)
            expect_json(usrMsg: "Request batching is not supported in KFServing deployments")
          end

          # Inference logging

          it "should fail to create a serving with kafka topic but without inference logging defined" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel11",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 batchingEnabled: false,
                 kafkaTopicDTO: {
                     name: "CREATE"
                 },
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(422)
            expect_json(usrMsg: "Inference logger mode not provided or invalid")
          end

          it "should fail to create a serving with inference logging mode but without kafka topic" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel12",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 batchingEnabled: false,
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 inferenceLogging: "ALL",
                 requestedInstances: 1
                }
            expect_status_details(422)
            expect_json(usrMsg: "Inference logger mode provided but no kafka topic specified")
          end

          it "should fail to create a serving with invalid inference logging mode" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel13",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 batchingEnabled: false,
                 kafkaTopicDTO: {
                  name: "CREATE"
                 },
                 inferenceLogging: "INVALID",
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(422)
            expect_json(usrMsg: "Inference logger mode not provided or invalid")
          end

          it "should fail to create a serving with fine-grained inference logging without KFServing as serving tool" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel13",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 batchingEnabled: false,
                 kafkaTopicDTO: {
                  name: "CREATE"
                 },
                 inferenceLogging: "PREDICTIONS",
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "DEFAULT",
                 requestedInstances: 1
                }
            expect_status_details(422)
            expect_json(usrMsg: "Fine-grained inference logger is only supported in KFServing deployments")
          end

          # Transformers

          it "should fail to create a serving with invalid transformer scrip path" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel14",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 batchingEnabled: false,
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 transformer: "/Projects/#{@project[:projectname]}/Models/mnist/invalid.ext",
                 requestedInstances: 1,
                 requestedTransformerInstances: 1
                }
            expect_status_details(422)
            expect_json(usrMsg: "Script name should be a valid python script name: .py, .ipynb")
          end

          it "should fail to create a serving with non-existent transformer script" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel15",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 batchingEnabled: false,
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 transformer: "/Projects/#{@project[:projectname]}/Models/mnist/non-existent.py",
                 requestedInstances: 1,
                 requestedTransformerInstances: 1
                }
            expect_status_details(422)
            expect_json(usrMsg: "Python script path does not exist in HDFS")
          end

          it "should create a serving with a python script as transformer" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel16",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 batchingEnabled: false,
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
                 requestedInstances: 1,
                 requestedTransformerInstances: 1
                }
            expect_status_details(201)

            serving = Serving.find_by(project_id: @project[:id], name: "testmodel16")
            expect(serving[:artifact_version]).to eql 1 # New version
          end

          it "should create a serving with a jupyter notebook as transformer" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel17",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 batchingEnabled: false,
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.ipynb",
                 requestedInstances: 1,
                 requestedTransformerInstances: 1
                }
            expect_status_details(201)

            serving = Serving.find_by(project_id: @project[:id], name: "testmodel17")
            expect(serving[:artifact_version]).to eql 2 # New version
          end

          it "should fail to create a serving with an existent artifact but different transformer script" do
            copy("/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
                 "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer-copy.py",
                 @user[:username], "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")

            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel18",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 batchingEnabled: false,
                 modelVersion: 1,
                 artifactVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer-copy.py",
                 requestedInstances: 1,
                 requestedTransformerInstances: 1
                }
            expect_status_details(422)
            expect_json(usrMsg: "Transformer script cannot change in an existent artifact")
          end

          # Artifact version

          it "should create a serving without artifact version when no transformer is specified" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel19",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 batchingEnabled: false,
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(201)

            serving = Serving.find_by(project_id: @project[:id], name: "testmodel19")
            expect(serving[:artifact_version]).to eql 0 # MODEL-ONLY
          end

          it "should create a serving with a MODEL-ONLY artifact without transformer" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel20",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 batchingEnabled: false,
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(201)

            serving = Serving.find_by(project_id: @project[:id], name: "testmodel20")
            expect(serving[:artifact_version]).to eql 0 # MODEL-ONLY
          end

          it "should fail to create a serving for a non-existent artifact version" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel21",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 batchingEnabled: false,
                 modelVersion: 1,
                 artifactVersion: "99",
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
                 requestedInstances: 1,
                 requestedTransformerInstances: 1
                }
            expect_status_details(422)
            expect_json(usrMsg: "Artifact with version 99 does not exist")
          end

          it "should create a serving with a new artifact version when artifact version is CREATE and a transformer
          is specified" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel22",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 batchingEnabled: false,
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
                 requestedInstances: 1,
                 requestedTransformerInstances: 1
                }
            expect_status_details(201)

            serving = Serving.find_by(project_id: @project[:id], name: "testmodel22")
            expect(serving[:artifact_version]).to eql 3 # New version
          end

          it "should fail to create a serving with an artifact version and without transformer" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel23",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 batchingEnabled: false,
                 modelVersion: 1,
                 artifactVersion: "1",
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(422)
            expect_json(usrMsg: "Transformer cannot be null for a non-MODEL-ONLY artifact")
          end

          it "should fail to create a serving with MODEL-ONLY artifact and transformer" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel24",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 batchingEnabled: false,
                 modelVersion: 1,
                 artifactVersion: "0", # MODEL-ONLY
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
                 requestedInstances: 1,
                 requestedTransformerInstances: 1
                }
            expect_status_details(422)
            expect_json(usrMsg: "Transformers are not supported with MODEL-ONLY artifacts (i.e version 0)")
          end

          # Transformer Instances

          it "should fail to create a serving with transformer and without requested instances" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                 {name: "testmodel25",
                  modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                  batchingEnabled: false,
                  modelVersion: 1,
                  modelServer: "TENSORFLOW_SERVING",
                  servingTool: "KFSERVING",
                  transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.ipynb",
                  requestedInstances: 1
                 }
            expect_status_details(422)
            expect_json(usrMsg: "Number of transformer instances must be provided when using a transformer")
          end

          it "should fail to create a serving with transformer instances but without transformer" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                 {name: "testmodel26",
                  modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                  batchingEnabled: false,
                  modelVersion: 1,
                  modelServer: "TENSORFLOW_SERVING",
                  servingTool: "KFSERVING",
                  requestedInstances: 1,
                  requestedTransformerInstances: 1
                 }
            expect_status_details(422)
            expect_json(usrMsg: "Number of transformer instances cannot be provided without a transformer")
          end

          it "should create a serving with transformer and requested transformer instances" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                 {name: "testmodel27",
                  modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                  batchingEnabled: false,
                  modelVersion: 1,
                  modelServer: "TENSORFLOW_SERVING",
                  servingTool: "KFSERVING",
                  transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.ipynb",
                  requestedInstances: 1,
                  requestedTransformerInstances: 1
                 }
            expect_status_details(201)
          end

          it "should fail to create a serving with a non-standard path" do
            rm("/Projects/#{@project[:projectname]}/Models/mnist/1/saved_model.pb")

            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel28",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 batchingEnabled: false,
                 modelVersion: 1,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(422)
            expect_json(usrMsg: "The model path does not respect the TensorFlow standard")
          end
        end
      end

      describe "#update", vm: true do
        before :all do
          with_valid_project
          with_kfserving_tensorflow(@project[:id], @project[:projectname], @user[:username])
        end

        after :all do
          purge_all_kfserving_instances(@project[:projectname])
        end

        after :each do
          sleep(10)
        end

        # Model (name, version)

        it "should be able to update the name" do
          serving = Serving.find(@serving[:id])
          topic = ProjectTopics.find(@serving[:kafka_topic_id])
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {id: serving[:id],
               name: "testmodelchanged",
               modelPath: serving[:model_path],
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
          expect(serving[:name]).to eql "testmodelchanged"
        end

        it "should be able to update the version" do
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

        # Kafka

        it "should be able to change the kafka topic it's writing to"  do
          json_result, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, INFERENCE_SCHEMA_VERSION)
          serving = Serving.find(@serving[:id])
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {id: serving[:id],
               name: serving[:name],
               modelPath: serving[:model_path],
               modelVersion: serving[:model_version],
               batchingEnabled: serving[:enable_batching],
               kafkaTopicDTO: {
                   name: topic_name
               },
               inferenceLogging: parse_inference_logging(serving[:inference_logging]),
               modelServer: parse_model_server(serving[:model_server]),
               servingTool: parse_serving_tool(serving[:serving_tool]),
               requestedInstances: serving[:instances]
              }
          expect_status_details(201)

          serving = Serving.find(@serving[:id])
          new_topic = ProjectTopics.find_by(topic_name: topic_name, project_id: @project[:id])
          expect(serving[:kafka_topic_id]).to be new_topic[:id]
        end

        it "should be able to update the inference logging mode" do
          serving = Serving.find(@serving[:id])
          topic = ProjectTopics.find(@serving[:kafka_topic_id])
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {id: serving[:id],
               name: serving[:name],
               modelPath: serving[:model_path],
               modelVersion: serving[:model_version],
               batchingEnabled: serving[:enable_batching],
               kafkaTopicDTO: {
                   name: topic[:topic_name]
               },
               inferenceLogging: "PREDICTIONS",
               modelServer: parse_model_server(serving[:model_server]),
               servingTool: parse_serving_tool(serving[:serving_tool]),
               requestedInstances: serving[:instances]
              }
          expect_status_details(201)

          serving = Serving.find(@serving[:id])
          expect(parse_inference_logging(serving[:inference_logging])).to eql "PREDICTIONS"
        end

        it "should be able to stop writing to a kafka topic" do
          serving = Serving.find(@serving[:id])
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {id: serving[:id],
               name: serving[:name],
               modelPath: serving[:model_path],
               modelVersion: serving[:model_version],
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

        # Model server

        it "should not be able to update the model server" do
          serving = Serving.find(@serving[:id])
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {id: serving[:id],
               name: serving[:name],
               modelPath: serving[:model_path],
               modelVersion: serving[:model_version],
               batchingEnabled: serving[:enable_batching],
               modelServer: "FLASK",
               servingTool: parse_serving_tool(serving[:serving_tool]),
               requestedInstances: serving[:instances]
              }
          expect_status_details(422)
        end

        # Artifact

        it "should create a zipped model artifact when updating the model version" do
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
            get_datasets_in_path(@project, "Models/mnist/#{serving[:model_version]}/Artifacts/#{serving[:artifact_version]}", query: "&type=DATASET")
            ds = json_body[:items].detect { |d| d[:attributes][:name] == "mnist_#{serving[:model_version]}_#{serving[:artifact_version]}.zip" }
            { "success" => ds.present?, "ds" => ds }
          end
          expect(wait_result['ds']).to be_present
        end

        # TODO: Baching not supported yet. https://logicalclocks.atlassian.net/jira/software/c/projects/HOPSWORKS/issues/HOPSWORKS-2501
        #it "should be able to update the batching" do
        #  put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
        #      {id: @serving[:id],
        #       name: "testmodelchanged",
        #       modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
        #       modelVersion: 2,
        #       batchingEnabled: true,
        #       kafkaTopicDTO: {
        #           name: @topic[:topic_name]
        #       },
        #       modelServer: "TENSORFLOW_SERVING",
        #       servingTool: "KFSERVING"
        #      }
        #  expect_status_details(201)
        #end

        it "should be able to update the model path and create a zipped model artifact" do
          mkdir("/Projects/#{@project[:projectname]}/Models/newMnist/", @user[:username],
                "#{@project[:projectname]}__Models", 750)

          copy("/Projects/#{@project[:projectname]}/Models/mnist/*",
               "/Projects/#{@project[:projectname]}/Models/newMnist/",
               @user[:username], "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")

          serving = Serving.find(@serving[:id])

          rmdir("/Projects/#{@project[:projectname]}/Models/newMnist/#{serving[:model_version]}/Artifacts")

          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {id: serving[:id],
               name: serving[:name],
               modelPath: "/Projects/#{@project[:projectname]}/Models/newMnist/",
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

        it "should be able to update a serving with a transformer script and new artifact version" do
          copy("/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
               "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer-copy-2.py",
               @user[:username], "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")

          serving = Serving.find(@serving[:id])

          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {id: serving[:id],
               name: serving[:name],
               modelPath: serving[:model_path],
               modelVersion: serving[:model_version],
               artifactVersion: -1, # New version
               batchingEnabled: serving[:enable_batching],
               modelServer: parse_model_server(serving[:model_server]),
               servingTool: parse_serving_tool(serving[:serving_tool]),
               transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer-copy-2.py",
               requestedInstances: serving[:instances],
               requestedTransformerInstances: 1,
              }
          expect_status_details(201)

          serving = Serving.find(@serving[:id])
          expect(serving[:artifact_version]).to be > 0

          wait_result = wait_for_me_time(30) do
            get_datasets_in_path(@project, "Models/newMnist/#{serving[:model_version]}/Artifacts/#{serving[:artifact_version]}", query: "&type=DATASET")
            ds = json_body[:items].detect { |d| d[:attributes][:name] == "newMnist_#{serving[:model_version]}_#{serving[:artifact_version]}.zip" }
            { "success" => ds.present?, "ds" => ds }
          end
          expect(wait_result['ds']).to be_present
        end

        it "should not be able to update a serving with different transformer script" do
          copy("/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
               "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer-copy-3.py",
               @user[:username], "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")

          serving = Serving.find(@serving[:id])

          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {id: serving[:id],
               name: serving[:name],
               modelPath: serving[:model_path],
               modelVersion: serving[:model_version],
               artifactVersion: serving[:artifact_version],
               batchingEnabled: serving[:enable_batching],
               modelServer: parse_model_server(serving[:model_server]),
               servingTool: parse_serving_tool(serving[:serving_tool]),
               transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer-copy-3.py",
               requestedInstances: serving[:instances],
               requestedTransformerInstances: 1,
              }
          expect_status_details(422)
          expect_json(usrMsg: "Transformer script cannot change in an existent artifact")
        end

        # Nº of instances

        it "should be able to update the number of instances of the predictor and transformer" do
          serving = Serving.find(@serving[:id])
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {id: serving[:id],
               name: serving[:name],
               modelPath: serving[:model_path],
               modelVersion: serving[:model_version],
               artifactVersion: serving[:artifact_version],
               batchingEnabled: serving[:enable_batching],
               modelServer: parse_model_server(serving[:model_server]),
               servingTool: parse_serving_tool(serving[:serving_tool]),
               transformer: serving[:transformer],
               requestedInstances: 2,
               requestedTransformerInstances: 2,
              }
          expect_status_details(201)

          serving = Serving.find(@serving[:id])
          expect(serving[:instances]).to eql 2
          expect(serving[:transformer_instances]).to eql 2
        end

      end

      describe "#start", vm: true do
        before :all do
          with_valid_project
          with_kfserving_tensorflow(@project[:id], @project[:projectname], @user[:username])
        end

        after :all do
          purge_all_kfserving_instances(@project[:projectname])
        end

        it "should be able to start a serving instance" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
          expect_status_details(200)

          # Check if the process is running on the host
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
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
        end

        it "should be able to start a serving instance with transformer" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
               {id: @serving[:id],
                name: @serving[:name],
                modelPath: @serving[:model_path],
                batchingEnabled: @serving[:enable_batching],
                modelVersion: @serving[:model_version],
                modelServer: parse_model_server(@serving[:model_server]),
                servingTool: parse_serving_tool(@serving[:serving_tool]),
                transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.ipynb",
                requestedInstances: @serving[:instances],
                requestedTransformerInstances: 1
               }
          expect_status_details(201)

          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
          expect_status_details(200)

          # Check if the process is running on the host
          wait_for_type(@serving[:name])

          # Check that the logs are written in the elastic index.
          wait_result = wait_for_me_time(30, 4) do
            elastic_rest do
              response = elastic_get "#{@project[:projectname].downcase}_serving*/_search?q=serving_name:#{@serving[:name]}"
              index = response.body
              parsed_index = JSON.parse(index)
              hits = parsed_index['hits']['total']['value']
              { "success": hits > 0, "hits": hits}
            end
          end
          expect(wait_result[:hits]).to be > 0

          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
        end
      end

      describe "#kill", vm: true do
        before :all do
          with_valid_project
          with_kfserving_tensorflow(@project[:id], @project[:projectname], @user[:username])
        end

        it "should fail to kill a non running instance" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
          expect_status_details(400, error_code: 240003)
        end

        it "should be able to kill a running serving instance" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
          expect_status_details(200)

          wait_for_type(@serving[:name])

          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
          expect_status_details(200)

          check_process_running("tensorflow_model_server")
        end
      end

      describe "#delete", vm: true do
        before :all do
          with_valid_project
          copy_mnist_files(@project[:id], @project[:projectname], @user[:username])
        end

        before :each do
          @serving = create_kfserving_tensorflow(@project[:id], @project[:projectname])
        end

        it "should be able to delete a serving instance" do
          delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}"
          expect_status_details(200)
        end

        it "should be able to delete a running instance" do
          # Start the serving instance
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
          expect_status_details(200)

          # Wait until the service instance is running
          wait_for_type(@serving[:name])

          delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}"
          expect_status_details(200)

          sleep(5)

          check_process_running("tensorflow_model_server")
        end
      end
    end

    describe 'sklearn' do

      describe "#create" do
        context 'without authentication' do
          before :all do
            with_valid_project
            reset_session
          end

          it "should fail to create the serving" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodelsklearn",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
                 modelVersion: 1,
                 modelServer: "FLASK",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(401, error_code: 200003)
          end
        end

        context 'with authentication', vm: true do
          before :all do
            with_valid_project
            with_python_enabled(@project[:id], ENV['PYTHON_VERSION'])

            # Make Serving Dir
            mkdir("/Projects/#{@project[:projectname]}/Models/irisflowerclassifier/1", "#{@user[:username]}",
                  "#{@project[:projectname]}__Models", 750)

            # Copy model to the servingversion dir
            copy(SKLEARN_MODEL_TOUR_FILE_LOCATION, "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier/1/",
                 "#{@user[:username]}",
                 "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")

            # Copy script to the servingversion dir
            copy(SKLEARN_SCRIPT_TOUR_FILE_LOCATION, "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier/1/",
                 "#{@user[:username]}",
                 "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")
          end

          after :all do
            purge_all_sklearn_serving_instances()
            delete_all_sklearn_serving_instances(@project)
          end

          it "should fail to create the serving with KFSERVING" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodelsklearn",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
                 modelVersion: 1,
                 modelServer: "FLASK",
                 servingTool: "KFSERVING",
                 requestedInstances: 1
                }
            expect_status_details(422)
            expect_json(usrMsg: "KFServing not supported for SKLearn models")
          end
        end
      end
    end
  end
end
