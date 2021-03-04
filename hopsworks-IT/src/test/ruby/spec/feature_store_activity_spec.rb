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
      feature_group_version = parsed_json["version"]
      path = "/apps/hive/warehouse/#{@project['projectname'].downcase}_featurestore.db/#{feature_group_name}_#{feature_group_version}"
      hoodie_path = path + "/.hoodie"
      mkdir(hoodie_path, getHopsworksUser, getHopsworksUser, 777)
      touchz(hoodie_path + "/20201024221125.commit", getHopsworksUser, getHopsworksUser)
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

    it "should be able to retrieve statistics events" do
      featurestore_id = get_featurestore_id(@project[:id])
      json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
      parsed_json = JSON.parse(json_result)
      create_statistics_commit(@project[:id], featurestore_id, "featuregroups", parsed_json["id"])
      expect_status_details(200)

      get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{parsed_json["id"]}/activity?filter_by=type:statistics&expand=statistics"
      expect_status_details(200)
      activity = JSON.parse(response.body)
      expect(activity["items"][0]["type"]).to eql("STATISTICS")
      expect(activity["items"][0]["statistics"]["commitTime"]).to eql(1597903688000)
    end

    it "should be able to retrieve validation events" do
      featurestore_id = get_featurestore_id(@project[:id])
      json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
      fg_json = parsed_json = JSON.parse(json_result)
      create_validation(@project[:id], featurestore_id, fg_json["id"])

      get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{parsed_json["id"]}/activity?filter_by=type:validations&expand=validations"
      expect_status_details(200)
      activity = JSON.parse(response.body)
      expect(activity["items"][0]["type"]).to eql("VALIDATIONS")
      expect(activity["items"][0]["validations"]["status"]).to eql("SUCCESS")
    end

    it "should be able to retrieve a limited set of events" do
      featurestore_id = get_featurestore_id(@project[:id])
      json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
      parsed_json = JSON.parse(json_result)
      base_timestamp = 1597903688000
      increment = 100000
      (1..15).each do |i|
        create_statistics_commit(@project[:id], featurestore_id, "featuregroups", parsed_json["id"], commit_time: base_timestamp + increment * i)
        expect_status_details(200)
      end
      get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{parsed_json["id"]}/activity" +
            "?filter_by=type:statistics&expand=statistics&sort_by=timestamp:desc&limit=5&offset=0"
      expect_status_details(200)
      activity = JSON.parse(response.body)

      expect(activity["items"].count).to eql(5)
      # The first element should be the most recent, day 15
      expect(activity["items"][0]["statistics"]["commitTime"]).to eql(base_timestamp + increment * 15)
      # The last element should be the least recent of the batch, day 11
      expect(activity["items"][4]["statistics"]["commitTime"]).to eql(base_timestamp + increment * 11)

      # Test second batch
      get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{parsed_json["id"]}/activity" +
            "?filter_by=type:statistics&expand=statistics&sort_by=timestamp:desc&limit=5&offset=5"
      expect_status_details(200)
      activity = JSON.parse(response.body)

      expect(activity["items"].count).to eql(5)
      # The first element should be the most recent, day 10
      expect(activity["items"][0]["statistics"]["commitTime"]).to eql(base_timestamp + increment * 10)
      # The last element should be the least recent of the batch, day 6
      expect(activity["items"][4]["statistics"]["commitTime"]).to eql(base_timestamp + increment * 6)
    end

    it "should be able to filter events by timestamp" do
      featurestore_id = get_featurestore_id(@project[:id])
      json_result, _ = create_cached_featuregroup(@project[:id], featurestore_id)
      parsed_json = JSON.parse(json_result)
      base_timestamp = 1597903688000
      increment = 100000
      (1..15).each do |i|
        create_statistics_commit(@project[:id], featurestore_id, "featuregroups", parsed_json["id"], commit_time: base_timestamp + increment * i)
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
  end
end