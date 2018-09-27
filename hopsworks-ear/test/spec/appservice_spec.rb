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

        it "should fail to get Kafka schema" do
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

        it "should be authenticated for getting the Kafka schema" do
          project = get_project
          download_cert_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/downloadCert"
          data = 'password=Pass123'
          headers = {"Content-Type" => 'application/x-www-form-urlencoded'}
          post download_cert_endpoint, data, headers # this POST request will trigger a materialization of keystore and pwd to /srv/hops/certs-dir/transient/
          key_pwd_file_pattern = "/srv/hops/certs-dir/transient/*.key"
          expect(!Dir.glob(key_pwd_file_pattern).empty?)
          key_pwd_file = Dir.glob(key_pwd_file_pattern)[0]
          key_pwd = File.read(key_pwd_file)
          user_key_cert_pwd= UserCerts.find_by(projectname:project.projectname)
          json_data = {
              keyStorePwd: key_pwd,
              keyStore: Base64.encode64(user_key_cert_pwd.user_key),
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
