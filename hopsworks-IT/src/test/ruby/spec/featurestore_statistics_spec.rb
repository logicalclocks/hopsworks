# This file is part of Hopsworks
# Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

require 'uri'

describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects(spec: "featurestore")}
  describe 'featurestore statistics' do

    describe "Create and get feature store statistics commits for feature groups and training datasets" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to add statistics as a commit to a feature group" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, _ = create_cached_featuregroup(project.id, featurestore_id)
          expect_status(201)
          parsed_json = JSON.parse(json_result)
          create_statistics_commit(project.id, featurestore_id, "featuregroups", parsed_json["id"])
          expect_status_details(200)
        end

        it "should be able to add statistics as a commit to a training dataset" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          expect_status(201)
          parsed_json = JSON.parse(json_result)
          create_statistics_commit(project.id, featurestore_id, "trainingdatasets", parsed_json["id"])
          expect_status_details(200)
        end

        it "should be able to get a specific statistics commit with content field of a feature group" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, _ = create_cached_featuregroup(project.id, featurestore_id)
          expect_status(201)
          parsed_json = JSON.parse(json_result)
          create_statistics_commit(project.id, featurestore_id, "featuregroups", parsed_json["id"])
          expect_status(200)
          json_result = get_statistics_commit(project.id, featurestore_id, "featuregroups", parsed_json["id"])
          expect_status_details(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json.key?("items")).to be true
          expect(parsed_json.key?("count")).to be true
          # should contain exactly one item
          expect(parsed_json["count"] == 1).to be true
          expect(parsed_json["items"][0].key?("content")).to be true
          expect(JSON.parse(parsed_json["items"][0]["content"])).to eql({"columns" => ["a", "b", "c"]})
          expect(parsed_json["items"][0]["commitTime"]).to eql(1597903688000)
        end

        it "should be able to get a specific statistics commit with content field of a training dataset" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          create_statistics_commit(project.id, featurestore_id, "trainingdatasets", parsed_json["id"])
          expect_status(200)
          json_result = get_statistics_commit(project.id, featurestore_id, "trainingdatasets", parsed_json["id"])
          expect_status_details(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json.key?("items")).to be true
          expect(parsed_json.key?("count")).to be true
          # should contain exactly one item
          expect(parsed_json["count"] == 1).to be true
          expect(parsed_json["items"][0].key?("content")).to be true
          expect(JSON.parse(parsed_json["items"][0]["content"])).to eql({"columns" => ["a", "b", "c"]})
          expect(parsed_json["items"][0]["commitTime"]).to eql(1597903688000)
        end

        it "should be able to get the latest statistics commit for a feature group" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, _ = create_cached_featuregroup(project.id, featurestore_id)
          expect_status(201)
          parsed_json = JSON.parse(json_result)
          create_statistics_commit(project.id, featurestore_id, "featuregroups", parsed_json["id"])
          expect_status(200)
          create_statistics_commit(project.id, featurestore_id, "featuregroups", parsed_json["id"],
                                                 commit_time: 1597990088000)
          expect_status(200)
          json_result = get_last_statistics_commit(project.id, featurestore_id, "featuregroups", parsed_json["id"])
          expect_status_details(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json.key?("items")).to be true
          expect(parsed_json.key?("count")).to be true
          expect(parsed_json["count"] == 2).to be true
          # should contain exactly one item
          expect(parsed_json["items"].length == 1).to be true
          expect(parsed_json["items"][0].key?("content")).to be true
          expect(JSON.parse(parsed_json["items"][0]["content"])).to eql({"columns" => ["a", "b", "c"]})
          expect(parsed_json["items"][0]["commitTime"]).to eql(1597990088000)
        end

        it "should be able to get the latest statistics commit for a training dataset" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          create_statistics_commit(project.id, featurestore_id, "trainingdatasets", parsed_json["id"])
          expect_status(200)
          create_statistics_commit(project.id, featurestore_id, "trainingdatasets", parsed_json["id"],
                                                 commit_time: 1597990088000)
          expect_status(200)
          json_result = get_last_statistics_commit(project.id, featurestore_id, "trainingdatasets", parsed_json["id"])
          expect_status_details(200)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json.key?("items")).to be true
          expect(parsed_json.key?("count")).to be true
          expect(parsed_json["count"] == 2).to be true
          # should contain exactly one item
          expect(parsed_json["items"].length == 1).to be true
          expect(parsed_json["items"][0].key?("content")).to be true
          expect(JSON.parse(parsed_json["items"][0]["content"])).to eql({"columns" => ["a", "b", "c"]})
          expect(parsed_json["items"][0]["commitTime"]).to eql(1597990088000)
        end

        it "deleting a cached feature group should delete all associated statistics commit files from hopsfs" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          expect_status(201)
          parsed_json = JSON.parse(json_result)
          create_statistics_commit(project.id, featurestore_id, "featuregroups", parsed_json["id"])
          expect_status(200)
          create_statistics_commit(project.id, featurestore_id, "featuregroups", parsed_json["id"],
                                                 commit_time: 1597990088000)
          expect_status(200)
          delete_featuregroup_checked(project.id, featurestore_id, parsed_json["id"])
          path = "/Projects/#{project[:projectname]}//Statistics/FeatureGroups/#{featuregroup_name}_1"
          expect(test_dir(path)).to be false
        end

        it "deleting a hopsfs training dataset should delete all associated statistics commit files from hopsfs" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          create_statistics_commit(project.id, featurestore_id, "trainingdatasets", parsed_json["id"])
          expect_status(200)
          create_statistics_commit(project.id, featurestore_id, "trainingdatasets", parsed_json["id"],
                                                 commit_time: 1597990088000)
          expect_status(200)
          delete_trainingdataset_checked(project.id, featurestore_id, parsed_json["id"])
          path = "/Projects/#{project[:projectname]}/Statistics/TrainingDatasets/#{training_dataset_name}_1"
          expect(test_dir(path)).to be false
        end
      end
    end
  end
end