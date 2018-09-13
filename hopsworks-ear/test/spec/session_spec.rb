=begin
 Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 are released under the following license:

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

 Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 are released under the following license:

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
describe "session" do
  before(:each) do
    reset_session
  end
  after :all do
    puts "after project test. Clean all"
    clean_projects
  end
  describe 'login' do
    it 'should work with valid params' do
      user = create_user
      post "#{ENV['HOPSWORKS_API']}/auth/login", URI.encode_www_form({ email: user.email, password: "Pass123"}), { content_type: 'application/x-www-form-urlencoded'}
      expect_json_types(sessionID: :string, status: :string)
      expect_status(200)
    end
    
    it "should work for two factor excluded user" do
      email = "#{random_id}@email.com"
      create_2factor_user_agent(email: email)
      set_two_factor("true")
      set_two_factor_exclud( "AGENT")
      create_session(email, "Pass123")
      expect_status(200)
    end

    it 'should fail with invalid params' do
      user = create_user
      post "#{ENV['HOPSWORKS_API']}/auth/login", URI.encode_www_form({ email: user.email, password: "not_pass"}), { content_type: 'application/x-www-form-urlencoded'}
      expect_json_types(errorMsg: :string)
      expect_status(401)
    end
    
    it "should fail to login with blocked account (status 6)" do
      email = "#{random_id}@email.com"
      create_blocked_user(email: email)
      create_session(email, "Pass123")
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: "This account has been blocked.")
      expect_status(401)
    end
    
    it "should fail to login with deactivated account (status 5)" do
      email = "#{random_id}@email.com"
      create_deactivated_user(email: email)
      create_session(email, "Pass123")
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: "This account has been deactivated.")
      expect_status(401)
    end
    
    it "should fail to login with lost device (status 7 or 8)" do
      email = "#{random_id}@email.com"
      create_lostdevice_user(email: email)
      create_session(email, "Pass123")
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: "This account has registered a lost device.")
      expect_status(401)
    end
    
    it "should fail to login without two factor" do
      set_two_factor("true")
      email = "#{random_id}@email.com"
      create_2factor_user(email: email)
      create_session(email, "Pass123")
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: "Second factor required.")
      expect_status(417)
    end
    
  end

  describe "register" do
    it "should create a new unvalidated user" do
      email = "#{random_id}@email.com"
      first_name = "name"
      last_name = "last"
      password = "Pass123"
      post "#{ENV['HOPSWORKS_API']}/auth/register", {email: email, chosenPassword: password, repeatedPassword: password, firstName: first_name, lastName: last_name, securityQuestion: "Name of your first pet?", securityAnswer: "example_answer", ToS: true, authType: "Mobile", twoFactor: false, testUser: true}
      expect_json(errorMsg: ->(value){ expect(value).to be_empty})
      expect_json(successMessage: ->(value){ expect(value).to include("We registered your account request")})
      expect_status(200)
    end

    it "should fail if email exists" do
      email = "#{random_id}@email.com"
      first_name = "name"
      last_name = "last"
      password = "Pass123"
      register_user(email: email)
      post "#{ENV['HOPSWORKS_API']}/auth/register", {email: email, chosenPassword: password, repeatedPassword: password, firstName: first_name, lastName: last_name, securityQuestion: "Name of your first pet?", securityAnswer: "example_answer", ToS: true, authType: "Mobile", testUser: true}
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: ->(value){ expect(value).to include("There is an existing account")})
      expect_status(400)
    end

    it "should validate an exisiting unvalidated user" do
      email = "#{random_id}@email.com"
      register_user(email: email)
      user = User.find_by(email: email)
      key = user.username + user.validation_key
      get "#{ENV['HOPSWORKS_ADMIN']}/security/validate_account.xhtml", {params: {key: key}}
      expect_status(200)
    end

    it "should fail to signin if not confirmed and no role" do
      email = "#{random_id}@email.com"
      create_unapproved_user(email: email)
      create_session(email, "Pass123")
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: ->(value){ expect(value).to include("This account has not yet been approved.")})
      expect_status(401)
    end

    it "should fail to signin with role and new account (status 1)" do
      email = "#{random_id}@email.com"
      register_user(email: email)
      create_role(User.find_by(email: email))
      create_session(email, "Pass123")
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: "This account has not been activated.")
      expect_status(401)
    end

    it "should fail with status 4 and no role" do
      email = "#{random_id}@email.com"
      create_user_without_role(email: email)
      create_session(email, "Pass123")
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: ->(value){ expect(value).to include("No valid role found for this user")})
      expect_status(401)
    end
  end
end
