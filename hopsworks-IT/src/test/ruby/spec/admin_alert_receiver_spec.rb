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
  after(:all) {clean_all_test_projects(spec: "admin receiver")}
  describe 'Admin Receiver' do
    context 'without authentication' do
      before :all do
        reset_session
        @project = {}
        @project[:projectname] = "testAdminProject1"
      end
      it "should fail to get" do
        get_receivers_admin
        expect_status_details(401)
      end
      it "should fail to get by name" do
        get_receivers_by_name_admin("receiverName")
        expect_status_details(401)
      end
      it "should fail to create" do
        create_receivers_admin(create_receiver(@project, emailConfigs: [create_email_config]))
        expect_status_details(401)
      end
      it "should fail to update" do
        update_receivers_admin(get_receiver_name(@project[:projectname], "receiver"),
                         create_receiver(@project, emailConfigs: [create_email_config]))
        expect_status_details(401)
      end
      it "should fail to delete" do
        delete_receivers_admin(get_receiver_name(@project[:projectname], "receiver"))
        expect_status_details(401)
      end
    end
    context 'with user role' do
      before :all do
        with_valid_session
        @project = {}
        @project[:projectname] = "testAdminProject1"
      end
      it "should fail to get" do
        get_receivers_admin
        expect_status_details(403)
      end
      it "should fail to get by name" do
        get_receivers_by_name_admin("receiverName")
        expect_status_details(403)
      end
      it "should fail to create" do
        create_receivers_admin(create_receiver(@project, emailConfigs: [create_email_config]))
        expect_status_details(403)
      end
      it "should fail to update" do
        update_receivers_admin(get_receiver_name(@project[:projectname], "receiver"),
                               create_receiver(@project, emailConfigs: [create_email_config]))
        expect_status_details(403)
      end
      it "should fail to delete" do
        delete_receivers_admin(get_receiver_name(@project[:projectname], "receiver"))
        expect_status_details(403)
      end
    end
    context 'with authentication' do
      before :all do
        with_admin_session
        @project = {}
        @project[:projectname] = "testAdminProject1"
        create_random_receiver_admin(@project)
        @receiver = create_receiver(@project, emailConfigs: [create_email_config])
        create_receivers_admin(@receiver)
      end
      it "should get" do
        get_receivers_admin
        expect_status_details(200)
        expect(json_body[:count]).to be >= 4
      end
      it "should get by name" do
        get_receivers_by_name_admin(@receiver[:name])
        expect_status_details(200)
        check_eq_receiver_email_config(json_body, @receiver)
      end
      it "should create" do
        receiver = create_receiver(@project, emailConfigs: [create_email_config])
        create_receivers_admin(receiver)
        expect_status_details(201)

        get_receivers_by_name_admin(receiver[:name])
        expect_status_details(200)
        check_eq_receiver_email_config(json_body, receiver)
      end
      it "should update" do
        receiver = create_receiver(@project, emailConfigs: [create_email_config])
        update_receivers_admin(@receiver[:name], receiver)
        expect_status_details(200)
        check_eq_receiver_email_config(json_body, receiver)
        @receiver = receiver
      end
      it "should delete" do
        delete_receivers_admin(@receiver[:name])
        expect_status_details(204)

        get_receivers_by_name_admin(@receiver[:name])
        expect_json(errorCode: 390003)
      end
    end
  end
end
