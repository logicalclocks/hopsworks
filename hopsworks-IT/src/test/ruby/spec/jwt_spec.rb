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
  describe "Renew service JWT" do
    before :all do
      reset_session
    end
    
    context "#not logged in" do
      it "should not be able to renew service JWT" do
        put "#{ENV['HOPSWORKS_API']}/jwt/service", {
              token: "some_token",
              expiresAt: "1234",
              nbf: "1234"
            }
        expect_status(401)
      end

      it "should not be able to invalidate JWT" do
        delete "#{ENV['HOPSWORKS_API']}/jwt/service/some_token"
        expect_status(401)
      end
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
        expect_status(403)
      end
    end

    context "#agent user" do
      it "should be able to login as service" do
        post "#{ENV['HOPSWORKS_API']}/auth/service",
             URI.encode_www_form({ email: "agent@hops.io", password: "admin"}), { content_type: 'application/x-www-form-urlencoded'}
        expect_status(200)
        expect(headers["authorization"]).not_to be_nil
        expect(headers["authorization"]).not_to be_empty
        renew_tokens = json_body[:renewTokens]
        expect(renew_tokens.length).to eql(5)
      end

      describe "#logged in as service user" do
        before :all do
          @service_jwt_lifetime = getVar("service_jwt_lifetime_ms").value
          setVar "service_jwt_lifetime_ms", "1000"
          refresh_variables
          reset_session
          
          post "#{ENV['HOPSWORKS_API']}/auth/service",
               URI.encode_www_form({ email: "agent@hops.io", password: "admin"}), { content_type: 'application/x-www-form-urlencoded'}
          @renew_tokens = json_body[:renewTokens]
          @master_token = headers["authorization"].split[1].strip
        end

        after :all do
          setVar "service_jwt_lifetime_ms", @service_jwt_lifetime
          refresh_variables
          reset_session
        end

        it "should be able to renew master jwt" do

          now = Time.now
          not_before = now.strftime("%Y-%m-%dT%H:%M:%S.%L%z")
          exp = now + 300
          new_expiration = exp.strftime("%Y-%m-%dT%H:%M:%S.%L%z")
          
          # Use one-time token
          Airborne.configure do |config|
            config.headers = {}
            config.headers["Authorization"] = "Bearer #{@renew_tokens[0]}"
          end
          sleep 1
          put "#{ENV['HOPSWORKS_API']}/jwt/service",
              {
                token: @master_token,
                expiresAt: new_expiration,
                nbf: not_before
              }

          expect_status(200)
          
          new_master_token = json_body[:jwt][:token]
          new_one_time_tokens = json_body[:renewTokens]
          expect(new_master_token).not_to be_nil
          expect(new_master_token).not_to be_empty

          expect(new_one_time_tokens.length).to eql(5)

          master_jwt = JWT.decode new_master_token, nil, false

          exp_response = Time.at(master_jwt[0]['exp'])
          nbf_response = Time.at(master_jwt[0]['nbf'])
          # Do not compare milliseconds, there might be different due to conversion
          expect(now.strftime("%Y-%m-%dT%H:%M:%S%z")).to eql(nbf_response.strftime("%Y-%m-%dT%H:%M:%S%z"))
          expect(exp.strftime("%Y-%m-%dT%H:%M:%S%z")).to eql(exp_response.strftime("%Y-%m-%dT%H:%M:%S%z"))
          
          # Previous token should still be valid
          Airborne.configure do |config|
            config.headers["Authorization"] = "Bearer #{@master_token}"
          end
          get "#{ENV['HOPSWORKS_CA']}/token"
          expect_status(200)

          # Invalidate previous master token
          Airborne.configure do |config|
            config.headers["Authorization"] = "Bearer #{new_master_token}"
          end
          delete "#{ENV['HOPSWORKS_API']}/jwt/service/#{@master_token}"
          expect_status(200)

          # Subsequent calls with the old master key should fail
          Airborne.configure do |config|
            config.headers["Authorization"] = "Bearer #{@master_token}"
          end
          get "#{ENV['HOPSWORKS_CA']}/token"
          expect_status(401)

          # But new master should be still valid...
          Airborne.configure do |config|
            config.headers["Authorization"] = "Bearer #{new_master_token}"
          end
          get "#{ENV['HOPSWORKS_CA']}/token"
          expect_status(200)
        end
      end
      
    end
    
  end
end
