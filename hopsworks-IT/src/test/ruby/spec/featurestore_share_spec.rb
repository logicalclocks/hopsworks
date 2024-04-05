=begin
 This file is part of Hopsworks
 Copyright (C) 2024, Hopsworks AB. All rights reserved

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
  after(:all) {clean_all_test_projects(spec: "featurestore_share")}
  describe 'featurestore' do
    describe "shared permissions" do
      before :all do
        # Create users
        @user1_params = {email: "user1_#{random_id}@email.com", first_name: "User", last_name: "1", password: "Pass123"}
        @user1 = create_user(@user1_params)
        pp "user email: #{@user1[:email]}" if defined?(@debugOpt) && @debugOpt
        @user2_params = {email: "user2_#{random_id}@email.com", first_name: "User", last_name: "2", password: "Pass123"}
        @user2 = create_user(@user2_params)
        pp "user email: #{@user2[:email]}" if defined?(@debugOpt) && @debugOpt
        @user_data_scientist_params = {email: "data_scientist_#{random_id}@email.com", first_name: "User", last_name: "data_scientist", password: "Pass123"}
        @user_data_scientist = create_user(@user_data_scientist_params)
        pp "user email: #{@user_data_scientist[:email]}" if defined?(@debugOpt) && @debugOpt
        @user_data_owner_params = {email: "data_owner_#{random_id}@email.com", first_name: "User", last_name: "data_owner", password: "Pass123"}
        @user_data_owner = create_user(@user_data_owner_params)
        pp "user email: #{@user_data_owner[:email]}" if defined?(@debugOpt) && @debugOpt

        # Create base project
        create_session(@user1[:email], @user1_params[:password])
        @project1 = create_project
        pp @project1[:projectname] if defined?(@debugOpt) && @debugOpt
        with_jdbc_connector(@project1[:id])

        # Create shared with projects
        create_session(@user2[:email], @user2_params[:password])
        @project_read_only = create_project
        pp @project_read_only[:projectname] if defined?(@debugOpt) && @debugOpt

        @featurestore_id = get_featurestore_id(@project_read_only[:id])
        json_result, _ = create_cached_featuregroup(@project_read_only[:id], @featurestore_id)
        expect_status_details(201)
        @cached_feature_group = JSON.parse(json_result)
        create_statistics_commit_fg(@project_read_only[:id], @featurestore_id, @cached_feature_group["id"])

        expectation_suite = generate_template_expectation_suite()
        @expectation_dto = create_expectation_suite(@project_read_only[:id], @featurestore_id,
                                                    @cached_feature_group["id"], expectation_suite)

        # Add members to projects
        add_member_to_project(@project_read_only, @user_data_owner_params[:email], "Data owner")
        add_member_to_project(@project_read_only, @user_data_scientist_params[:email], "Data scientist")

        # Share projects
        create_session(@user1[:email], @user1_params[:password])
        share_dataset_checked(@project1, "#{@project1[:projectname].downcase}_featurestore.db", @project_read_only[:projectname], datasetType: "FEATURESTORE")

        # Accept shared projects
        create_session(@user2[:email], @user2_params[:password])
        accept_dataset_checked(@project_read_only, "#{@project1[:projectname]}::#{@project1[:projectname].downcase}_featurestore.db", datasetType: "FEATURESTORE")
      end

      context "shared cached feature group permissions" do

        # create

        it 'data owner should not be able to create fg with read only permission' do
          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          fs = get_featurestore(@project_read_only[:id], fs_project_id: @project1[:id])
          json_result, _ = create_cached_featuregroup(@project_read_only[:id], fs["featurestoreId"], online:true)
          expect_status_details(403)
        end

        it 'data scientist should not be able to create fg with read only permission' do
          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          fs = get_featurestore(@project_read_only[:id], fs_project_id: @project1[:id])
          json_result, _ = create_cached_featuregroup(@project_read_only[:id], fs["featurestoreId"], online:true)
          expect_status_details(403)
        end

        # get

        it 'data owner should be able to get fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          _, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          expect_status_details(201)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          get_featuregroup_checked(@project_read_only[:id], fg_name, fs_id: fs["featurestoreId"])
        end

        it 'data scientist should be able to get fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          _, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          expect_status_details(201)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          get_featuregroup_checked(@project_read_only[:id], fg_name, fs_id: fs["featurestoreId"])
        end

        # update

        it 'data owner should not be able to update fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, _ = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          expect_status_details(201)
          fg = JSON.parse(json_result, :symbolize_names => true)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          new_description = "changed description"
          update_cached_featuregroup_metadata(@project_read_only[:id], fs["featurestoreId"], fg[:id], fg[:version], description: new_description)
          expect_status_details(403)

          new_fg = get_featuregroup_checked(@project_read_only[:id], fg[:name], fs_id: fs["featurestoreId"])
          expect(new_fg.length).to be 1
          new_fg = new_fg[0]
          expect(new_fg["description"]).not_to eql(new_description)
        end

        it 'data scientist should not be able to update fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, _ = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          expect_status_details(201)
          fg = JSON.parse(json_result, :symbolize_names => true)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          new_description = "changed description"
          update_cached_featuregroup_metadata(@project_read_only[:id], fs["featurestoreId"], fg[:id], fg[:version], description: new_description)
          expect_status_details(403)

          new_fg = get_featuregroup_checked(@project_read_only[:id], fg[:name], fs_id: fs["featurestoreId"])
          expect(new_fg.length).to be 1
          new_fg = new_fg[0]
          expect(new_fg["description"]).not_to eql(new_description)
        end

        # delete

        it 'data owner should not be able to delete fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, _ = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          expect_status_details(201)
          fg = JSON.parse(json_result, :symbolize_names => true)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          delete_featuregroup(@project_read_only[:id], fs["featurestoreId"], fg[:id])
          expect_status_details(403)

          get_featuregroup_checked(@project_read_only[:id], fg[:name], fs_id: fs["featurestoreId"])
        end

        it 'data scientist should not be able to delete fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, _ = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          expect_status_details(201)
          fg = JSON.parse(json_result, :symbolize_names => true)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          delete_featuregroup(@project_read_only[:id], fs["featurestoreId"], fg[:id])
          expect_status_details(403)

          get_featuregroup_checked(@project_read_only[:id], fg[:name], fs_id: fs["featurestoreId"])
        end
      end

      context "shared stream feature group permission" do

        # create

        it 'data owner should not be able to create fg with read only permission' do
          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          fs = get_featurestore(@project_read_only[:id], fs_project_id: @project1[:id])
          json_result, _ = create_stream_featuregroup(@project_read_only[:id], fs["featurestoreId"])
          expect_status_details(403)
        end

        it 'data scientist should not be able to create fg with read only permission' do
          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          fs = get_featurestore(@project_read_only[:id], fs_project_id: @project1[:id])
          json_result, _ = create_stream_featuregroup(@project_read_only[:id], fs["featurestoreId"])
          expect_status_details(403)
        end

        # get

        it 'data owner should be able to get fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          _, fg_name = create_stream_featuregroup(@project1[:id], fs["featurestoreId"])
          expect_status_details(201)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          get_featuregroup_checked(@project_read_only[:id], fg_name, fs_id: fs["featurestoreId"])
        end

        it 'data scientist should be able to get fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          _, fg_name = create_stream_featuregroup(@project1[:id], fs["featurestoreId"])
          expect_status_details(201)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          get_featuregroup_checked(@project_read_only[:id], fg_name, fs_id: fs["featurestoreId"])
        end

        # update

        it 'data owner should not be able to update fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, _ = create_stream_featuregroup(@project1[:id], fs["featurestoreId"])
          expect_status_details(201)
          fg = JSON.parse(json_result, :symbolize_names => true)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          new_description = "changed description"
          update_stream_featuregroup_metadata(@project_read_only[:id], fs["featurestoreId"], fg[:id], fg[:version], description: new_description)
          expect_status_details(403)

          new_fg = get_featuregroup_checked(@project_read_only[:id], fg[:name], fs_id: fs["featurestoreId"])
          expect(new_fg.length).to be 1
          new_fg = new_fg[0]
          expect(new_fg["description"]).not_to eql(new_description)
        end

        it 'data scientist should not be able to update fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, _ = create_stream_featuregroup(@project1[:id], fs["featurestoreId"])
          expect_status_details(201)
          fg = JSON.parse(json_result, :symbolize_names => true)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          new_description = "changed description"
          update_stream_featuregroup_metadata(@project_read_only[:id], fs["featurestoreId"], fg[:id], fg[:version], description: new_description)
          expect_status_details(403)

          new_fg = get_featuregroup_checked(@project_read_only[:id], fg[:name], fs_id: fs["featurestoreId"])
          expect(new_fg.length).to be 1
          new_fg = new_fg[0]
          expect(new_fg["description"]).not_to eql(new_description)
        end

        # delete

        it 'data owner should not be able to delete fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, _ = create_stream_featuregroup(@project1[:id], fs["featurestoreId"])
          expect_status_details(201)
          fg = JSON.parse(json_result, :symbolize_names => true)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          delete_featuregroup(@project_read_only[:id], fs["featurestoreId"], fg[:id])
          expect_status_details(403)

          get_featuregroup_checked(@project_read_only[:id], fg[:name], fs_id: fs["featurestoreId"])
        end

        it 'data scientist should not be able to delete fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, _ = create_stream_featuregroup(@project1[:id], fs["featurestoreId"])
          expect_status_details(201)
          fg = JSON.parse(json_result, :symbolize_names => true)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          delete_featuregroup(@project_read_only[:id], fs["featurestoreId"], fg[:id])
          expect_status_details(403)

          get_featuregroup_checked(@project_read_only[:id], fg[:name], fs_id: fs["featurestoreId"])
        end

        # get subject

        it 'data owner should be able to get fg schema' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, _ = create_stream_featuregroup(@project1[:id], fs["featurestoreId"])
          expect_status_details(201)
          fg = JSON.parse(json_result, :symbolize_names => true)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          get_featurestore_subject_details(@project_read_only, fs["featurestoreId"], "#{fg[:name]}_#{fg[:version]}", 1)
          expect_status_details(200)
        end

        it 'data scientist should be able to get fg schema' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, _ = create_stream_featuregroup(@project1[:id], fs["featurestoreId"])
          expect_status_details(201)
          fg = JSON.parse(json_result, :symbolize_names => true)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          get_featurestore_subject_details(@project_read_only, fs["featurestoreId"], "#{fg[:name]}_#{fg[:version]}", 1)
          expect_status_details(200)
        end

        it 'user should not be able to get fg schema from project that is not shared' do
          create_session(@user2[:email], @user2_params[:password])
          fs = get_featurestore(@project_read_only[:id])
          json_result, _ = create_stream_featuregroup(@project_read_only[:id], fs["featurestoreId"])
          expect_status_details(201)
          fg = JSON.parse(json_result, :symbolize_names => true)

          create_session(@user1[:email], @user1_params[:password])
          get_featurestore_subject_details(@project1, fs["featurestoreId"], "#{fg[:name]}_#{fg[:version]}", 1)
          expect_status_details(404)
        end
      end

      context "shared on demand feature group permission" do

        # create

        it 'data owner should not be able to create fg with read only permission' do
          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          fs = get_featurestore(@project_read_only[:id], fs_project_id: @project1[:id])
          connector_id = get_jdbc_connector_id
          create_on_demand_featuregroup(@project_read_only[:id], fs["featurestoreId"], connector_id)
          expect_status_details(403)
        end

        it 'data scientist should not be able to create fg with read only permission' do
          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          fs = get_featurestore(@project_read_only[:id], fs_project_id: @project1[:id])
          connector_id = get_jdbc_connector_id
          create_on_demand_featuregroup(@project_read_only[:id], fs["featurestoreId"], connector_id)
          expect_status_details(403)
        end

        # get

        it 'data owner should be able to get fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          connector_id = get_jdbc_connector_id
          _, fg_name = create_on_demand_featuregroup(@project1[:id], fs["featurestoreId"], connector_id)
          expect_status_details(201)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          get_featuregroup_checked(@project_read_only[:id], fg_name, fs_id: fs["featurestoreId"])
        end

        it 'data scientist should be able to get fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          connector_id = get_jdbc_connector_id
          _, fg_name = create_on_demand_featuregroup(@project1[:id], fs["featurestoreId"], connector_id)
          expect_status_details(201)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          get_featuregroup_checked(@project_read_only[:id], fg_name, fs_id: fs["featurestoreId"])
        end

        # update

        it 'data owner should not be able to update fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          connector_id = get_jdbc_connector_id
          json_result, _ = create_on_demand_featuregroup(@project1[:id], fs["featurestoreId"], connector_id)
          expect_status_details(201)
          fg = JSON.parse(json_result, :symbolize_names => true)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          new_description = "changed description"
          update_on_demand_featuregroup(@project_read_only[:id], fs["featurestoreId"], connector_id, fg[:id], fg[:version], featuregroup_desc: new_description)
          expect_status_details(403)

          new_fg = get_featuregroup_checked(@project_read_only[:id], fg[:name], fs_id: fs["featurestoreId"])
          expect(new_fg.length).to be 1
          new_fg = new_fg[0]
          expect(new_fg["description"]).not_to eql(new_description)
        end

        it 'data scientist should not be able to update fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          connector_id = get_jdbc_connector_id
          json_result, _ = create_on_demand_featuregroup(@project1[:id], fs["featurestoreId"], connector_id)
          expect_status_details(201)
          fg = JSON.parse(json_result, :symbolize_names => true)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          new_description = "changed description"
          update_on_demand_featuregroup(@project_read_only[:id], fs["featurestoreId"], connector_id, fg[:id], fg[:version], featuregroup_desc: new_description)
          expect_status_details(403)

          new_fg = get_featuregroup_checked(@project_read_only[:id], fg[:name], fs_id: fs["featurestoreId"])
          expect(new_fg.length).to be 1
          new_fg = new_fg[0]
          expect(new_fg["description"]).not_to eql(new_description)
        end

        # delete

        it 'data owner should not be able to delete fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          connector_id = get_jdbc_connector_id
          json_result, _ = create_on_demand_featuregroup(@project1[:id], fs["featurestoreId"], connector_id)
          expect_status_details(201)
          fg = JSON.parse(json_result, :symbolize_names => true)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          delete_featuregroup(@project_read_only[:id], fs["featurestoreId"], fg[:id])
          expect_status_details(403)

          get_featuregroup_checked(@project_read_only[:id], fg[:name], fs_id: fs["featurestoreId"])
        end

        it 'data scientist should not be able to delete fg with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          connector_id = get_jdbc_connector_id
          json_result, _ = create_on_demand_featuregroup(@project1[:id], fs["featurestoreId"], connector_id)
          expect_status_details(201)
          fg = JSON.parse(json_result, :symbolize_names => true)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          delete_featuregroup(@project_read_only[:id], fs["featurestoreId"], fg[:id])
          expect_status_details(403)

          get_featuregroup_checked(@project_read_only[:id], fg[:name], fs_id: fs["featurestoreId"])
        end
      end

      context "shared training dataset permissions" do

        # create

        it 'data owner should not be able to create td with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          connector = get_hopsfs_training_datasets_connector(@project1[:projectname])

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          json_result, training_dataset_name = create_hopsfs_training_dataset(@project_read_only[:id], fs["featurestoreId"], connector)
          expect_status_details(403)
        end

        it 'data scientist should not be able to create td with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          connector = get_hopsfs_training_datasets_connector(@project1[:projectname])

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          json_result, training_dataset_name = create_hopsfs_training_dataset(@project_read_only[:id], fs["featurestoreId"], connector)
          expect_status_details(403)
        end

        # get

        it 'data owner should be able to get td with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          connector = get_hopsfs_training_datasets_connector(@project1[:projectname])

          json_result, training_dataset_name = create_hopsfs_training_dataset(@project1[:id], fs["featurestoreId"], connector)
          expect_status_details(201)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          get_training_datasets_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project_read_only[:id]}/featurestores/#{fs["featurestoreId"]}/trainingdatasets/#{training_dataset_name}"
          get get_training_datasets_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status_details(200)
          expect(parsed_json.size).to eq 1
        end

        it 'data scientist should be able to get td with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          connector = get_hopsfs_training_datasets_connector(@project1[:projectname])

          json_result, training_dataset_name = create_hopsfs_training_dataset(@project1[:id], fs["featurestoreId"], connector)
          expect_status_details(201)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          get_training_datasets_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project_read_only[:id]}/featurestores/#{fs["featurestoreId"]}/trainingdatasets/#{training_dataset_name}"
          get get_training_datasets_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status_details(200)
          expect(parsed_json.size).to eq 1
        end

        # update

        it 'data owner should not be able to update td with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          connector = get_hopsfs_training_datasets_connector(@project1[:projectname])

          json_result, training_dataset_name = create_hopsfs_training_dataset(@project1[:id], fs["featurestoreId"], connector)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          json_result2 = update_hopsfs_training_dataset_metadata(@project_read_only[:id], fs["featurestoreId"], parsed_json["id"], "tfrecords", connector)
          expect_status_details(403)
        end

        it 'data scientist should not be able to update td with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          connector = get_hopsfs_training_datasets_connector(@project1[:projectname])

          json_result, training_dataset_name = create_hopsfs_training_dataset(@project1[:id], fs["featurestoreId"], connector)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          json_result2 = update_hopsfs_training_dataset_metadata(@project_read_only[:id], fs["featurestoreId"], parsed_json["id"], "tfrecords", connector)
          expect_status_details(403)
        end

        # delete

        it 'data owner should not be able to delete td with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          connector = get_hopsfs_training_datasets_connector(@project1[:projectname])

          json_result, training_dataset_name = create_hopsfs_training_dataset(@project1[:id], fs["featurestoreId"], connector)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          delete_training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + @project_read_only["id"].to_s +
            "/featurestores/" + fs["featurestoreId"].to_s + "/trainingdatasets/" + parsed_json["id"].to_s
          delete delete_training_dataset_endpoint
          expect_status_details(403)

          # make sure inode is deleted
          path = "/Projects/#{@project1['projectname']}/#{@project1['projectname']}_Training_Datasets/#{training_dataset_name}_1"
          expect(test_dir(path)).to be true
        end

        it 'data scientist should not be able to delete td with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          connector = get_hopsfs_training_datasets_connector(@project1[:projectname])

          json_result, training_dataset_name = create_hopsfs_training_dataset(@project1[:id], fs["featurestoreId"], connector)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          delete_training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + @project_read_only["id"].to_s +
            "/featurestores/" + fs["featurestoreId"].to_s + "/trainingdatasets/" + parsed_json["id"].to_s
          delete delete_training_dataset_endpoint
          expect_status_details(403)

          # make sure inode is deleted
          path = "/Projects/#{@project1['projectname']}/#{@project1['projectname']}_Training_Datasets/#{training_dataset_name}_1"
          expect(test_dir(path)).to be true
        end
      end

      context "shared feature view permissions" do

        # create

        it 'data owner should not be able to create fv with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          create_feature_view_from_feature_group(@project_read_only[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(403)
        end

        it 'data scientist should not be able to create fv with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          create_feature_view_from_feature_group(@project_read_only[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(403)
        end

        # get

        it 'data owner should be able to get fv with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          json_result = create_feature_view_from_feature_group(@project1[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          json_result = get_feature_view_by_name_and_version(@project_read_only[:id], fs["featurestoreId"], parsed_json["name"], parsed_json["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)
        end

        it 'data scientist should be able to get fv with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          json_result = create_feature_view_from_feature_group(@project1[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          json_result = get_feature_view_by_name_and_version(@project_read_only[:id], fs["featurestoreId"], parsed_json["name"], parsed_json["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)
        end

        # update

        it 'data owner should not be able to update fv with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          json_result = create_feature_view_from_feature_group(@project1[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          description = parsed_json["description"]

          json_data = {
            name: "new_testfeatureviewname",
            version: parsed_json["version"],
            type: "featureViewDTO",
            description: "temp desc"
          }
          json_result = update_feature_view(@project_read_only[:id], fs["featurestoreId"], json_data, parsed_json["name"], parsed_json["version"])
          expect_status_details(403)

          json_result = get_feature_view_by_name_and_version(@project_read_only[:id], fs["featurestoreId"], parsed_json["name"], parsed_json["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)
          expect(parsed_json["description"]).to eql(description)
        end

        it 'data scientist should not be able to update fv with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          json_result = create_feature_view_from_feature_group(@project1[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          description = parsed_json["description"]

          json_data = {
            name: "new_testfeatureviewname",
            version: parsed_json["version"],
            type: "featureViewDTO",
            description: "temp desc"
          }
          json_result = update_feature_view(@project_read_only[:id], fs["featurestoreId"], json_data, parsed_json["name"], parsed_json["version"])
          expect_status_details(403)

          json_result = get_feature_view_by_name_and_version(@project_read_only[:id], fs["featurestoreId"], parsed_json["name"], parsed_json["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)
          expect(parsed_json["description"]).to eql(description)
        end

        # delete

        it 'data owner should not be able to delete fv with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          json_result = create_feature_view_from_feature_group(@project1[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          delete_feature_view(@project_read_only["id"], parsed_json["name"], feature_store_id: fs["featurestoreId"], expected_status: 403)

          json_result = get_feature_view_by_name_and_version(@project_read_only[:id], fs["featurestoreId"], parsed_json["name"], parsed_json["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)
        end

        it 'data scientist should not be able to delete fv with read only permission' do
          create_session(@user1[:email], @user1_params[:password])
          fs = get_featurestore(@project1[:id])
          json_result, fg_name = create_cached_featuregroup(@project1[:id], fs["featurestoreId"], online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          json_result = create_feature_view_from_feature_group(@project1[:id], fs["featurestoreId"], parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          delete_feature_view(@project_read_only["id"], parsed_json["name"], feature_store_id: fs["featurestoreId"], expected_status: 403)

          json_result = get_feature_view_by_name_and_version(@project_read_only[:id], fs["featurestoreId"], parsed_json["name"], parsed_json["version"])
          parsed_json = JSON.parse(json_result)
          expect_status_details(200)
        end
      end

      context "shared statistics dataset permissions" do
        # get
        it 'data owner should be able to get statistics with read only permission' do
          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          json_result = get_statistics_commit_fg(@project_read_only[:id], @featurestore_id, @cached_feature_group["id"])
          expect_status_details(200)
        end

        it 'data scientist should be able to get statistics with read only permission' do
          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          json_result = get_statistics_commit_fg(@project_read_only[:id], @featurestore_id, @cached_feature_group["id"])
          expect_status_details(200)
        end

        it "should get stat of a shared statistics dataset" do
          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          get_dataset_stat(@project_read_only, "/Projects/#{@project1[:projectname]}/Statistics")
          expect_status_details(200)
        end

        it "should fail to create dir in a shared statistics dataset" do
          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          create_dir(@project_read_only, "/Projects/#{@project1[:projectname]}/Statistics/test")
          expect_status_details(403, error_code: 200002)
        end

      end

      context "shared validation dataset permissions" do
        # get
        it 'data owner should be able to get validation with read only permission' do
          create_session(@user_data_owner[:email], @user_data_owner_params[:password])
          json_result = get_expectation_suite(@project_read_only[:id], @featurestore_id, @cached_feature_group["id"])
          expect_status_details(200)
        end

        it 'data scientist should be able to get validation with read only permission' do
          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          json_result = get_expectation_suite(@project_read_only[:id], @featurestore_id, @cached_feature_group["id"])
          expect_status_details(200)
        end

        it "should get stat of a shared DataValidation dataset" do
          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          get_dataset_stat(@project_read_only, "/Projects/#{@project1[:projectname]}/DataValidation")
          expect_status_details(200)
        end

        it "should fail to create dir in a shared DataValidation dataset" do
          create_session(@user_data_scientist[:email], @user_data_scientist_params[:password])
          create_dir(@project_read_only, "/Projects/#{@project1[:projectname]}/DataValidation/test")
          expect_status_details(403, error_code: 200002)
        end

      end
    end
  end
end