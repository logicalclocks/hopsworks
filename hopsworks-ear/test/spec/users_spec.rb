=begin
 This file is part of Hopsworks
 Copyright (C) 2018, Logical Clocks AB. All rights reserved

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
  describe "users" do
    describe "username_generation" do
      it 'username should only contain alfanumeric chars no matter the user email' do
        user_params = {}
        email = "is~t.his-real_#{random_id}@hopsworks.se"
        user_params[:email] = email
        register_user(user_params)

        user = User.find_by(email: email)
        expect(user[:username]).to match(/^[a-z0-9]{8}$/)
      end

      it 'should handle emails shorter than the username length' do
        user_params = {}
        email = "s#{random_id}@hopsworks.se"
        user_params[:email] = email
        register_user(user_params)

        user = User.find_by(email: email)
        expect(user[:username]).to match(/^[a-z0-9]{8}$/)
      end

      it 'should fail to register user with capital letters in the email' do
        user_params = {}
        email = "TOLOWER#{random_id}@hopsworks.se"
        user_params[:email] = email
        register_user(user_params)

        user = User.find_by(email: email)
        expect(user).to be nil
      end

      it 'should handle multiple users with similar emails' do
        user_params = {}
        email = "userusera#{random_id}@hopsworks.se"
        user_params[:email] = email
        register_user(user_params)

        user = User.find_by(email: email)
        expect(user[:username]).to match(/^[a-z0-9]{8}$/)

        email = "useruserb#{random_id}@hopsworks.se"
        user_params[:email] = email
        register_user(user_params)

        user = User.find_by(email: email)
        expect(user[:username]).to match(/^[a-z0-9]{8}$/)
      end
    end
  end
end