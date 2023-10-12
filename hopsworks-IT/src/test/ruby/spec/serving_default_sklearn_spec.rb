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

# serving_default_sklearn_spec.rb: Tests for serving sklearn models on default deployments

require 'json'

describe "On #{ENV['OS']}" do

  before :all do
    # ensure data science profile is enabled
    setVar('enable_data_science_profile', "true")
  end

  after :all do
    clean_all_test_projects(spec: "serving_default_sklearn")
    purge_all_sklearn_serving_instances
  end

  describe 'sklearn_serving' do

    describe "#create" do

      context 'with authentication but without python enabled' do
        before :all do
          with_valid_project
          copy_iris_files(@project[:projectname], @user[:username])

          # Remove conda
          delete_conda_env(@project[:id])
        end

        after :all do
          purge_all_sklearn_serving_instances()
          delete_all_servings(@project[:id])
        end

        it "should fail to create the serving" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
              {name: "testModel",
               modelPath: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier",
               modelVersion: 1,
               predictor: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
               modelServer: "PYTHON",
               modelFramework: "SKLEARN",
               servingTool: "DEFAULT",
               requestedInstances: 1
              })
          expect_status_details(400, error_code: 240012)
        end
      end

      context 'with authentication and python enabled', vm: true do

        before :all do
          with_valid_project
          copy_iris_files(@project[:projectname], @user[:username])
        end

        after :all do
          purge_all_sklearn_serving_instances()
          delete_all_servings(@project[:id])
        end

        # predictor

        it "should fail to create a serving without predictor script" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
              {name: "testModel",
               modelPath: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier",
               modelVersion: 1,
               modelServer: "PYTHON",
               modelFramework: "SKLEARN",
               servingTool: "DEFAULT",
               requestedInstances: 1
              })
          expect_status_details(400, error_code: 240018)
          expect_json(usrMsg: "Default deployments require a predictor")
        end

        it "should fail to create a serving with non-existing predictor script path" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
              {name: "testModel",
               modelPath: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier",
               modelVersion: 1,
               predictor: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier/1/non-existing.py",
               modelServer: "PYTHON",
               modelFramework: "SKLEARN",
               servingTool: "DEFAULT",
               requestedInstances: 1
              })
          expect_status_details(400, error_code: 240016)
        end

        it "should fail to create a serving with non-python predictor script" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
              {name: "testModel",
               modelPath: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier",
               modelVersion: 1,
               predictor: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier/1/iris_knn.pkl",
               modelServer: "PYTHON",
               modelFramework: "SKLEARN",
               servingTool: "DEFAULT",
               requestedInstances: 1
              })
          expect_status_details(422)
          expect_json(usrMsg: "Predictor script should have a valid extension: .py")
        end

        it "should create a serving with a predictor script" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
              {name: "testModel",
               modelPath: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier",
               modelVersion: 1,
               predictor: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
               modelServer: "PYTHON",
               modelFramework: "SKLEARN",
               servingTool: "DEFAULT",
               requestedInstances: 1
              })
          expect_status_details(201)
        end

        # artifact version

        it "should fail to create a serving with artifact version but without predictor" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
              {name: "testModel4",
               modelPath: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier",
               modelVersion: 1,
               artifactVersion: "1",
               modelServer: "PYTHON",
               modelFramework: "SKLEARN",
               servingTool: "DEFAULT",
               requestedInstances: 1
              })
          if kubernetes_installed
            expect_status_details(400, error_code: 240019)
            expect_json(usrMsg: "Other than MODEL-ONLY artifacts require a predictor or transformer")
          else
            expect_status_details(400, error_code: 240014)
            expect_json(usrMsg: "Artifacts only supported in Kubernetes deployments")
          end
        end

        it "should fail to create a serving with MODEL-ONLY artifact and predictor" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
              {name: "testModel5",
               modelPath: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier",
               modelVersion: 1,
               artifactVersion: "0",
               predictor: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
               modelServer: "PYTHON",
               modelFramework: "SKLEARN",
               servingTool: "DEFAULT",
               requestedInstances: 1
              })
          if kubernetes_installed
            expect_status_details(400, error_code: 240019)
            expect_json(usrMsg: "Predictors and transformers cannot be used in MODEL-ONLY artifacts")
          else
            expect_status_details(400, error_code: 240014)
            expect_json(usrMsg: "Artifacts only supported in Kubernetes deployments")
          end
        end

        # request batching

        it "should fail to create a serving with request batching enabled" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
             {name: "testRequestBatchingPythonDefault1",
              modelPath: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier",
              modelVersion: 1,
              predictor: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
              modelServer: "PYTHON",
              modelFramework: "SKLEARN",
              servingTool: "DEFAULT",
              requestedInstances: 1,
              batchingConfiguration: {
                batchingEnabled: true
              }
             })
          expect_status_details(400, error_code: 240025)
          expect_json(usrMsg: "Request batching is not supported in Python deployments")
        end
      end
    end

    describe "#update", vm: true do

      context 'with serving and python enabled' do
        before :all do
          with_valid_project
          with_sklearn_serving(@project[:id], @project[:projectname], @user[:username])
          start_serving(@project, @serving)
          wait_for_type("sklearn_flask_server.py")
        end

        after :all do
          purge_all_sklearn_serving_instances
          delete_all_servings(@project[:id])
        end

        after :each do
          # Check if the process is
          wait_for_type("sklearn_flask_server.py")
        end

        # artifact version

        it "should fail to update the predictor script in an existing artifact" do
          if !kubernetes_installed
            skip "This test only runs without Kubernetes installed"
          end

          serving = Serving.find(@serving[:id])
          topic = ProjectTopics.find(@serving[:kafka_topic_id])
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
             {id: serving[:id],
              name: serving[:name],
              modelPath: serving[:model_path],
              modelVersion: serving[:model_version],
              artifactVersion: serving[:artifact_version],
              predictor: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
              kafkaTopicDTO: {
                  name: topic[:topic_name]
              },
              inferenceLogging: parse_inference_logging(serving[:inference_logging]),
              modelServer: parse_model_server(serving[:model_server]),
              modelFramework: parse_model_framework(serving[:model_framework]),
              servingTool: parse_serving_tool(serving[:serving_tool]),
              requestedInstances: serving[:instances]
              })
          expect_status_details(400, error_code: 240019)
          expect_json(usrMsg: "Existing artifacts cannot be modified. To change predictors or transformers, create a new artifact")
        end

        it "should create a new artifact version when updating the predictor script" do
          if !kubernetes_installed
            skip "This test only runs without Kubernetes installed"
          end

          serving = Serving.find(@serving[:id])
          topic = ProjectTopics.find(@serving[:kafka_topic_id])
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/", parse_serving_json(
             {id: serving[:id],
              name: serving[:name],
              modelPath: serving[:model_path],
              modelVersion: serving[:model_version],
              predictor: "/Projects/#{@project[:projectname]}/Models/irisflowerclassifier/1/#{SKLEARN_SCRIPT_FILE_NAME}",
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

          new_serving = Serving.find(@serving[:id])
          expect(new_serving[:artifact_version]).to be > serving[:artifact_version]
        end
      end
    end

    describe "#start", vm: true do

      context "with serving and python #{ENV['PYTHON_VERSION']}" do
        before :all do
          with_valid_project
          sleep(5)
          with_sklearn_serving(@project[:id], @project[:projectname], @user[:username])
          sleep(5)
        end

        after :all do
          purge_all_sklearn_serving_instances
          delete_all_servings(@project[:id])
        end

        it "should be able to start a serving instance" do
          start_serving(@project, @serving)
          wait_for_type("sklearn_flask_server.py")

          # Sleep a bit to make sure that logs are propagated correctly to the index
          sleep(30)

          # Check that the logs are written in the opensearch index.
          opensearch_rest do
            response = opensearch_get "#{@project[:projectname].downcase}_serving*/_search?q=serving_name:#{@serving[:name]}"
            index = response.body
            parsed_index = JSON.parse(index)
            expect(parsed_index['hits']['total']['value']).to be > 0
          end
        end

        it "should fail to start a running instance" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
          expect_status_details(400, error_code: 240003)
        end
      end
    end

    describe "#stop", vm: true do

      context 'with serving and python enabled' do
        before :all do
          with_valid_project
          with_sklearn_serving(@project[:id], @project[:projectname], @user[:username])
        end

        after :all do
          purge_all_sklearn_serving_instances()
          delete_all_servings(@project[:id])
        end

        it "should mark the serving as failed if the process dies" do
          if kubernetes_installed
            skip "This test does not run on Kubernetes"
          end

          start_serving(@project, @serving)
          wait_for_type("sklearn_flask_server.py")

          # Simulate the process dying by its own
          system "pgrep -f sklearn_flask_server.py | xargs kill -9"

          # Wait a bit
          sleep(30)

          # Check that the serving is reported as dead
          serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
          serving = JSON.parse(serving_list).select {|serving| serving["name"] == @serving[:name]}[0]
          expect(serving['status']).to eql "Failed"
        end
      end
    end
    
    describe "#delete", vm: true do

      context 'with serving and python enabled' do
        before :all do
          with_valid_project
          copy_iris_files(@project[:projectname], @user[:username])
        end

        before :each do
          @serving = create_sklearn_serving(@project[:id], @project[:projectname])
        end

        after :all do
          purge_all_sklearn_serving_instances()
          delete_all_servings(@project[:id])
        end

        it "should mark the serving as failed if the process dies" do
          if kubernetes_installed
            skip "This test does not run on Kubernetes"
          end

          start_serving(@project, @serving)
          wait_for_type("sklearn_flask_server.py")

          # Simulate the process dying by its own
          system "pgrep -f sklearn_flask_server.py | xargs kill -9"

          # Wait a bit
          sleep(30)

          # Check that the serving is reported as dead
          serving_list = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/"
          serving = JSON.parse(serving_list).select {|serving| serving["name"] == @serving[:name]}[0]
          expect(serving['status']).to eql "Failed"
        end

        it "should delete a serving instance" do
          delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}"
          expect_status_details(200)
        end

        it "should delete a running serving instance" do
          start_serving(@project, @serving)
          wait_for_type("sklearn_flask_server.py")

          # Sleep a bit to make sure that logs are propagated correctly to the index
          sleep(30)

          delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}"
          expect_status_details(200)
        end
      end
    end
  end
end