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
      purge_all_tf_serving_instances
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
               servingType: 0
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
               servingType: 0
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
               servingType: 0
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
               servingType: 0
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
               servingType: 0
              }
          expect_json(usrMsg: "Batching is null")
        end

        it "should fail to create a serving without a path" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel3",
               batchingEnabled: false,
               modelVersion: 1,
               servingType: 0
              }
          expect_json(usrMsg: "Artifact path not provided")
        end

        it "should fail to create a serving without a version" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
              {name: "testModel4",
               artifactPath: "/Projects/#{@project[:projectname]}/Models/mnist/",
               batchingEnabled: false,
               servingType: 0
              }
          expect_json(usrMsg: "Serving version not provided")
        end
      end
    end
  end
end
