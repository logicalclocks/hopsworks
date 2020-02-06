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
  describe "admin ops" do
    after :all do
      reset_session
    end
    describe "#change master encryption password" do
      
      context "without authentication" do
        before :all do
          reset_session
        end
        it "should fail" do
          put "#{ENV['HOPSWORKS_API']}/admin/encryptionPass",
              URI.encode_www_form({ oldPassword: 'oldPassword', newPassword: 'newPassword'}),
              { content_type: 'application/x-www-form-urlencoded'}
          expect_status(401)
          expect_json(errorCode: 200003)
        end
      end

      context "authenticated normal user" do
        before :all do
          with_valid_session()
        end

        it "should fail to change password" do
          put "#{ENV['HOPSWORKS_API']}/admin/encryptionPass",
              URI.encode_www_form({ oldPassword: 'verysecurepassword', newPassword: 'newPassword'}),
              { content_type: 'application/x-www-form-urlencoded'}
          expect_status(403)
        end
      end
      
      context "authenticated admin" do
        before :all do
          with_admin_session()
        end

        it "should fail with wrong password" do
          put "#{ENV['HOPSWORKS_API']}/admin/encryptionPass",
              URI.encode_www_form({ oldPassword: 'hopefully_this_is_a_wrong_password', newPassword: 'newPassword'}),
              { content_type: 'application/x-www-form-urlencoded'}
          expect_status(403)
        end

        it "should succeed with correct password" do
          ## Assuming that the password hasn't changed from the default
          put "#{ENV['HOPSWORKS_API']}/admin/encryptionPass",
              URI.encode_www_form({ oldPassword: 'verysecurepassword', newPassword: 'verysecurepassword'}),
              { content_type: 'application/x-www-form-urlencoded'}
          expect_status(201)
          opId = json_body[:successMessage]
          get "#{ENV['HOPSWORKS_API']}/admin/encryptionPass/#{opId}"
          wait_for do
            response.code == 200
          end
        end

        it "should not get any result for non-existing operation" do
          randOpId = rand(-2147483648..2147483647)
          get "#{ENV['HOPSWORKS_API']}/admin/encryptionPass/#{randOpId}"
          expect_status(404)
        end
      end
      
    end
    
  end

  describe "#credentials" do
    describe "#get x509 credentials" do
      before :all do
        reset_session
      end

      context "#not logged in" do
        it "should fail" do
          get "#{ENV['HOPSWORKS_API']}/admin/credentials/x509?project=some_project&username=some_username"
          expect_status(401)
        end
      end

      context "#logged in a normal user" do
        before :all do
          with_valid_session
        end

        it "should fail" do
          get "#{ENV['HOPSWORKS_API']}/admin/credentials/x509?project=some_project&username=some_username"
          expect_status(403)
        end
      end

      context "#logged in as admin or agent" do
        before :all do
          reset_session
          with_valid_project
          @project_user = @user
          with_admin_session
        end

        it "should succeed to get credentials" do
          project_username = @project[:projectname] + "__" + @project_user[:username]
          get "#{ENV['HOPSWORKS_API']}/admin/credentials/x509?username=#{project_username}"
          expect_status(200)
          expect(json_body[:fileExtension]).to eql("jks")
          expect(json_body[:kStore]).not_to be_nil
          expect(json_body[:tStore]).not_to be_nil
          expect(json_body[:password]).not_to be_nil
          with_agent_session
          get "#{ENV['HOPSWORKS_API']}/admin/credentials/x509?username=#{project_username}"
          expect_status(200)
        end
      end
    end
  end
end
