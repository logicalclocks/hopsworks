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

    describe "#create" do

      context 'without valid keystore and password' do
        before :all do
          with_valid_project
        end

        it "should fail to get Kafka schema without keystore and pwd" do
          post "#{ENV['HOPSWORKS_API']}/appservice/schema",
               {
                   keyStorePwd: "-",
                   keyStore: "-",
                   topicName: "test",
                   version: 1
               }
          expect_json(errorMsg: "Could not authenticate user")
          expect_status(401)
        end
      end

      context 'with keystore and password' do
        before :all do
          with_valid_project
        end

        it "should be authenticated for getting the Kafka schema with keystore and pwd" do
          project = get_project
          username = get_current_username
          download_user_cert(project.id) # need to download the certs to /srv/hops/certs-dir/transient because the .key file is encrypted in NDB
          user_key_path = get_user_key_path(project.projectname, username)
          expect(File.exist?(user_key_path)).to be true
          key_pwd = File.read(user_key_path)
          key_store = get_user_keystore(project.projectname)
          json_data = {
              keyStorePwd: key_pwd,
              keyStore: key_store,
              topicName: "test",
              version: 1
          }
          json_data = json_data.to_json
          post "#{ENV['HOPSWORKS_API']}/appservice/schema", json_data # This post request authenticates with keystore and pwd to get schema
          expect_status(200)
        end
      end
    end
  end
end
