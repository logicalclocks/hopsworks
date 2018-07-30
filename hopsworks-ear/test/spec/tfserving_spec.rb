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
describe 'tfserving' do
  after (:all){clean_projects}
  describe "#create" do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "not authenticated" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/tfserving/", {hdfsModelPath: "hdfs:///Projects/#{@project[:projectname]}/Models/mnist/1/model.pb", enableBatching: true}
        expect_json(errorMsg: "Client not authorized for this invocation")
        expect_status(401)
      end
    end

    context 'with authentication' do
      before :all do
        with_valid_project
      end
      it 'models dataset should exist' do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/createTopLevelDataSet", {name: "Models", description: "test dataset", searchable: true, generateReadme: true}
        # expect_json(errorMsg: "Invalid folder name for DataSet: A directory with the same name already exists. If you want to replace it delete it first then try recreating.")
        expect_status(400)
      end
      it 'non-existing .pb file should fail creating serving' do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/tfserving/", {hdfsModelPath: "hdfs:///Projects/#{@project[:projectname]}/Models/mnist/1/model.pb", enableBatching: true}
        expect_status(404)
      end
      # ******* Commenting out until it is fixed for CentOS *******

      # it 'serving lifecycle' do
      #   post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/", {name: "Models/mnist", description: "", searchable: true, template: 0}
      #   expect_status(200)
      #   post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/", {name: "Models/mnist/1", description: "", searchable: true, template: 0}
      #   expect_status(200)
      #   post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/dataset/", {name: "Models/mnist/1/mnist.pb", description: "", searchable: true, template: 0}
      #   expect_status(200)
      #
      #   post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/tfserving/", {hdfsModelPath: "hdfs:///Projects/#{@project[:projectname]}/Models/mnist/1/mnist.pb", enableBatching: true}
      #   expect_status(201)
      #
      #   get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/tfserving/"
      #   expect_status(200)
      #
      #   serving = json_body.detect { |serving| serving[:modelName] == "mnist" }
      #   serving_id = serving[:id]
      #
      #   # Start the serving
      #   post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/tfserving/start/#{serving_id}"
      #   expect_status(200)
      #
      #   # Get the logs
      #   get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/tfserving/logs/#{serving_id}"
      #   expect_status(200)
      #
      #   # Stop the serving
      #   post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/tfserving/stop/#{serving_id}"
      #   expect_status(200)
      #
      #   # Delete serving
      #   delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/tfserving/#{serving_id}"
      #   expect_status(200
      # end
    end
  end
end
