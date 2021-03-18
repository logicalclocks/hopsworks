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
#

require 'uri'

describe "On #{ENV['OS']}" do

  after(:all) {clean_all_test_projects(spec: "featurestore")}
  describe 'featurestore validation' do

    describe "Get feature store validation rule definitions" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to get all rule definitions" do
          get_rule_definitions
          expect_status(200)
          expect(ValidationRule.all.length).to be == json_body[:count]
        end

        it "should get a rule definition by name" do
          get_rule_definition_by_name("HAS_MIN")
          expect_status(200)
          expect(json_body[:name]).to eq("HAS_MIN")
          expect(URI(json_body[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/rules/HAS_MIN"
        end

        it "should return limit=x rules" do
          get_rule_definitions
          rules = json_body[:items].map {|rule| rule[:name]}
          sorted = rules.sort
          get_rule_definitions("?limit=2&sort_by=name:asc")
          sorted_res = json_body[:items].map {|rule| rule[:name]}
          expect(sorted_res).to eq(sorted.take(2))
        end
        it "should return expectations starting from offset=y" do
          get_rule_definitions
          rules = json_body[:items].map {|rule| rule[:name]}
          sorted = rules.sort
          get_rule_definitions("?offset=1&sort_by=name:asc")
          sorted_res = json_body[:items].map {|rule| rule[:name]}
          expect(sorted_res).to eq(sorted.drop(1))
        end
        it "should return limit=x expectations with offset=y" do
          get_rule_definitions
          rules = json_body[:items].map {|rule| rule[:name]}
          sorted = rules.sort
          get_rule_definitions("?offset=1&limit=2&&sort_by=name:asc")
          sorted_res = json_body[:items].map {|rule| rule[:name]}
          expect(sorted_res).to eq(sorted.drop(1).take(2))
        end
        it "should ignore if limit < 0" do
          get_rule_definitions
          rules = json_body[:items].map {|rule| rule[:name]}
          sorted = rules.sort
          get_rule_definitions("?limit<0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|rule| rule[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if offset < 0" do
          get_rule_definitions
          rules = json_body[:items].map {|rule| rule[:name]}
          sorted = rules.sort
          get_rule_definitions("?offset<0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|rule| rule[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if limit = 0" do
          get_rule_definitions
          rules = json_body[:items].map {|rule| rule[:name]}
          sorted = rules.sort
          get_rule_definitions("?limit=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|rule| rule[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if offset = 0" do
          get_rule_definitions
          rules = json_body[:items].map {|rule| rule[:name]}
          sorted = rules.sort
          get_rule_definitions("?offset=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|rule| rule[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if limit = 0 and offset = 0" do
          get_rule_definitions
          rules = json_body[:items].map {|rule| rule[:name]}
          sorted = rules.sort
          get_rule_definitions("?limit=0&offset=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|rule| rule[:name]}
          expect(sorted_res).to eq(sorted)
        end
      end
    end

    describe "Expectations" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          create_expectation(project.id, featurestore_id, "exp0")
          create_expectation(project.id, featurestore_id, "exp1")
          create_expectation(project.id, featurestore_id, "exp2")
        end
        it "should be able to get all expectations" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id)
          expect(json_body[:count]).to be == 3
          expect_status(200)
        end
        it "should be able to create an expectation" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          create_expectation(project.id, featurestore_id, "exp#{short_random_id}")
          expect_status(200)
        end
        it "should be able to get an expectation by name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id, json_data_expectation[:name])
          expect_status(200)
          expect(json_body[:name]).to be == json_data_expectation[:name]
          expect(URI(json_body[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/expectations/#{json_data_expectation[:name]}"
        end
        it "should be able to delete an expectation" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          exp_name = "exp#{short_random_id}"
          create_expectation(project.id, featurestore_id, exp_name)
          get_feature_store_expectations(project.id, featurestore_id)
          num_of_expectations = json_body[:count]
          delete_expectation(project.id, featurestore_id, exp_name)
          expect_status(204)
          get_feature_store_expectations(project.id, featurestore_id)
          expect(json_body[:count]).to be == (num_of_expectations-1)
        end
        it "should be able to attach an expectation to a featuregroup" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          attach_expectation(project.id, featurestore_id, fg_json["id"], json_data_expectation[:name])
          expect_status(200)
        end
        it "should be able to get an expectation that is attached to featuregroup" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          attach_expectation(project.id, featurestore_id, fg_json["id"], json_data_expectation[:name])
          expect_status(200)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], json_data_expectation[:name])
          expect_status(200)
          expect(json_body[:name]).to be == json_data_expectation[:name]
          expect(URI(json_body[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/#{fg_json["id"]}/expectations/#{json_data_expectation[:name]}"
        end
        it "should be able to get all expectations attached to featuregroup" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          attach_expectation(project.id, featurestore_id, fg_json["id"], json_data_expectation[:name])
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"])
          expect(json_body[:count]).to be == 1
          expect_status(200)
        end
        it "should be able to attach multiple expectation rules to the same featuregroup" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          attach_expectation(project.id, featurestore_id, fg_json["id"], json_data_expectation[:name])
          expect_status(200)
          attach_expectation(project.id, featurestore_id, fg_json["id"], "exp1")
          expect_status(200)
          attach_expectation(project.id, featurestore_id, fg_json["id"], "exp2")
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"])
          expect_status(200)
          expect(json_body[:count]).to be == 3
        end
        it "should be able to get feature groups filtered by expectations attached" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          create_expectation(project.id, featurestore_id, "expA")
          create_expectation(project.id, featurestore_id, "expB")
          json_result, fg1 = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          attach_expectation(project.id, featurestore_id, fg_json["id"], "expA")
          attach_expectation(project.id, featurestore_id, fg_json["id"], "expB")

          json_result, fg2 = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          attach_expectation(project.id, featurestore_id, fg_json["id"], "expA")

          # Filter by exp1, should return two FGs
          get_featuregroups(project.id, featurestore_id, query="filter_by=expectations:expA")
          expect(json_body.size).to be == 2

          # Filter by exp2, should return one FGs
          get_featuregroups(project.id, featurestore_id, query="filter_by=expectations:expB")
          expect(json_body.size).to be == 1

          # Filter by exp1 and exp2. should return two FGs
          get_featuregroups(project.id, featurestore_id, query="filter_by=expectations:expA&filter_by=expectations:expB")
          expect(json_body.size).to be == 2
        end
        it "should be able to detach an expectation from a featuregroup" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          attach_expectation(project.id, featurestore_id, fg_json["id"], json_data_expectation[:name])
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"])
          num_of_attached_expectations = json_body[:count]
          detach_expectation(project.id, featurestore_id, fg_json["id"], json_data_expectation[:name])
          expect_status(204)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"])
          expect(json_body[:count]).to be == (num_of_attached_expectations - 1)
        end
        it "should not be able to attach an expectation to non-existent featuregroup" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          attach_expectation(project.id, featurestore_id, 99999, json_data_expectation[:name])
          expect_status(404)
        end
        it "should not be able to attach an expectation to non-existent featuregroup feature" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          exp_name = "exp#{short_random_id}"
          create_expectation(project.id, featurestore_id, exp_name, "thisdoesnotexist")
          attach_expectation(project.id, featurestore_id, 99999, exp_name)
          expect_status(404)
        end
        it "attaching the same expectation should be idempotent" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          attach_expectation(project.id, featurestore_id, fg_json["id"], json_data_expectation[:name])
          attach_expectation(project.id, featurestore_id, fg_json["id"], json_data_expectation[:name])
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"])
          expect(json_body[:count]).to be == 1
        end
      end
    end

    describe "Expectations" do
      context 'with valid project, featurestore service enabled, a feature group and expectations attached' do
        fg_json = {}
        before :all do
          with_valid_project
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          create_expectation(project.id, featurestore_id, "exp0")
          create_expectation(project.id, featurestore_id, "exp1")
          create_expectation(project.id, featurestore_id, "exp2")
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          attach_expectation(project.id, featurestore_id, fg_json["id"], json_data_expectation[:name])
          attach_expectation(project.id, featurestore_id, fg_json["id"], "exp1")
          attach_expectation(project.id, featurestore_id, fg_json["id"], "exp2")

        end
        it "should be able to get only the features field of an expectation" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id,  json_data_expectation[:name], "?fields=features")
          expect(json_body[:description]).to be nil
          expect(json_body[:features]).to be == json_data_expectation[:features]
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], name=json_data_expectation[:name], query="?fields=features")
          expect(json_body[:description]).to be nil
          expect(json_body[:features]).to be == json_data_expectation[:features]
        end
        it "should be able to get only the rules field of an expectation" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id, json_data_expectation[:name], "?fields=rules")
          expect(json_body[:description]).to be nil
          expect(json_body[:rules]).to be == json_data_expectation[:rules]
          expect_status(200)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], name=json_data_expectation[:name], query="?fields=rules")
          expect(json_body[:description]).to be nil
          expect(json_body[:rules]).to be == json_data_expectation[:rules]
          expect_status(200)
        end
        it "should be able to get only the description field of an expectation" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id,  json_data_expectation[:name], "?fields=description")
          expect(json_body[:name]).to be nil
          expect(json_body[:description]).to be == json_data_expectation[:description]
          expect_status(200)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], name=json_data_expectation[:name], query="?fields=description")
          expect(json_body[:name]).to be nil
          expect(json_body[:description]).to be == json_data_expectation[:description]
          expect_status(200)
        end
        it "should be able to get only the name field of an expectation" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id, json_data_expectation[:name], "?fields=name")
          expect(json_body[:description]).to be nil
          expect(json_body[:name]).to be ==  json_data_expectation[:name]
          expect_status(200)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], name=json_data_expectation[:name], query="?fields=name")
          expect(json_body[:description]).to be nil
          expect(json_body[:name]).to be ==  json_data_expectation[:name]
          expect_status(200)
        end
        it "should be able to get only the name and description field of an expectation" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id, json_data_expectation[:name], "?fields=name,description")
          expect(json_body[:description]).to be ==  json_data_expectation[:description]
          expect(json_body[:name]).to be ==  json_data_expectation[:name]
          expect(json_body[:rules]).to be nil
          expect_status(200)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], name=json_data_expectation[:name], query="?fields=name,description")
          expect(json_body[:description]).to be ==  json_data_expectation[:description]
          expect(json_body[:name]).to be ==  json_data_expectation[:name]
          expect(json_body[:rules]).to be nil
          expect_status(200)
        end
        it "should be able to get only the features field of all expectations" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id, "?fields=features")
          expect(json_body[:description]).to be nil
          expect(json_body[:items][0][:features]).to be ==  json_data_expectation[:features]
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], query="?fields=features")
          expect(json_body[:description]).to be nil
          expect(json_body[:items][0][:features]).to be ==  json_data_expectation[:features]
        end
        it "should be able to get only the rules field of all expectations" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id, "?fields=rules")
          expect(json_body[:items][0][:rules]).to be ==  json_data_expectation[:rules]
          expect_status(200)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], query="?fields=rules")
          expect(json_body[:items][0][:rules]).to be ==  json_data_expectation[:rules]
          expect_status(200)
        end
        it "should be able to get only the description field of all expectations" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id, "?fields=description")
          expect(json_body[:items][0][:description]).to be == json_data_expectation[:description]
          expect_status(200)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], query="?fields=description")
          expect(json_body[:items][0][:description]).to be == json_data_expectation[:description]
          expect_status(200)
        end
        it "should be able to get only the name field of all expectations" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id, "?fields=name")
          expect(json_body[:items][0][:description]).to be nil
          expect(json_body[:items][0][:name]).to start_with("exp")
          expect_status(200)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], query="?fields=name")
          expect(json_body[:items][0][:description]).to be nil
          expect(json_body[:items][0][:name]).to start_with("exp")
          expect_status(200)
        end
        it "should be able to get the name and description fields of all expectations" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id, "?fields=description,name")
          expect(json_body[:items][0][:description]).to be ==  json_data_expectation[:description]
          expect(json_body[:items][0][:name]).to start_with("exp")
          expect_status(200)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], query="?fields=description,name")
          expect(json_body[:items][0][:description]).to be ==  json_data_expectation[:description]
          expect(json_body[:items][0][:name]).to start_with("exp")
          expect_status(200)
        end
        it "should be able to get all expectations sorted by name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id)
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_store_expectations(project.id, featurestore_id, "?sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"])
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], "?sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should be able to get all expectations sorted by name descending" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id)
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase).reverse
          get_feature_store_expectations(project.id, featurestore_id, "?sort_by=name:desc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"])
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase).reverse
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], "?sort_by=name:desc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should return limit=x expectations" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id)
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_store_expectations(project.id, featurestore_id, "?limit=2&sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted.take(2))
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"])
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], "?limit=2&sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted.take(2))
        end
        it "should return expectations starting from offset=y" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id)
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_store_expectations(project.id, featurestore_id, "?offset=1&sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted.drop(1))
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"])
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], "?offset=1&sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted.drop(1))
        end
        it "should return limit=x expectations with offset=y" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id)
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_store_expectations(project.id, featurestore_id, "?offset=1&limit=2&sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted.drop(1).take(2))
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"])
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], "?offset=1&limit=2&sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted.drop(1).take(2))
        end
        it "should ignore if limit < 0" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id)
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_store_expectations(project.id, featurestore_id, "?limit<0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"])
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], "?limit<0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if offset < 0" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id)
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_store_expectations(project.id, featurestore_id, "?offset<0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"])
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], "?offset<0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if limit = 0" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id)
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_store_expectations(project.id, featurestore_id, "?limit=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"])
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], "?limit=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if offset = 0" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id)
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_store_expectations(project.id, featurestore_id, "?offset=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"])
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], "?offset=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if limit = 0 and offset = 0" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id)
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_store_expectations(project.id, featurestore_id, "?limit=0&offset=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"])
          expectations = json_body[:items].map {|expectation| expectation[:name]}
          sorted = expectations.sort_by(&:downcase)
          get_feature_group_expectations(project.id, featurestore_id, fg_json["id"], "?limit=0&offset=0&sort_by=name:asc")
          sorted_res = json_body[:items].map {|expectation| expectation[:name]}
          expect(sorted_res).to eq(sorted)
        end
      end
    end
    describe "Validations" do
      context 'with valid project, featurestore service enabled and expectations' do
        before :all do
          with_valid_project
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          create_expectation(project.id, featurestore_id, "exp0")
          create_expectation(project.id, featurestore_id, "exp1")
          create_expectation(project.id, featurestore_id, "exp2")
        end
        it "should be able to create a validation" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          create_validation(project.id, featurestore_id, fg_json["id"])
          expect_status(200)
        end
        it "should be able to get all validations of a feature group" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          create_validation(project.id, featurestore_id, fg_json["id"])
          create_validation(project.id, featurestore_id, fg_json["id"], "1612285461348")
          get_validations(project.id, featurestore_id, fg_json["id"])
          expect(json_body[:count]).to be == 2
        end
        it "should be able to get a validation by id" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          create_validation(project.id, featurestore_id, fg_json["id"])
          validation_id = json_body[:validationId]
          get_validations(project.id, featurestore_id, fg_json["id"], validation_id)
          expect(json_body[:validationId]).to be == validation_id
          expect(URI(json_body[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/#{fg_json["id"]}/validations/#{validation_id}"
        end
      end
      context 'with valid project, featurestore service enabled, expectations and validations' do
        fg_json = {}
        before :all do
          with_valid_project
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          create_expectation(project.id, featurestore_id, "exp0")
          create_expectation(project.id, featurestore_id, "exp1")
          create_expectation(project.id, featurestore_id, "exp2")
          json_result, featuregroup_name = create_cached_featuregroup_with_partition(project.id, featurestore_id, time_travel_format: "HUDI")
          fg_json = JSON.parse(json_result)
          commit_metadata = {commitDateString:20201024221125,rowsInserted:4,rowsUpdated:0,rowsDeleted:0}
          commit_cached_featuregroup(project.id, featurestore_id, fg_json["id"], commit_metadata: commit_metadata)
          create_validation(project.id, featurestore_id, fg_json["id"])
          create_validation(project.id, featurestore_id, fg_json["id"], "1612285461348")
        end
        it "should be able to get all validations of a feature group sorted by validation id ascending" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"])
          validations = json_body[:items].map {|validation| validation[:validationId]}
          sorted = validations.sort
          get_validations(project.id, featurestore_id, fg_json["id"], "?sort_by=id:asc")
          sorted_res = json_body[:items].map {|validation| validation[:validationId]}
          expect(sorted_res).to eq(sorted)
        end
        it "should be able to get all validations of a feature group sorted by validation id descending" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"])
          validations = json_body[:items].map {|validation| validation[:validationId]}
          sorted = validations.sort.reverse
          get_validations(project.id, featurestore_id, fg_json["id"],"?sort_by=id:desc")
          sorted_res = json_body[:items].map {|validation| validation[:validationId]}
          expect(sorted_res).to eq(sorted)
        end
        it "should be able to get all validations of a feature group sorted by validation time ascending" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"])
          validations = json_body[:items].map {|validation| validation[:validationTime]}
          sorted = validations.sort
          get_validations(project.id, featurestore_id, fg_json["id"],"?sort_by=validation_time:asc")
          sorted_res = json_body[:items].map {|validation| validation[:validationTime]}
          expect(sorted_res).to eq(sorted)
        end
        it "should be able to get all validations of a feature group sorted by validation time descending" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"])
          validations = json_body[:items].map {|validation| validation[:validationTime]}
          sorted = validations.sort.reverse
          get_validations(project.id, featurestore_id, fg_json["id"], "?sort_by=validation_time:desc")
          sorted_res = json_body[:items].map {|validation| validation[:validationTime]}
          expect(sorted_res).to eq(sorted)
        end
        it "should be able to get all validations of a feature group sorted by commit time ascending" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"])
          validations = json_body[:items].map {|validation| validation[:commitTime]}
          sorted = validations.sort
          get_validations(project.id, featurestore_id, fg_json["id"], "?sort_by=commit_time:asc")
          sorted_res = json_body[:items].map {|validation| validation[:commitTime]}
          expect(sorted_res).to eq(sorted)
        end
        it "should be able to get all validations of a feature group sorted by commit time descending" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"])
          validations = json_body[:items].map {|validation| validation[:commitTime]}
          sorted = validations.sort.reverse
          get_validations(project.id, featurestore_id, fg_json["id"], "?sort_by=commit_time:desc")
          sorted_res = json_body[:items].map {|validation| validation[:commitTime]}
          expect(sorted_res).to eq(sorted)
        end
        it "should return limit=x validations" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"])
          validations = json_body[:items].map {|validation| validation[:validationTime]}
          sorted = validations.sort
          get_validations(project.id, featurestore_id, fg_json["id"], "?limit=2&sort_by=validation_time:asc")
          sorted_res = json_body[:items].map {|validation| validation[:validationTime]}
          expect(sorted_res).to eq(sorted.take(2))
        end
        it "should return validations starting from offset=y" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"])
          validations = json_body[:items].map {|validation| validation[:validationTime]}
          sorted = validations.sort
          get_validations(project.id, featurestore_id, fg_json["id"], "?offset=1&sort_by=validation_time:asc")
          sorted_res = json_body[:items].map {|validation| validation[:validationTime]}
          expect(sorted_res).to eq(sorted.drop(1))
        end
        it "should return limit=x validations with offset=y" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"])
          validations = json_body[:items].map {|validation| validation[:validationTime]}
          sorted = validations.sort
          get_validations(project.id, featurestore_id, fg_json["id"], "?offset=1&limit=2&sort_by=validation_time:asc")
          sorted_res = json_body[:items].map {|validation| validation[:validationTime]}
          expect(sorted_res).to eq(sorted.drop(1).take(2))
        end
        it "should ignore if limit < 0" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"])
          validations = json_body[:items].map {|validation| validation[:validationTime]}
          sorted = validations.sort
          get_validations(project.id, featurestore_id, fg_json["id"], "?limit<0&sort_by=validation_time:asc")
          sorted_res = json_body[:items].map {|validation| validation[:validationTime]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if offset < 0" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"])
          validations = json_body[:items].map {|validation| validation[:validationTime]}
          sorted = validations.sort
          get_validations(project.id, featurestore_id, fg_json["id"], "?offset<0&sort_by=validation_time:asc")
          sorted_res = json_body[:items].map {|validation| validation[:validationTime]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if limit = 0" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"])
          validations = json_body[:items].map {|validation| validation[:validationTime]}
          sorted = validations.sort
          get_validations(project.id, featurestore_id, fg_json["id"], "?limit=0&sort_by=validation_time:asc")
          sorted_res = json_body[:items].map {|validation| validation[:validationTime]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if offset = 0" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"])
          validations = json_body[:items].map {|validation| validation[:validationTime]}
          sorted = validations.sort
          get_validations(project.id, featurestore_id, fg_json["id"], "?offset=0&sort_by=validation_time:asc")
          sorted_res = json_body[:items].map {|validation| validation[:validationTime]}
          expect(sorted_res).to eq(sorted)
        end
        it "should ignore if limit = 0 and offset = 0" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"])
          validations = json_body[:items].map {|validation| validation[:validationTime]}
          sorted = validations.sort
          get_validations(project.id, featurestore_id, fg_json["id"], "?limit=0&offset=0&sort_by=validation_time:asc")
          sorted_res = json_body[:items].map {|validation| validation[:validationTime]}
          expect(sorted_res).to eq(sorted)
        end
        it "should return validations filtered by validation time eq" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"], "?filter_by=validation_time_eq:#{json_data_validation[:validationTime]}")
          expect_status(200)
          expect(json_body[:items].count).to eq 1
        end
        it "should return validations filtered by validation time gt" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"], "?filter_by=validation_time_gt:#{json_data_validation[:validationTime]}")
          expect_status(200)
          expect(json_body[:items].count).to eq 1
        end
        it "should return validations filtered by validation time lt" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"], "?filter_by=validation_time_lt:#{json_data_validation[:validationTime]}")
          expect_status(200)
          expect(json_body[:count]).to eq 0
        end
        # it "should return validations filtered by commit time eq" do
        #   project = get_project
        #   featurestore_id = get_featurestore_id(project.id)
        #   get_validations(project.id, featurestore_id, fg_json["id"], "?filter_by=commit_time_eq:#{json_data_validation[:commitTime]}")
        #   expect_status(200)
        #   expect(json_body[:items].count).to eq 1
        # end
        # it "should return validations filtered by commit time gt" do
        #   project = get_project
        #   featurestore_id = get_featurestore_id(project.id)
        #   get_validations(project.id, featurestore_id, fg_json["id"], "?filter_by=commit_time_gt:#{json_data_validation[:commitTime]}")
        #   expect_status(200)
        #   expect(json_body[:items].count).to eq 1
        # end
        # it "should return validations filtered by commit time lt" do
        #   project = get_project
        #   featurestore_id = get_featurestore_id(project.id)
        #   get_validations(project.id, featurestore_id, fg_json["id"], "?filter_by=commit_time_lt:#{json_data_validation[:commitTime]}")
        #   expect_status(200)
        #   expect(json_body[:items].count).to eq 0
        # end
        it "should return validations filtered by status" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_validations(project.id, featurestore_id, fg_json["id"], "?filter_by=status_eq:SUCCESS")
          expect_status(200)
          expect(json_body[:items].count).to eq 2
        end
      end
    end
  end
end