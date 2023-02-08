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
  describe "Audit" do
    context 'user login' do
      it 'should add row to table' do
        newUser = create_user
        create_session(newUser[:email], "Pass123")
        row = getLatestUserLogin(newUser[:uid], "LOGIN")
        expect(row[:outcome]).to eq("SUCCESS")
      end
    end
    context 'role ' do
      it 'should add row to table' do
        newUser = create_validated_user
        adminUser = with_admin_session_return_user
        admin_accept_user(newUser[:uid], user = {})
        row = getLatestAccountAudit(adminUser[:uid], newUser[:uid], "ACTIVATED ACCOUNT")
        expect(row[:outcome]).to eq("SUCCESS")
        row = getLatestRoleAudit(adminUser[:uid], newUser[:uid], "ADDED ROLE")
        expect(row[:outcome]).to eq("SUCCESS")
      end
    end
  end
  describe "Log" do
    context 'user login' do
      it 'should add row to log file' do
        newUser = create_user
        create_session(newUser[:email], "Pass123")

        testLog("login", "200", email: newUser[:email])
      end
    end
    context 'role ' do
      it 'should add row to log file' do
        newUser = create_validated_user
        adminUser = with_admin_session_return_user
        admin_accept_user(newUser[:uid], user = {})

        testLog("acceptUser", "200", email: adminUser[:email])
      end
    end
  end
end