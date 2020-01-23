=begin
 This file is part of Hopsworks
 Copyright (C) 2019, Logical Clocks AB. All rights reserved

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
  after (:all) do
    clean_all_test_projects
    purge_all_sklearn_serving_instances
  end
  describe 'sklearn_serving' do
    before (:all) do
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
               artifactPath: "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
               modelVersion: 1,
               servingType: "SKLEARN"
              }
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end

      context 'with authentication but without python enabled' do
        before :all do
          with_valid_project
          # Make Serving Dir
          mkdir("/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/", "#{@user[:username]}",
                "#{@project[:projectname]}__Models", 750)
          # Make Version Dir
          mkdir("/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1", "#{@user[:username]}",
                "#{@project[:projectname]}__Models", 750)
          # Copy model to the servingversion dir
          copy(SKLEARN_MODEL_TOUR_FILE_LOCATION, "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/",
               "#{@user[:username]}",
               "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")
          # Copy script to the servingversion dir
          copy(SKLEARN_SCRIPT_TOUR_FILE_LOCATION, "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/",
               "#{@user[:username]}",
               "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")
          # Remove conda
          delete_conda_env(@project[:id])
        end

        after :all do
          purge_all_sklearn_serving_instances()
          delete_all_sklearn_serving_instances(@project)
        end

        it "should fail to create the serving" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
               modelVersion: 1,
               servingType: "SKLEARN"
              }
          expect_json(errorCode: 240012)
          expect_status(400)
        end
      end

      context 'with authentication, and python enabled but with a non-existent HDFS-script path' do
        before :all do
          with_valid_project
          with_python_enabled(@project[:id], "3.6")
        end

        after :all do
          purge_all_sklearn_serving_instances()
          delete_all_sklearn_serving_instances(@project)
        end

        it "should fail to create the serving" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/NONEXISTENT",
               modelVersion: 1,
               servingType: "SKLEARN"
              }
          expect_json(errorCode: 120001)
          expect_status(422)
        end
      end

      context 'with authentication and python enabled', vm: true do
        before :all do
          with_valid_project
          with_python_enabled(@project[:id], "3.6")

          # Make Serving Dir
          mkdir("/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/", "#{@user[:username]}",
                "#{@project[:projectname]}__Models", 750)
          # Make Version Dir
          mkdir("/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1", "#{@user[:username]}",
                "#{@project[:projectname]}__Models", 750)
          # Copy model to the servingversion dir
          copy(SKLEARN_MODEL_TOUR_FILE_LOCATION, "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/",
               "#{@user[:username]}",
               "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")
          # Copy script to the servingversion dir
          copy(SKLEARN_SCRIPT_TOUR_FILE_LOCATION, "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/",
               "#{@user[:username]}",
               "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")
        end

        after :all do
          purge_all_sklearn_serving_instances()
          delete_all_sklearn_serving_instances(@project)
        end

        it "should create the serving without Kafka topic" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
               modelVersion: 1,
               servingType: "SKLEARN"
              }
          expect_status(201)

          serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
          kafka_topic = JSON.parse(serving_list).select {|serving| serving['name'] == "testModel"}[0]['kafkaTopicDTO']
          expect(kafka_topic).to be nil
        end

        it "should create the serving with a new Kafka topic" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel1",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
               modelVersion: 1,
               kafkaTopicDTO: {
                   name: "CREATE",
                   numOfPartitions: 1,
                   numOfReplicas: 1
               },
               servingType: "SKLEARN"
              }
          expect_status(201)

          # Kafka authorizer needs some time to take up the new permissions.
          sleep(5)

          # Check that the topic has been created correctly
          serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/kafka/topics"
	      expect(json_body[:items].count).to eq 1
          expect(json_body[:items][0][:schemaName]).to eql INFERENCE_SCHEMA_NAME
        end

        it "should create the serving with an existing Kafka topic with Inference Schema version 1" do
          # Create kafka topic
          json, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, 1)

          # Create serving
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel2",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
               modelVersion: 1,
               kafkaTopicDTO: {
                   name: topic_name
               },
               servingType: "SKLEARN"
              }
          expect_status(201)

          # Kafka authorizer needs some time to take up the new permissions.
          sleep(5)

          # Check that the serving is actually using that topic
          serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
          kafka_topic_name = JSON.parse(serving_list).select {|serving| serving['name'] == "testModel2"}[0]['kafkaTopicDTO']['name']
          expect(kafka_topic_name).to eql topic_name
        end

        it "should create the serving with an existing Kafka topic with Inference Schema version 2" do
          # Create kafka topic
          json, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, 2)

          # Create serving
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel3",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
               modelVersion: 1,
               kafkaTopicDTO: {
                   name: topic_name
               },
               servingType: "SKLEARN"
              }
          expect_status(201)

          # Kafka authorizer needs some time to take up the new permissions.
          sleep(5)

          # Check that the serving is actually using that topic
          serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
          kafka_topic_name = JSON.parse(serving_list).select {|serving| serving['name'] ==
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
              {name: "testModel4",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
               modelVersion: 1,
               kafkaTopicDTO: {
                   name: topic_name
               },
               servingType: "SKLEARN"
              }
          expect_status(400)
        end

        it "fail to create a serving with the same name" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel1",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
               modelVersion: 1,
               kafkaTopicDTO: {
                   name: "CREATE",
                   numOfPartitions: 1,
                   numOfReplicas: 1
               },
               servingType: "SKLEARN"
              }
          expect_json(errorMsg: "An entry with the same name already exists in this project")
          expect_status(400)
        end

        it "should fail to create a serving with a non-existent path" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel5",
               artifactPath: "/Projects/#{@project[:projectname]}/DOESNTEXISTS",
               modelVersion: 1,
               servingType: "SKLEARN"
              }
          expect_json(errorCode: 120001)
          expect_status(422)
        end
      end
    end

    describe "#start", vm: true do
      context 'with serving and python 3.6' do
        before :all do
          with_valid_project
          with_python_enabled(@project[:id], "3.6")
          sleep(5)
          with_sklearn_serving(@project[:id], @project[:projectname], @user[:username])
          sleep(5)
        end

        after :all do
          purge_all_sklearn_serving_instances()
          delete_all_sklearn_serving_instances(@project)
        end

        it "should be able to start a serving instance" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
          expect_status(200)

          # Check if the process is running on the host
          wait_for do
            system "pgrep -f sklearn_flask_server.py -a"
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
    end

    describe "#update", vm: true do
      context 'with serving and python enabled' do
        before :all do
          with_valid_project
          with_python_enabled(@project[:id], "3.6")
          with_sklearn_serving(@project[:id], @project[:projectname], @user[:username])

          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
          expect_status(200)

          wait_for do
            system "pgrep -f sklearn_flask_server.py -a"
            $?.exitstatus == 0
          end
        end

        after :all do
          purge_all_sklearn_serving_instances()
          delete_all_sklearn_serving_instances(@project)
        end

        after :each do
          # Check if the process is
          wait_for do
            system "pgrep -f sklearn_flask_server.py -a"
            $?.exitstatus == 0
          end
        end

        it "should be able to update the name" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {id: @serving[:id],
               name: "testModelChanged",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
               modelVersion: 1,
               kafkaTopicDTO: {
                   name: @topic[:topic_name]
               },
               servingType: "SKLEARN"
              }
          expect_status(201)
        end

        it "should be able to update the version" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {id: @serving[:id],
               name: "testModelChanged",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
               modelVersion: 2,
               kafkaTopicDTO: {
                   name: @topic[:topic_name]
               },
               servingType: "SKLEARN"
              }
          expect_status(201)
        end

        it "should not be able to update the serving type" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {id: @serving[:id],
               name: "testModelChanged",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
               modelVersion: 2,
               kafkaTopicDTO: {
                   name: @topic[:topic_name]
               },
               servingType: "TENSORFLOW"
              }
          expect_status(422)
        end

        it "should be able to change the kafka topic it's writing to" do
          json_result, topic_name = add_topic(@project[:id], INFERENCE_SCHEMA_NAME, INFERENCE_SCHEMA_VERSION)

          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {id: @serving[:id],
               name: "testModelChanged",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
               modelVersion: 2,
               kafkaTopicDTO: {
                   name: topic_name
               },
               servingType: "SKLEARN"
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
               artifactPath: "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
               modelVersion: 2,
               kafkaTopicDTO: {
                   name: "NONE"
               },
               servingType: "SKLEARN"
              }
          expect_status(201)

          serving = Serving.find(@serving[:id])
          expect(serving[:kafka_topic_id]).to be nil
        end
      end
    end

    describe "#kill", vm: true do
      context 'with serving and python enabled' do
        before :all do
          with_valid_project
          with_python_enabled(@project[:id], "3.6")
          with_sklearn_serving(@project[:id], @project[:projectname], @user[:username])
        end

        after :all do
          purge_all_sklearn_serving_instances()
          delete_all_sklearn_serving_instances(@project)
        end

        before :each do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
          expect_status(200)
        end

        it "should be able to kill a running serving instance" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
          expect_status(200)

          # Wait a bit
          sleep(30)

          # check if the process is running on the host
          system "pgrep -f sklearn_flask_server.py"
          if $?.exitstatus != 1
            raise "the process is still running"
          end
        end

        it "should fail to kill a non running instance" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=stop"
          expect_status(200)

          # Wait a bit
          sleep(30)

          # check if the process is running on the host
          system "pgrep -f sklearn_flask_server.py"
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
          system "pgrep -f sklearn_flask_server.py | xargs kill -9"

          # Wait a bit
          sleep(30)

          # Check that the serving is reported as dead
          serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
          serving = JSON.parse(serving_list).select {|serving| serving["name"] == @serving[:name]}[0]
          expect(serving['status']).to eql "Stopped"
        end
      end
    end

    describe "#delete", vm: true do
      context 'with serving and python enabled' do
        before :all do
          # Make sure no sklearn serving instance is running"
          system "pgrep -f sklearn_flask_server.py | xargs kill -9"
          with_valid_project
          with_python_enabled(@project[:id], "3.6")

          # Make Serving Dir
          mkdir("/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/", "#{@user[:username]}",
                "#{@project[:projectname]}__Models", 750)
          # Make Version Dir
          mkdir("/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1", "#{@user[:username]}",
                "#{@project[:projectname]}__Models", 750)
          # Copy model to the servingversion dir
          copy(SKLEARN_MODEL_TOUR_FILE_LOCATION, "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/",
               "#{@user[:username]}",
               "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")
          # Copy script to the servingversion dir
          copy(SKLEARN_SCRIPT_TOUR_FILE_LOCATION, "/Projects/#{@project[:projectname]}/Models/IrisFlowerClassifier/1/",
               "#{@user[:username]}",
               "#{@project[:projectname]}__Models", 750, "#{@project[:projectname]}")
        end

        after :all do
          purge_all_sklearn_serving_instances()
          delete_all_sklearn_serving_instances(@project)
        end

        before :each do
          @serving = create_sklearn_serving(@project[:id], @project[:projectname])
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
            system "pgrep -f sklearn_flask_server.py"
            $?.exitstatus == 0
          end

          delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}"
          expect_status(200)

          # Check that the process has been killed
          wait_for do
            system "pgrep -f sklearn_flask_server.py"
            $?.exitstatus == 1
          end
        end
      end
    end
  end
end
