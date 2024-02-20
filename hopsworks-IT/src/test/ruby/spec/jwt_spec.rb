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
require 'jwt'

describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects(spec: "jwt")}
  describe "Renew service JWT" do
    before :all do
      reset_session
    end

    context "#users" do
      before :all do
        reset_session
        with_valid_session
        @user_email = @user.email
        reset_session
      end
      
      it "should not be able to login as service" do
        post "#{ENV['HOPSWORKS_API']}/auth/service",
             URI.encode_www_form({ email: @user_email, password: "Pass123"}), { content_type: 'application/x-www-form-urlencoded'}
        expect_status_details(403)
      end
    end

    context "#agent user" do
      it "should be able to login as service" do
        post "#{ENV['HOPSWORKS_API']}/auth/service",
             URI.encode_www_form({ email: "agent@hops.io", password: "admin"}), { content_type: 'application/x-www-form-urlencoded'}
        expect_status_details(200)
        expect(headers["authorization"]).not_to be_nil
        expect(headers["authorization"]).not_to be_empty
      end

      describe "#logged in as service user" do
        before :all do
          @service_jwt_lifetime = getVar("service_jwt_lifetime_ms").value
          setVar "service_jwt_lifetime_ms", "1000"
          refresh_variables
          reset_session
          
          post "#{ENV['HOPSWORKS_API']}/auth/service",
               URI.encode_www_form({ email: "agent@hops.io", password: "admin"}), { content_type: 'application/x-www-form-urlencoded'}
          @master_token = headers["authorization"].split[1].strip
        end

        after :all do
          setVar "service_jwt_lifetime_ms", @service_jwt_lifetime
          refresh_variables
          reset_session
        end
      end
      
    end
    
  end

  describe "Elk JWT tokens" do

    context "#users" do
      before :all do
        with_valid_session
        @proj = create_project
        proj_inode = get_project_inode(@proj)
        @proj_inode_id=proj_inode[:id]
      end

      it "should be able to generate a jwt token with valid attributes" do
        get "#{ENV['HOPSWORKS_API']}/elastic/jwt/#{@proj[:id]}"
        expect_status_details(200)
        token = JWT.decode json_body[:token], nil, false
        expect(token[0]['sub']).to eql(@proj[:projectname].downcase)
        expect(token[0]['pn']).to eql(@proj[:projectname].downcase.gsub('_',''))
        expect(token[0]['roles']).to eql("data_owner")
        expect(token[0]['piid']).to eql(@proj_inode_id)
      end

      it "should fail to create jwt token if they have no role in the project" do
        reset_and_create_session
        get "#{ENV['HOPSWORKS_API']}/elastic/jwt/#{@proj[:id]}"
        expect(401)
        expect_json(errorCode: 160000)
      end
    end

   context "#data scientist" do
      before :all do
        with_valid_project
        @proj = get_project
        proj_inode = get_project_inode(@proj)
        @proj_inode_id=proj_inode[:id]
      end

      it "should be able to create a jwt token in the project with data_scientist role" do
        new_user = create_user[:email]
        add_member(new_user, "Data scientist")
        reset_session
        create_session(new_user,"Pass123")
        get "#{ENV['HOPSWORKS_API']}/elastic/jwt/#{@proj[:id]}"
        expect(200)
        token = JWT.decode json_body[:token], nil, false
        expect(token[0]['sub']).to eql(@proj[:projectname].downcase)
        expect(token[0]['pn']).to eql(@proj[:projectname].downcase.gsub('_',''))
        expect(token[0]['roles']).to eql("data_scientist")
        expect(token[0]['piid']).to eql(@proj_inode_id)
      end
   end
  end
end
