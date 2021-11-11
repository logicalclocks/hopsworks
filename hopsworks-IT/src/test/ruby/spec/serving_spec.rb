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
    clean_all_test_projects(spec: "serving")
    purge_all_tf_serving_instances
  end
  describe 'serving' do
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
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end

      context 'with authentication', vm: true do
        before :all do
          with_valid_project
          copy_mnist_files(@project[:id], @project[:projectname], @user[:username])
        end

        it "fail to create a serving with bad kafka configuration" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModelBadKafka",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false,
               kafkaTopicDTO: {
                   name: "CREATE",
                   numOfPartitions: -10,
                   numOfReplicas: 5
               },
               modelServer: "TENSORFLOW_SERVING",
               servingTool: "DEFAULT",
               requestedInstances: 1
              }
          expect_json(errorMsg: "Maximum topic replication factor exceeded")
          expect_status(400)
        end

        it "should fail to create the serving without a name" do
          # Create serving
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               batchingEnabled: false,
               modelVersion: 1,
               modelServer: "TENSORFLOW_SERVING",
               servingTool: "DEFAULT",
               requestedInstances: 1
              }
          expect_json(usrMsg: "Serving name not provided")
        end

        it "fail to create a serving with space in the name" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "test Model1",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
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

        it "fail to create a serving without batching specified" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "nobatchingModels",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               kafkaTopicDTO: {
                   name: "CREATE",
                   numOfPartitions: 1,
                   numOfReplicas: 1
               },
               modelServer: "TENSORFLOW_SERVING",
               servingTool: "DEFAULT",
               requestedInstances: 1
              }
          expect_json(usrMsg: "Batching is null")
        end

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

        it "should fail to create a serving without a version" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel4",
               modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               batchingEnabled: false,
               modelServer: "TENSORFLOW_SERVING",
               servingTool: "DEFAULT",
               requestedInstances: 1
              }
          expect_json(usrMsg: "Model version not provided")
        end

          it "should fail to create a serving without model server" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel5",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingEnabled: false,
                 servingTool: "DEFAULT",
                 requestedInstances: 1
                }
            expect_json(usrMsg: "Model server not provided or unsupported")
            expect_status(422)
          end

          it "should fail to create a serving without serving tool" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel6",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingEnabled: false,
                 modelServer: "TENSORFLOW_SERVING",
                 requestedInstances: 1
                }
            expect_json(usrMsg: "Serving tool not provided or invalid")
            expect_status(422)
          end

        it "should fail to create a serving without requested instances" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
                {name: "testmodel7",
                 modelPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
                 modelVersion: 1,
                 batchingEnabled: false,
                 modelServer: "TENSORFLOW_SERVING",
                 servingTool: "DEFAULT"
                }
          expect_json(usrMsg: "Number of instances not provided")
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
          copy_mnist_files(@project[:id], @project[:projectname], @user[:username])
          create_tf_serving(@project[:id], @project[:projectname])
          create_tf_serving(@project[:id], @project[:projectname])
          @tf_serving = create_tf_serving(@project[:id], @project[:projectname])
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

            wait_for_type(@tf_serving[:name])

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
end
