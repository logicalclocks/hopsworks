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

describe "On #{ENV['OS']}" do
  describe "Hopsworks Conf API" do
    context "not authenticated" do
      before :all do
        reset_session
      end

      it "should not be able to fetch the configuration" do
        get "#{ENV['HOPSWORKS_API']}/admin/configuration"
        expect_status_details(401)
      end
    end

    context "hops user" do
      before :all do
        with_valid_session
      end

      it "should not be able to fetch the configuration" do
        get "#{ENV['HOPSWORKS_API']}/admin/configuration"
        expect_status_details(403)
      end
    end

    context "hops admin" do
      before :all do
        with_admin_session
      end

      it "should be able to fetch the configuration" do
        get "#{ENV['HOPSWORKS_API']}/admin/configuration"
        expect_status_details(200)
        conf_dto = JSON.parse(response.body)

        expect(conf_dto['items'].length > 0).to be true

        hidden = conf_dto['items'].select {|c| c['name'].eql?("int_service_api_key")}
        expect(hidden[0]['hide']).to be true
      end

      it "should be able to set a configuration" do
        post "#{ENV['HOPSWORKS_API']}/admin/configuration/test", {
          name: "test",
          value: "test",
          visibility: "ADMIN",
          hide: true
        }

        expect_status_details(200)
        conf_dto = JSON.parse(response.body)

        expect(conf_dto['items'].length > 0).to be true

        hidden = conf_dto['items'].select {|c| c['name'].eql?("test")}
        expect(hidden[0]['value']).to eql "test"
        expect(hidden[0]['visibility']).to eql "ADMIN"
        expect(hidden[0]['hide']).to be true
      end

      it "should be able to update a configuration" do
        post "#{ENV['HOPSWORKS_API']}/admin/configuration/update", {
          name: "update",
          value: "test",
          visibility: "ADMIN",
          hide: true
        }

        post "#{ENV['HOPSWORKS_API']}/admin/configuration/update", {
          name: "update",
          value: "test",
          visibility: "USER",
          hide: false
        }

        expect_status_details(200)
        conf_dto = JSON.parse(response.body)

        expect(conf_dto['items'].length > 0).to be true

        hidden = conf_dto['items'].select {|c| c['name'].eql?("update")}
        expect(hidden[0]['value']).to eql "test"
        expect(hidden[0]['visibility']).to eql "USER"
        expect(hidden[0]['hide']).to be false
      end
    end
  end
end