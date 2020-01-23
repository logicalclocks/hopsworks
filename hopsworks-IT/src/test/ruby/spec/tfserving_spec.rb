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
    clean_all_test_projects
    purge_all_tf_serving_instances
  end
  describe 'tfserving' do
    before (:all) do
      if ENV['OS'] == "centos"
        skip "These tests do not run on centos"
      end
    end

    describe "#create" do

      context 'without authentication' do
        before :all do
          with_valid_project
          reset_session
        end

        it "should fail to create the serving" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false,
               servingType: "TENSORFLOW"
              }
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end

      context 'with authentication', vm: true do
        before :all do
          with_valid_project

          mkdir("/Projects/#{@project[:projectname]}/Models/mnist/", @user[:username], "#{@project[:projectname]}__Models", 750)
          copy(TF_MODEL_TOUR_FILE_LOCATION, "/Projects/#{@project[:projectname]}/Models/mnist/", @user[:username], "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")
        end

        it "should create the serving without Kafka topic" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false,
               servingType: "TENSORFLOW"
              }
          expect_status(201)

          serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
          kafka_topic = JSON.parse(serving_list).select { |serving| serving['name'] == "testModel"}[0]['kafkaTopicDTO']
          expect(kafka_topic).to be nil
        end

        it "should create the serving with batching" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModelBatching",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: true,
               servingType: "TENSORFLOW"
              }
          expect_status(201)
        end

        it "should create the serving with a new Kafka topic" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel1",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false,
               kafkaTopicDTO: {
                   name: "CREATE",
                   numOfPartitions: 1,
                   numOfReplicas: 1
               },
               servingType: "TENSORFLOW"
              }
          expect_status(201)

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
               artifactPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false,
               kafkaTopicDTO: {
                   name: topic_name
               },
               servingType: "TENSORFLOW"
              }
          expect_status(201)

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
               artifactPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false,
               kafkaTopicDTO: {
                   name: topic_name
               },
               servingType: "TENSORFLOW"
              }
          expect_status(201)

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
               artifactPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false,
               kafkaTopicDTO: {
                   name: topic_name
               },
               servingType: "TENSORFLOW"
              }
          expect_status(400)
        end

        it "should fail to create a serving with the same name" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel1",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false,
               kafkaTopicDTO: {
                   name: "CREATE",
                   numOfPartitions: 1,
                   numOfReplicas: 1
               },
               servingType: "TENSORFLOW"
              }
          expect_json(errorMsg: "An entry with the same name already exists in this project")
          expect_status(400)
        end

        it "should fail to create a serving with a non-existent path" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel5",
               artifactPath: "/Projects/#{@project[:projectname]}/DOESNTEXISTS",
               batchingEnabled: false,
               modelVersion: 1,
               servingType: "TENSORFLOW"
              }
          expect_json(usrMsg: "The model path provided does not exists")
          expect_status(422)
        end

        it "should fail to create a serving with a non-standard path" do
          rm("/Projects/#{@project[:projectname]}/Models/mnist/1/saved_model.pb")

          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel6",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               batchingEnabled: false,
               modelVersion: 1,
               servingType: "TENSORFLOW"
              }
          expect_json(usrMsg: "The model path does not respect the TensorFlow standard")
          expect_status(422)
        end
      end

    end

    describe "#start", vm: true do
      before :all do
        with_valid_project
        with_tf_serving(@project[:id], @project[:projectname], @user[:username])
      end

      after :all do
        purge_all_tf_serving_instances
      end

      it "should be able to start a serving instance" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
        expect_status(200)

        # Check if the process is running on the host
        wait_for do
          system "pgrep -f #{@serving[:name]} -a"
          $?.exitstatus == 0
        end

        # Sleep a bit to make sure that logs are propagated correctly to the index
        sleep(30)

        # Check that the logs are written in the elastic index.
        Airborne.configure do |config|
          config.base_url = ''
        end

        response = elastic_get "#{@project[:projectname].downcase}_serving*/_search?q=modelname:#{@serving[:name]}"
        index = response.body
        Airborne.configure do |config|
          config.base_url = "https://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
        end

        parsed_index = JSON.parse(index)
        expect(parsed_index['hits']['total']['value']).to be > 0
      end

      it "should fail to start a running instance" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
        expect_status(400)
        expect_json(errorCode: 240003)
        expect_json(usrMsg: "Instance is already started")
      end
    end

    describe "#update", vm: true do
      before :all do
        with_valid_project
        with_tf_serving(@project[:id], @project[:projectname], @user[:username])

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
        expect_status(200)

        wait_for do
          system "pgrep -f #{@serving[:name]} -a"
          $?.exitstatus == 0
        end
      end

      after :all do
        purge_all_tf_serving_instances
      end

      after :each do
        # Check if the process is
        wait_for do
          system "pgrep -f testModelChanged -a"
          $?.exitstatus == 0
        end
      end

      it "should be able to update the name" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: @serving[:id],
             name: "testModelChanged",
             artifactPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
             modelVersion: 1,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: @topic[:topic_name]
             },
             servingType: "TENSORFLOW"
            }
        expect_status(201)
      end

      it "should be able to update the version" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: @serving[:id],
             name: "testModelChanged",
             artifactPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
             modelVersion: 2,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: @topic[:topic_name]
             },
             servingType: "TENSORFLOW"
            }
        expect_status(201)
      end

      it "should be able to update the batching" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: @serving[:id],
             name: "testModelChanged",
             artifactPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
             modelVersion: 2,
             batchingEnabled: true,
             kafkaTopicDTO: {
                 name: @topic[:topic_name]
             },
             servingType: "TENSORFLOW"
            }
        expect_status(201)
      end

      it "should be able to update the path" do
        mkdir("/Projects/#{@project[:projectname]}/Models/newMnist/", @user[:username],
              "#{@project[:projectname]}__Models", 750)

        copy("/Projects/#{@project[:projectname]}/Models/mnist/*",
             "/Projects/#{@project[:projectname]}/Models/newMnist/",
             @user[:username], "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")

        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: @serving[:id],
             name: "testModelChanged",
             artifactPath: "/Projects/#{@project[:projectname]}/Models/newMnist/",
             modelVersion: 2,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: @topic[:topic_name]
             },
             servingType: "TENSORFLOW"
            }
        expect_status(201)
      end

      it "should not be able to update the serving type" do

        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: @serving[:id],
             name: "testModelChanged",
             artifactPath: "/Projects/#{@project[:projectname]}/Models/newMnist/",
             modelVersion: 2,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: @topic[:topic_name]
             },
             servingType: "SKLEARN"
            }
        expect_status(422)
      end

      it "should be able to change the kafka topic it's writing to"  do
        json_result, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, INFERENCE_SCHEMA_VERSION)

        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: @serving[:id],
             name: "testModelChanged",
             artifactPath: "/Projects/#{@project[:projectname]}/Models/newMnist/",
             modelVersion: 2,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: topic_name
             },
             servingType: "TENSORFLOW"
            }
        expect_status(201)

        serving = Serving.find(@serving[:id])
        new_topic = ProjectTopics.find_by(topic_name: topic_name, project_id: @project[:id])
        expect(serving[:kafka_topic_id]).to be new_topic[:id]
      end

      it "should be able to stop writing to a kafka topic" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: @serving[:id],
             name: "testModelChanged",
             artifactPath: "/Projects/#{@project[:projectname]}/Models/newMnist/",
             modelVersion: 2,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: "NONE"
             },
             servingType: "TENSORFLOW"
            }
        expect_status(201)

        serving = Serving.find(@serving[:id])
        expect(serving[:kafka_topic_id]).to be nil
      end
    end

    describe "#kill", vm: true do
      before :all do
        with_valid_project
        with_tf_serving(@project[:id], @project[:projectname], @user[:username])
      end

      before :each do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
        expect_status(200)
      end

      it "should be able to kill a running serving instance" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
        expect_status(200)

        sleep(5)

        # check if the process is running on the host
        system "pgrep -f tensorflow_model_server"
        if $?.exitstatus != 1
          raise "the process is still running"
        end
      end

      it "should fail to kill a non running instance" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
        expect_status(200)

        sleep(5)

        # check if the process is running on the host
        system "pgrep -f tensorflow_model_server"
        if $?.exitstatus != 1
          raise "the process is still running"
        end

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
        expect_status(400)
        expect_json(errorCode: 240003)
        expect_json(usrMsg: "Instance is already stopped")
      end

      it "should mark the serving as not running if the process dies" do
        # Simulate the process dying by its own
        system "pgrep -f tensorflow_model_server | xargs kill -9"

        # Wait a bit
        sleep(30)

        # Check that the serving is reported as dead
        serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
        serving = JSON.parse(serving_list).select { |serving| serving["name"] == @serving[:name]}[0]
        expect(serving['status']).to eql "Stopped"
      end
    end

    describe "#delete", vm: true do
      before :all do
        # Make sure no tensorflow serving instance is running"
        system "pgrep -f tensorflow_model_server | xargs kill -9"
        with_valid_project


        mkdir("/Projects/#{@project[:projectname]}/Models/mnist/", @user[:username], "#{@project[:projectname]}__Models", 750)
        copy(TF_MODEL_TOUR_FILE_LOCATION, "/Projects/#{@project[:projectname]}/Models/mnist/", @user[:username], "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")
      end

      before :each do
        @serving = create_tf_serving(@project[:id], @project[:projectname])
      end

      it "should be able to delete a serving instance" do
        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}"
        expect_status(200)
      end

      it "should be able to delete a running instance" do
        # Start the serving instance
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
        expect_status(200)

        # Wait until the service instance is running
        wait_for do
          system "pgrep -f tensorflow_model_server"
          $?.exitstatus == 0
        end

        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}"
        expect_status(200)

        sleep(5)

        # Check that the process has been killed
        wait_for do
          system "pgrep -f tensorflow_model_server"
          $?.exitstatus == 1
        end
      end
    end
  end
end
