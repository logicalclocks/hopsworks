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
  after(:all) {clean_all_test_projects(spec: "admin management")}
  describe 'Admin management' do
    context 'without authentication' do
      before :all do
        reset_session
      end
      it "should fail to get" do
        get_config_admin
        expect_status_details(401)
      end
      it "should fail to update global" do
        update_global_admin(get_global_admin)
        expect_status_details(401)
      end
      it "should fail to update template" do
        update_templates_admin(get_templates_admin)
        expect_status_details(401)
      end
      it "should fail to update route" do
        update_route_admin(get_route_admin)
        expect_status_details(401)
      end
      it "should fail to update inhibit" do
        update_inhibit_admin(get_inhibit_rules_admin)
        expect_status_details(401)
      end
    end
    context 'with user role' do
      before :all do
        with_valid_session
      end
      it "should fail to get" do
        get_config_admin
        expect_status_details(403)
      end
      it "should fail to update global" do
        update_global_admin(get_global_admin)
        expect_status_details(403)
      end
      it "should fail to update template" do
        update_templates_admin(get_templates_admin)
        expect_status_details(403)
      end
      it "should fail to update route" do
        update_route_admin(get_route_admin)
        expect_status_details(403)
      end
      it "should fail to update inhibit" do
        update_inhibit_admin(get_inhibit_rules_admin)
        expect_status_details(403)
      end
    end
    context 'with authentication' do
      before :all do
        with_admin_session
      end
      it "should get" do
        get_config_admin
        expect_status_details(200)
      end
      it "should update global" do
        update_global_admin(get_global_admin)
        expect_status_details(200)

        expect(json_body[:smtpSmarthost]).to eql(get_global_admin[:smtpSmarthost])
        expect(json_body[:smtpFrom]).to eql(get_global_admin[:smtpFrom])
        expect(json_body[:smtpAuthUsername]).to eql(get_global_admin[:smtpAuthUsername])
        expect(json_body[:smtpAuthIdentity]).to eql(get_global_admin[:smtpAuthIdentity])
        expect(json_body[:smtpAuthPassword]).to eql(get_global_admin[:smtpAuthPassword])
      end
      it "should update template" do
        update_templates_admin(get_templates_admin)
        expect_status_details(200)
      end
      it "should update route" do
        update_route_admin(get_route_admin)
        expect_status_details(200)
      end
      it "should update inhibit" do
        update_inhibit_admin({"postableInhibitRulesDTOs":get_inhibit_rules_admin})
        expect_status_details(200)
      end
    end
  end
end
