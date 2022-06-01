=begin
 This file is part of Hopsworks
 Copyright (C) 2018, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end

module FeaturestoreValidationHelper
  def generate_template_expectation()
    {
      "expectationType": "expect_column_max_to_be_between",
      "kwargs": "{\"min_value\": 0, \"max_value\": 1 }",
      "meta": "{\"whoAmI\": \"template_expectation\"}"
    }
  end
  
  def generate_template_expectation_suite()
    expectation_json = generate_template_expectation()
    {
      "geCloudId": "blue",
      "dataAssetType": "Suite",
      "expectationSuiteName": "expectation_suite_101",
      "expectations": [expectation_json],
      "meta": "{\"whoAmI\": \"template_expectation_suite\" }",
      "runValidation": true,
      "validationIngestionPolicy": "STRICT"
    }
  end
  
  def persist_expectation_suite(project_id, featurestore_id, featuregroup_id, expectation_suite)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/expectationsuite"
    put endpoint, expectation_suite
  end

  def get_expectation_suite(project_id, featurestore_id, featuregroup_id)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/expectationsuite"
    get endpoint
  end

  def delete_expectation_suite(project_id, featurestore_id, featuregroup_id)
    endpoint = "#{ENV["HOPSWORKS_API"]}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/expectationsuite"
    delete endpoint
  end

  def generate_template_validation_result()
    {
      "result": "{\"observed_value\":4,\"element_count\":5,\"missing_count\":null,\"missing_percent\":null}",
      "exceptionInfo": "{\"raised_exception\":false,\"exception_message\":null,\"exception_traceback\":null}",
      "meta": "{\"whoAmI\":\"template_validation_result\"}",
      "success": true,
      "expectationConfig": "{\"kwargs\":{\"column\":\"A\",\"max_value\":100,\"min_value\":4},\"expectation_type\":\"expect_column_max_to_be_between\",\"meta\":{\"expectationId\":1}}"
    }
  end

  def generate_template_validation_report()
    template_validation_result = generate_template_validation_result()
    {
      "evaluationParameters": "{}",
      "meta": "{\"great_expectations_version\":\"0.14.10\",\"expectation_suite_name\":\"expecations_suite_101\",\"run_id\":{\"run_time\":\"2022-03-11T13:06:24.481236+00:00\",\"run_name\":null},\"batch_kwargs\":{\"ge_batch_id\":\"0d0afc48-a13c-11ec-b113-020f94a1da7f\"},\"batch_markers\":{},\"batch_parameters\":{},\"validation_time\":\"20220311T130624.481059Z\",\"expectation_suite_meta\":{\"great_expectations_version\":\"0.14.10\"}}",
      "results": [template_validation_result],
      "success": false,
      "statistics": "{\"evaluated_expectations\":7,\"successful_expectations\":5,\"unsuccessful_expectations\":2,\"success_percent\":71.42857142857143}"
    }
  end

  def persist_validation_report(project_id, featurestore_id, featuregroup_id, validation_report)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/validationreport"
    put endpoint, validation_report
  end

  def parse_single_expectation_id(project_id, featurestore_id, featuregroup_id)
    expectation_suite = get_expectation_suite(project_id, featurestore_id, featuregroup_id)
    json_suite = JSON.parse(expectation_suite)
    JSON.parse(json_suite["expectations"][0]["meta"])["expectationId"]
  end

  def create_validation_report(project_id, featurestore_id, featuregroup_id, validation_report)
    expectation_id = parse_single_expectation_id(project_id, featurestore_id, featuregroup_id)
    expectation_config = JSON.parse(validation_report[:results][0][:expectationConfig])
    expectation_config["meta"]["expectationId"] = expectation_id
    validation_report[:results][0][:expectationConfig] = JSON.generate(expectation_config)
    persist_validation_report(project_id, featurestore_id, featuregroup_id, validation_report)
  end

  def get_validation_report_by_id(project_id, featurestore_id, featuregroup_id, validation_report_id)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/validationreport/#{validation_report_id}"
  end

  def get_latest_validation_report(project_id, featurestore_id, featuregroup_id)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/validationreport?sort_by=validation_time:desc&offset=0&limit=1"
  end

  def get_all_validation_report(project_id, featurestore_id, featuregroup_id)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/validationreport?sort_by=validation_time:desc"
  end

  def delete_validation_report(project_id, featurestore_id, featuregroup_id, validation_report_id)
    delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/validationreport/#{validation_report_id}"
  end
end