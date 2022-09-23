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
  after(:all) {clean_all_test_projects(spec: "route")}
  describe 'Route' do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "should fail to get" do
        get_routes(@project)
        expect_status_details(401)
      end
      it "should fail to get by receiver" do
        get_routes_by_receiver(@project, "receiverName")
        expect_status_details(401)
      end
      it "should fail to create" do
        create_routes(@project, create_route(@project))
        expect_status_details(401)
      end
      it "should fail to update" do
        update_routes(@project, create_route(@project), get_receiver_name(@project[:projectname], "receiver"))
        expect_status_details(401)
      end
      it "should fail to delete" do
        delete_routes(@project, 1)
        expect_status_details(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_project
        @route1 = create_random_route(@project)
        @receiver1 = @route1[:receiver]
        @project1 = @project
        reset_session
        with_valid_project
        create_random_routes(@project)
        @route = create_random_route(@project)
        @receiver = @route[:receiver]
      end
      it "should get" do
        get_routes(@project)
        expect_status_details(200)
        expect(json_body[:count]).to be >= 4
      end
      it "should get by receiver" do
        get_routes_by_receiver(@project, @receiver, query: "?match=project:#{@project[:projectname]}&match=type:project-alert")
        expect_status_details(200)
        expect(json_body[:receiver]).to eq(@receiver)
      end
      it "should fail to get by receiver of a different project" do
        get_routes_by_receiver(@project, @receiver1, query: "?match=project:#{@project[:projectname]}&match=type:project-alert")
        expect_status_details(400)
        expect_json(errorCode: 390004)
      end
      it "should fail to get by receiver with match for different project" do
        get_routes_by_receiver(@project, @receiver, query: "?match=project:#{@project1[:projectname]}&match=type:project-alert")
        expect_status_details(400)
        expect_json(errorCode: 390004)
      end
      it "should create" do
        receiver = create_receiver(@project, emailConfigs: [create_email_config])
        create_receivers_checked(@project, receiver)
        route = create_route(@project, receiver: receiver[:name])
        create_routes(@project, route)
        expect_status_details(201)
        check_backup_contains_route(json_body)
        get_routes_by_receiver(@project, route[:receiver], query: "?match=project:#{@project[:projectname]}&match=type:project-alert")
        expect_status_details(200)
        expect(json_body[:receiver]).to eq(route[:receiver])
      end
      it "should create with matchRe" do
        receiver = create_receiver(@project, emailConfigs: [create_email_config])
        create_receivers_checked(@project, receiver)
        route = create_route(@project, receiver: receiver[:name])
        route[:match] = nil
        route[:matchRe] = create_match(@project[:projectname])
        create_routes(@project, route)
        expect_status_details(201)
        check_backup_contains_route(json_body)

        get_routes_by_receiver(@project, route[:receiver], query: "?matchRe=project:#{@project[:projectname]}&matchRe=type:project-alert")
        expect_status_details(200)
        expect(json_body[:receiver]).to eq(route[:receiver])

        get_routes(@project)
        expect_status_details(200)
        newRoute = json_body[:items].detect { |r| r[:receiver] == route[:receiver] }
        expect(newRoute[:matchRe]).to eq({:project=>@project[:projectname], :type=>"project-alert"})
      end
      it "should fail to create with receiver name that does not exist" do
        route = create_route(@project)
        create_routes(@project, route)
        expect_status_details(400)
        expect_json(errorCode: 390003)
      end
      it "should fail to create with receiver name not starting with project name" do
        create_routes(@project, create_route(@project, receiver: get_receiver_name("project1", "receiver")))
        expect_status_details(403)
        expect_json(errorCode: 390010)
      end
      it "should fix project match" do
        receiver = create_receiver(@project, emailConfigs: [create_email_config])
        create_receivers_checked(@project, receiver)
        route = create_route(@project, receiver: receiver[:name])
        route[:match] = create_match("project1")
        create_routes(@project, route)
        expect_status_details(201)
        get_routes_by_receiver(@project, route[:receiver], query: "?match=project:#{@project[:projectname]}&match=type:project-alert")
        check_eq_match(json_body, @project)
      end
      it "should update" do
        receiver = create_receiver(@project, emailConfigs: [create_email_config])
        create_receivers_checked(@project, receiver)
        route = create_route(@project, receiver: receiver[:name])
        update_routes(@project, route, @receiver, query: "?match=project:#{@project[:projectname]}&match=type:project-alert")
        expect_status_details(200)
        expect(json_body[:receiver]).to eq(route[:receiver])
        @receiver = route[:receiver]
        check_eq_match(json_body, @project)
        check_backup_contains_route(json_body)
      end
      it "should update with matchRe" do
        receiver = create_receiver(@project, emailConfigs: [create_email_config])
        create_receivers_checked(@project, receiver)
        route = create_route(@project, receiver: receiver[:name])
        route[:match] = nil
        route[:matchRe] = create_match(@project[:projectname])
        create_routes(@project, route)
        expect_status_details(201)

        receiver1 = create_receiver(@project, emailConfigs: [create_email_config])
        create_receivers_checked(@project, receiver1)
        route1 = create_route(@project, receiver: receiver1[:name])
        update_routes(@project, route1, receiver[:name], query:
          "?matchRe=project:#{@project[:projectname]}&matchRe=type:project-alert")
        expect_status_details(200)
        expect(json_body[:receiver]).to eq(route1[:receiver])
        check_eq_match(json_body, @project)
        check_backup_contains_route(json_body)
      end
      it "should fail to update with receiver for another project" do
        route = create_route(@project, receiver: @receiver1)
        update_routes(@project, route, @receiver, query: "?match=project:#{@project[:projectname]}&match=type:project-alert")
        expect_status_details(400)
        expect_json(errorCode: 390004)
      end
      it "should fail to update another project's route" do
        route = create_route(@project, receiver: @receiver)
        update_routes(@project, route, @receiver1, query: "?match=project:#{@project[:projectname]}&match=type:project-alert")
        expect_status_details(403)
        expect_json(errorCode: 390010)
      end
      it "should fail to update if match is for another project" do
        receiver = create_receiver(@project, emailConfigs: [create_email_config])
        create_receivers_checked(@project, receiver)
        route = create_route(@project, receiver: receiver[:name])
        update_routes(@project, route, @receiver, query: "?match=project:project1")
        expect_status_details(403)
        expect_json(errorCode: 390010)
      end
      it "should delete" do
        delete_routes(@project, @receiver, query: "?match=project:#{@project[:projectname]}&match=type:project-alert")
        expect_status_details(204)
        check_route_deleted_from_backup(@route)
        get_routes_by_receiver(@project, @receiver, query: "?match=project:#{@project[:projectname]}&match=type:project-alert")
        expect_status_details(400)
        expect_json(errorCode: 390004)
      end
      it "should not delete route of another project" do
        delete_routes(@project, @receiver1, query: "?match=project:#{@project1[:projectname]}&match=type:project-alert")
        expect_status_details(204)

        with_admin_session
        get_routes_by_receiver_admin(@receiver1, query: "?match=project:#{@project1[:projectname]}&match=type:project-alert")
        expect_status_details(200)
        expect(json_body[:receiver]).to eq(@receiver1)
      end
    end
  end
end
