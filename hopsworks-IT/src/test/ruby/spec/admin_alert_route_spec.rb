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
  after(:all) {clean_all_test_projects(spec: "admin route")}
  describe 'Admin Route' do
    context 'without authentication' do
      before :all do
        reset_session
        @project = {}
        @project[:projectname] = "testAdminProject1"
      end
      it "should fail to get" do
        get_routes_admin
        expect_status_details(401)
      end
      it "should fail to get by receiver" do
        get_routes_by_receiver_admin(get_receiver_name(@project[:projectname], "receiver"),
                                     query: "?match=project:#{@project[:projectname]}")
        expect_status_details(401)
      end
      it "should fail to create" do
        create_routes_admin(create_route(@project))
        expect_status_details(401)
      end
      it "should fail to update" do
        update_routes_admin(create_route(@project), get_receiver_name(@project[:projectname], "receiver"),
                            query: "?match=project:#{@project[:projectname]}")
        expect_status_details(401)
      end
      it "should fail to delete" do
        delete_routes_admin(get_receiver_name(@project[:projectname], "receiver"),
                            query: "?match=project:#{@project[:projectname]}")
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
        get_routes_admin
        expect_status_details(403)
      end
      it "should fail to get by receiver" do
        get_routes_by_receiver_admin(get_receiver_name(@project[:projectname], "receiver"),
                                     query: "?match=project:#{@project[:projectname]}")
        expect_status_details(403)
      end
      it "should fail to create" do
        create_routes_admin(create_route(@project))
        expect_status_details(403)
      end
      it "should fail to update" do
        update_routes_admin(create_route(@project), get_receiver_name(@project[:projectname], "receiver"),
                            query: "?match=project:#{@project[:projectname]}")
        expect_status_details(403)
      end
      it "should fail to delete" do
        delete_routes_admin(get_receiver_name(@project[:projectname], "receiver"),
                            query: "?match=project:#{@project[:projectname]}")
        expect_status_details(403)
      end
    end
    context 'with authentication' do
      before :all do
        with_admin_session
        @project = {}
        @project[:projectname] = "testAdminProject1"
        create_random_routes_admin(@project)
        @route = create_random_route_admin(@project)
        @receiver = @route[:receiver]
      end
      it "should get" do
        get_routes_admin
        expect_status_details(200)
        expect(json_body[:count]).to be >= 3
      end
      it "should get by receiver" do
        get_routes_by_receiver_admin(@route[:receiver], query: "?match=project:#{@project[:projectname]}")
        expect_status_details(200)

        expect(json_body[:receiver]).to eq(@route[:receiver])
      end
      it "should create" do
        route = create_random_route_admin(@project)
        get_routes_by_receiver_admin(route[:receiver], query: "?match=project:#{@project[:projectname]}")
        expect_status_details(200)
        expect(json_body[:receiver]).to eq(route[:receiver])
      end
      it "should update" do
        receiver = create_receiver(@project, emailConfigs: [create_email_config])
        create_receivers_admin_checked(receiver)
        route1 = create_route(@project, receiver: receiver[:name])
        update_routes_admin(route1, @route[:receiver], query: "?match=project:#{@project[:projectname]}")
        expect_status_details(200)
        expect(json_body[:receiver]).to eq(route1[:receiver])
        check_eq_match(json_body, @project)
      end
      it "should delete" do
        delete_routes_admin(@route[:receiver], query: "?match=project:#{@project[:projectname]}")
        expect_status_details(204)
        get_routes_by_receiver_admin(@route[:receiver], query: "?match=project:#{@project[:projectname]}")
        expect_status_details(400)
        expect_json(errorCode: 390004)
      end
    end
  end
end
