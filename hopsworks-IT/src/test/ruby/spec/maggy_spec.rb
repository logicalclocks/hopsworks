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
describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects(spec: "maggy")}
  describe "maggy" do
    describe "maggy driver registration endpoint" do
      context "not logged in" do
        before(:all) do
          reset_session
        end
        it "should not be able to register a maggy driver" do
          post "#{ENV['HOPSWORKS_API']}/maggy/drivers", {appId: "42", hostIp: "127.0.3.5", port: 12345,
                                                         secret: "magster"}
          expect_status(401)
        end
      end
        context "logged in" do
        before(:all) do
          with_valid_session
        end

        it "should be able to register a maggy driver" do
          post "#{ENV['HOPSWORKS_API']}/maggy/drivers", {appId: "42", hostIp: "127.0.3.5", port: 12345,
                                                         secret: "magster"}
          expect_status(204)
        end

        it "should be able to get the latest registered driver" do
          # Sleep since we sort entities by timestamp, otherwise in tests they get created
          # in the same millisecond
          sleep(1)
          post "#{ENV['HOPSWORKS_API']}/maggy/drivers", {appId: "42", hostIp: "127.0.3.6", port: 54321,
                                                         secret: "magster"}
          expect_status(204)
          get "#{ENV['HOPSWORKS_API']}/maggy/drivers/42"
          expect_status(200)
          parsed_json = JSON.parse(response.body)
          expect(parsed_json["appId"] == "42").to be true
          expect(parsed_json["hostIp"] == "127.0.3.6").to be true
          expect(parsed_json["port"] == 54321).to be true
          expect(parsed_json["secret"] == "magster").to be true
        end

        it "should be able to delete all registered drivers for an appId" do
          delete "#{ENV['HOPSWORKS_API']}/maggy/drivers/42"
          expect_status(204)
          get "#{ENV['HOPSWORKS_API']}/maggy/drivers/42"
          expect_status(404)
        end
      end
    end
  end
end
