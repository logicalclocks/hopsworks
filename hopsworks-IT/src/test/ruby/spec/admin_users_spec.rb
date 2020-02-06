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
  after(:all) {clean_all_test_projects}
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
        expect_status(401)
        expect_json(errorCode: 200003)
      end
    end

    context "with user authentication" do
      before :all do
        with_valid_session()
      end

      it "restricts requests for admin resources from a normal user account" do
        admin_get_users()
        expect_status(403)
        expect_json(errorCode: 200014)
      end
    end

    context "with admin authentication and validated user" do
      before :all do
        with_admin_session()
      end

      let(:user) { create_validated_user() }

      it "gets the list of all users" do
        id = user[:id] 
        admin_get_users()
        expect_status(200)
        expect(json_body[:count]).to be > 0
        expect(json_body[:items].find { |i| i[:id] == id }).to_not be_nil
      end

      it "gets user by id" do
        id = user[:id]
        admin_get_user_by_id(id)
        expect_status(200)
        expect_json(id: id)
        expect_json(email: user[:email])
      end

      it "updates user's status by id" do
        id = user[:id]
        data = {status: "DEACTIVATED_ACCOUNT"}
        admin_update_user(id, data)
        expect_status(204)
        admin_get_user_by_id(id)
        expect_status(200)
        expect_json(status: 3)
      end

      it "updates user's max num projects by id" do
        id = user[:id]
        data = {maxNumProjects: 77}
        admin_update_user(id, data)
        expect_status(204)
        admin_get_user_by_id(id)
        expect_status(200)
        expect_json(maxNumProjects: 77)
      end

      it "accepts a verified user by its id" do
        id = user[:id]
        data = {status: "VERIFIED_ACCOUNT"}
        admin_update_user(id, data)
	      expect_status(204)
	      admin_accept_user(id)
	      expect_status(204)
	      admin_get_user_by_id(id)
	      expect_status(200)
	      expect_json(status: 2)
      end

      it "fails to accept a user with status different than verified" do 
        id = user[:id]
        data = {status: "NEW_MOBILE_ACCOUNT"}
        admin_update_user(id, data)
	      expect_status(204)
	      admin_accept_user(id)
	      expect_status(400)
	      expect_json(errorCode: 160046)
      end

      it "fails to accept a user with an invalid id" do
        admin_accept_user(1)
        expect_status(404)
        expect_json(errorCode: 160002)
      end

      it "rejects user" do
        id = user[:id]
        admin_reject_user(id)
        expect_status(204)
        admin_get_user_by_id(id)
        expect_status(200)
        expect_json(status: 6)
      end

      it "fails to reject a user with invalid id" do
        admin_reject_user(1)
        expect_status(404)
        expect_json(errorCode: 160002)
      end

      it "resends a confirmation email" do
        id = user[:id]
        data = {status: "NEW_MOBILE_ACCOUNT"}
        admin_update_user(id, data)
	      expect_status(204)
        admin_pend_user(id)
        expect_status(204)
      end

      it "fails to pend user with status other than new account" do
        id = user[:id]
        data = {status: "VERIFIED_ACCOUNT"}
        admin_update_user(id, data)
	      expect_status(204)
        admin_pend_user(id)
        expect_status(400)
        expect_json(errorCode: 160046)
      end

      it "fails to pend user with invalid id" do
        admin_pend_user(1)
        expect_status(404)
        expect_json(errorCode: 160002)
      end

      it "gets all user groups" do
        admin_get_user_groups()
        expect_status(200)
        expect(json_body[:count]).to be > 0
      end
    end
  end
end
