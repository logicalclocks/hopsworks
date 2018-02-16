=begin
Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved

Permission is hereby granted, free of charge, to any person obtaining a copy of this
software and associated documentation files (the "Software"), to deal in the Software
without restriction, including without limitation the rights to use, copy, modify, merge,
publish, distribute, sublicense, and/or sell copies of the Software, and to permit
persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
