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
    describe "Create cached feature group with attached expectation suite" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end
      
        it "should be able to create cached feature group with expectation suite" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_expectation_suite = generate_template_expectation_suite
          json_expectation_suite[:expectations][0][:kwargs] = "{\"min_value\": 0, \"max_value\": 1, \"column\":\"testfeature\" }"
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, expectation_suite: json_expectation_suite)
          expect_status_details(201)
	        parsed_json = JSON.parse(json_result)
          expect(parsed_json["expectationSuite"]["expectationSuiteName"]).to eq(json_expectation_suite[:expectationSuiteName])
          fetched_suite = get_expectation_suite(project.id, featurestore_id, parsed_json["id"])
          expect_status_details(200)
          parsed_suite = JSON.parse(fetched_suite)
          expect(parsed_suite["expectationSuiteName"]).to eq(json_expectation_suite[:expectationSuiteName])
          expect(parsed_suite["featureGroupId"]).to eq(parsed_json["id"])
          expect(parsed_suite["featureStoreId"]).to eql(featurestore_id)
        end


        it "should fail to create cached feature group with expectation suite when feature in expectation does not exist" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_expectation_suite = generate_template_expectation_suite
          json_expectation_suite[:expectations][0][:kwargs] = "{\"min_value\": 0, \"max_value\": 1, \"column\":\"notAFeature\" }"
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, expectation_suite: json_expectation_suite)
          expect_status_details(400)
	        error_json = JSON.parse(json_result)
          expected_error_message = "The Feature Name was not found in this version of the Feature Group."
          expect(error_json["errorCode"]).to eq(270210) 
          expect(error_json["errorMsg"]).to eq(expected_error_message)
          expect(error_json["usrMsg"]).to include("testfeature")
          expect(error_json["usrMsg"]).to include("notAFeature")
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
          dto = create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          template_suite = generate_template_expectation_suite()
	        expect_status_details(201)
	        dto_parsed = JSON.parse(dto)
	        expect(dto_parsed["expectationSuiteName"]).to eq(template_suite[:expectationSuiteName])
          expect(dto_parsed["featureGroupId"]).to eq(fg_json["id"])
          expect(dto_parsed["featureStoreId"]).to eql(featurestore_id)
        end

        it "should be able to get an expectation suite attached to a feature group" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
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
          expect(stored_expectation_suite["featureGroupId"]).to eq(fg_json["id"])
          expect(stored_expectation_suite["featureStoreId"]).to eql(featurestore_id)
          # Do not compare meta field as it contains an additional expectationId field in principle
	        # expect(stored_expectation_suite["expectations"][0]["meta"]).to eq(template_expectation_suite[:expectations][0][:meta])
        end

        it "should be able to delete an expectation suite" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          json_suite = create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
          parsed_suite = JSON.parse(json_suite)
          delete_expectation_suite(project.id, featurestore_id, fg_json["id"], parsed_suite["id"])
          expect_status_details(204)
          json_result = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          dto_parsed = JSON.parse(json_result)
          expect(dto_parsed["count"]).to eq(0)
        end

        # This should probably check that the database has been cleaned of expectation, reports and results
        # Especially reports which could have written to disk
        it "should be able to delete and re-create an expectation suite" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          json_suite = create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
          parsed_suite = JSON.parse(json_suite)
          delete_expectation_suite(project.id, featurestore_id, fg_json["id"], parsed_suite["id"])
          expect_status_details(204)
          json_result = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          dto_parsed = JSON.parse(json_result)
          expect(dto_parsed["count"]).to eq(0)
          create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
        end

        # Smart edit tests
        it "should be able to edit the metadata of a suite, e.g validation ingestion policy" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          first_persist_json = create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          parsed_first_persist_suite = JSON.parse(first_persist_json)
          expect_status_details(201)
          edited_name = "edited_name"
          edited_meta = "{\"whomAmI\": \"edited_suite\"}"
          expectation_suite["expectationSuiteName"] = edited_name
          expectation_suite["meta"] = edited_meta
          expectation_suite["validationIngestionPolicy"] = "STRICT"
          expectation_suite["runValidation"] = false
          expectation_suite["id"] = parsed_first_persist_suite["id"]
          update_metadata_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(200)
          json_suite = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          parsed_suite = JSON.parse(json_suite)
          expect(parsed_suite["id"]).to eq(parsed_first_persist_suite["id"])
          expect(parsed_suite["expectationSuiteName"]).to eq(edited_name)
          expect(parsed_suite["meta"]).to eq(edited_meta)
          expect(parsed_suite["validationIngestionPolicy"]).to eq("STRICT")
          expect(parsed_suite["runValidation"]).to eq(false)
          expect(parsed_suite["featureGroupId"]).to eq(fg_json["id"])
          expect(parsed_suite["featureStoreId"]).to eql(featurestore_id)
        end

        # Split the three functions of create, update and delete to avoid over-complicated testing
        it "should be able to smart edit the list of expectation: deletion of an expectation" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          first_persist_json = create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          json_fetched_expectation_suite = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          parsed_fetched_expectation_suite = JSON.parse(json_fetched_expectation_suite)
          parsed_fetched_expectation_suite["expectations"].pop()
          # Edit suite metadata to make sure both are edited on smart update
          edited_name = "edited_name"
          parsed_fetched_expectation_suite["expectationSuiteName"] = edited_name
          update_expectation_suite(project.id, featurestore_id, fg_json["id"], parsed_fetched_expectation_suite)
          expect_status_details(200)
          json_edited_suite = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          parsed_edited_suite = JSON.parse(json_edited_suite)
          expect(parsed_edited_suite["expectations"].length).to eq(0)
          expect(parsed_edited_suite["expectationSuiteName"]).to eq(edited_name)
        end
        
        it "should be able to smart edit the list of expectation: update of an expectation" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          first_persist_json = create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          json_fetched_expectation_suite = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          parsed_fetched_expectation_suite = JSON.parse(json_fetched_expectation_suite)
          to_be_preserved_expectation_id = parsed_fetched_expectation_suite["expectations"][0]["id"]
          new_kwargs = "{\"min_value\":-10, \"max_value\":10}"
          parsed_fetched_expectation_suite["expectations"][0]["kwargs"] = "{\"min_value\":-10, \"max_value\":10}"
          update_expectation_suite(project.id, featurestore_id, fg_json["id"], parsed_fetched_expectation_suite)
          expect_status_details(200)
          json_edited_suite = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          parsed_edited_suite = JSON.parse(json_edited_suite)
          expect(parsed_edited_suite["expectations"][0]["id"]).to eq(to_be_preserved_expectation_id)
          expect(parsed_edited_suite["expectations"][0]["kwargs"]).to eq(new_kwargs)
        end

        it "should be able to smart edit the list of expectation: update of an expectation with expectationId only in meta field" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          first_persist_json = create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          json_fetched_expectation_suite = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          parsed_fetched_expectation_suite = JSON.parse(json_fetched_expectation_suite)
          to_be_preserved_expectation_id = parsed_fetched_expectation_suite["expectations"][0]["id"]
          parsed_fetched_expectation_suite["expectations"][0].delete("id")
          new_kwargs = "{\"min_value\":-10, \"max_value\":10}"
          parsed_fetched_expectation_suite["expectations"][0]["kwargs"] = "{\"min_value\":-10, \"max_value\":10}"
          update_expectation_suite(project.id, featurestore_id, fg_json["id"], parsed_fetched_expectation_suite)
          expect_status_details(200)
          json_edited_suite = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          parsed_edited_suite = JSON.parse(json_edited_suite)
          expect(parsed_edited_suite["expectations"][0]["id"]).to eq(to_be_preserved_expectation_id)
          expect(parsed_edited_suite["expectations"][0]["kwargs"]).to eq(new_kwargs)
        end

        it "should be able to smart edit the list of expectation: append of an expectation" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          first_persist_json = create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          json_fetched_expectation_suite = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          parsed_fetched_expectation_suite = JSON.parse(json_fetched_expectation_suite)
          new_expectation = generate_template_expectation()
          new_expectation["expectationType"] = "expect_column_median_to_be_between"
          parsed_fetched_expectation_suite["expectations"].append(new_expectation)
          update_expectation_suite(project.id, featurestore_id, fg_json["id"], parsed_fetched_expectation_suite)
          expect_status_details(200)
          json_edited_suite = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          parsed_edited_suite = JSON.parse(json_edited_suite)
          expect(parsed_edited_suite["expectations"].length).to eq(2)
        end
      end
    end

    describe "expectation suite others" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "expectations should have matching id with expectationId in meta field" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
          json_result = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          expect_status_details(200)
          stored_expectation_suite = JSON.parse(json_result)
          meta = JSON.parse(stored_expectation_suite["expectations"][0]["meta"])
          expect(stored_expectation_suite["expectations"][0]["id"]).to eq(meta["expectationId"])
        end

        it "should trigger an error to create an expectation suite if one already exists" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
          json_error = create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          parsed_error = JSON.parse(json_error)
          expect(parsed_error.key?("errorCode")).to be true
          expect(parsed_error.key?("errorMsg")).to be true
          expect(parsed_error["errorCode"] == 270208).to be true
        end

        it "should be able to create and launch the validation job for a feature group with expectation suite" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_expectation_suite = generate_template_expectation_suite()
          json_result, _ = create_cached_featuregroup(project.id, featurestore_id, expectation_suite:
            json_expectation_suite)
          expect_status_details(201)
	        parsed_json = JSON.parse(json_result)
          expect(parsed_json["expectationSuite"]["expectationSuiteName"]).to eq(json_expectation_suite[:expectationSuiteName])

          post "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/#{parsed_json["id"]}/expectationsuite/validate"
          expect_status_details(201)

          # should work also a second time
          post "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/#{parsed_json["id"]}/expectationsuite/validate"
          expect_status_details(201)
        end

        it "should not be able to create and launch the validation job for a feature group without expectation suite" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, _ = create_cached_featuregroup(project.id, featurestore_id)
          expect_status_details(201)
          parsed_json = JSON.parse(json_result)

          post "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/#{parsed_json["id"]}/expectationsuite/validate"
          expect_status_details(404)
        end
      end
    end

    describe "CRUD expectation" do
      context 'with valid project, featurestore service enabled, a feature group and an expectation suite' do
        before :all do
          with_valid_project
        end

        it "should be able to append a single expectation to an expectation suite" do 
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          json_suite = create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          parsed_suite = JSON.parse(json_suite)
          expectation = generate_template_expectation()
          create_expectation(project.id, featurestore_id, fg_json["id"], parsed_suite["id"], expectation)
          expect_status_details(201)
          json_edited_suite = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          parsed_edited_suite = JSON.parse(json_edited_suite)
          expect(parsed_edited_suite["expectations"].length).to eq(2)
        end

        it "should be able to get a single expectation from an expectation suite" do 
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          json_suite = create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
          parsed_suite = JSON.parse(json_suite)
          json_expectation = get_expectation(project.id, featurestore_id, fg_json["id"], parsed_suite["id"], parsed_suite["expectations"][0]["id"])
          expect_status_details(200)
        end

        it "should be able to delete a single expectation from an expectation suite" do 
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          json_suite = create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
          parsed_suite = JSON.parse(json_suite)
          json_expectation = delete_expectation(project.id, featurestore_id, fg_json["id"], parsed_suite["id"], parsed_suite["expectations"][0]["id"])
          expect_status_details(204)
          json_empty_suite = get_expectation_suite(project.id, featurestore_id, fg_json["id"])
          parsed_empty_suite = JSON.parse(json_empty_suite)
          expect(parsed_empty_suite["expectations"].length).to eq(0) 
        end

        it "should be able to edit an expectation of an expectation suite via update endpoint" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          json_suite = create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
          parsed_suite = JSON.parse(json_suite)
          expectation = parsed_suite["expectations"][0]
          new_meta = "{\"whoAmI\": \"edited_expectation\" }"
          new_kwargs = "{\"min_value\": 0, \"max_value\": 10 }"
          expectation["meta"] = new_meta
          expectation["kwargs"] = new_kwargs
          json_persisted_expectation = update_expectation(project.id, featurestore_id, fg_json["id"], parsed_suite["id"], expectation["id"], expectation)
          parsed_persisted_expectation = JSON.parse(json_persisted_expectation)
          expect_status_details(200)
          json_fetched_expectation = get_expectation(project.id, featurestore_id, fg_json["id"], parsed_suite["id"], parsed_persisted_expectation["id"])
          parsed_fetched_expectation = JSON.parse(json_fetched_expectation)
          parsed_meta = JSON.parse(parsed_fetched_expectation["meta"])
          expect(parsed_fetched_expectation["kwargs"]).to eq(new_kwargs)
          expect(parsed_meta["whoAmI"]).to eq("edited_expectation")
        end

        it "should be able to edit an expectation of an expectation suite via create endpoint" do
          # In case the creation endpoint is used for an expectation which has id in meta["expectationId"]
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          json_suite = create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
          parsed_suite = JSON.parse(json_suite)
          expectation = parsed_suite["expectations"][0]
          new_meta = "{\"whoAmI\": \"edited_expectation\" }"
          new_kwargs = "{\"min_value\": 0, \"max_value\": 10 }"
          expectation["meta"] = new_meta
          expectation["kwargs"] = new_kwargs
          json_persisted_expectation = create_expectation(project.id, featurestore_id, fg_json["id"], parsed_suite["id"], expectation)
          parsed_persisted_expectation = JSON.parse(json_persisted_expectation)
          expect_status_details(201)
          json_fetched_expectation = get_expectation(project.id, featurestore_id, fg_json["id"], parsed_suite["id"], parsed_persisted_expectation["id"])
          parsed_fetched_expectation = JSON.parse(json_fetched_expectation)
          parsed_meta = JSON.parse(parsed_fetched_expectation["meta"])
          expect(parsed_fetched_expectation["kwargs"]).to eq(new_kwargs)
          expect(parsed_meta["whoAmI"]).to eq("edited_expectation")
        end

        
      end
    end

    describe "Expectation Various" do
      context 'with valid project, featurestore service enabled, a feature group and an expectation suite' do
        before :all do
          with_valid_project
        end

        it "should trigger an error to change expectation_type for existing expectation" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          first_kwargs = "{\"min_value\": 0, \"max_value\": 10}"
          expectation_suite = generate_template_expectation_suite()
          json_suite = create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
          parsed_suite = JSON.parse(json_suite)
          expectation = parsed_suite["expectations"][0]
          new_expectation_type = "expect_column"
          expectation["expectationType"] = new_expectation_type
          json_error = update_expectation(project.id, featurestore_id, fg_json["id"], parsed_suite["id"], expectation["id"], expectation)
          parsed_error = JSON.parse(json_error)
          expect(parsed_error.key?("errorCode")).to be true
          expect(parsed_error.key?("errorMsg")).to be true
          expect(parsed_error.key?("usrMsg")).to be true
          expect(parsed_error["errorCode"] == 270207).to be true
        end

        it "expectation creation should fail when kwargs contain column args and feature does not exist" do
          expected_error_message = "The Feature Name was not found in this version of the Feature Group."
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          fg_dto, fg_name = create_cached_featuregroup(project.id, featurestore_id)
          expect_status_details(201)
          fg_json = JSON.parse(fg_dto)
          expectation_suite = generate_template_expectation_suite()
          suite_dto = create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
          suite_json = JSON.parse(suite_dto)
          expectation = generate_template_expectation()
          expectation[:kwargs] = "{\"column\" :\"notAFeature\"}"
          error_dto = create_expectation(project.id, featurestore_id, fg_json["id"], suite_json["id"], expectation)
          expect_status_details(400)
          error_json = JSON.parse(error_dto)
          expect(error_json["errorCode"]).to eq(270210) 
          expect(error_json["errorMsg"]).to eq(expected_error_message)
          expect(error_json["usrMsg"]).to include("testfeature")
          expect(error_json["usrMsg"]).to include("notAFeature")
          expectation = generate_template_expectation()
          expectation[:kwargs] = "{\"columnA\" :\"notAFeature\", \"columnB\":\"testfeature\"}"
          error_dto = create_expectation(project.id, featurestore_id, fg_json["id"], suite_json["id"], expectation)
          expect_status_details(400)
          error_json = JSON.parse(error_dto)
          expect(error_json["errorCode"]).to eq(270210) 
          expect(error_json["errorMsg"]).to eq(expected_error_message)
          expect(error_json["usrMsg"]).to include("testfeature")
          expect(error_json["usrMsg"]).to include("notAFeature")
          expectation = generate_template_expectation()
          expectation[:kwargs] = "{\"column_list\" :[\"notAFeature\"]}"
          expectation_dto = create_expectation(project.id, featurestore_id, fg_json["id"], suite_json["id"], expectation)
          expect_status_details(400)
          error_json = JSON.parse(error_dto)
          expect(error_json["errorCode"]).to eq(270210) 
          expect(error_json["errorMsg"]).to eq(expected_error_message)
          expect(error_json["usrMsg"]).to include("testfeature")
          expect(error_json["usrMsg"]).to include("notAFeature")
          expectation = generate_template_expectation()
          expectation[:kwargs] = "{\"column_set\" :[\"notAFeature\"]}"
          expectation_dto = create_expectation(project.id, featurestore_id, fg_json["id"], suite_json["id"], expectation)
          expect_status_details(400)
          error_json = JSON.parse(error_dto)
          expect(error_json["errorCode"]).to eq(270210) 
          expect(error_json["errorMsg"]).to eq(expected_error_message)
          expect(error_json["usrMsg"]).to include("testfeature")
          expect(error_json["usrMsg"]).to include("notAFeature")
        end

        it "expectation creation should succeed when kwargs contain column args and feature exists" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          fg_dto, fg_name = create_cached_featuregroup(project.id, featurestore_id)
          expect_status_details(201)
          fg_json = JSON.parse(fg_dto)
          expectation_suite = generate_template_expectation_suite()
          suite_dto = create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
          suite_json = JSON.parse(suite_dto)
          expectation = generate_template_expectation()
          expectation[:kwargs] = "{\"column\" :\"testfeature\"}"
          expectation_dto = create_expectation(project.id, featurestore_id, fg_json["id"], suite_json["id"], expectation)
          expect_status_details(201)
          expectation = generate_template_expectation()
          expectation[:kwargs] = "{\"columnA\" :\"testfeature\", \"columnB\":\"testfeature\"}"
          expectation_dto = create_expectation(project.id, featurestore_id, fg_json["id"], suite_json["id"], expectation)
          expect_status_details(201)
          expectation = generate_template_expectation()
          expectation[:kwargs] = "{\"column_list\" :[\"testfeature\"]}"
          expectation_dto = create_expectation(project.id, featurestore_id, fg_json["id"], suite_json["id"], expectation)
          expect_status_details(201)
          expectation = generate_template_expectation()
          expectation[:kwargs] = "{\"column_set\" :[\"testfeature\"]}"
          expectation_dto = create_expectation(project.id, featurestore_id, fg_json["id"], suite_json["id"], expectation)
          expect_status_details(201)
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
          create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
          template_validation_report = generate_template_validation_report()
          dto = create_validation_report(project.id, featurestore_id, fg_json["id"], template_validation_report)
	        expect_status_details(201)
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
          create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
          template_validation_report = generate_template_validation_report()
          created_report = create_validation_report(project.id, featurestore_id, fg_json["id"], template_validation_report)
          expect_status_details(201)
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
          create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
          template_validation_report = generate_template_validation_report()
          created_report = create_validation_report(project.id, featurestore_id, fg_json["id"], template_validation_report)
          expect_status_details(201)
          meta_json = JSON.parse(template_validation_report[:meta])
          meta_json["age"] = "latest"
          meta_json["run_id"]["run_time"] = "2022-03-11T14:06:24.481236+00:00"
          template_validation_report[:meta] = JSON.generate(meta_json)
          sleep 3 
          created_report_2 = create_validation_report(project.id, featurestore_id, fg_json["id"], template_validation_report)
          expect_status_details(201)
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
          create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
          template_validation_report = generate_template_validation_report()
          created_report = create_validation_report(project.id, featurestore_id, fg_json["id"], template_validation_report)
          expect_status_details(201)
          meta_json = JSON.parse(template_validation_report[:meta])
          meta_json["age"] = "latest"
          meta_json["run_id"]["run_time"] = "2022-03-11T14:06:24.481236+00:00"
          template_validation_report[:meta] = JSON.generate(meta_json)
          sleep 3
          created_report_2 = create_validation_report(project.id, featurestore_id, fg_json["id"], template_validation_report)
          expect_status_details(201)
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
          create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
          template_validation_report = generate_template_validation_report()
          dto = create_validation_report(project.id, featurestore_id, fg_json["id"], template_validation_report)
	        expect_status_details(201)
	        dto_parsed = JSON.parse(dto)
	        expect(test_file(dto_parsed["fullReportPath"])).to eq(true)
          validation_report_id = dto_parsed["id"]
          delete_validation_report(project.id, featurestore_id, fg_json["id"], validation_report_id)
          expect_status_details(204)
          expect(test_file(dto_parsed["fullReportPath"])).to eq(false)
        end

        it "should delete on-disk report when featuregroup is deleted" do
        	project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          expectation_suite = generate_template_expectation_suite()
          create_expectation_suite(project.id, featurestore_id, fg_json["id"], expectation_suite)
          expect_status_details(201)
          template_validation_report = generate_template_validation_report()
          dto = create_validation_report(project.id, featurestore_id, fg_json["id"], template_validation_report)
          expect_status_details(201)
          dto_parsed = JSON.parse(dto)
          expect(test_file(dto_parsed["fullReportPath"])).to eq(true)
          delete_featuregroup_checked(project.id, featurestore_id, fg_json["id"])
          expect(test_file(dto_parsed["fullReportPath"])).to eq(false)
        end
      end
    end
    
  end
end
