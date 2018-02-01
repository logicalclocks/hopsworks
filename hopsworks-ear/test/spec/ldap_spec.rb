=begin
This file is part of HopsWorks

Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.

HopsWorks is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

HopsWorks is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
=end
describe "ldap" do
  before(:each) do
    reset_session
  end
  describe 'login' do
    it 'should fail if ldap disabled' do
      set_ldap_enabled("false")
      post "#{ENV['HOPSWORKS_API']}/auth/ldapLogin", URI.encode_www_form({ username: "ldapuser", password: "Pass123"}), { content_type: 'application/x-www-form-urlencoded'}
      expect_json(errorMsg: ->(value){ expect(value).to include("Could not reach LDAP server.")})
      expect_status(401)
    end
#    it 'should fail if ldap enabled but no ldap server.' do
#      set_ldap_enabled("true")
#      post "#{ENV['HOPSWORKS_API']}/auth/ldapLogin", URI.encode_www_form({ username: "ldapuser", password: "Pass123"}), { content_type: 'application/x-www-form-urlencoded'}
#      expect_json(errorMsg: ->(value){ expect(value).to include("Could not reach LDAP server.")})
#      expect_status(401)
#    end
  end
end
