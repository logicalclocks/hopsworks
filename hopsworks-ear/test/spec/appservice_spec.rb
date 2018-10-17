=begin
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
=end

describe "On #{ENV['OS']}" do
  describe 'appservice' do
    after (:all) {clean_projects}

    describe "get kafka schema from job service/jupyter authenticated with keystore/pwd" do

      context 'with valid project but without kafka topic and without valid keystore/pwd' do
        before :all do
          with_valid_project
        end

        it "should fail to get Kafka schema due to not authenticated" do
          post "#{ENV['HOPSWORKS_API']}/appservice/schema",
               {
                   keyStorePwd: "-",
                   keyStore: "-",
                   topicName: "test",
                   version: 1
               }
          expect_json(errorCode: 160008)
          expect_status(401)
        end
      end

      context 'project, keystore and password but without kafka topic' do
        before :all do
          with_valid_project
          project = get_project
          with_keystore(project)
          with_keystore_pwd(project)
        end

        it "should be authenticated but should fail since topic doesn't exist" do
          keystore = get_keystore
          keystore_pwd = get_keystore_pwd
          json_data = {
              keyStorePwd: keystore_pwd,
              keyStore: keystore,
              topicName: "kafka_topic_#{random_id}",
              version: 1
          }
          json_data = json_data.to_json
          post "#{ENV['HOPSWORKS_API']}/appservice/schema", json_data # This post request authenticates with keystore and pwd to get schema
          expect_status(404)
        end
      end

      context 'project, keystore, password and kafka topic' do
        before :all do
          with_valid_project
          project = get_project
          with_kafka_topic(project.id)
          with_keystore(project)
          with_keystore_pwd(project)
        end

        it "should be authenticated and request to get topic schema should succeed" do
          topic = get_topic
          keystore = get_keystore
          keystore_pwd = get_keystore_pwd
          json_data = {
              keyStorePwd: keystore_pwd,
              keyStore: keystore,
              topicName: topic.topic_name,
              version: 1
          }
          json_data = json_data.to_json
          json = post "#{ENV['HOPSWORKS_API']}/appservice/schema", json_data # This post request authenticates with keystore and pwd to get schema
          parsed_json = JSON.parse(response.body)
          expect(parsed_json.key?("contents")).to be true
          expect(parsed_json.key?("version")).to be true
          expect_status(200)
        end
      end
    end
  end
end
