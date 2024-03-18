# This file is part of Hopsworks
# Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

require 'json'

describe "On #{ENV['OS']}" do
  after(:all) { clean_all_test_projects(spec: "featureviewquery") }

  describe "feature view query" do
    describe "internal" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to create batch query" do
          featurestore_id = get_featurestore_id(@project.id)
          featurestore_name = get_featurestore_name(@project.id)
          featuregroup_suffix = short_random_id
          query = make_sample_query(@project, featurestore_id, featuregroup_suffix: featuregroup_suffix)
          json_result = create_feature_view(@project.id, featurestore_id, query)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]
          query_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/query/batch?start_time=1234&end_time=4321"
          expect_status_details(200)

          parsed_query_result = JSON.parse(query_result)
          expect(parsed_query_result["featureStoreId"]).to eql(featurestore_id)
          expect(parsed_query_result["featureStoreName"]).to eql(featurestore_name)
          expect(parsed_query_result["leftFeatureGroup"]["id"]).to eql(query[:leftFeatureGroup][:id])
          expect(parsed_query_result["leftFeatures"][0]["name"]).to eql(query[:leftFeatures][0][:name])
          expect(parsed_query_result["leftFeatures"][1]["name"]).to eql(query[:leftFeatures][1][:name])
          expect(parsed_query_result["joins"][0]["query"]["leftFeatureGroup"]["id"]).to eql(query[:joins][0][:query][:leftFeatureGroup][:id])
          expect(parsed_query_result["joins"][0]["query"]["leftFeatures"][0]["name"]).to eql(query[:joins][0][:query][:leftFeatures][1][:name])
          # a_testfeature1 > 0
          expect(parsed_query_result["filter"]["leftLogic"]["leftLogic"]["leftFilter"]["feature"]["name"]).to eql(query[:filter][:leftFilter][:feature][:name])
          expect(parsed_query_result["filter"]["leftLogic"]["leftLogic"]["leftFilter"]["condition"]).to eql(query[:filter][:leftFilter][:condition])
          expect(parsed_query_result["filter"]["leftLogic"]["leftLogic"]["leftFilter"]["value"]).to eql(query[:filter][:leftFilter][:value])
          # ts <= 1234
          expect(parsed_query_result["filter"]["leftLogic"]["rightLogic"]["leftFilter"]["feature"]["name"]).to eql("ts")
          expect(parsed_query_result["filter"]["leftLogic"]["rightLogic"]["leftFilter"]["condition"]).to eql("GREATER_THAN_OR_EQUAL")
          expect(parsed_query_result["filter"]["leftLogic"]["rightLogic"]["leftFilter"]["value"]).to eql("1234")
          # ts >= 4321
          expect(parsed_query_result["filter"]["rightLogic"]["leftFilter"]["feature"]["name"]).to eql("ts")
          expect(parsed_query_result["filter"]["rightLogic"]["leftFilter"]["condition"]).to eql("LESS_THAN")
          expect(parsed_query_result["filter"]["rightLogic"]["leftFilter"]["value"]).to eql("4321")
        end

        it "should be able to retrieve original query" do
          featurestore_id = get_featurestore_id(@project.id)
          featurestore_name = get_featurestore_name(@project.id)
          featuregroup_suffix = short_random_id
          query = make_sample_query(@project, featurestore_id, featuregroup_suffix: featuregroup_suffix)
          json_result = create_feature_view(@project.id, featurestore_id, query)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]
          query_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/query"
          expect_status_details(200)
          parsed_query_result = JSON.parse(query_result)
          expect(parsed_query_result["featureStoreId"]).to eql(featurestore_id)
          expect(parsed_query_result["featureStoreName"]).to eql(featurestore_name)
          expect(parsed_query_result["leftFeatureGroup"]["id"]).to eql(query[:leftFeatureGroup][:id])
          expect(parsed_query_result["leftFeatures"][0]["name"]).to eql(query[:leftFeatures][0][:name])
          expect(parsed_query_result["leftFeatures"][1]["name"]).to eql(query[:leftFeatures][1][:name])
          expect(parsed_query_result["joins"][0]["query"]["leftFeatureGroup"]["id"]).to eql(query[:joins][0][:query][:leftFeatureGroup][:id])
          expect(parsed_query_result["joins"][0]["query"]["leftFeatures"][0]["name"]).to eql(query[:joins][0][:query][:leftFeatures][1][:name])
          expect(parsed_query_result["filter"]["leftFilter"]["feature"]["name"]).to eql(query[:filter][:leftFilter][:feature][:name])
          expect(parsed_query_result["filter"]["leftFilter"]["condition"]).to eql(query[:filter][:leftFilter][:condition])
          expect(parsed_query_result["filter"]["leftFilter"]["value"]).to eql(query[:filter][:leftFilter][:value])
        end

        it "should be able to retrieve original query with nested filters" do
          featurestore_id = get_featurestore_id(@project.id)
          featurestore_name = get_featurestore_name(@project.id)
          featuregroup_suffix = short_random_id
          query = make_sample_query(@project, featurestore_id, featuregroup_suffix: featuregroup_suffix)
          inner_filter = {
                  type: "SINGLE",
                  leftFilter: {
                    feature: {
                      name: "b_testfeature1",
                      featureGroupId: query[:joins][0][:query][:leftFeatureGroup][:id]
                    },
                    condition: "GREATER_THAN",
                    value: "0"
                  }
                }
          query[:joins][0][:query][:filter] = inner_filter
          json_result = create_feature_view(@project.id, featurestore_id, query)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]
          query_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/query"
          expect_status_details(200)
          parsed_query_result = JSON.parse(query_result)
          expect(parsed_query_result["featureStoreId"]).to eql(featurestore_id)
          expect(parsed_query_result["featureStoreName"]).to eql(featurestore_name)
          expect(parsed_query_result["leftFeatureGroup"]["id"]).to eql(query[:leftFeatureGroup][:id])
          expect(parsed_query_result["leftFeatures"][0]["name"]).to eql(query[:leftFeatures][0][:name])
          expect(parsed_query_result["leftFeatures"][1]["name"]).to eql(query[:leftFeatures][1][:name])
          expect(parsed_query_result["joins"][0]["query"]["leftFeatureGroup"]["id"]).to eql(query[:joins][0][:query][:leftFeatureGroup][:id])
          expect(parsed_query_result["joins"][0]["query"]["leftFeatures"][0]["name"]).to eql(query[:joins][0][:query][:leftFeatures][1][:name])
          expect(parsed_query_result["filter"]["type"]).to eql("AND")
          expect(parsed_query_result["filter"]["leftFilter"]["feature"]["name"]).to eql(query[:filter][:leftFilter][:feature][:name])
          expect(parsed_query_result["filter"]["leftFilter"]["condition"]).to eql(query[:filter][:leftFilter][:condition])
          expect(parsed_query_result["filter"]["leftFilter"]["value"]).to eql(query[:filter][:leftFilter][:value])
          expect(parsed_query_result["filter"]["rightFilter"]["feature"]["name"]).to eql(inner_filter[:leftFilter][:feature][:name])
          expect(parsed_query_result["filter"]["rightFilter"]["condition"]).to eql(inner_filter[:leftFilter][:condition])
          expect(parsed_query_result["filter"]["rightFilter"]["value"]).to eql(inner_filter[:leftFilter][:value])
        end

        it "should be able to retrieve original query with nested filter logic" do
          featurestore_id = get_featurestore_id(@project.id)
          featurestore_name = get_featurestore_name(@project.id)
          featuregroup_suffix = short_random_id
          query = make_sample_query(@project, featurestore_id, featuregroup_suffix: featuregroup_suffix)
          inner_filter = {
                  type: "OR",
                  leftFilter: {
                    feature: {
                      name: "b_testfeature1",
                      featureGroupId: query[:joins][0][:query][:leftFeatureGroup][:id]
                    },
                    condition: "GREATER_THAN",
                    value: "0"
                  },
                  rightFilter: {
                    feature: {
                      name: "b_testfeature1",
                      featureGroupId: query[:joins][0][:query][:leftFeatureGroup][:id]
                    },
                    condition: "LESS_THAN",
                    value: "10"
                  }
                }
          query[:joins][0][:query][:filter] = inner_filter
          json_result = create_feature_view(@project.id, featurestore_id, query)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]
          query_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/query"
          expect_status_details(200)
          parsed_query_result = JSON.parse(query_result)
          expect(parsed_query_result["featureStoreId"]).to eql(featurestore_id)
          expect(parsed_query_result["featureStoreName"]).to eql(featurestore_name)
          expect(parsed_query_result["leftFeatureGroup"]["id"]).to eql(query[:leftFeatureGroup][:id])
          expect(parsed_query_result["leftFeatures"][0]["name"]).to eql(query[:leftFeatures][0][:name])
          expect(parsed_query_result["leftFeatures"][1]["name"]).to eql(query[:leftFeatures][1][:name])
          expect(parsed_query_result["joins"][0]["query"]["leftFeatureGroup"]["id"]).to eql(query[:joins][0][:query][:leftFeatureGroup][:id])
          expect(parsed_query_result["joins"][0]["query"]["leftFeatures"][0]["name"]).to eql(query[:joins][0][:query][:leftFeatures][1][:name])
          expect(parsed_query_result["filter"]["type"]).to eql("AND")
          expect(parsed_query_result["filter"]["leftFilter"]["feature"]["name"]).to eql(query[:filter][:leftFilter][:feature][:name])
          expect(parsed_query_result["filter"]["leftFilter"]["condition"]).to eql(query[:filter][:leftFilter][:condition])
          expect(parsed_query_result["filter"]["leftFilter"]["value"]).to eql(query[:filter][:leftFilter][:value])
          expect(parsed_query_result["filter"]["rightLogic"]["type"]).to eql("OR")
          expect(parsed_query_result["filter"]["rightLogic"]["leftFilter"]["feature"]["name"]).to eql(inner_filter[:leftFilter][:feature][:name])
          expect(parsed_query_result["filter"]["rightLogic"]["leftFilter"]["condition"]).to eql(inner_filter[:leftFilter][:condition])
          expect(parsed_query_result["filter"]["rightLogic"]["leftFilter"]["value"]).to eql(inner_filter[:leftFilter][:value])
          expect(parsed_query_result["filter"]["rightLogic"]["rightFilter"]["feature"]["name"]).to eql(inner_filter[:rightFilter][:feature][:name])
          expect(parsed_query_result["filter"]["rightLogic"]["rightFilter"]["condition"]).to eql(inner_filter[:rightFilter][:condition])
          expect(parsed_query_result["filter"]["rightLogic"]["rightFilter"]["value"]).to eql(inner_filter[:rightFilter][:value])
        end

        it "should be able to create batch query using retrieved query" do
          featurestore_id = get_featurestore_id(@project.id)
          featurestore_name = get_featurestore_name(@project.id)
          featuregroup_suffix = short_random_id
          query = make_sample_query(@project, featurestore_id, featuregroup_suffix: featuregroup_suffix)
          json_result = create_feature_view(@project.id, featurestore_id, query)
          expect_status_details(201)
          parsed_json = JSON.parse(json_result)

          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]
          query_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/query"
          expect_status_details(200)
          parsed_query_result = JSON.parse(query_result)
          json_result = create_feature_view(@project.id, featurestore_id, parsed_query_result)
          expect_status_details(201)
          parsed_json_new = JSON.parse(json_result)
          feature_view_version_new = parsed_json_new["version"]

          query_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version_new}/query/batch?start_time=1234&end_time=4321"
          expect_status_details(200)

          parsed_query_result = JSON.parse(query_result)
          expect(parsed_query_result["featureStoreId"]).to eql(featurestore_id)
          expect(parsed_query_result["featureStoreName"]).to eql(featurestore_name)
          expect(parsed_query_result["leftFeatureGroup"]["id"]).to eql(query[:leftFeatureGroup][:id])
          expect(parsed_query_result["leftFeatures"][0]["name"]).to eql(query[:leftFeatures][0][:name])
          expect(parsed_query_result["leftFeatures"][1]["name"]).to eql(query[:leftFeatures][1][:name])
          expect(parsed_query_result["joins"][0]["query"]["leftFeatureGroup"]["id"]).to eql(query[:joins][0][:query][:leftFeatureGroup][:id])
          expect(parsed_query_result["joins"][0]["query"]["leftFeatures"][0]["name"]).to eql(query[:joins][0][:query][:leftFeatures][1][:name])
          # a_testfeature1 > 0
          expect(parsed_query_result["filter"]["leftLogic"]["leftLogic"]["leftFilter"]["feature"]["name"]).to eql(query[:filter][:leftFilter][:feature][:name])
          expect(parsed_query_result["filter"]["leftLogic"]["leftLogic"]["leftFilter"]["condition"]).to eql(query[:filter][:leftFilter][:condition])
          expect(parsed_query_result["filter"]["leftLogic"]["leftLogic"]["leftFilter"]["value"]).to eql(query[:filter][:leftFilter][:value])
          # ts <= 1234
          expect(parsed_query_result["filter"]["leftLogic"]["rightLogic"]["leftFilter"]["feature"]["name"]).to eql("ts")
          expect(parsed_query_result["filter"]["leftLogic"]["rightLogic"]["leftFilter"]["condition"]).to eql("GREATER_THAN_OR_EQUAL")
          expect(parsed_query_result["filter"]["leftLogic"]["rightLogic"]["leftFilter"]["value"]).to eql("1234")
          # ts >= 4321
          expect(parsed_query_result["filter"]["rightLogic"]["leftFilter"]["feature"]["name"]).to eql("ts")
          expect(parsed_query_result["filter"]["rightLogic"]["leftFilter"]["condition"]).to eql("LESS_THAN")
          expect(parsed_query_result["filter"]["rightLogic"]["leftFilter"]["value"]).to eql("4321")
        end


        it "should be able to create sql string with different type of event time filter without them included in the selected feature list" do
          featurestore_id = get_featurestore_id(@project.id)
          project_name = @project.projectname.downcase
          featurestore_name = get_featurestore_name(@project.id)
          featuregroup_suffix = short_random_id
          features_a = [
            {type: "INT", name: "id", primary: true },
            {type: "INT", name: "a_testfeature1"},
            {type: "INT", name: "a_testfeature2"},
            {type: "TIMESTAMP", name: "ts" },
          ]

          fg = create_cached_featuregroup_checked_return_fg(@project.id, featurestore_id,
                                                         "test_fg_a#{featuregroup_suffix}",
                                                     features: features_a,
                                                     event_time: "ts")
          query = {
            leftFeatureGroup: {
              id: fg[:id],
              type: fg[:type]
            },
            leftFeatures: [{ name: 'a_testfeature1' }, { name: 'a_testfeature2' }],
            filter: {
              type: "AND",
              leftFilter: {
                feature: {
                  name: "a_testfeature1",
                  featureGroupId: fg[:id]
                },
                condition: "GREATER_THAN",
                value: 0
              },
              rightFilter: {
                feature: {
                  name: "ts",
                  featureGroupId: fg[:id]
                },
                condition: "GREATER_THAN",
                value: "2022-02-01 00:00:00"
              }
            }
          }

          query_result = put "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/query", query.to_json
          expect_status_details(200)
          parsed_query_result = JSON.parse(query_result)

          expect(parsed_query_result['query']).to eql(
                                                    "SELECT `fg0`.`a_testfeature1` `a_testfeature1`, `fg0`.`a_testfeature2` `a_testfeature2`\n" +
                                                      "FROM `#{featurestore_name}`.`test_fg_a#{featuregroup_suffix}_1` `fg0`\n" +
                                                      "WHERE `fg0`.`a_testfeature1` > #{query[:filter][:leftFilter][:value]} AND `fg0`.`ts` > TIMESTAMP '#{query[:filter][:rightFilter][:value]}.000'"
                                                  )

          expect(parsed_query_result['queryOnline']).to eql(
                                                          "SELECT `fg0`.`a_testfeature1` `a_testfeature1`, `fg0`.`a_testfeature2` `a_testfeature2`\n" +
                                                "FROM `#{project_name.downcase}`.`test_fg_a#{featuregroup_suffix}_1` `fg0`\n" +
                                                "WHERE `fg0`.`a_testfeature1` > #{query[:filter][:leftFilter][:value]} AND `fg0`.`ts` > TIMESTAMP '#{query[:filter][:rightFilter][:value]}.000'"
                                                        )
        end


        it "should be able to create sql string  in joins with different type of event time filter without them included in the selected feature list" do
          featurestore_id = get_featurestore_id(@project.id)
          project_name = @project.projectname.downcase
          featurestore_name = get_featurestore_name(@project.id)
          featuregroup_suffix = short_random_id
          features_a = [
            {type: "INT", name: "id", primary: true },
            {type: "INT", name: "a_testfeature1"},
            {type: "INT", name: "a_testfeature2"},
            {type: "TIMESTAMP", name: "ts" },
          ]

         features_b = [
            {type: "INT", name: "id", primary: true },
            {type: "INT", name: "b_testfeature1"},
            {type: "INT", name: "b_testfeature2"},
            {type: "TIMESTAMP", name: "ts" },
          ]

          fg_a = create_cached_featuregroup_checked_return_fg(@project.id, featurestore_id,
                                                         "test_fg_a#{featuregroup_suffix}",
                                                     features: features_a,
                                                     event_time: "ts")

          fg_b = create_cached_featuregroup_checked_return_fg(@project.id, featurestore_id,
                                                         "test_fg_b#{featuregroup_suffix}",
                                                     features: features_b,
                                                     event_time: "ts")

          query = {
                      leftFeatureGroup: {
                        id: fg_a[:id],
                        type: fg_a[:type]
                      },
                      leftFeatures: [{name: 'a_testfeature1'}, {name: 'a_testfeature2'}],
                      joins: [{
                                  query: {
                                      leftFeatureGroup: {
                                        id: fg_b[:id],
                                        type: fg_b[:type]
                                      },
                                      leftFeatures: [{name: 'b_testfeature1'}, {name: 'b_testfeature2'}],
                                  }
                              }
                      ],
                      filter: {
                        type: "AND",
                        leftFilter: {
                          feature: {
                            name: "a_testfeature1",
                            featureGroupId: fg_a[:id]
                          },
                          condition: "GREATER_THAN",
                          value: 0
                        },
                        rightFilter: {
                          feature: {
                            name: "ts",
                            featureGroupId: fg_a[:id]
                          },
                          condition: "GREATER_THAN",
                          value: "2022-02-01 00:00:00"
                        }
                      }

                  }

          query_result = put "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/query", query.to_json
          expect_status_details(200)
          parsed_query_result = JSON.parse(query_result)

          expect(parsed_query_result['query']).to eql(
                                                    "SELECT `fg1`.`a_testfeature1` `a_testfeature1`, `fg1`.`a_testfeature2` `a_testfeature2`, `fg0`.`b_testfeature1` `b_testfeature1`, `fg0`.`b_testfeature2` `b_testfeature2`\n" +
                                                      "FROM `#{featurestore_name}`.`test_fg_a#{featuregroup_suffix}_1` `fg1`\n" +
                                                      "INNER JOIN `#{featurestore_name}`.`test_fg_b#{featuregroup_suffix}_1` `fg0` ON `fg1`.`id` = `fg0`.`id`\n" +
                                                      "WHERE `fg1`.`a_testfeature1` > #{query[:filter][:leftFilter][:value]} AND `fg1`.`ts` > TIMESTAMP '#{query[:filter][:rightFilter][:value]}.000'"
                                                  )

          expect(parsed_query_result['queryOnline']).to eql(
                                                    "SELECT `fg1`.`a_testfeature1` `a_testfeature1`, `fg1`.`a_testfeature2` `a_testfeature2`, `fg0`.`b_testfeature1` `b_testfeature1`, `fg0`.`b_testfeature2` `b_testfeature2`\n" +
                                                      "FROM `#{project_name.downcase}`.`test_fg_a#{featuregroup_suffix}_1` `fg1`\n" +
                                                      "INNER JOIN `#{project_name.downcase}`.`test_fg_b#{featuregroup_suffix}_1` `fg0` ON `fg1`.`id` = `fg0`.`id`\n" +
                                                      "WHERE `fg1`.`a_testfeature1` > #{query[:filter][:leftFilter][:value]} AND `fg1`.`ts` > TIMESTAMP '#{query[:filter][:rightFilter][:value]}.000'"
                                                        )
        end



        it "should be able to create sql string from query with or without helper columns" do
          project_name = @project.projectname.downcase
          featurestore_id = get_featurestore_id(@project.id)
          featurestore_name = get_featurestore_name(@project.id)
          featuregroup_suffix = short_random_id

          features_1 = [
                        {"name": "ts", "type": "TIMESTAMP"},
                        {"name": "pk", "type": "INT", "primary": true},
                        {"name": "a", "type": "INT"},
                        {"name": "b", "type": "INT"},
                        {"name": "c", "type": "INT"}
                        ]

          features_2 = [
                        {"name": "ts", "type": "TIMESTAMP"},
                        {"name": "pk", "type": "INT", "primary": true},
                        {"name": "d", "type": "INT"},
                        {"name": "e", "type": "INT"},
                        {"name": "f", "type": "INT"}
                        ]

          features_3 = [
                        {"name": "ts", "type": "TIMESTAMP"},
                        {"name": "pk", "type": "INT", "primary": true},
                        {"name": "g", "type": "INT"},
                        {"name": "h", "type": "INT"},
                        {"name": "i", "type": "INT"}
                        ]

          fg1 = create_cached_featuregroup_checked_return_fg(@project.id, featurestore_id,
                                                         "test_fg_1#{featuregroup_suffix}",
                                                          features: features_1,
                                                          event_time: "ts")

          fg2 = create_cached_featuregroup_checked_return_fg(@project.id, featurestore_id,
                                                         "test_fg_2#{featuregroup_suffix}",
                                                          features: features_2,
                                                          event_time: "ts")
          fg3 = create_cached_featuregroup_checked_return_fg(@project.id, featurestore_id,
                                                         "test_fg_3#{featuregroup_suffix}",
                                                          features: features_3,
                                                          event_time: "ts")

          json_data = {
                      name: "feature_view_#{random_id}",
                      version: 1,
                      description: "testfeatureviewdescription",
                      query: {
                        leftFeatureGroup: {id: fg1[:id], type: fg1[:type]},
                        leftFeatures: [{name: "a"}, {name: "b"}, {name: "c"}],
                        joins: [
                          {
                            query: {
                              leftFeatureGroup: {id: fg2[:id], type: fg2[:type]},
                              leftFeatures: [{name: "d"}, {name: "e"}, {name: "f"}],
                              joins: [],
                              filter: nil,
                            },
                            on: [],
                            leftOn: [{name: "pk"}],
                            rightOn: [{name: "pk"}],
                            type: "INNER",
                            prefix: nil
                          },
                          {
                            query: {
                              leftFeatureGroup: {id: fg3[:id], type: fg3[:type]},
                              leftFeatures: [{name: "g"}, {name: "h"}, {name: "i"}],
                              joins: [],
                              filter: nil,
                            },
                            on: [],
                            leftOn: [{name: "pk"}],
                            rightOn: [{name: "pk"}],
                            type: "INNER",
                            prefix: nil
                          }
                        ],
                        filter: nil,
                      },
                      features: [
                                {
                                  name: "a",
                                  type: "INT",
                                  index: 0,
                                  label: true,
                                  inferenceHelperColumn: false,
                                  trainingHelperColumn: false,
                                  transformationFunction: nil,
                                  featureGroupFeatureName: nil,
                                  featuregroup: fg1
                                },
                                {
                                  name: "b",
                                  type: "INT",
                                  index: 1,
                                  label: false,
                                  inferenceHelperColumn: false,
                                  trainingHelperColumn: false,
                                  transformationFunction: nil,
                                  featureGroupFeatureName: nil,
                                  featuregroup: fg1
                                },
                                {
                                  name: "c",
                                  index: 2,
                                  label: false,
                                  inferenceHelperColumn: false,
                                  trainingHelperColumn: false,
                                  transformationFunction: nil,
                                  featureGroupFeatureName: nil,
                                  featuregroup: fg1
                                },
                                {
                                  name: "d",
                                  index: 3,
                                  label: false,
                                  inferenceHelperColumn: true,
                                  trainingHelperColumn: false,
                                  transformationFunction: nil,
                                  featureGroupFeatureName: nil,
                                  featuregroup: fg2
                                },
                                {
                                  name: "e",
                                  index: 4,
                                  label: false,
                                  inferenceHelperColumn: false,
                                  trainingHelperColumn: false,
                                  transformationFunction: nil,
                                  featureGroupFeatureName: nil,
                                  featuregroup: fg2

                                },
                                {
                                  name: "f",
                                  index: 5,
                                  label: false,
                                  inferenceHelperColumn: false,
                                  trainingHelperColumn: false,
                                  transformationFunction: nil,
                                  featureGroupFeatureName: nil,
                                  featuregroup: fg2

                                },
                                {
                                  name: "g",
                                  index: 6,
                                  label: false,
                                  inferenceHelperColumn: false,
                                  trainingHelperColumn: true,
                                  transformationFunction: nil,
                                  featureGroupFeatureName: nil,
                                  featuregroup: fg3
                                },
                                {
                                  name: "h",
                                  index: 7,
                                  label: false,
                                  inferenceHelperColumn: false,
                                  trainingHelperColumn: false,
                                  transformationFunction: nil,
                                  featureGroupFeatureName: nil,
                                  featuregroup: fg3
                                },
                                {
                                  name: "i",
                                  index: 8,
                                  label: false,
                                  inferenceHelperColumn: false,
                                  trainingHelperColumn: false,
                                  transformationFunction: nil,
                                  featureGroupFeatureName: nil,
                                  featuregroup: fg3
                                }
                                ],
                      type: "featureViewDTO"
                    }

          json_result = create_feature_view_with_json(@project.id, featurestore_id, json_data)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]

          # without helper columns
          fs_query = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/query/batch?start_time=1234&end_time=4321"
          fs_query_result = put "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/query", JSON.parse(fs_query)
          parsed_query_result = JSON.parse(fs_query_result)
          expect(parsed_query_result['query']).to eql("SELECT `fg2`.`b` `b`, `fg2`.`c` `c`, `fg0`.`e` `e`, `fg0`.`f` `f`, `fg1`.`h` `h`, `fg1`.`i` `i`\nFROM `#{featurestore_name}`.`test_fg_1#{featuregroup_suffix}_1` `fg2`\nINNER JOIN `#{featurestore_name}`.`test_fg_2#{featuregroup_suffix}_1` `fg0` ON `fg2`.`pk` = `fg0`.`pk`\nINNER JOIN `#{featurestore_name}`.`test_fg_3#{featuregroup_suffix}_1` `fg1` ON `fg2`.`pk` = `fg1`.`pk`\nWHERE `fg2`.`ts` >= TIMESTAMP '1970-01-01 00:00:01.000' AND `fg2`.`ts` < TIMESTAMP '1970-01-01 00:00:04.000'")
          expect(parsed_query_result['queryOnline']).to eql("SELECT `fg2`.`b` `b`, `fg2`.`c` `c`, `fg0`.`e` `e`, `fg0`.`f` `f`, `fg1`.`h` `h`, `fg1`.`i` `i`\nFROM `#{project_name.downcase}`.`test_fg_1#{featuregroup_suffix}_1` `fg2`\nINNER JOIN `#{project_name.downcase}`.`test_fg_2#{featuregroup_suffix}_1` `fg0` ON `fg2`.`pk` = `fg0`.`pk`\nINNER JOIN `#{project_name.downcase}`.`test_fg_3#{featuregroup_suffix}_1` `fg1` ON `fg2`.`pk` = `fg1`.`pk`\nWHERE `fg2`.`ts` >= TIMESTAMP '1970-01-01 00:00:01.000' AND `fg2`.`ts` < TIMESTAMP '1970-01-01 00:00:04.000'")
          expect_status_details(200)

          # with helper columns
          fs_query = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/query/batch?start_time=1234&end_time=4321&with_primary_keys=true&with_event_time=true&inference_helper_columns&inference_helper_columns=true&training_helper_columns=true"
          fs_query_result = put "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/query", JSON.parse(fs_query)
          parsed_query_result = JSON.parse(fs_query_result)
          expect(parsed_query_result['query']).to eql("SELECT `fg2`.`b` `b`, `fg2`.`c` `c`, `fg2`.`ts` `ts`, `fg2`.`pk` `pk`, `fg0`.`e` `e`, `fg0`.`f` `f`, `fg1`.`g` `g`, `fg1`.`h` `h`, `fg1`.`i` `i`\nFROM `#{featurestore_name}`.`test_fg_1#{featuregroup_suffix}_1` `fg2`\nINNER JOIN `#{featurestore_name}`.`test_fg_2#{featuregroup_suffix}_1` `fg0` ON `fg2`.`pk` = `fg0`.`pk`\nINNER JOIN `#{featurestore_name}`.`test_fg_3#{featuregroup_suffix}_1` `fg1` ON `fg2`.`pk` = `fg1`.`pk`\nWHERE `fg2`.`ts` >= TIMESTAMP '1970-01-01 00:00:01.000' AND `fg2`.`ts` < TIMESTAMP '1970-01-01 00:00:04.000'")
          expect(parsed_query_result['queryOnline']).to eql("SELECT `fg2`.`b` `b`, `fg2`.`c` `c`, `fg2`.`ts` `ts`, `fg2`.`pk` `pk`, `fg0`.`e` `e`, `fg0`.`f` `f`, `fg1`.`g` `g`, `fg1`.`h` `h`, `fg1`.`i` `i`\nFROM `#{project_name.downcase}`.`test_fg_1#{featuregroup_suffix}_1` `fg2`\nINNER JOIN `#{project_name.downcase}`.`test_fg_2#{featuregroup_suffix}_1` `fg0` ON `fg2`.`pk` = `fg0`.`pk`\nINNER JOIN `#{project_name.downcase}`.`test_fg_3#{featuregroup_suffix}_1` `fg1` ON `fg2`.`pk` = `fg1`.`pk`\nWHERE `fg2`.`ts` >= TIMESTAMP '1970-01-01 00:00:01.000' AND `fg2`.`ts` < TIMESTAMP '1970-01-01 00:00:04.000'")
          expect_status_details(200)
        end
      end
    end
  end
end
