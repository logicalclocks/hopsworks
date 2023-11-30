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
  after(:all) {clean_all_test_projects(spec: "admin_users")}
  describe "Admin user ops" do
    after :all do
      reset_session
    end

    context "without authentication" do
      before :all do
        reset_session
      end
      it "restricts requests for admin resources from non-admin accounts" do
        admin_get_users()
        expect_status_details(401, error_code: 200003)
      end
    end

    context "with user authentication" do
      before :all do
        with_valid_session()
      end

      it "restricts requests for admin resources from a normal user account" do
        admin_get_users()
        expect_status_details(403, error_code: 200014)
      end
    end

    context "with admin authentication and validated user" do
      before :all do
        with_admin_session()
        @key = create_api_key("admin_user#{random_id_len(4)}", %w(ADMINISTER_USERS))
        @key_register = create_api_key("admin_user#{random_id_len(4)}", %w(ADMINISTER_USERS_REGISTER))
      end

      let(:user) { create_validated_user() }

      it "gets the list of all users" do
        id = user[:uid] 
        admin_get_users()
        expect_status_details(200)
        expect(json_body[:count]).to be > 0
        expect(json_body[:items].find { |i| i[:id] == id }).to_not be_nil
      end

      it "gets user by id" do
        id = user[:uid]
        admin_get_user_by_id(id)
        expect_status_details(200)
        expect_json(id: id)
        expect_json(email: user[:email])
      end

      it "updates user's status by id" do
        id = user[:uid]
        data = {status: "DEACTIVATED_ACCOUNT"}
        admin_update_user(id, data)
        expect_status_details(200)
        admin_get_user_by_id(id)
        expect_status_details(200)
        expect_json(status: 3)
      end

      it "should add user to kube config map when user's status changes to activated" do
        if !kserve_installed
          skip "This test only runs with KServe installed"
        end
        id = user[:uid]
        username = user[:username]

        admin_update_user(id, {status: "DEACTIVATED_ACCOUNT"})
        expect_status_details(200)
        cm = get_users_kube_config_map()
        expect(cm).not_to be_empty
        if !cm["data"].nil? # data might not exist
          expect(cm["data"]).not_to include(username)
        end

        admin_update_user(id, {status: "ACTIVATED_ACCOUNT", })
        cm = get_users_kube_config_map()
        expect(cm).not_to be_empty
        expect(cm).to include("data")
        expect(cm["data"]).to include(username)
      end

      it "should update user role in kube config map when changing roles of a user" do
        if !kserve_installed
          skip "This test only runs with KServe installed"
        end
        newUser = create_user_with_role("HOPS_USER")
        admin_update_user(newUser.uid, {status: "ACTIVATED_ACCOUNT"})
        expect_status_details(200)

        cm = get_users_kube_config_map()
        expect(cm).not_to be_empty
        expect(cm).to include("data")
        expect(cm["data"]).to include(newUser.username)
        expect(cm["data"][newUser.username]).to eq "HOPS_USER"

        add_user_role(newUser.uid, "HOPS_ADMIN")
        cm = get_users_kube_config_map()
        expect(cm).not_to be_empty
        expect(cm).to include("data")
        expect(cm["data"]).to include(newUser.username)
        expect(cm["data"][newUser.username]).to eq "HOPS_USER,HOPS_ADMIN"

        remove_user_role(newUser.uid, "HOPS_USER")
        cm = get_users_kube_config_map()
        expect(cm).not_to be_empty
        expect(cm).to include("data")
        expect(cm["data"]).to include(newUser.username)
        expect(cm["data"][newUser.username]).to eq "HOPS_ADMIN"
      end

      it "should create serving api key when user's status changes to activated" do
        if !kserve_installed
          skip "This test only runs with KServe installed"
        end
        id = user[:uid]
        username = user[:username]
        uid = user[:uid]
        email = user[:email]

        with_valid_project
        add_member_to_project(@project, email, "Data owner")

        admin_update_user(id, {status: "DEACTIVATED_ACCOUNT"})
        expect_status_details(200)
        secret = get_api_key_kube_hops_serving_secret(username, uid)
        expect(secret).to be_nil

        admin_update_user(id, {status: "ACTIVATED_ACCOUNT"})
        secret = get_api_key_kube_hops_serving_secret(username, uid)
        expect(secret).not_to be_nil
        expect(secret).to include("data")
        expect(secret["data"]).to include("apiKey")
        secret = get_api_key_kube_project_serving_secret(@project[:projectname], username)
        expect(secret).not_to be_nil
        expect(secret).to include("data")
        expect(secret["data"]).to include("apiKey")
      end

      it "should remove user from kube config map when user's status changes to other than activated" do
        if !kserve_installed
          skip "This test only runs with KServe installed"
        end
        id = user[:uid]
        username = user[:username]

        admin_update_user(id, {status: "ACTIVATED_ACCOUNT"})
        expect_status_details(200)
        cm = get_users_kube_config_map()
        expect(cm).not_to be_empty
        expect(cm).to include("data")
        expect(cm["data"]).to include(username)

        admin_update_user(id, {status: "DEACTIVATED_ACCOUNT"})
        cm = get_users_kube_config_map()
        expect(cm).not_to be_empty
        expect(cm).to include("data")
        expect(cm["data"]).not_to include(username)
      end

      it "should delete serving api key when user's status changes to other than activated" do
        if !kserve_installed
          skip "This test only runs with KServe installed"
        end
        id = user[:uid]
        username = user[:username]
        uid = user[:uid]

        admin_update_user(id, {status: "ACTIVATED_ACCOUNT"})
        expect_status_details(200)
        secret = get_api_key_kube_hops_serving_secret(username, uid)
        expect(secret).not_to be_nil
        expect(secret).to include("data")
        expect(secret["data"]).to include("apiKey")

        admin_update_user(id, {status: "DEACTIVATED_ACCOUNT"})
        secret = get_api_key_kube_hops_serving_secret(username, uid)
        expect(secret).to be_nil
      end

      it "updates user's max num projects by id" do
        id = user[:uid]
        data = {maxNumProjects: 77}
        admin_update_user(id, data)
        expect_status_details(200)
        admin_get_user_by_id(id)
        expect_status_details(200)
        expect_json(maxNumProjects: 77)
      end

      it "accepts a verified user by its id" do
        id = user[:uid]
        data = {status: "VERIFIED_ACCOUNT"}
        admin_update_user(id, data)
        expect_status_details(200)
	      admin_accept_user(id)
        expect_status_details(200)
	      admin_get_user_by_id(id)
        expect_status_details(200)
	      expect_json(status: 2)
      end

      it "accept a user with status different than verified" do
        id = user[:uid]
        data = {status: "NEW_MOBILE_ACCOUNT"}
        admin_update_user(id, data)
        expect_status_details(200)
	      admin_accept_user(id)
        expect_status_details(200)
      end

      it "fails to accept a user with an invalid id" do
        admin_accept_user(1)
        expect_status_details(404)
        expect_json(errorCode: 160002)
      end

      it "rejects user" do
        id = user[:uid]
        admin_reject_user(id)
        expect_status_details(200)
        admin_get_user_by_id(id)
        expect_status_details(200)
        expect_json(status: 6)
      end

      it "fails to reject a user with invalid id" do
        admin_reject_user(1)
        expect_status_details(404)
        expect_json(errorCode: 160002)
      end

      it "resends a confirmation email" do
        id = user[:uid]
        data = {status: "NEW_MOBILE_ACCOUNT"}
        admin_update_user(id, data)
        expect_status_details(200)
        admin_pend_user(id)
        expect_status_details(200)
      end

      it "fails to pend user with status other than new account" do
        id = user[:uid]
        data = {status: "VERIFIED_ACCOUNT"}
        admin_update_user(id, data)
        expect_status_details(200)
        admin_pend_user(id)
        expect_status_details(400, error_code: 160046)
      end

      it "fails to pend user with invalid id" do
        admin_pend_user(1)
        expect_status_details(404, error_code: 160002)
      end

      it "gets all user groups" do
        admin_get_user_groups()
        expect_status_details(200)
        expect(json_body[:count]).to be > 0
      end

      it "should register new user" do
        register_user_as_admin("#{random_id}@email.com", "name", "last", password: "Pass123", maxNumProjects: "5",
                               status: "ACTIVATED_ACCOUNT")
        expect_status_details(201)
        expect(json_body[:maxNumProjects]).to be == 5
        expect(json_body[:status]).to be == 2
        expect(json_body[:password]).to be_nil
      end
      it "should register new user with capital letters (should lowercase before saving)" do
        email = "#{random_id}@EMAIL.com"
        register_user_as_admin(email, "name", "last", password: "Pass123", maxNumProjects: "5",
                               status: "ACTIVATED_ACCOUNT")
        expect_status_details(201)
        expect(json_body[:maxNumProjects]).to be == 5
        expect(json_body[:status]).to be == 2
        expect(json_body[:password]).to be_nil
        expect(json_body[:email]).to eq(email.downcase)
      end
      it "should add new activated user to kube config map" do
        if !kserve_installed
          skip "This test only runs with KServe installed"
        end
        register_user_as_admin("#{random_id}@email.com", "name", "last", password: "Pass123", maxNumProjects: "5",
                               status: "ACTIVATED_ACCOUNT")
        expect_status_details(201)
        username = json_body[:username]
        cm = get_users_kube_config_map()
        expect(cm).not_to be_empty
        expect(cm).to include("data")
        expect(cm["data"]).to include(username)
        roles = get_roles(json_body[:email])
        expect(cm["data"][username]).to eq roles.join(",")
      end
      it "should create serving api key for a new activated user" do
        if !kserve_installed
          skip "This test only runs with KServe installed"
        end
        register_user_as_admin("#{random_id}@email.com", "name", "last", password: "Pass123", maxNumProjects: "5",
                                       status: "ACTIVATED_ACCOUNT")
        expect_status_details(201)
        username = json_body[:username]
        uid = json_body[:uid]
        secret = get_api_key_kube_hops_serving_secret(username, uid)
        expect(secret).not_to be_nil
        expect(secret).to include("data")
        expect(secret["data"]).to include("apiKey")
      end
      it "should register new user with no password" do
        register_user_as_admin("#{random_id}@email.com", "name", "last", maxNumProjects: "5", status: "ACTIVATED_ACCOUNT")
        expect_status_details(201)
        expect(json_body[:maxNumProjects]).to be == 5
        expect(json_body[:status]).to be == 2
        expect(json_body[:password]).not_to be_nil
      end
      it "should register new user with no number of projects" do
        register_user_as_admin("#{random_id}@email.com", "name", "last", status: "ACTIVATED_ACCOUNT")
        expect_status_details(201)
        expect(json_body[:maxNumProjects]).to be == 10
        expect(json_body[:status]).to be == 2
        expect(json_body[:password]).not_to be_nil
      end
      it "should register new user with no status" do
        register_user_as_admin("#{random_id}@email.com", "name", "last")
        expect_status_details(201)
        expect(json_body[:maxNumProjects]).to be == 10
        expect(json_body[:status]).to be == 7
        expect(json_body[:password]).not_to be_nil
      end
      it "should not add new non-activated user without status to kube config map" do
        if !kserve_installed
          skip "This test only runs with KServe installed"
        end
        register_user_as_admin("#{random_id}@email.com", "name", "last")
        expect_status_details(201)
        username = json_body[:username]
        cm = get_users_kube_config_map()
        expect(cm).not_to be_empty
        expect(cm).to include("data")
        expect(cm["data"]).not_to include(username)
      end
      it "should not create serving api key for a non-activated new user" do
        if !kserve_installed
          skip "This test only runs with KServe installed"
        end
        register_user_as_admin("#{random_id}@email.com", "name", "last")
        expect_status_details(201)
        username = json_body[:username]
        uid = json_body[:uid]
        secret = get_api_key_kube_hops_serving_secret(username, uid)
        expect(secret).to be_nil
      end

      it "should fail to register new user with no name" do
        register_user_as_admin("#{random_id}@email.com", "", "")
        expect_status_details(400)
      end
      it "should delete user" do
        newUser = create_validated_user()
        admin_delete_user(newUser[:uid])
        expect_status_details(204)
      end
      it "should remove deleted user from kube config map" do
        if !kserve_installed
          skip "This test only runs with KServe installed"
        end
        register_user_as_admin("#{random_id}@email.com", "name", "last", maxNumProjects: "5", status: "ACTIVATED_ACCOUNT")
        newUser = json_body
        username = newUser[:username]
        cm = get_users_kube_config_map()
        expect(cm).not_to be_empty
        expect(cm).to include("data")
        expect(cm["data"]).to include(username)

        admin_delete_user(newUser[:id])
        expect_status_details(204)
        cm = get_users_kube_config_map()
        expect(cm).not_to be_empty
        expect(cm).to include("data")
        expect(cm["data"]).not_to include(username)
      end

      it "should remove serving api key when deleting a user" do
        if !kserve_installed
          skip "This test only runs with KServe installed"
        end
        register_user_as_admin("#{random_id}@email.com", "name", "last", maxNumProjects: "5", status: "ACTIVATED_ACCOUNT")
        newUser = json_body
        username = newUser[:username]
        uid = newUser[:uid]
        secret = get_api_key_kube_hops_serving_secret(username, uid)
        expect(secret).not_to be_nil
        admin_delete_user(newUser[:id])
        expect_status_details(204)
        secret = get_api_key_kube_hops_serving_secret(username, uid)
        expect(secret).to be_nil
      end

      it "should fail to delete user with project" do
        newUser = create_user_with_role("HOPS_USER")
        create_session(newUser[:email], "Pass123")
        with_valid_project
        with_admin_session
        admin_delete_user(newUser[:uid])
        expect_status_details(400)
      end
      it "should delete a user with project after deleting project" do
        newUser = create_user_with_role("HOPS_USER")
        create_session(newUser[:email], "Pass123")
        with_valid_project
        delete_project(@project)
        with_admin_session
        admin_delete_user(newUser[:uid])
        expect_status_details(204)
      end
      it "should fail to delete an admin that is an initiator of an account audit" do
        adminUser = create_user_with_role("HOPS_ADMIN")

        newUser = create_unapproved_user
        create_session(adminUser[:email], "Pass123")

        admin_update_user(newUser[:uid], {status: "ACTIVATED_ACCOUNT"})

        with_admin_session
        admin_delete_user(adminUser[:uid])
        expect_status_details(400)
      end
      it "should reset password" do
        newUser = create_user_with_role("HOPS_USER")
        admin_reset_password(newUser[:uid])
        expect_status_details(200)
        password = json_body[:password]
        reset_session
        try_login(newUser, password)
        expect_status_details(200)
      end
      it "should register new user with api key" do
        reset_session
        set_api_key_to_header(@key)
        register_user_as_admin("#{random_id}@email.com", "name", "last")
        expect_status_details(201)
        expect(json_body[:maxNumProjects]).to be == 10
        expect(json_body[:status]).to be == 7
        expect(json_body[:password]).not_to be_nil
      end
      it "should register new user with api key scope register" do
        reset_session
        set_api_key_to_header(@key_register)
        register_user_as_admin("#{random_id}@email.com", "name", "last")
        expect_status_details(201)
        expect(json_body[:maxNumProjects]).to be == 10
        expect(json_body[:status]).to be == 7
        expect(json_body[:password]).not_to be_nil
      end
    end
  end
end
