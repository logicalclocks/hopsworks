=begin
 This file is part of Hopsworks
 Copyright (C) 2021, Logical Clocks AB. All rights reserved

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
  after(:all) {clean_all_test_projects(spec: "admin_alert")}
  describe 'Admin alert' do
    context 'without authentication' do
      before :all do
        reset_session
      end
      it "should fail to get" do
        get_alerts_admin
        expect_status_details(401)
      end
      it "should fail to get groups" do
        get_alert_groups_admin
        expect_status_details(401)
      end
      it "should fail to create" do
        create_random_alerts_admin
        expect_status_details(401)
      end
    end
    context 'with user role' do
      before :all do
        with_valid_session
      end
      it "should fail to get" do
        get_alerts_admin
        expect_status_details(403)
      end
      it "should fail to get groups" do
        get_alert_groups_admin
        expect_status_details(403)
      end
      it "should fail to create" do
        create_random_alerts_admin
        expect_status_details(403)
      end
    end
    context 'with authentication' do
      before :all do
        with_admin_session
        create_random_alerts_admin
      end
      it "should get" do
        get_alerts_admin
        expect_status_details(200)
        expect(json_body[:count]).to be >= 3
      end
      it "should get groups" do
        get_alert_groups_admin
        expect_status_details(200)
        expect(json_body[:count]).to be > 0
      end
      it "should create" do
        create_random_alerts_admin
        expect_status_details(200)

        get_alerts_admin
        expect_status_details(200)
        expect(json_body[:count]).to be >= 6
      end
    end
  end
end

