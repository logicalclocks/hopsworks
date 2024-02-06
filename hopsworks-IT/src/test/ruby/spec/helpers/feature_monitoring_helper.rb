# This file is part of Hopsworks
# Copyright (C) 2023, Hopsworks AB. All rights reserved
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

module FeatureMonitoringHelper
  def generate_template_descriptive_statistics_comparison_config()
    {
      "threshold": 1,
      "strict": true,
      "relative": true,
      "metric": "MEAN"
    }
  end

  def generate_template_monitoring_scheduler_config()
    {
      "startDateTime": 1677670460000, # Wed Mar 01 2023 11:30:00 GMT+0000
      "cronExpression": "0 0 * ? * * *",
      "enabled": true,

    }
  end

  def generate_template_detection_window_config(window_type: "ROLLING_TIME")
    window = { "windowConfigType": window_type }
    if window_type == "ROLLING_TIME"
      window = window.merge({
        "rowPercentage": 0.13,
        "windowLength": "1h",
        "timeOffset": "1d"
      })
    end
    # TODO: add other types of detection window
    window
  end

  def generate_template_reference_window_config(window_type: "TRAINING_DATASET")
    if window_type.nil?
      return nil
    end

    window = { "windowConfigType": window_type }
    if window_type == "TRAINING_DATASET"
      window = window.merge({ "trainingDatasetVersion": 12 })
    end
    if window_type == "SPECIFIC_VALUE"
      window = window.merge({ "specificValue": 20 })
    end
    if window_type == "ROLLING_TIME"
      window = window.merge({
        "rowPercentage": 1.0,
        "windowLength": "1h",
        "timeOffset": "1d"
      })
    end
    # TODO: add other types of reference window
    window
  end

  def generate_template_stats_monitoring_config(featurestore_id, entity_id, entity_name, entity_version, is_entity_fg)
    detection_window = generate_template_detection_window_config()
    schedule = generate_template_monitoring_scheduler_config()
    config = {
      "jobSchedule": schedule,
      "featureStoreId": featurestore_id,
      "featureName": "testfeature",
      "name": "test_stats_config",
      "description": "Configuration created for ruby integration test",
      "featureMonitoringType": "STATISTICS_COMPUTATION",
      "detectionWindowConfig": detection_window,
    }
    if is_entity_fg
      config[:featureGroupId] = entity_id
    else
      config[:featureViewName] = entity_name
      config[:featureViewVersion] = entity_version
    end
    config
  end

  def generate_template_feature_monitoring_config(featurestore_id, entity_id, entity_name, entity_version, is_entity_fg, \
    det_window_type: "ROLLING_TIME", ref_window_type: "TRAINING_DATASET", name: "test_config")
    stats_comparison_config = generate_template_descriptive_statistics_comparison_config()
    detection_window = generate_template_detection_window_config(window_type: det_window_type)
    reference_window = generate_template_reference_window_config(window_type: ref_window_type)
    schedule = generate_template_monitoring_scheduler_config()
    config = {
      "jobSchedule": schedule,
      "featureStoreId": featurestore_id,
      "featureName": "testfeature",
      "name": name,
      "description": "Configuration created for ruby integration test",
      "featureMonitoringType": "STATISTICS_COMPARISON",
      "statisticsComparisonConfig": stats_comparison_config,
      "detectionWindowConfig": detection_window,
      "referenceWindowConfig": reference_window
    }
    if is_entity_fg
      config[:featureGroupId] = entity_id
    else
      config[:featureViewName] = entity_name
      config[:featureViewVersion] = entity_version
    end
    config
  end

  def expect_window_config_equal(actual, expected)
    expect(actual.has_key?("id")).to be true
    expect(actual["windowConfigType"]).to eq(expected[:windowConfigType])
    expect(actual["specificValue"]).to eq(expected[:specificValue])
    expect(actual["rowPercentage"]).to eq(expected[:rowPercentage])
    expect(actual["windowLength"]).to eq(expected[:windowLength])
    expect(actual["timeOffset"]).to eq(expected[:timeOffset])
    expect(actual["trainingDatasetVersion"]).to eq(expected[:trainingDatasetVersion])
  end

  def expect_stats_comparison_equal(actual, expected, threshold: nil)
    expect(actual.has_key?("id")).to be true
    expect(actual["strict"]).to eq(expected[:strict])
    expect(actual["relative"]).to eq(expected[:relative])
    expect(actual["metric"]).to eq(expected[:metric])
    if threshold.nil?
      expect(actual["threshold"]).to eq(expected[:threshold])
    else
      expect(actual["threshold"]).to eq(threshold)
    end
  end

  def expect_monitoring_scheduler_equal(actual, expected)
    expect(actual.has_key?("id")).to be true
    expect(actual["startDateTime"]).to eq(1677670460000) # Wed Mar 01 2023 11:30:00 GMT+0000
    expect(actual["cronExpression"]).to eq(expected[:cronExpression])
    expect(actual["enabled"]).to eq(expected[:enabled])
  end

  def expect_partial_feature_monitoring_equal(actual, expected, job_name, name, fg_id: nil, fv_name: nil, fv_version: nil)
    expect(actual.has_key?("id")).to be true
    expect(actual["featureStoreId"]).to eq(expected[:featureStoreId])
    expect(actual["featureName"]).to eq(expected[:featureName])
    expect(actual["name"]).to eq(name)
    expect(actual["description"]).to eq(expected[:description])
    expect(actual["featureMonitoringType"]).to eq(expected[:featureMonitoringType])
    expect(actual["featureGroupId"]).to eq(fg_id)
    expect(actual["featureViewName"]).to eq(fv_name)
    expect(actual["featureViewVersion"]).to eq(fv_version)
    expect(actual["jobName"]).to eq(job_name)
  end

  def expect_feature_monitoring_equal(actual, expected, job_name, name, fg_id: nil, fv_name: nil, fv_version: nil)
    expect_partial_feature_monitoring_equal(actual, expected, job_name, name, fg_id: fg_id, fv_name: fv_name, fv_version: fv_version)
    expect_monitoring_scheduler_equal(actual["jobSchedule"], expected[:jobSchedule])
    expect_window_config_equal(actual["detectionWindowConfig"], expected[:detectionWindowConfig])
    if expected.has_key?(:referenceWindowConfig)
      expect_stats_comparison_equal(actual["statisticsComparisonConfig"], expected[:statisticsComparisonConfig])
      expect_window_config_equal(actual["referenceWindowConfig"], expected[:referenceWindowConfig])
    else
      expect(actual.has_key?("statisticsComparisonConfig")).to eq(false)
    end
  end


  def create_feature_monitoring_configuration_fg(project_id, featurestore_id, featuregroup_id, feature_monitoring_config)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featuregroups/#{featuregroup_id}/featuremonitoring/config"
    post endpoint, feature_monitoring_config
  end

  def get_feature_monitoring_configuration_by_feature_name_fg(project_id, featurestore_id, featuregroup_id, feature_name)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featuregroups/#{featuregroup_id}/featuremonitoring/config/feature/#{feature_name}"
    get endpoint
  end

  def get_feature_monitoring_configuration_by_entity_fg(project_id, featurestore_id, featuregroup_id)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featuregroups/#{featuregroup_id}/featuremonitoring/config/entity"
    get endpoint
  end

  def get_feature_monitoring_configuration_by_id_fg(project_id, featurestore_id, featuregroup_id, config_id)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featuregroups/#{featuregroup_id}/featuremonitoring/config/#{config_id}"
    get endpoint
  end

  def get_feature_monitoring_configuration_by_name_fg(project_id, featurestore_id, featuregroup_id, config_name)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featuregroups/#{featuregroup_id}/featuremonitoring/config/name/#{config_name}"
    get endpoint
  end

  def update_feature_monitoring_configuration_fg(project_id, featurestore_id, featuregroup_id, feature_monitoring_config)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featuregroups/#{featuregroup_id}/featuremonitoring/config/#{feature_monitoring_config[:id]}"
    put endpoint, feature_monitoring_config
  end

  def delete_feature_monitoring_configuration_by_id_fg(\
    project_id, featurestore_id, featuregroup_id, config_id)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featuregroups/#{featuregroup_id}/featuremonitoring/config/#{config_id}"
    delete endpoint
  end

  def create_feature_monitoring_configuration_fv(\
    project_id, featurestore_id, feature_view_name, feature_view_version, feature_monitoring_config)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featureview/#{feature_view_name}/version/#{feature_view_version}/featuremonitoring/config"
    post endpoint, feature_monitoring_config
  end

  def get_feature_monitoring_configuration_by_feature_name_fv(\
    project_id, featurestore_id, feature_view_name, feature_view_version, feature_name)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featureview/#{feature_view_name}/version/#{feature_view_version}/featuremonitoring/config/"
    endpoint += "feature/#{feature_name}"
    get endpoint
  end

  def get_feature_monitoring_configuration_by_entity_fv(\
    project_id, featurestore_id, feature_view_name, feature_view_version)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featureview/#{feature_view_name}/version/#{feature_view_version}/featuremonitoring/config/"
    endpoint += "entity"
    get endpoint
  end

  def get_feature_monitoring_configuration_by_id_fv(\
    project_id, featurestore_id, feature_view_name, feature_view_version, config_id)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featureview/#{feature_view_name}/version/#{feature_view_version}/featuremonitoring/config/#{config_id}"
    get endpoint
  end

  def get_feature_monitoring_configuration_by_name_fv(\
    project_id, featurestore_id, feature_view_name, feature_view_version, config_name)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featureview/#{feature_view_name}/version/#{feature_view_version}/"
    endpoint += "featuremonitoring/config/name/#{config_name}"
    get endpoint
  end

  def update_feature_monitoring_configuration_fv(\
    project_id, featurestore_id, feature_view_name, feature_view_version, config)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featureview/#{feature_view_name}/version/#{feature_view_version}/featuremonitoring/config/"
    endpoint += "#{config[:id]}"
    put endpoint, config
  end

  def delete_feature_monitoring_configuration_by_id_fv(\
    project_id, featurestore_id, feature_view_name, feature_view_version, config_id)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featureview/#{feature_view_name}/version/#{feature_view_version}/featuremonitoring/config/#{config_id}"
    delete endpoint
  end

  def generate_template_feature_monitoring_result(\
    featurestore_id, config_id, monitoring_time, detection_statistics_id, reference_statistics_id: nil, specific_value: nil)
    result = {
      "featureStoreId": featurestore_id,
      "configId": config_id,
      "featureName": "testfeature",
      "executionId": 1,
      "difference": -1,
      "specificValue": specific_value,
      "shiftDetected": false,
      "monitoringTime": monitoring_time.round(-3),
      "detectionStatisticsId": detection_statistics_id,
      "referenceStatisticsId": reference_statistics_id,
      "raisedException": false,
      "emptyDetectionWindow": false,
      "emptyReferenceWindow": false,
    }
    if detection_statistics_id.nil?
      result[:emptyDetectionWindow] = true
    end
    if reference_statistics_id.nil?
      result[:emptyReferenceWindow] = true
    end
    result
  end

  def create_feature_monitoring_result_fg(project_id, featurestore_id, featuregroup_id, feature_monitoring_result)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featuregroups/#{featuregroup_id}/featuremonitoring/result"
    post endpoint, feature_monitoring_result
  end

  def get_all_feature_monitoring_results_by_config_id_fg(\
    project_id, featurestore_id, featuregroup_id, config_id, with_stats: false)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featuregroups/#{featuregroup_id}/featuremonitoring/result/"
    endpoint += "byconfig/#{config_id}?sort_by=monitoring_time:desc"
    if with_stats
      endpoint += "&expand=statistics"
    end
    get endpoint
  end

  def get_feature_monitoring_result_by_id_fg(project_id, featurestore_id, featuregroup_id, result_id)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featuregroups/#{featuregroup_id}/featuremonitoring/result/#{result_id}"
    get endpoint
  end

  def delete_feature_monitoring_result_by_id_fg(project_id, featurestore_id, featuregroup_id, result_id)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featuregroups/#{featuregroup_id}/featuremonitoring/result/#{result_id}"
    delete endpoint
  end

  def create_feature_monitoring_result_fv(\
    project_id, featurestore_id, feature_view_name, feature_view_version, feature_monitoring_result)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featureview/#{feature_view_name}/version/#{feature_view_version}/"
    endpoint += "featuremonitoring/result"
    post endpoint, feature_monitoring_result
  end

  def get_all_feature_monitoring_results_by_config_id_fv(\
    project_id, featurestore_id, feature_view_name, feature_view_version, config_id, with_stats: false)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featureview/#{feature_view_name}/version/#{feature_view_version}/"
    endpoint += "featuremonitoring/result/byconfig/#{config_id}?sort_by=monitoring_time:desc"
    if with_stats
      endpoint += "&expand=statistics"
    end
    get endpoint
  end

  def get_feature_monitoring_result_by_id_fv(\
    project_id, featurestore_id, feature_view_name, feature_view_version, result_id)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featureview/#{feature_view_name}/version/#{feature_view_version}/"
    endpoint += "featuremonitoring/result/#{result_id}"
    get endpoint
  end

  def delete_feature_monitoring_result_by_id_fv(\
    project_id, featurestore_id, feature_view_name, feature_view_version, result_id)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/"
    endpoint += "featureview/#{feature_view_name}/version/#{feature_view_version}/"
    endpoint += "featuremonitoring/result/#{result_id}"
    delete endpoint
  end

  def expect_desc_statistics_to_be_eq(stats_1, stats_2)
    expect(stats_1).not_to be_nil
    expect(stats_2).not_to be_nil

    # sanity ruby hashes so they both use string keys
    stats_1 = JSON.parse(JSON.generate(stats_1))
    stats_2 = JSON.parse(JSON.generate(stats_2))
    if stats_1.key?("id") || stats_2.key?("id")
      stats_1["id"] = nil
      stats_2["id"] = nil
    end

    # start / end times are not included when retrieving statistics
    stats_1.delete("startTime") unless stats_1["startTime"].nil?
    stats_1.delete("endTime") unless stats_1["endTime"].nil?
    stats_1.delete("rowPercentage") unless stats_1["rowPercentage"].nil?
    stats_2.delete("startTime") unless stats_2["startTime"].nil?
    stats_2.delete("endTime") unless stats_2["endTime"].nil?
    stats_2.delete("rowPercentage") unless stats_2["rowPercentage"].nil?

    expect(stats_1).to eq(stats_2)
  end
end