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
  describe 'tfserving' do
    before (:all) do
      if ENV['OS'] == "centos"
        skip "These tests do not run on centos"
      end
    end

    after (:all) do
      clean_projects
      purge_all_serving_instances
    end

    describe "#create" do

      context 'without authentication' do
        before :all do
          with_valid_project
          reset_session
        end

        it "should fail to create the serving" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {modelName: "testModel",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false }
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end

      context 'with authentication' do
        before :all do
          with_valid_project

          mkdir("/Projects/#{@project[:projectname]}/Models/mnist/", @user[:username], "#{@project[:projectname]}__Models", 750)
          copy(TOUR_FILE_LOCATION, "/Projects/#{@project[:projectname]}/Models/mnist/", @user[:username], "#{@project[:projectname]}__Models", 750)
        end

        it "should create the serving without Kafka topic" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {modelName: "testModel",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false}
          expect_status(201)

          serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
          kafka_topic = JSON.parse(serving_list).select { |serving| serving['modelName'] == "testModel"}[0]['kafkaTopicDTO']
          expect(kafka_topic).to be nil
        end

        it "should create the serving with batching" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {modelName: "testModelBatching",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: true}
          expect_status(201)
        end

        it "should create the serving with a new Kafka topic" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {modelName: "testModel1",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false,
               kafkaTopicDTO: {
                   name: "CREATE",
                   numOfPartitions: 1,
                   numOfReplicas: 1
               }}
          expect_status(201)

          # Kafka authorizer needs some time to take up the new permissions.
          sleep(5)

          # Check that the topic has been created correctly
          serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
          kafka_topic_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/kafka/topics"

          kafka_topic_name = JSON.parse(serving_list).select { |serving| serving['modelName'] == "testModel1"}[0]['kafkaTopicDTO']['name']
          kafka_topic = JSON.parse(kafka_topic_list).select { |topic| topic['name'] == kafka_topic_name}
          expect(kafka_topic.size).to eq 1
          expect(kafka_topic[0]['schemaName']).to eql INFERENCE_SCHEMA_NAME
        end

        it "should create the serving with an existing Kakfa topic" do
          # Create kafka topic
          json, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, INFERENCE_SCHEMA_VERSION)

          # Create serving
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {modelName: "testModel2",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false,
               kafkaTopicDTO: {
                   name: topic_name
               }}
          expect_status(201)

          # Kafka authorizer needs some time to take up the new permissions.
          sleep(5)

          # Check that the serving is actually using that topic
          serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
          kafka_topic_name = JSON.parse(serving_list).select { |serving| serving['modelName'] == "testModel2"}[0]['kafkaTopicDTO']['name']
          expect(kafka_topic_name).to eql topic_name
        end

        it "fail to create a serving with the same name" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {modelName: "testModel1",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false,
               kafkaTopicDTO: {
                   name: "CREATE",
                   numOfPartitions: 1,
                   numOfReplicas: 1
               }}
          expect_json(errorMsg: "An entry with the same name already exists in this project")
          expect_status(400)
        end

        it "fail to create a serving with bad kafka configuration" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {modelName: "testModelBadKafka",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false,
               kafkaTopicDTO: {
                   name: "CREATE",
                   numOfPartitions: -10,
                   numOfReplicas: 5
               }}
          expect_json(errorMsg: "Maximum topic replication factor exceeded")
          expect_status(400)
        end

        it "should fail to create the serving without a name" do
          # Create serving
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               batchingEnabled: false,
               modelVersion: 1}
          expect_json(usrMsg: "Model name not provided")
        end

        it "fail to create a serving with space in the name" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {modelName: "test Model1",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false,
               kafkaTopicDTO: {
                   name: "CREATE",
                   numOfPartitions: 1,
                   numOfReplicas: 1
               }}
          expect_json(usrMsg: "Model name cannot contain spaces")
          expect_status(422)
        end

        it "fail to create a serving without batching specified" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {modelName: "nobatchingModels",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               kafkaTopicDTO: {
                   name: "CREATE",
                   numOfPartitions: 1,
                   numOfReplicas: 1
               }}
          expect_json(usrMsg: "Batching is null")
        end

        it "should fail to create a serving without a path" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {modelName: "testModel3",
               batchingEnabled: false,
               modelVersion: 1}
          expect_json(usrMsg: "Model path not provided")
        end

        it "should fail to create a serving without a version" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {modelName: "testModel4",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               batchingEnabled: false}
          expect_json(usrMsg: "Model version not provided")
        end

        it "should fail to create a serving with a non-standard path" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {modelName: "testModel5",
               modelPath: "/Projects/#{@project[:projectname]}/DOESNTEXISTS",
               batchingEnabled: false,
               modelVersion: 1}
          expect_json(usrMsg: "The model path provided does not exists")
          expect_status(422)
        end

        it "should fail to create a serving with a non-standard path" do
          rm("/Projects/#{@project[:projectname]}/Models/mnist/1/saved_model.pb")

          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {modelName: "testModel6",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               batchingEnabled: false,
               modelVersion: 1}
          expect_json(usrMsg: "The model path does not respect the TensorFlow standard")
          expect_status(422)
        end
      end

    end

    describe "#start" do
      before :all do
        with_valid_project
        with_serving(@project[:id], @project[:projectname], @user[:username])
      end

      after :all do
        purge_all_serving_instances
      end

      it "should be able to start a serving instance" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
        expect_status(200)

        # Check if the process is running on the host
        wait_for do
          system "pgrep -f #{@serving[:model_name]} -a"
          $?.exitstatus == 0
        end

        # Sleep a bit to make sure that logs are propagated correctly to the index
        sleep(30)

        # Check that the logs are written in the elastic index.
        Airborne.configure do |config|
          config.base_url = ''
        end

        index = get "#{ENV['ELASTIC_API']}/#{@project[:projectname]}_serving*/_search?q=modelname:#{@serving[:model_name]}"

        Airborne.configure do |config|
          config.base_url = "http://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
        end

        parsed_index = JSON.parse(index)
        expect(parsed_index['hits']['total']).to be > 0
      end

      it "should fail to start a running instance" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
        expect_status(400)
        expect_json(errorCode: 240003)
        expect_json(usrMsg: "Instance is already started")
      end
    end

    describe "#update" do
      before :all do
        with_valid_project
        with_serving(@project[:id], @project[:projectname], @user[:username])

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
        expect_status(200)

        wait_for do
          system "pgrep -f #{@serving[:model_name]} -a"
          $?.exitstatus == 0
        end
      end

      after :all do
        purge_all_serving_instances
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
             modelName: "testModelChanged",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
             modelVersion: 1,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: @topic[:topic_name]
             }}
        expect_status(201)
      end

      it "should be able to update the version" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: @serving[:id],
             modelName: "testModelChanged",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
             modelVersion: 2,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: @topic[:topic_name]
             }}
        expect_status(201)
      end

      it "should be able to update the batching" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: @serving[:id],
             modelName: "testModelChanged",
             modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
             modelVersion: 2,
             batchingEnabled: true,
             kafkaTopicDTO: {
                 name: @topic[:topic_name]
             }}
        expect_status(201)
      end

      it "should be able to update the path" do
        mkdir("/Projects/#{@project[:projectname]}/Models/newMnist/", @user[:username],
              "#{@project[:projectname]}__Models", 750)

        copy("/Projects/#{@project[:projectname]}/Models/mnist/*",
             "/Projects/#{@project[:projectname]}/Models/newMnist/",
             @user[:username], "#{@project[:projectname]}__Models", 750)

        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: @serving[:id],
             modelName: "testModelChanged",
             modelPath: "/Projects/#{@project[:projectname]}/Models/newMnist/",
             modelVersion: 2,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: @topic[:topic_name]
             }}
        expect_status(201)
      end

      it "should be able to change the kafka topic it's writing to"  do
        json_result, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, INFERENCE_SCHEMA_VERSION)

        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: @serving[:id],
             modelName: "testModelChanged",
             modelPath: "/Projects/#{@project[:projectname]}/Models/newMnist/",
             modelVersion: 2,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: topic_name
             }}
        expect_status(201)

        serving = TfServing.find(@serving[:id])
        new_topic = ProjectTopics.find_by(topic_name: topic_name, project_id: @project[:id])
        expect(serving[:kafka_topic_id]).to be new_topic[:id]
      end

      it "should be able to stop writing to a kafka topic" do
        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
            {id: @serving[:id],
             modelName: "testModelChanged",
             modelPath: "/Projects/#{@project[:projectname]}/Models/newMnist/",
             modelVersion: 2,
             batchingEnabled: false,
             kafkaTopicDTO: {
                 name: "NONE"
             }}
        expect_status(201)

        serving = TfServing.find(@serving[:id])
        expect(serving[:kafka_topic_id]).to be nil
      end
    end

    describe "#kill" do
      before :all do
        with_valid_project
        with_serving(@project[:id], @project[:projectname], @user[:username])
      end

      before :each do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
        expect_status(200)
      end

      it "should be able to kill a running serving instance" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
        expect_status(200)

        # check if the process is running on the host
        system "pgrep -f tensorflow_model_server"
        if $?.exitstatus != 1
          raise "the process is still running"
        end
      end

      it "should fail to kill a non running instance" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
        expect_status(200)

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
        serving = JSON.parse(serving_list).select { |serving| serving[:modelName] == @serving[:modelName]}[0]
        expect(serving['status']).to eql "Stopped"
      end
    end

    describe "#delete" do
      before :all do
        # Make sure no tensorflow serving instance is running"
        system "pgrep -f tensorflow_model_server | xargs kill -9"
        with_valid_project


        mkdir("/Projects/#{@project[:projectname]}/Models/mnist/", @user[:username], "#{@project[:projectname]}__Models", 750)
        copy(TOUR_FILE_LOCATION, "/Projects/#{@project[:projectname]}/Models/mnist/", @user[:username], "#{@project[:projectname]}__Models", 750)
      end

      before :each do
        @serving = create_serving(@project[:id], @project[:projectname])
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

        # Check that the process has been killed
        wait_for do
          system "pgrep -f tensorflow_model_server"
          $?.exitstatus == 1
        end
      end
    end
  end
end
