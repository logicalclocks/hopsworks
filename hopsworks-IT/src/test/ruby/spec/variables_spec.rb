=begin
 This file is part of Hopsworks
 Copyright (C) 2020, Logical Clocks AB. All rights reserved

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
  describe 'admin variables' do
    before :all do
      with_admin_session
    end

    it 'should succeed to fetch the value of a variable' do
      result = get "#{ENV['HOPSWORKS_API']}/variables/hopsworks_user"
      expect_status(200)
      expect(JSON.parse(result)['successMessage']).to eql("glassfish")
    end

    it 'should succeed to fetch a variable with admin visibility' do
      get "#{ENV['HOPSWORKS_API']}/variables/hopsworks_master_password"
      expect_status(200)
    end

    it 'should receive a 404 if the variable does not exists' do
      get "#{ENV['HOPSWORKS_API']}/variables/doesnotexists"
      expect_status(404)
    end
  end

  describe 'with user session' do
    before :all do
      with_valid_session
    end

    it 'should be able to fetch a variable with notauthenticated visibility' do
      get "#{ENV['HOPSWORKS_API']}/variables/first_time_login"
      expect_status(200)
    end

    it 'should be able to fetch a variable with user visibility' do
      get "#{ENV['HOPSWORKS_API']}/variables/hopsworks_enterprise"
      expect_status(200)
    end

    it 'should fail to fetch a variable with admin visibility' do
      get "#{ENV['HOPSWORKS_API']}/variables/hopsworks_master_password"
      expect_status(403)
    end
  end

  describe 'without authentication' do
    before :all do
      reset_session
    end

    it 'should fail to fetch a variable' do
      get "#{ENV['HOPSWORKS_API']}/variables/hopsworks_master_password"
      expect_status(401)
    end
  end
end
