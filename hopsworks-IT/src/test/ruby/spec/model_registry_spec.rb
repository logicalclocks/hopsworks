=begin
 This file is part of Hopsworks
 Copyright (C) 2021, Logical Clocks AB. All rights reserved

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
  after(:all) {clean_all_test_projects(spec: "model_registry")}
  describe 'model registry' do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "should fail" do
        get_model_registries(@project[:id], nil)
        expect_status_details(401)
      end
    end
    context 'with authentication' do
      before :all do
        setup_initial_project
        setup_shared_project
        setup_private_project
      end
      def setup_initial_project()
        @user1_params = {email: "user1_#{random_id}@email.com", first_name: "User", last_name: "1", password: "Pass123"}
        @user1 = create_user_with_role(@user1_params, "HOPS_ADMIN")

        create_session(@user1[:email], @user1_params[:password])
        @project1 = create_project
      end
      def setup_shared_project()
        @user2_params = {email: "user2_#{random_id}@email.com", first_name: "User", last_name: "2", password: "Pass123"}
        @user2 = create_user_with_role(@user2_params, "HOPS_ADMIN")

        create_session(@user2[:email], @user2_params[:password])
        @project2 = create_project

        share_dataset_checked(@project2, "Models", @project1[:projectname], datasetType: "DATASET")

        create_session(@user1_params[:email], @user1_params[:password])
        accept_dataset_checked(@project1, "#{@project2[:projectname]}::Models", datasetType: "DATASET")
      end
      def setup_private_project()
        @user3_params = {email: "user3_#{random_id}@email.com", first_name: "User", last_name: "3", password: "Pass123"}
        @user3 = create_user_with_role(@user3_params, "HOPS_ADMIN")

        create_session(@user3[:email], @user3_params[:password])
        @project3 = create_project
      end

      it "should get default model registry for created project" do
        create_session(@user2_params[:email], @user2_params[:password])

        get_model_registries(@project2[:id], nil)
        expect_status_details(200)

        expect(json_body[:items].count).to eq 1
        expect(json_body[:count]).to eq 1
        expect(json_body[:items][0][:id]).to eq @project2[:id]
        expect(json_body[:items][0][:name]).to eq @project2[:projectname]

        initial_registry_project = json_body[:items].detect { |registry| registry[:name] == @project2[:projectname] }
        expect(URI(initial_registry_project[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{@project2[:id]}/modelregistries/#{@project2[:id]}"
      end

      it "should get both model registries after sharing model registry" do
        create_session(@user1_params[:email], @user1_params[:password])

        get_model_registries(@project1[:id], nil)
        expect_status_details(200)
        expect(json_body[:items].count).to eq 2
        expect(json_body[:count]).to eq 2

        initial_registry_project = json_body[:items].detect { |registry| registry[:name] == @project1[:projectname] }
        expect(URI(initial_registry_project[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{@project1[:id]}/modelregistries/#{@project1[:id]}"

        shared_registry_project = json_body[:items].detect { |registry| registry[:name] == @project2[:projectname] }
        expect(URI(shared_registry_project[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{@project1[:id]}/modelregistries/#{@project2[:id]}"
      end

      it "should have access to project model registry" do
        create_session(@user1_params[:email], @user1_params[:password])

        get_model_registry(@project1[:id], @project1[:id], nil)
        expect_status_details(200)
      end

      it "should have access to model registry shared with project" do
        create_session(@user1_params[:email], @user1_params[:password])

        get_model_registry(@project1[:id], @project2[:id], nil)
        expect_status_details(200)
      end

      it "should not have access to model registry not shared with project" do
        create_session(@user1_params[:email], @user1_params[:password])

        get_model_registry(@project1[:id], @project3[:id], nil)
        expect_status_details(403)
      end
    end
  end
end
