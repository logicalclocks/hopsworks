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
    purge_all_tf_serving_instances
  end
  describe 'serving' do
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

        it "fail to create a serving with bad kafka configuration" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModelBadKafka",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               batchingEnabled: false,
               kafkaTopicDTO: {
                   name: "CREATE",
                   numOfPartitions: -10,
                   numOfReplicas: 5
               },
               servingType: "TENSORFLOW"
              }
          expect_json(errorMsg: "Maximum topic replication factor exceeded")
          expect_status(400)
        end

        it "should fail to create the serving without a name" do
          # Create serving
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {artifactPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               batchingEnabled: false,
               modelVersion: 1,
               servingType: "TENSORFLOW"
              }
          expect_json(usrMsg: "Serving name not provided")
        end

        it "fail to create a serving with space in the name" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "test Model1",
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
          expect_json(usrMsg: "Serving name cannot contain spaces")
          expect_status(422)
        end

        it "fail to create a serving without batching specified" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "nobatchingModels",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               modelVersion: 1,
               kafkaTopicDTO: {
                   name: "CREATE",
                   numOfPartitions: 1,
                   numOfReplicas: 1
               },
               servingType: "TENSORFLOW"
              }
          expect_json(usrMsg: "Batching is null")
        end

        it "should fail to create a serving without a path" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel3",
               batchingEnabled: false,
               modelVersion: 1,
               servingType: "TENSORFLOW"
              }
          expect_json(usrMsg: "Artifact path not provided")
        end

        it "should fail to create a serving without a version" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel4",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               batchingEnabled: false,
               servingType: "TENSORFLOW"
              }
          expect_json(usrMsg: "Serving version not provided")
        end
      end
    end
  end
end
