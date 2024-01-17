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
          @featurestore_id = get_featurestore_id(@project[:id])
        end

        context 'create statistics' do

          before :all do
            # cached feature group - no time travel by default
            json_result, _ = create_cached_featuregroup(@project[:id], @featurestore_id)
            expect_status_details(201)
            @cached_feature_group = JSON.parse(json_result)
            # stream feature group - time travel enabled - with two commits
            json_result, _ = create_stream_featuregroup(@project[:id], @featurestore_id, materialize_offline: true)
            expect_status_details(200)
            @stream_feature_group = JSON.parse(json_result)
            commit_metadata = {commitDateString:20201024221125,commitTime:1603577485000,rowsInserted:4,rowsUpdated:0,rowsDeleted:0}
            json_result = commit_cached_featuregroup(@project[:id], @featurestore_id, @stream_feature_group["id"], commit_metadata: commit_metadata)
            commit_metadata = {commitDateString:20201025231125,commitTime:1603667485000,rowsInserted:4,rowsUpdated:0,rowsDeleted:0}
            json_result = commit_cached_featuregroup(@project[:id], @featurestore_id, @stream_feature_group["id"], commit_metadata: commit_metadata)
            expect_status_details(200)
            # training datasets - with and without splits
            all_metadata = create_featureview_training_dataset_from_project(@project)
            @training_dataset = all_metadata["response"]
            @feature_view = all_metadata["featureView"]
            connector = all_metadata["connector"]
            # -- with splits
            splits = [
                {
                    name: "train",
                    percentage: 0.8
                },
                {
                    name: "test",
                    percentage: 0.2
                }
            ]
            json_result, _ = create_featureview_training_dataset(@project[:id], @feature_view, connector, version: nil, splits: splits, train_split: "train")
            expect_status_details(201)
            @training_dataset_with_splits = JSON.parse(json_result)
            expect(@training_dataset_with_splits.key?("splits")).to be true
            expect(@training_dataset_with_splits["splits"].length).to be 2
          end

          # cached feature group - no time travel

          it "should be able to add statistics as a commit to a feature group with time travel disabled" do
            create_statistics_commit_fg(@project[:id], @featurestore_id, @cached_feature_group["id"])
            expect_status_details(200)
          end

          it "should fail to add statistics as a commit to a feature group with time travel disabled and window times" do
            create_statistics_commit_fg(@project[:id], @featurestore_id, @cached_feature_group["id"], computation_time: 1597903688010, window_start_commit_time: 1597903688000, window_end_commit_time: 1597903688010)
            expect_status_details(400, error_code: 270229)
            # all query parameter combinations (including window times) are covered in the unit tests
          end

          # stream feature group - time travel enable

          it "should be able to add statistics as a commit to a stream feature group" do
            create_statistics_commit_fg(@project[:id], @featurestore_id, @stream_feature_group["id"])
            expect_status_details(200)
          end

          it "should be able to add statistics as a commit to a stream feature group with window times" do
            # on two commits
            create_statistics_commit_fg(@project[:id], @featurestore_id, @stream_feature_group["id"], computation_time: 1603667485000, window_start_commit_time: 1603577485000, window_end_commit_time: 1603667485000)
            expect_status_details(200)
            # on a single commit
            create_statistics_commit_fg(@project[:id], @featurestore_id, @stream_feature_group["id"], computation_time: 1603667485000, window_start_commit_time: 1603667485000, window_end_commit_time: 1603667485000)
            expect_status_details(200)
            # all query parameter combinations (including window times) are covered in the unit tests
          end

          # training dataset

          it "should be able to add statistics as a commit to a training dataset" do
            create_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset["version"])
            expect_status_details(200)
          end

          it "should fail to add statistics as a commit to a training dataset with window times" do
            create_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset["version"],
                                        window_start_commit_time: 1597903688000, window_end_commit_time: 1597903688010)
            expect_status_details(400, error_code: 270229)
            # all query parameter combinations are covered in the unit tests
          end
          
          it "should be able to create a training dataset split statistics" do
            splitStatistics = [
              {"name": "train", "featureDescriptiveStatistics": JSON.parse('[{"featureName": "a"}, {"featureName": "b"}, {"featureName": "c"}]') },
              {"name": "test", "featureDescriptiveStatistics": JSON.parse('[{"featureName": "a"}, {"featureName": "b"}, {"featureName": "c"}]') }
            ]
            create_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset_with_splits["version"],
                                        split_statistics: splitStatistics)
            expect_status_details(200)
          end
          
          # transformation functions

          it "should be able to create a transformation function statistics for training dataset without splits" do
            # create general feature statistics content
            create_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset["version"],
                                        before_transformation: false)

            # create feature statistics content before transformation function
            beforeTransformationStatistics = [{"featureName": "c"}]
            create_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset["version"],
                                        feature_descriptive_statistics: beforeTransformationStatistics, before_transformation: true)
            expect_status_details(200)
          end

          it "should be able to create a transformation function statistics for training dataset with splits" do
            # create general feature statistics content
            splitStatistics = [
              {"name": "train", "featureDescriptiveStatistics": JSON.parse('[{"featureName": "a"}, {"featureName": "b"}, {"featureName": "c"}]')},
              {"name": "test", "featureDescriptiveStatistics": JSON.parse('[{"featureName": "a"}, {"featureName": "b"}, {"featureName": "c"}]')}
            ]
            create_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset_with_splits["version"],
                                        split_statistics: splitStatistics, before_transformation: false)
            expect_status_details(200)

            # create feature statistics content before transformation function
            beforeTransformationStatistics = [{"featureName": "c"}]
            create_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset_with_splits["version"],
                                        feature_descriptive_statistics: beforeTransformationStatistics, before_transformation: true)
            expect_status_details(200)
          end
        end

        context 'get statistics' do

          before :all do
            @first_commit_time = 1597990088000
            @second_commit_time = 1603577485000
            @third_commit_time = 1603667485000
            @forth_commit_time = 1603797485000
            # cached feature group
            json_result, _ = create_cached_featuregroup(@project[:id], @featurestore_id)
            expect_status_details(201)
            @cached_feature_group = JSON.parse(json_result)
            create_statistics_commit_fg(@project[:id], @featurestore_id, @cached_feature_group["id"], computation_time: @first_commit_time)
            expect_status_details(200)
            create_statistics_commit_fg(@project[:id], @featurestore_id, @cached_feature_group["id"], computation_time: @second_commit_time)
            expect_status_details(200)
            create_statistics_commit_fg(@project[:id], @featurestore_id, @cached_feature_group["id"], computation_time: @third_commit_time)
            expect_status_details(200)
            # stream feature group
            json_result, _ = create_stream_featuregroup(@project[:id], @featurestore_id, materialize_offline: true, commit_time: @first_commit_time)
            expect_status_details(200)
            @stream_feature_group = JSON.parse(json_result)
            commit_metadata = {commitDateString:20201024221125,commitTime:@second_commit_time,rowsInserted:4,rowsUpdated:0,rowsDeleted:0}
            json_result = commit_cached_featuregroup(@project[:id], @featurestore_id, @stream_feature_group["id"], commit_metadata: commit_metadata)
            expect_status_details(200)
            commit_metadata = {commitDateString:20201025231125,commitTime:@third_commit_time,rowsInserted:4,rowsUpdated:0,rowsDeleted:0}
            json_result = commit_cached_featuregroup(@project[:id], @featurestore_id, @stream_feature_group["id"], commit_metadata: commit_metadata)
            expect_status_details(200)
            create_statistics_commit_fg(@project[:id], @featurestore_id, @stream_feature_group["id"], computation_time: @second_commit_time, window_start_commit_time: @first_commit_time, window_end_commit_time: @second_commit_time)
            expect_status_details(200)
            create_statistics_commit_fg(@project[:id], @featurestore_id, @stream_feature_group["id"], computation_time: @third_commit_time, window_start_commit_time: nil, window_end_commit_time: @third_commit_time)
            expect_status_details(200)
            create_statistics_commit_fg(@project[:id], @featurestore_id, @stream_feature_group["id"], computation_time: @forth_commit_time, window_start_commit_time: @first_commit_time, window_end_commit_time: @third_commit_time)
            expect_status_details(200)
            # training dataset
            all_metadata = create_featureview_training_dataset_from_project(@project)
            @training_dataset = all_metadata["response"]
            @feature_view = all_metadata["featureView"]
            connector = all_metadata["connector"]
            create_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset["version"],
                                        computation_time: @first_commit_time, before_transformation: false)
            expect_status_details(200)
            beforeTransformationStatistics = [{"featureName": "c"}]
            create_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset["version"],
                                        feature_descriptive_statistics: beforeTransformationStatistics, computation_time: @first_commit_time, before_transformation: true)
            # -- with splits
            splits = [
                {
                    name: "train",
                    percentage: 0.8
                },
                {
                    name: "test",
                    percentage: 0.2
                }
            ]
            json_result, _ = create_featureview_training_dataset(@project[:id], @feature_view, connector, version: nil, splits: splits, train_split: "train")
            expect_status_details(201)
            @training_dataset_with_splits = JSON.parse(json_result)
            expect(@training_dataset_with_splits.key?("splits")).to be true
            expect(@training_dataset_with_splits["splits"].length).to be 2
            splitStatistics = [
              {"name": "train", "featureDescriptiveStatistics": JSON.parse('[{"featureName": "a"}, {"featureName": "b"}, {"featureName": "c"}]') },
              {"name": "test", "featureDescriptiveStatistics": JSON.parse('[{"featureName": "a"}, {"featureName": "b"}, {"featureName": "c"}]') }
            ]
            create_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset_with_splits["version"],
                                        split_statistics: splitStatistics, computation_time: @first_commit_time)
            expect_status_details(200)
            beforeTransformationStatistics = [{"featureName": "c"}]
            create_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset_with_splits["version"],
                                        feature_descriptive_statistics: beforeTransformationStatistics, computation_time: @first_commit_time, before_transformation: true)
            expect_status_details(200)
          end

          # feature groups

          it "should be able to get statistics by specific computation time with content field of a feature group with time travel disabled" do
            json_result = get_statistics_commit_fg(@project[:id], @featurestore_id, @cached_feature_group["id"], computation_time: @first_commit_time)
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 1).to be true
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 3).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"].sort_by {|fds| fds["featureName"]}
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(sorted_feature_descriptive_statistics[1]["featureName"]).to eql("b")
            expect(sorted_feature_descriptive_statistics[2]["featureName"]).to eql("c")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@first_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)

            json_result = get_statistics_commit_fg(@project[:id], @featurestore_id, @cached_feature_group["id"], computation_time: @third_commit_time)
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 3).to be true # computation_time_ltoeq is used
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 3).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"].sort_by {|fds| fds["featureName"]}
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(sorted_feature_descriptive_statistics[1]["featureName"]).to eql("b")
            expect(sorted_feature_descriptive_statistics[2]["featureName"]).to eql("c")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@third_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get statistics by feature name and computation time with content field of a feature group with time travel disabled" do
            json_result = get_statistics_commit_fg(@project[:id], @featurestore_id, @cached_feature_group["id"], computation_time: @first_commit_time, feature_names: "a")
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 1).to be true
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 1).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"]
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@first_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)

            json_result = get_statistics_commit_fg(@project[:id], @featurestore_id, @cached_feature_group["id"], computation_time: @second_commit_time, feature_names: "b")
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 2).to be true
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 1).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"]
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("b")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@second_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get statistics by specific computation time with content field of a stream feature group" do
            json_result = get_statistics_commit_fg(@project[:id], @featurestore_id, @stream_feature_group["id"], computation_time: @second_commit_time)
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 1).to be true
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 3).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"].sort_by {|fds| fds["featureName"]}
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(sorted_feature_descriptive_statistics[1]["featureName"]).to eql("b")
            expect(sorted_feature_descriptive_statistics[2]["featureName"]).to eql("c")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@second_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)

            json_result = get_statistics_commit_fg(@project[:id], @featurestore_id, @stream_feature_group["id"], computation_time: @third_commit_time)
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 2).to be true # computation_time_ltoeq is used
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 3).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"].sort_by {|fds| fds["featureName"]}
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(sorted_feature_descriptive_statistics[1]["featureName"]).to eql("b")
            expect(sorted_feature_descriptive_statistics[2]["featureName"]).to eql("c")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@third_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get statistics by feature name and specific commit with content field of a stream feature group" do
            json_result = get_statistics_commit_fg(@project[:id], @featurestore_id, @stream_feature_group["id"], computation_time: @second_commit_time, feature_names: "a")
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 1).to be true
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 1).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"]
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@second_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)

            json_result = get_statistics_commit_fg(@project[:id], @featurestore_id, @stream_feature_group["id"], computation_time: @third_commit_time, feature_names: "b")
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 2).to be true # computation_time_ltoeq is used
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 1).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"]
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("b")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@third_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get statistics by specific window commit with content field of a stream feature group" do
            json_result = get_statistics_commit_by_window_fg(@project[:id], @featurestore_id, @stream_feature_group["id"], window_start_commit_time: @first_commit_time, window_end_commit_time: @second_commit_time)
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 1).to be true  # there is only one statistics within the commit window
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 3).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"].sort_by {|fds| fds["featureName"]}
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(sorted_feature_descriptive_statistics[1]["featureName"]).to eql("b")
            expect(sorted_feature_descriptive_statistics[2]["featureName"]).to eql("c")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@second_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)

            json_result = get_statistics_commit_by_window_fg(@project[:id], @featurestore_id, @stream_feature_group["id"], window_start_commit_time: @first_commit_time, window_end_commit_time: @third_commit_time)
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 2).to be true # there are two statistics within the commit window
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 3).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"].sort_by {|fds| fds["featureName"]}
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(sorted_feature_descriptive_statistics[1]["featureName"]).to eql("b")
            expect(sorted_feature_descriptive_statistics[2]["featureName"]).to eql("c")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@forth_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get statistics by feature name and specific window commit with content field of a stream feature group" do
            json_result = get_statistics_commit_by_window_fg(@project[:id], @featurestore_id, @stream_feature_group["id"], window_start_commit_time: @first_commit_time, window_end_commit_time: @second_commit_time, feature_names: "a")
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 1).to be true  # there is only one statistics within the commit window
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 1).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"]
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@second_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)

            json_result = get_statistics_commit_by_window_fg(@project[:id], @featurestore_id, @stream_feature_group["id"], window_start_commit_time: @first_commit_time, window_end_commit_time: @third_commit_time, feature_names: "b")
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 2).to be true  # there are two statistics within the commit window
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 1).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"]
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("b")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@forth_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end
          
          it "should be able to get the last computed statistics for a feature group" do
            json_result = get_last_computed_statistics_commit_fg(@project[:id], @featurestore_id, @cached_feature_group["id"])
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"]).to eql(3) # cached feature group has three statistics
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true # should contain only the last statistics
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 3).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"].sort_by {|fds| fds["featureName"]}
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(sorted_feature_descriptive_statistics[1]["featureName"]).to eql("b")
            expect(sorted_feature_descriptive_statistics[2]["featureName"]).to eql("c")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@third_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get the last computed statistics for a feature group by feature name" do
            json_result = get_last_computed_statistics_commit_fg(@project[:id], @featurestore_id, @cached_feature_group["id"], feature_names: "a")
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"]).to eql(3)  # cached feature group has three statistics
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true  # should contain only the last statistics
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 1).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"]
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@third_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end
  
          it "should be able to get the last computed statistics for a stream feature group" do
            json_result = get_last_computed_statistics_commit_fg(@project[:id], @featurestore_id, @stream_feature_group["id"])
            expect_status_details(200)
            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"]).to eql(3)  # stream feature group has three statistics
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true  # should contain only the last statistics
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 3).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"].sort_by {|fds| fds["featureName"]}
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(sorted_feature_descriptive_statistics[1]["featureName"]).to eql("b")
            expect(sorted_feature_descriptive_statistics[2]["featureName"]).to eql("c")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@forth_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get the last computed statistics for a stream feature group by feature name" do
            json_result = get_last_computed_statistics_commit_fg(@project[:id], @featurestore_id, @stream_feature_group["id"], feature_names: "a")
            expect_status_details(200)
            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"]).to eql(3)  # stream feature group has three statistics
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true  # should contain only the last statistics
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 1).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"]
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@forth_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end
  
          it "should be able to get the statistics computed on the last feature values at a specific commit of a stream feature group" do
            json_result = get_last_computed_statistics_by_window_fg(@project[:id], @featurestore_id, @stream_feature_group["id"], window_end_commit_time: @third_commit_time)
            expect_status_details(200)
  
            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"]).to eql(1) # stream feature group has three statistics, but only one per commit window
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 3).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"].sort_by {|fds| fds["featureName"]}
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(sorted_feature_descriptive_statistics[1]["featureName"]).to eql("b")
            expect(sorted_feature_descriptive_statistics[2]["featureName"]).to eql("c")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@third_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get the statistics computed on the last feature values of a stream feature group by feature name" do
            json_result = get_last_computed_statistics_by_window_fg(@project[:id], @featurestore_id, @stream_feature_group["id"], window_end_commit_time: @third_commit_time, feature_names: "a")
            expect_status_details(200)
  
            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"]).to eql(1)  # stream feature group has three statistics, but only one per commit window
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 1).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"]
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@third_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get all statistics of a feature group with time travel disabled" do
            json_result = get_statistics_commits_fg(@project[:id], @featurestore_id, @cached_feature_group["id"])
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 3).to be true
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 3).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 3).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"].sort_by {|fds| fds["featureName"]}
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(sorted_feature_descriptive_statistics[1]["featureName"]).to eql("b")
            expect(sorted_feature_descriptive_statistics[2]["featureName"]).to eql("c")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@third_commit_time) # computation_time:desc
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get all statistics of a feature group with time travel disabled by feature name" do
            json_result = get_statistics_commits_fg(@project[:id], @featurestore_id, @cached_feature_group["id"], feature_names: "a")
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 3).to be true
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 3).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 1).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"]
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@third_commit_time) # computation_time:desc 
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get all statistics of a stream feature group" do
            json_result = get_statistics_commits_fg(@project[:id], @featurestore_id, @stream_feature_group["id"])
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 3).to be true
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 3).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 3).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"].sort_by {|fds| fds["featureName"]}
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(sorted_feature_descriptive_statistics[1]["featureName"]).to eql("b")
            expect(sorted_feature_descriptive_statistics[2]["featureName"]).to eql("c")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@forth_commit_time)  # computation_time:desc
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get all statistics of a stream feature group by feature name" do
            json_result = get_statistics_commits_fg(@project[:id], @featurestore_id, @stream_feature_group["id"], feature_names: "a")
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            # should contain more than one item - stream feature group has two statistics
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 3).to be true
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 3).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 1).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"]
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@forth_commit_time) # computation_time:desc
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          # training datasets

          it "should be able to get a specific statistics commit with content field of a training dataset" do
            json_result = get_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset["version"])
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 1).to be true # should contain exactly one item - training datasets only has one statistics
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 3).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"].sort_by {|fds| fds["featureName"]}
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(sorted_feature_descriptive_statistics[1]["featureName"]).to eql("b")
            expect(sorted_feature_descriptive_statistics[2]["featureName"]).to eql("c")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@first_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get a specific statistics commit with content field of a training dataset by feature name" do
            json_result = get_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset["version"],
                                                   feature_names: "a")
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            # should contain exactly one item
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 1).to be true
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 1).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"]
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@first_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get the last computed statistics for a training dataset" do
            json_result = get_last_computed_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset["version"])
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            # should contain exactly one item
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 1).to be true
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 3).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"].sort_by {|fds| fds["featureName"]}
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(sorted_feature_descriptive_statistics[1]["featureName"]).to eql("b")
            expect(sorted_feature_descriptive_statistics[2]["featureName"]).to eql("c")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@first_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get the last computed statistics for a training dataset by feature name" do
            json_result = get_last_computed_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset["version"],
                                                                 feature_names: "a")
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            # should contain exactly one item
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 1).to be true
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["featureDescriptiveStatistics"].length == 1).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["featureDescriptiveStatistics"]
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@first_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get the last computed statistics for training dataset splits" do
            json_result = get_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset_with_splits["version"])
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            # should contain exactly one item - training datasets only has one statistics
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 1).to be true
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("splitStatistics")).to be true
            expect(parsed_json["items"][0]["splitStatistics"].length).to be 2
            expect(parsed_json["items"][0]["splitStatistics"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["splitStatistics"][0]["featureDescriptiveStatistics"].length == 3).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["splitStatistics"][0]["featureDescriptiveStatistics"].sort_by {|fds| fds["featureName"]}
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(sorted_feature_descriptive_statistics[1]["featureName"]).to eql("b")
            expect(sorted_feature_descriptive_statistics[2]["featureName"]).to eql("c")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@first_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get the last computed statistics for training dataset splits by feature name" do
            json_result = get_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset_with_splits["version"],
                                                   feature_names: "a")
            expect_status_details(200)

            parsed_json = JSON.parse(json_result)
            # should contain exactly one item
            expect(parsed_json.key?("count")).to be true
            expect(parsed_json["count"] == 1).to be true
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"].length == 1).to be true
            expect(parsed_json["items"][0].key?("splitStatistics")).to be true
            expect(parsed_json["items"][0]["splitStatistics"].length).to be 2
            expect(parsed_json["items"][0]["splitStatistics"][0].key?("featureDescriptiveStatistics")).to be true
            expect(parsed_json["items"][0]["splitStatistics"][0]["featureDescriptiveStatistics"].length == 1).to be true
            sorted_feature_descriptive_statistics = parsed_json["items"][0]["splitStatistics"][0]["featureDescriptiveStatistics"]
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(parsed_json["items"][0]["computationTime"]).to eql(@first_commit_time)
            expect(parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          # transformation functions

          it "should be able to get the last computed statistics before transformation functions of a training
          dataset" do
            stat_json_result = get_last_computed_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset["version"],
                                                                      before_transformation: false)
            expect_status_details(200)

            stat_parsed_json = JSON.parse(stat_json_result)
            # should contain exactly one item
            expect(stat_parsed_json.key?("count")).to be true
            expect(stat_parsed_json["count"]).to eql(1)
            expect(stat_parsed_json.key?("items")).to be true
            expect(stat_parsed_json["items"].length == 1).to be true
            expect(stat_parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            expect(stat_parsed_json["items"][0]["featureDescriptiveStatistics"].length == 3).to be true
            sorted_feature_descriptive_statistics = stat_parsed_json["items"][0]["featureDescriptiveStatistics"].sort_by {|fds| fds["featureName"]}
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(sorted_feature_descriptive_statistics[1]["featureName"]).to eql("b")
            expect(sorted_feature_descriptive_statistics[2]["featureName"]).to eql("c")
            expect(stat_parsed_json["items"][0]["computationTime"]).to eql(@first_commit_time)
            expect(stat_parsed_json["items"][0]["rowPercentage"]).to eql(1.0)

            before_transformation_json_result = get_last_computed_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset["version"],
                                                                                       before_transformation: true)
            expect_status_details(200)
            before_transformation_parsed_json = JSON.parse(before_transformation_json_result)
            # should contain exactly one item
            expect(before_transformation_parsed_json.key?("count")).to be true
            expect(before_transformation_parsed_json["count"]).to eql(1)
            expect(before_transformation_parsed_json.key?("items")).to be true
            expect(before_transformation_parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            # transformation function statistics was computed for feature "c" only
            expect(before_transformation_parsed_json["items"][0]["featureDescriptiveStatistics"].length == 1).to be true
            expect(before_transformation_parsed_json["items"][0]["featureDescriptiveStatistics"][0]["featureName"]).to eql("c")
            expect(before_transformation_parsed_json["items"][0]["computationTime"]).to eql(@first_commit_time)
            expect(before_transformation_parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end

          it "should be able to get the last computed statistics before transformation functions of a training dataset
          split" do
            json_result = get_last_computed_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset_with_splits["version"],
                                                                 before_transformation: false)
            expect_status_details(200)

            stat_parsed_json = JSON.parse(json_result)
            # should contain exactly one item
            expect(stat_parsed_json.key?("count")).to be true
            expect(stat_parsed_json["count"] == 1).to be true
            expect(stat_parsed_json.key?("items")).to be true
            expect(stat_parsed_json["items"].length == 1).to be true
            expect(stat_parsed_json["items"][0].key?("splitStatistics")).to be true
            expect(stat_parsed_json["items"][0]["splitStatistics"].length).to be 2
            expect(stat_parsed_json["items"][0]["splitStatistics"][0].key?("featureDescriptiveStatistics")).to be true
            expect(stat_parsed_json["items"][0]["splitStatistics"][0]["featureDescriptiveStatistics"].length == 3).to be true
            sorted_feature_descriptive_statistics = stat_parsed_json["items"][0]["splitStatistics"][0]["featureDescriptiveStatistics"].sort_by {|fds| fds["featureName"]}
            expect(sorted_feature_descriptive_statistics[0]["featureName"]).to eql("a")
            expect(sorted_feature_descriptive_statistics[1]["featureName"]).to eql("b")
            expect(sorted_feature_descriptive_statistics[2]["featureName"]).to eql("c")
            expect(stat_parsed_json["items"][0]["computationTime"]).to eql(@first_commit_time)
            expect(stat_parsed_json["items"][0]["rowPercentage"]).to eql(1.0)

            before_transformation_json_result = get_last_computed_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset_with_splits["version"],
                                                                                       before_transformation: true)
            expect_status_details(200)
            before_transformation_parsed_json = JSON.parse(before_transformation_json_result)
            # should contain exactly one item
            expect(before_transformation_parsed_json["count"]).to eql(1)
            expect(before_transformation_parsed_json["items"][0].key?("featureDescriptiveStatistics")).to be true
            # transformation function statistics was computed for feature "c" only
            expect(before_transformation_parsed_json["items"][0]["featureDescriptiveStatistics"].length == 1).to be true
            expect(before_transformation_parsed_json["items"][0]["featureDescriptiveStatistics"][0]["featureName"]).to eql("c")
            expect(before_transformation_parsed_json["items"][0]["computationTime"]).to eql(@first_commit_time)
            expect(before_transformation_parsed_json["items"][0]["rowPercentage"]).to eql(1.0)
          end
        end

        context 'delete statistics' do

          it "deleting a cached feature group should delete all associated statistics commit files from hopsfs" do
            json_result, featuregroup_name = create_cached_featuregroup(@project[:id], @featurestore_id)
            expect_status_details(201)
            parsed_json = JSON.parse(json_result)
            create_statistics_commit_fg(@project[:id], @featurestore_id, parsed_json["id"])
            expect_status_details(200)
            create_statistics_commit_fg(@project[:id], @featurestore_id, parsed_json["id"], computation_time: 1597990088000)
            expect_status_details(200)
            path = "/Projects/#{@project[:projectname]}//Statistics/FeatureGroups/#{featuregroup_name}_1"
            expect(test_dir(path)).to be true
            delete_featuregroup_checked(@project[:id], @featurestore_id, parsed_json["id"])
            expect(test_dir(path)).to be false
          end

          it "deleting a stream feature group should delete all associated statistics commit files from hopsfs" do
            json_result, featuregroup_name = create_stream_featuregroup(@project[:id], @featurestore_id, materialize_offline: true, commit_time: 1597990088000)
            expect_status_details(200)
            parsed_json = JSON.parse(json_result)
            create_statistics_commit_fg(@project[:id], @featurestore_id, parsed_json["id"])
            expect_status_details(200)
            create_statistics_commit_fg(@project[:id], @featurestore_id, parsed_json["id"], computation_time: 1597990088000)
            expect_status_details(200)
            path = "/Projects/#{@project[:projectname]}//Statistics/FeatureGroups/#{featuregroup_name}_1"
            expect(test_dir(path)).to be true
            delete_featuregroup_checked(@project[:id], @featurestore_id, parsed_json["id"])
            expect(test_dir(path)).to be false
          end

          it "deleting a hopsfs training dataset should delete all associated statistics commit files from hopsfs" do
            all_metadata = create_featureview_training_dataset_from_project(@project)
            @training_dataset = all_metadata["response"]
            @feature_view = all_metadata["featureView"]
            create_statistics_commit_td(@project[:id], @featurestore_id, @feature_view["name"], @feature_view["version"], @training_dataset["version"],
                                        computation_time: 1597990088000, before_transformation: false)
            expect_status_details(200)
            path = "/Projects/#{@project[:projectname]}/Statistics/TrainingDatasets/#{@training_dataset["name"]}_1"
            expect(test_dir(path)).to be true
            delete_trainingdataset_checked(@project[:id], @featurestore_id, @training_dataset["id"])
            expect(test_dir(path)).to be false
          end
        end
      end
    end
  end
end