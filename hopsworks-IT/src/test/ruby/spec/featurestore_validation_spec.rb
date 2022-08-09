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

    describe "expectation suite others" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
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
