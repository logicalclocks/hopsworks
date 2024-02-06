  # This file is part of Hopsworks
  # Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

  describe "On #{ENV['OS']}" do
    after :all do
      clean_all_test_projects(spec: "featuregroup")
    end

    describe "fs activity" do
      before :all do
        with_valid_project
      end

      it "should be able to retrieve feature group creation event" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
        expect_status_details(201)
        feature_group_id = JSON.parse(json_result)["id"]

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{feature_group_id}/activity"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"][0]["type"]).to eql("METADATA")
        expect(activity["items"][0]["metadata"]).to eql("Feature group was created")
      end

      it "should be able to retrieve alter features event" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, feature_group_name = create_cached_featuregroup(@project[:id], featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status_details(201)
        new_schema = [
          {type: "INT", name: "testfeature", description: "testfeaturedescription", primary: true, partition: false},
          {type: "DOUBLE", name: "testfeature2", description: "testfeaturedescription", primary: false,
          onlineType: "DOUBLE", partition: false, defaultValue: "10.0"},
        ]
        _ = update_cached_featuregroup_metadata(@project[:id], featurestore_id,
                                                parsed_json["id"],
                                                parsed_json["version"],
                                                featuregroup_name: feature_group_name,
                                                features: new_schema)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{parsed_json["id"]}/activity?sort_by=timestamp:desc"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"][0]["type"]).to eql("METADATA")
        expect(activity["items"][0]["metadata"]).to eql("Feature group was altered New features: testfeature2")
      end

      it "should be able to retrieve online enable event" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status_details(201)
        sleep 1
        enable_cached_featuregroup_online(@project[:id], featurestore_id, parsed_json["id"])
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{parsed_json["id"]}/activity?sort_by=timestamp:desc"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"][0]["type"]).to eql("METADATA")
        expect(activity["items"][0]["metadata"]).to eql("Feature group available online")
      end

      it "should be able to retrieve commit events" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, feature_group_name = create_cached_featuregroup_with_partition(@project[:id],
                                                                                  featurestore_id,
                                                                                  time_travel_format: "HUDI")
        parsed_json = JSON.parse(json_result)
        feature_group_id = parsed_json["id"]
        commit_metadata = {commitDateString:20201024221125,commitTime:1603577485000,rowsInserted:4,rowsUpdated:2,rowsDeleted:0}
        commit_cached_featuregroup(@project[:id], featurestore_id, feature_group_id, commit_metadata: commit_metadata)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{feature_group_id}/activity?filter_by=type:commit&expand=commits"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"][0]["type"]).to eql("COMMIT")
        expect(activity["items"][0]["timestamp"]).to eql(activity["items"][0]["commit"]["commitTime"])
        expect(activity["items"][0]["commit"]['rowsInserted']).to eql(4)
        expect(activity["items"][0]["commit"]['rowsUpdated']).to eql(2)
        expect(activity["items"][0]["commit"]['rowsDeleted']).to eql(0)
      end

      it "should be able to retrieve feature group statistics events" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
        parsed_json = JSON.parse(json_result)
        create_statistics_commit_fg(@project[:id], featurestore_id, parsed_json["id"])
        expect_status_details(200)
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{parsed_json["id"]}/activity?filter_by=type:statistics&expand=statistics"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"][0]["type"]).to eql("STATISTICS")
        expect(activity["items"][0]["statistics"]["computationTime"]).to eql(1597903688000)
      end

      it "should be able to retrieve feature view statistics events" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
        parsed_json = JSON.parse(json_result)
        json_result = create_feature_view_from_feature_group(@project[:id], featurestore_id, parsed_json)
        parsed_json = JSON.parse(json_result)
        expect_status_details(201)
        create_statistics_commit_fv(@project[:id], featurestore_id, parsed_json["name"], parsed_json["version"])
        expect_status_details(200)
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featureview/#{parsed_json["name"]}/version/#{parsed_json["version"]}/activity?filter_by=type:statistics&expand=statistics"
        expect_status_details(400, error_code: 260002)
      end

      it "should be able to retrieve training dataset statistics events" do
        featurestore_id = get_featurestore_id(@project[:id])
        all_metadata = create_featureview_training_dataset_from_project(@project)
        trainingdataset = all_metadata["response"]
        featureview = all_metadata["featureView"]
        create_statistics_commit_td(@project[:id], featurestore_id, featureview["name"], featureview["version"], trainingdataset["version"])
        expect_status_details(200)
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/trainingdatasets/#{trainingdataset["id"]}/activity?filter_by=type:statistics&expand=statistics"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"][0]["type"]).to eql("STATISTICS")
        expect(activity["items"][0]["statistics"]["computationTime"]).to eql(1597903688000)
      end

      it "should be able to retrieve activity related to creation of an expectation suite" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
        fg_json = JSON.parse(json_result)
        expectation_suite = generate_template_expectation_suite()
        suite_dto = create_expectation_suite(@project[:id], featurestore_id, fg_json["id"], expectation_suite)
        expect_status_details(201)
        suite_json = JSON.parse(suite_dto)
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{fg_json["id"]}/activity?filter_by=type:expectations&expand=expectationsuite"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"][0]["type"]).to eql("EXPECTATIONS")
        expect(activity["items"][0]["metadata"]).to eql("An Expectation Suite was attached to the Feature Group. ")
        expect(activity["items"][0]["expectationSuite"]["expectationSuiteName"]).to eql(suite_json["expectationSuiteName"])
        expect(activity["items"][0]["expectationSuite"]["expectations"][0]["expectationType"]).to eql(suite_json["expectations"][0]["expectationType"])
        expect(activity["items"][0]["expectationSuite"]["expectations"][0]["meta"]["expectationId"]).to eql(suite_json["expectations"][0]["meta"]["expectationId"])
      end

      it "should be able to retrieve activity related to the update of an expectation suite" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
        fg_json = JSON.parse(json_result)
        expectation_suite = generate_template_expectation_suite()
        suite_dto = create_expectation_suite(@project[:id], featurestore_id, fg_json["id"], expectation_suite)
        expect_status_details(201)
        suite_json = JSON.parse(suite_dto)
        template_expectation = generate_template_expectation()
        template_expectation["expectationType"] = "expect_column_std_to_be_between"
        suite_json["expectations"].append(template_expectation)
        suite_json["expectationSuiteName"] = "updated_suite"
        sleep 1
        update_expectation_suite(@project[:id], featurestore_id, fg_json["id"], suite_json)
        expect_status_details(200)
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{fg_json["id"]}/activity?filter_by=type:expectations&expand=expectationsuite&sort_by=timestamp:desc"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"][0]["type"]).to eql("EXPECTATIONS")
        expect(activity["items"][0]["metadata"]).to eql("The Expectation Suite was updated. ")
        expect(activity["items"][0]["expectationSuite"]["expectationSuiteName"]).to eql("updated_suite")
        expect(activity["items"][0]["expectationSuite"]["expectations"].length).to eql(2)
      end

      it "should be able to retrieve activity related to the metadata update of an expectation suite" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
        fg_json = JSON.parse(json_result)
        expectation_suite = generate_template_expectation_suite()
        suite_dto = create_expectation_suite(@project[:id], featurestore_id, fg_json["id"], expectation_suite)
        expect_status_details(201)
        suite_json = JSON.parse(suite_dto)
        suite_json["expectationSuiteName"] = "updated_suite"
        sleep 1
        update_metadata_expectation_suite(@project[:id], featurestore_id, fg_json["id"], suite_json)
        expect_status_details(200)
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{fg_json["id"]}/activity?filter_by=type:expectations&expand=expectationsuite&sort_by=timestamp:desc"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"][0]["type"]).to eql("EXPECTATIONS")
        expect(activity["items"][0]["metadata"]).to eql("The Expectation Suite metadata was updated. ")
        expect(activity["items"][0]["expectationSuite"]["expectationSuiteName"]).to eql("updated_suite")
      end

      it "should be able to retrieve activity related to deletion of an expectation suite" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
        fg_json = JSON.parse(json_result)
        expectation_suite = generate_template_expectation_suite()
        suite_dto = create_expectation_suite(@project[:id], featurestore_id, fg_json["id"], expectation_suite)
        expect_status_details(201)
        suite_json = JSON.parse(suite_dto)
        sleep 1
        delete_expectation_suite(@project[:id], featurestore_id, fg_json["id"], suite_json["id"])
        expect_status_details(204)
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{fg_json["id"]}/activity?filter_by=type:expectations&expand=expectationsuite&sort_by=timestamp:desc"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"][0]["type"]).to eql("EXPECTATIONS")
        expect(activity["items"][0]["metadata"]).to eql("The Expectation Suite was deleted. ")
      end

      it "should be able to retrieve activity related to append of an expectation" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
        fg_json = JSON.parse(json_result)
        expectation_suite = generate_template_expectation_suite()
        suite_dto = create_expectation_suite(@project[:id], featurestore_id, fg_json["id"], expectation_suite)
        expect_status_details(201)
        suite_json = JSON.parse(suite_dto)
        template_expectation = generate_template_expectation()
        template_expectation["expectationType"] = "expect_column_std_to_be_between"
        sleep 1
        dto_expectation = create_expectation(@project[:id], featurestore_id, fg_json["id"], suite_json["id"], template_expectation)
        expect_status_details(201)
        json_expectation = JSON.parse(dto_expectation)
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{fg_json["id"]}/activity?filter_by=type:expectations&expand=expectationsuite&sort_by=timestamp:desc"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"][0]["type"]).to eql("EXPECTATIONS")
        expect(activity["items"][0]["metadata"]).to eql("The Expectation Suite was updated. Created expectation with id: #{json_expectation["id"]}.")
        expect(activity["items"][0]["expectationSuite"]["expectations"].length).to eql(2)
      end

      it "should be able to retrieve activity related to the update of a single expectation" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
        fg_json = JSON.parse(json_result)
        expectation_suite = generate_template_expectation_suite()
        suite_dto = create_expectation_suite(@project[:id], featurestore_id, fg_json["id"], expectation_suite)
        expect_status_details(201)
        suite_json = JSON.parse(suite_dto)
        template_expectation = generate_template_expectation()
        template_expectation["meta"] = "{\"whoAmI\": \"updated_expectation\"}"
        template_expectation["id"] = suite_json["expectations"][0]["id"]
        sleep 1
        update_expectation(@project[:id], featurestore_id, fg_json["id"], suite_json["id"], template_expectation["id"], template_expectation)
        expect_status_details(200)
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{fg_json["id"]}/activity?filter_by=type:expectations&expand=expectationsuite&sort_by=timestamp:desc"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"][0]["type"]).to eql("EXPECTATIONS")
        expect(activity["items"][0]["metadata"]).to eql("The Expectation Suite was updated. Updated expectation with id: #{template_expectation["id"]}.")
        expect(activity["items"][0]["expectationSuite"]["expectations"].length).to eql(1)
        updated_meta = JSON.parse(activity["items"][0]["expectationSuite"]["expectations"][0]["meta"])
        expect(updated_meta["whoAmI"]).to eql("updated_expectation")
      end

      it "should be able to retrieve activity related to deletion of an expectation" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
        fg_json = JSON.parse(json_result)
        expectation_suite = generate_template_expectation_suite()
        suite_dto = create_expectation_suite(@project[:id], featurestore_id, fg_json["id"], expectation_suite)
        suite_json = JSON.parse(suite_dto)
        sleep 1
        delete_expectation(@project[:id], featurestore_id, fg_json["id"], suite_json["id"], suite_json["expectations"][0]["id"])
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{fg_json["id"]}/activity?filter_by=type:expectations&expand=expectationsuite&sort_by=timestamp:desc"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"][0]["type"]).to eql("EXPECTATIONS")
        expect(activity["items"][0]["metadata"]).to eql("The Expectation Suite was updated. Deleted expectation with id: #{suite_json["expectations"][0]["id"]}.")
        expect(activity["items"][0]["expectationSuite"]["expectations"].length).to eql(0)
      end

      it "should be able to retrieve activity related to upload of a validation report" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
        fg_json = JSON.parse(json_result)
        expectation_suite = generate_template_expectation_suite()
        suite_dto = create_expectation_suite(@project[:id], featurestore_id, fg_json["id"], expectation_suite)
        expect_status_details(201)
        suite_json = JSON.parse(suite_dto)
        report = generate_template_validation_report()
        report_dto = create_validation_report(@project[:id], featurestore_id, fg_json["id"], report)
        expect_status_details(201)
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{fg_json["id"]}/activity?filter_by=type:validations&expand=validationreport"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"][0]["type"]).to eql("VALIDATIONS")
        expect(activity["items"][0]["metadata"]).to eql("A Validation Report was uploaded.")
        expect(activity["items"][0]["validationReport"]["success"]).to eql(report[:success])
        expect(activity["items"][0]["validationReport"]["exceptionInfo"]).to eql(report[:exceptionInfo])
        expect(activity["items"][0]["validationReport"]["statistics"]).to eql(report[:statistics])
        expect(activity["items"][0]["validationReport"]["meta"]).to eql(report[:meta])
      end

      it "should be able to retrieve a limited set of events" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
        parsed_json = JSON.parse(json_result)
        base_timestamp = 1597903688000
        increment = 100000
        (1..15).each do |i|
          create_statistics_commit_fg(@project[:id], featurestore_id, parsed_json["id"], computation_time: base_timestamp + increment * i)
          expect_status_details(200)
        end
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{parsed_json["id"]}/activity" +
              "?filter_by=type:statistics&expand=statistics&sort_by=timestamp:desc&limit=5&offset=0"
        expect_status_details(200)
        activity = JSON.parse(response.body)

        expect(activity["items"].count).to eql(5)
        # The first element should be the most recent, day 15
        expect(activity["items"][0]["statistics"]["computationTime"]).to eql(base_timestamp + increment * 15)
        # The last element should be the least recent of the batch, day 11
        expect(activity["items"][4]["statistics"]["computationTime"]).to eql(base_timestamp + increment * 11)

        # Test second batch
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{parsed_json["id"]}/activity" +
              "?filter_by=type:statistics&expand=statistics&sort_by=timestamp:desc&limit=5&offset=5"
        expect_status_details(200)
        activity = JSON.parse(response.body)

        expect(activity["items"].count).to eql(5)
        # The first element should be the most recent, day 10
        expect(activity["items"][0]["statistics"]["computationTime"]).to eql(base_timestamp + increment * 10)
        # The last element should be the least recent of the batch, day 6
        expect(activity["items"][4]["statistics"]["computationTime"]).to eql(base_timestamp + increment * 6)
      end

      it "should be able to filter events by timestamp" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
        parsed_json = JSON.parse(json_result)
        base_timestamp = 1597903688000
        increment = 100000
        (1..15).each do |i|
          create_statistics_commit_fg(@project[:id], featurestore_id, parsed_json["id"], computation_time: base_timestamp + increment * i)
          expect_status_details(200)
        end
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{parsed_json["id"]}/activity" +
              "?filter_by=type:statistics&expand=statistics&sort_by=timestamp:desc&limit=5&offset=2"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        upper_timestamp = activity['items'][0]["timestamp"].to_i + 1
        lower_timestamp = activity['items'][4]["timestamp"].to_i - 1

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{parsed_json["id"]}/activity" +
              "?filter_by=type:statistics&filter_by=timestamp_lt:#{upper_timestamp}&filter_by=timestamp_gt:#{lower_timestamp}&expand=statistics&sort_by=timestamp"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"][0]["timestamp"].to_i).to eql(upper_timestamp - 1)
        expect(activity["items"][4]["timestamp"].to_i).to eql(lower_timestamp + 1)
      end

      it "should be able to expand users resource" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
        expect_status_details(201)
        feature_group_id = JSON.parse(json_result)["id"]

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{feature_group_id}/activity?expand=users"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"][0]["user"]["username"]).to eql(@user[:username])
      end

      it "should be able to return metadata activities for training datasets" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_hopsfs_training_dataset(@project[:id], featurestore_id, nil)
        expect_status_details(201)
        parsed_json = JSON.parse(json_result)
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/trainingdatasets/#{parsed_json["id"]}/activity"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"][0]["type"]).to eql("METADATA")
        expect(activity["items"][0]["metadata"]).to eql("The training dataset was created")
      end
    
      it "should be able to retrieve feature view creation event" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
        expect_status_details(201)
      
        parsed_json = JSON.parse(json_result)
        # create queryDTO object
        query = {
          leftFeatureGroup: {
            id: parsed_json["id"],
            type: parsed_json["type"],
          },
          leftFeatures: ['testfeature'].map do |feat_name|
            {name: feat_name}
          end,
          joins: []
        }

        json_result = create_feature_view(@project.id, featurestore_id, query)
        parsed_json = JSON.parse(json_result)
        expect_status_details(201)

        feature_view_name = parsed_json["name"]
        feature_view_version = parsed_json["version"]
        json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/activity"
        expect_status_details(200)

        activity = JSON.parse(response.body)
        expect(activity["items"][0]["type"]).to eql("METADATA")
        expect(activity["items"][0]["metadata"]).to eql("The feature view was created")
      end
    end
  end