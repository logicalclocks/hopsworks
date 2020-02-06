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
  after(:all) {clean_all_test_projects}
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
    describe "User sort, filter, offset and limit." do
      context 'with authentication' do
        before :all do
          with_valid_session
          @users=create_users
        end
        describe "Users sort" do
          it 'should get all users sorted by firstname' do
            names = @users.map { |o| "#{o[:firstname]}" }
            sorted = names.sort_by(&:downcase)
            get "#{ENV['HOPSWORKS_API']}/users?sort_by=first_name"
            sortedRes = json_body[:items].map { |o| "#{o[:firstname]}" }
            expect(sortedRes).to eq(sorted)
          end
          it 'should get all users sorted by firstname and lastname' do
            names = @users.map { |o| "#{o[:firstname]} #{o[:lastname]}" }
            sorted = names.sort_by(&:downcase)
            get "#{ENV['HOPSWORKS_API']}/users?sort_by=first_name,last_name"
            sortedRes = json_body[:items].map { |o| "#{o[:firstname]} #{o[:lastname]}" }
            expect(sortedRes).to eq(sorted)
          end
          it 'should get all users sorted by firstname descending.' do
            names = @users.map { |o| "#{o[:firstname]}" }
            sorted = names.sort_by(&:downcase).reverse
            get "#{ENV['HOPSWORKS_API']}/users?sort_by=first_name:desc"
            sortedRes = json_body[:items].map { |o| "#{o[:firstname]}" }
            expect(sortedRes).to eq(sorted)
          end
          it 'should get all users sorted by firstname and lastname descending' do
            names = @users.map { |o| "#{o[:firstname]} #{o[:lastname]}" }
            sorted = names.sort_by(&:downcase).reverse
            get "#{ENV['HOPSWORKS_API']}/users?sort_by=first_name:desc,last_name:desc"
            sortedRes = json_body[:items].map { |o| "#{o[:firstname]} #{o[:lastname]}" }
            expect(sortedRes).to eq(sorted)
          end
          it 'should get all users sorted by email descending' do
            names = @users.map { |o| "#{o[:email]}" }
            sorted = names.sort_by(&:downcase).reverse
            get "#{ENV['HOPSWORKS_API']}/users?sort_by=email:desc"
            sortedRes = json_body[:items].map { |o| "#{o[:email]}" }
            expect(sortedRes).to eq(sorted)
          end
          it 'should get all users sorted by email ascending' do
            names = @users.map { |o| "#{o[:email]}" }
            sorted = names.sort_by(&:downcase)
            get "#{ENV['HOPSWORKS_API']}/users?sort_by=email:asc"
            sortedRes = json_body[:items].map { |o| "#{o[:email]}" }
            expect(sortedRes).to eq(sorted)
          end
          it 'should get all users sorted by firstname ascending and lastname descending' do
            s = @users.sort do |a, b|
                res = (a[:firstname].downcase <=> b[:firstname].downcase)
                res = -(a[:lastname].downcase <=> b[:lastname].downcase) if res == 0
                res
            end
            sorted = s.map { |o| "#{o[:firstname]} #{o[:lastname]}" }
            get "#{ENV['HOPSWORKS_API']}/users?sort_by=first_name:asc,last_name:desc"
            sortedRes = json_body[:items].map { |o| "#{o[:firstname]} #{o[:lastname]}" }
            expect(sortedRes).to eq(sorted)
          end
          it 'should get all users sorted by firstname descending and lastname ascending' do
            s = @users.sort do |a, b|
                res = -(a[:firstname].downcase <=> b[:firstname].downcase)
                res = (a[:lastname].downcase <=> b[:lastname].downcase) if res == 0
                res
            end
            sorted = s.map { |o| "#{o[:firstname]} #{o[:lastname]}" }
            get "#{ENV['HOPSWORKS_API']}/users?sort_by=first_name:desc,last_name:asc"
            sortedRes = json_body[:items].map { |o| "#{o[:firstname]} #{o[:lastname]}" }
            expect(sortedRes).to eq(sorted)
          end
          it 'should get all users sorted by lastname ascending and firstname descending' do
            s = @users.sort do |a, b|
                res = (a[:lastname].downcase <=> b[:lastname].downcase)
                res = -(a[:firstname].downcase <=> b[:firstname].downcase) if res == 0
                res
            end
            sorted = s.map { |o| "#{o[:lastname]} #{o[:firstname]}" }
            get "#{ENV['HOPSWORKS_API']}/users?sort_by=last_name:asc,first_name:desc"
            sortedRes = json_body[:items].map { |o| "#{o[:lastname]} #{o[:firstname]}" }
            expect(sortedRes).to eq(sorted)
          end
          it 'should get all users sorted by lastname descending and firstname ascending' do
            s = @users.sort do |a, b|
                res = -(a[:lastname].downcase <=> b[:lastname].downcase)
                res = (a[:firstname].downcase <=> b[:firstname].downcase) if res == 0
                res
            end
            sorted = s.map { |o| "#{o[:lastname]} #{o[:firstname]}" }
            get "#{ENV['HOPSWORKS_API']}/users?sort_by=last_name:desc,first_name:asc"
            sortedRes = json_body[:items].map { |o| "#{o[:lastname]} #{o[:firstname]}" }
            expect(sortedRes).to eq(sorted)
          end
        end
        describe "Users limit and offset" do
          it 'should get only limit=x users' do
            get "#{ENV['HOPSWORKS_API']}/users?limit=10"
            expect(json_body[:items].size).to eq(10)
            get "#{ENV['HOPSWORKS_API']}/users?limit=5"
            expect(json_body[:items].size).to eq(5)
          end
          it 'should get users with offset=y' do
            names = @users.map { |o| "#{o[:firstname]}" }
            sorted = names.sort_by(&:downcase)
            get "#{ENV['HOPSWORKS_API']}/users?offset=5&sort_by=first_name"
            sortedRes = json_body[:items].map { |o| "#{o[:firstname]}" }
            expect(sortedRes).to eq(sorted.drop(5))
          end
          it 'should get only limit=x users with offset=y' do
            names = @users.map { |o| "#{o[:firstname]}" }
            sorted = names.sort_by(&:downcase)
            get "#{ENV['HOPSWORKS_API']}/users?offset=5&limit=6&sort_by=first_name"
            sortedRes = json_body[:items].map { |o| "#{o[:firstname]}" }
            expect(sortedRes).to eq(sorted.drop(5).take(6))
          end
          it 'should ignore if limit < 0' do
            names = @users.map { |o| "#{o[:firstname]}" }
            sorted = names.sort_by(&:downcase)
            get "#{ENV['HOPSWORKS_API']}/users?limit=-6&sort_by=first_name"
            sortedRes = json_body[:items].map { |o| "#{o[:firstname]}" }
            expect(sortedRes).to eq(sorted)
          end
          it 'should ignore if offset < 0' do
            names = @users.map { |o| "#{o[:firstname]}" }
            sorted = names.sort_by(&:downcase)
            get "#{ENV['HOPSWORKS_API']}/users?offset=-6&sort_by=first_name"
            sortedRes = json_body[:items].map { |o| "#{o[:firstname]}" }
            expect(sortedRes).to eq(sorted)
          end
          it 'should ignore limit=0' do
            names = @users.map { |o| "#{o[:firstname]}" }
            sorted = names.sort_by(&:downcase)
            get "#{ENV['HOPSWORKS_API']}/users?limit=0&sort_by=first_name"
            sortedRes = json_body[:items].map { |o| "#{o[:firstname]}" }
            expect(sortedRes).to eq(sorted)
          end
          it 'should work for offset >= size' do
            size = @users.size
            get "#{ENV['HOPSWORKS_API']}/users?offset=#{size}"
            expect(json_body[:items]).to be_nil
            get "#{ENV['HOPSWORKS_API']}/users?offset=#{size + 1}"
            expect(json_body[:items]).to be_nil
          end
        end
        describe "Users detail" do
          it 'should get own user\'s detail using the href' do
            get "#{ENV['HOPSWORKS_API']}/users"
            res = json_body[:items]
            user = res.detect { |e| e[:email] == @user[:email] }
            uri = URI(user[:href])
            get uri.path
            expect(user[:email]).to eq(json_body[:email])
          end
          it 'should fail to get a different user\'s detail using the href' do
            get "#{ENV['HOPSWORKS_API']}/users"
            res = json_body[:items]
            user = res.detect { |e| e[:email] != @user[:email] }
            uri = URI(user[:href])
            get uri.path
            expect(json_body[:errorCode]).to eq(160047)
          end
        end
        describe "Users filter" do
          it 'should only get users with status NEW_MOBILE_ACCOUNT' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status:new_mobile_account"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to eq(0)
            end
          end
          it 'should only get users with status VERIFIED_ACCOUNT' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status:verified_account"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to eq(1)
            end
          end
          it 'should only get users with status ACTIVATED_ACCOUNT' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status:activated_account"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to eq(2)
            end
          end
          it 'should only get users with status DEACTIVATED_ACCOUNT' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status:deactivated_account"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to eq(3)
            end
          end
          it 'should only get users with status BLOCKED_ACCOUNT' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status:blocked_account"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to eq(4)
            end
          end
          it 'should only get users with status LOST_MOBILE' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status:lost_mobile"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to eq(5)
            end
          end
          it 'should only get users with status SPAM_ACCOUNT' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status:spam_account"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to eq(6)
            end
          end
          it 'should only get users with status NEW_MOBILE_ACCOUNT and VERIFIED_ACCOUNT' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status_lt:deactivated_account"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to be < 3
            end
          end
          it 'should only get users with status DEACTIVATED_ACCOUNT, BLOCKED_ACCOUNT, LOST_MOBILE, and SPAM_ACCOUNT' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status_gt:activated_account"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to be > 2
            end
          end
          it 'should only get users with status 0' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status:0"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to eq(0)
            end
          end
          it 'should only get users with status 1' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status:1"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to eq(1)
            end
          end
          it 'should only get users with status 2' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status:2"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to eq(2)
            end
          end
          it 'should only get users with status 3' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status:3"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to eq(3)
            end
          end
          it 'should only get users with status 4' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status:4"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to eq(4)
            end
          end
          it 'should only get users with status 5' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status:5"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to eq(5)
            end
          end
          it 'should only get users with status 6' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status:6"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to eq(6)
            end
          end
          it 'should only get users with status 0, 1 and 2' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status_lt:3"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to be < 3
            end
          end
          it 'should only get users with status 3, 4, 5, and 6' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status_gt:2"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to be > 2
            end
          end
          it 'should only get users with status 2 < x < 5' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=status_gt:2&filter_by=status_lt:5"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.status).to be_between(2, 5)
            end
          end
          it 'should only get users in role hops_users' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=role:hops_user"
            res = json_body[:items]
            res.each do | u |
              role = get_roles(u[:email])
              expect(role).to include('HOPS_USER')
            end
          end
          it 'should only get users in role hops_admin' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=role:hops_admin"
            res = json_body[:items]
            res.each do | u |
              role = get_roles(u[:email])
              expect(role).to include('HOPS_ADMIN')
            end
          end
          it 'should only get users in role agent' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=role:agent"
            res = json_body[:items]
            res.each do | u |
              role = get_roles(u[:email])
              expect(role).to include('AGENT')
            end
          end
          it 'should only get users in role hops_user or hops_admin' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=role:hops_user,hops_admin"
            res = json_body[:items]
            res.each do | u |
              role = get_roles(u[:email])
              expect(role & ['HOPS_ADMIN', 'HOPS_USER']).not_to be_empty
            end
          end
          it 'should only get users not in role hops_user' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=role_neq:hops_user"
            res = json_body[:items]
            res.each do | u |
              role = get_roles(u[:email])            
              expect(role & ['HOPS_ADMIN', 'AGENT']).not_to be_empty # should be admin or agent to be returned
            end
          end
          it 'should only get users not in role hops_admin' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=role_neq:hops_admin"
            res = json_body[:items]
            res.each do | u |
              role = get_roles(u[:email])
              expect(role & ['HOPS_USER', 'AGENT']).not_to be_empty # should be user or agent to be returned
            end
          end
          it 'should only get users not in role agent, cluster_agent' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=role_neq:agent,cluster_agent"
            res = json_body[:items]
            res.each do | u |
              role = get_roles(u[:email])
              expect(role & ["HOPS_ADMIN", "HOPS_USER"]).not_to be_empty # should be user or admin to be returned 
            end
          end
          it 'should only get users not in role hops_admin, agent and cluster_agent' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=role_neq:hops_admin,agent,cluster_agent"
            res = json_body[:items]
            res.each do | u |
              role = get_roles(u[:email])
              expect(role & ['HOPS_USER']).not_to be_empty # should be user to be returned
            end
          end
          it 'should only get users not in role hops_admin and hops_user' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=role_neq:hops_admin,hops_user"
            res = json_body[:items]
            res.each do | u |
              role = get_roles(u[:email])
              expect(role & ['AGENT']).not_to be_empty # should be agent to be returned
            end
          end
          it 'should only get users in role hops_admin and agent' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=role:hops_admin,agent"
            res = json_body[:items]
            res.each do | u |
              role = get_roles(u[:email])
              expect(role & ['HOPS_ADMIN', 'AGENT']).not_to be_empty
            end
          end
          it 'should only get users that are online' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=is_online:1"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.isonline).to eq(1)
            end
          end
          it 'should only get users that are offline' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=is_online:0"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.isonline).to eq(0)
            end
          end
          it 'should only get users that have false login attempt = 12 ' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=false_login:12"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.false_login).to eq(12)
            end
          end
          it 'should only get users that have false login attempt > 10 ' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=false_login_gt:10"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.false_login).to be > 10
            end
          end
          it 'should only get users that have false login attempt < 10 ' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=false_login_lt:10"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.false_login).to be < 10
            end
          end
          it 'should only get users that have false login attempt 10 < x < 20 ' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=false_login_lt:20&filter_by=false_login_gt:10"
            res = json_body[:items]
            res.each do | u |
              user = User.find_by(email: u[:email])
              expect(user.false_login).to be_between(10, 20)
            end
          end
        end
        describe "Users invalid query" do
          it 'should return invalid query error code if filter by param is invalid.' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=false_login_lt:bla"
            expect(json_body[:errorCode]).to eq(310000)
          end
          it 'should return invalid query error code if filter by key is invalid.' do
            get "#{ENV['HOPSWORKS_API']}/users?filter_by=false_login_ls:bla"
            expect(json_body[:errorCode]).to eq(120004)
          end
          it 'should return invalid query error code if sort by param is invalid.' do
            get "#{ENV['HOPSWORKS_API']}/users?sort_by=false_login_lt:bla"
            expect(json_body[:errorCode]).to eq(120004) 
          end
          it 'should return invalid query error code if sort by key is invalid.' do
            get "#{ENV['HOPSWORKS_API']}/users?sort_by=false_login_ls:bla"
            expect(json_body[:errorCode]).to eq(120004)
          end
        end
      end
    end

    describe "Secrets" do
      before :each do
        with_valid_session()
        with_valid_project()
      end
      
      it "should be able to add Secret" do
        private_secret_name = "my_private_secret"
        shared_secret_name = "my_shared_secret"
        secret = "my_secret"
        
        add_private_secret private_secret_name, secret
        expect_status(200)

        add_shared_secret shared_secret_name, secret, @project[:id]
        expect_status(200)

        get_secrets_name
        expect_status(200)
        secrets = json_body[:items]
        private_found = false
        shared_found = false
        
        secrets.each do |secret|
          if secret[:name].eql? private_secret_name
               private_found = true
          end

          if secret[:name].eql? shared_secret_name
               shared_found = true
          end
        end
        expect(private_found).to be true
        expect(shared_found).to be true
      end

      it "should be able to delete a Secret" do
        secret_name = "another_secret_name"
        
        add_private_secret secret_name, "secret"
        expect_status(200)
        
        delete_secret secret_name
        expect_status(200)

        get_secrets_name
        items = json_body[:items]
        found = false
        items.each do |item|
          if item[:name].eql? secret_name
            found = true
          end
        end

        expect(found).to be false
      end

      it "should not be able to add duplicate Secret" do
        secret_name = "my_secret_name"
        add_private_secret secret_name, "secret"
        expect_status(200)

        add_private_secret secret_name, "another_secret"
        expect_status(409)
      end

      it "should be able to delete all secrets" do
        NUM_OF_SECRETS = 10
        (1..NUM_OF_SECRETS).each do |idx|
          secret_name = "secret-#{idx}"
          add_private_secret secret_name, "some_secret"
          expect_status(200)
        end

        get_secrets_name
        expect_json_types(items: :array_of_objects)
        items = json_body[:items]
        # Expect all secrets to be there
        (1..NUM_OF_SECRETS).each do |idx|
          secret_name = "secret-#{idx}"
          found = false
          items.each do |item|
            if item[:name].eql? secret_name
              found = true
              break
            end
          end
          expect(found).to be true
        end

        delete_secrets
        expect_status(200)

        get_secrets_name
        expect_json_types(items: :null)
      end
      
      it "should not be able to add empty secret" do
        add_private_secret "", "secret"
        expect_status(404)
      end

      it "member of Project should be able to access only Shared secret" do
        private_secret_name = "private_secret"
        shared_secret_name = "shared_secret"
        add_private_secret private_secret_name, "secret"
        expect_status(200)

        add_shared_secret shared_secret_name, "another_secret", @project[:id]
        expect_status(200)

        owner_username = @user[:username]
        # Create user and add it as member to project
        member = create_user
        add_member(member[:email], "Data scientist")
        create_session(member[:email], "Pass123")
        
        get_private_secret private_secret_name
        expect_status(404)
        # Private secret should not be available
        get_shared_secret private_secret_name, owner_username
        expect_status(403)

        get_shared_secret shared_secret_name, owner_username
        expect_status(200)

        # Create 
        not_member = create_user
        create_session(not_member[:email], "Pass123")
        get_shared_secret shared_secret_name, owner_username
        expect_status(403)        
      end

      it "member of Project should not be able to delete secret he does not own" do
        shared_secret_name = "shared_secret_1"
        add_shared_secret shared_secret_name, "secret", @project[:id]
        expect_status(200)
        owner = @user
        
        not_member = create_user
        create_session not_member[:email], "Pass123"

        delete_secret shared_secret_name
        expect_status(200)

        create_session owner[:email], "Pass123"
        get_secrets_name
        expect_status(200)
        secrets = json_body[:items]
        found = false
        secrets.each do |secret|
          if secret[:name].eql? shared_secret_name
            found = true
          end
        end
        expect(found).to be true      
      end      
    end
  end
end
