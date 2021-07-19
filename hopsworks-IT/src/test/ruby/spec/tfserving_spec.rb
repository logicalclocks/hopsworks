=begin
 Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 are released under the following license:

 This file is part of Hopsworks
 Copyright (C) 2018, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.

 Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 are released under the following license:

 Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved

 Permission is hereby granted, free of charge, to any person obtaining a copy of this
 software and associated documentation files (the "Software"), to deal in the Software
 without restriction, including without limitation the rights to use, copy, modify, merge,
 publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or
 substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
=end

require 'json'

describe "On #{ENV['OS']}" do
  after (:all) do
    clean_all_test_projects(spec: "tfserving")
    purge_all_tf_serving_instances
  end
  describe 'tfserving' do

    describe "#create" do

      context 'without authentication' do
        before :all do
          with_valid_project
          copy_mnist_files(@project[:id], @project[:projectname], @user[:username])
          reset_session
        end

        it "should fail to create the serving" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
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
          copy_mnist_files(@project[:id], @project[:projectname], @user[:username])
        end

        it "should create the serving without Kafka topic" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false,
               modelServer: "TENSORFLOW_SERVING",
               servingTool: "DEFAULT",
               requestedInstances: 1
              }
          expect_status_details(201)

          serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
          kafka_topic = JSON.parse(serving_list).select { |serving| serving['name'] == "testModel"}[0]['kafkaTopicDTO']
          expect(kafka_topic).to be nil
        end

        it "should create the serving with batching" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModelBatching",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: true,
               modelServer: "TENSORFLOW_SERVING",
               servingTool: "DEFAULT",
               requestedInstances: 1
              }
          expect_status_details(201)
        end

        it "should create the serving with a new Kafka topic" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel1",
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
               servingTool: "DEFAULT",
               requestedInstances: 1
              }
          expect_status_details(201)

          # Kafka authorizer needs some time to take up the new permissions.
          sleep(5)

          # Check that the topic has been created correctly
          serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
          kafka_topic_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/kafka/topics"

          kafka_topic_name = JSON.parse(serving_list).select { |serving| serving['name'] == "testModel1"}[0]['kafkaTopicDTO']['name']
          kafka_topic = JSON.parse(kafka_topic_list)['items'].select { |topic| topic['name'] == kafka_topic_name}
          expect(kafka_topic.size).to eq 1
          expect(kafka_topic[0]['schemaName']).to eql INFERENCE_SCHEMA_NAME
        end

        it "should create the serving with an existing Kafka topic with Inference Schema version 1" do
          # Create kafka topic
          json, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, 1)

          # Create serving
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel2",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
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
          expect_status_details(201)

          # Kafka authorizer needs some time to take up the new permissions.
          sleep(5)

          # Check that the serving is actually using that topic
          serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
          kafka_topic_name = JSON.parse(serving_list).select { |serving| serving['name'] == "testModel2"}[0]['kafkaTopicDTO']['name']
          expect(kafka_topic_name).to eql topic_name
        end

        it "should create the serving with an existing Kafka topic with Inference Schema version 2" do
          # Create kafka topic
          json, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, 2)

          # Create serving
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel3",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
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
          expect_status_details(201)

          # Kafka authorizer needs some time to take up the new permissions.
          sleep(5)

          # Check that the serving is actually using that topic
          serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
          kafka_topic_name = JSON.parse(serving_list).select { |serving| serving['name'] ==
              "testModel3"}[0]['kafkaTopicDTO']['name']
          expect(kafka_topic_name).to eql topic_name
        end

        it "should fail to create the serving with an existing Kafka topic without the Inference Schema" do
          # Create Kafka Schema
          json_result, schema_name = add_schema(@project[:id])

          # Create kafka topic
          json, topic_name = add_topic(@project[:id], schema_name, 1)

          # Create serving
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel5",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
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
          expect_status_details(400)
        end

        it "should fail to create a serving with the same name" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel1",
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
               servingTool: "DEFAULT",
               requestedInstances: 1
              }
          expect_status_details(400, error_code: 240011)
          expect_json(errorMsg: "An entry with the same name already exists in this project")
        end

        it "should fail to create a serving with a non-existent path" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel5",
               modelPath: "/Projects/#{@project[:projectname]}/DOESNTEXISTS",
               batchingEnabled: false,
               modelVersion: 1,
               modelServer: "TENSORFLOW_SERVING",
               servingTool: "DEFAULT",
               requestedInstances: 1
              }
          expect_status_details(422)
          expect_json(usrMsg: "The model path provided does not exists")
        end

        it "should fail to create a serving without serving tool" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "invalidName",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false,
               kafkaTopicDTO: {
                   name: "NONE"
               },
               modelServer: "TENSORFLOW_SERVING",
               requestedInstances: 1
              }
          expect_status_details(422)
          expect_json(usrMsg: "Serving tool not provided or invalid")
        end

        it "should fail to create a serving with KFSERVING tool when Kubernetes is not installed" do
          if kubernetes_installed
            skip "This test does not run on Kubernetes"
          end
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
          expect_status_details(400, error_code: 240014)
          expect_json(errorMsg: "Kubernetes is not installed", usrMsg: "Serving tool not supported. KFServing requires Kubernetes to be installed")
        end

        it "should fail to create a serving with KFSERVING tool when KFServing is not installed" do
          if !kubernetes_installed
            skip "This test only run on Kubernetes"
          end
          if kfserving_installed
            skip "This test does not run with KFServing installed"
          end
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
          expect_status_details(400, error_code: 240015)
          expect_json(errorMsg: "KFServing is not installed or disabled", usrMsg: "Serving tool not supported")
        end

        it "should fail to create a serving with artifact when Kubernetes is not installed" do
          if kubernetes_installed
            skip "This test only run without Kubernetes"
          end
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "invalidName",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
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
          expect_json(errorMsg: "Kubernetes is not installed", usrMsg: "Artifacts only supported in Kubernetes or KFServing deployments")
        end

        it "should fail to create a serving with transformer but without KFSERVING" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModelwithTransformer",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false,
               kafkaTopicDTO: {
                   name: "NONE"
               },
               modelServer: "TENSORFLOW_SERVING",
               servingTool: "DEFAULT",
               transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
               requestedInstances: 1,
               requestedTransformerInstances: 1
              }
          expect_status_details(422)
          expect_json(usrMsg: "KFServing is required for using transformers")
        end

        it "should fail to create a serving with a non-standard path" do
          rm("/Projects/#{@project[:projectname]}/Models/mnist/1/saved_model.pb")

          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel6",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               batchingEnabled: false,
               modelVersion: 1,
               modelServer: "TENSORFLOW_SERVING",
               servingTool: "DEFAULT",
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
        with_tf_serving(@project[:id], @project[:projectname], @user[:username])

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
        expect_status_details(200)

        wait_for_type(@serving[:name])
      end

      after :all do
        purge_all_tf_serving_instances
        delete_all_sklearn_serving_instances(@project)
      end

      after :each do
        serving = Serving.find(@serving[:id])
        wait_for_type(serving[:name])
      end

      it "should be able to update the name" do
        serving = Serving.find(@serving[:id])
        topic = ProjectTopics.find(@serving[:kafka_topic_id])
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: serving[:id],
             name: "testModelChanged",
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
        expect(serving[:name]).to eql "testModelChanged"
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

      it "should be able to update the batching" do
        serving = Serving.find(@serving[:id])
        topic = ProjectTopics.find(@serving[:kafka_topic_id])
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: serving[:id],
             name: serving[:name],
             modelPath: serving[:model_path],
             modelVersion: serving[:model_version],
             batchingEnabled: true,
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
        expect(serving[:enable_batching]).to eql true
      end

      it "should be able to update the path" do
        mkdir("/Projects/#{@project[:projectname]}/Models/newMnist/", @user[:username],
              "#{@project[:projectname]}__Models", 750)

        copy("/Projects/#{@project[:projectname]}/Models/mnist/*",
             "/Projects/#{@project[:projectname]}/Models/newMnist/",
             @user[:username], "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")

        serving = Serving.find(@serving[:id])
        topic = ProjectTopics.find(@serving[:kafka_topic_id])
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: serving[:id],
             name: serving[:name],
             modelPath: "/Projects/#{@project[:projectname]}/Models/newMnist",
             modelVersion: serving[:model_version],
             batchingEnabled: true,
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
        expect(serving[:model_path]).to eql "/Projects/#{@project[:projectname]}/Models/newMnist"
      end

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

      it "should fail to update the inference logging mode" do
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
        expect_status_details(422, error_code: 120001)
        expect_json(usrMsg: "Fine-grained inference logger is only supported in KFServing deployments")
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
    end

    describe "#start", vm: true do
      before :all do
        with_valid_project
        with_tf_serving(@project[:id], @project[:projectname], @user[:username])
      end

      after :all do
        purge_all_tf_serving_instances
        delete_all_sklearn_serving_instances(@project)
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
      end
    end

    describe "#kill", vm: true do
      before :all do
        with_valid_project
        with_tf_serving(@project[:id], @project[:projectname], @user[:username])
      end

      before :each do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
        expect_status_details(200)
        # Wait a bit for tfserving server to be in a running state
        sleep(5)
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

    describe "#delete", vm: true do
      before :all do
        # Make sure no tensorflow serving instance is running"
        system "pgrep -f tensorflow_model_server | xargs kill -9"
        with_valid_project
        copy_mnist_files(@project[:id], @project[:projectname], @user[:username])
      end

      before :each do
        @serving = create_tf_serving(@project[:id], @project[:projectname])
      end

      it "should be able to delete a serving instance" do
        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}"
        expect_status_details(200)
      end

      it "should be able to delete a running instance" do
        # Start the serving instance
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
        expect_status(200)

        # Wait until the service instance is running
        wait_for_type(@serving[:name])

        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}"
        expect_status(200)

        sleep(5)

        check_process_running("tensorflow_model_server")
      end
    end
  end
end
