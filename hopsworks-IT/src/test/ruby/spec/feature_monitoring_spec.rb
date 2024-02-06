# This file is part of Hopsworks
# Copyright (C) 2024, Hopsworks AB. All rights reserved
#
# Hopsworks is free software: you can redistribute it and/or modify it under the terms of
# the GNU Affero General Public License as published by the Free Software Foundation,
# either version 3 of the License, or (at your option) any later version.
#
# Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
# without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
# PURPOSE.  See the GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License along with this program.
# If not, see <https://www.gnu.org/licenses/>.
#

describe "On #{ENV['OS']}" do

  before :all do
    # ensure feature monitoring is enabled
    @enable_feature_monitoring = getVar('enable_feature_monitoring')
    setVar('enable_feature_monitoring', "true")
  end

  after :all do
    # revert feature monitoring flag
    setVar('enable_feature_monitoring', @enable_feature_monitoring[:value])
    clean_all_test_projects(spec: "feature_monitoring_persistence")
  end

  describe 'feature monitoring persistence' do

    describe "Create feature monitoring configuration for Feature Groups" do
      context 'with valid project, featurestore, feature_group and feature_view' do
        before :all do
          with_valid_project
          @featurestore_id = get_featurestore_id(@project[:id])
          fg_dto, featuregroup_name = create_cached_featuregroup(@project[:id], @featurestore_id)
          expect_status_details(201)
          @fg_json = JSON.parse(fg_dto)
          expect_status_details(201)
          fv_dto = create_feature_view_from_feature_group(@project[:id], @featurestore_id, @fg_json)
          @fv_json = JSON.parse(fv_dto)
          expect_status_details(201)
          @template_config_name_fg = "test_config"
          @job_name_fg = "#{@fg_json['name']}_1_#{@template_config_name_fg}_run_feature_monitoring"
          @template_stats_config_name_fg = "test_stats_config"
          @job_stats_name_fg = "#{@fg_json['name']}_1_#{@template_stats_config_name_fg}_run_feature_monitoring"
          @template_config_name_fv = "test_config"
          @job_name_fv = "#{@fv_json['name']}_1_#{@template_config_name_fv}_run_feature_monitoring"
          @template_stats_config_name_fv = "test_stats_config"
          @job_stats_name_fv = "#{@fv_json['name']}_1_#{@template_stats_config_name_fv}_run_feature_monitoring"
        end

        it "should create a feature monitoring config attached to a featuregroup" do
          config = generate_template_feature_monitoring_config(\
            @featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true)
          config_res = create_feature_monitoring_configuration_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], config)
          expect_status_details(201)
          config_json = JSON.parse(config_res)
          expect_feature_monitoring_equal(\
            config_json, config, @job_name_fg, @template_config_name_fg, fg_id: @fg_json["id"])
        end

        it "should create a feature monitoring config attached to a feature view" do
          config = generate_template_feature_monitoring_config(
            @featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"], false)
          config_res = create_feature_monitoring_configuration_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], config)
          expect_status_details(201)
          config_json = JSON.parse(config_res)
          expect_feature_monitoring_equal(\
            config_json, config, @job_name_fv, @template_config_name_fv, fg_id: nil, fv_name: @fv_json["name"], fv_version: @fv_json["version"])
        end

        it "should create a stats only feature monitoring config attached to a featuregroup" do
          config = generate_template_stats_monitoring_config(\
            @featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true)
          config_res = create_feature_monitoring_configuration_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], config)
          expect_status_details(201)
          config_json = JSON.parse(config_res)
          expect_feature_monitoring_equal(\
            config_json, config, @job_stats_name_fg, @template_stats_config_name_fg, fg_id: @fg_json["id"])
        end

        it "should create a stats only feature monitoring config attached to a feature view" do
          config = generate_template_stats_monitoring_config(
            @featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"], false)
          config_res = create_feature_monitoring_configuration_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], config)
          expect_status_details(201)
          config_json = JSON.parse(config_res)
          expect_feature_monitoring_equal(\
            config_json, config, @job_stats_name_fv, @template_stats_config_name_fv, fg_id: nil, fv_name: @fv_json["name"], fv_version: @fv_json["version"])
        end
      end
    end

    describe "Retrieve, Update and Delete feature monitoring configuration for Feature Group and Feature View" do
      context 'with valid project, featurestore, feature_group, feature_view and corresponding feature monitoring configs' do
        before :all do
          with_valid_project
          @featurestore_id = get_featurestore_id(@project[:id])
          fg_dto, featuregroup_name = create_cached_featuregroup(@project[:id], @featurestore_id, online:true)
          expect_status_details(201)
          @fg_json = JSON.parse(fg_dto)
          expect_status_details(201)
          fv_dto = create_feature_view_from_feature_group(@project[:id], @featurestore_id, @fg_json)
          @fv_json = JSON.parse(fv_dto)
          expect_status_details(201)
          @feature_name = "testfeature"
          @template_config_name_fg = "test_config"
          @job_name_fg = "#{@fg_json['name']}_1_#{@template_config_name_fg}_run_feature_monitoring"
          @template_config_name_fv = "test_config"
          @job_name_fv = "#{@fv_json['name']}_1_#{@template_config_name_fv}_run_feature_monitoring"
          # Create a feature monitoring config for both featuregroup and feature view
          config = generate_template_feature_monitoring_config(
            @featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"],  false)
          config_res = create_feature_monitoring_configuration_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], config)
          expect_status_details(201)
          config = generate_template_feature_monitoring_config(\
            @featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true)
          config_res = create_feature_monitoring_configuration_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], config)
          expect_status_details(201)
        end


        it "should fetch a list of feature monitoring config attached to a featuregroup via feature name" do
          config = generate_template_feature_monitoring_config(\
            @featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true)
          fetched_configs = get_feature_monitoring_configuration_by_feature_name_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], @feature_name)
          expect_status_details(200)
          config_json = JSON.parse(fetched_configs)
          expect(config_json.key?("count")).to be true
          expect(config_json["count"] == 1).to be true
          expect(config_json.key?("items")).to be true
          expect(config_json["items"].length == 1).to be true
          expect_feature_monitoring_equal(\
            config_json["items"][0], config, @job_name_fg, @template_config_name_fg, fg_id: @fg_json["id"])
        end

        it "should fetch a list of feature monitoring config attached to a featuregroup" do
          config = generate_template_feature_monitoring_config(\
            @featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true)
          fetched_configs = get_feature_monitoring_configuration_by_entity_fg(\
            @project[:id], @featurestore_id, @fg_json["id"])
          expect_status_details(200)
          config_json = JSON.parse(fetched_configs)
          expect(config_json.key?("count")).to be true
          expect(config_json["count"] == 1).to be true
          expect(config_json.key?("items")).to be true
          expect(config_json["items"].length == 1).to be true
          expect_feature_monitoring_equal(\
            config_json["items"][0], config, @job_name_fg, @template_config_name_fg, fg_id: @fg_json["id"])
        end

        it "should fetch a feature monitoring config attached to a featuregroup via its name" do
          config = generate_template_feature_monitoring_config(\
            @featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true)
          fetched_config_by_name = get_feature_monitoring_configuration_by_name_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], @template_config_name_fg)
          expect_status_details(200)
          config_json = JSON.parse(fetched_config_by_name)
          expect_feature_monitoring_equal(\
            config_json, config, @job_name_fg, @template_config_name_fg, fg_id: @fg_json["id"])
        end

        it "should trigger the execution of the job associated to a feature monitoring config" do
          fetched_config_by_name = get_feature_monitoring_configuration_by_name_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], @template_config_name_fg)
          expect_status_details(200)
          config_json = JSON.parse(fetched_config_by_name)
          execution_res = start_execution(@project[:id], config_json["jobName"])
          execution_json = JSON.parse(execution_res)
          expect(execution_json.has_key?("id")).to eq(true)
          args = execution_json["args"]
          expect(args[-11,args.length]).to eql("config.json")
          expect(args[0,26]).to eql("-op run_feature_monitoring")
          expect(args[args.length - 12 - config_json['jobName'].length..args.length-13]).to eql(config_json['jobName'])
        end

        it "should fetch a feature monitoring config attached to a featuregroup via config id" do
          config = generate_template_feature_monitoring_config(\
            @featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true)
          fetched_config_by_name = get_feature_monitoring_configuration_by_name_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], @template_config_name_fg)
          expect_status_details(200)
          json_config_by_name = JSON.parse(fetched_config_by_name)
          config_id = json_config_by_name["id"]
          fetched_config_by_id = get_feature_monitoring_configuration_by_id_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], config_id)
          expect_status_details(200)
          config_json = JSON.parse(fetched_config_by_id)
          expect_feature_monitoring_equal(\
            config_json, config, @job_name_fg, @template_config_name_fg, fg_id: @fg_json["id"])
        end

        it "should update the threshold of the feature monitoring config" do
          config = generate_template_feature_monitoring_config(\
            @featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true)
          fetched_config_by_name = get_feature_monitoring_configuration_by_name_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], @template_config_name_fg)
          expect_status_details(200)
          json_config = JSON.parse(fetched_config_by_name)
          threshold = config[:statisticsComparisonConfig][:threshold] + 1
          config[:statisticsComparisonConfig][:threshold] = threshold
          config[:statisticsComparisonConfig][:strict] = false
          config[:jobSchedule][:id] = json_config["jobSchedule"]["id"]
          config[:jobSchedule][:cronExpression] = "0 10 * ? * * *"
          config[:jobSchedule][:enabled] = false
          config[:description] = "updated description"
          # job_name is not editable but should be set by backend
          config[:jobName] = @job_name_fg
          config[:id] = json_config["id"]
          updated_config_res = update_feature_monitoring_configuration_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], config)
          expect_status_details(200)
          updated_config_json = JSON.parse(updated_config_res)
          expect(updated_config_json["statisticsComparisonConfig"]["threshold"]).to be > (threshold - 0.01)
          expect_partial_feature_monitoring_equal(\
            updated_config_json, config, @job_name_fg, @template_config_name_fg, fg_id: @fg_json["id"])
          expect_window_config_equal(updated_config_json["detectionWindowConfig"], config[:detectionWindowConfig])
          expect_window_config_equal(updated_config_json["referenceWindowConfig"], config[:referenceWindowConfig])
          expect_monitoring_scheduler_equal(updated_config_json["jobSchedule"], config[:jobSchedule])
          expect_stats_comparison_equal(updated_config_json["statisticsComparisonConfig"], config[:statisticsComparisonConfig], threshold: threshold)
        end

        it "should fail to delete a job attached to a feature monitoring config" do
          delete_job(@project[:id], @job_name_fg, expected_status: 400, error_code: 130012)
        end

        it "should delete a feature monitoring config attached to a Feature Group" do
          fetched_config_by_name = get_feature_monitoring_configuration_by_name_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], @template_config_name_fg)
          expect_status_details(200)
          json_config_by_name = JSON.parse(fetched_config_by_name)
          id = json_config_by_name["id"]
          delete_feature_monitoring_configuration_by_id_fg(@project[:id], @featurestore_id, @fg_json["id"], id)
          expect_status_details(204)
        end

        it "should fetch a feature monitoring config attached to a feature view via feature name" do
          config = generate_template_feature_monitoring_config(
            @featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"], false)
          fetched_configs = get_feature_monitoring_configuration_by_feature_name_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], @feature_name)
          expect_status_details(200)
          config_json = JSON.parse(fetched_configs)["items"][0]
          expect_feature_monitoring_equal(\
            config_json, config, @job_name_fv, @template_config_name_fv, fg_id: nil, fv_name: @fv_json["name"], fv_version: @fv_json["version"])
        end

        it "should fetch a feature monitoring config attached to a feature view" do
          config = generate_template_feature_monitoring_config(
            @featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"], false)
          fetched_configs = get_feature_monitoring_configuration_by_entity_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"])
          expect_status_details(200)
          config_json = JSON.parse(fetched_configs)["items"][0]
          expect_feature_monitoring_equal(\
            config_json, config, @job_name_fv, @template_config_name_fv, fg_id: nil, fv_name: @fv_json["name"], fv_version: @fv_json["version"])
        end

        it "should fetch a feature monitoring config attached to a feature view via its name" do
          config = generate_template_feature_monitoring_config(\
            @featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"], false)
          fetched_config = get_feature_monitoring_configuration_by_name_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], @template_config_name_fv)
          expect_status_details(200)
          config_json = JSON.parse(fetched_config)
          expect_feature_monitoring_equal(\
            config_json, config, @job_name_fv, @template_config_name_fv, fg_id: nil, fv_name: @fv_json["name"], fv_version: @fv_json["version"])
        end

        it "should trigger the job associated to a feature monitoring config" do
          fetched_config_by_name = get_feature_monitoring_configuration_by_name_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], @template_config_name_fv)
          expect_status_details(200)
          config_json = JSON.parse(fetched_config_by_name)
          execution_res = start_execution(@project[:id], config_json["jobName"])
          execution_json = JSON.parse(execution_res)
          expect(execution_json.has_key?("id")).to eq(true)
          args = execution_json["args"]
          expect(args[-11,args.length]).to eql("config.json")
          expect(args[0,26]).to eql("-op run_feature_monitoring")
          expect(args[args.length - 12 - config_json['jobName'].length..args.length-13]).to eql(config_json['jobName'])
        end

        it "should fetch a feature monitoring config attached to a feature view via config id" do
          config = generate_template_feature_monitoring_config(
            @featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"], false)
          fetched_config_by_feature_name = get_feature_monitoring_configuration_by_feature_name_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], @feature_name)
          expect_status_details(200)
          json_config_by_feature_name = JSON.parse(fetched_config_by_feature_name)
          config_id = json_config_by_feature_name["items"][0]["id"]
          fetched_config_by_id = get_feature_monitoring_configuration_by_id_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], config_id)
          expect_status_details(200)
          config_json = JSON.parse(fetched_config_by_id)
          expect_feature_monitoring_equal(\
            config_json, config, @job_name_fv, @template_config_name_fv, fg_id: nil, fv_name: @fv_json["name"], fv_version: @fv_json["version"])
        end

        it "should update the threshold of the feature monitoring config attached to a feature view" do
          config = generate_template_feature_monitoring_config(
            @featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"], false)
          fetched_config_by_name = get_feature_monitoring_configuration_by_name_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], @template_config_name_fv)
          expect_status_details(200)
          json_config = JSON.parse(fetched_config_by_name)
          threshold = config[:statisticsComparisonConfig][:threshold] + 1
          config[:statisticsComparisonConfig][:threshold] = threshold
          config[:statisticsComparisonConfig][:strict] = false
          config[:jobSchedule][:id] = json_config["jobSchedule"]["id"]
          config[:jobSchedule][:cronExpression] = "0 10 * ? * * *"
          config[:jobSchedule][:enabled] = false
          config[:description] = "updated description"
          # job_name is not editable but should be set by backend
          config[:jobName] = @job_name_fv
          config[:id] = json_config["id"]
          updated_config_res = update_feature_monitoring_configuration_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], config)
          expect_status_details(200)
          updated_config_json = JSON.parse(updated_config_res)
          expect(updated_config_json["statisticsComparisonConfig"]["threshold"]).to be > (threshold - 0.01)
          expect_partial_feature_monitoring_equal(\
            updated_config_json, config, @job_name_fv, @template_config_name_fv, fg_id: nil, fv_name: @fv_json["name"], fv_version: @fv_json["version"])
          expect_monitoring_scheduler_equal(updated_config_json["jobSchedule"], config[:jobSchedule])
          expect_window_config_equal(updated_config_json["detectionWindowConfig"], config[:detectionWindowConfig])
          expect_window_config_equal(updated_config_json["referenceWindowConfig"], config[:referenceWindowConfig])
          expect_stats_comparison_equal(updated_config_json["statisticsComparisonConfig"], config[:statisticsComparisonConfig], threshold: threshold)
        end

        it "should delete a feature monitoring config attached to a Feature View" do
          feature_name = "testfeature"
          fetched_config_by_name = get_feature_monitoring_configuration_by_name_fv(
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], @template_config_name_fv)
          expect_status_details(200)
          json_config_by_name = JSON.parse(fetched_config_by_name)
          id = json_config_by_name["id"]
          delete_feature_monitoring_configuration_by_id_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], id)
          expect_status_details(204)
        end
      end
    end

    describe "Retrieve, Update and Delete statistics monitoring configuration for Feature Group and Feature View" do
      context 'with valid project, featurestore, feature_group, feature_view and corresponding statistics monitoring configs' do
        before :all do
          with_valid_project
          @featurestore_id = get_featurestore_id(@project[:id])
          fg_dto, featuregroup_name = create_cached_featuregroup(@project[:id], @featurestore_id, online:true)
          expect_status_details(201)
          @fg_json = JSON.parse(fg_dto)
          expect_status_details(201)
          fv_dto = create_feature_view_from_feature_group(@project[:id], @featurestore_id, @fg_json)
          @fv_json = JSON.parse(fv_dto)
          expect_status_details(201)
          @feature_name = "testfeature"
          @template_stats_config_name_fg = "test_stats_config"
          @job_stats_name_fg = "#{@fg_json['name']}_1_#{@template_stats_config_name_fg}_run_feature_monitoring"
          @template_stats_config_name_fv = "test_stats_config"
          @job_stats_name_fv = "#{@fv_json['name']}_1_#{@template_stats_config_name_fv}_run_feature_monitoring"
          # Create a stats monitoring config for both featuregroup and feature view
          config = generate_template_stats_monitoring_config(\
            @featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true)
          config_res = create_feature_monitoring_configuration_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], config)
          expect_status_details(201)
          config = generate_template_stats_monitoring_config(
            @featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"], false)
          config_res = create_feature_monitoring_configuration_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], config)
          expect_status_details(201)
        end

        it "should fetch a list of stats only feature monitoring config attached to a featuregroup via feature name" do
          config = generate_template_stats_monitoring_config(\
            @featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true)
          fetched_configs = get_feature_monitoring_configuration_by_feature_name_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], @feature_name)
          expect_status_details(200)
          config_json = JSON.parse(fetched_configs)
          expect(config_json.key?("count")).to be true
          expect(config_json["count"] == 1).to be true
          expect(config_json.key?("items")).to be true
          expect(config_json["items"].length == 1).to be true
          expect_feature_monitoring_equal(\
            config_json["items"][0], config, @job_stats_name_fg, @template_stats_config_name_fg, fg_id: @fg_json["id"])
        end

        it "should fetch a list of stats only feature monitoring config attached to a featuregroup" do
          config = generate_template_stats_monitoring_config(\
            @featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true)
          fetched_configs = get_feature_monitoring_configuration_by_entity_fg(\
            @project[:id], @featurestore_id, @fg_json["id"])
          expect_status_details(200)
          config_json = JSON.parse(fetched_configs)
          expect(config_json.key?("count")).to be true
          expect(config_json["count"] == 1).to be true
          expect(config_json.key?("items")).to be true
          expect(config_json["items"].length == 1).to be true
          expect_feature_monitoring_equal(\
            config_json["items"][0], config, @job_stats_name_fg, @template_stats_config_name_fg, fg_id: @fg_json["id"])
        end

        it "should fetch a stats only feature monitoring config attached to a featuregroup via its name" do
          config = generate_template_stats_monitoring_config(\
            @featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true)
          fetched_config_by_name = get_feature_monitoring_configuration_by_name_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], @template_stats_config_name_fg)
          expect_status_details(200)
          config_json = JSON.parse(fetched_config_by_name)
          expect_feature_monitoring_equal(\
            config_json, config, @job_stats_name_fg, @template_stats_config_name_fg, fg_id: @fg_json["id"])
        end

        it "should trigger the execution of the job associated to a stats only feature monitoring config" do
          fetched_config_by_name = get_feature_monitoring_configuration_by_name_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], @template_stats_config_name_fg)
          expect_status_details(200)
          config_json = JSON.parse(fetched_config_by_name)
          execution_res = start_execution(@project[:id], config_json["jobName"])
          execution_json = JSON.parse(execution_res)
          expect(execution_json.has_key?("id")).to eq(true)
          args = execution_json["args"]
          expect(args[-11,args.length]).to eql("config.json")
          expect(args[0,26]).to eql("-op run_feature_monitoring")
          expect(args[args.length - 12 - config_json['jobName'].length..args.length-13]).to eql(config_json['jobName'])
        end

        it "should fetch a stats only feature monitoring config attached to a featuregroup via config id" do
          config = generate_template_stats_monitoring_config(\
            @featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true)
          fetched_config_by_name = get_feature_monitoring_configuration_by_name_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], @template_stats_config_name_fg)
          expect_status_details(200)
          json_config_by_name = JSON.parse(fetched_config_by_name)
          config_id = json_config_by_name["id"]
          fetched_config_by_id = get_feature_monitoring_configuration_by_id_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], config_id)
          expect_status_details(200)
          config_json = JSON.parse(fetched_config_by_id)
          expect_feature_monitoring_equal(\
            config_json, config, @job_stats_name_fg, @template_stats_config_name_fg, fg_id: @fg_json["id"])
        end

        it "should delete a stats only feature monitoring config" do
          fetched_config_by_name = get_feature_monitoring_configuration_by_name_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], @template_stats_config_name_fg)
          expect_status_details(200)
          json_config_by_name = JSON.parse(fetched_config_by_name)
          id = json_config_by_name["id"]
          delete_feature_monitoring_configuration_by_id_fg(@project[:id], @featurestore_id, @fg_json["id"], id)
          expect_status_details(204)
        end

        it "should fetch a stats only feature monitoring config attached to a feature view via feature name" do
          config = generate_template_stats_monitoring_config(
            @featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"], false)
          fetched_configs = get_feature_monitoring_configuration_by_feature_name_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], @feature_name)
          expect_status_details(200)
          config_json = JSON.parse(fetched_configs)["items"][0]
          expect_feature_monitoring_equal(\
            config_json, config, @job_stats_name_fv, @template_stats_config_name_fv, fg_id: nil, fv_name: @fv_json["name"], fv_version: @fv_json["version"])
        end

        it "should fetch a stats only feature monitoring config attached to a feature view" do
          config = generate_template_stats_monitoring_config(
            @featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"], false)
          fetched_configs = get_feature_monitoring_configuration_by_entity_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"])
          expect_status_details(200)
          config_json = JSON.parse(fetched_configs)["items"][0]
          expect_feature_monitoring_equal(\
            config_json, config, @job_stats_name_fv, @template_stats_config_name_fv, fg_id: nil, fv_name: @fv_json["name"], fv_version: @fv_json["version"])
        end

        it "should fetch a stats only feature monitoring config attached to a feature view via its name" do
          config = generate_template_stats_monitoring_config(\
            @featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"], false)
          fetched_config = get_feature_monitoring_configuration_by_name_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], @template_stats_config_name_fv)
          expect_status_details(200)
          config_json = JSON.parse(fetched_config)
          expect_feature_monitoring_equal(\
            config_json, config, @job_stats_name_fv, @template_stats_config_name_fv, fg_id: nil, fv_name: @fv_json["name"], fv_version: @fv_json["version"])
        end

        it "should trigger the job associated to a stats only feature monitoring config" do
          fetched_config_by_name = get_feature_monitoring_configuration_by_name_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], @template_stats_config_name_fv)
          expect_status_details(200)
          config_json = JSON.parse(fetched_config_by_name)
          execution_res = start_execution(@project[:id], config_json["jobName"])
          execution_json = JSON.parse(execution_res)
          expect(execution_json.has_key?("id")).to eq(true)
          args = execution_json["args"]
          expect(args[-11,args.length]).to eql("config.json")
          expect(args[0,26]).to eql("-op run_feature_monitoring")
          expect(args[args.length - 12 - config_json['jobName'].length..args.length-13]).to eql(config_json['jobName'])
        end

        it "should fetch a stats only feature monitoring config attached to a feature view via config id" do
          config = generate_template_stats_monitoring_config(
            @featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"], false)
          fetched_config_by_name = get_feature_monitoring_configuration_by_name_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], @template_stats_config_name_fv)
          expect_status_details(200)
          json_config_by_name = JSON.parse(fetched_config_by_name)
          config_id = json_config_by_name["id"]
          fetched_config_by_id = get_feature_monitoring_configuration_by_id_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], config_id)
          expect_status_details(200)
          config_json = JSON.parse(fetched_config_by_id)
          expect_feature_monitoring_equal(\
            config_json, config, @job_stats_name_fv, @template_stats_config_name_fv, fg_id: nil, fv_name: @fv_json["name"], fv_version: @fv_json["version"])
        end

        it "should delete a stats only feature monitoring config attached to a Feature View" do
          fetched_config_by_name = get_feature_monitoring_configuration_by_name_fv(
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], @template_stats_config_name_fv)
          expect_status_details(200)
          json_config_by_name = JSON.parse(fetched_config_by_name)
          id = json_config_by_name["id"]
          delete_feature_monitoring_configuration_by_id_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], id)
          expect_status_details(204)
        end

        # Testing here so we can test fg and fv endpoints for both config and result with same test...
        it "should raise feature flag exception when feature monitoring is not enabled for all config endpoints" do
          setVar('enable_feature_monitoring', 'false')
          config = generate_template_feature_monitoring_config(@featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true)
          err_res = create_feature_monitoring_configuration_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], config)
          err_json = JSON.parse(err_res)
          expect_status_details(400)
          expect(err_json["errorCode"]).to eq(270234)
          expect(err_json["errorMsg"]).to eql("Feature monitoring is not enabled.")
          config = generate_template_feature_monitoring_config(@featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"], false)
          err_res = create_feature_monitoring_configuration_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], config)
          err_json = JSON.parse(err_res)
          expect_status_details(400)
          expect(err_json["errorCode"]).to eq(270234)
          expect(err_json["errorMsg"]).to eql("Feature monitoring is not enabled.")
          monitoring_time = 1676457000
          result = generate_template_feature_monitoring_result(@featurestore_id, 0, monitoring_time, nil)
          err_res = create_feature_monitoring_result_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], result)
          err_json = JSON.parse(err_res)
          expect_status_details(400)
          expect(err_json["errorCode"]).to eq(270234)
          expect(err_json["errorMsg"]).to eql("Feature monitoring is not enabled.")
          err_res = create_feature_monitoring_result_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], result)
          err_json = JSON.parse(err_res)
          expect_status_details(400)
          expect(err_json["errorCode"]).to eq(270234)
          expect(err_json["errorMsg"]).to eql("Feature monitoring is not enabled.")
          setVar('enable_feature_monitoring', 'true')
        end
      end
    end

    describe "Create feature monitoring result" do
      context 'with valid project, featurestore, featuregroup and feature view as well as respective feature monitoring configurations' do
        before :all do
          with_valid_project
          @featurestore_id = get_featurestore_id(@project[:id])
          # create feature monitoring configs from feature group
          fg_dto, featuregroup_name = create_cached_featuregroup(@project[:id], @featurestore_id, online:true)
          expect_status_details(201)
          @fg_json = JSON.parse(fg_dto)
          expect_status_details(201)
          @config_td = "test_config_td"
          config_fg = generate_template_feature_monitoring_config(@featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true,\
            det_window_type: "ROLLING_TIME", ref_window_type: "TRAINING_DATASET", name: @config_td)
          config_res_fg = create_feature_monitoring_configuration_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], config_fg)
          expect_status_details(201)
          @config_td_json_fg = JSON.parse(config_res_fg)  # det window: ROLLING_TIME, ref window: TRAINING_DATASET
          @config_specific_value = "test_config_specific_value"
          config_fg = generate_template_feature_monitoring_config(@featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true,\
            det_window_type: "ROLLING_TIME", ref_window_type: "SPECIFIC_VALUE", name: @config_specific_value)
          config_res_fg = create_feature_monitoring_configuration_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], config_fg)
          expect_status_details(201)
          @config_specific_value_json_fg = JSON.parse(config_res_fg)  # det window: ROLLING_TIME, ref window: SPECIFIC_VALUE
          # create feature monitoring configs from feature view
          fv_dto = create_feature_view_from_feature_group(@project[:id], @featurestore_id, @fg_json)
          @fv_json = JSON.parse(fv_dto)
          expect_status_details(201)
          config_fv = generate_template_feature_monitoring_config(@featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"], false,\
            det_window_type: "ROLLING_TIME", ref_window_type: "TRAINING_DATASET", name: @config_td)
          config_res_fv = create_feature_monitoring_configuration_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], config_fv)
          expect_status_details(201)
          @config_td_json_fv = JSON.parse(config_res_fv)  # det window: ROLLING_TIME, ref window: TRAINING_DATASET
          config_fv = generate_template_feature_monitoring_config(@featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"], false,\
          det_window_type: "ROLLING_TIME", ref_window_type: "SPECIFIC_VALUE", name: @config_specific_value)
          config_res_fv = create_feature_monitoring_configuration_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], config_fv)
          expect_status_details(201)
          @config_specific_value_json_fv = JSON.parse(config_res_fv)  # det window: ROLLING_TIME, ref window: SPECIFIC_VALUE
          @monitoring_time = 1676457346
        end

        it "should create a feature monitoring result attached to a config via Feature Group Api" do
          # create statistics
          create_statistics_commit_fg(@project[:id], @featurestore_id, @fg_json["id"], computation_time: 1677670460000)
          expect_status_details(200)
          parsed_json = JSON.parse(json_body.to_json)
          expect(parsed_json.key?("featureDescriptiveStatistics")).to be true
          descriptive_statistics_id = parsed_json["featureDescriptiveStatistics"][0]["id"]
          # create result
          result = generate_template_feature_monitoring_result(\
            @featurestore_id, @config_specific_value_json_fg["id"], @monitoring_time, \
            descriptive_statistics_id, specific_value: @config_specific_value_json_fg["referenceWindowConfig"]["specificValue"])
          result_res = create_feature_monitoring_result_fg(@project[:id], @featurestore_id, @fg_json["id"], result)
          expect_status_details(201)
          result_json = JSON.parse(result_res)
          result_res = get_feature_monitoring_result_by_id_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], result_json["id"])
          result_json = JSON.parse(result_res)

          expect(result_json["featureStoreId"]).to eq(@featurestore_id)
          expect(result_json.has_key?("id")).to eq(true)
          expect(result_json["executionId"]).to eq(result[:executionId])
          expect(result_json["monitoringTime"]).to eq(result[:monitoringTime])
          expect(result_json["shiftDetected"]).to eq(result[:shiftDetected])
          expect(result_json["featureName"]).to eq(result[:featureName])
          expect(result_json["difference"]).to be > (result[:difference] - 0.1)
          expect(result_json["difference"]).to be < (result[:difference] + 0.1)
          expect(result_json["detectionStatisticsId"]).to be nil
          expect(result_json["referenceStatisticsId"]).to be nil
          expect(result_json["detectionStatistics"]).not_to be_nil
          expect(result_json["referenceStatistics"]).to be_nil
          expect(result_json["specificValue"]).to eq(result[:specificValue])
          expect(result_json["emptyDetectionWindow"]).to eq(result[:emptyDetectionWindow])
          expect(result_json["emptyReferenceWindow"]).to eq(result[:emptyReferenceWindow])
        end

        it "should fail to create a feature monitoring result without statistics via Feature Group Api" do
          descriptive_statistics_id = 1072
          # create result
          result = generate_template_feature_monitoring_result(@featurestore_id, @config_td_json_fg["id"], @monitoring_time, nil)

          # should fail to create without detection statistics
          result[:detectionStatisticsId] = nil
          result[:referenceStatisticsId] = descriptive_statistics_id
          result_res = create_feature_monitoring_result_fg(@project[:id], @featurestore_id, @fg_json["id"], result)
          expect_status_details(422)
          expect_json(usrMsg: "Descriptive statistics id not provided for the detection window.")

          # should fail to create result without reference statistics if window config is not specific value
          result[:detectionStatisticsId] = descriptive_statistics_id
          result[:referenceStatisticsId] = nil
          result_res = create_feature_monitoring_result_fg(@project[:id], @featurestore_id, @fg_json["id"], result)
          expect_status_details(422)
          expect_json(usrMsg: "Feature monitoring configuration " + @config_td_json_fg["name"] + " result cannot have null reference statistics id" +
          " field when the reference window is configured to use training dataset.")

          # should fail to create result with reference statistics if window config is specific value
          result = generate_template_feature_monitoring_result(@featurestore_id, @config_specific_value_json_fg["id"], @monitoring_time, nil)
          result[:detectionStatisticsId] = descriptive_statistics_id
          result[:referenceStatisticsId] = descriptive_statistics_id
          result_res = create_feature_monitoring_result_fg(@project[:id], @featurestore_id, @fg_json["id"], result)
          expect_status_details(422)
          expect_json(usrMsg: "Feature monitoring configuration " + @config_specific_value_json_fv["name"] + " result cannot have null specific value field" +
          " when the reference window is configured to use specific value.")
        end

        it "should create a feature monitoring result attached to a config via Feature View Api" do
          # create statistics
          create_statistics_commit_fv(@project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], computation_time: 1677670460000)
          expect_status_details(200)
          parsed_json = JSON.parse(json_body.to_json)
          expect(parsed_json.key?("featureDescriptiveStatistics")).to be true
          descriptive_statistics_id = parsed_json["featureDescriptiveStatistics"][0]["id"]
          # create result
          result = generate_template_feature_monitoring_result(\
            @featurestore_id, @config_specific_value_json_fv["id"], @monitoring_time, \
            descriptive_statistics_id, specific_value: @config_specific_value_json_fg["referenceWindowConfig"]["specificValue"])
          result_res = create_feature_monitoring_result_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], result)
          expect_status_details(201)
          result_json = JSON.parse(result_res)
          result_res = get_feature_monitoring_result_by_id_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], result_json["id"])
          result_json = JSON.parse(result_res)

          result_json = JSON.parse(result_res)
          expect(result_json["featureStoreId"]).to eq(@featurestore_id)
          expect(result_json.has_key?("id")).to eq(true)
          expect(result_json["executionId"]).to eq(result[:executionId])
          expect(result_json["monitoringTime"]).to eq(result[:monitoringTime])
          expect(result_json["shiftDetected"]).to eq(result[:shiftDetected])
          expect(result_json["featureName"]).to eq(result[:featureName])
          expect(result_json["difference"]).to be > (result[:difference] - 0.1)
          expect(result_json["difference"]).to be < (result[:difference] + 0.1)
          expect(result_json["detectionStatisticsId"]).to be nil
          expect(result_json["referenceStatisticsId"]).to be nil
          expect(result_json["detectionStatistics"]).not_to be_nil
          expect(result_json["referenceStatistics"]).to be nil
          expect(result_json["specificValue"]).to eq(result[:specificValue])
          expect(result_json["emptyDetectionWindow"]).to eq(result[:emptyDetectionWindow])
          expect(result_json["emptyReferenceWindow"]).to eq(result[:emptyReferenceWindow])
        end

        it "should fail to create a feature monitoring result without statistics via Feature View Api" do
          descriptive_statistics_id = 1072
          # create result
          result = generate_template_feature_monitoring_result(@featurestore_id, @config_td_json_fv["id"], @monitoring_time, nil)

          # should fail to create without detection statistics
          result[:detectionStatisticsId] = nil
          result[:referenceStatisticsId] = descriptive_statistics_id
          result_res = create_feature_monitoring_result_fv(@project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], result)
          expect_status_details(422)
          expect_json(usrMsg: "Descriptive statistics id not provided for the detection window.")

          # should fail to create result without reference statistics if window config is not specific value
          result[:detectionStatisticsId] = descriptive_statistics_id
          result[:referenceStatisticsId] = nil
          result_res = create_feature_monitoring_result_fv(@project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], result)
          expect_status_details(422)
          expect_json(usrMsg: "Feature monitoring configuration " + @config_td_json_fg["name"] + " result cannot have null reference statistics id" +
          " field when the reference window is configured to use training dataset.")

          # should fail to create result with reference statistics if window config is specific value
          result = generate_template_feature_monitoring_result(@featurestore_id, @config_specific_value_json_fv["id"], @monitoring_time, nil)
          result[:detectionStatisticsId] = descriptive_statistics_id
          result[:referenceStatisticsId] = descriptive_statistics_id
          result_res = create_feature_monitoring_result_fv(@project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], result)
          expect_status_details(422)
          expect_json(usrMsg: "Feature monitoring configuration " + @config_specific_value_json_fv["name"] + " result cannot have null specific value field" +
          " when the reference window is configured to use specific value.")
        end
      end
    end

    describe "Retrieve feature monitoring results" do
      context 'with valid project, featurestore, feature group, feature view, feature monitoring configurations and results' do
        before :all do
          with_valid_project
          @featurestore_id = get_featurestore_id(@project[:id])
          # create feature monitoring configs from feature group
          fg_dto, featuregroup_name = create_cached_featuregroup(@project[:id], @featurestore_id, online:true)
          expect_status_details(201)
          @fg_json = JSON.parse(fg_dto)
          expect_status_details(201)
          @config_td = "test_config_td"
          config_fg = generate_template_feature_monitoring_config(@featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true,\
            det_window_type: "ROLLING_TIME", ref_window_type: "TRAINING_DATASET", name: @config_td)
          config_res_fg = create_feature_monitoring_configuration_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], config_fg)
          expect_status_details(201)
          @config_rt_td_json_fg = JSON.parse(config_res_fg)  # det window: ROLLING_TIME, ref window: TRAINING_DATASET
          @config_specific_value = "test_config_specific_value"
          config_fg = generate_template_feature_monitoring_config(@featurestore_id, @fg_json["id"], @fg_json["name"], @fg_json["version"], true,\
            det_window_type: "ROLLING_TIME", ref_window_type: "SPECIFIC_VALUE", name: @config_specific_value)
          config_res_fg = create_feature_monitoring_configuration_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], config_fg)
          expect_status_details(201)
          @config_rt_sv_json_fg = JSON.parse(config_res_fg)  # det window: ROLLING_TIME, ref window: SPECIFIC_VALUE
          # create feature monitoring configs from feature view
          fv_dto = create_feature_view_from_feature_group(@project[:id], @featurestore_id, @fg_json)
          @fv_json = JSON.parse(fv_dto)
          expect_status_details(201)
          config_fv = generate_template_feature_monitoring_config(@featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"], false,\
            det_window_type: "ROLLING_TIME", ref_window_type: "TRAINING_DATASET", name: @config_td)
          config_res_fv = create_feature_monitoring_configuration_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], config_fv)
          expect_status_details(201)
          @config_rt_td_json_fv = JSON.parse(config_res_fv)  # det window: ROLLING_TIME, ref window: TRAINING_DATASET
          config_fv = generate_template_feature_monitoring_config(@featurestore_id, @fv_json["id"], @fv_json["name"], @fv_json["version"], false,\
          det_window_type: "ROLLING_TIME", ref_window_type: "SPECIFIC_VALUE", name: @config_specific_value)
          config_res_fv = create_feature_monitoring_configuration_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], config_fv)
          expect_status_details(201)
          @config_rt_sv_json_fv = JSON.parse(config_res_fv)  # det window: ROLLING_TIME, ref window: SPECIFIC_VALUE
          # create feature monitoring results
          @monitoring_time = 1676457000
          @desc_stats = generate_template_feature_descriptive_statistics(exact_uniqueness: true, shift_delta: 0.4)
          create_statistics_commit_fg(@project[:id], @featurestore_id, @fg_json["id"], feature_descriptive_statistics: @desc_stats, computation_time: 1677670460000)
          expect_status_details(200)
          parsed_json = JSON.parse(json_body.to_json)
          expect(parsed_json.key?("featureDescriptiveStatistics")).to be true
          @descriptive_statistics_id_fg = parsed_json["featureDescriptiveStatistics"][0]["id"]
          result = generate_template_feature_monitoring_result(\
            @featurestore_id, @config_rt_td_json_fg["id"], @monitoring_time, @descriptive_statistics_id_fg, reference_statistics_id: @descriptive_statistics_id_fg)
          result_res = create_feature_monitoring_result_fg(@project[:id], @featurestore_id, @fg_json["id"], result)
          expect_status_details(201)
          create_statistics_commit_fv(@project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], feature_descriptive_statistics: @desc_stats, computation_time: 1677670460000)
          expect_status_details(200)
          parsed_json = JSON.parse(json_body.to_json)
          expect(parsed_json.key?("featureDescriptiveStatistics")).to be true
          @descriptive_statistics_id_fv = parsed_json["featureDescriptiveStatistics"][0]["id"]
          result = generate_template_feature_monitoring_result(\
            @featurestore_id, @config_rt_td_json_fv["id"], @monitoring_time, @descriptive_statistics_id_fv, reference_statistics_id: @descriptive_statistics_id_fv)
          result_res = create_feature_monitoring_result_fv(@project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], result)
          expect_status_details(201)
        end

        it "should fetch results attached to a monitoring config via Feature Group Api" do
          result = generate_template_feature_monitoring_result(\
            @featurestore_id, @config_rt_td_json_fg["id"], @monitoring_time, @descriptive_statistics_id_fg, reference_statistics_id: @descriptive_statistics_id_fg)
          result_res = get_all_feature_monitoring_results_by_config_id_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], @config_rt_td_json_fg["id"])
          expect_status_details(200)
          result_json = JSON.parse(result_res)

          # validate first FM result against the template
          expect(result_json["count"]).to eq(1)
          expect(result_json["items"][0]["featureStoreId"]).to eql(@featurestore_id)
          expect(result_json["items"][0].has_key?("id")).to eq(true)
          expect(result_json["items"][0]["executionId"]).to eq(result[:executionId])
          expect(result_json["items"][0]["monitoringTime"]).to eq(result[:monitoringTime])
          expect(result_json["items"][0]["shiftDetected"]).to eq(result[:shiftDetected])
          expect(result_json["items"][0]["featureName"]).to eq(result[:featureName])
          expect(result_json["items"][0]["difference"]).to be > (result[:difference] - 0.1)
          expect(result_json["items"][0]["difference"]).to be < (result[:difference] + 0.1)
          expect(result_json["items"][0]["detectionStatisticsId"]).not_to be_nil
          expect(result_json["items"][0]["referenceStatisticsId"]).not_to be_nil
          expect(result_json["items"][0]["detectionStatistics"]).to be nil
          expect(result_json["items"][0]["referenceStatistics"]).to be nil

          # validate first FM result against the FM result in the database
          result_single_res = get_feature_monitoring_result_by_id_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], result_json["items"][0]["id"])
          expect_status_details(200)
          result_single_json = JSON.parse(result_single_res)
          expect(result_single_json["detectionStatisticsId"]).to be nil
          expect(result_single_json["referenceStatisticsId"]).to be nil
          expect(result_single_json["detectionStatistics"]).not_to be_nil
          expect(result_single_json["referenceStatistics"]).not_to be_nil
          result_single_json["detectionStatisticsId"] = result_json["items"][0]["detectionStatisticsId"]
          result_single_json["referenceStatisticsId"] = result_json["items"][0]["referenceStatisticsId"]
          result_single_json.delete("detectionStatistics")  # stats not included without expansion \
          result_single_json.delete("referenceStatistics")  # when fetching multiple results
          expect(result_single_json).to eql(result_json["items"][0])
        end

        it "should fetch results attached to a monitoring config, including statistics when the statistics expansion is set via Feature Group Api" do
          result = generate_template_feature_monitoring_result(\
            @featurestore_id, @config_rt_td_json_fg["id"], @monitoring_time, @descriptive_statistics_id_fg, reference_statistics_id: @descriptive_statistics_id_fg)
          desc_stats = generate_template_feature_descriptive_statistics(exact_uniqueness: true, shift_delta: 0.4)[0]
          result[:detectionStatistics] = desc_stats
          result[:referenceStatistics] = desc_stats
          # get results
          result_res = get_all_feature_monitoring_results_by_config_id_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], @config_rt_td_json_fg["id"], with_stats: true)
          expect_status_details(200)
          result_json = JSON.parse(result_res)

          # validate first FM result against the templates
          expect(result_json["count"]).to eq(1)
          expect(result_json["items"][0]["featureStoreId"]).to eql(@featurestore_id)
          expect(result_json["items"][0].has_key?("id")).to eq(true)
          expect(result_json["items"][0]["executionId"]).to eq(result[:executionId])
          expect(result_json["items"][0]["monitoringTime"]).to eq(result[:monitoringTime])
          expect(result_json["items"][0]["shiftDetected"]).to eq(result[:shiftDetected])
          expect(result_json["items"][0]["featureName"]).to eq(result[:featureName])
          expect(result_json["items"][0]["difference"]).to be > (result[:difference] - 0.1)
          expect(result_json["items"][0]["difference"]).to be < (result[:difference] + 0.1)
          expect(result_json["items"][0]["detectionStatisticsId"]).to be_nil
          expect(result_json["items"][0]["referenceStatisticsId"]).to be_nil
          expect_desc_statistics_to_be_eq(result_json["items"][0]["detectionStatistics"], result[:detectionStatistics])
          expect_desc_statistics_to_be_eq(result_json["items"][0]["referenceStatistics"], result[:referenceStatistics])
          # validate first FM result against the FM result in the database
          result_single_res = get_feature_monitoring_result_by_id_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], result_json["items"][0]["id"])
          expect_status_details(200)
          result_single_json = JSON.parse(result_single_res)
          expect(result_single_json).to eql(result_json["items"][0])
        end

        it "should fetch results attached to a monitoring config via Feature View Api" do
          result = generate_template_feature_monitoring_result(\
            @featurestore_id, @config_rt_td_json_fg["id"], @monitoring_time, @descriptive_statistics_id_fv, reference_statistics_id: @descriptive_statistics_id_fv)
          result_res = get_all_feature_monitoring_results_by_config_id_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], @config_rt_td_json_fv["id"])
          expect_status_details(200)
          result_json = JSON.parse(result_res)

          # validate first FM result against the templates
          expect(result_json["count"]).to eq(1)
          expect(result_json["items"][0]["featureStoreId"]).to eql(@featurestore_id)
          expect(result_json["items"][0].has_key?("id")).to eq(true)
          expect(result_json["items"][0]["executionId"]).to eq(result[:executionId])
          expect(result_json["items"][0]["monitoringTime"]).to eq(result[:monitoringTime])
          expect(result_json["items"][0]["shiftDetected"]).to eq(result[:shiftDetected])
          expect(result_json["items"][0]["featureName"]).to eq(result[:featureName])
          expect(result_json["items"][0]["difference"]).to be > (result[:difference] - 0.1)
          expect(result_json["items"][0]["difference"]).to be < (result[:difference] + 0.1)
          expect(result_json["items"][0]["detectionStatisticsId"]).not_to be_nil
          expect(result_json["items"][0]["referenceStatisticsId"]).not_to be_nil
          expect(result_json["items"][0]["detectionStatistics"]).to be nil
          expect(result_json["items"][0]["referenceStatistics"]).to be nil
          # validate first FM result against the FM result in the database
          result_single_res = get_feature_monitoring_result_by_id_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], result_json["items"][0]["id"])
          expect_status_details(200)
          result_single_json = JSON.parse(result_single_res)
          expect(result_single_json["detectionStatisticsId"]).to be nil
          expect(result_single_json["referenceStatisticsId"]).to be nil
          expect(result_single_json["detectionStatistics"]).not_to be_nil
          expect(result_single_json["referenceStatistics"]).not_to be_nil
          result_single_json["detectionStatisticsId"] = result_json["items"][0]["detectionStatisticsId"]
          result_single_json["referenceStatisticsId"] = result_json["items"][0]["referenceStatisticsId"]
          result_single_json.delete("detectionStatistics")  # stats not included without expansion \
          result_single_json.delete("referenceStatistics")  # when fetching multiple results
          expect(result_single_json).to eql(result_json["items"][0])
        end

        it "should fetch results attached to a monitoring config, including statistics when the statistics expansion is set via Feature View Api" do
          result = generate_template_feature_monitoring_result(\
            @featurestore_id, @config_rt_td_json_fg["id"], @monitoring_time, @descriptive_statistics_id_fv, reference_statistics_id: @descriptive_statistics_id_fv)
          desc_stats = generate_template_feature_descriptive_statistics(exact_uniqueness: true, shift_delta: 0.4)[0]
          result[:detectionStatistics] = desc_stats
          result[:referenceStatistics] = desc_stats
          # get results
          result_res = get_all_feature_monitoring_results_by_config_id_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], @config_rt_td_json_fv["id"], with_stats: true)
          expect_status_details(200)
          result_json = JSON.parse(result_res)

          # validate first FM result against the templates
          expect(result_json["count"]).to eq(1)
          expect(result_json["items"][0]["featureStoreId"]).to eql(@featurestore_id)
          expect(result_json["items"][0].has_key?("id")).to eq(true)
          expect(result_json["items"][0]["executionId"]).to eq(result[:executionId])
          expect(result_json["items"][0]["monitoringTime"]).to eq(result[:monitoringTime])
          expect(result_json["items"][0]["shiftDetected"]).to eq(result[:shiftDetected])
          expect(result_json["items"][0]["featureName"]).to eq(result[:featureName])
          expect(result_json["items"][0]["difference"]).to be > (result[:difference] - 0.1)
          expect(result_json["items"][0]["difference"]).to be < (result[:difference] + 0.1)
          expect(result_json["items"][0]["detectionStatisticsId"]).to be_nil
          expect(result_json["items"][0]["referenceStatisticsId"]).to be_nil
          expect_desc_statistics_to_be_eq(result_json["items"][0]["detectionStatistics"], result[:detectionStatistics])
          expect_desc_statistics_to_be_eq(result_json["items"][0]["referenceStatistics"], result[:referenceStatistics])
          # validate first FM result against the FM result in the database
          result_single_res = get_feature_monitoring_result_by_id_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], result_json["items"][0]["id"])
          expect_status_details(200)
          result_single_json = JSON.parse(result_single_res)
          expect(result_single_json).to eql(result_json["items"][0])
        end

        it "should fetch multiple feature monitoring result attached to a config via Feature Group Api" do
          monitoring_time_1 = 1676457000
          monitoring_time_2 = 1676557000
          result = generate_template_feature_monitoring_result(\
            @featurestore_id, @config_rt_td_json_fg["id"], monitoring_time_2, @descriptive_statistics_id_fg, reference_statistics_id: @descriptive_statistics_id_fg)
          result_res = create_feature_monitoring_result_fg(@project[:id], @featurestore_id, @fg_json["id"], result)
          expect_status_details(201)
          result_res = get_all_feature_monitoring_results_by_config_id_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], @config_rt_td_json_fg["id"])
          expect_status_details(200)
          result_json = JSON.parse(result_res)
          expect(result_json["count"]).to eq(2)
          expect(result_json["items"][0]["monitoringTime"]).to eq(monitoring_time_2)
          expect(result_json["items"][1]["monitoringTime"]).to eq(monitoring_time_1)
        end

        it "should fetch multiple feature monitoring result attached to a config via Feature View Api" do
          monitoring_time_1 = 1676457000
          monitoring_time_2 = 1676557000
          result = generate_template_feature_monitoring_result(\
            @featurestore_id, @config_rt_td_json_fv["id"], monitoring_time_2, @descriptive_statistics_id_fv, reference_statistics_id: @descriptive_statistics_id_fv) 
          result_res = create_feature_monitoring_result_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], result)
          expect_status_details(201)
          result_res = get_all_feature_monitoring_results_by_config_id_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], @config_rt_td_json_fv["id"])
          expect_status_details(200)
          result_json = JSON.parse(result_res)
          expect(result_json["count"]).to eq(2)
          expect(result_json["items"][0]["monitoringTime"]).to eq(monitoring_time_2)
          expect(result_json["items"][1]["monitoringTime"]).to eq(monitoring_time_1)
        end

        it "should delete a single feature monitoring result attached to a config via Feature Group Api" do
          result_res = get_all_feature_monitoring_results_by_config_id_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], @config_rt_td_json_fg["id"])
          expect_status_details(200)
          result_json = JSON.parse(result_res)
          expect(result_json["count"]).to eq(2)
          delete_feature_monitoring_result_by_id_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], result_json["items"][0]["id"])
          expect_status_details(204)
          result_res = get_all_feature_monitoring_results_by_config_id_fg(\
            @project[:id], @featurestore_id, @fg_json["id"], @config_rt_td_json_fg["id"])
          expect_status_details(200)
          result_json = JSON.parse(result_res)
          expect(result_json["count"]).to eq(1)
        end

        it "should delete a single feature monitoring result attached to a config via Feature View Api" do
          result_res = get_all_feature_monitoring_results_by_config_id_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], @config_rt_td_json_fv["id"])
          expect_status_details(200)
          result_json = JSON.parse(result_res)
          expect(result_json["count"]).to eq(2)
          delete_feature_monitoring_result_by_id_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], result_json["items"][0]["id"])
          expect_status_details(204)
          result_res = get_all_feature_monitoring_results_by_config_id_fv(\
            @project[:id], @featurestore_id, @fv_json["name"], @fv_json["version"], @config_rt_td_json_fv["id"])
          expect_status_details(200)
          result_json = JSON.parse(result_res)
          expect(result_json["count"]).to eq(1)
        end
      end
    end
  end
end