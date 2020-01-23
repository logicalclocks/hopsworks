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
describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects}
  describe "session" do
    before(:each) do
      reset_session
    end
    describe 'login' do
      it 'should work with valid params' do
        user = create_user
        post "#{ENV['HOPSWORKS_API']}/auth/login", URI.encode_www_form({email: user.email, password: "Pass123"}), {content_type: 'application/x-www-form-urlencoded'}
        expect_json_types(sessionID: :string)
        expect_status(200)
      end

      it "should work for two factor excluded user" do
        email = "#{random_id}@email.com"
        create_2factor_user_agent(email: email)
        set_two_factor("true")
        set_two_factor_exclud("AGENT")
        create_session(email, "Pass123")
      end

      it 'should fail with invalid params' do
        user = create_user
        post "#{ENV['HOPSWORKS_API']}/auth/login", URI.encode_www_form({email: user.email, password: "not_pass"}), {content_type: 'application/x-www-form-urlencoded'}
        expect_json(errorCode: 160008)
        expect_status(401)
      end

      it "should fail to login if false login > 5 for hops users" do
        user = create_user
        set_false_login(user, 6)
        try_login(user, "Pass1234")
        expect_status(401)
        try_login(user, "Pass123")
        expect_json(errorCode: 160007)
        expect_status(401)
      end

      it "should login agent if false login < 20" do
        params={}
        user = create_user_with_role(params, "AGENT")
        set_false_login(user, 18)
        try_login(user, "Pass1234")
        expect_status(401)
        try_login(user, "Pass123")
        expect_status(200)
      end

      it "should fail to login if false login > 20 for agent" do
        params={}
        user = create_user_with_role(params, "AGENT")
        set_false_login(user, 21)
        try_login(user, "Pass1234")
        expect_status(401)
        try_login(user, "Pass123")
        expect_json(errorCode: 160007)
        expect_status(401)
      end

      it "should fail to login with blocked account (status 4)" do
        email = "#{random_id}@email.com"
        create_blocked_user(email: email)
        raw_create_session(email, "Pass123")
        expect_json(successMessage: ->(value) {expect(value).to be_nil})
        expect_json(errorCode: 160007)
        expect_status(401)
      end

      it "should fail to login with spam account (status 6)" do
        email = "#{random_id}@email.com"
        create_spam_user(email: email)
        raw_create_session(email, "Pass123")
        expect_json(successMessage: ->(value) {expect(value).to be_nil})
        expect_json(errorCode: 160007)
        expect_status(401)
      end

      it "should fail to login with deactivated account (status 3)" do
        email = "#{random_id}@email.com"
        create_deactivated_user(email: email)
        raw_create_session(email, "Pass123")
        expect_json(successMessage: ->(value) {expect(value).to be_nil})
        expect_json(errorCode: 160005)
        expect_status(401)
      end

      it "should fail to login with lost device (status 5)" do
        email = "#{random_id}@email.com"
        create_lostdevice_user(email: email)
        raw_create_session(email, "Pass123")
        expect_json(successMessage: ->(value) {expect(value).to be_nil})
        expect_json(errorMsg: "This account has registered a lost device.")
        expect_status(401)
      end

      it "should fail to login without two factor" do
        set_two_factor("true")
        email = "#{random_id}@email.com"
        create_2factor_user(email: email)
        raw_create_session(email, "Pass123")
        expect_json(successMessage: ->(value) {expect(value).to be_nil})
        expect_json(errorCode: 120002)
        expect_status(400)
        set_two_factor("false")
      end

      it "should fail to login blocked account with two factor" do
        set_two_factor("true")
        email = "#{random_id}@email.com"
        user = create_2factor_user(email: email)
        set_status(user, 4)
        raw_create_session(email, "Pass123")
        expect_json(successMessage: ->(value) {expect(value).to be_nil})
        expect_json(errorCode: 160007)
        expect_status(401)
        set_two_factor("false")
      end

    end

    describe "register" do
      it "should create a new unvalidated user" do
        email = "#{random_id}@email.com"
        first_name = "name"
        last_name = "last"
        password = "Pass123"
        post "#{ENV['HOPSWORKS_API']}/auth/register", {email: email, chosenPassword: password, repeatedPassword:
            password, firstName: first_name, lastName: last_name, securityQuestion: "Name of your first pet?",
                                                       securityAnswer: "example_answer", tos: true, authType:
                                                           "Mobile", twoFactor: false, testUser: true}
        expect_json(errorMsg: ->(value) {expect(value).to be_nil})
        expect_json(successMessage: ->(value) {expect(value).to include("We registered your account request")})
        expect_status(200)
      end

      it "should fail if email exists" do
        email = "#{random_id}@email.com"
        first_name = "name"
        last_name = "last"
        password = "Pass123"
        register_user(email: email)
        post "#{ENV['HOPSWORKS_API']}/auth/register", {email: email, chosenPassword: password, repeatedPassword:
            password, firstName: first_name, lastName: last_name, securityQuestion: "Name of your first pet?",
                                                       securityAnswer: "example_answer", tos: true, authType:
                                                           "Mobile", testUser: true}
        expect_json(successMessage: ->(value) {expect(value).to be_nil})
        expect_json(errorCode: 160003)
        expect_status(409)
      end

      it "should validate an existing unvalidated user" do
        email = "#{random_id}@email.com"
        register_user(email: email)
        user = User.find_by(email: email)
        key = user.username + user.validation_key
        type = user.validation_key_type
        expect(type).to eq("EMAIL")
        get "#{ENV['HOPSWORKS_ADMIN']}/security/validate_account.xhtml", {params: {key: key}}
        #check html content 200 is always returned
        expect(response).to include("You have successfully validated your email address")
        expect_status(200)
      end

      it "should fail to validate a validated user" do
        email = "#{random_id}@email.com"
        register_user(email: email)
        user = User.find_by(email: email)
        key = user.username + user.validation_key
        type = user.validation_key_type
        expect(type).to eq("EMAIL")
        get "#{ENV['HOPSWORKS_ADMIN']}/security/validate_account.xhtml", {params: {key: key}}
        #check html content 200 is always returned
        expect(response).to include("You have successfully validated your email address")
        expect_status(200)
        get "#{ENV['HOPSWORKS_ADMIN']}/security/validate_account.xhtml", {params: {key: key}}
        expect(response).to include("Your email address has already been validated.")
        expect_status(200)
      end

      it "should fail to validate already activated user" do
        email = "#{random_id}@email.com"
        register_user(email: email)
        user = User.find_by(email: email)
        key = user.username + user.validation_key
        type = user.validation_key_type
        expect(type).to eq("EMAIL")
        get "#{ENV['HOPSWORKS_ADMIN']}/security/validate_account.xhtml", {params: {key: key}}
        #check html content 200 is always returned
        expect(response).to include("You have successfully validated your email address")
        expect_status(200)
        user.status = 2
        user.save
        get "#{ENV['HOPSWORKS_ADMIN']}/security/validate_account.xhtml", {params: {key: key}}
        expect(response).to include("Your account has already been approved!")
        expect_status(200)
      end

      it "should fail to validate user email with wrong verification key" do
        email = "#{random_id}@email.com"
        register_user(email: email)
        user = User.find_by(email: email)
        false_logins = user.false_login
        key = user.username + user.validation_key + "1"
        type = user.validation_key_type
        expect(type).to eq("EMAIL")
        get "#{ENV['HOPSWORKS_ADMIN']}/security/validate_account.xhtml", {params: {key: key}}
        #check html content 200 is always returned
        expect(response).to include("The email address you are trying to validate does not exist in the system.")
        expect_status(200)
        user = User.find_by(email: email)
        expect(user.false_login).to eq(false_logins + 1)
      end

      it "should validate an existing unvalidated user with rest" do
        email = "#{random_id}@email.com"
        register_user(email: email)
        user = User.find_by(email: email)
        key = user.username + user.validation_key
        type = user.validation_key_type
        expect(type).to eq("EMAIL")
        validate_user_rest(key)
        expect_status(200)
      end

      it "should fail to sign in if not confirmed and no role" do
        email = "#{random_id}@email.com"
        create_unapproved_user(email: email)
        raw_create_session(email, "Pass123")
        expect_json(successMessage: ->(value) {expect(value).to be_nil})
        expect_json(errorCode: 160036)
        expect_status(401)
      end

      it "should fail to signin with role and new account (status 1)" do
        email = "#{random_id}@email.com"
        register_user(email: email)
        create_role(User.find_by(email: email))
        raw_create_session(email, "Pass123")
        expect_json(successMessage: ->(value) {expect(value).to be_nil})
        expect_json(errorCode: 160034)
        expect_status(401)
      end

      it "should fail with status 4 and no role" do
        email = "#{random_id}@email.com"
        create_user_without_role(email: email)
        raw_create_session(email, "Pass123")
        expect_json(successMessage: ->(value) {expect(value).to be_nil})
        expect_json(errorCode: 160000)
        expect_status(401)
      end
    end

    describe "recovery" do
      context 'with Rest api' do
        after :all do
          set_two_factor("false")
        end
        it "should create password reset key" do
          user = create_user()
          start_password_reset(user.email, "Name of your first pet?", "example_answer")
          expect_status(200)
          user = User.find_by(email: user.email)
          type = user.validation_key_type
          expect(type).to eq("PASSWORD")
        end

        it "should verify password reset key" do
          user = create_user()
          start_password_reset(user.email, "Name of your first pet?", "example_answer")
          expect_status(200)
          user = User.find_by(email: user.email)
          key = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("PASSWORD")
          validate_recovery_key(key)
          expect_status(200)
          user = User.find_by(email: user.email)
          type = user.validation_key_type
          expect(type).to eq("PASSWORD_RESET")
        end

        it "should reset password" do
          user = create_user()
          start_password_reset(user.email, "Name of your first pet?", "example_answer")
          expect_status(200)
          user = User.find_by(email: user.email)
          key = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("PASSWORD")
          validate_recovery_key(key)
          expect_status(200)
          user = User.find_by(email: user.email)
          type = user.validation_key_type
          expect(type).to eq("PASSWORD_RESET")
          reset_password(key, "Pass1234", "Pass1234")
          expect_status(200)
          try_login(user, "Pass1234")
          expect_status(200)
        end

        it "should fail to reset qr code with password key" do
          user = create_user()
          start_password_reset(user.email, "Name of your first pet?", "example_answer")
          expect_status(200)
          user = User.find_by(email: user.email)
          key = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("PASSWORD")
          reset_qr_code(key)
          expect_json(errorCode: 160041)
          expect_status(400)
        end

        it "should fail to reset qr code with password reset key" do
          user = create_user()
          start_password_reset(user.email, "Name of your first pet?", "example_answer")
          expect_status(200)
          user = User.find_by(email: user.email)
          key = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("PASSWORD")
          validate_recovery_key(key)
          expect_status(200)
          user = User.find_by(email: user.email)
          type = user.validation_key_type
          expect(type).to eq("PASSWORD_RESET")
          reset_qr_code(key)
          expect_json(errorCode: 160041)
          expect_status(400)
        end

        it "should fail to reset password if security question is wrong" do
          user = create_user()
          start_password_reset(user.email, "Name of your first love?", "example_answer")
          expect_status(400)
          user = User.find_by(email: user.email)
          key = user.validation_key
          type = user.validation_key_type
          expect(type).to be_nil
          expect(key).to be_nil
        end

        it "should fail to reset password if security answer is wrong" do
          user = create_user()
          start_password_reset(user.email, "Name of your first pet?", "example")
          expect_status(400)
          user = User.find_by(email: user.email)
          key = user.validation_key
          type = user.validation_key_type
          expect(type).to be_nil
          expect(key).to be_nil
        end

        it "should fail to create qr reset key if 2 factor not enabled" do
          set_two_factor("false")
          user = create_user()
          start_qr_recovery(user.email, "Pass123")
          expect_status(412)
          user = User.find_by(email: user.email)
          key = user.validation_key
          type = user.validation_key_type
          expect(type).to be_nil
          expect(key).to be_nil
        end

        it "should fail to create qr reset key if 2 factor not enabled for user" do
          set_two_factor("true")
          user = create_user()
          start_qr_recovery(user.email, "Pass123")
          expect_status(412)
          user = User.find_by(email: user.email)
          key = user.validation_key
          type = user.validation_key_type
          expect(type).to be_nil
          expect(key).to be_nil
        end

        it "should create qr reset key" do
          set_two_factor("true")
          user = create_2factor_user()
          start_qr_recovery(user.email, "Pass123")
          expect_status(200)
          user = User.find_by(email: user.email)
          type = user.validation_key_type
          expect(type).to eq("QR_RESET")
        end

        it "should fail to create qr reset key with wrong password" do
          set_two_factor("true")
          user = create_2factor_user()
          start_qr_recovery(user.email, "Pass123456")
          expect_status(400)
          user = User.find_by(email: user.email)
          key = user.validation_key
          type = user.validation_key_type
          expect(type).to be_nil
          expect(key).to be_nil
        end

        it "should reset qr code" do
          user = create_2factor_user()
          start_qr_recovery(user.email, "Pass123")
          expect_status(200)
          user = User.find_by(email: user.email)
          key = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("QR_RESET")
          reset_qr_code(key)
          expect_status(200)
        end

        it "should fail to verify a qr code reset key" do
          user = create_2factor_user()
          start_qr_recovery(user.email, "Pass123")
          expect_status(200)
          user = User.find_by(email: user.email)
          key = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("QR_RESET")
          validate_recovery_key(key)
          expect_json(errorCode: 160041)
          expect_status(400)
        end

        it "should fail to reset password with qr code reset key" do
          user = create_2factor_user()
          start_qr_recovery(user.email, "Pass123")
          expect_status(200)
          user = User.find_by(email: user.email)
          key = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("QR_RESET")
          reset_password(key, "Pass1234", "Pass1234")
          expect_json(errorCode: 160041)
          expect_status(400)
        end

        it "should fail to verify email validation key" do
          email = "#{random_id}@email.com"
          register_user(email: email)
          user = User.find_by(email: email)
          set_status(user, 2)
          key = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("EMAIL")
          validate_recovery_key(key)
          expect_json(errorCode: 160041)
          expect_status(400)
        end

        it "should fail to reset password with email validation key" do
          email = "#{random_id}@email.com"
          register_user(email: email)
          user = User.find_by(email: email)
          set_status(user, 2)
          key = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("EMAIL")
          reset_password(key, "Pass1234", "Pass1234")
          expect_json(errorCode: 160041)
          expect_status(400)
        end

        it "should fail to reset qr code with email validation key" do
          email = "#{random_id}@email.com"
          register_user(email: email)
          user = User.find_by(email: email)
          set_status(user, 2)
          key = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("EMAIL")
          reset_qr_code(key)
          expect_json(errorCode: 160041)
          expect_status(400)
        end

        it "should fail to validate wrong password reset key" do
          user = create_user()
          start_password_reset(user.email, "Name of your first pet?", "example_answer")
          expect_status(200)
          user = User.find_by(email: user.email)
          false_logins = user.false_login
          key = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("PASSWORD")
          validate_recovery_key("#{key}1")
          expect_status(400)
          user = User.find_by(email: user.email)
          expect(user.false_login).to eq(false_logins + 1)
        end

        it "should fail to reset qr code with wrong validation key" do
          set_two_factor("true")
          user = create_2factor_user()
          start_qr_recovery(user.email, "Pass123")
          expect_status(200)
          user = User.find_by(email: user.email)
          false_logins = user.false_login
          key = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("QR_RESET")
          reset_qr_code("#{key}1")
          expect_status(400)
          user = User.find_by(email: user.email)
          expect(user.false_login).to eq(false_logins + 1)
        end

        it "should fail to reset password with wrong validation key" do
          user = create_user()
          start_password_reset(user.email, "Name of your first pet?", "example_answer")
          expect_status(200)
          user = User.find_by(email: user.email)
          false_logins = user.false_login
          key = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("PASSWORD")
          validate_recovery_key(key)
          expect_status(200)
          user = User.find_by(email: user.email)
          type = user.validation_key_type
          expect(type).to eq("PASSWORD_RESET")
          reset_password("#{key}1", "Pass1234", "Pass1234")
          expect_status(400)
          user = User.find_by(email: user.email)
          expect(user.false_login).to eq(false_logins + 1)
        end

        it "should not let unvalidated user reset password" do
          email = "#{random_id}@email.com"
          register_user(email: email)
          user = User.find_by(email: email)
          type = user.validation_key_type
          expect(type).to eq("EMAIL")
          start_password_reset(user.email, "Name of your first pet?", "example_answer")
          expect_status(400)
        end

        it "should not let unvalidated user reset qr code" do
          set_two_factor("true")
          email = "#{random_id}@email.com"
          params={}
          params[:email] = email
          params[:twoFactor] = 1
          register_user(params)
          user = User.find_by(email: email)
          type = user.validation_key_type
          expect(type).to eq("EMAIL")
          start_qr_recovery(user.email, "Pass123")
          expect_status(400)
        end

        it "should change validation key type if a request is resubmitted" do
          set_two_factor("true")
          user = create_2factor_user()
          start_password_reset(user.email, "Name of your first pet?", "example_answer")
          expect_status(200)
          user = User.find_by(email: user.email)
          key1 = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("PASSWORD")
          start_qr_recovery(user.email, "Pass123")
          expect_status(200)
          user = User.find_by(email: user.email)
          key2 = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("QR_RESET")
          expect(key1).not_to eq(key2)
        end

        it "should change validation key type if a request is resubmitted (password to qr)" do
          set_two_factor("true")
          user = create_2factor_user()
          start_password_reset(user.email, "Name of your first pet?", "example_answer")
          expect_status(200)
          user = User.find_by(email: user.email)
          key1 = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("PASSWORD")
          validate_recovery_key(key1)
          expect_status(200)
          user = User.find_by(email: user.email)
          type = user.validation_key_type
          expect(type).to eq("PASSWORD_RESET")
          start_qr_recovery(user.email, "Pass123")
          expect_status(200)
          user = User.find_by(email: user.email)
          key2 = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("QR_RESET")
          expect(key1).not_to eq(key2)
        end

        it "should change validation key type if a request is resubmitted (qr to password)" do
          set_two_factor("true")
          user = create_2factor_user()
          start_qr_recovery(user.email, "Pass123")
          expect_status(200)
          user = User.find_by(email: user.email)
          key1 = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("QR_RESET")
          start_password_reset(user.email, "Name of your first pet?", "example_answer")
          expect_status(200)
          user = User.find_by(email: user.email)
          key2 = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("PASSWORD")
          expect(key1).not_to eq(key2)
        end

        it "should resend the same key if request is sent again (qr code)" do
          user = create_2factor_user()
          start_qr_recovery(user.email, "Pass123")
          expect_status(200)
          user = User.find_by(email: user.email)
          key1 = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("QR_RESET")
          start_qr_recovery(user.email, "Pass123")
          expect_status(200)
          user = User.find_by(email: user.email)
          key2 = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("QR_RESET")
          expect(key1).to eq(key2)
        end

        it "should resend the same key if request is sent again (password)" do
          user = create_user()
          start_password_reset(user.email, "Name of your first pet?", "example_answer")
          expect_status(200)
          user = User.find_by(email: user.email)
          key1 = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("PASSWORD")
          start_password_reset(user.email, "Name of your first pet?", "example_answer")
          expect_status(200)
          user = User.find_by(email: user.email)
          key2 = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("PASSWORD")
          expect(key1).to eq(key2)
        end

        it "should resend the same key if request is sent again (password)" do
          user = create_user()
          start_password_reset(user.email, "Name of your first pet?", "example_answer")
          expect_status(200)
          user = User.find_by(email: user.email)
          key1 = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("PASSWORD")
          validate_recovery_key(key1)
          expect_status(200)
          user = User.find_by(email: user.email)
          type = user.validation_key_type
          expect(type).to eq("PASSWORD_RESET")
          start_password_reset(user.email, "Name of your first pet?", "example_answer")
          expect_status(200)
          user = User.find_by(email: user.email)
          key2 = user.username + user.validation_key
          type = user.validation_key_type
          expect(type).to eq("PASSWORD")
          expect(key1).not_to eq(key2)
        end

      end
    end
  end
end
