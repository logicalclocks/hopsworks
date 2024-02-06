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

module FeatureStoreStatisticsHelper

  def generate_template_feature_descriptive_statistics(exact_uniqueness: false, shift_delta: 0.0)
    desc_stats = {
      featureName: "testfeature",
      featureType: "Fractional",
      min: (1 - shift_delta).floor(2),
      max: (6.9 + shift_delta).floor(2),
      sum: (558 + shift_delta).floor(2),
      count: 745,
      mean: (3.74 + shift_delta).floor(2),
      stddev: (1.76 + shift_delta).floor(2),
      completeness: 0.98,
      numNonNullValues: 742,
      numNullValues: 3,
      # percentiles: array of 100 numbers with the 25%, 50%, 75%, 90%, 95%, 99% percentile values.
      percentiles: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (1.72 - shift_delta).floor(2), 0, 0, 0, 0, 0, \
                      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (3.45 - shift_delta).floor(2), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, \
                      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (5.17 + shift_delta).floor(2), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, \
                      0, 0, 0, 0, 0, 0, 0, 0, 0, (6.21 + shift_delta).floor(2), 0, 0, 0, 0, (6.55 + shift_delta).floor(2), 0, 0, 0, (6.83 + shift_delta).floor(2), 0,],
      approxNumDistinctValues: 489,
    }
    if exact_uniqueness
      desc_stats = desc_stats.merge({
        distinctness: 0.8,
        entropy: 0.78,
        uniqueness: 0.82,
        exactNumDistinctValues: 490,
      })
    end
    [desc_stats]
  end

  def generate_template_feature_group_statistics(feature_descriptive_statistics: nil, split_statistics: nil, computation_time: 1597903688000, window_start_commit_time: nil, window_end_commit_time: nil, before_transformation: false)
    json_data = {
        computationTime: computation_time,
        rowPercentage: 1.0
    }
    # no implicit conversion of Array into String
    if feature_descriptive_statistics == nil
        if split_statistics == nil
           json_data[:featureDescriptiveStatistics] = JSON.parse('[{"featureName": "a"}, {"featureName": "b"}, {"featureName": "c"}]')
        else
           json_data[:splitStatistics] = split_statistics
        end
    else
        json_data[:featureDescriptiveStatistics] = JSON.parse(feature_descriptive_statistics.to_json)
    end

    json_data[:windowStartCommitTime] = window_start_commit_time
    json_data[:windowEndCommitTime] = window_end_commit_time
    json_data[:beforeTransformation] = before_transformation

    return json_data
  end

  def create_statistics_commit_fg(project_id, featurestore_id, fg_id, feature_descriptive_statistics: nil, computation_time: 1597903688000, window_start_commit_time: nil, window_end_commit_time: nil)
    post_statistics_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{fg_id}/statistics"
    post_statistics_commit(post_statistics_endpoint, feature_descriptive_statistics: feature_descriptive_statistics, computation_time: computation_time, window_start_commit_time: window_start_commit_time, window_end_commit_time: window_end_commit_time)
  end

  # for feature monitoring test only
  def create_statistics_commit_fv(project_id, featurestore_id, fv_name, fv_version, feature_descriptive_statistics: nil, computation_time: 1597903688000, window_start_commit_time: nil, window_end_commit_time: nil)
    post_statistics_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featureview/#{fv_name}/version/#{fv_version}/statistics"
    post_statistics_commit(post_statistics_endpoint, feature_descriptive_statistics: feature_descriptive_statistics, computation_time: computation_time, window_start_commit_time: window_start_commit_time, window_end_commit_time: window_end_commit_time)
  end

  def create_statistics_commit_td(project_id, featurestore_id, fv_name, fv_version, td_version, feature_descriptive_statistics: nil, split_statistics: nil, computation_time: 1597903688000, window_start_commit_time: nil, window_end_commit_time: nil, before_transformation: false)
    post_statistics_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featureview/#{fv_name}/version/#{fv_version}/trainingdatasets/version/#{td_version}/statistics"
    post_statistics_commit(post_statistics_endpoint, feature_descriptive_statistics: feature_descriptive_statistics, split_statistics: split_statistics, computation_time: computation_time, window_start_commit_time: window_start_commit_time, window_end_commit_time: window_end_commit_time, before_transformation: before_transformation)
  end

  def post_statistics_commit(post_statistics_endpoint, feature_descriptive_statistics: nil, split_statistics: nil, computation_time: 1597903688000, window_start_commit_time: nil, window_end_commit_time: nil, before_transformation: false)
    json_data = generate_template_feature_group_statistics(feature_descriptive_statistics: feature_descriptive_statistics, split_statistics: split_statistics, computation_time: computation_time, window_start_commit_time: window_start_commit_time, window_end_commit_time: window_end_commit_time, before_transformation: before_transformation)
    post post_statistics_endpoint, json_data.to_json
  end
 
  def get_statistics_commit_fg(project_id, featurestore_id, fg_id, computation_time: 1597903688000, content: true, feature_names: nil)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{fg_id}/statistics?sort_by=computation_time:desc&filter_by=computation_time_ltoeq:#{computation_time}&offset=0&limit=1"
    if feature_names != nil
      endpoint = endpoint + "&feature_names=" + feature_names
    end
    if content != nil && content
      endpoint = endpoint + "&fields=content"
    end
    get endpoint
  end

  def get_statistics_commit_td(project_id, featurestore_id, fv_name, fv_version, td_version, before_transformation: false, content: true, feature_names: nil)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featureview/#{fv_name}/version/#{fv_version}/trainingdatasets/version/#{td_version}/statistics?filter_by=before_transformation_eq:#{before_transformation}"
    if feature_names != nil
      endpoint = endpoint + "&feature_names=" + feature_names
    end
    if content != nil && content
      endpoint = endpoint + "&fields=content"
    end
    get endpoint
  end

  def get_statistics_commits_fg(project_id, featurestore_id, fg_id, content: true, feature_names: nil)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{fg_id}/statistics?sort_by=computation_time:desc"
    if feature_names != nil
      endpoint = endpoint + "&feature_names=" + feature_names
    end
    if content != nil && content
      endpoint = endpoint + "&fields=content"
    end
    get endpoint
  end

  def get_statistics_commits_td(project_id, featurestore_id, fv_name, fv_version, td_version, before_transformation: false, content: true, feature_names: nil)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featureview/#{fv_name}/version/#{fv_version}/trainingdatasets/version/#{td_version}/statistics?sort_by=computation_time:desc&filter_by=before_transformation_eq:#{before_transformation}"
    if feature_names != nil
      endpoint = endpoint + "&feature_names=" + feature_names
    end
    if content != nil && content
      endpoint = endpoint + "&fields=content"
    end
    get endpoint
  end

  def get_statistics_commit_by_window_fg(project_id, featurestore_id, fg_id, window_start_commit_time: 1603577485000, window_end_commit_time: 1603667485000, content: true, feature_names: nil)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{fg_id}/statistics?sort_by=window_end_commit_time:desc&sort_by=window_start_commit_time:asc&filter_by=window_start_commit_time_gtoeq:#{window_start_commit_time}&filter_by=window_end_commit_time_ltoeq:#{window_end_commit_time}&offset=0&limit=1"
    if feature_names != nil
      endpoint = endpoint + "&feature_names=" + feature_names
    end
    if content != nil && content
      endpoint = endpoint + "&fields=content"
    end
    get endpoint
  end

  def get_last_computed_statistics_commit_fg(project_id, featurestore_id, fg_id, content: true, feature_names: nil)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{fg_id}/statistics?sort_by=computation_time:desc&offset=0&limit=1"
    if feature_names != nil
      endpoint = endpoint + "&feature_names=" + feature_names
    end
    if content != nil && content
      endpoint = endpoint + "&fields=content"
    end
    get endpoint
  end

  def get_last_computed_statistics_by_window_fg(project_id, featurestore_id, fg_id, window_end_commit_time: 1603667485000, content: true, feature_names: nil)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featuregroups/#{fg_id}/statistics?sort_by=window_end_commit_time:desc&sort_by=window_start_commit_time:asc&offset=0&limit=1&filter_by=window_end_commit_time_eq:#{window_end_commit_time}"
    if feature_names != nil
      endpoint = endpoint + "&feature_names=" + feature_names
    end
    if content != nil && content
      endpoint = endpoint + "&fields=content"
    end
    get endpoint
  end

  def get_last_computed_statistics_commit_td(project_id, featurestore_id, fv_name, fv_version, td_version, before_transformation: false, content: true, feature_names: nil)
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/featurestores/#{featurestore_id}/featureview/#{fv_name}/version/#{fv_version}/trainingdatasets/version/#{td_version}/statistics?sort_by=computation_time:desc&offset=0&limit=1&filter_by=before_transformation_eq:#{before_transformation}"
    if feature_names != nil
      endpoint = endpoint + "&feature_names=" + feature_names
    end
    if content != nil && content
      endpoint = endpoint + "&fields=content"
    end
    get endpoint
  end
end
