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
require 'date'

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
          create_expectation(project.id, featurestore_id, json_data_expectation_categorical[:name], feature="testfeature", custom_expectation=json_data_expectation_categorical)
        end
        it "should be able to get all expectations" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_feature_store_expectations(project.id, featurestore_id)
          expect(json_body[:count]).to be == 4
          expect_status(200)
        end
        it "should be able to create an expectation" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          create_expectation(project.id, featurestore_id, "exp#{short_random_id}")
          expect_status(200)
        end
        it "should fail to create an expectation with missing predicate" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          # First test the HAS_PATTERN rule
          create_expectation(project.id, featurestore_id, json_data_expectation_categorical_invalid[:name], feature="testfeature", custom_expectation=json_data_expectation_categorical_invalid)
          expect_status(400)
          expect_json(errorCode: 270175)

          # Test a compliance rule such as IS_LESS_THAN
          create_expectation(project.id, featurestore_id, json_data_expectation_compliance[:name], feature="testfeature", custom_expectation=json_data_expectation_compliance_invalid)
          expect_status(400)
          expect_json(errorCode: 270175)
        end
        it "should fail to create an expectation with missing min and max" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          create_expectation(project.id, featurestore_id, "exp_predicate_missing", feature="testfeature", custom_expectation=json_data_expectation_invalid)
          expect_status(400)
          expect_json(errorCode: 270175)
        end
        it "should create an expectation with a compliance rule and set default min/max." do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          create_expectation(project.id, featurestore_id, json_data_expectation_compliance[:name], feature="testfeature", custom_expectation=json_data_expectation_compliance)
          expect_status(200)
          get_feature_store_expectations(project.id, featurestore_id, json_data_expectation_compliance[:name])
          expect(json_body[:rules][0][:min]).to be == 1.0
          expect(json_body[:rules][0][:max]).to be == 1.0
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
        it "should fail to attach an expectation to a featuregroup with a feature type mismatch" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          attach_expectation(project.id, featurestore_id, fg_json["id"], json_data_expectation_categorical[:name])
          expect_status(400)
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
        it "should be able to get all validations of a on-demand feature group" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = with_jdbc_connector(project.id)
          json_result, featuregroup_name = create_on_demand_featuregroup(project.id, featurestore_id, connector_id)
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

    describe "Create cached feature group with attached expectation suite" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end
      
        it "should be able to create cached feature group with expectation suite" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_expectation_suite = generate_template_expectation_suite()
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, expectation_suite: json_expectation_suite)
          expect_status_details(201)
	        parsed_json = JSON.parse(json_result)
          expect(parsed_json["expectationSuite"]["expectationSuiteName"]).to eq(json_expectation_suite[:expectationSuiteName])
        end
      end
    end

    describe "CRUD expectation suite" do
      context 'with valid project, featurestore service enabled, a feature group' do
        fg_json = {}
        before :all do
          with_valid_project
        end

        it "should be able to create an expectation suite attached to the feature group" do
	        project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          dto = persist_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          template_suite = generate_template_expectation_suite()
	        expect_status_details(200)
	        dto_parsed = JSON.parse(dto)
	        expect(dto_parsed["expectationSuiteName"]).to eq(template_suite[:expectationSuiteName])
        end

        it "should be able to get an expectation suite attached to a feature group" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          persist_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          json_result = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          expect_status_details(200)
          stored_expectation_suite = JSON.parse(json_result)
          template_expectation_suite = generate_template_expectation_suite()
          expect(stored_expectation_suite["expectationSuiteName"]).to eq(template_expectation_suite[:expectationSuiteName])
          expect(stored_expectation_suite["meta"]).to eq(template_expectation_suite[:meta])
          expect(stored_expectation_suite["geCloudId"]).to eq(template_expectation_suite[:geCloudId])
          expect(stored_expectation_suite["runValidation"]).to eq(template_expectation_suite[:runValidation])
          expect(stored_expectation_suite["validationIngestionPolicy"]).to eq(template_expectation_suite[:validationIngestionPolicy])
          expect(stored_expectation_suite["dataAssetType"]).to eq(template_expectation_suite[:dataAssetType])
          expect(stored_expectation_suite["expectations"][0]["expectationType"]).to eq(template_expectation_suite[:expectations][0][:expectationType])
          expect(stored_expectation_suite["expectations"][0]["kwargs"]).to eq(template_expectation_suite[:expectations][0][:kwargs])
          # Do not compare meta field as it contains an additional expectationId field in principle
	        # expect(stored_expectation_suite["expectations"][0]["meta"]).to eq(template_expectation_suite[:expectations][0][:meta])
        end

        it "should be able to create an expectation suite to replace an existing expectation suite" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          _ = persist_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(200)
          new_expectation_suite = generate_template_expectation_suite()
          new_expectation_suite[:expectationSuiteName] = "new_expectation_suite"
          _ = persist_expectation_suite(project.id, featurestore_id, fg_json["id"], new_expectation_suite)
          expect_status_details(200)
          json_result = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          stored_expectation_suite = JSON.parse(json_result)
          expect(stored_expectation_suite["expectationSuiteName"]).to eq(new_expectation_suite[:expectationSuiteName])
        end

        it "should be able to delete an expectation suite" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          persist_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(200)
          delete_expectation_suite(project.id, featurestore_id, fg_json["id"])
          expect_status_details(204)
          json_result = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          dto_parsed = JSON.parse(json_result)
          expect(dto_parsed["count"]).to eq(0)
        end

        it "expectations should have matching id with expectationId in meta field" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          persist_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          json_result = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          expect_status_details(200)
          stored_expectation_suite = JSON.parse(json_result)
          meta = JSON.parse(stored_expectation_suite["expectations"][0]["meta"])
          expect(stored_expectation_suite["expectations"][0]["id"]).to eq(meta["expectationId"])
        end

        # When writing a fetched modified expectation suite to DB, expectationId in meta field is not changed.
        # It is therefore incorrect as recreating the expectation suite changes all ids. On fetching the modified se  
        # suite incorrect expectationIds should be overwritten by the expectation suite builder.
        it "updated expectation suite should have correct expectationId in meta fields" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          expectation_suite[:expectations][0][:meta] = expectation_suite[:expectations][0][:meta][0..-2] + ", \"expectationsId\":1}"
          persist_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          json_result = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          expect_status_details(200)
          updated_expectation_suite = JSON.parse(json_result)
          meta = JSON.parse(updated_expectation_suite["expectations"][0]["meta"])
          expect(updated_expectation_suite["expectations"][0]["id"]).to eq(meta["expectationId"])
        end
      end
    end

    describe "CRUD validation report" do
      context 'with valid project, featurestore service enabled, a feature group and an expectation suite' do
        fg_json = {}
        before :all do
          with_valid_project
        end

        it "should be able to create a single validation report to attach to featuregroup" do
	        project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          persist_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          template_validation_report = generate_template_validation_report()
          dto = create_validation_report(project.id, featurestore_id, fg_json["id"], template_validation_report)
	        expect_status_details(200)
	        dto_parsed = JSON.parse(dto)
	        expect(dto_parsed["success"]).to eq(template_validation_report[:success])
          expect(dto_parsed["statistics"]).to eq(template_validation_report[:statistics])
          expect(dto_parsed["exceptionInfo"]).to eq(template_validation_report[:exceptionInfo])
          expect(dto_parsed["meta"]).to eq(template_validation_report[:meta])
        end

        it "should be able to get a validation report by id from featuregroup" do
	        project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          persist_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          template_validation_report = generate_template_validation_report()
          created_report = create_validation_report(project.id, featurestore_id, fg_json["id"], template_validation_report)
          expect_status_details(200)
          validation_report_id = JSON.parse(created_report)["id"]
	        dto = get_validation_report_by_id(project.id, featurestore_id, fg_json["id"], validation_report_id)
          dto_parsed = JSON.parse(dto)
	        expect(dto_parsed["success"]).to eq(template_validation_report[:success])
          expect(dto_parsed["statistics"]).to eq(template_validation_report[:statistics])
          expect(dto_parsed["exceptionInfo"]).to eq(template_validation_report[:exceptionInfo])
          expect(dto_parsed["meta"]).to eq(template_validation_report[:meta])
        end

        it "should be able to get latest validation report from featuregroup" do
	        project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          persist_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          template_validation_report = generate_template_validation_report()
          created_report = create_validation_report(project.id, featurestore_id, fg_json["id"], template_validation_report)
          meta_json = JSON.parse(template_validation_report[:meta])
          meta_json["age"] = "latest"
          meta_json["run_id"]["run_time"] = "2022-03-11T14:06:24.481236+00:00"
          template_validation_report[:meta] = JSON.generate(meta_json)
          sleep 3 
          created_report_2 = create_validation_report(project.id, featurestore_id, fg_json["id"], template_validation_report)
          expect_status_details(200)
          latest_report = get_latest_validation_report(project.id, featurestore_id, fg_json["id"])
          latest_json = JSON.parse(latest_report)
          meta_latest = latest_json["items"][0]["meta"]
	        expect(JSON.parse(meta_latest)["age"]).to eq("latest")
        end

        it "should be able to get all validation report from featuregroup ordered by creation date" do
	        project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          persist_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          template_validation_report = generate_template_validation_report()
          created_report = create_validation_report(project.id, featurestore_id, fg_json["id"], template_validation_report)
          meta_json = JSON.parse(template_validation_report[:meta])
          meta_json["age"] = "latest"
          meta_json["run_id"]["run_time"] = "2022-03-11T14:06:24.481236+00:00"
          template_validation_report[:meta] = JSON.generate(meta_json)
          sleep 3
          created_report_2 = create_validation_report(project.id, featurestore_id, fg_json["id"], template_validation_report)
          expect_status_details(200)
          dtos = get_all_validation_report(project.id, featurestore_id, fg_json["id"])
          dtos_parsed = JSON.parse(dtos)
          dto_1 = dtos_parsed["items"][0]
          dto_2 = dtos_parsed["items"][1]
          expect(DateTime.parse(dto_1["validationTime"])).to be > DateTime.parse(dto_2["validationTime"])
        end

        it "should be able to delete a single validation report by id" do
	        project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          persist_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          template_validation_report = generate_template_validation_report()
          dto = create_validation_report(project.id, featurestore_id, fg_json["id"], template_validation_report)
	        expect_status_details(200)
	        dto_parsed = JSON.parse(dto)
          validation_report_id = dto_parsed["id"]
          delete_validation_report(project.id, featurestore_id, fg_json["id"], validation_report_id)
          expect_status_details(204)
        end
      end
    end
    
  end
end
