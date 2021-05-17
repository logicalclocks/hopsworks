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
  after(:all) {clean_all_test_projects(spec: "receiver")}
  describe 'Receiver' do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "should fail to get" do
        get_receivers(@project)
        expect_status_details(401)
      end
      it "should fail to get by name" do
        get_receivers_by_name(@project, "receiverName")
        expect_status_details(401)
      end
      it "should fail to create" do
        create_receivers(@project, create_receiver(@project, emailConfigs: [create_email_config]))
        expect_status_details(401)
      end
      it "should fail to update" do
        update_receivers(@project, get_receiver_name(@project[:projectname], "receiver"),
                         create_receiver(@project, emailConfigs: [create_email_config]))
        expect_status_details(401)
      end
      it "should fail to delete" do
        delete_receivers(@project, get_receiver_name(@project[:projectname], "receiver"))
        expect_status_details(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_project
        @receiver1 = create_receiver(@project, emailConfigs: [create_email_config])
        create_receivers_checked(@project, @receiver1)
        @project1 = @project
        reset_session
        with_valid_project
        create_random_receiver(@project)
        @receiver = create_receiver(@project, emailConfigs: [create_email_config])
        create_receivers_checked(@project, @receiver)
      end
      it "should get" do
        get_receivers(@project)
        expect_status_details(200)
        expect(json_body[:count]).to be >= 4
      end
      it "should get by name" do
        get_receivers_by_name(@project, @receiver[:name])
        expect_status_details(200)
        check_eq_receiver_email_config(json_body, @receiver)
      end
      it "should fail to get by name of a different project" do
        get_receivers_by_name(@project, @receiver1[:name])
        expect_status_details(403)
        expect_json(errorCode: 390010)
      end
      it "should create" do
        receiver = create_receiver(@project, emailConfigs: [create_email_config])
        create_receivers_checked(@project, receiver)

        check_backup_contains_receiver(receiver)
        get_receivers_by_name(@project, receiver[:name])
        expect_status_details(200)
        check_eq_receiver_email_config(json_body, receiver)
      end
      it "should fix receiver name not starting with project name" do
        receiver = create_receiver(@project, name: "project1__receiver", emailConfigs: [create_email_config])
        create_receivers_checked(@project, create_receiver(@project, name: "project1__receiver", emailConfigs:
          [create_email_config]))

        receiver[:name] = "#{@project[:projectname]}__#{receiver[:name]}" #fix name
        check_backup_contains_receiver(receiver)
        get_receivers_by_name(@project, receiver[:name])
        expect_status_details(200)
        check_eq_receiver_email_config(json_body, receiver)
      end
      it "should update" do
        receiver = create_receiver(@project, emailConfigs: [create_email_config])
        update_receivers(@project, @receiver[:name], receiver)
        expect_status_details(200)
        check_eq_receiver_email_config(json_body, receiver)
        check_backup_contains_receiver(receiver)
        @receiver = receiver
      end
      it "should fail to update another projects receiver" do
        receiver = create_receiver(@project, emailConfigs: [create_email_config])
        update_receivers(@project, @receiver1[:name], receiver)
        expect_status_details(403)
        expect_json(errorCode: 390010)
      end
      it "should delete" do
        delete_receivers(@project, @receiver[:name])
        expect_status_details(204)
        check_receiver_deleted_from_backup(@receiver)
        get_receivers_by_name(@project, @receiver[:name])
        expect_json(errorCode: 390003)
      end
      it "should not delete another projects receiver" do
        delete_receivers(@project, @receiver1[:name])
        expect_status_details(204)

        with_admin_session
        get_receivers_by_name_admin(@receiver1[:name])
        expect_status_details(200)
        check_eq_receiver_email_config(json_body, @receiver1)
      end
    end
  end
end
